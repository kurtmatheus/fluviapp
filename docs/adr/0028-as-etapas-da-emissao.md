# ADR-0028: As etapas da emissão — três passos, um bilhete e um cabeçalho que não se digita

**Status:** Aceita em direção (decisões do analista em 2026-08-13) · implementação na F9.4/F9.5

**Insumos:** [ADR-0026](0026-orquestracao-e-apresentacao-da-passagem.md) D4 (que fixou o **eixo** — emissão por
etapas — e deixou o roteiro aberto), [ADR-0023](0023-passagem-por-categoria-e-referencia.md) (a categoria como
raiz), [ADR-0013](0013-tabela-de-tarifa-e-tipo-tarifario.md) §4/§8 (tipo tarifário e cota de gratuidade) e a
tela demolida na F9.2, que serve de base — *"use o atual como base e melhore"*.

---

## Contexto

O ADR-0026 D4 decidiu que a emissão passa a ser **por etapas**, e disse por quê: é o que resolve os **47
parâmetros** de uma tela só. O que ele deliberadamente **não** decidiu foi *quantas etapas* e *o que vai em
cada uma* — e isso não é detalhe de layout, porque a ordem das etapas é, em boa parte, **imposta pelo
domínio**: a acomodação precisa vir antes da lista de clientes porque é ela quem declara quantos cabem.

Este ADR fecha o roteiro. Ele registra quatro decisões do analista e deriva o resto do que os tipos já
obrigam.

## Decisão

### D1 — O bilhete é **unitário**, exceto suíte e camarote

*"Bilhete unitário para todos os casos exceto suíte e camarote"* — palavra do analista, e ela confirma no
fluxo o que os tipos já diziam: `Acomodacao.REDE` tem `ocupacaoMaxima = 1`, suíte e camarote têm 3.

**A consequência prática é sobre o atendimento, e é ela que vale registrar:** uma família de três em redes são
**três emissões**, não uma emissão com três pessoas. Carro e motorista, idem — são **categorias diferentes**,
logo dois bilhetes. Não existe "emitir vários de uma vez", e o antigo *checkbox* `isVeiculoChecked` — que
punha veículo e passageiro no mesmo formulário — **morre**: ele era a forma visível de um modelo que
misturava as duas coisas.

O agrupamento continua existindo **só onde o espaço é compartilhado**: suíte e camarote são um espaço vendido
a até três pessoas, e por isso são **um** bilhete com até três clientes.

### D2 — A gratuidade **volta ao agregado**, e a cota continua valendo

*"Volta ao agregado, continua valendo"*.

A F9.1 deixou uma lacuna que só a emissão expõe: `PassagemDePassageiro` guarda `tipo`
(`INTEIRA`/`MEIA`/`GRATUIDADE`), mas **não guarda qual** gratuidade — o `TipoGratuidade` (idoso, PcD, criança
até 5, passe federal) ficou sem portador nenhum no código. Sem ele, *"gratuidade"* vira um rótulo que não diz
nada ao balanço nem à fiscalização, e a **cota do ADR-0013 §8** — no máximo **2 por categoria, por viagem** —
não tem sobre o que contar.

Então:

- o subtipo volta como campo do agregado, **presente somente quando o tipo é `GRATUIDADE`**. Não é um campo
  opcional que às vezes se preenche: é a informação que *completa* a gratuidade, e a forma que impede
  "gratuidade sem categoria" é a mesma do `CarimboEmbarque` — o par ou existe inteiro, ou não existe;
- ele entra na **fronteira** (o codec grava e lê) porque é sobre ele que a contagem da cota roda;
- a cota permanece **validação de aplicação, firestore-driven**, com o limite já declarado no ADR-0013: duas
  emissões simultâneas podem furá-la, e endurecer isso exigiria contador transacionado por categoria — que
  **não** entra aqui.

### D3 — Bilhete de passageiro **exige portador**; o de veículo, não — e salvar tem de tolerar falha

*"Não pode; deve ser tolerante a falha; mas o negócio exige bilhete com portador exceto de veículo."*

Três coisas, nesta ordem:

1. **Não se emite passagem de passageiro com cliente que não está no pool.** O agregado referencia
   `clienteId` (ADR-0023 D5), então um bilhete sem cliente salvo é literalmente inescrevível — e isso é
   proteção, não burocracia: era assim que se emitia bilhete sem credencial nenhuma antes do ADR-0018 D4;
2. **o veículo é a exceção**, e já era: `responsavelRetirada` é opcional **por regra de negócio** — quem
   retira costuma ser definido na hora, informalmente, entre despachante e transportadora. **Bilhete de
   veículo sem ninguém nomeado é a forma normal**;
3. **o registro no pool tem de tolerar falha.** Ele é a única operação do app que **exige rede**
   (`criarOuAssinar` decide criar × assinar no servidor, ADR-0025 D6), e a bilheteria é de beira de rio.
   Tolerar falha aqui significa: **o atendimento não se perde** — o que foi digitado permanece, o operador vê
   o que aconteceu e tenta de novo. O que **não** significa: emitir assim mesmo.

### D4 — Três passos, com abas numeradas, avanço e volta

*"Passos com ação e volta, com abas numeradas e um cabeçalho de guia com as informações da viagem."*

| # | Passo | O que decide | Por que aqui |
|---|---|---|---|
| **1** | **O bilhete** | categoria (passageiro × veículo); se passageiro: **acomodação** → tipo tarifário → subtipo de gratuidade | é a **raiz**: a categoria decide qual sub-domínio existe, e a acomodação decide *quantos cabem* e *que tipos admite*. Nada do que vem depois se pergunta sem isto |
| **2** | **Quem viaja** (ou **o que embarca**) | passageiro: titular + acompanhantes até `ocupacaoMaxima`; veículo: placa, classe, modelo/cilindrada, cor e o responsável **opcional** | depende inteiramente do passo 1 — o número de campos de pessoa **é** a ocupação máxima da acomodação escolhida |
| **3** | **O pagamento** | lançamentos por forma, observação, e a **ação de emitir** | é o fim porque a emissão é **pós-pagamento**: registra-se o que entrou. E é aqui que o número se reserva |

**O tipo tarifário fica no passo 1, e não junto do passageiro**, porque ele é propriedade do **espaço
vendido**: meia e gratuidade só existem na rede (ADR-0013 §4), então fora dela o seletor **não existe** — não
aparece desabilitado.

**A confirmação não é um quarto passo.** Emitir é a ação do passo 3, e o passo 3 já mostra o que se está
cobrando; um passo de revisão a mais custaria um toque em todo atendimento para repetir o que está na tela.
A irreversibilidade é tratada onde ela de fato é: **cancelar é estado** e o número fica com o bilhete
cancelado (ADR-0024 D11).

### D5 — O cabeçalho de guia: as informações da viagem, **e elas não se digitam**

O card da viagem existia na tela antiga (`ViagemCard`) e ficava **dentro da rolagem** — sumia justamente
quando o operador estava preenchendo o que dependia dele. Ele vira **cabeçalho persistente**, visível nos
três passos.

E vem com a correção que é a razão principal desta decisão: **data e hora deixam de ser campos**. Elas eram
`FormFieldCalendario` + `FormFieldRelogio` editáveis no meio do formulário, isto é, o operador podia digitar
uma data que **discorda da saída escolhida** — exatamente o defeito que a agência digitada teve até a P2.3.
A ocorrência (`viagemId` + data) chega **pronta**, do card de saída do Início, e o cabeçalho a **exibe**.

### D6 — O que a forma nova apaga

| Peça | Por quê |
|---|---|
| `isVeiculoChecked` | a categoria é o passo 1 (D1) |
| `FormFieldCalendario`/`FormFieldRelogio` da emissão | a ocorrência vem pronta (D5) |
| `scrollParaErro` (o *nonce* que rolava até o primeiro erro) | com passos, o erro **está no passo**, e a aba numerada o sinaliza — não há para onde rolar |
| os **47 parâmetros** | cada passo recebe o seu `UiState` e os seus eventos (ADR-0026 D2/D4) |

## Consequências

- **o atendimento fica mais longo em casos múltiplos** (três redes = três emissões) e mais **simples em
  todos**: some o formulário que servia a dois sub-domínios ao mesmo tempo;
- **a cota de gratuidade volta a ser verificável**, porque volta a haver o que contar (D2);
- **a emissão passa a exigir rede em dois pontos** — registrar o participante e reservar o número —, e os dois
  precisam de desfecho declarado na tela, não de um *spinner* infinito;
- **o cabeçalho elimina uma classe inteira de erro**: bilhete emitido para data diferente da saída escolhida.

## O que este ADR não decide

- **como o operador acha um cliente que já está no pool** (busca por nome/documento na `consultarDaAgencia`)
  — é desenho do passo 2, e entra com ele;
- **o texto e o layout** de cada passo, que são da F9.5;
- **o endurecimento da cota** por contador transacionado (ADR-0013 §8 já o registra como limite aceito);
- **a passagem incompleta** (o rascunho como atendimento em curso) — nota lateral do ADR-0026, fora da F9.

## Referências

- [ADR-0026](0026-orquestracao-e-apresentacao-da-passagem.md) D4 — o eixo que este ADR completa
- [ADR-0023](0023-passagem-por-categoria-e-referencia.md) — a categoria como raiz, e o participante por id
- [ADR-0013](0013-tabela-de-tarifa-e-tipo-tarifario.md) §4/§8 — tipo tarifário por acomodação e a cota
- [ADR-0018](0018-agregado-passagem-participantes-modo-e-lancamentos.md) D3/D4 — o pool e o documento obrigatório