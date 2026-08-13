# ADR-0029: Os fluxos da emissão — um totem, e o roteiro que a escolha desenha

**Status:** Aceita em direção (decisões do analista em 2026-08-13) · implementação na F9.5

**Supera:** o **D4 do [ADR-0028](0028-as-etapas-da-emissao.md)** — *"três passos"* —, escrito horas antes.
O que muda não é o princípio, é a **granularidade**: os passos deixam de ser três blocos e passam a ser
**muitos passos pequenos**, um por decisão, cujo roteiro **depende do que se escolheu**. Continuam valendo
integralmente os D1, D2, D3, D5 e D6 daquele ADR.

---

## Contexto

O ADR-0028 organizou a emissão em três passos porque essa era a tradução direta do formulário antigo:
bilhete, quem viaja, pagamento. Ela resolvia o problema declarado (os 47 parâmetros), mas mantinha uma
premissa que o analista desfez: a de que cada passo é **uma tela com campos**.

A premissa nova é outra: **um totem de restaurante**. Cada tela faz **uma** pergunta, quase sempre com
botões, e o operador atravessa a emissão tocando — não preenchendo. Formulário só onde há dado que **só se
digita**: a pessoa e o veículo.

## Decisão

### D1 — Cada passo é uma pergunta, e a resposta é um toque

*"Os comportamentos são orientados ao evento… resguardados pela menor quantidade de campo preenchido
possível, como se fosse um totem de restaurante."*

Três consequências diretas:

- **botão no lugar de campo** sempre que o domínio já enumera as respostas — e ele enumera quase todas:
  categoria, acomodação, tipo tarifário, subtipo de gratuidade, classe de veículo são **enums**, não texto
  livre. Um `dropdown` obriga a abrir, ler e escolher; um botão é a escolha;
- **ícones**, *"para melhor renderização e melhorar a memória do app"* — e há um ganho além do desenho: a
  bilheteria de beira de rio opera em aparelho modesto, e um alvo grande com ícone é mais rápido de acertar
  do que uma linha de lista;
- **erro de preenchimento quase desaparece**: onde não há campo, não há valor inválido. O que sobra de
  validação (ADR-0028) fica concentrado nos dois formulários que restaram.

### D2 — Os dois fluxos, e os passos que eles compartilham

**Passo 1 — comum:** dois botões, **Passageiro** ou **Veículo**. É a categoria, que é a raiz (ADR-0023 D1).

| | **Passageiro** | **Veículo** |
|---|---|---|
| **2** | três botões: **rede**, **suíte**, **camarote** | **classe do veículo**, em lista vertical de botões |
| **3** | **3.1** rede → três botões de **tipo tarifário**<br>**3.1.1** se gratuidade → botões de **subtipo**<br>**3.2** suíte/camarote → três botões: **1, 2 ou 3 pessoas** | **form do veículo**, rearranjado conforme a classe |
| **4** | **form de cliente**, um por pessoa contada — **salvando cada um** | **form do cliente responsável pela retirada** — **opcional, com botão de pular** |
| **5** | lançamentos e observação | lançamentos e observação |
| **6** | **resolver a emissão** | **resolver a emissão** |

**O passo 4 é "cliente" nos dois fluxos**, e isso é decisão de desenho, não coincidência: *"assim melhora e
mantém o princípio dos passos com o cliente sincronizando como 4 nos dois"*. Quem opera aprende **uma**
sequência, e a diferença entre os fluxos fica nos passos 2 e 3, que é onde ela é real.

Três observações que o roteiro torna explícitas:

- **a quantidade de pessoas é pergunta só na suíte e no camarote.** Na rede ela não existe: rede é
  `ocupacaoMaxima = 1`, então perguntar seria oferecer uma escolha que não há;
- **o subtipo de gratuidade é um passo**, e não um campo dentro do passo do tipo. Ele só aparece quando a
  resposta anterior foi *gratuidade* — que é a forma de o ADR-0028 D2 (*gratuidade sem subtipo é incoerente*)
  virar impossível em vez de validado;
- **no veículo, o form vem depois da classe** porque é a classe que decide o que perguntar: carreta e
  caminhão não têm modelo a informar, e só a moto tem cilindrada (ADR-0023 D4).

### D3 — O roteiro é **derivado**, não fixo

Como o caminho depende das respostas, a sequência não pode ser uma constante: ela é **função pura do estado**
— escolhas dentro, lista de passos fora.

Isso dá três coisas de graça, e a terceira é a que mais importa:

1. **"passo 3 de 6"** sai do tamanho da lista, e fica certo em qualquer fluxo;
2. **voltar** é andar para trás na lista, sem `if` sobre categoria espalhado pela navegação;
3. **o roteiro vira teste**: comparar a lista esperada com a produzida é como se verifica que *"escolher
   gratuidade insere o passo do subtipo"* — sem tela, sem ViewModel.

### D4 — O cliente é salvo **no passo dele**, e não no fim

*"Forms cliente dependendo da contagem de pessoas, salvando cada um."*

Cada formulário de pessoa termina registrando aquela pessoa no pool (`criarOuAssinar`, ADR-0025 D6). Não se
acumula para salvar tudo na emissão, e a razão é a tolerância a falha do ADR-0028 D3: **a operação que exige
rede passa a falhar uma pessoa por vez**, no passo em que o operador está, com o que ele acabou de digitar na
tela — em vez de falhar no fim, depois de três formulários, sem dizer qual deles não subiu.

Consequência coerente com o ADR-0018 D3: quando a pessoa **já existe** no pool, o passo é uma **assinatura** e
o operador não percebe diferença.

### D5 — O passo 6 é o **desfecho**, e hoje ele é só o digital

*"Resolver emissão — por enquanto somente digital, mas vai ter física, assim como vias (navio, agência,
cliente); quando chegar lá vai precisar de estudo e ADR próprios."*

Então o passo 6 nasce com **uma** saída — o bilhete digital — e com a forma preparada para ter mais de uma. O
que **não** se faz agora é desenhar a impressão física por antecipação: ela traz surface própria (impressora
térmica, Bluetooth), **vias** com destinatários diferentes (navio, agência, cliente) e regra de o que cada
via mostra. Isso é estudo e ADR próprios, **encapsulados no mesmo escopo** da impressão.

## Consequências

- **a emissão fica mais longa em número de telas e mais curta em tempo**: são mais passos, mas cada um é um
  toque, e o formulário sobra só para pessoa e veículo;
- **a orquestração da F9.4 muda**: `PassoEmissao` como enum de três valores dá lugar ao roteiro derivado (D3).
  O ViewModel continua o **único escritor**, e a sequência da emissão continua nele — o que muda é quantos
  passos ele conhece;
- **o registro no pool se distribui** pelos passos de cliente (D4), então a emissão do passo 6 fica com menos
  I/O: guardas, número e escrita;
- **a máscara e o teclado por tipo importam mais**, porque os únicos campos que sobraram são os que mais
  erram: documento, placa e valor.

## O que este ADR não decide

- **a impressão física e as vias** (D5) — estudo e ADR próprios;
- **o texto e o ícone de cada botão**, que são do incremento de tela;
- **como o operador reaproveita alguém do pool** (buscar por nome/documento) — continua em aberto desde o
  ADR-0028, e entra com o passo 4 quando for desenhado;
- **o rascunho do atendimento em curso** (a passagem incompleta) — nota lateral do ADR-0026, fora da F9.

## Referências

- [ADR-0028](0028-as-etapas-da-emissao.md) — o roteiro que este substitui em granularidade, e cujas demais
  decisões seguem valendo
- [ADR-0023](0023-passagem-por-categoria-e-referencia.md) D1/D4 — a categoria como raiz e o que a classe do
  veículo exige
- [ADR-0018](0018-agregado-passagem-participantes-modo-e-lancamentos.md) D3 — criar ou assinar no pool
- [ADR-0026](0026-orquestracao-e-apresentacao-da-passagem.md) D1/D3 — o VM como único escritor e o one-shot