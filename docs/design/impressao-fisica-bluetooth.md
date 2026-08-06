# Estudo — a impressão física por Bluetooth

> **Estado: aberto.** Mapeia o que existe, com arquivo e linha, e prepara decisão. **Nada aqui muda o
> MVP**: a impressão física não está no escopo da revitalização e não abre seção no menu. O documento
> existe para que, quando a vez dela chegar, a conversa comece de um retrato e não de uma lembrança.
>
> Escrito em 2026-08-05, contra o código em `e879b22`.

## 1. Onde isso vive

Sete arquivos, em três camadas que não se conhecem por porta nenhuma:

| Camada | Arquivo | Papel |
|---|---|---|
| Transporte | `services/printerservice/printer/Printer.kt` | a **única** abstração do assunto: `open/write/close` |
| Transporte | `services/printerservice/printer/ThermalPrinterConnection.kt` | socket RFCOMM sobre `BluetoothDevice` |
| Protocolo | `services/printerservice/Commands.kt` | 90 linhas de constantes ESC/POS |
| Protocolo | `services/printerservice/PrinterService.kt` | 331 linhas: fontes, tamanhos, alinhamento, código de barras, corte, gaveta, bipe, imagem |
| Protocolo | `services/printerservice/image/Image.kt` | bitmap → raster de 24 linhas (`SELECT_BIT_IMAGE_MODE`) |
| Protocolo | `services/printerservice/qrcode/QrCodeGenerator.kt` | ZXing → `Bitmap` |
| Conteúdo | `business/ImpressaoPassagem.kt` | monta **o bilhete** como uma `String` gigante |
| Orquestração | `ui/viewmodel/helpers/ImpressaoHelper.kt` | descobre impressora, imprime, muda o status da passagem |
| Estado | `ui/states/ImpressaoState.kt` | flags de tela + **dois `var` no companion** |

Entrada pela tela: `DetalhesPassagemNavComposable.kt:79-85` (via do cliente) e `:85` / `DetalhesPassagemScreen.kt:230` (via da empresa, por diálogo).

## 2. O fluxo, ponta a ponta

1. `onClickImpressaoFisica` → `atualizarViaCliente(true)` → `validarImprimir(context)`
   (`ImpressaoHelper.kt:65`);
2. sem impressora escolhida, `verificarImpressoras` varre `bluetoothAdapter.bondedDevices` e filtra
   `majorDeviceClass == 1536` (`:159`); lista vazia → joga a pessoa nas **configurações de Bluetooth do
   sistema** (`:171`);
3. escolhida, `imprimir` cria `ThermalPrinterConnection` e `PrinterService`, cujo `init` **abre o socket**
   e envia `HW_INIT` (`PrinterService.kt:17-20`);
4. `ImpressaoPassagem.getComandoImpressao()` devolve o bilhete inteiro como texto com bytes ESC/POS
   embutidos (`ImpressaoPassagem.kt:294`), enviado num `printLn` só;
5. depois vão QR (`printQRCode`, 200 px) e rodapé de operador/via;
6. toast de sucesso, `close()`, e `atualizaSituacao()` transiciona a passagem para `EMITIDA`.

Tudo isso dentro de **dois `runBlocking`** (`ImpressaoHelper.kt:32` e `:85`), na thread que chamou.

## 3. O que já está certo

Antes da lista de defeitos, o que não se deve perder numa eventual reescrita:

- **`Printer` é uma porta de verdade** (`open/write/close`), e é o que permitirá um renderizador de
  mentira sem tocar em nada acima dela;
- **o ESC/POS está tabelado e comentado** (`Commands.kt`) — é conhecimento de protocolo já pago;
- **o raster de imagem funciona** e é o caminho pelo qual o QR chega ao papel;
- **o QR do bilhete é o mesmo do embarque** (ADR-0012): o papel participa do ciclo de vida, não é enfeite.

## 4. As falhas, por consequência

### F1 — O sucesso mentiroso *(a mais grave)*

`ThermalPrinterConnection.open()` engole `IOException` e deixa `isOpen = false`
(`ThermalPrinterConnection.kt:29`). Em `imprimir`, o bloco de escrita é condicionado a `isOpen`
(`ImpressaoHelper.kt:88`), **mas o toast de sucesso e a transição de status estão fora do `if`**
(`:98` e `:102`).

Impressora desligada, fora de alcance ou pareada com outro aparelho: nada sai no papel, a pessoa lê
*"Emissão bem sucedida"* e **a passagem vira `EMITIDA`**. O bilhete existe no sistema e não existe na mão
do passageiro — e, pelo ADR-0012, `EMITIDA` é irreversível para trás.

O mesmo `write` engole `IOException` silenciosamente (`:38`): papel acabou no meio, metade do bilhete saiu,
ninguém soube.

### F2 — Bluetooth na thread da UI

`runBlocking` em `:32` e `:85` executa conexão RFCOMM e escrita **na thread chamadora**. `connect()` de
socket Bluetooth bloqueia por segundos e pode ficar preso até o timeout do stack — é ANR à espera de uma
impressora lenta. Nada aqui é `suspend`; `Printer` não precisa mudar de forma para virar suspenso, mas
`PrinterService.init` abrir o socket **no construtor** impede que a abertura vá para um dispatcher sem
inverter a construção do objeto.

### F3 — Acentos

Duas camadas concorrem para estragar a mesma coisa:

- `setCharCode()` existe (`PrinterService.kt:278`) e **nunca é chamado** — a impressora fica na tabela de
  fábrica, que raramente é a portuguesa (`CHARCODE_PC860`);
- o conteúdo trafega como `String` e é convertido com `text.toByteArray()` (`:23`), que usa **UTF-8**;
  impressora térmica espera **uma tabela de 8 bits**. "Belém" sai como `Belém`.

Os bytes de comando sobrevivem porque são todos `< 0x80` (o truque de `String(TXT_ALIGN_CT)` funciona por
sorte, não por desenho — ver F8).

### F4 — O papel não é cortado

`cutPart()` / `cutFull()` (`:121`, `:125`) **nunca são chamados**. Junto com eles ficam de fora os cinco
avanços de linha que o `cut()` faz antes (`:130`) — ou seja, o bilhete termina colado no cabeçote e a
próxima impressão começa emendada. Hoje quem corta é a mão do operador na serrilha, no lugar onde o papel
parou.

### F5 — "VIA DO EMBARCACAO" impresso no papel

`ImpressaoPassagem.kt:59`:

```kotlin
const val VIA_EMPRESA = "VIA DO EMBARCACAO"
```

É dano do `sed` do rename `Navio` → `Embarcacao` (commit `4694a54`) que **atravessou até o papel**: a
constante chama-se `VIA_EMPRESA`, o texto dizia "VIA DO NAVIO", e hoje imprime uma concordância errada. É
a mesma família de erro que o `TipoEmbarcacao.NAVIO` sofreu e que já foi corrigida no domínio — este ficou.

### F6 — "Valor Pago" imprime o valor a pagar

`ImpressaoPassagem.kt:141` imprime `dadosPassagem.valorAPagar` sob o rótulo `LABEL_VALOR_PAGO`. Não é
descuido de digitação: **`DadosPassagem` não tem campo de valor pago** — 58 campos, e nenhum deles. O
bilhete afirma um recebimento que o sistema não registrou.

Some com o ADR-0018 D11 (lançamentos), que é onde o valor pago passa a existir de verdade.

### F7 — O terceiro passageiro leva o documento do segundo

`ImpressaoPassagem.kt:176`: o passageiro 3 é impresso com `tipoDocumentoPassageiro2`. Copiar/colar, e sem
teste que o pegue.

### F8 — A régua de 31 colunas

- `LINE_TOTAL_LENGTH = 31` é **constante** (`:17`): assume bobina de 58 mm em fonte A. Bobina de 80 mm
  (48 colunas) desalinha tudo;
- `alignRightCampo` (`:277`) usa `for (i in 0..offLabel step 1)`, que produz **`offLabel + 1`** espaços —
  um a mais, sempre;
- não há **truncamento**: campo maior que a linha produz `offLabel` negativo, nenhum espaço, e a impressora
  quebra a linha onde quiser;
- o cálculo mistura contagem de caracteres com bytes de comando: `String(TXT_ALIGN_CT)` vira 3 caracteres
  invisíveis que **não contam** como coluna, mas contam como `length` se alguém medir a string montada.

### F9 — A impressora escolhida é estado global

`ImpressaoState.kt:13-16` guarda `isPrinterSelected` e `impressoraSelecionada` no `companion object`:

- **não persiste**: morre com o processo, e a pessoa reescolhe a impressora a cada abertura do app;
- **não é por usuário**: sobrevive ao logout, no aparelho compartilhado de uma agência;
- **não é observável**: `DetalhesPassagemScreen.kt:218` lê `ImpressaoState.isPrinterSelected` *dentro de um
  composable*, sem ser estado do Compose — a tela não recompõe quando ele muda, e o diálogo só some porque
  a outra flag (`exibirDialogImpressoras`) é do `UiState`.

### F10 — `lateinit context` e o caminho digital

`ImpressaoHelper.context` é `lateinit` e só recebe valor em `validarImprimir` (`:68`). Mas
`atualizaSituacao(ehEmissaoDigital = true)` é chamada pelo caminho **digital**
(`DetalhesPassagemNavComposable.kt:100`), que nunca passou por lá. Se a transição falhar, o `catch` toca
`context` (`:39`) e o app morre com `UninitializedPropertyAccessException` — um crash que só aparece
quando **outra** coisa já deu errado.

### F11 — Permissões e o `@RequiresApi(S)` contagioso

- `AndroidManifest.xml:5-6` declara `BLUETOOTH` e `BLUETOOTH_CONNECT` sem `maxSdkVersion` nem
  `usesPermissionFlags` — a legada continua pedida em API 31+, e a nova é pedida em aparelhos 26–30 onde
  não existe;
- `MainScreenNavComposable.kt:56` pede as duas em runtime **ao abrir o painel**, com callbacks vazios em
  `onGranted`/`onDenied`: negar não muda nada, e o efeito só aparece lá na impressão, como
  `SecurityException` engolida;
- por causa disso, `@RequiresApi(Build.VERSION_CODES.S)` marca `MainActivity`, `FluviAppNavHost`,
  `MainScreenGraphNavigation` e `MainScreenNavComposable` — **quatro arquivos de navegação anotados com
  API 31 num app de `minSdk 26`**, por causa de um recurso que hoje está dormente. É o achado que o estudo
  da camada de apresentação já registrava;
- `@SuppressLint("MissingPermission")` em `ThermalPrinterConnection.kt:22` e `ImpressaoHelper.kt:151`
  silencia o lint **e** o problema.

### F12 — A descoberta que não explica

`majorDeviceClass == 1536` (`ImpressaoHelper.kt:160`) é `0x600`, a classe **IMAGING** — número mágico, sem
constante. Impressoras que se anunciam como `UNCATEGORIZED` (comum em modelos genéricos) **nunca aparecem
na lista**, e o app responde a isso abrindo as configurações de Bluetooth do sistema, sem dizer por quê. O
sintoma que chega ao suporte é *"minha impressora está pareada e o app não acha"*.

### F13 — Zero teste

Nenhum arquivo em `app/src/test` cobre `printerservice`, `ImpressaoPassagem` ou `ImpressaoHelper`. Nada
aqui é testável hoje sem aparelho, e é por isso que F5, F6 e F7 sobreviveram: são erros de **conteúdo**,
que um teste de string pegaria em um segundo.

## 5. Inconsistências com o domínio de hoje

O bilhete impresso é um retrato do domínio de 2024, e envelheceu junto com ele:

| No papel | Hoje o domínio diz |
|---|---|
| `acomodacao` (String) e `ehVeiculo = placa.isNotBlank()` | **`ModoPassagem`**, eixo de quatro valores (ADR-0018 D6). `DadosPassagem.kt:69` já faz `ModoPassagem.de(acomodacao)` — a ida e volta pelo texto sobrou |
| `tipoVeiculo` (String) | `ClasseVeiculo` (ADR-0018 D7) |
| `agencia` como campo do bilhete | derivada do **emissor** (ADR-0015 P2.3) — e só é impressa na via da empresa (`:227`) |
| `tarifa` só quando `ehRede` (`:134`) | a tarifa passa a ser **inferida** (ADR-0016 §7.2), e a régua vale para todo modo |
| "VIA DO EMBARCACAO" | a via é **da empresa**; a embarcação é o ativo |
| Tipo de passagem e gratuidade **comentados** (`:157-162`, `:198`) | são categorias tarifárias vivas (ADR-0013) — o papel não diz se a passagem é meia |

## 6. Como isso melhora — o desenho que resolve a raiz

Todas as falhas de conteúdo (F5–F8) têm a mesma causa: **o bilhete é montado como texto com bytes de
comando dentro**. Não há como olhar para ele senão imprimindo, e não há como testá-lo senão comparando
strings que contêm caracteres de controle.

A saída é um **documento intermediário** — uma lista de blocos que descreve *o que* o bilhete diz, sem
saber como se imprime:

```
BilheteImpresso = List<Bloco>
  Bloco.Texto(conteudo, alinhamento, ênfase, tamanho)
  Bloco.ParRotuloValor(rotulo, valor)        ← quem alinha à direita é o renderizador
  Bloco.Separador
  Bloco.QrCode(conteudo)
  Bloco.Corte
```

Com ele, três coisas passam a ser possíveis e hoje não são:

1. **dois renderizadores sobre a mesma fonte** — o de ESC/POS (o que existe, reorganizado) e o de tela
   (§7). O papel e a prévia deixam de poder divergir, porque nascem do mesmo objeto;
2. **teste de conteúdo em JVM**: `montarBilhete(dadosPassagem)` é função pura de dados para blocos — F5,
   F6 e F7 viram três asserções;
3. **largura como parâmetro**, e não constante: 32 ou 48 colunas entram no renderizador, e o alinhamento
   passa a ser calculado sobre colunas de texto, sem bytes de comando no meio da conta.

O transporte melhora em paralelo, e independentemente: `Printer` vira `suspend`, `open()` sai do
construtor de `PrinterService`, o resultado da escrita vira um tipo (`Impresso` / `FalhouAoAbrir` /
`FalhouNoMeio`) em vez de `Unit` com exceção engolida — e aí F1 deixa de ser possível, porque *sucesso*
passa a ser uma resposta que alguém precisa devolver.

## 7. O preview em Compose — é viável, e é barato

**Sim, dá para simular a impressão térmica em Compose, e o custo é pequeno depois do §6** — sem ele, é
reescrever o parser do que hoje é uma string com bytes dentro; com ele, é um `@Composable` que percorre
uma lista de blocos.

O que a simulação **entrega com fidelidade alta**:

- **contagem de colunas** (32 ou 48), com `FontFamily.Monospace` — é o que expõe truncamento e
  desalinhamento antes do papel;
- **quebra de linha real**, porque o texto é medido na mesma régua do renderizador ESC/POS;
- **ênfase e tamanho**: negrito é negrito; dobro de altura/largura é escala de fonte;
- **alinhamento** (esquerda, centro, direita) e as linhas em branco — que é justamente onde o bilhete de
  hoje erra;
- **o QR**, gerado pelo mesmo `QRCodeGenerator`, na proporção que ele terá no papel;
- **a bobina**: largura fixa em `dp` proporcional às colunas, fundo levemente creme, serrilha desenhada no
  fim — o suficiente para o olho julgar o conjunto.

O que ela **não** entrega, e precisa estar dito para não virar falsa confiança:

- **densidade térmica e contraste**: papel velho, cabeçote sujo e `setTextDensity` mudam o resultado real;
- **a fonte da impressora**: cada modelo tem a sua fonte A/B; o monoespaçado da tela é uma aproximação de
  métrica, não a mesma tipografia;
- **o comportamento do firmware**: alguns modelos ignoram comandos (corte parcial, sublinhado duplo),
  outros interpretam a tabela de caracteres à sua maneira — F3 é justamente disso;
- **o papel acabando no meio.**

Em tempo real, o caminho natural é a prévia **ao lado do formulário de emissão**: o `UiState` já é a fonte,
o `montarBilhete` é puro, e a recomposição faz o resto — muda o nome do passageiro, o papel na tela muda
junto. É também a forma mais barata de ajustar layout: hoje cada ajuste de espaçamento custa uma bobina e
uma viagem até a impressora.

E há um efeito colateral que vale mais que a prévia: **um renderizador em Compose é um caminho para o
bilhete virar imagem** — e imagem, o app já sabe imprimir (`printImage`) e já sabe salvar na galeria
(ADR-0017 D4). Renderizar o bilhete inteiro como bitmap resolve acentos e fonte de uma vez, ao preço de
uma impressão mais lenta e de mais bytes no ar. É uma opção de rota, não uma recomendação — fica registrada
porque a decisão entre *texto com tabela de caracteres certa* e *imagem* é o cruzamento principal deste
assunto.

## 8. O que dá para testar sem impressora

- **conteúdo**: `montarBilhete(dados)` → blocos, com asserções sobre rótulos, valores e presença do QR;
- **largura**: nenhuma linha ultrapassa N colunas, para os dois N;
- **bytes**: renderizador ESC/POS com um `Printer` fake que acumula em `ByteArrayOutputStream` — *golden
  test* do fluxo, que trava regressão de comando (o `HW_INIT`, o corte no fim, a tabela de caracteres no
  começo);
- **falha**: fake que lança na abertura → o resultado precisa ser `FalhouAoAbrir`, **sem** transição para
  `EMITIDA`. É o teste que impede F1 de voltar.

Nada disso pede aparelho, e é o oposto do que existe hoje.

## 9. O que este estudo não decide

Quatro perguntas, na ordem em que mudam o resto:

1. **Texto ou imagem?** Renderizar o bilhete como bitmap resolve acento e fonte de uma vez, mas troca
   velocidade e bytes por fidelidade. Texto mantém a impressão rápida e obriga a acertar a tabela de
   caracteres por modelo.
2. **Uma bobina ou duas?** Hoje 31 colunas é lei. Suportar 58 e 80 mm muda o modelo (largura vira
   parâmetro) e a tela de configuração (alguém precisa dizer qual é).
3. **A impressora é do aparelho ou do usuário?** Persistir a escolha no DataStore é trivial; a pergunta é
   se ela deve sobreviver ao logout num aparelho de agência compartilhado.
4. **A via da empresa continua existindo?** Ela imprime agência e observação (`:227`), dados que hoje
   derivam do emissor. Com o bilhete digital e o QR de embarque, talvez a segunda via seja hábito de papel
   e não requisito.

E uma que não é pergunta: **F1 é defeito, não decisão.** Quando a impressão voltar ao escopo, é o primeiro
a cair — imprimir nada e dizer que imprimiu é a única falha desta lista que produz passageiro sem bilhete
com a passagem já marcada como emitida.