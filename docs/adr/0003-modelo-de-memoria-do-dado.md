# ADR-0003: Modelo de memória do dado — DTO-cêntrico, Room espelhando Firestore

**Status:** Aceita (direção); execução faseada, em preparação

**Contexto**

O app nasceu com três representações paralelas por entidade (Room `@Entity`, Firestore
`Documento`, REST `Request`/`Response`). O mapeamento real da camada de dados revelou:

- **São duas camadas vivas, não três.** Firestore é a **verdade / origem de sincronização**
  (repositórios usam `addSnapshotListener`); Room é o **cache de leitura local** (toda leitura
  do app vem de um DAO, nunca do Firestore direto); a UI (states Compose + `screendata`) é o
  estado **vivo de edição**.
- **Toda a família REST/Retrofit é código morto** — provida no Hilt (`RestApiModule`) mas sem
  nenhum consumidor (`ApiHandler` e os `*Service` não têm referências fora dos próprios
  arquivos). É representação paralela só no papel.
- **`Passagem` é o ponto de tensão SQL×JSON**: plana no Room (~40 colunas), aninhada no
  Firestore (`PassagemDocumento` com sub-documentos `Viagem`/`Passageiro`×3/`Veiculo`). O
  flatten/unflatten mora nos mappers (`PassagemDocumentoExtensions`, `PassagemDocumento.toPassagem`).
- `Agente` **já segue** o padrão alvo (Firestore→listener→Room→UI), com uma inconsistência:
  `salvar` grava só no Firestore e depende do round-trip do listener, enquanto `Viagem`/`Passagem`
  gravam Room+Firestore.

A visão do usuário: tratar os **DTOs como core** — o dado "viaja" entre camadas como mensagem
(pergunta→resposta: "deu certo? prossiga"), tudo por parâmetro/função/host/interface; a
**tenacidade do SQL** (schema tipado, migrations) recua e nasce o **dinamismo NoSQL/JSON**;
trazer o conceito de **snapshot/rascunho** do projeto irmão `autorizacao-servico-app`.

**Modelo de memória (importado do AS, adaptado à topologia daqui)**

Três níveis de memória do dado — *a forma como o dado é gerido É a arquitetura*:

| Nível | AS (origem) | FluviApp (esta topologia) |
|---|---|---|
| **Volátil** | `FormXxxState` em RAM | states Compose + `screendata` (`DadosPassagem`) |
| **Cacheada** | snapshot JSON (rascunho durável, não-autoritativo) | **Room** (espelho local do Firestore, durável mas não é a verdade) + **snapshot de rascunho** (a criar) |
| **Sólida** | colunas Room → backend | **Firestore** (verdade committada / origem de sync) |

Diferença-chave em relação ao AS: lá a **sólida era o Room**; aqui o Room **desceu para
cacheada** (é projeção do Firestore), e a **sólida é o Firestore**. Isso reposiciona o SQL
exatamente como o princípio prega: *SQL puro = memória (cache/consulta), nunca o substrato de
trabalho nem a verdade final*.

**Transições = mappers puros, batizados pela direção** (já existem, só precisam ser nomeados
como fronteiras): Firestore→Room `toXxx(id)`; Room→Firestore `toDocumento()`;
Room→screendata mappers de UI; (a criar) volátil→cacheada `montarRascunho()` e
cacheada→volátil `aplicarEm()` para o ciclo de rascunho.

**Decisão**

Adotar o modelo de memória como régua de design e caminhar para um núcleo **DTO-cêntrico**,
de forma **aditiva** (padrão "stack dormente" do AS: nada do legado quebra até o flip). Ordem:

1. **Normalizar o espelho** (Room ⇐ Firestore): unificar o `salvar` do `Agente` com o de
   `Viagem`/`Passagem` (gravar Room+Firestore) para que o espelhamento seja consistente e
   previsível. É a base de "preparar o Room como espelhamento do Firebase".
2. **DTO como core que viaja**: o `Documento` (JSON, aninhado, schemaless) passa a ser a forma
   canônica que atravessa as camadas; Room e screendata derivam dele por mapper. Piloto já
   feito: a capability `podeSelecionarFormaPagamento` (ADR-0002) agora viaja
   `AgenteDocumento` ↔ `Agente` ↔ domínio.
3. **Snapshot/rascunho na emissão de Passagem**: introduzir o ciclo volátil→cacheada(rascunho
   JSON)→sólida(Firestore no "emitir"), trazendo crash-safety/offline-first ao fluxo de
   passagem — o laboratório natural (é o fluxo mais rico e o único com estrutura aninhada).
4. **Aposentar o REST morto** — FEITO: removida toda a família Retrofit (`services/network/**`,
   `ApiHandler`, `RestApiModule`) e as dependências retrofit/okhttp/jackson. O app assume
   explicitamente **2 camadas vivas** (Firestore + Room). Decisão do usuário: começou com a
   possibilidade de um cliente HTTP, mas o Firestore/Firestore-query dá todo o suporte do
   processo de negócio — inclusive **agregação via query de Firestore**, sem SQL.

**Camada normalizadora (conceito nomeado pelo usuário)**

O dado vive **livre** (JSON dinâmico) tanto no app quanto no Firestore; a **normalização é sob
demanda**, feita por uma camada normalizadora *conforme a aplicação e a realidade de uso* — não
uma forma canônica imposta no schema. Corolários:

- A "normalização" que o SQL fazia em repouso (colunas, joins, agregação) passa a acontecer
  **em trânsito/na fronteira**: query de Firestore + mappers de normalização no app (os atuais
  `Dados*Mapper` são exatamente isso, e generalizam para essa camada).
- A normalização pode acontecer **fora do app**: um pool de dados do Firestore exportado e
  normalizado para relatórios (ex.: PowerBI) — o app não precisa carregar a rigidez analítica.
- Isso fecha a resposta ao "e as queries SQL que se perdem?": elas não somem, mudam de lugar —
  viram query de Firestore + normalizador de aplicação (ou análise externa).

**O trade-off (a pergunta central: "qual o trade-off?")**

Trocar **tenacidade SQL** por **dinamismo NoSQL/JSON** é trocar *integridade estrutural em
repouso* por *fluidez em trânsito*:

- **O que se ganha:** o dado vira mensagem. Adicionar/mudar um campo é livre no `Documento`
  (schemaless) — provado no piloto: `podeSelecionarFormaPagamento` entrou no `AgenteDocumento`
  de graça. Some a dupla-forma (Entity tipada + Documento + flatten/unflatten). O fluxo fica
  "pergunta→resposta": cada fronteira recebe o DTO por parâmetro, valida, e responde
  seguir/parar — acoplamento por interface/host, não por schema compartilhado.
- **O que se perde:** *"o banco me protege"*. A validação estrutural sai do schema e vira
  responsabilidade **da fronteira** (quem promove volátil→sólida). E — o custo mais concreto
  aqui — **perdem-se as consultas SQL que o app usa**: `AgenteDao.obterTodosPorAgencia`,
  `ViagemDao.obterPorCodigo`, `ContadorDao`, e a agregação do `BalancoPassagensMapper`. Com o
  dado como blob JSON, ou se filtra em memória (carrega tudo, filtra em Kotlin) ou se mantém
  poucas colunas indexadas ao lado do blob.
- **Atenuante desta topologia:** como o Room é **espelho** do Firestore, o custo de migração do
  SQL tipado é **baixo** — se preciso, uma migração destrutiva é segura (a verdade re-sincroniza
  do Firestore; só `PassagemDigital`, local-only, exigiria cuidado). Ou seja: aqui o argumento
  pró-JSON **não** é "fugir da dor de migração" (ela é barata) — é **eliminar a dupla-forma** e
  ganhar evolução livre de documento. Por isso a migração deste ADR (v1→v2 da capability) foi
  feita tipada e **não-destrutiva**: enquanto o Room for consultável, vale manter o tipo.

**Quando compensa:** o ciclo rascunho→verdade só paga o custo de complexidade se as fronteiras
de promoção forem **poucas e explícitas**. No AS era o gate de finalização. No FluviApp o
candidato é o "emitir passagem". Para as entidades de cadastro (Empresa, Navio, Constante,
Agente) que são essencialmente **read-only sincronizadas do Firestore**, o ciclo de rascunho é
over-engineering — elas só precisam do espelho consistente (passo 1), não de snapshot.

**Análise das 9 entidades (classificação no modelo)**

| Entidade | Persistência hoje | Papel no modelo | Precisa de rascunho? |
|---|---|---|---|
| **Passagem** | Room(flat)+Firestore(nested) | volátil→**rascunho**→sólida | **SIM** — lab do snapshot (fluxo de emissão, aninhado) |
| **Agente** | Room+Firestore (salvar só FS) | cacheada espelhada + capability | Não; normalizar `salvar` (passo 1) |
| **Viagem** | Room+Firestore | cacheada espelhada; criável | Talvez (cadastro de viagem também é edição) |
| **Empresa** | Room+Firestore (sync-only) | cacheada espelhada (read-only) | Não |
| **Navio** | Room+Firestore | cacheada espelhada | Não |
| **Constante** | Room+Firestore (sync-only) | cacheada espelhada (catálogo) | Não |
| **Usuario** | Room+Firestore+Auth | cacheada + auth | Não |
| **ContadorBilhete** | Room+Firestore | sólida especial (sequência) | Não (transacional no emitir) |
| **PassagemDigital** | **Room only** | sólida local (caminho de arquivo) | Não — atenção: não re-sincroniza, cuidado em migração destrutiva |

Sub-documentos `PassageiroDocumento`/`VeiculoDocumento` não têm entidade nem coleção próprias —
são *value objects* embutidos no `PassagemDocumento`, o que reforça tratar Passagem como
**agregado** (DDD) cujo DTO aninhado é a forma natural (JSON), e a forma plana do Room é o
artefato de tenacidade a ser questionado.

**Consequências**

- A verdade passa a ser inequivocamente o Firestore; o Room assume o papel de cache/consulta;
  o snapshot de rascunho (a criar) assume a crash-safety da edição.
- A validação estrutural migra do schema para as fronteiras de promoção (poucas, explícitas).
- Enquanto o passo 3 (rascunho) não existe, o ganho é sobretudo conceitual + o piloto da
  capability; o REST morto (passo 4) é dívida a limpar.

**Alternativas futuras / pontos de reversão**

- Se as consultas por atributo crescerem (relatórios, filtros ricos), **não** migrar Passagem
  para blob JSON puro — manter colunas indexadas (índice ao lado do blob) ou reter o Room
  tipado. O JSON-Room só compensa onde a evolução de forma supera a necessidade de query.
- Avança a "Opção 2" adiada em [[ADR-0002]] (capability no agregado) — este ADR a promove como
  piloto do arco DTO-cêntrico.