# Estudo de design — a camada de apresentação (navegação, composição e plataforma Android)

**Status:** Rascunho — Claude mapeou, decisões do analista pendentes. Primeiro estudo do eixo de
**apresentação**: até aqui os ADRs cobriram domínio (0008, 0012, 0013, 0015, 0016), dados (0003, 0009,
0017) e autorização (0010, 0011) — a UI nunca teve um. Ancorado no código em `2026-07-31`.

> Conversa com o [molde de cadastro](../adr/0006-molde-de-cadastro.md) (VM dona do estado, UiState puro,
> evento one-shot), o [ADR-0016](../adr/0016-dominio-da-plataforma.md) (que parte o menu em duas famílias
> e é o que **força** este estudo agora) e o [catálogo do domínio](dominio-da-plataforma.md), que é a base
> de qualquer transformação daqui. Complementa os estudos de fluxo já existentes
> ([login](fluxo-login.md), [main screen](fluxo-main-screen.md),
> [form de passagem](form-passagem-validacao-exibicao.md)), que olham **uma tela cada**; aqui o corte é a
> **camada**.

**Números do mapeamento.** 138 arquivos em `ui/` (22 telas, 50 componentes, 23 estados, 39 ViewModels e
helpers, 4 de tema) e 25 em `navigation/`. Compose BOM `2024.12.01`, Material3, navigation-compose
`2.8.5`, `minSdk 26` / `targetSdk 34` / `compileSdk 35`.

---

## 1. O que já está certo (e é o que torna o resto corrigível)

Começar pelo defeito daria a impressão errada. A fronteira mais difícil de Compose **já está no lugar**:

- **Nenhum composable de `ui/screens` ou `ui/components` recebe ViewModel.** O grep não acha uma
  referência sequer. Telas recebem `state` + lambdas; quem conhece o ViewModel é a camada de navegação.
- **60 arquivos com `@Preview`.** Isso não é acaso: é *consequência* do item anterior. Tela que depende de
  ViewModel não tem preview, e este app tem previews porque as telas são funções de estado.
- **O estado é `StateFlow` no ViewModel, coletado na fronteira** (`collectAsState` no navcomposable).
- **`CommonScaffold` centraliza o esqueleto** (top bar, bottom bar, drawer, pull-to-refresh, FAB), com
  drawer permanente acima de 600dp — há responsividade pensada, não só empilhamento.

Ou seja: o *state-down / events-up* está respeitado. Os problemas abaixo são de **forma e de lugar**, não
de arquitetura de Compose — e é por isso que quase todos são corrigíveis sem reescrever tela.

## 2. Navegação

### 2.1 Rota é String concatenada à mão — e o código já paga por isso

As rotas são literais numa sealed class (`FluviAppNavComposableDestinations`), e a navegação monta a URL
por interpolação, em 20 funções de extensão sobre `NavHostController`:

```kotlin
fun NavHostController.navegarParaFormularioPassagemComViagem(idViagem: String, idPassagem: String? = null) {
    navegaDireto(".../${FormPassagemNavComposable.route}/$idViagem/$idPassagem")   // ← idPassagem null
}
```

Quando `idPassagem` é `null`, o que entra na URL é a **String `"null"`**. Do outro lado, o ViewModel faz
`checkNotNull(savedStateHandle[EDIT_PASSAGEM_ARGUMENT])` — que **nunca** falha, porque o valor chegou
preenchido com o texto `"null"` — e o app decide "é edição ou é novo?" com esta extensão:

```kotlin
fun String?.isTextoNaoNulo(): Boolean = this != null && this != "null"
```

**Existe uma função utilitária no projeto cuja única razão de ser é compensar a rota não-tipada.** Não é
um bug — funciona —, é um *tell*: o tipo foi perdido na fronteira e alguém teve que reconstruí-lo por
comparação de string. O `checkNotNull` reforça: ele parece uma guarda e não guarda nada.

**O recurso para resolver já está no projeto.** `navigation-compose 2.8.5` suporta **rotas type-safe** com
`@Serializable` desde a 2.8.0: o destino vira um tipo, o argumento vira propriedade, `null` é `null`, e o
compilador cobra. É a correção mais barata deste documento em relação ao que devolve.

### 2.2 A navegação carrega orquestração de negócio

`formPassagemNavComposable` não navega — ele **conduz a emissão**:

```kotlin
onClickAvancar = {
    if (viewModel.validarFormularios()) {
        viewModel.formPassagemHelper.atualizarIsSaving()
        coroutineScope.launch {
            val id = viewModel.salvarPassagem(context)          // ← Context indo para o ViewModel
            viewModel.formPassagemHelper.atualizarIsSaving()
            when {
                id != null -> onNavegaParaDetalhesPassagem(id)
                viewModel.uiStatePassagem.value.emissaoBloqueadaMsg != 0 -> scrollParaErro++
                else -> context.toastMessage(...)
            }
        }
    } else scrollParaErro++
}
```

Três coisas se acumulam aqui: **a decisão do desfecho** (salvou? bloqueou? falhou?) está na navegação e não
no ViewModel; **o `isSaving` é ligado e desligado de fora**, o que faz do estado de carregamento uma
responsabilidade compartilhada; e **`Context` é passado para dentro do ViewModel**, o que amarra a camada
de estado ao Android.

O molde do ADR-0006 já tem a resposta para isso — **evento one-shot**: o ViewModel emite `Salvo(id)` /
`Bloqueado(msg)` / `Falhou`, e a navegação só reage. A fatia de emissão nunca foi migrada para o molde
(é o que o [estudo do form de passagem](form-passagem-validacao-exibicao.md) já registrou por outro
ângulo).

### 2.3 Callback drilling: 13 lambdas num grafo, 43 numa tela

O `NavHost` distribui lambdas para os grafos, que distribuem para os navcomposables, que distribuem para as
telas:

| Ponto | Lambdas |
|---|---|
| `mainScreenGraph(...)` | 13 |
| `FormPassagemScreen(...)` | **43** |
| `pesquisarPassagemGraph(...)` | 6 |

As 43 da tela de passagem são quase todas do mesmo formato (`onXChange`, `onCheckX`, `onClickLimparX`) e
são repassadas uma a uma como `viewModel::onXChange`. O padrão idiomático para isso é agrupar: **um objeto
de ações** (`FormPassagemActions`, ainda testável e previsível) ou **um `onEvent(Event)`** com uma sealed
class de eventos. As duas cabem no molde do ADR-0006; a escolha é do analista (§6).

### 2.4 A política de menu mora na navegação

`MainScreenNavComposable` tem um `when (secao)` exaustivo que mapeia cada `SecaoMenu` para os seus botões
(`acoesDe`), e `DadosBotoesMenus` carrega a lambda de navegação dentro de um objeto que vive em
`domain/screendata/`.

Isso vai colidir de frente com o ADR-0016 §2, que **parte `SecaoMenu` em duas famílias** (painel × operação,
com `EQUIPE` nos dois) e acrescenta `PORTO`, `TRECHO`, `CATALOGO` e `ROTA`. Hoje, acrescentar uma seção
significa mexer num `when` dentro da camada de navegação — que é o lugar mais distante possível de
`PermissoesUsuario`, onde a política realmente mora.

### 2.5 Duas estratégias de back stack, sem critério escrito

```kotlin
fun NavHostController.navegaDireto(rota: String)  = navigate(rota) { popUpTo(startDestination) { saveState = true }; launchSingleTop = true; restoreState = true }
fun NavHostController.navegaLimpo(rota: String)   = navigate(rota) { popUpTo(0) }
```

`navegaLimpo` (zera a pilha) é usada para login e main screen; `navegaDireto` para o resto. O critério é
razoável — trocas de sessão zeram, navegação normal preserva —, mas **não está escrito em lugar nenhum**, e
`popUpTo(0)` é um número mágico onde caberia o id do grafo raiz.

## 3. Plataforma Android

### 3.1 `@RequiresApi(S)` na porta de entrada de um app `minSdk 26`

Cinco anotações, e elas formam uma corrente:

```
MainActivity.onCreate  →  FluviApp  →  FluviAppNavHost  →  mainScreenGraph  →  mainScreenNavComposable
```

A causa está na ponta: a Main Screen pede permissão de Bluetooth na entrada, e
`Manifest.permission.BLUETOOTH_CONNECT` é uma constante **API 31**:

```kotlin
RequestMultiplePermissions(
    permissionsList = listOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_CONNECT)
)
```

Três consequências, em ordem de gravidade:

1. **A anotação subiu até a `Activity` para calar o lint.** `@RequiresApi(S)` na porta de entrada de um app
   que declara `minSdk 26` é uma contradição declarada: ou o app não roda em Android 8–11, ou a anotação
   está mentindo. Ela está mentindo — a constante é resolvida em tempo de compilação e não quebra em
   runtime —, mas o efeito colateral é que **o lint parou de olhar para todo o caminho**, e qualquer chamada
   realmente API-31 que apareça lá dentro passa sem aviso.
2. **Em API 26–30 o pedido não faz sentido:** `BLUETOOTH_CONNECT` não é permissão de runtime lá; o pedido é
   ruído.
3. **É para a impressora térmica, que está dormente.** O ADR-0012 deixou a impressão física esperando o
   módulo de check-in. O app pede Bluetooth na entrada da tela principal por uma funcionalidade que não
   está em uso.

### 3.2 Abstração vazia na raiz

```kotlin
@Composable fun FluviApp(navController: NavHostController = rememberNavController()) {
    FluviApp { FluviAppNavHost(navController = navController) }
}
@Composable fun FluviApp(content: @Composable () -> Unit) { content() }
```

Duas funções de mesmo nome onde a segunda **só invoca o parâmetro**. É um ponto de extensão que nunca foi
usado — provavelmente restou de um scaffold global que migrou para o `CommonScaffold`.

### 3.3 Dívidas de SDK já conhecidas

`compileSdk 35` com `targetSdk 34`: subir o target exige o bump do CameraX (1.3.4 tem um `.so` a 4 KB e
quebra o alinhamento de 16 KB). Já está registrado como dívida; entra aqui só para o mapa ficar completo.

## 4. Composição e estado

### 4.1 Quatro UiStates carregam lambdas

`ContagemPassagemUiState`, `LoginUiState`, `PesquisarPassagemUiState` e `RecuperarSenhaUiState` têm
`() -> Unit` como propriedade. O molde do ADR-0006 pede **UiState puro** (só valores), e por três razões
concretas: um state com lambda não é comparável (recomposição desnecessária), não é serializável (não
sobrevive a *process death*, ao contrário do rascunho do ADR-0004) e não é montável num `@Preview` sem
inventar funções.

### 4.2 `CommonScaffold` é configurado por flags booleanas

Treze ocorrências de `Boolean` na assinatura — `isMainTopAppBar`, `isShowBottomAppBar`, `isShowRightIcon`,
`hasRefresh`, `isRefreshing`, `inicioAtivo`… Cada flag multiplica os arranjos possíveis, e a maior parte
deles nunca é usada. O componente hoje é, na prática, **três esqueletos diferentes** (main, formulário,
listagem) espremidos num só por parâmetro.

### 4.3 Strings literais na UI

29 ocorrências de `text = "..."` fora de `stringResource`. Não há tradução no app hoje, então isso não
quebra nada — mas é onde a internacionalização vai doer, e é barato de corrigir enquanto são 29.

### 4.4 Cobertura de teste da UI

3 arquivos em `androidTest` para 22 telas. Os 244 testes JVM cobrem ViewModel, validação e domínio — o que
é a escolha certa de prioridade —, mas quer dizer que **navegação e composição não têm rede**. É relevante
agora: mexer em rota type-safe ou partir o `SecaoMenu` são exatamente as mudanças que um teste de navegação
pegaria.

## 5. O que está acoplado a quê (mapa de dependências da camada)

```
MainActivity ──> FluviApp ──> FluviAppNavHost ──┬──> graphs/ ──> navcomposables/ ──> screens/
                                                │                      │
                                                │                      ├──> hiltViewModel()
                                                │                      └──> orquestração (§2.2)
                                                └──> extensions/NavControllerExtensions (20 fns)

navcomposables/ ──> domain/screendata/DadosBotoesMenus  (objeto de UI com lambda dentro)
navcomposables/ ──> domain/screendata/SecaoMenu         (política de menu — §2.4)
```

Duas observações: a camada de navegação é o **ponto de encontro de tudo** (ViewModel, política, orquestração
e destino), e `domain/screendata/` é apresentação morando dentro do pacote de domínio — o que o
[catálogo do domínio §6](dominio-da-plataforma.md) já registrou pelo outro lado.

## 6. Fatias possíveis, na ordem em que eu proporia

Nenhuma depende de decisão de domínio pendente, e todas são independentes entre si:

| # | Fatia | Tamanho | Por que agora |
|---|---|---|---|
| 1 | **Rotas type-safe** (§2.1) | médio | a versão já suporta; mata `isTextoNaoNulo`, o `"null"` e o `checkNotNull` decorativo |
| 2 | **Desfecho da emissão como evento one-shot** (§2.2) | pequeno | tira orquestração e `Context` da navegação; fecha uma dívida do ADR-0006 |
| 3 | **Política de menu sai da navegação** (§2.4) | pequeno | **pré-requisito prático da F3 do ADR-0016** (duas famílias de seção) |
| 4 | **Bluetooth sob demanda + tirar o `@RequiresApi`** (§3.1) | pequeno | devolve o lint ao caminho inteiro e para de pedir permissão para função dormente |
| 5 | **Agrupar as 43 lambdas** (§2.3) | médio | depende de escolher entre objeto de ações e `onEvent` |
| 6 | **UiState puro nos quatro** (§4.1) | pequeno | alinha ao molde; habilita preview e comparação |
| 7 | **Partir o `CommonScaffold`** (§4.2) | médio | melhor fazer *depois* da 3, que já mexe no menu |

A 3 é a que eu faria primeiro se o ADR-0016 for começar pelo painel: ela é pequena e desbloqueia.

## 7. Perguntas para o analista

1. **Rotas type-safe entram?** É a fatia com melhor relação custo/benefício, mas toca as 20 extensões e os
   17 destinos de uma vez — não dá para fazer meia migração sem ficar com dois vocabulários de rota.
2. **As 43 lambdas viram objeto de ações ou `onEvent(Event)`?** Objeto de ações preserva o estilo atual e é
   mais fácil de previsualizar; `onEvent` é mais enxuto e casa melhor com evento one-shot, mas muda a forma
   de todas as telas de formulário. *Inclinação minha: objeto de ações — muda menos e resolve o mesmo.*
3. **A política de menu vai para onde?** `PermissoesUsuario` já decide *quais seções aparecem*; falta decidir
   se ela também passa a decidir *quais ações cada seção oferece* (hoje é o `when` da navegação). Com as duas
   famílias do ADR-0016, isso vira pergunta de domínio, não de UI.
4. **O Bluetooth continua sendo pedido na entrada?** Se a impressão física está dormente até o check-in, a
   permissão pode ir junto — pedida quando o recurso existir.
5. **Vale um ADR de apresentação, ou cada fatia entra como decisão isolada?** Minha leitura: **um ADR só**,
   fixando o padrão da camada (rota tipada, desfecho por evento, agrupamento de ações, UiState puro), com as
   fatias como fases — é o mesmo formato que funcionou no ADR-0009 e no 0015.