# ADR-0006: Molde de cadastro (convenção + portas finas, sem framework CRUD genérico)

**Status:** Aceita

> Consolida o estudo em [`docs/design/cadastro-modulos.md`](../design/cadastro-modulos.md). Aplicado
> a Empresa (novo), Viagem e Agente (migrados).

**Contexto**

Os módulos de cadastro (Viagem, Agente, Empresa) repetem a mesma fatia — `NavComposable → Screen →
ContentAreaForm → ViewModel → validação → Repository → DAO → Firestore` — mas divergiam em qualidade
e sofriam os mesmos defeitos: eventos (`onXChange`) embutidos no `UiState` (quebra imutabilidade),
helpers segurando o `MutableStateFlow`, validação com efeito colateral (mutando estado), `runBlocking`
em `init`, sucesso/navegação em `finally` (reportava sucesso mesmo em falha), criar/editar por
sentinela string `"null"` na URL, `Context` descendo até o helper, e **repositórios concretos** (não
portas) — logo, ViewModels não testáveis com fake. Havia bugs reais daí (Agente "editar não atualiza";
`EmpresaDao.obterPorId(Int)` vs `id: String`).

**Opções consideradas**

1. **Status quo** — manter helpers com estado, lambdas no state, validação impura, repos concretos.
2. **Framework CRUD genérico** — `FormViewModel<T>` + `RepositorioEspelhado<T>` genéricos, máximo reuso.
3. **Molde por convenção + portas finas** — padronizar o *contrato* (repositório) e a *forma* da camada
   de formulário, sem generalizar o comportamento; único código compartilhado é um helper de sync.

**Decisão**

Opção 3. Resolve testabilidade e os defeitos recorrentes sem o custo da opção 2 (abstração prematura:
cada form tem campos e validação próprios, e um genérico viraria um framework rígido com casos especiais).

Contrato de **persistência** (portas — DIP, como a `AutenticacaoRepository` do [ADR-0005](0005-autenticacao-sessao-firebase-datastore.md)):
- Interface por entidade (`ViagemRepository`/`AgenteRepository`/`EmpresaRepository`, e as fontes
  `ConstanteRepository`/`NavioRepository`) + impl `*FirestoreRepository`, ligadas por `@Binds` no
  `RepositorioModule`. ViewModels dependem da porta; testes usam fakes.
- `salvar(modelo)` **suspenso**, id em branco → auto-id, **Room otimista + Firestore** (evita a corrida
  com o listener). `obterPorId(id: String): T?` nulo-seguro. `sincronizar()` via helper
  `sincronizarColecao` (espelho Room, ADR-0003) que **não lança dentro do listener** (loga).

Convenção da **camada de formulário**:
- VM é dona do `MutableStateFlow`; `UiState` **puro** (só dados/flags, sem lambdas) — eventos são
  métodos no VM.
- Validação **pura**: `validar(state): Erros` (sem mutar estado); o VM aplica o resultado.
- Sucesso por **evento one-shot** (`Channel`) consumido por `LaunchedEffect` na navegação; nada de
  callback `lateinit`, `finally` ou `Context` no VM.
- Criar/editar por **arg de rota opcional** (`?id={id}`, `defaultValue=""`), sem sentinela `"null"`.
- Cargas de fontes **suspensas** no `viewModelScope` (sem `runBlocking`).

**Consequências**

- **Boilerplate permanece por módulo**: o reuso é por *convenção/cópia*, não por abstração. Custo:
  repetição de forma; benefício: cada módulo é legível, independente e de baixo acoplamento — e a
  consistência depende de disciplina (seguir este ADR + o doc de design), não do compilador.
- Cada repositório ganha uma camada fina (interface + impl + binding).
- A migração é **incremental**: **Viagem** já convergiu por completo (cadastro + repositório +
  pesquisa/detalhes/delete + mapper, com testes); **Agente** (cadastro + busca) e **Empresa**
  (cadastro) estão no molde; demais consumidores (ex.: mappers de passagem/balanço, que ainda usam
  `runBlocking`) convergem quando tocados.

**Alternativas futuras**

- Se surgirem **4+ formulários estáveis e idênticos em forma**, reavaliar extrair um `FormCadastro`
  genérico (opção 2), agora com casos reais para guiar a abstração.
- Observabilidade não faz parte deste molde ainda (só `Log`); se padronizada (telemetria/`recordException`),
  incorporar aqui como parte do contrato.
