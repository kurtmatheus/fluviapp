# ADR-0026: A orquestração e a apresentação da Passagem — quem escreve o estado, e quem só reage

**Status:** Aceita em direção (decisões do analista em 2026-08-11) · sem código

**Estudos que prepararam:** [`orquestracao-passagem.md`](../design/orquestracao-passagem.md) e
[`apresentacao-passagem.md`](../design/apresentacao-passagem.md)

---

## Contexto

Este ADR fecha os **quatro passos** da Passagem: domínio ([ADR-0023](0023-passagem-por-categoria-e-referencia.md))
→ fronteira ([ADR-0024](0024-fronteira-de-dados-da-passagem.md)) → camada
([ADR-0025](0025-camada-de-dados-da-passagem.md)) → **orquestração e apresentação**. Vêm juntas porque **se
cruzam em dois pontos**: o `UiState` e o evento one-shot — separar produziria dois ADRs que só se entendem lidos
em par.

Os dois estudos mediram onze desvios contra o molde ([ADR-0006](0006-molde-de-cadastro.md)) e contra a trinca
acima. O que eles têm em comum é a origem, e ela não é o domínio: **é o tamanho do formulário**. A prova está
dentro da própria ilha — o `EmbarqueViewModel`, 67 linhas da época do ADR-0012, **está no molde**: um `UiState`,
dono do estado, sem helper, sem `Context`. Qualquer desenho aqui responde ao tamanho, não à natureza.

## Decisão

### D1 — O ViewModel é o único que escreve; as classes auxiliares perdem o handle e o repositório

As auxiliares **ficam** — o problema nunca foi existirem, foi o que recebiam. Hoje o VM declara três
`MutableStateFlow` e **entrega o handle mutável** a três helpers, que também carregam o repositório
(`FormPassagemViewModel.kt:121-136`). Com isso *"quem mudou este campo?"* deixa de ter resposta local, e há I/O
escondido dentro de peças de tela.

Elas passam a ser **transformação**: recebem estado, devolvem estado — `aplicarValorPix(state, valor)` — ou
recebem estado e devolvem o agregado. **O VM chama e guarda o resultado**, e continua sendo o único escritor.

Isso preserva o que o helper resolvia de verdade (não deixar 43 mutadores dentro de um ViewModel) e remove o que
ele cobrava. Cada auxiliar fica testável como função: entra estado, sai estado — sem fake, sem corrotina.

**Some junto o `lateinit`**: hoje os três helpers são `lateinit var` inicializados dentro de um
`viewModelScope.launch`, o que abre uma janela em que o VM existe e eles não. Peça sem estado não precisa de
ciclo de vida.

### D2 — Um `UiState` por categoria

O estado da tela passa a ter a **forma do agregado** (ADR-0023 D1): um `UiState` por categoria, em vez de três
estados coordenados por `isVeiculoChecked`.

Com isso saem **por construção** o `checkVeiculo()` e o `limparCamposPassageiroOuVeiculo()`
(`FormPassagemHelper.kt:33,43`) — limpeza reativa que existia para impedir um estado misto que o domínio já
tornou irrepresentável. Não há campos de veículo para limpar quando a categoria é passageiro.

**O preço, aceito:** os campos comuns (ocorrência, lançamento, observação) ficam repetidos nos estados. A
alternativa — um estado único com a categoria tipada dentro — economizaria a repetição e devolveria o risco do
meio-preenchido, que é exatamente o que o D1 do ADR-0023 foi feito para tirar.

### D3 — A sequência da emissão volta para o ViewModel; a navegação só navega

Hoje o `onClickAvancar` do navcomposable **orquestra**: valida, marca *salvando* mexendo no helper que o VM
expõe (`internal lateinit var`), lança no escopo **da composição**, salva, desmarca e escolhe o desfecho
(`FormPassagemNavComposable.kt:86-102`).

Passa a ser uma chamada — `viewModel.emitir()` —, com a sequência dentro do VM e o desfecho como **evento
one-shot**: `Emitida(id)`, `Bloqueada(motivo)`, `Falhou(motivo)`. **Os três bastam por hora.** A tela **reage**:
navega, rola até o banner, mostra mensagem — as três são decisões de apresentação, e continuam sendo dela.

Morrem três coisas nesse movimento:

- **o `Context` no ViewModel** e o toast como canal de erro de regra (`FormPassagemViewModel.kt:208,227`);
- **o `internal lateinit var formPassagemHelper`**, que era como a navegação alcançava o estado;
- **o `scrollParaErro: Int`** — um contador que existia só para tornar distinguível *"aconteceu de novo"*, que é
  precisamente o que o evento one-shot resolve. Rolar passa a ser reação a evento.

E o ganho que justifica a mudança: **com a porta (ADR-0025 D1) mais a sequência dentro do VM, a emissão fica
testável em JVM** — hoje a porta sozinha daria um VM testável com o fluxo ainda de fora.

### D4 — A emissão é **por etapas**

O eixo é do domínio (categoria como raiz); o desenho é uma **tela em etapas**, não destinos separados por
categoria. É a *emissão por etapas* que o ADR-0018 F7 previa, e a primeira tela do app desenhada a partir de um
**eixo de domínio** em vez de uma lista de campos.

O efeito imediato é mensurável: `FormPassagemScreen` recebe hoje **47 parâmetros**, e a etapa resolve isso de uma
vez — **nenhuma etapa recebe os campos das outras**. A assinatura larga não era excesso de zelo com lambdas; era
o modelo achatado aparecendo na interface, e ele já caiu no domínio.

### D5 — A formatação vive numa **camada fina** de formatadores por tipo

Com o DTO carregando tipo (ADR-0024 D8), formatar passa a ser da apresentação. Ela não se espalha dentro de cada
componente: fica numa **camada fina por tipo** — moeda, documento, data, hora — que as telas chamam. É o que
mantém a regra de exibição num lugar só quando duas telas mostram o mesmo valor, e o precedente é a
`HoraVisualTransformation` da F8: a transformação vive à parte, o componente a usa.

Vale registrar que isto é **trabalho que chega**, não defeito que se corrige: hoje **um único arquivo** de `ui/`
formata (`ContentPagamentoAreaForm`), porque o mapper entregava texto pronto.

### D6 — O argumento de rota vira opcional, e a sentinela `"null"` morre

A rota exige dois argumentos (`…/{idViagem}/{idPassagem}`), e como *emitir* não tem `idPassagem`, quem navega
manda o **texto `"null"`** — para o qual existe uma função dedicada:
`fun String?.isTextoNaoNulo() = this != null && this != "null"` (`StringExtensions.kt:81`).

**A correção já está provada quatro vezes**: Porto, Embarcação, Localidade e Empresa carregam a mesma linha —
*"`""` = criação; id preenchido = edição (arg de rota opcional, sem sentinela `null`)"*. A Passagem é a última
portadora, e o molde (ADR-0006) já chama isso de *arg opcional*.

E há um alcance que só a medição mostrou: a sentinela **atravessou a fronteira** — é ela que decide criar ×
atualizar dentro do repositório (`PassagemFirestoreRepository.kt:115,123`). Com a porta do ADR-0025 (onde
`emitir` não recebe id) e o argumento opcional, **os dois usos somem juntos**, e a extensão pode ser apagada.

### D7 — O que não muda

Dizer isto evita retrabalho, e é metade do valor de um estudo de revitalização:

- **`StatusPassagemBadge`** — deriva do tipo e exibe rótulo; é o que o resto deveria parecer;
- **o scanner do embarque** (`EmbarqueScreen` + `EmbarqueViewModel`) — CameraX + ML Kit, offline, no molde. O
  ADR-0012 está inteiro;
- **a captura do bilhete em Compose** (`graphicsLayer.toImageBitmap()`) — mecanismo bom; muda só o **destino** do
  arquivo, que vai para a galeria (ADR-0017 D5);
- **os `@Preview`** — são a razão de as telas serem testáveis sem Hilt. Cada tela refeita mantém os seus.

### D8 — A ordem: domínio → dados → (camada + orquestração) → apresentação

**Camada e orquestração andam na mesma fatia**, porque mexem nos mesmos arquivos: a porta entra no VM no mesmo
movimento em que a auxiliar perde o repositório. Fazer a orquestração antes dos dados seria escrever o VM contra
a classe concreta e reescrevê-lo em seguida.

**A apresentação vem por último** porque consome as três: só sabe o que exibir depois do DTO tipado, e só sabe a
que reagir depois do evento one-shot.

## Nota lateral — o snapshot como passagem incompleta *(fora da linha da F9)*

Ao responder sobre o botão de salvar cliente, o analista abriu domínio novo: *"um snapshot é uma passagem
incompleta e pode ter vários para o mesmo agente, então terá uma tela de recuperação dessas em preenchimento"* —
*"com garantia do Room"*.

**A decisão vale e três coisas ficam revogadas por ela:** o **D4 do [ADR-0017](0017-eixo-de-storage-firestore-only.md)**
(rascunho → DataStore), a linha do **ADR-0025 D7** que trocava a implementação, e do
**[ADR-0004](0004-snapshot-e-observabilidade-emissao.md)** o **slot único** e a invariante *existe ⇔ é rascunho*.
Consequência de eixo: **o Room não morre inteiro** — e isso *precisa* o Firestore-only em vez de enfraquecê-lo,
porque ele vale para o **fato compartilhado**, e atendimento em curso é local por natureza.

**Construí-la fica fora da F9** (decisão do analista): ela **acrescenta função**, e a F9 existe para recuperar o
que já existia. O que a F9 deve respeitar é só *não apagar o caminho*: a porta `RascunhoStore` fica, o rascunho
fica no Room, e a `Passagem` continua sendo um tipo que **não admite incompleto** — que é o que obrigará o
incompleto a nascer como **outro tipo** quando chegar a vez. Os pontos abertos estão em
[`apresentacao-passagem.md`](../design/apresentacao-passagem.md) §6.3.

## Consequências

**O que se ganha**

- **a emissão fica testável em JVM** (D1+D3), pela primeira vez — sequência no VM, porta injetada, desfecho
  observável;
- **estados ilegais de tela deixam de existir** (D2): não há o que limpar quando não há campo;
- **47 parâmetros deixam de existir de uma vez** (D4);
- **uma extensão inteira desaparece do app** (D6), com os dois usos que ela tinha;
- **a regra de exibição fica num lugar só** (D5).

**O que se paga**

- **repetição de campos comuns** entre os `UiState` por categoria (D2);
- **a formatação inteira migra** para a apresentação (D5) — volume, não risco;
- **as telas se refazem** com as etapas, e cada uma leva os seus `@Preview`;
- **os testes que afirmam a forma antiga** se reescrevem: são parte das 22 classes congeladas.

## O que este ADR não decide

- **O faseamento da F9** — que vem em seguida, com base nos quatro ADRs e nos estudos.
- **A construção do snapshot como passagem incompleta** (nota lateral).
- **A impressão em papel** — surface própria, estudo próprio, fora do MVP.
- **Quantas etapas a emissão tem e o que vai em cada uma** (D4 fixa o eixo, não o roteiro).

## Referências

- [ADR-0006](0006-molde-de-cadastro.md) — o molde que os sete cadastros seguem e que a Passagem passa a seguir
- [ADR-0023](0023-passagem-por-categoria-e-referencia.md) · [ADR-0024](0024-fronteira-de-dados-da-passagem.md) ·
  [ADR-0025](0025-camada-de-dados-da-passagem.md) — os três passos anteriores
- [ADR-0018](0018-agregado-passagem-participantes-modo-e-lancamentos.md) F7 — a emissão por etapas, prevista lá
- [`docs/design/camada-de-apresentacao.md`](../design/camada-de-apresentacao.md) — o estudo transversal da camada,
  de onde vêm os padrões que valem além da Passagem