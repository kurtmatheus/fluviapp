# ADR-0027: O faseamento da F9 — seis fatias, e onde a Passagem é cortada

**Status:** Aceita em direção (decisões do analista em 2026-08-11) · sem código

**Insumos:** os quatro ADRs da Passagem — [0023](0023-passagem-por-categoria-e-referencia.md) (domínio),
[0024](0024-fronteira-de-dados-da-passagem.md) (fronteira), [0025](0025-camada-de-dados-da-passagem.md) (camada) e
[0026](0026-orquestracao-e-apresentacao-da-passagem.md) (orquestração e apresentação) — e os estudos
[`f9-passagens-terreno.md`](../design/f9-passagens-terreno.md),
[`orquestracao-passagem.md`](../design/orquestracao-passagem.md) e
[`apresentacao-passagem.md`](../design/apresentacao-passagem.md).

---

## Contexto

O estudo do terreno (2026-08-11) propôs oito fatias e se declarou **"proposta medida, não plano aprovado"**,
porque duas perguntas estavam abertas: **onde cortar a fase** e **em que ordem**. Naquele momento o corte
dependia de decisões que ainda não existiam. Elas existem agora — e mudaram o desenho o suficiente para que o
plano se reescreva em vez de se emendar.

Este ADR **supera o §7 daquele estudo**.

## Decisão

### D1 — A ordem é a que o ADR-0026 D8 fixou

**Domínio → dados → (camada + orquestração) → apresentação → a seção acende.**

Duas justificativas de ordem, e as duas são técnicas, não de gosto:

- **camada e orquestração na mesma fatia**, porque mexem nos mesmos arquivos: a porta entra no ViewModel no mesmo
  movimento em que a classe auxiliar perde o repositório;
- **a `Passagem` sai do Room antes de qualquer mudança de forma**, porque enquanto ela for tabela **cada campo
  novo é DDL** (a restrição que o ADR-0018 já registrava contra o ADR-0017).

### D2 — O corte: o que está dentro da F9

| Dentro | Por quê |
|---|---|
| os tipos do agregado, o `Cliente` e o `Veiculo` | **o agregado referencia `clienteId` e `veiculoId`** (ADR-0023 D3/D5): sem os pools não há emissão. O corte que o estudo do terreno deixou aberto foi **respondido pelo domínio** — não é fase própria, é pré-requisito |
| o cancelamento (`CANCELADA` na FSM) | entra **junto** com a saída do *delete* (ADR-0024 D11). Tirar o delete sem ter cancelar deixaria o operador sem saída nenhuma para um bilhete errado |
| a emissão **por etapas** | ADR-0026 D4: é o que resolve os 47 parâmetros, e é consequência direta da categoria como raiz |
| a numeração por ocorrência e o contador atômico | ADR-0024 D6: sem ele, duas vendas simultâneas colidem |

| Fora | Por quê |
|---|---|
| **ocupação, balanço e análise** | **não têm domínio planejado.** Entram quando forem planejados — e não se estima o que elas cobram (é a restrição de método do ADR-0025) |
| **o snapshot como passagem incompleta** e a tela de recuperação | **nota lateral** (ADR-0026): *acrescenta função*, e a F9 existe para recuperar o que já existia |
| **a impressão em papel** | surface própria, estudo próprio, fora do MVP |
| **a inferência tarifária** | módulo de faturamento (ADR-0018 D12); a emissão não calcula preço — *preço é I/O* |

**O que as seções não revitalizadas recebem:** nada de correção. `Contagem` e `Balanço` leem a `Passagem` e vão
quebrar de forma quando ela mudar — e **é assim que fica**, marcado com `// REVITALIZAÇÃO:` no ponto exato, como
a F8.0 fez com o `FormPassagemHelper`. Consertar código que nenhuma tela alcança é gastar hoje o que se vai
reescrever quando o domínio delas for planejado.

### D3 — As seis fatias

| Fatia | Entrega | Fecha |
|---|---|---|
| **F9.1 — O domínio** | os tipos puros e JVM-testáveis: `Passagem` selada por categoria, `Acomodacao` (com os tipos tarifários que admite), `ClasseVeiculo` (+`VAN`, `SUV`, `exigeModelo`), `Cliente`, `Veiculo`, `Lancamento`, `MetadadosPassagem`, `OcorrenciaViagem`, e **`CANCELADA` na FSM**. Morrem `ModoPassagem` como eixo único, `ResultadoEmissao.SemTarifa`, `tarifaBase` e `tarifasViagem`; o **D19 é corrigido no tipo**, não no validador | ADR-0023 inteiro |
| **F9.2 — Os dados** | a `Passagem` **sai do Room**; codec com **discriminador**, `CriterioPassagem`, porta `PassagemRepository`, contador em **subcoleção** com `increment`, telemetria com os três desfechos renomeados. Regras: `allow delete: if false`, a transição `CANCELADA` e o caso de emulador que hoje afirma o contrário **invertido** | ADR-0024, ADR-0025 D1/D2/D5 |
| **F9.3 — Os pools** | `clientes/` e `veiculos/` com codec, porta, **criar-ou-assinar** e `obterPorIds` em lote; regra + emulador + **índice composto** no mesmo incremento (é PII). Morre o `getListaNome()` vazio | ADR-0023 D5, ADR-0025 D6 |
| **F9.4 — Camada + orquestração** | junção como **função pura** (coletar é dos dados, traduzir é do domínio), DTO **por consumidor** para a emissão e o bilhete, VM como **único escritor**, auxiliares **sem handle e sem repositório**, `UiState` **por categoria**, **evento one-shot**. Saem `Context` do VM, o `lateinit` exposto e o `scrollParaErro`. **Primeiro teste de ViewModel da emissão** | ADR-0025 D3/D4, ADR-0026 D1/D2/D3 |
| **F9.5 — A apresentação** | a **emissão por etapas**, a **camada fina de formatadores**, o **argumento de rota opcional** (e a extensão `isTextoNaoNulo` apagada, com os dois usos), a **máscara de placa** no precedente da F8, e o bilhete digital indo para a **galeria** | ADR-0026 D4/D5/D6, ADR-0017 D5 |
| **F9.6 — A seção acende** | `PASSAGEM` em `SECOES_REVITALIZADAS`, o recorte por **`agenciaId`** na consulta, as classes congeladas de volta à suíte de escopo (reescritas onde afirmam a forma antiga), e as **três redes verdes** | a F9 |

### D4 — A definição de pronto de cada fatia

Vale para todas, e não é rigor extra — é o que a rc.3 do Porto cobrou (ADR-0022 D6):

1. **regra publicada**, não só versionada — o job de deploy roda depois do commit de `firestore.rules`;
2. **coberta na suíte de emulador**, incluindo os casos que passam *de propósito* para documentar um limite;
3. **JVM verde no escopo**;
4. **e, nas fatias que têm tela (F9.5, F9.6), teste de tela em aparelho** — o pré-requisito que a F8 cumpriu ao
   fim e que nasceu de dois defeitos que atravessaram todas as suítes de JVM e morreram só no aparelho.

### D5 — O que a F9 não pode apagar

Duas coisas que não são entrega desta fase, mas que ela precisa **preservar**:

- **a porta `RascunhoStore` e o rascunho no Room** — é o caminho por onde a passagem incompleta vai nascer
  (nota lateral do ADR-0026). O Room termina a F9 com `Usuario`, `Constante` e o rascunho;
- **a `Passagem` como tipo que não admite incompleto** — é o que obrigará o incompleto a ser outro tipo, em vez
  de nulos espalhados pelo agregado.

## Consequências

- **a F9 tem seis fatias e não oito**, porque duas do plano antigo (preço; forma do documento) se dissolveram nas
  decisões: preço é I/O e a forma vem do tipo selado;
- **o Room termina a F9 vivo**, com três habitantes e uma razão declarada para cada — o que precisa o
  Firestore-only em vez de enfraquecê-lo;
- **a F10 (o Início da plataforma) continua sendo a última** do ADR-0022, e nada aqui a antecipa;
- **ocupação e balanço saem da F9 sem dívida oculta**: ficam marcados no código, e o que os define é o domínio
  deles, quando existir;
- **o andaime da revitalização se esvazia** ao fim da F9.6: `SECOES_REVITALIZADAS` iguala `SecaoMenu.entries`
  menos o que nunca existiu, e o `EscopoRevitalizado` passa a poder morrer.

## O que este ADR não decide

- **quantas etapas a emissão tem** e o que vai em cada uma (o ADR-0026 D4 fixou o eixo, não o roteiro);
- **o domínio de ocupação, balanço e análise**, e por consequência as projeções deles;
- **a construção do snapshot como passagem incompleta** (nota lateral);
- **datas**. Fatia é unidade de trabalho, não de calendário — e o histórico deste repositório mostra por quê:
  a F8 foi de cinco fatias e a demolição do trecho não estava prevista em nenhuma delas.