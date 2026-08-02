# ADR-0019: Camada de dados dinâmica — `Map` na fronteira e DTO por caso de uso

**Status:** **Aceita (direção)** — decisões do analista em **2026-08-02**. Sem código: este ADR fixa o
regime da camada de dados e o plano; a implementação é faseada abaixo. Formaliza o
[estudo DTO por entidade × por caso de uso](../design/dto-por-entidade-ou-caso-de-uso.md) e **realiza o
"passo 2" que o [ADR-0003](0003-modelo-de-memoria-do-dado.md) previu e não decidiu** (a forma do dado,
DTO-cêntrica). Nasce do **ponto aberto 10 do [ADR-0016](0016-dominio-da-plataforma.md)**, que saiu de lá
para virar estudo próprio.

> Conversa com o [ADR-0003](0003-modelo-de-memoria-do-dado.md) (formas do dado), o
> [ADR-0009](0009-sincronizacao-reativa-firestore-room.md) (que criou a porta `FonteSnapshots` e o
> `DocumentoBruto`), o [ADR-0016](0016-dominio-da-plataforma.md) (**entidade = lei · DTO = trânsito ·
> documento = armazenamento**), o [ADR-0017](0017-eixo-de-storage-firestore-only.md) (o Room sai) e o
> [ADR-0018](0018-agregado-passagem-participantes-modo-e-lancamentos.md) (o agregado deixa de ser plano).
> Ancorado no código em `2026-08-02`.

---

## Contexto

### O que o app tem hoje

Quatro formas para o mesmo dado, e a última é a que cobra:

```
PassagemDocumento ──► Passagem ──► DadosPassagem ──► telas
  (documento, 25)      (entidade, 47)   (projeção, 58)    (5 consumidores)
```

**A projeção de tela cresceu mais que a entidade.** `DadosPassagem` tem **58 campos** e serve cinco
consumidores com necessidades muito diferentes:

| Consumidor | Usa | Desperdício |
|---|---|---|
| `ImpressaoPassagem` | 41 / 58 | 29% |
| Bilhete digital | 30 / 58 | 48% |
| Lista de resultados | 11 / 58 | 81% |
| `PassagemPreviewCard` | 10 / 58 | 83% |

**O custo não é memória — é leitura.** `PassagemDadosPassagemMapper` resolve **empresa e navio por id** para
preencher sete campos, e roda **item a item** na pesquisa (`PesquisarPassagemViewModel.kt:132`). Uma busca
de 50 bilhetes faz **100 leituras de cadastro** para preencher campos que a lista **não mostra** — ela usa
onze, e nenhum é de empresa ou navio. O `N+1` que vários ADRs registram como dívida tem aqui a causa raiz:
não é o laço, é **a projeção única**.

E um argumento que costuma decidir isso não vale aqui: **o SDK Android não expõe projeção de campos** — o
documento vem inteiro do servidor de qualquer forma.

### O padrão dinâmico já existe, e está provado

Este ADR **não inventa** a fronteira em `Map`: generaliza o que o app já faz em dois lugares.

- **Na leitura**, o ADR-0009 criou `DocumentoBruto(id, dados: Map<String, Any?>)` com acessores defensivos —
  `texto()`, `inteiro()`, `mapaDeDoubles()` —, todos **fail-closed** (ausente ou tipo errado → vazio), e o
  `DocumentoBrutoMappers.kt` já converte `Map → *Documento` **sem o `toObject` do Firestore**, em funções
  puras e testáveis. Foi feito para testar o ciclo de vida sem Firebase; virou a peça central deste ADR.
- **Na escrita**, as transições da passagem já gravam `Map` com os nomes de campo em constantes
  (`update(mapOf(FIELD_STATUS to novo.name))`, `PassagemFirestoreRepository.kt:227,261,278`).

Restam **11 usos de `toObject`** e um punhado de `set(objetoTipado)` — é essa a superfície a virar.

## Decisão

- **D1 — DTO é por caso de uso, não por entidade.** Cada consumidor recebe a projeção do que **ele** usa. A
  `DadosPassagem` de 58 campos é refatorada em projeções por consumidor; as projeções saudáveis que já
  existem (`DadosViagemCard`, `DadosContagemPassagem`, `DadosImpressora`) são o exemplo do padrão certo — uma
  por consumidor.

- **D2 — As classes `[Entidade]Documento` saem dos repositórios e, depois, deixam de existir.** No nível do
  código elas *"podiam ser só `Map`s de chave e valor que funcionariam do mesmo jeito"*. Primeiro deixam de
  ser o tipo que o repositório instancia; **quando o domínio revisado estiver implementado, elas não são mais
  necessárias — nem como documentação.** Quem documenta a estrutura é **o próprio domínio**: as entidades
  ricas (ADR-0016 §9, *entidade = lei*) e o [catálogo do domínio da plataforma](../design/dominio-da-plataforma.md),
  que descreve entidades, campos, enums e regras **na linguagem do negócio**. Manter uma segunda descrição
  da mesma estrutura, em Kotlin e sem compilador que a verifique, seria criar a segunda verdade que este
  projeto vem eliminando desde o ADR-0008.

- **D3 — A fronteira de leitura é o `Map`, via `DocumentoBruto`.** Generaliza-se a porta que já existe: o
  repositório lê `DocumentoBruto` e entrega ao mapper **do caso de uso**. Some a desserialização por
  reflexão (`toObject`) e, com ela, a instanciação de 25 campos tipados para o consumidor usar onze.

- **D4 — A escrita também é `Map`, com os nomes de campo em constantes.** É o que as transições da passagem
  já fazem. Constante nomeada em vez de literal solta é o que mantém a escrita conferível sem o compilador —
  e é o mesmo mecanismo que as regras do servidor usam (`hasOnly([...])`, ADR-0011).

- **D5 — A tipagem acontece onde o uso é conhecido.** Não se tipa "o documento"; tipa-se **o que a tela
  precisa**, no mapper daquele caso de uso, com conversão explícita e **fail-closed** — a mesma convenção
  que o domínio já usa em toda fronteira (`de(valor)` → `null` quando desconhecido, ADR-0012/0013). O
  domínio continua rico e tipado; o que fica dinâmico é **o transporte**.

- **D6 — O método: da tela nascem as fronteiras e as camadas.** *O domínio define a relação; o caso de uso
  define o uso.* Ao definir uma tela, dela derivam mapper, ViewModel, camada de dados e apresentação — nessa
  ordem, e não a partir do que o banco já tem. É *domain-driven* literal, e tem consequência de produto: **o
  painel da plataforma é o molde do painel por agência**, que é o molde dos painéis dos outros segmentos.
  Por isso o painel vem **depois** do domínio revisado, e não antes.

- **D7 — Chave ausente não é falha: é a forma que a entidade tomou.** *"A Passagem é uma entidade única que
  assume várias formas, e esse dinamismo se paga com o Firestore."* O **modo** (ADR-0018 D6) decide o que
  existe no documento: bilhete de veículo não tem passageiros 2 e 3; de rede não tem placa nem cilindrada;
  sem embarque não tem carimbo (D14 de lá). **A ausência carrega informação — e é importante para análise**:
  a forma do documento revela o modo, sem depender de um campo que o declare. Documento esparso é o
  esperado, não defeito.
  Isso separa dois casos que a primeira redação deste ADR confundia:
  - **ausência semântica** — o campo não existe porque aquela forma não o tem. É **dado**; o default
    fail-closed está certo, e a projeção de cada caso de uso simplesmente não pergunta pelo que o modo não
    tem;
  - **ausência acidental** — o campo deveria existir e o nome saiu errado. **Essa** é a que o compilador
    cobria, e é o que se endereça: **constantes de nome de campo** (D4), **teste de mapeamento** por caso de
    uso (`DocumentoBrutoMappersTest` é o molde) e o **domínio como fonte única** do que cada forma tem (D2).

- **D8 — O domínio fixado aqui é o canônico da plataforma.** O app é *mobile-first* por decisão (ADR-0017), e
  **o domínio definido neste regime vai se repetir nos outros sistemas** — inclusive no back-end
  centralizador que aquele ADR previu. Isso eleva o que D2 diz: o domínio não é documentação *do app*, é **a
  especificação da plataforma**, e a forma esparsa do D7 é parte dela. Quem for ler estas coleções amanhã
  precisa encontrar escrito que **ausência significa forma**, não dado faltando.

## Consequências

**O que se ganha**

- **O `N+1` morre por construção** — a projeção da lista não tem campo de empresa, então não há o que
  resolver. É a dívida mais antiga do app fechando por mudança de forma, não por otimização.
- **Uma tradução a menos e menos alocação por item** — o repositório para de materializar objeto tipado
  completo para o mapper descartar dois terços.
- **Coleção nova não exige classe nova.** Num domínio que ainda cresce em forma (ADR-0016: multi-empresa,
  multi-segmento, catálogo dinâmico), cada entidade nova custava um `*Documento` + mapper + manutenção. Passa
  a custar um mapper por caso de uso — e só quando houver caso de uso.
- **Some a última dependência de reflexão do Firebase na leitura**, o que já era o objetivo da porta
  `FonteSnapshots` (ADR-0009 §10).

**O que se paga**

- **Erro de nome de campo vira erro de execução**, não de compilação — só na *ausência acidental* (D7).
- **Mais mappers**, um por caso de uso, no lugar de um por entidade — é a troca deliberada: mais classes
  pequenas e específicas em vez de uma grande e genérica.
- **O domínio passa a ter obrigação de estar certo.** Some a descrição paralela em Kotlin (D2), então o
  catálogo do domínio e as entidades ricas viram a **única** fonte do que cada forma tem. Domínio
  desatualizado deixa de ser dívida de documentação e passa a ser dívida de **especificação** — para este e
  para os sistemas que vierem (D8).

**Reversibilidade.** Alta e por coleção: como cada repositório é dono do próprio caminho, dá para voltar a
`toObject` numa coleção específica sem tocar nas outras. O que não é reversível de graça é a **quebra da
`DadosPassagem`** — mas ela vai ser quebrada de qualquer forma pelo ADR-0018 (participantes viram coleção,
pagamento vira lançamentos).

## Plano de migração (faseado)

- **F1 — `Catalogo`, junto da E3 do roadmap.** A mesma fatia que já é a F1 do ADR-0016 (`Constante` →
  `Catalogo`) e a F1 do ADR-0017 (piloto Firestore-only) passa a ser **o primeiro caso de DTO por caso de uso
  com fronteira `Map`** — leitura, escrita (o CRUD do ADM) e a regra no servidor, na coleção mais simples do
  app.
- **F2 — Leitura dos demais cadastros** (Empresa, Navio, Funcionario, Viagem): trocar `toObject` por
  `DocumentoBruto` + mapper do caso de uso, uma coleção por vez.
- **F3 — Quebrar a `DadosPassagem`** nas projeções por consumidor — **lista** primeiro (é onde o `N+1`
  aparece), depois detalhe/impressão. Encontra-se com a F5/F6 do ADR-0017 e com a F6 do ADR-0018.
- **F4 — Escrita em `Map`** nos repositórios que ainda usam `set(objetoTipado)`.
- **F5 — Remover os `*Documento`** (D2), junto com o `DocumentoBrutoMappers` intermediário, quando não
  sobrar chamador — com o domínio implementado, não há o que eles ainda descrevam.

Cada fase compila e mantém a suíte verde — mesma tradição do ADR-0009 e do ADR-0017.

## O que este ADR não decide

- **Se o DTO carrega tipo ou `String` formatada.** Hoje o mapper devolve tudo formatado (inclusive dinheiro);
  a inferência tarifária (ADR-0016 §7.2) vai pedir número. É a pergunta 2 do estudo, ainda aberta.
- **Os derivados duplicados** (`ehVeiculo`, `ehRede`, `tem2Pessoas` no DTO × `temPassageiro2`, `ehVeiculo` na
  entidade) — de onde passam a vir.
- **Criar uma camada de caso de uso.** O app não tem `usecase/`; a orquestração vive em ViewModels e
  helpers. "Caso de uso" aqui significa **o consumidor concreto**, e este ADR não muda isso.
- **A forma do documento no servidor** — o que muda é como o app o lê e escreve, não o que está gravado.

## Referências

- [Estudo: DTO por entidade ou por caso de uso?](../design/dto-por-entidade-ou-caso-de-uso.md) — a medida
- [ADR-0003](0003-modelo-de-memoria-do-dado.md) — o "passo 2" que este ADR realiza
- [ADR-0009](0009-sincronizacao-reativa-firestore-room.md) — `FonteSnapshots` e `DocumentoBruto`
- [ADR-0016](0016-dominio-da-plataforma.md) — entidade = lei · DTO = trânsito · documento = armazenamento
- [ADR-0017](0017-eixo-de-storage-firestore-only.md) / [ADR-0018](0018-agregado-passagem-participantes-modo-e-lancamentos.md) — os dois eixos que este encontra
- [Roadmap do MVP](../design/mvp-roadmap.md) — as frentes E1/E2/E3