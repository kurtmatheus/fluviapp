# A orquestração da Passagem — helpers e ViewModels contra o que já foi decidido

> **Status:** **aberto** — mede a orquestração como ela existe em `cdf15b3` (2026-08-11) e a confronta com o
> molde ([ADR-0006](../adr/0006-molde-de-cadastro.md)) e com a trinca da Passagem:
> [ADR-0023](../adr/0023-passagem-por-categoria-e-referencia.md) (domínio),
> [ADR-0024](../adr/0024-fronteira-de-dados-da-passagem.md) (fronteira) e
> [ADR-0025](../adr/0025-camada-de-dados-da-passagem.md) (camada). **Não é inventário de defeito: é material de
> correção e revitalização** — cada desvio abaixo tem a decisão que o resolve. As perguntas estão no §6.
>
> O passo seguinte, e último, é a **apresentação** (navegação e UI).

## 1. A régua, que já está escrita

Duas fontes decidem o que a orquestração deve ser, e nenhuma delas é nova:

**O molde de cadastro** (ADR-0006), que já governa os sete cadastros revitalizados: **o ViewModel é o dono do
estado**; o `UiState` é **puro** (sem lambda, sem flag de mecanismo); a **validação é pura** — `(state) → erros`,
e é o VM que aplica; o que a tela precisa saber uma vez é **evento one-shot**, não estado; argumento de destino
é **opcional**.

**A trinca da Passagem**, que muda o que há para orquestrar:

| Decisão | O que ela cobra da orquestração |
|---|---|
| ADR-0023 D1 — categoria é a raiz, tipo fechado | o estado do form deixa de ter *booleano de veículo*; o que existe é **categoria** |
| ADR-0023 D3/D4 — regra no tipo | a limpeza reativa de campos e as exigências saem do helper e vão para o tipo |
| ADR-0024 D8 — DTO com tipo | o VM para de receber texto formatado e a formatação sobe para a tela |
| ADR-0025 D1 — porta `PassagemRepository` | o VM injeta **porta**, e passa a ser testável em JVM |
| ADR-0025 D3 — a tradução não busca | helper deixa de carregar repositório; **coletar é da camada de dados** |
| preço é I/O | `validarEmissao`/`SemTarifa` e a resolução de tarifa **saem** do fluxo |

## 2. O mapa

| Peça | Linhas | Papel hoje |
|---|---|---|
| `FormPassagemViewModel` | 329 | orquestra a emissão; 3 `UiState`; ~43 `on*Change` delegando |
| `FormPassagemHelper` | 466 | mutadores + orquestração + construção do agregado + regra de tarifa |
| `FormPassageiroHelper` | 258 | mutadores do sub-form de passageiro |
| `FormVeiculoHelper` | 142 | mutadores do sub-form de veículo |
| `PesquisarPassagemViewModel` | 140 | consulta |
| `FormPesquisarPassagemHelper` | 122 | mutadores do form de busca |
| `DetalhesPassagemViewModel` | 137 | detalhe + bilhete |
| `PassagemDigitalHelper` | 69 | geração/registro do bilhete digital |
| `ContagemPassagemViewModel` + helper | 47 + 54 | ocupação |
| **`EmbarqueViewModel`** | **67** | **o contraexemplo — está no molde** |

## 3. Os seis desvios, medidos

### 3.1 O estado é do ViewModel, mas o **handle mutável** é entregue aos helpers

```kotlin
// FormPassagemViewModel.kt:121-136
formPassagemHelper = FormPassagemHelper(
    uiStatePassagem = _uiStatePassagem,   // ← o MutableStateFlow, não o StateFlow
    uiStatePassageiro = _uiStatePassageiro,
    uiStateVeiculo = _uiStateVeiculo,
    passagemRepository = passagemRepository,
)
```

O molde diz *"o VM é dono do estado"*, e formalmente ele é: os três `MutableStateFlow` estão declarados nele.
Mas o **direito de escrever** é passado adiante para três objetos, e com isso a pergunta *"quem mudou este
campo?"* deixa de ter uma resposta local. Não é o helper que está errado por existir — é o **handle mutável**
que atravessa a fronteira.

Efeito colateral de forma: os helpers são `lateinit var` inicializados dentro de um `viewModelScope.launch`
(`:76-86`), então existe uma janela em que o VM está construído e os helpers não. Hoje ela não pipoca porque a
tela ainda não é alcançável; ela é dívida esperando alcance.

### 3.2 O `FormPassagemHelper` acumula quatro papéis

Pelas assinaturas (466 linhas, ~30 métodos), ele é ao mesmo tempo:

1. **banco de mutadores** — `atualizarDataViagem`, `checkPix`, `atualizarValorPix`, … (um por campo);
2. **orquestrador** — `salvarPassagem(...)`, `validarEmissao(...)`, `atualizarIsSaving/IsLoading`;
3. **construtor do agregado** — `montarPassagem(...)` (`:369`);
4. **regra de negócio** — `resolverTarifaBase(...)` (`:464`) e os bloqueios de emissão.

Os quatro têm destinos **diferentes** nas decisões já tomadas, e é isso que faz esta divisão importar: o (1) é
o único que é de fato orquestração de tela; o (3) vira construção de um **tipo fechado** (ADR-0023 D1); o (4)
**morre** (preço é I/O); e o (2) é o que precisa de dono.

### 3.3 A orquestração da emissão mora na **navegação**

```kotlin
// FormPassagemNavComposable.kt:86-102
onClickAvancar = {
    if (viewModel.validarFormularios()) {
        viewModel.formPassagemHelper.atualizarIsSaving()      // ← navegação mexe no helper do VM
        coroutineScope.launch {                                // ← escopo da composição, não do VM
            val id = viewModel.salvarPassagem(context)
            viewModel.formPassagemHelper.atualizarIsSaving()
            when {
                id != null -> onNavegaParaDetalhesPassagem(id)
                viewModel.uiStatePassagem.value.emissaoBloqueadaMsg != 0 -> scrollParaErro++
                else -> context.toastMessage(…)
            }
        }
    } else scrollParaErro++
}
```

Três coisas acontecem aqui que não são de navegação: **decidir a sequência** (validar → marcar salvando →
salvar → desmarcar), **escolher o desfecho** (navegar × rolar × avisar) e **mutar estado** através de um helper
que o VM expõe (`internal lateinit var formPassagemHelper`, `:72`). O `coroutineScope` é o da composição — se a
recomposição levar o escopo embora no meio, a emissão vai com ele.

E a razão pela qual isso é **correção, não gosto**: com a porta do ADR-0025 D1 o VM fica testável em JVM; mas
se a sequência da emissão vive no navcomposable, **o teste do VM não cobre a emissão** — cobre as peças que ela
usa. Fica-se com um VM testável e um fluxo não testado.

### 3.4 `Context` no ViewModel, e toast como canal de erro

`salvarPassagem(context: Context)` recebe o contexto e chama `context.toastMessage(...)` em dois pontos
(`:208`, `:227`) — um deles para dizer *"o emissor não tem funcionário"*, que é **regra de negócio**
comunicando-se por um mecanismo de UI. O molde já resolveu isso nos sete cadastros: **evento one-shot**.

### 3.5 `scrollParaErro: Int` — o one-shot feito à mão

```kotlin
var scrollParaErro by remember { mutableIntStateOf(0) }   // FormPassagemNavComposable.kt:37
… scrollParaErro++                                        // "aconteceu de novo"
```

Um contador que só existe para que **o mesmo evento aconteça duas vezes** seja distinguível. É exatamente o
problema que o evento one-shot resolve, resolvido à mão — e resolvido **na navegação**, onde o estado da tela
não deveria morar.

### 3.6 Três `UiState` e um booleano de veículo — o estado espelha o modelo que o ADR-0023 desfez

O VM mantém `uiStatePassagem`, `uiStatePassageiro` e `uiStateVeiculo`, e o que decide qual sub-form vale é
`isVeiculoChecked` — um **booleano**. Daí saem `checkVeiculo()` e `limparCamposPassageiroOuVeiculo()`
(`FormPassagemHelper.kt:33,43`): limpeza reativa para impedir o estado misto.

**O ADR-0023 D1 tornou o estado misto irrepresentável no domínio** — categoria é tipo fechado. A limpeza
reativa existe porque o *estado da tela* ainda representa o que o domínio já não representa. Corrigir aqui não
é renomear: é o `UiState` passar a ter a **mesma forma** do agregado.

### O contraexemplo, que vale mais que os seis desvios

`EmbarqueViewModel` (67 linhas, era do ADR-0012) **está no molde**: um `UiState`, dono do estado, sem helper,
sem `Context`, `viewModelScope` próprio, e o resultado exposto como valor (`ResultadoEmbarque`) para a tela
reagir caso a caso.

Ele prova que o desvio **não é do domínio da passagem** — é do **tamanho do formulário**. A emissão é a maior
tela do app, e foi ela que produziu helpers, handles mutáveis e orquestração espalhada. Qualquer desenho
proposto tem de responder ao tamanho, não à natureza.

## 4. O desenho proposto

### 4.1 O helper deixa de existir como *classe com estado*; sobra caso de uso e função pura

Os quatro papéis do §3.2 se separam:

| Papel de hoje | Para onde vai |
|---|---|
| mutadores (`atualizarX`, `checkY`) | **para o VM**, que é o dono do estado — e encolhem, porque um `UiState` com a forma do agregado tem menos campos que três |
| orquestração (`salvarPassagem`, `isSaving`) | **para o VM**, num único método `emitir()` que devolve **evento** |
| construção do agregado (`montarPassagem`) | **função pura** `paraPassagemDePassageiro(state)` / `paraPassagemDeVeiculo(state)` — testável em JVM, e o tipo fechado obriga a escolher |
| regra (`resolverTarifaBase`, bloqueios) | **morre** (preço é I/O); o que sobrar de regra vira propriedade do tipo (ADR-0023 D3/D4) |

O que **não** desaparece é a necessidade de dividir arquivo: 43 mutadores num VM continuam sendo 43. A saída que
a casa já usa é **extensão do `UiState`** — funções puras `fun FormPassagemUiState.comValorPix(v: String)` — em
vez de uma classe que guarda o handle mutável. O VM continua o único que escreve; o arquivo continua pequeno.

### 4.2 O `UiState` ganha a forma do agregado

Um estado por **categoria**, espelhando o sealed do ADR-0023 — ou um estado com a categoria tipada dentro. Com
isso `isVeiculoChecked` e a limpeza reativa **saem por construção**: não há campos de veículo para limpar
quando a categoria é passageiro.

### 4.3 A navegação volta a navegar

O `onClickAvancar` passa a ser uma chamada: `viewModel.emitir()`. A sequência (validar, marcar, salvar,
desmarcar) fica no VM; o desfecho vira **evento one-shot** que a tela coleta — `Emitida(id)`,
`BloqueadaPor(motivo)`, `Falhou(motivo)` —, e é a tela que decide navegar, rolar ou mostrar mensagem, porque
essas três coisas **são** de apresentação. O `scrollParaErro` desaparece: rolar é reação a evento.

Com isso caem também o `Context` no VM e o `internal lateinit var` exposto.

## 5. O que isto destrava

- **o primeiro teste de ViewModel de emissão**: porta (ADR-0025 D1) + sequência dentro do VM + evento
  observável. Hoje faltam os três;
- **a construção do agregado testável em JVM**: `montarPassagem` é hoje `private suspend` dentro de um helper
  que carrega repositório; vira função pura que recebe estado e devolve o tipo fechado;
- **menos superfície**: três `UiState` → um; 43 delegações → mutadores diretos; quatro papéis → dois.

## 6. As perguntas, e o que o analista respondeu (2026-08-11)

| # | Pergunta | Resposta |
|---|---|---|
| 1 | mutadores como extensões puras × **classes auxiliares** (§4.1) | **classes auxiliares** — mantidas, **sem** o handle mutável e **sem** repositório |
| 2 | um `UiState` por categoria × um só com a categoria dentro (§4.2) | **um `UiState` por categoria** |
| 3 | os eventos `Emitida` / `Bloqueada` / `Falhou` bastam? (§4.3) | **bastam, por hora** |
| 4 | orquestração junto da camada × depois (§4) | **junto com a camada**, e as duas **depois de domínio e dados prontos** |

### O que as respostas 1 e 2 fixam juntas

**A classe auxiliar sobrevive; o que morre é o acoplamento dela.** Ela deixa de receber o `MutableStateFlow` e o
repositório, e passa a ser peça de **transformação**: recebe estado e devolve estado
(`aplicarValorPix(state, valor): FormPassagemUiState`), ou recebe estado e devolve o agregado. O VM continua o
**único que escreve** — chama a auxiliar e guarda o resultado.

Isso preserva o que o helper resolvia de verdade — não deixar 43 mutadores dentro de um ViewModel — e remove o
que ele cobrava: posse difusa do estado e I/O escondido. Cada auxiliar passa a ser testável como função: entra
estado, sai estado.

**Com um `UiState` por categoria**, a auxiliar de veículo opera sobre o estado de veículo e a de passageiro sobre
o de passageiro, sem um terceiro estado carregando o booleano que dizia qual dos dois vale. Os campos comuns
(ocorrência, lançamento, observação) ficam repetidos nos dois — e **esse é o preço aceito**: a alternativa, um
estado único com a categoria dentro, preservaria o risco de meio-preenchido que o ADR-0023 D1 existe para tirar.

### O que a resposta 4 fixa sobre a ordem

A F9 ganha uma **sequência declarada**: domínio → dados → **(camada + orquestração juntas)** → apresentação. As
duas do meio andam na mesma fatia porque mexem nos mesmos arquivos — a porta entra no VM no mesmo movimento em
que a auxiliar perde o repositório. Fazer a orquestração antes dos dados seria reescrever o VM contra a classe
concreta e reescrevê-lo de novo em seguida.