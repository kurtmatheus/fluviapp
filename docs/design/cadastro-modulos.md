# Módulos de Cadastro — Análise (Viagem, Agente) e Estrutura Refatorada

Estudo de arquitetura e lógica dos módulos de cadastro **Viagem** e **Agente**, avaliado contra as
decisões de arquitetura já adotadas no projeto, com uma **estrutura refatorada** que serve de molde
para implementar **Empresa** limpo (fase 2 de [fluxo-main-screen.md](fluxo-main-screen.md)).

Decisões-base de referência:
- **DIP + resultado de domínio** na borda (padrão `ResultadoAutenticacao`, ADR-0005).
- **Room espelha Firestore** (ADR-0003).
- **Fonte única de estado** e **validação/autorização centralizada** (fluxo-main-screen §8).
- **Sem framework CRUD genérico prematuro** (fluxo-main-screen §8.6) — normalizar por *convenção* e
  um repositório-base fino, não um `FormViewModel<T>` genérico.

---

## 1. Anatomia comum (fatia por entidade)

```
NavComposable → Screen → ContentAreaForm → ViewModel → { FormHelper, ValidacaoHelper } → Repository → DAO → Firestore
```
Cada entidade repete essa fatia de 5 camadas. Viagem é o irmão mais maduro; **Agente lê como uma
cópia apressada** do Viagem (com bugs adicionais); Empresa não tem a fatia de cadastro ainda.

---

## 2. Módulo Viagem

**VM** (`FormViagemViewModel`): dona do `StateFlow`, guarda `idViagem` e o prefill; delega quase tudo
a `FormViagemHelper`. Helpers são `lateinit`, construídos dentro de uma coroutine no `init`.

**Lógica notável e smells** (ancorados):
- **Prefill de edição bugado** (`FormViagemViewModel.kt:78`): lê `it.trechoOrigem` (estado velho, `""`)
  em vez de `viagemCard.origem` → destino fica desabilitado na edição.
- **`salvarViagem` reporta sucesso no `finally`** (`FormViagemHelper.kt:130-159`): o toast de sucesso e
  o `onNavegaParaMainScreen()` rodam **mesmo em exceção** — erro engolido.
- **`runBlocking` para dropdowns** (`FormViagemHelper.kt:54-56`) e **re-busca de navios a cada troca de
  empresa** (`:70-82`).
- **Reverse-map por descrição** com `.first{}`/`extrairPorDescricao` (`:136-142`) → `NoSuchElementException`
  se o texto não estiver na lista (validação só checa `isBlank`).
- **Validação impura** (`ValidacaoFormViagemHelper`): seta flags de erro no estado compartilhado e lê de
  volta; nunca reseta para `false`; empresa tem flag de erro mas **não é validada**.

## 3. Módulo Agente (e comparação)

**VM único compartilhado** (`AgenteViewModel`) serve **form e busca** — um `AgenteUiState` mistura
campos de formulário, filtro de busca e `resultadosListaAgente`. A tela de busca é forçada a mandar
`idAgente="null"` só para satisfazer o `checkNotNull` do VM de formulário.

**Bugs/smells próprios:**
- **Editar não atualiza (bug real, `FormAgenteHelper.kt:83-109`):** em edição, busca o agente por id e,
  pelo elvis `agenteExistente ?: Agente(...)`, **re-salva a entidade inalterada** — os campos editados
  (`state.agente/agencia/lotacao`) só são usados na criação. Edição nunca persiste alteração.
- **Sucesso no `finally`** (`:106-107`) — igual ao Viagem: falha vira "salvo" + navega.
- **Duplo `obterPorId`** (prefill em `preencherCampos:111` + de novo no `salvar:90`).
- **Filtro de busca client-side no composable** (`ResultSearchAgenteScreen.kt:80` `filtrarResultados`) —
  regra na view. Viagem filtra no VM (`PesquisarViagemViewModel`).
- **Pacote errado:** helper em `helpers/passagem/`, content em `components/forms/areas/passagem/`
  (agente não é passagem). Preview mal-nomeado `NovaViagemScreenPreview`.

| Concern | Viagem | Agente |
|---|---|---|
| VM | 2 VMs (form + pesquisa) | **1 VM** compartilhado (form+busca) |
| Editar persiste? | ✅ sim | ❌ **não** (re-salva inalterado) |
| Filtro de busca | VM | composable (view) |
| Coroutine de save | dentro do VM | na **camada de navegação** |

## 4. Smells compartilhados (os dois módulos)

1. **Eventos embutidos no UiState** — `onXChange`/`onClickLimpar` como lambdas `= {}` injetadas em runtime
   pelo helper. Quebra imutabilidade/igualdade do `data class`; mistura estado e evento.
2. **Helper segura o `MutableStateFlow`**; **validação com efeito colateral** (muta o estado) em vez de
   `(state) -> Resultado` pura.
3. **`lateinit` helpers no `init`** + **camada de navegação acessando `viewModel.formHelper`/`validacaoHelper`
   direto** → janela de corrida + vazamento de encapsulamento.
4. **Sucesso/navegação via callback `lateinit onNavegaParaMainScreen`** setado pela nav; e no `finally`.
5. **Criar-vs-editar via sentinela string `"null"`** interpolada na URL + `checkNotNull` + `isTextoNaoNulo()`.
6. **`Context` desce UI→VM→helper** para toasts (Android na camada de lógica).
7. **`titleJanela` no estado** como flag de modo (derivável de criar/editar).
8. **`runBlocking`** em `init` de helper para carregar fontes de dropdown.

## 5. Camada de persistência (repositórios/DAOs)

**Bug latente Empresa:** `EmpresaDao.obterPorId(idEmpresa: Int)` com `WHERE id = :idEmpresa`, mas
`Empresa.id` é **String** — ids auto do Firestore nunca casam. Viagem/Agente já usam `obterPorId(String)`.

**Contrato de `salvar` divergente:**
| | Assinatura | suspend | Escreve | "novo" |
|---|---|---|---|---|
| Viagem | `salvar(id: String?, navio, empresa, origem, destino)` | ✅ | Room + Firestore | `id==null` |
| Agente | `salvar(agente: Agente)` | ❌ | **só Firestore** (espera listener) | `id.isBlank()` |
| Empresa | — inexistente | — | — | — |

- A escrita "só Firestore" do Agente → **corrida com o listener** (a mesma do login Google).
- **`sincronizar()` triplicado** e idêntico nos 3; **lança `RuntimeException` dentro do listener**
  (thread async) — não propaga, só derruba/loga (origem do "Falha na Sincronização").
- `obterPorId` retorna `Flow<T>` **não-nulo** (arrisca crash sem linha); `UsuarioDao` usa `Flow<T?>`.
- Pacotes/nomes dispersos: `firebase/ViagemFirestoreRepository`, `cadastro/passagem/AgenteRepository`,
  `cadastro/viagem/EmpresaRepository`.

---

## 6. Diagnóstico × decisões de arquitetura

| Smell | Decisão violada | Correção no molde |
|---|---|---|
| Eventos no UiState; helper dono do estado | Fonte única de estado | VM dona do estado; UiState puro; `onEvent`/métodos no VM |
| Validação com efeito colateral | Validação centralizada/pura | `validar(state): ResultadoValidacao` pura |
| Sucesso no `finally`; RuntimeException solta | DIP + resultado de domínio | `ResultadoPersistencia` (Sucesso/Falha) + evento one-shot |
| `salvar` divergente; Agente só-Firestore | Contrato normalizado | `salvar(modelo)` suspend, Room+Firestore, id `""`→auto |
| `sincronizar` triplicado + throw no listener | Contrato normalizado (ADR-0003) | base/ajuda comum; logar sem throw |
| Nav acessa internals do VM; `"null"` na URL | Encapsulamento | arg nullable/opcional; orquestração no VM |
| `runBlocking`; refetch por tecla | — | `suspend` no `viewModelScope`; carregar uma vez |

---

## 7. Estrutura refatorada (molde)

### 7.1 Persistência — contrato normalizado
Um repositório-base fino mata o triplo boilerplate (justificado; ≠ framework de form):
```kotlin
abstract class RepositorioEspelhado<T : Identificavel>(
    private val colecao: String,
    private val firestore: FirebaseFirestore,
) {
    protected abstract suspend fun salvarLocal(modelo: T)      // dao.salvar
    protected abstract fun paraModelo(doc: DocumentSnapshot): T?
    protected abstract fun T.paraDocumento(): Any
    protected abstract fun comId(modelo: T, id: String): T

    fun sincronizar(onErro: (Throwable) -> Unit = {}) {
        firestore.collection(colecao).addSnapshotListener { v, e ->
            v?.documents?.mapNotNull(::paraModelo)?.forEach { runBlocking { salvarLocal(it) } }
            if (e != null) { Log.e(colecao, e.message, e); onErro(e) }   // sem throw no listener
        }
    }
    suspend fun salvar(modelo: T) {
        val ref = if (modelo.id.isBlank()) firestore.collection(colecao).document()
                  else firestore.collection(colecao).document(modelo.id)
        val comId = comId(modelo, ref.id)
        salvarLocal(comId)          // Room otimista (offline-first)
        ref.set(comId.paraDocumento())
    }
}
```
> Alternativa pragmática se os genéricos incomodarem: manter repos por entidade, mas **padronizar a
> assinatura** (`salvar(modelo)` suspend, `obterPorId(String): T?`, `sincronizar` sem throw) e extrair só
> um helper `sincronizarColecao(...)`. O importante é o **contrato**, não a herança.

- `obterPorId(id: String): T?` (nulo-seguro) em todos; DAOs com `Flow<T?>`.

### 7.2 Camada de formulário — convenção (não framework)
- **VM dona do `MutableStateFlow`**; helpers **puros** (recebem/retornam dados, sem segurar estado).
- **UiState puro**: só dados + flags; **sem lambdas**. Eventos por métodos no VM (`onNomeChange(v)`) ou
  `onEvent(FormEvent)`.
- **Validação pura**: `fun validar(state): ResultadoValidacao` (por campo + regras cruzadas), sem mutar
  estado; o VM aplica o resultado.
- **Sucesso/erro por estado + evento one-shot** (`Channel`/`SharedFlow` consumido por `LaunchedEffect` na
  Screen) — nada de `onNavegaParaMainScreen` `lateinit`, nada de navegar no `finally`, sem `Context` no VM.
- **Criar/editar**: nav arg **opcional** (`formEmpresa?id={id}`, `defaultValue=""`) → sem sentinela `"null"`.
- **Carga de dropdowns**: `suspend` no `viewModelScope`, uma vez.
- **Nav não toca internals**: chama `viewModel.salvar()`; o VM valida e emite evento de sucesso.
- **Pacote/nome consistentes** por entidade (ex.: `.../empresa/...`, não sob `viagem`/`passagem`).

---

## 8. Aplicação ao Empresa (fase 2 — sobre o molde)

Empresa (`id, nome, razaoSocial, cnpj, endereco, telefone1, telefone2`) implementado já refatorado:
1. **Persistência**: `EmpresaDao.obterPorId(id: String)` (corrige `Int`); `EmpresaDocumento` já tem
   `toEmpresa`, criar `Empresa.toDocumento()`; `EmpresaRepository.salvar(empresa)` (Room+Firestore, id
   `""`→auto) no contrato §7.1; `sincronizar` sem throw.
2. **Form (convenção §7.2)**: `FormEmpresaUiState` puro; `FormEmpresaViewModel` dono do estado, `salvar()`
   + evento de sucesso; validação pura (`nome`/`razaoSocial`/`cnpj` obrigatórios; telefones opcionais;
   máscara de CNPJ a confirmar); `ContentEmpresaAreaForm`; `FormEmpresaScreen` (`CommonScreenNoBottom`).
3. **Navegação**: rota `formEmpresa?id={id}` (arg opcional), destino, wiring no `FluviAppNavHost`,
   extensão `navegaParaFormularioEmpresa(id?)`.
4. **Menu**: `SecaoMenu.EMPRESA` (gate gestor via `PermissoesUsuario`) + card "Nova empresa".

## 9. Escopo / faseamento

- **Empresa nasce refatorado** sobre §7 (é o objetivo imediato).
- **Viagem/Agente**: migração para o molde é **incremental e separada** — prioridade nos **bugs reais
  primeiro** (Agente editar-não-atualiza; sucesso-no-`finally` dos dois; `obterPorId` Empresa), depois a
  normalização de estado/validação/nav. Não reescrever tudo de uma vez.
- **Decisões (resolvidas)**: ✅ **alternativa pragmática** — repos por entidade com contrato padronizado
  (`salvar(modelo)` suspend, `obterPorId(String): T?`, `sincronizar` sem throw) + um helper
  `sincronizarColecao`; sem `RepositorioEspelhado<T>` genérico. ✅ **CNPJ com máscara** (00.000.000/0000-00)
  + validação de dígitos verificadores; telefones **opcionais**; nome/razão social/CNPJ obrigatórios.
