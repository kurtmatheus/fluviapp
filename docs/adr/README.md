# Decisões de arquitetura — índice de vigência

Cada arquivo desta pasta é um **ADR**: uma decisão tomada, com o contexto que a justificou e as
consequências assumidas. ADR não se apaga e não se reescreve quando muda de ideia — **escreve-se outro**, e
o antigo passa a valer como história. Este índice existe para responder, sem abrir os dezessete arquivos,
**o que ainda está valendo e o que deixou de valer**.

> Regra de precedência: **ADR vence estudo** (`docs/design/`), e **ADR mais novo vence ADR mais velho** no
> ponto em que se cruzam — nunca no documento inteiro. Por isso a coluna *o que caiu* é específica: quase
> nenhum ADR foi superado por completo.

**Revisado em 2026-08-01**, contra as últimas revisões do domínio: o
[ADR-0016](0016-dominio-da-plataforma.md) (plataforma, 9 rodadas), o
[ADR-0017](0017-eixo-de-storage-firestore-only.md) (storage) e o
[ADR-0018](0018-agregado-passagem-participantes-modo-e-lancamentos.md) (agregado Passagem).

---

## Quadro geral

| Estado | Significa |
|---|---|
| **vigente** | vale inteiro |
| **vigente ·  parcial** | o essencial vale; um ponto específico caiu — está dito abaixo |
| **dormente** | a decisão continua correta, mas **não será construída** |
| **superada** | não vale mais; fica como história |

| ADR | Do que trata | Estado | O que caiu, e por quem |
|---|---|---|---|
| [0002](0002-capability-forma-pagamento.md) | Capability de forma de pagamento | **superada** | inteiro — ADR-0015 §4a: todo emissor escolhe a forma |
| [0003](0003-modelo-de-memoria-do-dado.md) | Modelo de memória (Room espelha Firestore) | **vigente · parcial** | o nível *cacheada* deixa de ser o Room — ADR-0017. A camada **entidade / DTO / documento** foi refinada pelo ADR-0016 (7ª rodada) |
| [0004](0004-snapshot-e-observabilidade-emissao.md) | Rascunho local + observabilidade da emissão | **vigente · parcial** | o rascunho sai do Room e vai para o DataStore — ADR-0017 D4 |
| [0005](0005-autenticacao-sessao-firebase-datastore.md) | Sessão Firebase + DataStore | **vigente** | — (o provisionamento fechado veio no ADR-0015 §2.1; o bootstrap do 1º ADM, no ADR-0016 §10) |
| [0006](0006-molde-de-cadastro.md) | Molde de cadastro | **vigente** | — é a convenção viva de todo form |
| [0007](0007-observabilidade-cadastros.md) | Telemetria dos cadastros | **vigente** | — |
| [0008](0008-relacionamentos-por-identidade.md) | Id para relacionar × valor para lembrar | **vigente** | — e **estendido**: o ADR-0018 D1 aplica o mesmo par aos participantes da passagem |
| [0009](0009-sincronizacao-reativa-firestore-room.md) | Pipeline reativo único | **vigente · parcial** | o **destino** muda: o DAO deixa de ser a fonte reativa e vira `StateFlow` — ADR-0017 D1. Ciclo de vida, porta `FonteSnapshots` e telemetria seguem |
| [0010](0010-autorizacao-por-cargo.md) | Política única de autorização | **vigente · parcial** | os cargos foram **renomeados** (ADR-0015) e a política ganhou a **atuação** como terceira coordenada (ADR-0016, 8ª rodada) |
| [0011](0011-regras-firestore-por-cargo.md) | Regras no servidor | **vigente** | — cresce com cada coleção nova (catálogo, clientes, veículos, unicidade de rota/viagem) |
| [0012](0012-ciclo-de-vida-passagem-e-embarque-qr.md) | FSM da passagem + embarque por QR | **vigente · parcial** | **cancelar deixa de ser *delete* físico** e vira estado — ADR-0018 D17. O carimbo de embarque vira sub-objeto — D14 |
| [0013](0013-tabela-de-tarifa-e-tipo-tarifario.md) | Tarifa tabelada e tipo tarifário | **dormente (a tabela) · vigente (as funções)** | a **tabela cadastrada não será construída** e `SemTarifa` morre — ADR-0016 §7.2. As funções puras vivem: muda a **fonte** da base |
| [0014](0014-balanco-financeiro-da-travessia.md) | Balanço financeiro | **vigente · parcial** | a **régua** muda (esperada vem da inferência, não do tabelado — ADR-0016 §7.2); agrega por **ocorrência** e exclui canceladas — ADR-0018 D9/D18 |
| [0015](0015-rework-agente-equipe.md) | Equipe, agência, cargo | **vigente · parcial** | o cargo passa a ser **por vínculo `(empresa, atuação)`**, não por pessoa — ADR-0016 (8ª rodada) |
| [0016](0016-dominio-da-plataforma.md) | Domínio da plataforma | **vigente** | — 9 rodadas; **sem pontos abertos** desde 2026-08-01 |
| [0017](0017-eixo-de-storage-firestore-only.md) | Firestore-only | **vigente** | — |
| [0018](0018-agregado-passagem-participantes-modo-e-lancamentos.md) | O agregado Passagem | **vigente** | — |

---

## O que mudou de nome ou de dono (o vocabulário)

A maior fonte de confusão ao ler um ADR antigo não é a decisão — é a **palavra**. Estas trocaram de sentido:

| Onde se lê… | Hoje é… | Desde |
|---|---|---|
| `Agente` (entidade) | `Funcionario`; e o "agente do bilhete" é o **emissor** (`funcionarioId`) | ADR-0015 |
| `DIRETOR`, `COLABORADOR_MASTER`, `OPERADOR` (como cargos) | **papel** `ADM`/`GESTOR`/`OPERADOR` × **cargo** `SUPERVISOR`/`AGENTE` | ADR-0015 |
| `agencia` como texto do formulário | derivada do **emissor**, e por **id** | ADR-0015 P2.3 · ADR-0018 D13 |
| `Constante` | `Catalogo` (e `IObjetoSimplificado` fica só nele) | ADR-0016 §3 |
| `Viagem` (a entidade antiga) | **Rota** (o onde) + **Viagem** (o quando e em quê, atômica) + **ocorrência** `(viagemId, data)` | ADR-0016 §7.1 |
| `Trecho` | **dissolvido** — o par de cidades é derivável dos portos | ADR-0016 (7ª rodada) |
| `model/` (pacote) | `domain/` | rename de 2026-07-31 |
| Room como *datasource* | cache do SDK + `StateFlow` por coleção | ADR-0017 |
| `acomodacao` + `isVeiculoChecked` | **modo** da passagem, um eixo de quatro valores exclusivos | ADR-0018 D6 |
| tarifa **cadastrada** | tarifa **observada** (inferida por agregação) | ADR-0016 §7.2 |

## As três revisões que mais reescreveram o passado

**ADR-0016 — o domínio da plataforma.** Trocou o app de uma empresa por uma plataforma multi-empresa e
multi-segmento. Efeito colateral maior: **o seed morre** e o painel administrativo vira a porta de entrada
do dado; **rota e viagem viram capacidades compartilhadas sem dono**; e a **tarifa cadastrada adormece**,
porque uma entidade sem dono não tem de quem ter tarifa.

**ADR-0017 — Firestore-only.** Tirou o Room do caminho, e com isso **destravou** o ADR-0016 (dois pontos
abertos deixaram de existir) e barateou o ADR-0018 (o achatamento da Passagem perde a razão de ser).

**ADR-0018 — o agregado Passagem.** Deu identidade aos participantes (pools `Cliente` e `Veiculo`), tipou o
**modo**, pôs a capacidade no navio, fixou a numeração por ocorrência, trocou os quatro campos de pagamento
por **lançamentos** e transformou o cancelamento em estado, porque **manter histórico é prioridade**.

## O que está esperando decisão

- **O método da inferência tarifária** — janela, mínimo de bilhetes, viagem sem histórico, cálculo na
  leitura × materializado. Situado no **módulo faturamento** (ADR-0018, *o que não decide*).
- **ADR da camada de dados dinâmica** — **decidido em 2026-08-02, falta escrever**: DTO **por caso de uso**,
  e as classes `[Entidade]Documento` saem dos repositórios em favor de **`Map` na fronteira** (elas ficam
  como documentação da estrutura). Base medida no
  [estudo](../design/dto-por-entidade-ou-caso-de-uso.md) §7 — é o "passo 2" que o ADR-0003 previu e não
  decidiu.
- **O módulo faturamento** — conciliação, taxa e prazo, conta corrente do pagador, estorno, fechamento de
  caixa.

## Como escrever o próximo

1. O estudo vem antes (`docs/design/`), mapeando o código como está — com arquivo e linha.
2. O ADR registra **o que foi decidido e por quê**, não o que seria bonito.
3. Superou algo? Diga **onde** — seção e decisão —, nunca "supera o ADR-XX" inteiro.
4. Atualize **este índice** na mesma leva. Um índice que envelhece é pior que nenhum: ele mente com ar de
   autoridade. *(Foi o que aconteceu com a lista de pontos abertos do ADR-0016, que ficou para trás entre
   rodadas e induziu a leituras erradas em 2026-08-01.)*