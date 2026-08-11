# A apresentação da Passagem — navegação e UI contra o que já foi decidido

> **Status:** **aberto** — mede a apresentação como ela existe em `cdf15b3` (2026-08-11) e a confronta com as
> decisões já tomadas. É o **último dos quatro passos** da Passagem: domínio
> ([ADR-0023](../adr/0023-passagem-por-categoria-e-referencia.md)) → fronteira
> ([ADR-0024](../adr/0024-fronteira-de-dados-da-passagem.md)) → camada
> ([ADR-0025](../adr/0025-camada-de-dados-da-passagem.md)) → **orquestração**
> ([orquestracao-passagem.md](orquestracao-passagem.md)) e apresentação.
>
> Complementa o estudo transversal [camada-de-apresentacao.md](camada-de-apresentacao.md), que mapeia a camada
> **inteira**; aqui só a Passagem, e sempre com a decisão que corrige ao lado. A **impressão em papel** fica
> fora: tem estudo próprio ([impressao-fisica-bluetooth.md](impressao-fisica-bluetooth.md)) e está fora do MVP.

## 1. O mapa

**Navegação** — 6 navcomposables e um grafo:

| Peça | Linhas |
|---|---|
| `FormPassagemNavComposable` | 100 |
| `DetalhesPassagemNavComposable` | 106 |
| `FormPesquisarPassagemNavComposable` · `ResultPassagemNavComposable` | 38 · 30 |
| `EmbarqueNavComposable` · `ContagemPassagemNavComposable` | 24 · 30 |
| `PesquisarPassagemGraph` | 39 |

**UI** — as telas e os componentes que só a Passagem usa:

| Peça | Linhas | O que é |
|---|---|---|
| `ContentPagamentoAreaForm` | 424 | a área de pagamento; **o único arquivo de `ui/` que formata moeda** |
| `EmissaoPassagemDigitalDialog` | 366 | o bilhete digital: desenha em Compose e **captura como imagem** |
| `FormPassagemScreen` | 380 | a maior tela do app — **47 parâmetros** |
| `DetalhamentoPassagemContent` | 312 | o detalhe, consumindo `DadosPassagem` |
| `ContentPassageiroAreaForm` | 309 | área de passageiros (1..3) |
| `DetalhesPassagemScreen` | 284 | a tela de detalhe |
| `EmbarqueScreen` | 252 | scanner + confirmação |
| `PassagemPreviewCard` | 189 | o card da listagem |
| `ContentAreaVeiculoForm` | 169 | área de veículo |
| `ContagemEmbarcacaoCard` · `ContagemPassagemScreen` | 141 · 122 | ocupação |
| `FormPesquisarPassagemScreen` · `ResultadosPassagemSearchScreen` | 133 · 123 | busca e resultado |
| `StatusPassagemBadge` | 71 | badge do status (ADR-0012) — **está bom, não muda** |

## 2. Os cinco desvios, medidos

### 2.1 A rota exige o argumento que não existe, e nasceu daí uma função para mentir

```kotlin
// FormPassagemNavComposable.kt:28
route = "…/{idViagem}/{idPassagem}"          // dois argumentos OBRIGATÓRIOS
// FormPassagemViewModel.kt:70
private val idPassagem: String = checkNotNull(savedStateHandle[EDIT_PASSAGEM_ARGUMENT])
```

Como *emitir* não tem `idPassagem`, quem navega manda o texto **`"null"`** — e existe uma extensão só para
desfazer isso:

```kotlin
// StringExtensions.kt:81
fun String?.isTextoNaoNulo(): Boolean = this != null && this != "null"
```

**A correção já está provada quatro vezes.** Porto, Embarcação, Localidade e Empresa carregam a mesma linha de
comentário — *"`""` = criação; id preenchido = edição (arg de rota opcional, sem sentinela `null`)"* — porque a
revitalização trocou o argumento obrigatório por **opcional**. A Passagem é a **última** portadora da sentinela,
e ela é o que o [ADR-0006](../adr/0006-molde-de-cadastro.md) chama de *arg opcional*.

Efeito colateral que vale nomear: `isTextoNaoNulo` é usada **também na camada de dados**
(`PassagemFirestoreRepository.kt:115,123`) para decidir criar × atualizar. Ou seja, **uma decisão de navegação
atravessou até a escrita no Firestore**. Com a porta do ADR-0025 (`emitir` não recebe id) e o argumento
opcional, os dois usos somem juntos.

### 2.2 A tela tem 47 parâmetros, e a razão é o modelo antigo

`FormPassagemScreen` recebe **47** coisas: três `UiState`, um contador de rolagem, dois cliques e ~41
`on*Change`. O `NavComposable` que a monta é uma parede de 60 linhas de `viewModel::…`
(`FormPassagemNavComposable.kt:43-83`).

Não é excesso de zelo em passar lambda: é o **modelo achatado aparecendo na assinatura**. Um `on*Change` por
campo, e os campos são três passageiros × quatro dados, quatro formas de pagamento × valor, cinco do veículo. O
ADR-0023 desfez essa planura no domínio (lista de clientes, lançamentos, categoria como tipo), e o
[estudo da orquestração](orquestracao-passagem.md) §4.2 propõe o `UiState` com a forma do agregado. **A
assinatura da tela é consequência das duas coisas** — corrigir aqui sem corrigir lá é trocar 47 parâmetros por
47 parâmetros de outro nome.

### 2.3 O bilhete digital é uma tela que também é um arquivo

`EmissaoPassagemDigitalDialog` (366 linhas) desenha o bilhete em Compose, gera o QR e **captura o resultado como
imagem** (`graphicsLayer.toImageBitmap()`, `:96`), entregando-a por um callback
(`onProcessaImageBitmap: (ImageBitmap) -> Unit`).

O mecanismo é bom e **fica** — desenhar em Compose e capturar evita um segundo renderizador. O que muda é o
**destino**: pelo ADR-0017 D5 o arquivo vai para a **galeria**, com nome derivado do `idPassagem`, e o índice em
tabela (`PassagemDigital`) **deixa de existir** (ADR-0025 D7). E o dialog é um dos consumidores que o ADR-0025
D4 divide: hoje ele recebe `DadosPassagem`, o DTO de ~58 campos que serve quatro telas ao mesmo tempo.

### 2.4 Quem formata é a camada de dados — e isso se inverte

Em todo o `ui/` da Passagem, **um único arquivo formata**: `ContentPagamentoAreaForm` (moeda, na entrada). Todo
o resto recebe texto já pronto, porque o mapper formata tudo — moeda, documento, rótulo de status.

O **ADR-0024 D8 inverteu isso**: o DTO carrega **tipo**, e formatar passa a ser da apresentação, que é a única
camada que sabe *para quem* está formatando. Então este item não é defeito de UI: é **trabalho que chega**. Vale
medir para não subestimar a fatia — a formatação hoje espalhada em mappers vai reaparecer em Compose, e o lugar
certo para ela é junto do componente, não numa nova pilha de `String`.

### 2.5 A navegação decide o que é de tela (e o §3.3 da orquestração é o mesmo achado visto daqui)

O `onClickAvancar` (`FormPassagemNavComposable.kt:86-102`) escolhe entre **navegar**, **rolar até o erro** e
**mostrar toast** — as três são decisões de apresentação, e estão certas *como decisões*; o que está errado é
**onde** elas moram e **de onde** vêm os dados que as sustentam: um `Int` contador (`scrollParaErro`) e uma
espiada em `viewModel.uiStatePassagem.value.emissaoBloqueadaMsg`.

Com o **evento one-shot** proposto na orquestração (§4.3), a tela passa a **reagir** — `Emitida(id)` → navega,
`Bloqueada(motivo)` → rola e mostra o banner, `Falhou(motivo)` → mensagem — e o contador desaparece. É a mesma
correção vista do outro lado da fronteira.

## 3. O que as decisões já tomadas trazem para a UI

| Decisão | O que aparece na tela |
|---|---|
| **ADR-0023 D1** — categoria é a raiz | a emissão deixa de ser *um form com um checkbox de veículo* e passa a ter **um caminho por categoria**. É a **emissão por etapas** que o ADR-0018 F7 previa — e o estudo transversal já anotou: *a primeira tela desenhada a partir de um eixo de domínio*, não de uma lista de campos |
| **ADR-0023 D3** — acomodação limita o tipo tarifário | o seletor de *meia/gratuidade* não precisa ser escondido por lógica de tela: ele **não existe** fora da rede |
| **ADR-0023 D4** — o tipo de veículo governa | *modelo* deixa de ser pedido para carreta e caminhão, e *cilindrada* só aparece para moto — por propriedade do tipo, não por `if` na tela |
| **ADR-0023 D5** — `Cliente` com telefone | um campo novo, e o **botão de salvar cliente** no meio do preenchimento (ADR-0018 D2/D3) |
| **ADR-0024 D8** — DTO com tipo | a formatação chega (§2.4) |
| **ADR-0025 D4** — DTO por consumidor | `DadosPassagem` se divide, e cada tela para de receber campos que não usa |
| **ADR-0018 D15** — máscara de placa | precedente pronto e recente: `mascararHora` + `HoraVisualTransformation` da F8, onde **o separador é pintura e o campo guarda dígito** |

Sobre a última linha, vale registrar a lição por escrito, porque ela custou uma versão de homologação: o campo
de hora pedia `HH:mm` com teclado numérico — **e o teclado numérico do Android não tem `:`**. A placa tem a
mesma forma de problema (dois padrões, um com traço), e a solução é a mesma: **máscara na entrada, separador
desenhado**, com teste de tela cobrindo o cursor.

## 4. O que fica como está

Nem tudo é correção, e dizer o que não muda evita retrabalho:

- **`StatusPassagemBadge`** (71 linhas) — deriva do tipo, exibe rótulo; é o que o resto deveria parecer;
- **o scanner do embarque** (`EmbarqueScreen`, 252) — CameraX + ML Kit, offline, com o `EmbarqueViewModel` no
  molde. O ADR-0012 está inteiro e **não se refaz** (a dívida do CameraX 1.3.4 para 16 KB é de *build*, não de
  UI);
- **a captura do bilhete em Compose** (§2.3) — muda o destino do arquivo, não o mecanismo;
- **os `@Preview`** — o app tem 60, e eles são a razão pela qual as telas são testáveis sem Hilt. Cada tela
  refeita mantém os seus.

## 5. Perguntas ao analista

1. **A emissão por categoria é uma tela com etapas ou telas separadas?** (§3, 1ª linha) — o eixo é do domínio, o
   desenho é seu: um fluxo em passos com a categoria escolhida no início, ou destinos distintos na navegação
   para passageiro e veículo?
2. **Onde a formatação passa a morar** (§2.4): junto do componente que exibe, ou numa camada fina de
   *formatadores* por tipo (moeda, documento, data) que as telas chamam?
3. **O botão de salvar cliente** (§3, 4ª linha) fica no meio do preenchimento, como o ADR-0018 D2 previu — ou a
   emissão salva o cliente **junto** com o bilhete, sem botão?
4. **A ordem de correção**: a apresentação entra na mesma fatia da orquestração (as duas se cruzam no evento
   one-shot e no `UiState`) ou depois dela?