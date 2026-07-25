# Desenho de domínio — o agregado Passagem (estado atual)

**Status:** Rascunho — Claude rascunhou, revisão do analista pendente. Fotografia do estado corrente em
`2026-07`, após o [ADR-0012](../adr/0012-ciclo-de-vida-passagem-e-embarque-qr.md) fechado (F1..F5).

> Complementa o [estudo transversal de domínio](dominio-relacionamentos-e-camadas.md) (nove entidades),
> que descrevia a Passagem como "~50 campos planos / snapshot" **antes** dela virar agregado com ciclo
> de vida tipado. Aqui o foco é só a Passagem, com o que o ADR-0012 acrescentou. Decisões-fonte:
> [ADR-0003](../adr/0003-modelo-de-memoria-do-dado.md) (camadas do dado),
> [ADR-0008](../adr/0008-relacionamentos-por-identidade.md) (id vs. snapshot),
> [ADR-0010](../adr/0010-autorizacao-por-cargo.md) / [ADR-0011](../adr/0011-regras-firestore-por-cargo.md)
> (autorização) e [ADR-0012](../adr/0012-ciclo-de-vida-passagem-e-embarque-qr.md) (ciclo de vida + QR).

---

## 1. O que a Passagem é no domínio

A **Passagem** é o bilhete emitido — o registro **transacional / de evento** do negócio (venda e emissão
de passagens de balsa fluvial). É o único agregado do app: as demais entidades (Empresa, Navio, Viagem,
Agente) são *master data*; a Passagem é o fato que as consome e congela.

Em DDD, a Passagem é um **agregado** cujo DTO aninhado no Firestore é a forma natural (viagem +
passageiros + veículo embutidos), e cuja forma plana no Room (~60 colunas) é o artefato de tenacidade
discutido no ADR-0003.

**Quem viaja são os participantes do agregado — e eles são de mesmo nível.** O **sujeito** da passagem é
o **passageiro 1**, o titular do bilhete (obrigatório); os passageiros 2 e 3 são acompanhantes opcionais
(daí os derivados `temPassageiro2`/`temPassageiro3`). O **veículo é participante de mesmo nível do
passageiro — não um value object descritivo** — presente quando há veículo na travessia (derivado
`ehVeiculo`). Ele viaja sob um **passageiro responsável pela retirada**; o código comprova o vínculo — os
campos `nomeResponsavelRetirada`/`tipoDocumentoResponsavelRetirada`/`documentoResponsavelRetirada` moram
no **estado do veículo** (`FormVeiculoUiState`), não num estado de passageiro. Esse responsável, porém, é
**opcional**: pode não ser nomeado, o que admite um veículo **sem responsável identificado** — uma
inconsistência de informação **tolerada, não barrada** pelo modelo.

> Isto **revisa** o rótulo "value object" que o [estudo transversal](dominio-relacionamentos-e-camadas.md)
> deu a passageiros/veículo. No plano do domínio eles são participantes do agregado — passageiro como
> titular, veículo como parte de mesmo nível com responsável próprio. (No plano da persistência ainda são
> membros embutidos sem identidade própria — não têm `id`; a tensão entre as duas leituras fica em §9.)

Uma característica estrutural a registrar: **`Passagem.kt` acumula três papéis num só arquivo** —
`@Entity` do Room, modelo de domínio e fonte dos snapshots. Não há entidade Room separada do modelo de
domínio (contraste com a fronteira DTO↔domínio que existe do lado do Firestore). É simplicidade
deliberada, mas amarra o schema de persistência ao tipo de domínio.

`app/src/main/java/dev/matheus/fluviapp/model/passagem/Passagem.kt:9`

## 2. Anatomia da entidade

Os campos se agrupam por natureza — e a natureza é o que decide se o dado é ponteiro, snapshot ou estado:

| Grupo | Campos | Natureza |
|---|---|---|
| Identidade | `id` (PK), `numero` | chave surrogate + número do bilhete |
| **Ponteiros por id** (ADR-0008) | `viagemId`, `navioId`, `empresaId` | link vivo p/ relacionar/agregar |
| **Snapshot por valor** | `codigoViagem`, `empresa`, `navio`, `origem`, `destino`, `dataViagem`, `horaViagem` | histórico imutável do bilhete |
| Comercial | `agencia`, `agente`, `valorPago/Pix/Dinheiro/Debito/Credito`, `desconto`, `tipoPassagem`, `gratuidade`, `acomodacao`, `observacao` | dados da venda |
| Passageiros ×3 | `nome/documento/numeroDocumento/dataNascimento` (×3) | participantes do agregado — o **passageiro 1** é o **titular** (obrigatório); 2 e 3 são acompanhantes opcionais |
| Veículo (+ responsável) | `tipoVeiculo`, `modeloVeiculo`, `placaVeiculo`, `corVeiculo` + `*ResponsavelRetirada` | participante de **mesmo nível** do passageiro; carrega o **passageiro responsável pela retirada** |
| **Autoria** (ADR-0010) | `funcionarioId` (uid dono, congelado na emissão) + `funcionarioResponsavel` (nome snapshot) | par id+snapshot |
| **Ciclo de vida** (ADR-0012) | `status: String` | estado da FSM (String só na fronteira — ver §3) |
| **Registro de embarque** (ADR-0012) | `embarcadaPorId` (uid), `embarcadaPor` (nome snapshot), `embarcadaEm` (timestamp) | carimbo do check-in |
| Derivados `@Ignore` | `temPassageiro2`, `temPassageiro3`, `ehVeiculo` | conveniências de UI |

Todos os campos aditivos nasceram com default `""`/`null` para cobrir bilhetes anteriores sem backfill
(portfólio — regenera via seed). As migrações Room correspondentes: `viagemId/navioId/empresaId`
(v9→v11), `funcionarioId` (v11→v12), campos de embarque (v12→v13).

`Passagem.kt:11-68`

## 3. Ciclo de vida — `StatusPassagem` como tipo + FSM

O status **deixou de ser String pendurada no catálogo genérico** (`Constante.Descricao`, origem da grafia
à deriva "A EMITIR"/"A_EMITIR"/"EMITIDA") e virou tipo de domínio próprio com máquina de estados
fail-closed.

```
  criar                 emitir/imprimir            escanear QR no embarque
 ───────►  A_EMITIR ───────────────────► EMITIDA ────────────────────────► EMBARCADA
 (rascunho)            (QR passa a valer)          (check-in: valida e            (terminal —
                                                    consome o bilhete)             irreversível)
```

- `enum class StatusPassagem { A_EMITIR, EMITIDA, EMBARCADA }` — `proximos` define as arestas;
  `EMBARCADA` é terminal (`proximos` vazio). `podeTransicionarPara(destino)`, `ehTerminal()`.
- **String só na fronteira**: `de(valor: String?)` converte na leitura (tolerante à grafia legada:
  normaliza espaço→underscore e caixa; `null` fail-closed p/ desconhecido); grava-se sempre o `.name`
  canônico; `rotulo()` formata só para exibição.
- **Cancelar não é estado** — continua sendo *delete físico*. `CANCELADA`/`EXPIRADA` ficaram registrados
  como futuro (evitam mexer no fluxo de deletar e em regra temporal neste incremento).

`app/src/main/java/dev/matheus/fluviapp/model/passagem/StatusPassagem.kt:13`

**Onde cada aresta é disparada:**
- `A_EMITIR → EMITIDA`: ao **imprimir** o bilhete (`ImpressaoHelper.atualizaSituacao` →
  `transicionar(..., EMITIDA)`). Emitir ≈ imprimir/compartilhar — o QR passa a valer.
- `EMITIDA → EMBARCADA`: ao **confirmar o embarque** por QR (§4).

A transição de aplicação vive no repositório, idempotente e fail-closed, espelhando **Room + Firestore**
(corrige o vazamento de SSOT do antigo `atualizarSituacao`, que só tocava o Firestore):
`PassagemFirestoreRepository.transicionar(id, novo)` — `PassagemFirestoreRepository.kt:189`.

## 4. Confirmação de embarque por QR

O QR do bilhete é **ponteiro, não credencial**: o payload é sempre `Passagem.id`, idêntico no bilhete
físico e no digital (ZXing, `QRCodeGenerator.generate`). O scanner (CameraX + ML Kit, offline) lê o id e
**a validação acontece contra o servidor**, não contra o espelho local — o bilhete pode ter sido emitido
em outro device.

Fluxo (`EmbarqueViewModel` → `PassagemFirestoreRepository.confirmarEmbarque`):
1. `obterDoServidorPorId(id)` — leitura **ao vivo** do Firestore (`PassagemFirestoreRepository.kt:208`).
2. Avalia o status corrente pela FSM e retorna um caso tipado (`PassagemFirestoreRepository.kt:218`).
3. Se legal, carimba `status=EMBARCADA` + `embarcadaPorId`/`embarcadaPor`/`embarcadaEm` (timestamp
   `dd/MM/yyyy HH:mm`), grava Firestore (fronteira) e espelha Room best-effort.

O resultado é um `sealed interface ResultadoEmbarque` que a UI consome caso a caso
(`app/src/main/java/dev/matheus/fluviapp/model/passagem/ResultadoEmbarque.kt:7`):

| Caso | Significado |
|---|---|
| `Confirmada(passagem)` | aresta `EMITIDA→EMBARCADA` aplicada; carrega o bilhete carimbado |
| `JaEmbarcada(por, em)` | reuso barrado (idempotência / antifraude) — mostra quem e quando embarcou |
| `NaoEmitida` | ainda `A_EMITIR` (ou status desconhecido) — não pode embarcar |
| `NaoEncontrada` | nenhum doc com esse id no Firestore (QR estranho ao sistema) |

O acesso à tela de embarque é o **FAB central protruso** da barra inferior (Início · Embarque · Menu),
tratado como ação de rotina a um toque; a tela tem Scaffold próprio e trata a permissão `CAMERA` em
runtime. `ui/screens/passagem/EmbarqueScreen.kt`, `ui/viewmodel/passagem/EmbarqueViewModel.kt:22`.

## 5. Relações por identidade e snapshots (ADR-0008)

A Passagem é o caso onde **id-para-relacionar e valor-para-lembrar coexistem legitimamente** — o design
maduro que o estudo transversal apontou:

- **Ponteiros por id, congelados na emissão** (`viagemId`, `navioId`, `empresaId`): sobrevivem a rename
  do master data. O balanço agrega por `navioId` frozen — renomear/reatribuir a Viagem depois **não**
  altera bilhetes históricos.
- **Snapshot por valor** (`empresa`, `navio`, `origem`, `destino`, `codigoViagem`…): registro imutável
  do que o bilhete dizia no momento da emissão. Um bilhete impresso não deve mudar se a Viagem for
  editada depois.

O congelamento acontece na montagem (`FormPassagemHelper.montarPassagem`, populado por
`atualizarDadosViagemPorId`). Na leitura, `PassagemDadosPassagemMapper` já resolve **empresa por id**
(rename-safe, órfão detectável); o **navio ainda vem do snapshot** — ponto aberto residual (§8).

## 6. Formas do dado e mappers

A Passagem existe em quatro formas, cada uma na sua camada (ADR-0003: Firestore=verdade, Room=cache
espelho, tela=volátil):

```
PassagemDocumento  ──toPassagem(id)──►  Passagem  ──map──►  DadosPassagem
  (Firestore,          ◄──toPassagemDocumento──   (Room + domínio,   (tela: detalhes/
   aninhado/DTO)                                    plano)             impressão; situacao rotulada)
```

| Forma | Arquivo | Papel |
|---|---|---|
| `PassagemDocumento` | `services/repository/firebase/documents/PassagemDocumento.kt:5` | DTO Firestore **aninhado** (viagem/passageiros/veículo) |
| `Passagem` | `model/passagem/Passagem.kt:9` | entidade Room **plana** + domínio |
| `DadosPassagem` | `model/screendata/DadosPassagem.kt` | projeção de tela (`situacao` já rotulada) |
| `RascunhoPassagem` | `model/rascunho/RascunhoPassagemMapper.kt` | rascunho/draft da emissão |

Mappers: `extensions/PassagemDocumentoExtensions.kt:9` (`toPassagemDocumento`, achata→aninha),
`PassagemDocumento.toPassagem` (aninha→achata, `:39`),
`model/mappers/PassagemDadosPassagemMapper.kt:30` (resolve empresa por id, calcula tarifa/total,
`situacao = StatusPassagem.de(status)?.rotulo()`), `model/mappers/BalancoPassagensMapper.kt` (faturamento
por `navioId`).

## 7. Autorização — dois eixos (ADR-0010/0011/0012)

A política única (`PermissoesUsuario`) é a fonte de verdade Kotlin, **espelhada** nas regras Firestore. A
Passagem tem dois eixos independentes:

- **Editar conteúdo do bilhete** — por posse (`ehDono`, via `funcionarioId == uid`) ou cargo gestor
  (`podeEditarQualquerPassagem`). Cobre criar, editar, deletar e a aresta `A_EMITIR→EMITIDA`.
- **Confirmar embarque** — eixo **novo** do ADR-0012: `podeConfirmarEmbarque(cargo)` = **qualquer cargo
  conhecido** (embarque é ação de doca; quem está lá valida, mesmo sem ter vendido). Explicitamente
  **não** colapsado em editar-qualquer.

`model/operacoes/PermissoesUsuario.kt:20` (cargos: `ADM, DIRETOR, COLABORADOR_MASTER, OPERADOR`).

## 8. A FSM em três lugares — dever de paridade

A mesma matriz de transições é imposta em três camadas, que precisam mudar juntas:

| Camada | Onde | O que impõe |
|---|---|---|
| **Domínio** | `StatusPassagem.kt` (`proximos`) | arestas legais, terminal, fail-closed |
| **Aplicação** | `PassagemFirestoreRepository.transicionar` / `confirmarEmbarque` | idempotência, carimbo, espelho Room+Firestore |
| **Servidor** | `firestore.rules` → `transicaoStatusLegal()` / `ehConfirmacaoEmbarque()` | fronteira real: sem retrocesso, sem pulo, sem forjar autoria, sem piggyback |

No servidor, `ehConfirmacaoEmbarque()` endurece o check-in: exige `EMITIDA→EMBARCADA` **tocando só os 4
campos** (`hasOnly(['status','embarcadaPorId','embarcadaPor','embarcadaEm'])`) e carimbando o **próprio
uid** (`embarcadaPorId == request.auth.uid`) — não dá contrabandear edição de conteúdo por não-dono nem
forjar quem embarcou. `funcionarioId` é sempre imutável. Grafia legada normalizada só na leitura
(`normStatus`, espelha `StatusPassagem.de`). `firestore.rules:42-75, 114-148`. Suíte de emulador em
`firestore-tests/rules.test.js` (bloco de confirmação de embarque).

> Consequência assumida: mudou uma aresta? muda nos três lugares. A suíte de emulador é a rede que trava
> a divergência.

## 9. Dívidas e pontos abertos

- **Navio ainda por snapshot na leitura** — `PassagemDadosPassagemMapper` resolve empresa por id mas lê
  o navio do snapshot; `navioId` está congelado, só falta consumi-lo na resolução (rename-safe).
- **Enum legado remanescente** — `Constante.Descricao.{A_EMITIR, EMITIDA}` sobrevive só como opções do
  dropdown de filtro; não é mais o tipo. `EMBARCADA` não existe lá. Duas verdades a vigiar.
- **`N+1` + `runBlocking`** — `BalancoPassagensMapper` e a persistência da busca (`obterTodasPorDataStatus`
  faz `runBlocking { dao.salvar }` por doc). Dívida de perf/threading anotada desde o ADR-0006.
- **Validação offline do embarque** — hoje exige rede (lê o doc ao vivo). Token assinado + sync posterior
  ficou como futuro.
- **`Passagem` acumula 3 papéis** (§1) — sem separação entidade Room ↔ domínio.
- **Participantes sem identidade própria** — passageiro e veículo são participantes do agregado (§1),
  mas na persistência são membros embutidos achatados na Passagem, sem `id` próprio. O vínculo
  veículo→passageiro responsável existe só por convenção de campo (`*ResponsavelRetirada` no bloco do
  veículo), não por relação modelada — e é **opcional** (pode ficar em branco, tolerando um veículo sem
  responsável nomeado). Formalizá-los como entidades-parte é um refino futuro.
- **Assento numerado** — só `acomodacao` (classe), sem poltrona.

## 10. Referências

- [ADR-0012](../adr/0012-ciclo-de-vida-passagem-e-embarque-qr.md) — ciclo de vida + embarque por QR (mãe)
- [ADR-0008](../adr/0008-relacionamentos-por-identidade.md) — relacionar por identidade
- [ADR-0010](../adr/0010-autorizacao-por-cargo.md) / [ADR-0011](../adr/0011-regras-firestore-por-cargo.md) — autorização por cargo + regras Firestore
- [ADR-0003](../adr/0003-modelo-de-memoria-do-dado.md) — modelo de memória do dado (camadas)
- [Estudo transversal de domínio](dominio-relacionamentos-e-camadas.md) — as nove entidades e as camadas