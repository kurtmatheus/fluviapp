# O bilhete digital — o que existia, o que já foi decidido e o que falta decidir

**Estudo (2026-08-13)** · prepara o ADR do bilhete digital · escrito depois da F9.5 (emissão até o
detalhamento), a pedido do analista: *"em relação ao bilhete digital, gostaria que o usuário também tivesse
uma pré-visualização, porém deve ser no mesmo ato de emitir"*.

---

## 1. Por que este estudo existe agora, e não antes

A F9.2 apagou o bilhete digital junto com o resto do caminho antigo. Isso foi deliberado — *"criar do lado o
novo e ir apagando o antigo"* —, mas deixou um botão sem destino: o passo 6 da emissão oferece **Bilhete
digital** e o `onNavegaParaBilhete` chega vazio ao `FluviAppNavHost` (`FluviAppNavHost.kt`, no
`emissaoNavComposable`).

O analista já delimitou o escopo deste trabalho duas vezes, e as duas valem como restrição:

- **agora**: pré-visualização **no mesmo ato de emitir**;
- **depois, com ADR próprio**: a impressão **física** e as **vias** (navio, agência, cliente), *"para
  encapsular no mesmo escopo"* — ADR-0029 D5.

## 2. O que existia: quatro peças e um índice que não precisava existir

Medido em `582f53b`, o commit anterior à demolição.

| Peça | Tamanho | O que fazia |
|---|---|---|
| `EmissaoPassagemDigitalDialog.kt` | 400 linhas | renderizava o bilhete num `Dialog`, **capturava como imagem** e devolvia o `ImageBitmap` |
| `PassagemDigitalHelper.kt` | 81 linhas | gravava o PNG, guardava o caminho no Room e abria o `ACTION_SEND` |
| `PassagemDIgitalRepository.kt` + `PassagemDigital.kt` | 29 linhas | o índice local `idPassagem → caminho` |
| `ImpressaoPassagem.kt` | 307 linhas | o **outro** caminho: a impressora térmica (fora deste escopo) |

### 2.1 A captura, que era a parte boa

O mecanismo de captura é o que **vale trazer de volta**, e o ADR-0026 D7 já o declarou intocado. Ele resolve
um problema real com duas linhas não óbvias (`EmissaoPassagemDigitalDialog.kt:71-97`):

- **`drawWithContent` + `rememberGraphicsLayer`** grava o que foi desenhado, com fundo branco explícito —
  porque o bilhete é lido claro, independentemente do tema do app;
- **espera o layout acontecer antes de capturar**: `while (graphicsLayer.size.width == 0) withFrameNanos {}`.
  O comentário no código diz o que aconteceu sem isso: *"capturar no 1º frame pega o layout antes do
  posicionamento — daí a imagem saía com elementos sobrepostos"*. É um defeito que já foi pago uma vez.

E uma separação que também vale manter: **`ConteudoPassagemDigital` é `@Preview`-ável**, porque o `Dialog`
não aparece na prévia do Android Studio. O conteúdo visual e a captura são coisas diferentes.

### 2.2 O índice local, que era consequência de um nome ruim

`PassagemDigitalHelper.kt:47` escrevia `passagem_<timestamp>.png` em `getExternalFilesDir(DIRECTORY_PICTURES)`
— **diretório privado do app, que não é a galeria**. O timestamp tornava o arquivo inencontrável a partir do
`idPassagem`, e é isso — e só isso — que obrigava a tabela `PassagemDigital` a existir: ela era o único mapa
`idPassagem → caminho`.

O **ADR-0017 D5** já decidiu o desfecho: nome derivado do `idPassagem`, arquivo na **galeria**, e o índice
morre. Ele acrescenta a regra que fecha o assunto: **arquivo ausente não é erro, é regenerar** — o bilhete é
renderizado a partir da `Passagem`, que está no Firestore; ele nunca foi dado de origem.

### 2.3 Três coisas que o helper fazia e não devem voltar

1. **`runBlocking` em três pontos** (`PassagemDigitalHelper.kt:28,36,64`) — o padrão que a F9 vem apagando;
2. **`Context` dentro da camada lógica**: `emitirPassagemDigital(context, bitmap)` misturava render, escrita
   de arquivo e `startActivity`;
3. **o compartilhamento embutido no salvamento**: gravar e abrir o `ACTION_SEND` eram a mesma chamada, então
   não havia como salvar sem compartilhar nem compartilhar sem regravar.

## 3. O que já está decidido e não se rediscute

| Decisão | Onde | O que significa aqui |
|---|---|---|
| bilhete vai para a **galeria**, nome derivado do `idPassagem` | ADR-0017 D5 | destino e nome mudam juntos; o índice local não volta |
| **arquivo ausente = regenerar** | ADR-0017 D5 | o bilhete é cache de conveniência, não origem |
| o **QR carrega o `idPassagem`** | ADR-0012 | é ponteiro; o embarque resolve ao vivo no servidor |
| a **marca é da agência emissora** | ADR-0015 §5 | logo no topo assinando, logo 2 como marca d'água; sem marca própria, assina o FluviApp |
| **captura em Compose não muda** | ADR-0026 D7 | o mecanismo do `graphicsLayer` volta como está |
| **física e vias têm ADR próprio** | ADR-0029 D5 | este trabalho é só o digital |

E uma que a F9 acrescentou e muda o conteúdo do bilhete: **nada é congelado no agregado** (ADR-0023 D8). O
bilhete antigo lia `DadosPassagem`, um DTO com ~58 campos de texto já formatado; o novo terá de ser montado
pela **junção** (ADR-0025 D3) — e a junção dos participantes existe, é `ColetorDeReferencias.completas`, que
foi escrita na F9.4 e **ainda não tem consumidor**. O bilhete é o consumidor dela.

## 4. O que muda com o pedido novo: pré-visualizar **no ato de emitir**

Hoje o passo 6 (desfecho) diz *"A passagem foi emitida"* e oferece um botão que não leva a lugar nenhum. O
pedido é que ali exista a **pré-visualização** do bilhete.

Isso tem uma consequência de sequência que vale medir antes de decidir: **a pré-visualização é a mesma
renderização que a captura usa**. Se a tela do desfecho já desenha o bilhete, o `graphicsLayer` pode gravar
**o que está na tela** — e não uma segunda renderização escondida num `Dialog`, como era antes. O caminho
antigo desenhava duas vezes: uma no diálogo (para o operador ver) e outra idêntica para capturar.

Três perguntas de ordem prática nascem daí, e são de negócio, não de código:

1. **o arquivo nasce sozinho ou por gesto?** Ver o bilhete e salvá-lo na galeria são a mesma coisa, ou o
   operador vê e decide?
2. **compartilhar continua existindo?** O `ACTION_SEND` era o único caminho para o passageiro receber. Com o
   arquivo na galeria, ele deixa de ser obrigatório — mas continua sendo o gesto que entrega.
3. **o que acontece quando o mesmo bilhete é aberto de novo** (pela lista, adiante): regenera, ou procura o
   arquivo na galeria primeiro? O D5 diz que regenerar é legítimo; falta dizer se é o **primeiro** caminho.

## 5. O que o app ainda tem, e serve

- **`QRCodeGenerator`** (`services/printerservice/qrcode/QrCodeGenerator.kt`) — vivo e intacto;
- **`FileProvider`** declarado no manifesto com `${applicationId}.provider` e `provider_paths.xml`;
- **`marcaDaAgencia`** (`ui/theme/MarcaAgencia.kt`), reapontado nesta semana para as logos **vetorizadas** —
  o que importa aqui mais do que em qualquer outra tela, porque o bilhete **vira imagem**: um PNG de origem
  escalado chega serrilhado ao arquivo que o passageiro guarda;
- **`ConfirmacaoDaEmissao`** e o `DetalhamentoDaEmissao` (F9.5) — a conferência já monta e exibe quase tudo
  o que o bilhete mostra. A diferença entre os dois documentos é de **destinatário**, e ela precisa ser dita.

### 5.1 Uma coisa que o `WRITE_EXTERNAL_STORAGE` do manifesto revela

`AndroidManifest.xml:9` ainda declara `WRITE_EXTERNAL_STORAGE`, permissão que **não vale desde a API 29** e
que o app pede sem usar. Escrever na galeria hoje é `MediaStore` com `RELATIVE_PATH`, **sem permissão
nenhuma** — o que significa que o caminho novo é mais simples que o antigo, e que a linha do manifesto é
resíduo a remover junto.

## 6. As decisões que faltam (para o ADR)

1. **quando o arquivo nasce** — ao ver, ou por gesto explícito (§4.1);
2. **se o compartilhar continua**, e se ele é o gesto principal ou secundário (§4.2);
3. **o que o bilhete mostra**, que é a pergunta de fundo: ele é o **documento do passageiro**, e o
   detalhamento é a **conferência do operador**. Documento oculta ou mostra o número do documento? A
   máscara que a revisão de UI/UX pediu para o detalhamento vale aqui — ou aqui é o oposto, já que o bilhete
   vai para a mão de quem já sabe o próprio número?
4. **se o desfecho substitui ou acompanha** a tela de detalhes de uma passagem já emitida (que ainda não
   existe, e é da mesma família);
5. **reabertura**: regenerar sempre, ou procurar antes (§4.3).

## 7. O que este estudo não mede

- **a impressão térmica** (`ImpressaoPassagem.kt`, 307 linhas no histórico) e as **vias** — ADR próprio, por
  decisão do analista;
- **a tela de detalhes de passagem emitida** e a **pesquisa**, que voltam na F9.6 ou depois;
- **o custo de armazenamento** de imagens na galeria: sem consumidor planejado que o pague, medir aqui seria
  a projeção que o ADR-0025 recusa por método.