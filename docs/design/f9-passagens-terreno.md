# F9 — Passagens: o terreno, antes do plano

> **Status:** **aberto** — mapeamento do código como ele está em `6b11f34` (2026-08-11), com o faseamento
> proposto no §7 e as perguntas de decisão no §8. Nada aqui foi decidido.
>
> Conversa com o [ADR-0018](../adr/0018-agregado-passagem-participantes-modo-e-lancamentos.md) (o agregado
> novo e o plano de migração dele), o [ADR-0022](../adr/0022-painel-da-empresa-e-fases.md) (a F9 como fase),
> o [ADR-0017](../adr/0017-eixo-de-storage-firestore-only.md) (a F5 dele *é* a Passagem saindo do Room), o
> [estudo do agregado](dominio-passagem.md) e o [estudo do form](form-passagem-validacao-exibicao.md).

## 1. O que a F9 recebeu para fazer

Duas fontes, e elas não coincidem:

- o **ADR-0022** define a fase como *"emissão sobre a viagem, e a revitalização do que já existe (ciclo de
  vida, QR, bilhete)"* — e ganhou, na virada da F8, a **ocupação** que a tabela de fases punha na F8 e que
  não podia ser entregue lá, porque contar ocupação é contar bilhete;
- o **ADR-0018** tem um **plano de migração próprio, em oito passos** (F1, F1b, F2…F7), que reescreve o
  agregado inteiro: participantes com identidade, pools `Cliente` e `Veiculo`, lançamentos de pagamento,
  numeração por ocorrência, carimbo como sub-objeto e a emissão por etapas.

A F9 é o encontro dos dois, e **eles não têm o mesmo tamanho**. O §6 propõe onde cortar; o §7 propõe a
ordem. Antes disso, o terreno.

## 2. O tamanho da ilha

Tudo medido, não estimado:

| Medida | Valor |
|---|---|
| Arquivos de produção que tocam passagem/bilhete/contagem/embarque | **75** |
| Linhas neles | **7.791** |
| Testes JVM congelados fora do escopo (`@Category(ForaDoEscopo)`) | **156**, em **22** classes — a suíte completa tem 672 em 76; a de escopo, 516 em 54 |
| Casos da suíte de emulador em `skipped` (todos de `passagens`) | **26** de 129 |
| Tabelas do Room (v7) que são da passagem | **4 de 6** — `Passagem`, `ContadorBilhete`, `PassagemDigital`, `RascunhoPassagemEntity` |
| Seções do menu que faltam revitalizar | **1** — `PASSAGEM` |

A última linha é a que dá a dimensão certa: `SecaoMenu.PASSAGEM` **já está** em `secoesDa(AGENCIAMENTO)`
(`MenuDaAtuacao.kt:69`). O que a esconde é a interseção com `SECOES_REVITALIZADAS`
(`EscopoRevitalizado.kt:40-57`), onde ela é a única ausente. **A F9 é a fatia que apaga o andaime** — e é a
única fase cujo último passo é uma linha.

E é por isso que este é o maior bloco de **código escuro** do app: 7.791 linhas que compilam, que têm teste
escrito, e que nenhum usuário alcança. Código escuro não avisa quando apodrece — a F8.0 descobriu isso ao
demolir a Viagem-trecho, que também era inalcançável.

Uma boa notícia medida hoje: a suíte **completa** (`-PsuiteCompleta`) passa — **672 testes, 0 falhas**. Os
156 congelados não estão vermelhos esperando alguém; estão verdes contra um modelo que mudou por baixo
deles. É diferente, e é pior de um jeito específico: eles **provam o comportamento antigo**, e parte deles
vai ter de ser reescrita justamente porque passa.

## 3. O terreno, camada por camada

### 3.1 O domínio: a `Passagem` é uma tabela

`domain/passagem/Passagem.kt` importa `androidx.room` nas linhas 3-6 e é declarada `@Entity` na 8. É a
**única entidade viva que ainda mistura domínio e persistência** — Empresa, Embarcação, Localidade, Porto,
Funcionário, Rota e Viagem já saíram. São **49 campos planos** e três derivados `@Ignore`, e a planura é o
que o ADR-0018 D1 desfaz:

| O que está plano | Linhas | O que o ADR-0018 quer |
|---|---|---|
| passageiros 1, 2 e 3, quatro campos cada | 61-72 | participantes com **chave** (`clienteId`) + valores congelados (D1) |
| veículo (tipo, modelo, placa, cor, cilindrada) | 76-81 | pool `Veiculo` por placa (D5) |
| responsável pela retirada | 73-75 | participante opcional, mesmo par id+snapshot |
| quatro colunas de pagamento (`valorPix`, `valorDinheiro`, `valorDebito`, `valorCredito`) | 49-52 | **lançamentos** (D11) |
| carimbo de embarque em três campos com default `""` | 89-91 | sub-objeto ausente/presente (D14) |
| `tarifaBase` | 56 | a base **inferida**, não congelada de uma tabela que morreu |

Dois detalhes que só aparecem lendo o arquivo: o passageiro 3 usa `tipoDocumentoPassageiro3` (linha 70)
enquanto 1 e 2 usam `documentoPassageiroN` (62, 66) — **o mesmo fato com dois nomes**, e é o tipo de
divergência que a serialização por nome de campo transforma em bug silencioso; e `agenciaId` (linha 48) já
está gravado, plantado na F7 **antes de ter leitor**, exatamente para que o recorte por empresa da F9
encontrasse id nos bilhetes de antes dela.

Os tipos do ADR-0018 D6/D7 (`ModoPassagem`, `ClasseVeiculo`) **existem e são testados** — mas a `Passagem`
persiste `String`, e há código comparando contra `Constante.Descricao.*`
(`ContagemPassagensMapper.kt:98-117`, `ValidacaoVeiculo.kt:54`). O `Catalogo` morreu (ADR-0020 D1) e a
tabela `Constante` continua no Room: **o tipo entrou, a persistência não seguiu**.

### 3.2 Os dados: dois donos e um espelho

`PassagemFirestoreRepository.kt` (272 linhas) é o último repositório no regime antigo:

- **o Room é fonte, não cache**: `salvar` grava no DAO e só depois transmite (:88-100), `obterPorId` lê **do
  DAO** (:128) e `obterTodasPorDataStatus` espelha o resultado da query (:206). A leitura ao vivo existe só
  para o embarque (:235), e ali está escrito por quê: o QR pode chegar num aparelho que nunca viu o bilhete;
- **documento tipado**, `PassagemDocumento` + `toObject<>` (:201, :237) — contra a fronteira em `Map` do
  ADR-0019;
- **um `runBlocking`** dentro do fluxo de emissão (:116), atualizando o contador;
- **`deletar` físico** (:130-139), e a regra do servidor **permite** (`firestore.rules:414`). É exatamente o
  que o ADR-0018 D17/D18 proíbe: apagar leva embora o fato de que o bilhete existiu;
- **o contador é um documento global** `passagens/contador` (:276-279) espelhado na tabela `ContadorBilhete`
  — contra a numeração **por ocorrência** do D10;
- **o recorte por agência é por nome** (:197, `whereEqualTo("agencia", …)`), com o `agenciaId` já gravado ao
  lado e sem uso. O KDoc do método (:174-181) documenta o próprio limite: o isolamento é de UI, o servidor
  não recorta;
- **`getListaNome()` devolve lista vazia** (:168-170) — a cova que o ADR-0018 D2/D3 preenche com o pool de
  clientes. Hoje o autocomplete de nome é uma promessa que retorna `emptyList()`.

### 3.3 A emissão: a tela existe, o preço não

Este é **o achado que decide a fase**. A tela (`FormPassagemScreen.kt`, 380 linhas) e o helper
(`FormPassagemHelper.kt`, 466) estão de pé, mas a base da tarifa vem de `statePassagem.tarifasViagem` — e
**nada mais preenche esse mapa**, porque a tabela cadastrada morreu no ADR-0016 §7.2. O próprio código diz
onde vai dar (`FormPassagemHelper.kt:458-461`):

> *"isto resolve sempre `null` para quem não é moto, e a guarda a jusante bloqueia — que é o comportamento
> correto para um preço que não existe."*

Ou seja: **hoje a emissão não fecha** para nenhum caso que não seja moto (a moto tem regra própria por
cilindrada). Não é bug — é *fail-closed* deliberado, deixado assim pela F8.0. Mas significa que **nenhuma
fatia da F9 entrega uma passagem emitida antes de o preço ter fonte**, e a fonte é decisão de negócio (§5).

### 3.4 O snapshot da viagem perdeu a fonte

O bilhete congela nomes — empresa, embarcação, origem, destino, data, hora. Eles vinham do
`ViagemDadosViagemMapper` sobre a Viagem-trecho, que a F8.0 demoliu. Os dois pontos estão marcados no
código (`FormPassagemHelper.kt:307-313`, `FormPassagemViewModel.kt:142-146`), com a forma nova já escrita:
os nomes passam a sair de **Viagem → Rota → Portos → Localidades**, mais a **Embarcação**.

E há uma peça que a F8.4 entregou e que muda o que a tela seleciona: a **`ViagemSemana`**. A passagem não é
emitida sobre a *viagem* ("terça às 18h"), é emitida sobre a **ocorrência** ("terça, 12 de agosto, às
18h") — e essa distinção é a chave da numeração, da ocupação e do balanço (ADR-0018 D8/D9/D10).

### 3.5 Ciclo de vida, QR e bilhete: a parte que está de pé

O ADR-0012 foi entregue inteiro e **não precisa ser refeito**: `StatusPassagem` é tipo com FSM
(`A_EMITIR→EMITIDA→EMBARCADA`, irreversível), o embarque valida ao vivo e carimba autoria (:245-273), o
scanner CameraX+MLKit funciona, e a regra do servidor cobre a transição — os 26 casos de emulador que estão
`skipped` são justamente esses, escritos e verdes quando rodaram.

O que **não** está no lugar é o resíduo local: o **rascunho** vive em Room+Gson
(`RascunhoPassagemStoreRoom.kt`) e o **bilhete digital** numa tabela (`PassagemDigital`). O ADR-0017 D4 já
decidiu o destino dos dois — DataStore e galeria com nome derivado do `idPassagem` —, e eles são metade das
tabelas que sobram no banco.

### 3.6 A ocupação existe, e conta pela chave errada

`contarOcupacaoEmbarcacao` (`ContagemPassagensMapper.kt:48`) é função **pura, testada**, que conta rede,
suíte (com o bucket de 2 × 3 pessoas), camarote e veículo por classe, já contra a capacidade declarada na
`Embarcacao`. A máquina da ocupação **está pronta**. O que está errado é a chave: ela agrupa por
`embarcacaoId` (:31) sobre uma consulta por `dataViagem` **em texto** (`PassagemFirestoreRepository.kt:146`)
— e não pela ocorrência `(viagemId, data)`. Duas viagens da mesma embarcação no mesmo dia somam no mesmo
balde; e como `CANCELADA` não existe, cancelada continuaria ocupando vaga.

### 3.7 A validação é pura, e diverge da regra em três pontos

Os cinco validadores já estão no molde do ADR-0006 (puros, `(state) -> Erros`, JVM-testáveis). O que
sobrevive são os **achados de regra** do D19, e eles seguem lá:

- `modeloVeiculo` é exigido **sempre** (`ValidacaoVeiculo.kt:50`), então carreta e caminhão não passam sem
  modelo — contra o D7;
- o documento do responsável pela retirada é exigido **sempre que editável** (:37), e o KDoc do arquivo
  (:12-13) afirma o contrário. O comentário do código (:31-33) **admite a divergência e mantém o código**,
  porque é o que a suíte cobre. Está honesto, e está errado num dos dois lugares;
- `getListaNome()` vazio (§3.2).

## 4. O que não se refaz

Vale escrever, porque muda o tamanho da fase: a FSM e o embarque (§3.5), os validadores puros (§3.7), a
contagem pura (§3.6), os tipos `ModoPassagem`/`ClasseVeiculo`/`TipoPassagem`/`TipoGratuidade`/`FormaPagamento`,
as funções puras de `CalculoTarifa` (o ADR-0013 caiu na *tabela*, não nas contas) e a telemetria de emissão
(`RegistroEmissao`). A F9 é **mais reencaixe que reescrita** — o que muda é onde o dado mora, qual é a
chave e de onde vem o preço.

## 5. A pergunta que trava a fase: de onde vem o preço

O ADR-0016 §7.2 matou a tarifa **cadastrada** com um argumento de dono: *a Rota é compartilhada, e uma
entidade sem dono não tem de quem ter tarifa*. Em troca, a base passaria a ser **inferida** dos valores
praticados. Só que o **método** da inferência nunca foi decidido — e o ADR-0018 o situou no *módulo
faturamento*, que não existe. Entre os dois, a emissão ficou sem preço (§3.3).

Três formas de responder, com o que cada uma custa:

**(a) O emissor digita o valor, e a inferência é só sugestão.** A F9 anda sem depender de método nenhum.
Custo: *tarifa devida* deixa de ser calculável, e com ela o **desconto como resíduo** (ADR-0013) e a régua
*esperada × real* do balanço (ADR-0014); meia e gratuidade voltam a ser confiança no operador. É o modelo
mais barato e o que menos sustenta relatório.

**(b) Inferência mínima já na F9.** A base é o último valor praticado para `(rota, modo, tipo)`, por
agregação sobre as passagens; viagem sem histórico cai num caminho declarado — o form pede o valor, e aquele
bilhete passa a ser a semente. Custo: decidir agora um pedaço do que foi empurrado para o faturamento
(janela, mínimo de bilhetes, empate) e uma consulta a mais na abertura do form. Ganho: mantém a régua do
balanço e o desconto como resíduo.

**(c) A tarifa volta a ser cadastrada — mas da agência, não da Rota.** Vale registrar que o argumento do
§7.2 **não cobre este caso**: ele nega tarifa na entidade *sem dono*; um preço cadastrado sobre a **oferta
da agência** `(agência, rota ou viagem, modo, classe)` tem dono, e tem o dono certo — quem vende. Custo:
reabre uma decisão tomada e cria cadastro novo (coleção, regra, emulador, tela), o que empurra a F9 para
dentro do território do faturamento por outro caminho. Ganho: preço determinístico e auditável, com a
inferência virando **relatório**, não fonte.

Não há como fasear a emissão sem esta resposta: ela decide se a F9 tem uma fatia de preço pequena (a),
média (b) ou uma fatia de cadastro inteira (c).

## 6. O outro corte: revitalizar × trocar o agregado

O ADR-0022 D5 registrou como aberta a pergunta *"a Passagem da F9 é reescrita no molde ou adaptada"*. O
terreno responde metade dela: tela, validação e tipos **já estão no molde**; o que está fora são a
persistência (§3.2) e a forma do documento (§3.1). Então não é "reescrever × adaptar" no sentido de tela —
é **até onde a forma do documento muda dentro da F9**.

E aqui há uma diferença de natureza que vale separar:

- **revitalizar a emissão** = fazer a seção voltar a funcionar honestamente sobre o modelo novo (viagem,
  ocorrência, agência por id, Firestore-only, ocupação, cancelamento). Nada disso é capacidade nova;
- **os pools `Cliente` e `Veiculo`** (ADR-0018 F2/F3) e a **emissão por etapas em abas** (F7) são
  **capacidade nova**: duas coleções com PII, cada uma exigindo regra e emulador no mesmo incremento, mais
  um redesenho de tela.

Minha leitura: a F9 fica com o primeiro grupo, e o segundo vira fase própria depois — não por tamanho, e sim
porque **o pool de clientes é um cadastro, e cadastro tem fase**. Colocá-lo dentro da F9 faria a fatia mais
longa do roadmap ficar mais longa ainda, e o critério que vem funcionando desde a revitalização é *uma
entidade viva por vez*.

## 7. Faseamento proposto

Oito fatias, na ordem em que cada uma destrava a seguinte. A restrição de ordem do ADR-0018 é respeitada:
**enquanto a `Passagem` estiver no Room, cada campo novo é DDL** — por isso a saída do Room (F9.1) vem
antes de toda mudança de forma.

| Fatia | Entrega | Por que aqui |
|---|---|---|
| **F9.0** | **O reencontro com a viagem** (só leitura): o snapshot do bilhete nasce de Viagem → Rota → Portos → Localidades + Embarcação, num DTO por caso de uso (ADR-0019); a tela passa a selecionar a **`ViagemSemana`**, não a viagem | é a dívida explícita que a F8.0 deixou marcada no código, não muda persistência e é inteiramente JVM-testável |
| **F9.1** | **A Passagem sai do Room** (ADR-0017 F5): codec + coleção, fronteira em `Map`, morrem o `PassagemDao` e o espelho, sai o `runBlocking`, sai o `deletar` **e o `allow delete` da regra**. Rascunho → DataStore e bilhete digital → galeria (ADR-0017 D4) | destrava tudo o que vem depois: a partir daqui campo novo é campo, não migração. E tira o delete físico antes de a seção acender |
| **F9.2** | **O preço** — a resposta do §5, mais o fim do `ResultadoEmissao.SemTarifa` como estado normal | é o que faz a emissão **fechar**; sem ela as fatias seguintes refinam um fluxo que não conclui |
| **F9.3** | **A forma do documento** (D1/D13/D14): participantes com chave + snapshot, agência por **id** na consulta, carimbo de embarque como sub-objeto — com a regra `hasOnly` e o emulador no mesmo incremento | os 49 campos planos caem aqui, já sem custo de DDL |
| **F9.4** | **Lançamentos** (D11): as quatro colunas de pagamento viram lista, mais `criadoEm`/`alteradoEm` | mesma natureza da F9.3 e depende dela; separado porque muda o balanço |
| **F9.5** | **Ocorrência, numeração e capacidade** (D8/D9/D10), juntas: a chave `(viagemId, data)`, o contador **por ocorrência** (morrem o `passagens/contador` global e a tabela `ContadorBilhete`) e o estoque que **barra** a emissão. É aqui que entra a **ocupação herdada da F8** | o ADR-0018 já pede as três juntas: contador e estoque são o mesmo problema de corrida |
| **F9.6** | **Cancelamento** (D17/D18): `CANCELADA` na FSM, política de quem cancela, regra + emulador, e canceladas fora de ocupação, cota e receita | depende da F9.5 para "não ocupa"; e a urgência que o ADR-0018 lhe dava é menor do que parecia — hoje a seção está escura, então **nenhum cancelamento está apagando histórico agora** |
| **F9.7** | **A seção acende**: `PASSAGEM` em `SECOES_REVITALIZADAS`, testes de tela no aparelho, e as 22 classes congeladas voltam à suíte de escopo | é a definição de pronto da fase inteira, e a fatia mais curta: uma linha de andaime e a rede de tela |

**Fora da F9, por decisão a tomar (§6):** pools `Cliente` e `Veiculo` (ADR-0018 F2/F3) e a emissão por
etapas (F7).

**Um efeito colateral que vale notar:** ao fim da F9.1, o Room fica com **`Usuario` e `Constante`** — e as
duas são resíduo de coisas já decididas (o `Usuario` é do Firestore desde a F6; `Constante` é a tabela do
catálogo que o ADR-0020 matou). A **F6 do ADR-0017** (remover o Room) passa a estar a duas remoções de
distância, e é a primeira vez que isso é verdade.

## 8. Perguntas ao analista

1. **De onde vem o preço** (§5) — (a) digitado, (b) inferência mínima na F9, (c) tarifa cadastrada da
   agência? É a que trava o faseamento: ela dimensiona a F9.2 e decide se o balanço mantém a régua.
2. **Onde cortar a fase** (§6) — a F9 fica na revitalização (as oito fatias) e os pools `Cliente`/`Veiculo`
   + abas viram fase própria depois? Ou algum deles entra dentro da F9?
3. **A ordem proposta no §7 serve?** O ponto em que ela é discutível é a F9.6 (cancelamento) tão tarde: o
   ADR-0018 a queria cedo, e o argumento para adiá-la é que a seção escura torna o dano hipotético.