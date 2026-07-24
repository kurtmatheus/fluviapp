# Estudo de design — Balanço financeiro da travessia

**Status:** Rascunho — Claude escreveu, revisão do analista pendente. Prepara o terreno para um ADR próprio
(o [ADR-0013](../adr/0013-tabela-de-tarifa-e-tipo-tarifario.md) deixou o relatório financeiro fora de
escopo, Fase 4: "só preparar os campos"). Consome o modelo de preço tabelado que o ADR-0013 já entregou.

> Conversa com o [ADR-0013](../adr/0013-tabela-de-tarifa-e-tipo-tarifario.md) (tarifa tabelada, tarifaBase
> congelada, tarifaDevida/desconto derivados), o [ADR-0008](../adr/0008-relacionamentos-por-identidade.md)
> (agregar pelo `navioId` congelado, rename-safe), o [ADR-0003](../adr/0003-modelo-de-memoria-do-dado.md)
> (camadas do dado) e o [estudo de domínio da passagem](dominio-passagem.md).

---

## 1. Onde o balanço está hoje

O balanço atual é **só de ocupação**, não financeiro. `BalancoPassagensMapper`
(`model/mappers/BalancoPassagensMapper.kt`) agrupa as passagens pelo **`navioId` congelado** (ADR-0008) e
conta, por navio: redes/inteiras/meias/gratuidades, suítes (2/3 pessoas), camarotes e veículos
(carro/moto/caminhão/**carreta**), sempre **preenchidas × capacidade** do navio. `DadosBalancoPassagem` e o
`BalancoNavioCard` são todos contadores — **nenhum campo de dinheiro**.

Ou seja: o app responde *"quantos lugares foram ocupados"*, não *"quanto se faturou / se deixou de
faturar"*. O ADR-0013 acabou de tornar a segunda pergunta respondível.

## 2. O que o modelo de preço já entrega (ingredientes — sem nova persistência)

O balanço financeiro **não precisa de novos campos persistidos** — a Fase 4 do ADR-0013 está satisfeita:

- **`tarifaBase`** — congelada na Passagem na emissão (a tarifa da inteira da célula viagem × chave).
- **`tarifaDevida`** — pura, `TipoPassagem.tarifaDevida(base)` (inteira=base, meia=metade, gratuidade=0);
  veículo = base cheia (tipoPassagem nulo).
- **`desconto`** — puro, `descontoDerivado(tarifaDevida, valorCobrado)` (resíduo abaixo da devida).
- **`valorCobrado`** — soma dos meios de pagamento (já na Passagem).

Tudo já usado pelo `PassagemDadosPassagemMapper` para exibir uma passagem. O balanço é **a mesma derivação,
agregada por navio/viagem**.

## 3. As dimensões financeiras propostas

Por navio (frozen) e/ou por viagem, somando sobre as passagens do grupo:

| Dimensão | Fórmula | Lê o quê |
|---|---|---|
| **Receita esperada** | Σ `tarifaDevida` | o que as categorias emitidas *deviam* render |
| **Receita real** | Σ `valorCobrado` | o que de fato entrou |
| **Desconto concedido** | Σ `desconto` | redução **discricionária** (abaixo da devida) |
| **Redução mandatória** | Σ (`tarifaBase` − `tarifaDevida`) p/ meia+gratuidade | o que a lei "custou" (meia + gratuidade) |
| **Déficit / superávit** | Receita real − Receita esperada | furo (ou sobra) contra o tabelado |

O insight forte do ADR-0013 aparece aqui: **separar redução mandatória (meia/gratuidade) de desconto
discricionário** deixa o gestor ver *quanto foi obrigação de lei* vs. *quanto o vendedor abriu de mão* — e
a **receita abdicada pela gratuidade** (Σ `tarifaBase` das gratuidades) vira um número visível, não um
buraco silencioso.

## 4. Achados / pontos abertos do BALANÇO

- **Ocupação e financeiro são preocupações distintas.** O `BalancoPassagensMapper` atual é um laço de
  contadores. Misturar somas de dinheiro nele incharia o método. Provável: um **mapper financeiro à parte**
  (ou uma projeção separada) que reusa as funções puras do ADR-0013, deixando a ocupação como está.
- **Bilhetes de fallback (`tarifaBase` null).** Bilhetes antigos / sem tarifa tabelada não têm base. No
  detalhe eles degradam para "valor cobrado". No **agregado**, somar "esperado" ignorando-os subestima o
  esperado. Decisão: excluí-los do "esperado" e sinalizar a contagem de fallback (não mascarar).
- **Dívida de perf (herdada, ADR-0006 / domínio §9): `N+1` + `runBlocking`.** A leitura das passagens do
  balanço faz `runBlocking { dao.salvar }` por doc; agregar dinheiro por cima amplia o custo. Vale medir
  antes de crescer o balanço.
- **CARRETA no card (ocupação) — meio-feito.** `BalancoPassagensMapper`/`DadosBalancoPassagem` já **contam**
  carreta; falta a linha no `BalancoNavioCard` (`totalCarretas`) + string `label_carretas`. Ponto pequeno,
  aberto.
- **Granularidade do relatório.** Por navio (como hoje) e/ou por viagem? A receita esperada faz mais
  sentido por **viagem** (a tabela de tarifa é da viagem); a ocupação é por **navio**. Talvez dois eixos.

## 5. Pontos de PASSAGEM ainda a finalizar (pré-requisitos do balanço)

O balanço herda o que a passagem registrar. Estes pontos ficaram abertos no ADR-0013 e afetam a fidelidade
do balanço financeiro — **precisam ser fechados antes (ou junto) do balanço**:

- **Cilindrada da moto não é persistida** (slice 2b). Hoje a `tarifaBase` da moto é congelada, mas o `cc`
  que a justificou não fica no bilhete (`FormVeiculoUiState.cilindrada` é transitória). Consequência:
  edição de moto mostra `cc` em branco; o bilhete não registra a cilindrada; auditoria da tarifa da moto
  fica cega. Persistir como atributo de veículo (migração + DTO + mapper + prefill).
- **State de desconto vestigial.** `FormPassagemUiState.desconto`/`onDescontoChange`/`isDescontoEnabled`
  ficaram no estado (o campo de UI foi removido; `montarPassagem` lê como fallback). Limpeza pendente.
- **`CardValor` não faz preview de veículo.** Resolve `tarifasViagem[acomodacao]` — para veículo (acomodação
  vazia) não mostra a tarifa. Estender para resolver por `tipoVeiculo`/moto.
- **Cota de gratuidade só na criação.** `validarEmissao` checa a cota quando `idPassagem` é vazio; uma
  **edição que muda o tipo para gratuidade** não re-checa. E a cota é validação de aplicação, não invariante
  de servidor (concorrência pode furar — ressalva já assumida no ADR-0013 §8).
- **Fail-closed pode surpreender.** Emitir numa viagem sem a tarifa da acomodação/classe é **bloqueado**.
  É o comportamento desejado, mas a UX do bloqueio (toast) pode merecer um caminho mais explícito ("cadastre
  a tarifa" com atalho).

## 6. Perguntas para o analista (semear o ADR do balanço)

1. **Mapper à parte ou estender o de ocupação?** (inclinação: à parte, reusa as funções puras).
2. **Eixo do relatório:** por navio, por viagem, ou os dois?
3. **Fallback (`tarifaBase` null):** excluir do "esperado" e sinalizar a contagem — confirma?
4. **Escopo do 1º incremento financeiro:** só os totais (esperado/real/desconto/déficit) por navio, ou já
   o breakdown por categoria (inteira/meia/gratuidade) e a receita abdicada pela gratuidade?
5. **Ordem vs. passagem:** fechar os pontos de passagem (§5) antes, ou em paralelo ao balanço?