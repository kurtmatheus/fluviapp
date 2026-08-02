# ADR-0014: Balanço financeiro da travessia (receita esperada × real × déficit)

**Status:** Rascunho — Claude rascunhou, revisão do analista pendente. Decisões de escopo **fechadas**
(ver *Decisões resolvidas*); nenhum código ainda.

> **A régua e o eixo mudaram antes de este ADR virar código** — o desenho segue de pé, com três correções:
> - **"Receita esperada" não vem mais de tarifa cadastrada.** A tabela adormeceu
>   ([ADR-0016 §7.2](0016-dominio-da-plataforma.md)) e a base passa a ser **inferida por agregação** — só das
>   inteiras, por `(viagem, acomodação)` e `(viagem, classe de veículo)`. As funções puras do ADR-0013
>   sobrevivem; muda a **fonte** da base. Some também a nota de rodapé do *fallback* `tarifaBase = null`: ele
>   deixa de ser exceção e passa a ser o **caso normal** — o que o relatório precisa tratar é o **cold start**
>   (viagem nova ainda sem base inferida).
> - **O eixo é a ocorrência, não a "viagem" antiga.** Com `(rota, navio, diaSemana, hora)` e a ocorrência
>   `(viagemId, data)` ([ADR-0016 §7.1](0016-dominio-da-plataforma.md),
>   [ADR-0018 D9](0018-agregado-passagem-participantes-modo-e-lancamentos.md)), "receita da travessia" passa a
>   significar exatamente uma partida.
> - **Passagens canceladas ficam de fora** da esperada e da real, e são **contadas à parte**
>   ([ADR-0018 D18](0018-agregado-passagem-participantes-modo-e-lancamentos.md)) — mesmo tratamento que este
>   ADR já dá ao que não entra no esperado: não mascarar.
>
> A **receita real** também deixa de ser soma de quatro campos e passa a ser soma dos **lançamentos**
> (ADR-0018 D11), o que não muda o total, só de onde ele vem.

> Consome o [ADR-0013](0013-tabela-de-tarifa-e-tipo-tarifario.md) (tarifa tabelada, `tarifaBase` congelada,
> `tarifaDevida`/`desconto` derivados — as regras puras que este balanço reusa). Conversa com o
> [ADR-0008](0008-relacionamentos-por-identidade.md) (agregar por id congelado — aqui `viagemId`) e o
> [ADR-0003](0003-modelo-de-memoria-do-dado.md). Documento-base companheiro:
> [`docs/design/balanco-financeiro.md`](../design/balanco-financeiro.md).

## Contexto

O balanço de hoje é **só de ocupação** (`BalancoPassagensMapper`: preenchidas × capacidade por navio; zero
dinheiro). O ADR-0013 acabou de tornar o **financeiro** possível **sem nova persistência** — a `tarifaBase`
está congelada na Passagem e as regras `TipoPassagem.tarifaDevida(base)` / `descontoDerivado(devida,
cobrado)` são puras e reutilizáveis. Falta agregar.

## Decisão

Um relatório financeiro **novo**, à parte da ocupação, que soma a economia derivada por viagem.

1. **Mapper novo, à parte.** Não estende o `BalancoPassagensMapper` (laço de contadores de ocupação) — um
   mapper financeiro separado (`BalancoFinanceiroMapper` ou similar) que **reusa as funções puras do
   ADR-0013**. Ocupação e finanças ficam como projeções distintas (separação de preocupações).
2. **Agrega por viagem** (`viagemId`). A tarifa é da viagem, então receita esperada × real × déficit faz
   sentido por travessia (não por navio, que misturaria viagens de tabelas diferentes). Contraponto: a
   ocupação segue por navio — os dois eixos coexistem, cada relatório no seu.
3. **Totais (1º incremento):** por viagem,
   - **Receita esperada** = Σ `tarifaDevida` (via `TipoPassagem`; veículo = `tarifaBase` cheia)
   - **Receita real** = Σ `valorCobrado` (soma dos meios de pagamento)
   - **Desconto concedido** = Σ `descontoDerivado`
   - **Déficit / superávit** = real − esperada
4. **Fallback (`tarifaBase` null) excluído do esperado, e sinalizado.** Bilhetes antigos / sem tarifa
   tabelada não entram na receita esperada (somá-los subestimaria); conta-se **quantos** ficaram de fora e
   mostra-se esse número (não mascara).

A derivação por passagem é a **mesma** do `PassagemDadosPassagemMapper` (ADR-0013 §5) — este mapper apenas
a **agrega**. Mantém-se puro/testável (entra `List<Passagem>`, sai a projeção por viagem).

## Consequências

- **O balanço passa a responder "quanto se faturou × se esperava × se abriu de desconto"**, não só "quantos
  lugares foram ocupados".
- **Zero nova persistência** — reusa `tarifaBase` (persistida) + funções puras. Fase 4 do ADR-0013 (preparar
  campos) estava satisfeita.
- **Dois eixos** (ocupação por navio, financeiro por viagem) — mais claro conceitualmente, dois relatórios.
- **Dívida herdada (ADR-0006 / domínio §9): `N+1` + `runBlocking`** na leitura das passagens do balanço —
  agregar dinheiro por cima **não piora** o schema, mas amplia o custo do laço; medir antes de crescer.
- **Fallback visível** — a contagem de bilhetes sem `tarifaBase` no relatório evita um "esperado"
  silenciosamente subestimado.

## Plano de migração (faseado, aditivo)

- **Fase 1 — Modelo + agregação (sem UI).** `DadosBalancoFinanceiro` (por viagem: esperada/real/desconto/
  déficit + contagem de fallback, formatados via `BigDecimal` scale 2). `BalancoFinanceiroMapper` (reusa
  `TipoPassagem.tarifaDevida`/`descontoDerivado`; agrupa por `viagemId`; exclui `tarifaBase` null do
  esperado e conta). Testes JVM puros (esperada/real/desconto/déficit + fallback excluído/contado).
- **Fase 2 — Tela/relatório.** Exibir o balanço financeiro por viagem (card), reusando o padrão do
  `BalancoNavioCard`. Escopo de tela decidido na revisão.

## Alternativas consideradas

- **Estender o `BalancoPassagensMapper`** — rejeitado: incha o laço de ocupação e mistura preocupações.
- **Agregar por navio** — rejeitado como eixo financeiro (mistura tarifas de viagens diferentes); segue
  como eixo da ocupação.
- **Fallback com esperado = cobrado** — rejeitado: injeta bilhetes sem referência real no "esperado".

## Alternativas futuras

- **Breakdown por categoria** (inteira/meia/gratuidade) e a **receita abdicada pela gratuidade**
  (Σ `tarifaBase` das gratuidades) — o número que mostra o custo da lei.
- **Eixo por navio** no financeiro (além da viagem), se o gestor quiser consolidar.
- **Relatório exportável / período** (intervalo de datas), fora do 1º incremento.
- Endereçar o `N+1`/`runBlocking` herdado do balanço.

## Decisões resolvidas na revisão (analista)

- **Mapper novo à parte** (reusa as funções puras do ADR-0013), não estende a ocupação.
- **Eixo por viagem** (a tarifa é da viagem).
- **1º incremento = só os totais** (esperada/real/desconto/déficit); breakdown + abdicada = futuro.
- **Fallback (`tarifaBase` null): excluído do esperado + contagem sinalizada.**