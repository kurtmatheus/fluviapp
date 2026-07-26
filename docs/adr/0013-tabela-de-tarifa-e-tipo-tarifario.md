# ADR-0013: Tabela de tarifa na Viagem e tipo tarifário da passagem (desconto derivado)

**Status:** Aceita (direção) — decisões de revisão fechadas (ver *Decisões resolvidas*); implementação por
fase, nenhum código ainda. Claude rascunhou, analista revisou.

> Conversa com o [ADR-0008](0008-relacionamentos-por-identidade.md) (a tarifa vira **par id+snapshot**: a
> tabela é master data na Viagem, o valor devido é congelado na Passagem), o
> [ADR-0012](0012-ciclo-de-vida-passagem-e-embarque-qr.md) (o mesmo movimento que tirou o `status` do enum
> genérico e o fez **tipo de domínio** — aqui repetido para o *tipo tarifário*), o
> [ADR-0003](0003-modelo-de-memoria-do-dado.md) (forma do dado nas camadas: DTO aninhado no Firestore ×
> plano no Room, e o trade-off SQL×JSON para a matriz), o
> [ADR-0004](0004-snapshot-e-observabilidade-emissao.md) (o congelamento acontece na emissão) e o
> [ADR-0006](0006-molde-de-cadastro.md) (validação pura, VM dona do estado — a tabela entra no form de
> Viagem). Continua o desenho de domínio em [`docs/design/dominio-passagem.md`](../design/dominio-passagem.md)
> (§ tarifa/desconto, hoje ausente).

## Contexto

A passagem tem **três tipos tarifários** — inteira, meia e gratuidade — e a gratuidade se subdivide
(**Idoso, PcD, Criança até 5 anos, Passe Federal**). Hoje o app não modela nada disso com fidelidade; o preço é
uma constante mágica e o desconto é um input solto que embaralha redução legal com redução comercial.
Cinco rachaduras concretas:

- **Tarifa é constante global, não tabela.** `Passagem.TARIFA_ANTAC = "300"` e `DESCONTO_ANTAC = "50"`
  (`Passagem.kt:80-81`) — `String`, hardcoded, iguais para toda viagem, todo navio, toda acomodação. Não
  há preço por travessia.
- **Tipo e gratuidade são `String?` soltos** (`Passagem.kt:37-38`), servidos do catálogo genérico
  `Constante.Descricao`, que só conhece `INTEIRA/MEIA/GRATUIDADE` + `CORTESIA` (`Constante.kt:17-22`). Os
  quatro subtipos de gratuidade **não existem**. É a mesma "String pendurada no catálogo genérico" que o
  `status` **abandonou** no ADR-0012 — e o mesmo enum ainda carrega lixo legado (`A_EMITIR/EMITIDA`,
  `Constante.kt:52-53`).
- **O total é reconstruído ao contrário.** `getValorTotal` **soma os meios de pagamento + desconto**
  (`PassagemDadosPassagemMapper.kt:144-158`) e depois `valorAPagar = valorTotal − desconto`
  (`:63`). O "total" nasce do que foi *arrecadado*, não de uma tarifa de referência. É circular: o
  desconto entra na conta que deveria explicá-lo.
- **A meia mora *dentro* do desconto.** `calcularDesconto` **soma 50 (ou 25 na meia) ao desconto
  digitado**, só para acomodação `REDE` e só em passagem nova (`CalculoDesconto.kt:14-32`). Redução
  mandatória (meia), taxa fixa (o "50") e desconto discricionário viram **um número só** — impossível
  reportar quanto foi obrigação de lei e quanto foi decisão do vendedor.
- **Veículo sem tarifa própria.** O veículo é participante de mesmo nível do passageiro
  (`dominio-passagem.md` §1), mas não tem preço no modelo. `Constante.Descricao` conhece `CARRO/MOTO/
  CAMINHAO` (`Constante.kt:36-39`) — **falta `CARRETA`** — e não há campo de **cilindradas** para
  diferenciar moto (`FormVeiculoUiState.kt:21`: só `tipoVeiculo/modelo/placa/cor`).

**Diagnóstico:** falta a **tarifa tabelada como fonte da verdade**. Enquanto o preço for global e o
desconto for input, "descontos dados" não é analisável e "déficit/lucro por viagem" não existe.

## Decisão

Inverter o modelo: a **tarifa tabelada é a referência de receita**; o valor cobrado se *mede contra*
ela; **meia e gratuidade são categorias tarifárias** (redução mandatória), e **desconto é só o que se
abre abaixo da tarifa devida** (redução discricionária, discriminada por passagem).

### 1. Tabela de tarifa na Viagem — uma estrutura, dois eixos

A Viagem passa a carregar a **tabela de tarifas da inteira**, em `BigDecimal` scale 2 (`RoundingMode.UP`).
A tabela é um **mapa `chave tarifária → valor da inteira`**, e a mesma estrutura serve os dois tipos de
participante (reaproveitamento pedido na revisão):

- **Passageiro** — chave = **acomodação** (`REDE`, `SUITE`, `CAMAROTE`; `Constante.kt:24-27`).
- **Veículo** — chave = **classe tarifária de veículo**, um **eixo novo, só de preço** (decidido na
  revisão), separado do `tipoVeiculo` descritivo que o form já coleta (`CARRO/MOTO/CAMINHAO`,
  `Constante.kt:36-39`; sem `CARRETA`). A classe existe para precificar, não para descrever.
  - **`CARRO`, `CARRETA`, `CAMINHAO`** têm **célula na tabela da Viagem** (valor cadastrado por travessia).
  - **`MOTO` é precificada por *regra*, não por célula** — a tarifa deriva da **cilindrada**, capturada no
    form de veículo por **dropdown** (valores discretos). Regra provisória (decidida na revisão, "até o
    momento"): **piso à centena da cilindrada, 1:1 em reais** — `floor(cc / 100) * 100`
    (125cc→R$100, 250cc→R$200, 300cc→R$300). A cilindrada é o dado capturado; a `tarifaBase` da moto é o
    resultado da regra, **congelado na emissão** (§2) — trocar a regra depois não mexe em bilhete antigo.

Cada passagem consome **exatamente uma célula** da tabela conforme sua natureza (passageiro → acomodação;
veículo → classe). Essa célula é a **tarifa da inteira** daquele bilhete.

> **Forma no Room — tabela-filha normalizada (decidido na revisão).** A tabela é naturalmente aninhada:
> segue como **mapa no `ViagemDocumento`** (Firestore, forma natural do DTO). No Room, em vez de uma coluna
> JSON opaca, vira **tabela-filha `TarifaViagem(viagemId, chave, valor)`** — uma linha por célula. O motivo
> é o balanço: vão existir **várias tarifas-base por viagem**, e linhas normalizadas são **agregáveis por
> `viagemId`/`navioId` direto em SQL** (receita esperada × real × déficit, item 7), o que a coluna JSON
> exigiria desempacotar em memória. É o lado "SQL" do trade-off do ADR-0003, escolhido pela consulta. O
> mapper achata mapa→linhas na escrita do espelho e linhas→mapa na leitura (mesma costura DTO↔Room do
> resto do app).

### 2. Tarifa é par id+snapshot (ADR-0008) — congela só a base

A tabela é master data na Viagem (**pointer** por `viagemId`, já congelado na Passagem — ADR-0008). Na
emissão, congela-se **apenas a `tarifaBase`** da passagem (o valor da célula que valia naquele momento) —
um novo campo em `Passagem`. Editar a tabela da Viagem depois **não reescreve** a economia de bilhetes
históricos, exatamente como `empresa/navio/origem` já fazem (`dominio-passagem.md` §5).

**`tarifaDevida` e `desconto` NÃO são congelados** (decisão da revisão): são **derivados no mapper** a
partir de `tarifaBase` + tipo. Menos colunas, fonte única de regra — assumindo o trade-off de que mudar a
regra de cálculo altera a leitura de bilhetes antigos (aceitável em portfólio; a `tarifaBase` congelada
preserva o número que importa).

### 3. Tipo tarifário como tipo de domínio (espelha o ADR-0012)

Criar tipos próprios, tirando o conceito do `Constante.Descricao`:

```kotlin
enum class TipoPassagem { INTEIRA, MEIA, GRATUIDADE }
enum class TipoGratuidade { IDOSO, PCD, CRIANCA_ATE_5, PASSE_FEDERAL }  // só quando GRATUIDADE
```

Com `de(valor: String?)` fail-closed e tolerante à grafia legada, e `.name` canônico na escrita — o mesmo
contrato de `StatusPassagem` (`StatusPassagem.kt`). O `Constante(TIPO_PASSAGEM)`/`(GRATUIDADE)` deixa de
ser o *tipo* e fica só como **opções de filtro/dropdown** (como o status ficou). `CORTESIA` (legado) é
**aposentado** (decisão da revisão): cortesia é redução **comercial**, não gratuidade legal — se preciso,
cabe como desconto (§5), não como `TipoGratuidade`. Os quatro subtipos são as gratuidades legais.

### 4. Tarifa devida por categoria — regra pura, um lugar só

Cada categoria sabe derivar sua **tarifa devida** a partir da `tarifaBase` (a inteira da célula):

| Categoria | Tarifa devida |
|---|---|
| `INTEIRA` | `tarifaBase` |
| `MEIA` | `tarifaBase.divide(2, scale = 2, RoundingMode.UP)` |
| `GRATUIDADE` (qualquer subtipo) | `BigDecimal.ZERO` |

**Meia e gratuidade valem só para o passageiro EM REDE** (revisão 2026-07-26). O veículo é sempre
**inteira da sua classe** — não tem meia nem gratuidade (idoso/PcD/criança/passe federal são categorias
*de pessoa*). E o passageiro de **suíte/camarote é sempre `INTEIRA`**: o **tipo tarifário existe apenas na
acomodação `REDE`** — em suíte/camarote **não há preenchimento nem validação** de tipo tarifário (o form
esconde o campo quando a acomodação não é rede; fallback de UX). O `TipoPassagem` qualifica só a linha do
passageiro de rede; suíte/camarote/veículo são sempre `INTEIRA` da célula.

### 5. Desconto é o resíduo abaixo da devida — e é discriminado

```
valorCobrado = Σ (valorPago avulso, Pix, Dinheiro, Débito, Crédito)   // o que de fato entrou
desconto     = max(ZERO, tarifaDevida − valorCobrado)                  // só o que se abriu abaixo da devida
```

- **Meia não é desconto** — já foi descontada na `tarifaDevida`. Desconto é só a diferença *adicional*
  aberta pelo operador abaixo da devida.
- **Gratuidade ⇒ `tarifaDevida = 0` ⇒ desconto sempre 0** (não há o que abrir abaixo de zero).
- O desconto passa a ser **grandeza derivada e auditável por passagem** — visível nos Detalhes e agregável
  no balanço. É o que torna "descontos dados" analisável, separado das reduções mandatórias.

Isto **substitui** `getValorTotal` (soma circular), `obterTotalTarifa` (300/150 hardcoded,
`PassagemDadosPassagemMapper.kt:135-141`) e o acúmulo ANTAC de `CalculoDesconto.kt`. `TARIFA_ANTAC` e
`DESCONTO_ANTAC` são removidos.

### 6. Dinheiro: `Double?` na fronteira, `BigDecimal` scale 2 no cálculo

Sem migração de tipo (decisão da revisão): os campos monetários seguem `Double?` no Firestore/Room
(`Passagem.kt:30-35`); a **disciplina scale 2 / `RoundingMode.UP`** vale em todo cálculo, convertendo na
borda (o `converterParaBigDecimal` já existente, `PassagemDadosPassagemMapper.kt:46-51`). A `tarifaBase`
nova segue o mesmo padrão de fronteira, pela consistência de ter **uma** representação de dinheiro.

### 7. Balanço ganha eixo de análise (receita esperada × real)

Com tarifa tabelada e desconto derivado, `BalancoPassagensMapper` (agrega por `navioId`) passa a poder
reportar, por viagem/navio: **receita de referência** (Σ `tarifaDevida`), **receita real**
(Σ `valorCobrado`), **desconto concedido** (Σ `desconto`) e o **déficit/lucro** contra a tabela da Viagem
— o uso que a revisão pediu explicitamente ("a tarifa por viagem serve para calcular balanços, déficits
ou lucros"). **Nesta rodada só se preparam/persistem os campos** que essas somas consomem; o **relatório**
(tela + agregação esperada×real×déficit) fica para **ADR próprio** (decisão da revisão).

### 8. Cota de gratuidade — máx. 2 por categoria, por viagem (validação por contagem)

Cada **viagem** concede no máximo **2 gratuidades por categoria** (2 `IDOSO`, 2 `PCD`, 2 `CRIANCA_ATE_5`,
2 `PASSE_FEDERAL`) — cota de assento livre da travessia (decisão da revisão). Como a gratuidade existe
**só na acomodação `REDE`** (§4, revisão 2026-07-26), a cota conta apenas passagens de rede. A guarda é uma
**validação por contagem, firestore-driven**: antes de emitir uma passagem com gratuidade, **conta as gratuidades já
emitidas para aquela `viagemId` + categoria**; ao atingir 2, **bloqueia** a emissão (fail-closed). A
contagem lê a fonte da verdade (Firestore, ADR-0009/design firestore-driven), não só o espelho local — a
gratuidade pode ter sido emitida em outro device.

> **Ressalva de concorrência (assumida).** Contagem-e-emite não é atômico: dois operadores emitindo a
> 2ª/3ª gratuidade simultaneamente podem ambos passar no *count* e furar a cota. A fronteira real seria a
> regra Firestore, mas **contar coleção em `firestore.rules` é caro/inviável** (não há agregação barata) —
> então a cota fica como **validação de aplicação**, não invariante do servidor. Endurecer (contador
> transacional, ou `count()` agregado) fica como dívida anotada, no espírito do "dever de paridade" do
> ADR-0012 — mas aqui a paridade servidor é reconhecidamente parcial.

## Plano de migração (faseado, aditivo — "stack dormente" do ADR-0003)

- **Fase 1 — Núcleo de domínio (só aditivo, sem UI, sem tocar o mapper). FEITA.** `TipoPassagem` +
  `TipoGratuidade` (`de()`/`.name`/`rotulo()`, espelho de `StatusPassagem`); regra pura
  `TipoPassagem.tarifaDevida(base)` (inteira=base, meia=base÷2 scale 2 UP, gratuidade=0) e
  `descontoDerivado(tarifaDevida, valorCobrado)` (resíduo abaixo da devida, piso 0). `CORTESIA` já não é
  `TipoGratuidade` (`de("CORTESIA")`→null). Testes JVM verdes (`testDebugUnitTest` BUILD SUCCESSFUL:
  `TipoPassagemTest`/`TipoGratuidadeTest`/`CalculoTarifaTest`). **A parte destrutiva foi adiada para a
  Fase 2** (decisão da revisão, opção B): religar o mapper e remover o legado só faz sentido *com a
  `tarifaBase` real* — antes disso seria um meio-termo com base falsa (`300` global).
- **Fase 2 — Tabela de tarifa (passageiro) + religar o mapper e remover o legado. FEITA.** Tarifa por
  acomodação como **mapa no `ViagemDocumento`** (Firestore) espelhado na **tabela-filha
  `TarifaViagem(viagemId, chave, valor)`** (Room, nova `@Entity` + DAO + migração v13→v14); o mapper achata
  mapa↔linhas (`TarifaViagemExtensions`); `ViagemFirestoreRepository` espelha as tarifas no sync/refresh
  (delete+insert por viagem) e as escreve no doc no save (`salvar(viagem, tarifas)`; porta ganhou
  `obterTarifas`). Form de Viagem coleta a tarifa por acomodação (input dinâmico do catálogo, branco = não
  ofertada; `TarifaInputUiState` + validação número>0 + UI `FormTextFieldBrownLeadingIconLabelText`).
  `tarifaBase` congelada na emissão da Passagem (novo campo `Double?`, migração v14→v15; resolvida por
  acomodação, preservada na edição). **Fatia destrutiva:** `PassagemDadosPassagemMapper` deriva
  `tarifaDevida`/`desconto` da `tarifaBase` (fallback p/ null); removidos `TARIFA_ANTAC`/`DESCONTO_ANTAC`,
  `obterTotalTarifa`, `getValorTotal`, `CalculoDesconto` (+ teste) e o acúmulo ANTAC em `FormPassagemHelper`;
  `CardValor` do form de pagamento religado à `tarifaBase` real; campo de desconto manual removido. Suíte
  JVM verde. (`CORTESIA` já saíra na Fase 1.)
- **Fase 2b — Guardas de emissão (PENDENTE).** **Célula ausente ⇒ emissão bloqueada (fail-closed)** — sem
  tarifa tabelada não há como medir desconto/déficit. **Cota de gratuidade** (§8): função de validação por
  contagem + query de contagem no repositório (`viagemId` + categoria, firestore-driven), que **bloqueia** a
  emissão ao atingir 2. Balanço inalterado.
- **Fase 3 — Tarifa de veículo (célula p/ carro/carreta/caminhão + regra p/ moto).** Classe tarifária de
  veículo como eixo de preço; `CARRO/CARRETA/CAMINHAO` como células na tabela-filha `TarifaViagem`
  (acrescentar `CARRETA` ao catálogo). **Moto:** campo de **cilindrada** novo no form de veículo (dropdown;
  hoje ausente em `FormVeiculoUiState`) + função pura `tarifaMotoBase(cc) = floor(cc/100)*100`. Emissão de
  veículo congela `tarifaBase` (da célula, ou da regra no caso da moto). Testes da regra da moto.
- **Fase 4 — Preparar os campos do balanço (só persistência, sem relatório).** Garantir que
  `tarifaBase`/`tarifaDevida`/`desconto` estejam disponíveis para agregação por `viagemId`/`navioId`; a
  **tela e a agregação esperada×real×déficit ficam para ADR próprio** (decisão da revisão). N+1/runBlocking
  do balanço seguem como dívida à parte.

## Consequências

- **Descontos viram analisáveis** — separados de meia/gratuidade (mandatórias). O balanço passa a medir
  receita esperada × real × déficit/lucro.
- **Uma tabela, dois eixos** — passageiro-por-acomodação e veículo-por-classe reusam a mesma estrutura;
  cresce em uma peça só.
- **Dever de paridade** — a regra de tarifa/desconto é pura e num lugar só (Fase 1); a `tarifaBase`
  congelada dá o âmbito de auditoria sem congelar a regra.
- **Grafia legada** — bilhetes antigos: `tipoPassagem`/`gratuidade` lidos via `de()` tolerante;
  `tarifaBase` ausente ⇒ derivação degrada com clareza (sem tarifa tabelada, cai no valor cobrado).
- **Novo dado de veículo** — cilindrada capturada por **dropdown** (valores discretos) para precificar
  moto; obrigatória para veículo-moto (sem ela a emissão bloqueia, como célula ausente).
- **`CORTESIA` aposentado** — cortesia deixa de ser gratuidade; onde existia, vira redução comercial
  (desconto). Uma verdade a menos no catálogo genérico.
- **Cota de gratuidade tem paridade de servidor parcial** — validada na aplicação por contagem firestore-
  driven; o servidor não a impõe (contar coleção em `firestore.rules` é inviável). Concorrência pode furar
  a cota (§8) — dívida anotada.

## Alternativas consideradas

- **Manter tarifa global + desconto como input (status quo)** — rejeitado: é a origem da conta circular e
  da meia embaralhada no desconto; inviabiliza análise.
- **Congelar `tarifaDevida`/`desconto` na Passagem** — rejeitado na revisão: mais colunas e duplicação de
  regra; só a `tarifaBase` é congelada (fonte única de cálculo).
- **Migrar dinheiro para `String` exato no Firestore/Room** — rejeitado na revisão (custo de migração);
  disciplina scale 2 fica no cálculo. Fica como futuro se a imprecisão de `Double` incomodar.
- **Preço único por viagem (sem matriz)** — rejeitado na revisão: veículo tem tarifa própria por tipo, e
  acomodação diferencia passageiro.

## Alternativas futuras

- **Tabela de tarifa versionada por vigência** (reajuste com histórico), em vez de sobrescrever a da
  Viagem.
- **Dinheiro exato** (String/inteiro-de-centavos) na fronteira.
- **Regra de gratuidade parametrizável** (ex.: idade da criança, validação de documento do idoso/PcD).
- **Moto por célula cadastrada na Viagem** (faixas de cilindrada com valor por travessia), substituindo a
  regra provisória `floor(cc/100)*100` quando a precificação da moto amadurecer.

## Decisões resolvidas na revisão (analista)

- **Matriz, não preço único** — veículo tem tarifa própria por tipo; acomodação diferencia o passageiro.
- **Congela só `tarifaBase`** na Passagem; `tarifaDevida`/`desconto` derivados no mapper (fonte única).
- **Dinheiro fica `Double?` na fronteira** (sem migração); disciplina scale 2/`RoundingMode.UP` no cálculo.
- **Tabela no Room = tabela-filha normalizada** `TarifaViagem(viagemId, chave, valor)` — várias tarifas-
  base por viagem, agregáveis por `viagemId`/`navioId` em SQL para o balanço (lado "SQL" do ADR-0003).
- **Classe tarifária de veículo = eixo novo, só de preço** — não reusa nem substitui o `tipoVeiculo`
  descritivo do form.
- **Moto por regra, não por célula** — cilindrada capturada por dropdown; tarifa provisória
  `floor(cc/100)*100` (piso à centena, 1:1 em reais). `CARRO/CARRETA/CAMINHAO` seguem por célula na tabela.
- **`CORTESIA` aposentado** — não vira `TipoGratuidade`; os quatro subtipos são as gratuidades legais.
- **Criança é até 5 anos, inclusive** — subtipo `CRIANCA_ATE_5`, faixa **0–5** (o 5 conta; a proposta
  inicial "0 a 6" foi corrigida).
- **Tipo tarifário só na REDE (revisão 2026-07-26)** — meia e gratuidade existem **apenas** na acomodação
  `REDE`; suíte/camarote/veículo são sempre `INTEIRA`. Em não-rede **não há preenchimento nem validação** de
  tipo tarifário (fallback de UX no form). Reconcilia a tensão levantada no estudo da contagem
  (`docs/design/balanco-passagens-mapper.md` §7): a gratuidade fica restrita à rede; o breakdown
  inteira/meia/gratuidade da contagem é, por consequência, rede-only por natureza.
- **Cota de gratuidade: máx. 2 por categoria, POR VIAGEM** — validação por contagem firestore-driven,
  bloqueia a emissão ao atingir 2 (§8; paridade de servidor parcial, ressalva de concorrência).
- **Célula ausente = fail-closed** — viagem sem tarifa para a acomodação/classe bloqueia a emissão.
- **Balanço: só preparar os campos** nesta rodada; relatório (esperada×real×déficit) em ADR próprio.

## Pontos abertos para a revisão (analista decide)

Nenhum — todas as decisões da revisão foram fechadas (ver *Decisões resolvidas*). Direção pronta para
implementação faseada.
