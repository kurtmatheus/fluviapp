# ADR-0020: O fim do Catálogo — vocabulário é tipo, e o painel carrega o contexto

**Status:** Aceita · **F1 feita, F2 parcial** (`2026-08-03`) — ver [§ Estado da execução](#estado-da-execução-2026-08-03).
F3–F5 pendentes.
**Supera:** [ADR-0016](0016-dominio-da-plataforma.md) §3, §8 e parte de §4/§5/§6 · o piloto do
[ADR-0017](0017-eixo-de-storage-firestore-only.md) (F1) · a F1 do
[ADR-0019](0019-camada-de-dados-dinamica-e-dto-por-caso-de-uso.md)
**Estudo:** [`docs/design/e2-painel-e-fim-do-catalogo.md`](../design/e2-painel-e-fim-do-catalogo.md) — a
medição · [`e3-catalogo.md`](../design/e3-catalogo.md) — a prova parcial que a antecedeu

---

## Contexto

O [ADR-0016 §3](0016-dominio-da-plataforma.md) decidiu renomear `Constante` → `Catalogo` e escreveu a régua
que governa a decisão: **quem tem regra vira tipo de domínio; quem é só rótulo vira linha de catálogo.**

A régua está certa. O que não se fez foi aplicá-la até o fim — e o preço apareceu como **três exceções
nomeadas dentro do próprio ADR-0016**:

1. §3 — `Constante.Descricao` morre, mas `Categoria` *"continua tipo fechado"* porque **o código depende
   dela**;
2. §8 — o tipo de embarcação é *"catálogo **e** tem comportamento"*, resolvido com *"a lista é catálogo, a
   capacidade é código"* e a consequência assumida de que um valor novo **nasce inerte**;
3. §5 — a `Localidade` nasce com **dois `Catalogo` embutidos**, mais um parágrafo justificando embutir
   `id` + `descricao` para *"reresolver contra o catálogo se a grafia mudar"*.

Três exceções numa decisão só é o modelo avisando que a categoria não fecha. O
[mapa da E3](../design/e3-catalogo.md) mediu o resto e concluiu que *"some quase tudo"*, deixando `DOCUMENTO`
como *"rótulo puro, sem regra"* e `PAGAMENTO` em aberto.

**`DOCUMENTO` não é rótulo puro, e o código prova há muito tempo:**

```kotlin
// extensions/UtilExtensions.kt:10-20
fun visualTransformation(tipoDocumento: String) =
    when (tipoDocumento) {
        Constante.Descricao.CPF.name -> CpfVisualTransformation()
        Constante.Descricao.CNPJ.name -> CnpjVisualTransformation()
        Constante.Descricao.PASSAPORTE.name -> PassaporteVisualTransformation()
        else -> VisualTransformation.None
    }
```

Dois `when` sobre valores de catálogo decidindo **máscara** e **teclado** — e um item novo cadastrado pelo
administrador cai no `else`: renderiza sem máscara, com teclado numérico, **em silêncio**. O catálogo promete
uma extensibilidade que o código não honra, e falha *fail-open* — o oposto do que o ADR-0010 e o próprio §8
estabeleceram.

`domain/catalogo/Catalogo.kt` **nunca chegou a ser escrito**. Este é o momento mais barato que existirá para
corrigir a categoria em vez de instanciá-la.

## Decisão

### D1 — O `Catalogo` não nasce; a coleção não existe

Aplicada sem exceção, a régua do ADR-0016 §3 não deixa nenhuma categoria de pé:

| Categoria | Destino | Por quê |
|---|---|---|
| `STATUS_PASSAGEM`, `TIPO_PASSAGEM`, `GRATUIDADE` | já eram tipo | ADR-0012, ADR-0013 |
| `ACOMODACAO`, `CATEGORIA_PASSAGEM`, `VEICULO` | `ModoPassagem`, `ClasseVeiculo` | ADR-0018 D6/D7 |
| `DOCUMENTO` | **`TipoDocumento`** | máscara, teclado, validação e exibição — **D2** |
| `PAGAMENTO` | **`FormaPagamento`** | é do lançamento — **D3** |
| `TIPO_EMBARCACAO` | **`TipoEmbarcacao`** | decide o que a embarcação carrega — **D4** |
| `ATUACAO` | **`Atuacao`** | id de documento, eixo de permissão e seletor de menu — **D5** |
| `MUNICIPIO` / `UF` | **`Localidade`** + enum `Uf` | **D6** |

**Não sobra linha.** O `Catalogo` não seria um `Constante` menor — seria uma coleção sem documentos, um CRUD
sem o que criar, uma seção de menu com tela vazia e uma regra de servidor guardando nada.

**A regra que substitui a régua do §3, e é o núcleo deste ADR:**

> **Vocabulário que o código consome é *tipo*. Dado que o negócio cria é *entidade*. Não há terceira
> categoria.**

A tabela `Constante` era a terceira categoria. Cada exceção nomeada no ADR-0016 foi uma tentativa de mantê-la,
e as três apontam para o mesmo lugar: quando o código precisa falar antes de um valor significar alguma coisa,
**a lista não é a fonte — o tipo é**.

**A palavra `Constante` se liberta** e volta a significar invariante de sistema: extensão de arquivo e MIME
(`PassagemDigitalHelper.kt:47,76`), formatos, nomes de coleção. `REDE`, `CPF` e `PIX` nunca foram constantes —
eram domínio guardado numa tabela.

### D2 — `TipoDocumento` carrega máscara, teclado, validação e política de exibição

O tipo de documento passa a ser tipo de domínio, e **carrega o comportamento junto** em vez de ser a chave de
um `when` espalhado pela camada de apresentação.

**A razão não é organização de código, é LGPD.** A máscara é o ponto onde o app decide **como um identificador
pessoal aparece na tela**: formatar (`123.456.789-00`) e, onde couber, **ocultar parcialmente**
(`***.456.789-**`) na consulta, no card e no bilhete. Política de exibição de dado pessoal é regra de
tratamento — não pode morar numa linha do Firestore que um administrador edita, nem depender de a coleção ter
sincronizado.

Consequência que o desenho atual não permitia: **hoje não há validação nenhuma.** `CpfVisualTransformation` só
formata (`formatarCampoCPF`); `RG` e `CNH` não têm sequer transformação; o campo `documento` é texto livre.
Tipificar é o que cria o lugar onde a validação de dígito passa a caber — hoje ela não tem onde morar.

**O contra-argumento, e a resposta.** Documento muda por país: um dia entram `DNI`, `RUT`, `CI`. Esse dia exige
máscara, teclado e validação novos — **deploy de qualquer forma**. O catálogo só adiantaria o rótulo, que é a
parte que não serve sozinha.

### D3 — `FormaPagamento` é do lançamento

O [ADR-0018 D11](0018-agregado-passagem-participantes-modo-e-lancamentos.md) já havia percebido que, com
lançamentos `{id, forma, valor}`, *"`forma` quer ser tipo"*. Fica sendo.

O contra-argumento registrado no estudo da E3 era *"meio de pagamento novo é fato de mercado, não de código"*.
Ele não sobrevive ao PIX, que chegou trazendo QR, conciliação e liquidação imediata — tudo código. Uma linha
`VOUCHER` no catálogo dá uma entrada de dropdown e nada mais: não diz se o valor é equivalente a caixa, se
liquida no ato ou em D+30, nem como o [balanço financeiro](0014-balanco-financeiro-da-travessia.md) deve
tratá-lo.

### D4 — `TipoEmbarcacao` é da embarcação, e `Navio` vira `Embarcacao`

O §8 do ADR-0016 chamou o tipo de embarcação de *"a exceção nomeada"* — catálogo **e** comportamento — e
resolveu com *"o catálogo guarda a lista, a capacidade é código; Catamarã novo nasce só-passageiro"*. **Essa
resolução é o argumento contra si mesma.**

A razão, na formulação do analista: **não se vende veículo para uma lancha se a cadastrarmos.** O tipo não é um
rótulo que o código interpreta — é o que decide o que a embarcação carrega. Vira enum **da embarcação**, com o
conjunto de `ClasseVeiculo` admitida como propriedade sua. A tabela do §8 (F/B leva tudo; Navio leva
carro e moto; Lancha só passageiro) e o ponto de aplicação (a emissão, conforme o ajuste de 2026-08-01) não
mudam — muda de onde a regra vem.

> **`Navio` → `Embarcacao`.** *"Navio é uma semântica muito limitada."* Lancha não é navio e balsa não é
> navio; a entidade que hoje se chama `Navio` é a generalização, e o nome está errado desde o começo — como
> `Viagem` estava (ADR-0016 §7). **O rename não é desta frente:** entra quando a estrutura de embarcações for
> mexida, e o `TipoEmbarcacao` nasce dentro dela já com o nome certo. Registrado aqui para não se perder.

### D5 — `Atuacao` é tipo; a atuação **da empresa** continua sendo cadastrada

A distinção que evita ler D1 como algo que ele não diz. **A plataforma cadastra a atuação da empresa** — é o
painel que declara *que esta empresa atua assim*. O que vira tipo é o **conjunto de valores possíveis**.

| | O que é | Natureza | Quem cria |
|---|---|---|---|
| `Atuacao` | os valores que existem (`AGENCIAMENTO`, `TRANSPORTE`, `PORTUARIA_OPERACAO`, `PORTUARIA_ARRENDAMENTO`) | **tipo** — o código deriva menu, cargo e permissão deles | o deploy |
| `empresas/{id}/atuacoes/{ATUACAO}` | *esta empresa exerce esta atuação*, com `portoIds[]` / `navioIds[]` | **dado** | o painel (`ADM`/`GESTOR`) |

O ADR-0016 já tratava o valor como fechado em quatro lugares — id do documento (§4), derivação do menu (§2),
qualificação do cargo (§6.1) e o fail-closed de "valor novo não ganha painel sozinho" (§8) — contra uma linha
que o chamava de categoria de catálogo. Este ADR resolve a contradição a favor das quatro.

### D6 — `Localidade` sem catálogo embutido

`uf` vira **enum `Uf`** (27 unidades federativas, fechadas por constituição — o conjunto mais estável do
domínio). `municipio` vira **campo da própria `Localidade`**: não é rótulo de outra tabela, é *o nome desta
entidade*, e a autoridade sobre ele é o IBGE (`codigoIbge`), não o gestor.

Some junto a regra de unicidade `(categoria, descricao)` que o §3 criou só para impedir *"dois Belém em
`MUNICIPIO`"* — sem a tabela genérica, a duplicidade é impedida pela chave natural. **A `Localidade` fica mais
simples do que estava**, junto com o parágrafo do "reresolver contra o catálogo".

### D7 — `IObjetoSimplificado` sai de uso, não de existência

A forma "id + descrição" é um **value object de rótulo** legítimo, e pode voltar a ser relevante se a expansão
da plataforma trouxer catálogos que sejam *de fato* rótulo. O que morre é **o uso dela neste app**, consumido
pelas implementações e revitalizações.

A diferença importa e é registrada de propósito: não se está dizendo que a forma é errada — está-se dizendo
que **hoje não há dado com essa natureza**. Junto com o uso saem as duas armadilhas que o estudo encontrou:
`extrairPorId`/`extrairPorDescricao` usam `first { }`, que **lança** em vez de devolver `null`, e
`extrairPorDescricao` é o casamento por nome que o [ADR-0008](0008-relacionamentos-por-identidade.md) mata.

### D8 — Nenhuma superfície é `ADM`-only por enquanto

O único ponto onde `ADM` e `GESTOR` iam se separar era o CRUD do catálogo (ADR-0016 §6; ADR-0017 §7.1). Sem
catálogo:

- `ADM` e `GESTOR` **continuam idênticos em capacidade** — `PermissoesUsuario.kt:33`
  (`ehPapelPlataforma`) segue sendo a pergunta certa, e `firestore.rules:34`
  (`papel() in ['ADM','GESTOR']`) segue descrevendo o painel inteiro. **Nada a construir.**
- `ADM` fica sendo o **papel de bootstrap** provisionado no console (ADR-0016 §10) — distinto por origem, não
  por permissão.
- O critério do §6 — *"quanto mais perto o dado está da semântica do código, mais restrito é quem o
  escreve"* — **não é revogado; fica disponível**. Ele volta a ter uso quando existir dado dessa natureza.
  Inventar uma restrição para ter onde aplicá-lo seria construir a regra antes do caso.

### D9 — O painel deriva da atuação, e a splash carrega o contexto

O painel muda conforme a atuação da empresa do funcionário que logou. Para que **nenhuma informação seja
omitida enquanto o painel da empresa carrega**, a splash passa a ser uma tela de carregamento de verdade.

**Isto completa a E1.1 em vez de contradizê-la.** O comentário em `SplashScreenViewModel.kt:26-28` —
*"a splash existe para cobrir o tempo real de decidir, não para ser vista"* — continua literalmente válido: o
que a E1.1 removeu foi o `delay(Random)`, espera **artificial**; o que entra agora é espera **real**, e é a
primeira vez que existe uma.

| | Hoje | Com o painel por atuação |
|---|---|---|
| O que a splash resolve | `firebaseAuth.currentUser != null` (`SplashScreenViewModel.kt:32`) | o **contexto inteiro**: usuário → funcionário → vínculo → empresa → atuações |
| Onde o dado está | sessão persistida do Firebase, **local** | `empresas/{id}/atuacoes` — **vai à rede** |
| Custo | instantâneo | I/O real, com falha possível |

A porta já existe: `SessaoUsuario.atual()` é `suspend` e resolve `usuário → funcionarioId → funcionário` num
lugar só (ADR-0015 §8.1). **A splash não a usa** — pergunta ao `FirebaseAuth` direto. A mudança é fazê-la usar
e estender `ContextoUsuario` com o vínculo ativo (empresa + atuação). `SessaoUsuarioRoom` diz de si mesma que
*"as duas leituras são locais, não vão à rede"*: é essa frase que deixa de valer, e é ela que justifica a tela.

Três implicações que fazem parte da decisão:

1. **`SplashScreenState` ganha `Erro`.** O sealed já tem `Carregando`, hoje um estado que nunca se observa;
   passa a ser o estado normal. Sem estado de erro, falha de rede prenderia a splash em `Carregando` para
   sempre — exatamente a "informação omitida" que esta decisão existe para impedir.
2. **Multi-vínculo é onde a splash não decide sozinha.** Com **um** vínculo, ela resolve e entra; com **mais
   de um**, carrega e **apresenta a escolha** (ADR-0016, 8ª rodada: *a escolha do vínculo é no login*).
   Carregar é dela; escolher não.
3. **`ADM`/`GESTOR` não têm funcionário nem vínculo**, e isso é estado válido (`ContextoUsuario.kt:9-10`).
   Para eles a splash resolve como hoje, direto para o painel da plataforma — **o caminho rápido continua
   existindo**. O carregamento é do painel *da empresa*.

### D10 — O piloto passa a ser **Empresa**

O `Catalogo` era a F1 de três ADRs ao mesmo tempo — *"uma fatia que paga dois eixos"*. Ele foi escolhido por
ser *"a mais boba do inventário"* e *"o lugar mais barato do app"* (ADR-0017 §7.1), critério que deixou de
existir junto com a entidade. **Empresa** o substitui como piloto do Firestore-only (ADR-0017 F1) e como
primeiro DTO por caso de uso (ADR-0019 F1), e é a primeira seção depois do Painel Principal:

- **já existe** — `domain/viagem/Empresa.kt`, `EmpresaDao`, repositório, `FormEmpresaUiState`,
  `ContentEmpresaAreaForm` (com `CnpjVisualTransformation` no lugar). O crivo *"reaproveitar o que existe"*
  tem o que reaproveitar;
- **é onde a atuação nasce** (D5). O menu da E2 deriva da atuação; sem empresa cadastrada, ele deriva de nada.
  A ordem **E2 → Empresa** é autoconsistente;
- **tem domínio de verdade** — CNPJ, razão social, atuações, concessão de portos e navios: o trilho
  *domínio → dados → lógica → apresentação → regra → teste observável* passa a ser provado numa fatia que o
  **exerce**, em vez de atravessá-lo vazio.

**Trade-off nomeado:** a fatia fica mais cara. Empresa tem espelho Room a remover — o piloto do ADR-0017 deixa
de ser *"coleção que nasce sem espelho"* e passa a ser *"coleção que perde o espelho"* — e uma subcoleção a
criar. Em compensação é **representativa**: todas as coleções restantes têm espelho a remover, e nenhuma nasce
do nada. Um piloto barato que não prova o caso geral custa mais do que economiza.

## Consequências

**Ganha-se:**

- **Os seletores saem da rede.** As **16 chamadas** de `obterTodosPorCategoria` em 11 ViewModels/helpers viram
  `.entries`: sem listener, sem suspensão, sem espelho. **"Dropdown vazio" deixa de ser um estado possível** —
  hoje é o que aparece quando a coleção não sincronizou, sem erro e sem log.
- **O fail-open do documento vira fail-closed.** Não existe mais valor de documento sem máscara: o conjunto é
  o enum.
- **O domínio fecha.** O passo *domínio* da frente E2 passa a ser um diff **só de JVM**: cinco tipos
  (`TipoDocumento`, `FormaPagamento`, `TipoEmbarcacao`, `Atuacao`, `Uf`), a derivação do menu como função pura
  `Atuacao → Set<SecaoMenu>`, e a saída de `Constante`. Sem Firestore, sem Room, sem tela.
- **A E2 deixa de esperar cadastro.** *"As seções derivam da atuação"* vira função pura e testável — e o
  `acoesDe(secao)` que hoje mora no grafo (`MainScreenNavComposable:66-93`, 13 callbacks) ganha destino.

**Perde-se, e é aceito:**

- **Acrescentar um valor de vocabulário passa a exigir deploy.** É a troca central deste ADR. Ela é aceitável
  porque, em todos os casos medidos, o valor novo já exigia código para significar alguma coisa — o catálogo
  adiantava só o rótulo.
- **O piloto fica mais caro** (D10).
- **A tela de entrada fica mais lenta** (D9) — mas informada, com erro visível, em vez de rápida e
  incompleta.

**Não muda:**

- a régua do ADR-0016 §3 (é ela que produz este resultado), o critério de colocação do §4, o eixo
  parte × atuação × ativo, a política única do ADR-0010 e o regime do ADR-0019 (fronteira `Map`, DTO por caso
  de uso);
- o ponto de aplicação da regra de embarcação (a emissão) e a tabela tipo → classes (D4).

## Plano

Segue a ordem das frentes (*domínio → dados → lógica → apresentação*), e o descarte é **progressivo**: cada
fase apaga o que ela tornou obsoleto.

- **F1 — Os tipos** (JVM puro) — ✅ **FEITA** (`5580b48`, `d4ff6a5`). `TipoDocumento`, `FormaPagamento`,
  `TipoEmbarcacao`, `Atuacao`, `Uf`, mais **`ClasseVeiculo`** (ADR-0018 D7) e **`ModoPassagem`**
  (ADR-0018 D6), que não estavam nesta lista — ver §*Estado da execução*.
- **F2 — A saída de `Constante`** — ⚠️ **PARCIAL** (`c5023d7`, `10dc514`): **12 das 16** chamadas saíram e o
  form de passagem deixou de ler o catálogo. `ConstanteDao`, o repositório, o `ConstanteDocumento`, a coleção
  `constants`, a regra em `firestore.rules:183` e o `IObjetoSimplificado` (D7) **não saíram** — as quatro
  chamadas restantes são de `MUNICIPIO`, e município não vira `.entries`: vira `Localidade` (D6). ~~A fase só
  fecha com as capacidades da plataforma.~~

  **Corrigido em 2026-08-05, quando a `Localidade` nasceu e o `Constante` não morreu junto.** A frase acima
  supunha que os quatro leitores seriam *migrados* para a entidade nova. Não serão: eles vivem em
  `FormViagemViewModel`, `PesquisarViagemViewModel`, `ViagemDadosViagemMapper` e `FormFuncionarioViewModel`
  — seções fora do escopo da revitalização —, e o ADR-0016 §7 já decidiu que a **Rota referencia portos por
  id**, com o par de cidades virando *leitura sobre os portos*, não campo. O `listaMunicipios` do form de
  viagem não muda de fonte: ele **deixa de existir** quando a Rota nascer, e o mesmo vale para a lotação do
  funcionário no §6. Migrá-los agora seria escrever contra um domínio já marcado para mudar de forma.

  Então a condição real de fechamento é outra: **`Constante` morre com a Rota e a Equipe**, não com as
  capacidades. O que a `Localidade` fechou aqui foi só o que era dela — o `LoginViewModel` parou de
  sincronizar a coleção `constants`, que era a última dependência do caminho vivo.
- **F3 — A derivação do menu** (E2): `Atuacao → Set<SecaoMenu>` como função pura; `acoesDe` sai do grafo;
  `DadosBotoesMenus` perde o `onClick` (ADR-0019 §7).
- **F4 — O contexto e a splash** (D9): `ContextoUsuario` ganha o vínculo ativo; `SplashScreenViewModel` passa
  a usar `SessaoUsuario`; `SplashScreenState` ganha `Erro`; escolha de vínculo quando houver mais de um.
- **F5 — Empresa** (D10): a fatia completa — domínio → fronteira `Map` → DTO por caso de uso → tela →
  `atuacoes` como subcoleção → regra de servidor → suíte de emulador → **teste Android observável**.

`Localidade`/`Uf` (D6) entram com a fase das capacidades da plataforma; `Navio` → `Embarcacao` (D4) entra com
a estrutura de embarcações. **O contador de bilhete** não precisa de fase: ele já é substituído por
`count(passagens where viagemId = X and data = D)` (ADR-0016 §7.1, ADR-0018 D10) — não é um contador melhor, é
uma contagem, e sai com a numeração por ocorrência. Junto sai `ViagemDao.obterContagem()`, sem chamador.

## Estado da execução *(2026-08-03)*

F1 e F2 entregues; a suíte foi de **238 para 316 testes**, verde, com `assembleRelease` verde. O que a
execução acrescentou ou corrigiu em relação ao que este ADR previu:

### O que entrou além do previsto

| Peça | Por quê |
|---|---|
| **`ClasseVeiculo`** (ADR-0018 D7) | sem ela `TipoEmbarcacao` não tem o que admitir — a regra do D4 **é** um conjunto de classes |
| **`ModoPassagem`** (ADR-0018 D6) | a acomodação era a última lista de vocabulário do form de passagem; sem o tipo, `Constante` sobreviveria servindo duas categorias em vez de uma |
| **A validação ligada** | o D2 criava o *lugar* da validação; ligá-la foi decisão do analista na mesma leva, com `validarDocumento(tipo, numero)` como regra pura e mensagem própria para "inválido" × "obrigatório" |
| **`limitarDocumento()`** | ao migrar apareceu um **quarto** `when` sobre `Constante.Descricao` — o que limita o tamanho do campo —, duplicado idêntico em dois helpers |

### Emenda ao D2 — a política de ocultação do CPF

O D2 dizia que as formas de ocultação replicariam o que o app já imprimia, "para que a F2 não mude nada
visível", e registrava que valia apertá-las. **O analista apertou na mesma leva:** o CPF passa a esconder os
**6 primeiros** dígitos e mostrar os 5 últimos (`###.###.247-25`), no lugar da forma herdada, que escondia as
pontas e expunha os 6 do meio. As demais formas seguem como eram.

### Precisão sobre o D2 — o tipo é puro; o Compose fica no adaptador

O D2 diz que `TipoDocumento` "carrega máscara, teclado, validação e política de exibição". `VisualTransformation`
e `KeyboardType` são **Compose**, e pô-los no enum meteria framework dentro do domínio — exatamente a dívida
que o ADR-0019 D2 quer pagar. O tipo carrega **o formato e a política**, puros; a tradução para Compose mora
em `util/visualtransformation/DocumentoVisual.kt`. O ganho não muda: o `when` passa a ser exaustivo sobre o
enum, e o `else` silencioso deixa de existir.

### Os defeitos que a tipificação revelou

Nenhum deles era conhecido quando este ADR foi escrito. Todos são a mesma falha — **um vocabulário em duas
fontes que divergiram em silêncio** — e é a evidência mais forte a favor do D1:

1. **A acomodação nunca casava.** O catálogo semeava `"Rede"`, `"Suíte p/ 2 Pessoas"`, `"Camarote"`; o código
   comparava com `"REDE"`, `"SUITE"`, `"CAMAROTE"`. Efeito: `ehAcomodacaoRede` sempre falso — a regra "tipo
   tarifário e gratuidade só na REDE" (ADR-0013) **nunca disparava** — e `contarOcupacaoNavio` classificava
   tudo no `else`, ou seja, **rede, suíte e camarote saíam zerados na Contagem de Passagem**.
2. **O filtro de status oferecia três buscas impossíveis** (`EM TRANSITO`, `FINALIZADA`, `EM ANÁLISE`, que não
   existem na FSM do ADR-0012) e **omitia `EMBARCADA`**, que existe.
3. **A gratuidade oferecia oito opções** contra as quatro legais do ADR-0013, incluindo `CORTESIA`, que aquele
   ADR aposentou.
4. **`formatarCNPJ()` fixava `/0001-`** e ignorava os dígitos 8..11: toda filial era impressa como matriz.
5. **`extrairDocumentoFormatado` devolvia `""` no `else`**: documento de tipo desconhecido **sumia do
   bilhete**, sem erro e sem log.
6. **RG e CNH não tinham limite de digitação** — caíam no `else` do quarto `when`.

Os seis estão corrigidos. O item 1 é o que mais pesa: era regra de negócio e relatório, não cosmética.

## Alternativas consideradas

- **Manter o `Catalogo` só com `DOCUMENTO` e `PAGAMENTO`** (o desenho do estudo da E3). Rejeitada: os dois têm
  regra (D2, D3), e manter a coleção por causa deles conservaria toda a infraestrutura — repositório,
  listener, regra, espelho, DTO — para zero linha de dado.
- **Catálogo como *fallback* do enum** (o tipo manda; o catálogo acrescenta rótulos sem comportamento).
  Rejeitada: cria duas fontes para o mesmo vocabulário e reintroduz o fail-open — o valor extra apareceria no
  seletor sem máscara nem regra, que é precisamente o defeito de hoje.
- **Deixar `TIPO_EMBARCACAO` no catálogo** como o ADR-0016 §8 previa. Rejeitada por D4: um valor que só produz
  uma oferta vazia não é extensibilidade, é uma linha inerte com aparência de configuração.
- **Criar uma superfície `ADM`-only para preservar a distinção de papéis.** Rejeitada por D8: seria construir
  a regra antes do caso.
- **Manter a splash instantânea e carregar o painel dentro dele** (spinner na própria tela). Rejeitada por
  D9: o painel *é* o que deriva da atuação — montá-lo antes de saber a atuação é montar o menu errado e
  corrigi-lo depois, que é a informação "esquecida" que a decisão evita.
