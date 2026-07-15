# ADR-0008: Relacionamentos do domínio por identidade — separar referência viva de snapshot histórico

**Status:** Proposta (aguardando aceite; execução faseada e aditiva, não iniciada)

> Formaliza o [estudo do domínio](../design/dominio-relacionamentos-e-camadas.md). Escopo = a
> **Opção 1** do estudo (consertar o relacionamento; manter o Room como cache). A decisão de eixo de
> storage (Firestore-only / híbrido — Opções 2/3) fica para um ADR futuro e **não** é pré-requisito
> desta. Conversa com o [ADR-0003](0003-modelo-de-memoria-do-dado.md) (Firestore=verdade,
> Room=espelho) e reusa o padrão **stack dormente / aditivo** de lá.

**Contexto**

Todo relacionamento *vivo* do domínio é feito por **nome** (string), não por `id`:
`Navio.empresa`, `Viagem.empresa`, `Viagem.navio` guardam o rótulo selecionado no dropdown
(`listaEmpresas.map { it.nome }`), não a chave estável da entidade — que existe (auto-id do
Firestore) mas fica sem uso na relação. Consequências concretas:

- **Rename quebra a relação em silêncio**: renomear uma Empresa orfaniza todos os Navios/Viagens que
  apontavam pro nome antigo. Nome é rótulo de negócio *mutável* sendo usado como chave estrangeira.
- **Ambiguidade** se dois registros compartilham nome — exatamente o que o id resolveria.
- **Agregação frágil**: `BalancoPassagensMapper` casa Passagem→Navio por `extrairPorDescricao(nome)`;
  se o Navio for renomeado, passagens antigas deixam de casar com as capacidades atuais.

Ao mesmo tempo, `Passagem` **copiar** os dados da Viagem (`empresa`/`navio`/`origem`/`destino`) está
**certo**: bilhete é registro imutável, deve congelar o estado da viagem no momento da emissão. O
código usa a mesma técnica — copiar o nome — para dois casos distintos (referência viva × snapshot
histórico), e só o segundo está correto.

**Opções consideradas**

1. **Status quo** — relacionar por nome. Simples, mas mantém o bug de rename/ambiguidade e a
   agregação frágil.
2. **Normalizar tudo por id, inclusive Passagem** — máxima integridade relacional; mas Passagem
   perderia o snapshot histórico (um bilhete passaria a "seguir" a viagem editada — errado) e cada
   leitura de bilhete exigiria join com master data que pode ter mudado.
3. **Por identidade nas referências vivas; snapshot por valor + ids embutidos na Passagem** —
   `Navio`/`Viagem` relacionam por `id` (nome resolvido na leitura pelo master list já em cache);
   `Passagem` mantém a cópia de valor (histórico) **e** ganha ids estáveis (`viagemId`/`navioId`/
   `empresaId`) só para relacionar/agregar.

**Decisão**

Opção 3. Um agregado transacional legitimamente guarda **id para relacionar** e **valor para
lembrar**; master data vivo relaciona por identidade.

- **Referências vivas por `id`**: `Navio.empresaId`, `Viagem.empresaId`, `Viagem.navioId`. O **nome
  não é armazenado** na entidade que referencia (evita a própria stale que queremos eliminar); é
  **resolvido na leitura** contra a lista de master data já sincronizada em Room (lookup em memória,
  barato — o primitivo `List<IObjetoSimplificado>.extrairPorId` já existe).
- **Passagem = snapshot por valor + ids embutidos**: mantém `empresa`/`navio`/`origem`/`destino`
  copiados (histórico imutável, exibição) e passa a carregar `viagemId`/`navioId`/`empresaId` para
  agregação e filtros. O balanço passa a casar por `navioId`, não por nome.
- **Padronizar `IObjetoSimplificado`** (`id` + `descricaoNome`) em Empresa/Viagem para tornar a
  resolução id↔nome genérica (habilitador; ver §3 do estudo).
- **Resolução de nome na fronteira, não no schema**: coerente com o ADR-0003 ("normalização em
  trânsito") — o mapper Room→screendata resolve o nome; o dropdown seleciona a entidade (id), não o
  rótulo.

**Plano de migração (faseado, aditivo — "stack dormente" do ADR-0003; nada quebra até o flip)**

*Fase 0 — Preparar (aditivo, sem flip).* Adicionar os campos de id nos `Documento` do Firestore
(`empresaId` em `NavioDocumento`/`ViagemDocumento`, `navioId` em `ViagemDocumento`; `*Id` em
`PassagemDocumento`) — schemaless, entra de graça. Manter os campos de nome vivos durante toda a
transição. **Prontidão de dado:** este é um app de **portfólio** — sem produção, sem dados oficiais,
tudo nasce do `SeedFirestore`. Logo **não há "dado antigo" para migrar**: basta o seed escrever o id
(ex.: `SeedFirestore` resolve `empresaId` do navio contra a empresa-sample). Backfill de documentos
existentes seria over-engineering aqui (foi considerado e descartado); só faria sentido com uma base
real em produção — e, mesmo lá, atenção a nome homônimo (que exige desempate manual).

*Fase 1 — Escrever id junto do nome.* Cadastros passam a gravar o `id` do item selecionado (do
dropdown), não só o nome. Mudança mínima possível: no `salvar`, resolver o nome selecionado → id via
lista em cache (`listaEmpresas.first { it.nome == state.empresa }.id`), mantendo a UI intacta;
evoluir o `DropDownFormField` para carregar id↔label quando conveniente. Migração destrutiva do Room
é **segura** (re-sincroniza do Firestore; só `PassagemDigital`, local-only, exige cuidado — ADR-0003).

*Fase 2 — Ler por id.* Mappers/repositórios resolvem o nome para exibição a partir do `id` + master
list em cache. Na **emissão** de Passagem, resolver os nomes no momento e congelá-los no snapshot
(fronteira de promoção volátil→sólida), gravando também os ids. `BalancoPassagensMapper` passa a
casar por `navioId` (e vira a oportunidade de matar o `runBlocking`/N+1 e depender da **porta**
`ViagemRepository`, não do concreto — smell §6 do estudo).

*Fase 3 — Flip / aposentar o nome nas referências vivas.* Quando todas as leituras usam id e todas
as escritas populam id, remover o campo de **nome** dos documentos de referência viva
(`Navio.empresa`, `Viagem.empresa`/`navio`). **Passagem mantém** os nomes copiados (snapshot). Bump
de versão do Room (migração destrutiva segura).

**Consequências**

- **Rename-safe e sem ambiguidade**: renomear master data não quebra relação; a exclusão vira
  **detectável** (lookup por id retorna `null` → "empresa removida" em vez de casar rótulo obsoleto).
  Política de órfão (bloquear/marcar) passa a ser possível — fica como trabalho à parte.
- **Custo de leitura**: toda tela que mostra "empresa/navio" resolve nome via master list em cache.
  É barato (lista pequena, já em memória), mas é uma dependência de leitura nova a modelar nos VMs.
- **Migração toca Firestore + Room** (não só Room), mas **sem backfill** (portfólio, sem dado real):
  a prontidão de dado é o `SeedFirestore` gravar o id. O caráter aditivo/faseado mantém tudo
  funcionando até a Fase 3.
- **Ortogonal ao storage**: nada aqui decide SQL×NoSQL; ao contrário, torna as Opções 2/3 do estudo
  mais limpas (relações explícitas por id migram melhor pra qualquer substrato).

**Alternativas futuras**

- **Normalização pura** (sem `descricaoNome` em lugar nenhum, só id + join `@Relation` do Room) —
  adiada: o app exibe nome em toda parte e o custo/benefício não fecha agora.
- **Decisão de eixo de storage** (Firestore-only / híbrido) — ADR futuro, com este relacionamento
  já consertado como base.
- **Política de integridade referencial na exclusão** (orfãos) — depende desta base de id; casar com
  a dívida de "observabilidade da exclusão" do [ADR-0007](0007-observabilidade-cadastros.md).