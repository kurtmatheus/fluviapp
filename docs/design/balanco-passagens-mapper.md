# Estudo — o `BalancoPassagensMapper` hoje (ocupação): estrutura, threading e o que de fato conta

**Status:** Rascunho — Claude escreveu, revisão do analista pendente. Fotografia do estado corrente em
`2026-07`. Diferente do [estudo do balanço financeiro](balanco-financeiro.md) (que propõe *adicionar*
dimensões de dinheiro): aqui o foco é o **mapper de ocupação como ele já é** — como está estruturado, o que
apresenta, a real situação de threading/eficiência, e onde **o que ele apresenta diverge do que de fato
calcula**.

> Conversa com o [ADR-0008](../adr/0008-relacionamentos-por-identidade.md) (agrega pelo `navioId`
> congelado), o [estudo de domínio da passagem](dominio-passagem.md) (participantes do agregado) e o
> [ADR-0013](../adr/0013-tabela-de-tarifa-e-tipo-tarifario.md) (categorias/gratuidade).

---

## 1. O pipeline hoje

```
BalancoScreen ("Gerar", data)
   └─ BalancoViewModel.atualizarLista(data)                       BalancoViewModel.kt:44
        ├─ passagemRepository.obterTodasPorData(data).await()     PassagemFirestoreRepository.kt:140
        │     → Firestore .get() por FIELD_DATA_VIAGEM (1 query), Task<QuerySnapshot>
        ├─ snapshot.documents.mapNotNull { toObject<PassagemDocumento>()?.toPassagem(id) }   (em memória)
        ├─ balancoPassagensMapper.map(listaPassagemNova)          BalancoPassagensMapper.kt:26  (suspend)
        └─ helper.atualizarDadosBalanco(...) + atualizarProcessamento()
```

O `map` (`BalancoPassagensMapper.kt:26-38`):
1. `navioRepository.obterTodos()` **uma vez** (`:29`) — leitura do Room (`NavioFirestoreRepository.kt:47`,
   `dao.obterTodos().first()`).
2. `entry.groupBy { it.navioId }` (`:32`) — agrupa pelo id congelado (ADR-0008, rename-safe).
3. `mapNotNull { (navioId, passagens) -> navios.firstOrNull { it.id == navioId } ?: return@… null; contador(navio, passagens) }`
   (`:33-37`) — órfão (navioId sem navio vivo) descarta o grupo.
4. `contador(navio, passagens)` (`:40-149`) — um laço único sobre as passagens do grupo, acumulando
   contadores, devolvendo `DadosBalancoPassagem` (`model/screendata/DadosBalancoPassagem.kt`).

## 2. O que ele apresenta

Um card expansível por navio (`BalancoNavioCard.kt`), **puramente de ocupação — nenhum campo de dinheiro**:

| Bloco (card) | Campos | Como conta hoje |
|---|---|---|
| **Redes** (contagem crua) | `preenchidasRedes` | 1 por passagem de acomodação REDE |
| ↳ indentado sob Redes | `preenchidasInteiras`, `preenchidasMeias`, `preenchidasGratuidades` | por `tipoPassagem` — **só dentro do ramo REDE** |
| **Veículos** `preench/capac` | `preenchidosVeiculo`/`capacidadeVeiculos` + carros/motos/caminhões/carretas | 1 por passagem de veículo, por `tipoVeiculo` |
| **Suítes** `preench/capac` | `preenchidasSuitesGeral`/`capacidadeSuitesGeral` + suites2/suites3 | por **presença de passageiro 2/3** (ver §4) |
| **Camarotes** `preench/capac` | `preenchidosCamarotes`/`capacidadeCamarotes` | 1 por passagem de acomodação CAMAROTE |

`capacidade*` vêm do `Navio` resolvido por id (`capacidadeVeiculo`, `capacidadeSuite2+3`, `capacidadeCamarote`).
Redes não tem capacidade (área de rede, sem lotação fixa). Resposta do app: *"quantos lugares foram
ocupados"*.

## 3. Threading e eficiência — o que **de fato** acontece

**Correção de rota:** tanto a memória do projeto quanto o [estudo financeiro §4](balanco-financeiro.md)
afirmam que *"a leitura das passagens do balanço faz `runBlocking { dao.salvar }` por doc (N+1)"*. **Isso não
é verdade para o caminho do balanço.** Confusão entre dois métodos irmãos do repositório:

- **`obterTodasPorData(data)`** (`PassagemFirestoreRepository.kt:140`) — o que o **balanço** usa. É um
  `.get()` puro que devolve `Task<QuerySnapshot>`. **Não** persiste no Room, **não** tem `runBlocking`. A VM
  faz `.await()` dentro de `viewModelScope.launch` (`BalancoViewModel.kt:45-46`) e mapeia em memória.
- **`obterTodasPorDataStatus(...)`** (`:168`) — o que a **tela de pesquisa** (`PesquisarPassagemViewModel`)
  usa. **Esse** sim tem o anti-padrão: dentro do `addOnSuccessListener` (callback não-suspenso) faz
  `.forEach { runBlocking { dao.salvar(it) } }` (`:183` e `:193`) — N+1 de escrita no Room, bloqueando a
  thread do callback.

Ou seja: a dívida de threading real **existe, mas não no balanço** — mora no `obterTodasPorDataStatus` da
pesquisa. O `BalancoPassagensMapper` já foi saneado (ADR-0008 `ea151c1`: `obterTodos` 1x, sem ida à Viagem
viva; e `9aef6de`: `Mapper.map` virou `suspend`, saiu o `runBlocking`). **O mapper do balanço, hoje, está
limpo de threading.**

O que resta de eficiência no balanço é **pequeno e algorítmico**, não de threading:

- **Lookup linear do navio** (`:35`): `navios.firstOrNull { it.id == navioId }` roda por grupo → O(grupos ×
  navios). Um `navios.associateBy { it.id }` fora do laço deixa O(1) por grupo. Ganho real só com muitos
  navios; é limpeza barata que **não muda o resultado**.
- **Carrega todas as passagens da data em memória** para agrupar. É inerente a "somar por navio" e a
  contagem é O(passagens) num passe único — aceitável. Não há paginação; se um dia uma data tiver milhares
  de bilhetes, medir.

**Conclusão de threading:** o pedido "refatorar a threading do balanço" tem uma resposta honesta — **no
mapper do balanço não há o que refatorar**; o alvo de threading (o `runBlocking` N+1) é o
`obterTodasPorDataStatus` da **pesquisa**, uma fatia irmã e separada. Vale corrigir a afirmação herdada nos
docs (feito aqui).

## 4. O que se **propõe** vs. o que **resulta** — inconsistências de contagem

Aqui está o achado de maior valor: o `contador` produz números que **divergem do que os rótulos do card
prometem**. Todos são candidatos a confirmação de intenção com o analista (estou inferindo o domínio a
partir do código).

### 4.1 Suíte: `preenchidasSuitesGeral` conta **passageiros extras**, mas é exibido contra **nº de suítes**

O ramo SUITE (`BalancoPassagensMapper.kt:81-91`):
```
SUITE -> if (temPassageiro2) { preenchidasSuite++; preenchidasSuite2Pessoas++ }
         if (temPassageiro3) { preenchidasSuite++; preenchidasSuite3Pessoas++ }
```
O titular (passageiro 1) **nunca** é contado; a conta só reage a p2/p3. Traçando (lembrando que o form só
libera p3 depois de p2, então "3 pessoas" ⇒ p2 e p3 ambos):

| Ocupação da suíte | `SuitesGeral` | `Suites2` | `Suites3` |
|---|---|---|---|
| só titular (1 pessoa) | **0** | 0 | 0 |
| titular + 1 (2 pessoas) | 1 | 1 | 0 |
| titular + 2 (3 pessoas) | **2** | **1** | 1 |

Dois problemas:
- **Suíte de 1 ocupante fica invisível** (0). O form permite emitir suíte só com o titular (o checkbox de
  passageiro 2 é opcional fora da rede) — mas o balanço não a enxerga.
- **Suíte de 3 pessoas conta como 2** no geral e ainda **marca uma suíte-de-2 E uma suíte-de-3**. Um único
  bilhete de uma suíte de 3 lugares aparece ocupando duas suítes.

A raiz: `preenchidasSuitesGeral` está, de fato, contando **"passageiros além do titular"** (nº de p2 + nº de
p3), enquanto `capacidadeSuitesGeral` conta **suítes** (`capacidadeSuite2 + capacidadeSuite3`). O card compara
`preenchidas/capacidade` como se fossem a mesma unidade — não são. O denominador é "suítes"; o numerador é
"passageiros extras". Daí o ratio poder estourar (preenchidas > capacidade) numa viagem cheia de suítes de 3.

**Pergunta ao analista:** a unidade da ocupação de suíte é **suíte** (1 bilhete de suíte = 1 suíte ocupada,
classificada como de-2 ou de-3 pela lotação) ou **leito/pessoa**? A regra correta muda conforme a resposta —
mas dificilmente é a atual (que mistura as duas).

### 4.2 O breakdown inteira/meia/gratuidade é **só de rede**, mas os rótulos são genéricos

A contagem por `tipoPassagem` mora **dentro** do ramo `REDE` (`:64-78`). Para suíte e camarote, o
`tipoPassagem` (inteira/gratuidade — meia é filtrada fora da rede no form) **não é tabulado**. Então
`preenchidasInteiras` = *inteiras de rede*, não *inteiras da travessia*.

No card, esses três aparecem indentados logo abaixo de "Redes" (`BalancoNavioCard.kt:58-76`) — o que
*sugere* que o breakdown é escopo-rede de propósito. Mas os rótulos são genéricos (`label_total_inteiras`,
`label_total_gratuidade`) e há um efeito colateral de domínio: a **cota de gratuidade do ADR-0013 é por
viagem, sobre todas as acomodações**. Um gestor lendo "gratuidades: 1" no balanço perde as gratuidades de
suíte/camarote — o número não bate com a cota que o próprio app fiscaliza na emissão.

**Pergunta ao analista:** o breakdown deve ser **escopo-rede** (então os rótulos precisam dizer "da rede") ou
**da travessia inteira** (então a tabulação precisa sair do ramo REDE)?

### 4.3 Filosofia de contagem inconsistente entre acomodações

- **REDE** conta **1 por bilhete** (o titular). Coerente: rede é individual (o form esconde o checkbox de
  p2 quando `ehAcomodacaoRede`).
- **CAMAROTE** conta **1 por bilhete** (`:93-95`), independente de ter p2/p3 → **por bilhete/cabine**.
- **SUÍTE** conta por **presença de p2/p3** → **por passageiro extra** (§4.1).

Três acomodações, dois eixos de contagem diferentes. Rede e camarote contam "unidades vendidas"; suíte conta
"pessoas extras". Para um relatório de ocupação coerente, o eixo precisa ser único e explícito.

### 4.4 Menores

- **REDE com `tipoPassagem` fora de {inteira, meia, gratuidade}** cai no `else -> {}` (`:77`): entra em
  `preenchidasRedes` mas em nenhum sub-bucket → `inteiras+meias+gratuidades ≤ redes` (pode não fechar).
- **Bilhete de veículo não conta passageiro.** `if (!ehVeiculo) {…} else {veículo}` (`:59`) — um bilhete de
  veículo é só veículo, nunca acomodação. Parece correto (veículo ocupa vaga de veículo), mas vale registrar
  que o **responsável pela retirada** (uma pessoa) não entra em nenhuma contagem de pessoas.

## 5. Melhorias que **preservam** o resultado atual (mecânicas)

Independentes das decisões de domínio do §4 (essas mudam números, precisam de aval):

- **`associateBy` para o lookup de navio** (`:35`) — O(grupos×navios) → O(grupos). Resultado idêntico.
- **Corrigir os docs herdados** que atribuem o `runBlocking` N+1 ao balanço (é do `obterTodasPorDataStatus`
  da pesquisa) — feito neste estudo; a memória e o `balanco-financeiro.md §4` merecem a mesma correção.
- (Opcional, sem mudar resultado) extrair o `contador` para uma função pura testável por acomodação — hoje
  já é determinístico, mas um teste por caso de suíte/camarote deixaria o §4 travado quando for decidido.

## 6. Perguntas para o analista (semear a decisão)

1. **Suíte (§4.1):** a unidade de ocupação é **suíte** ou **pessoa**? (isso decide a regra correta e conserta
   o double-count / a suíte-solo invisível).
2. **Breakdown (§4.2):** inteira/meia/gratuidade é **da rede** (rotular assim) ou **da travessia** (mover a
   tabulação para fora do ramo REDE)? Casa com a cota de gratuidade por viagem do ADR-0013.
3. **Eixo de contagem (§4.3):** unificar "por bilhete/unidade" para todas as acomodações?
4. **Threading:** confirmar que o alvo real é o `obterTodasPorDataStatus` da **pesquisa** (não o balanço) —
   e se ele entra no mesmo movimento ou vira fatia própria.
5. **Escopo:** corrigir a contagem (§4) é o próximo passo, ou primeiro só a limpeza mecânica (§5) que não
   mexe em números, deixando a semântica para quando o balanço financeiro (ADR-0014) for desenhado junto?

## 7. Decisões do analista (2026-07-26) — o que vira direção

**Renomeação/reposicionamento do módulo (resposta à Q5):**
- Este módulo é **Contagem de Passagem** (ocupação), não "balanço". Corrigir a semântica no nome (menu,
  títulos, classes onde couber sem quebrar).
- **Faturamento vira módulo separado** (opção de menu própria) com a ação **"gerar balanço financeiro"** — é
  para lá que o [ADR-0014](../adr/0014-balanco-financeiro-da-travessia.md) migra. Ocupação e dinheiro deixam
  de compartilhar tela.
- **Visibilidade por cargo: NÃO na contagem** (revisão 2026-07-26). O **OPERADOR vê a contagem GERAL** —
  precisa conhecer os números da viagem para fins de orientação. O **filtro por cargo fica só no Faturamento**
  (dinheiro), onde faz sentido restringir. A contagem é informação operacional, aberta a todos os cargos.

**Q1 — Suíte (§4.1) DECIDIDO:** a unidade é a **suíte (o bilhete)**, não a pessoa. Só há suítes de **2 ou 3
pessoas**; só o **titular é obrigatório** (p2/p3 opcionais). Regra correta:
- cada bilhete de suíte → `preenchidasSuitesGeral += 1` (uma suíte ocupada, mesmo com só o titular);
- `temPassageiro3` (3 nomeados; p3 ⇒ p2) → bucket **3 pessoas**; senão → bucket **2 pessoas**.
Conserta o double-count do trio (era 2) e a suíte-solo invisível (era 0); `Geral = suites2 + suites3`, casando
com a capacidade.

**Q2 — Tipo de passagem (§4.2) DECIDIDO + vira achado de FORM:** o **tipo tarifário (inteira/meia/gratuidade)
só existe para REDE**. Para acomodação ≠ REDE **não há preenchimento nem validação** de tipo de passagem —
o form precisa de um **fallback de UX**: esconder o dropdown de tipo de passagem (e não validá-lo) quando a
acomodação não é rede. Consequência para a contagem: o breakdown inteira/meia/gratuidade é **rede-only por
natureza** (não há tipo em suíte/camarote) — mantém-se onde está, não se move para fora do ramo REDE.
> ✅ **RESOLVIDO (2026-07-26): gratuidade só na rede.** O analista confirmou que o tipo tarifário
> (meia/gratuidade) existe **apenas na acomodação REDE**; suíte/camarote/veículo são sempre `INTEIRA`. O
> [ADR-0013](../adr/0013-tabela-de-tarifa-e-tipo-tarifario.md) foi ajustado (§4 + §8 + Decisões resolvidas).
> Consequência: o breakdown inteira/meia/gratuidade da contagem é rede-only **por natureza** (não há tipo em
> suíte/camarote), e a **fatia 1** (form) esconde/não-valida o tipo tarifário quando a acomodação não é rede.

**Q3 — Contagem de todas as acomodações DECIDIDO:** a **ocupação conta todas as acomodações** (rede, suíte,
camarote, veículo), com contagem **por bilhete/unidade** uniforme (resolve §4.3 — some a divergência de eixo:
suíte deixa de contar "pessoas extras" e passa a contar suítes, como rede/camarote contam unidades).

**Q4 — Threading:** a intenção é **reaproveitar `obterTodasPorDataStatus`**, mas **verificar se é a melhor
alternativa** antes — o método hoje faz `runBlocking { dao.salvar }` por doc no `addOnSuccessListener`
(N+1 bloqueante). Avaliar trocar por `suspend`/`.await()` + cache em lote (como o balanço já faz com
`obterTodasPorData`). Fatia própria, não bloqueia as demais.

**Fatiamento resultante (proposta, cada fatia = 1 turno verde):**
1. **FORM** — fallback do tipo de passagem só-rede (não exibe/valida `tipoPassagem` quando acomodação ≠ REDE).
2. **Contagem — suíte** (Q1): 1 por bilhete, bucket por `temPassageiro3`; + `associateBy` no lookup de navio
   (§5, mecânico). Teste por caso (solo/dupla/trio).
3. **Rename** módulo → *Contagem de Passagem* + **visibilidade por cargo** (operador próprio / gestores geral).
4. **Faturamento** módulo separado (menu) — migra o ADR-0014 para lá.
5. **Threading** (Q4) — avaliar/refatorar `obterTodasPorDataStatus`.

A ordem-fonte da confusão (Viagem × Trecho) está em [viagem-vs-trecho.md](viagem-vs-trecho.md) — rework maior,
à parte.
```