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
| [0016](0016-dominio-da-plataforma.md) | Domínio da plataforma | **vigente · parcial** | **o `Catalogo` não nasce** — ADR-0020 D1: caem §3 inteiro, a exceção do tipo de embarcação (§8), a coleção `catalogo/` do mapa (§4), o catálogo embutido na `Localidade` (§5), a linha "Catálogo — só `ADM`" (§6) e a **F1** do plano. O eixo, o critério de colocação e as 9 rodadas seguem |
| [0017](0017-eixo-de-storage-firestore-only.md) | Firestore-only | **vigente · em execução** | o **piloto** deixa de ser `Catalogo` e passa a ser **Empresa** — ADR-0020 D10. F1 vira "coleção que *perde* o espelho", não "que nasce sem". **Empresa (schema v3) e Embarcação (v4) já saíram do Room**; o CRUD comum virou `ColecaoFirestore<T>` + `CodecFirestore<T>` — cada entidade nova declara um codec e compõe |
| [0018](0018-agregado-passagem-participantes-modo-e-lancamentos.md) | O agregado Passagem | **vigente** · D6/D7 já em código | **D6 (`ModoPassagem`) e D7 (`ClasseVeiculo`) foram implementados** junto do ADR-0020, antes das fases do próprio 0018 — os tipos vieram primeiro porque o catálogo dependia deles. O `forma` do lançamento (D11) fica confirmado como tipo (ADR-0020 D3), sem código ainda |
| [0019](0019-camada-de-dados-dinamica-e-dto-por-caso-de-uso.md) | `Map` na fronteira, DTO por caso de uso | **vigente · parcial** | a **F1** deixa de ser `Catalogo` e passa a ser Empresa — ADR-0020 D10. O regime não muda. Realiza o *passo 2* que o ADR-0003 previu |
| [0020](0020-fim-do-catalogo-e-o-contexto-do-painel.md) | O fim do Catálogo; o painel deriva da atuação | **vigente** · F1 e F2 feitas | o **D2 foi emendado** na execução (a máscara do CPF esconde os 6 primeiros dígitos, não as pontas). **F2 fechada em 2026-08-03**: o `SeedFirestore` foi removido. O rename `Navio` → **`Embarcacao`**, que o ADR adiava, foi executado em 2026-08-04 e foi até a fronteira (coleção `embarcacoes`, campo `embarcacaoIds`); a seção do menu chama-se **Flotilha** |
| [0021](0021-usuarios-da-plataforma-adm-only.md) | Usuários da plataforma (`ADM`-only) | **direção · FORA DO MVP** (D0) | **não implementar**: o cadastro no console vira **princípio** — a administração da plataforma vive fora do app, somando P2.2c + anti-escalonamento + fim do seed + ADR-0016 §10. D1–D4 valem como desenho de quando a seção nascer: primeira divergência entre `ADM` e `GESTOR`, só leitura, e `allow read` de `users` restrito |

---

## O que mudou de nome ou de dono (o vocabulário)

A maior fonte de confusão ao ler um ADR antigo não é a decisão — é a **palavra**. Estas trocaram de sentido:

| Onde se lê… | Hoje é… | Desde |
|---|---|---|
| `Agente` (entidade) | `Funcionario`; e o "agente do bilhete" é o **emissor** (`funcionarioId`) | ADR-0015 |
| `DIRETOR`, `COLABORADOR_MASTER`, `OPERADOR` (como cargos) | **papel** `ADM`/`GESTOR`/`OPERADOR` × **cargo** `SUPERVISOR`/`AGENTE` | ADR-0015 |
| `agencia` como texto do formulário | derivada do **emissor**, e por **id** | ADR-0015 P2.3 · ADR-0018 D13 |
| `Constante` (a tabela de rótulos) | **tipos de domínio** — o `Catalogo` **não chega a nascer**. A palavra volta a significar *invariante de sistema* (extensão de arquivo, MIME) | ADR-0020 D1 |
| `Constante.Categoria.DOCUMENTO` / `PAGAMENTO` / `TIPO_EMBARCACAO` / `MUNICIPIO`+UF | `TipoDocumento` · `FormaPagamento` · `TipoEmbarcacao` · `Localidade`+`Uf` | ADR-0020 D2/D3/D4/D6 |
| `ACOMODACAO` (catálogo) + `isVeiculoChecked` + `CATEGORIA_PASSAGEM` | **`ModoPassagem`** — um eixo de quatro valores (rede/suíte/camarote/veículo) | ADR-0018 D6, implementado em `10dc514` |
| `Constante.Categoria.VEICULO` | **`ClasseVeiculo`** | ADR-0018 D7, implementado em `5580b48` |
| `Navio` (a entidade) | **`Embarcacao`** — decidido, **rename ainda não feito**: entra com a estrutura de embarcações | ADR-0020 D4 |
| "atuação é categoria do catálogo" | `Atuacao` é **tipo**; a atuação *da empresa* continua **cadastrada** (`atuacoes/{ATUACAO}`) | ADR-0020 D5 |
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

**ADR-0019 — a camada de dados.** Fechou o *passo 2* do ADR-0003, que estava aberto desde o começo: a
fronteira vira `Map` e o DTO passa a ser por **caso de uso**. E trouxe o método que governa o resto do
roadmap: **da tela nascem as fronteiras e as camadas**, nunca o contrário.

**ADR-0018 — o agregado Passagem.** Deu identidade aos participantes (pools `Cliente` e `Veiculo`), tipou o
**modo**, pôs a capacidade no navio, fixou a numeração por ocorrência, trocou os quatro campos de pagamento
por **lançamentos** e transformou o cancelamento em estado, porque **manter histórico é prioridade**.

## O que está esperando decisão

- **O método da inferência tarifária** — janela, mínimo de bilhetes, viagem sem histórico, cálculo na
  leitura × materializado. Situado no **módulo faturamento** (ADR-0018, *o que não decide*).
- **Se o DTO carrega tipo ou `String` formatada** (ADR-0019, *o que não decide*) — hoje o mapper formata
  tudo, e a inferência tarifária vai pedir número.
- **O módulo faturamento** — conciliação, taxa e prazo, conta corrente do pagador, estorno, fechamento de
  caixa.

## Como escrever o próximo

1. O estudo vem antes (`docs/design/`), mapeando o código como está — com arquivo e linha.
2. O ADR registra **o que foi decidido e por quê**, não o que seria bonito.
3. Superou algo? Diga **onde** — seção e decisão —, nunca "supera o ADR-XX" inteiro.
4. Atualize **este índice** na mesma leva. Um índice que envelhece é pior que nenhum: ele mente com ar de
   autoridade. *(Foi o que aconteceu com a lista de pontos abertos do ADR-0016, que ficou para trás entre
   rodadas e induziu a leituras erradas em 2026-08-01.)*