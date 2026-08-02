# Estudo de design — DTO por entidade ou por caso de uso?

**Status:** Rascunho — Claude mapeou, decisão do analista pendente. Nasce do **ponto aberto 10** do
[ADR-0016](../adr/0016-dominio-da-plataforma.md), que em 2026-08-01 saiu da lista de lá para virar estudo
próprio: *"é decisão de camada, não de domínio, e tem alcance sobre todas as coleções"*. Ancorado no código
em `2026-08-01`.

> Conversa com o [ADR-0003](../adr/0003-modelo-de-memoria-do-dado.md) (formas do dado), o
> [ADR-0016](../adr/0016-dominio-da-plataforma.md) (7ª rodada: **entidade = lei · DTO = trânsito · documento
> = armazenamento**), o [ADR-0017](../adr/0017-eixo-de-storage-firestore-only.md) (o Room sai — e a classe
> plana **muda de camada** em vez de sumir) e o
> [ADR-0018](../adr/0018-agregado-passagem-participantes-modo-e-lancamentos.md) (o agregado deixa de ser
> plano).

---

## 1. A pergunta está mal posta — o app já tem os dois

A pergunta original — *"um DTO por entidade ou um por caso de uso?"* — supõe uma escolha a fazer. **O código
já fez as duas**, em camadas diferentes, e a decisão real é sobre **a segunda**:

```
PassagemDocumento ──► Passagem ──► DadosPassagem ──► telas
  (documento,          (entidade/DTO      (projeção de       (5 consumidores
   25 campos)           plano, 47)         tela, 58)          diferentes)
```

- **Por entidade** existe e é o meio: `Passagem`, `Viagem`, `Navio`… — hoje a classe plana anotada do Room,
  que com o ADR-0017 perde as anotações e **continua existindo** como o DTO de trânsito (é o que o ADR-0016
  §9 quis dizer com *"não some — muda de camada"*).
- **Por caso de uso** também existe: é o pacote `domain/screendata/` — `DadosPassagem`, `DadosViagemCard`,
  `DadosContagemPassagem`, `DadosImpressora`, `DadosBotoesMenus`.

Só que **`screendata` não é por caso de uso: é por entidade com outro nome.** Há **um** `DadosPassagem`
servindo cinco telas com necessidades muito diferentes — e é daí que vem todo o custo medido abaixo.

> Nota de vocabulário: o app **não tem camada de caso de uso** (não há `usecase/`; a orquestração vive nos
> ViewModels e helpers). "Caso de uso" aqui significa **o consumidor concreto** — uma tela, um relatório, a
> impressão.

## 2. A medida — `DadosPassagem` como caso extremo

`domain/screendata/DadosPassagem.kt` tem **58 campos**: mais que a entidade `Passagem` (47) e mais que o
dobro do documento no Firestore (25). Quanto cada consumidor de fato usa:

| Consumidor | Campos usados | Desperdício |
|---|---|---|
| `ImpressaoPassagem` | **41** / 58 | 29% |
| `EmissaoPassagemDigitalDialog` (bilhete digital) | **30** / 58 | 48% |
| `ResultadosPassagemSearchScreen` (lista) | **11** / 58 | 81% |
| `PassagemPreviewCard` | **10** / 58 | 83% |

A lista de resultados usa exatamente estes onze: `numero`, `idPassagem`, os três nomes de passageiro, os
três documentos, `placaVeiculo` e os dois campos do responsável. **Nenhum de empresa. Nenhum de navio.**

### 2a. O custo não é memória — é leitura

`PassagemDadosPassagemMapper` resolve **empresa por id** e **navio por id** para preencher sete campos
(`empresaNome`, `empresaRazaoSocial`, `empresaCnpj`, `empresaEndereco`, `empresaTelefone1/2`, `navio`). E o
mapper é aplicado **item a item** na pesquisa:

```kotlin
val listDadosPassagemCard = listaPassagemFiltered.map { passagem ->
    dadosPassagemMapper.map(passagem)          // PesquisarPassagemViewModel.kt:132-134
}
```

Numa busca que devolve 50 bilhetes, são **100 leituras de cadastro** para preencher campos que **aquela tela
não mostra**. É o `N+1` que o ADR-0014 e o §9 do estudo do agregado registram como dívida — e a causa raiz
não é o laço: **é o DTO único**. Um DTO da lista não teria de onde puxar empresa, porque não teria o campo.

### 2b. Três sintomas menores, do mesmo lugar

- **Tudo vira `String`, inclusive dinheiro** (`tarifa`, `valorTotal`, `valorPix`…) e datas. A formatação
  acontece **no mapper**, então quem consome não pode reformatar nem somar — e o `BigDecimal` disciplinado do
  ADR-0013 morre na fronteira de saída.
- **Regra duplicada.** `tem2Pessoas`, `ehVeiculo`, `ehRede` repetem derivados que a entidade já tem
  (`temPassageiro2`, `ehVeiculo`) — e `ehRede` compara com `REDE.name` do catálogo, que o ADR-0016 F1
  aposenta. Duas cópias da mesma regra em duas camadas.
- **Campos que nunca foram preenchidos.** `idPassageiro1/2/3` e `idVeiculo` existem no DTO e o mapper grava
  `""` em todos (`PassagemDadosPassagemMapper.kt:89,97,118`). São a **cova** que o ADR-0018 D1 vem preencher
  — a projeção previu a identidade dos participantes antes de o domínio a ter.

### 2c. As outras projeções estão saudáveis

`DadosViagemCard` (13 campos), `DadosContagemPassagem` (18), `DadosImpressora` (2), `DadosBotoesMenus` (3).
Cada uma serve **um** consumidor, e nenhuma apresenta o problema. **O problema não é o padrão — é a
`DadosPassagem` ter virado o depósito comum de cinco telas.**

## 3. O que o Firestore permite (e o que não)

Um argumento que costuma decidir esta discussão **não vale aqui**: o SDK Android **não expõe projeção de
campos** (`select()` existe no protocolo, não na API do cliente; `grep '.select('` no projeto: nenhum
resultado). O documento **vem inteiro do servidor**, com 25 campos, queira a tela um ou vinte.

Portanto, **DTO por consumidor não reduz custo de rede na coleção principal**. O que ele reduz é:

1. **leitura de outras coleções** (o `N+1` do §2a) — este é o ganho real e mensurável;
2. **acoplamento**: hoje mudar um campo para a impressão obriga a olhar cinco telas;
3. **trabalho de CPU e alocação** por item de lista (formatar 58 campos para usar 11).

E o que ele **custa** é multiplicação de classes e de mappers — o argumento que sustentava "um por entidade"
no ADR-0016.

## 4. As três formas possíveis

| Forma | O que seria | Custo | Ganho |
|---|---|---|---|
| **A. Um por entidade** (assunção atual) | `DadosPassagem` continua servindo todo mundo | o `N+1` fica; acoplamento fica | menos classes |
| **B. Um por consumidor** | `DadosBilheteImpresso`, `DadosPassagemLista`, `DadosPassagemDetalhe`… | +3 classes e +3 mappers só para passagem | mata o `N+1`; cada tela declara o que precisa |
| **C. Híbrido por natureza da leitura** | **lista** e **detalhe/impressão** são coisas diferentes; o resto segue por entidade | +1 classe e +1 mapper | pega ~90% do ganho de B com ~30% do custo |

**C é o que os números sugerem**: os dois extremos (41/58 na impressão e 10-11/58 nas listas) não são
variações do mesmo consumidor — são **dois regimes de leitura**. Um quer o bilhete inteiro resolvido; o
outro quer identificar uma linha. Detalhe e impressão podem continuar juntos (41 e 30 campos, com forte
sobreposição).

## 5. O que muda com os ADRs recentes — e por que decidir agora

- **ADR-0017 (o Room sai):** a classe plana perde as anotações e **assume** o papel de DTO de trânsito. Se a
  decisão for B ou C, é o momento natural — a fatia já vai tocar esses tipos.
- **ADR-0018 (o agregado deixa de ser plano):** participantes viram entidades referenciadas e o pagamento
  vira lista de lançamentos. **A `DadosPassagem` de 58 campos planos não sobrevive a isso sem reescrita** —
  os `nomePassageiro1/2/3` viram coleção. Ou seja: **essa classe vai ser mexida de qualquer jeito**; a
  pergunta é se ela é mexida uma vez (dividindo) ou duas (mantendo e depois dividindo).
- **A inferência tarifária** (ADR-0016 §7.2) traz um consumidor novo — relatório com agregação —, que não
  quer campo formatado em `String`: quer número.

## 6. Critério proposto (não decisão)

Se for para escrever uma regra em vez de decidir caso a caso, a que os dados sustentam é:

> **Um DTO por entidade, exceto quando o consumidor é uma lista.** Listas ganham projeção própria, com o
> mínimo que a linha desenha e **sem resolução de outras coleções**. Detalhe, impressão e documento
> compartilham a projeção completa.

O gatilho para especializar fica objetivo — e verificável: **consumidor que usa menos de um terço dos campos,
ou que dispara leitura para preencher campo que não exibe.** Hoje isso acusaria exatamente os dois casos do
§2.

## 7. Decisão do analista (2026-08-02) — DTO por caso de uso, e a camada de dados vira dinâmica

**Decidido, e vai além da pergunta que o estudo fez.**

- **DTO por caso de uso é a pedida.** `DadosPassagem` é o exemplo do que não fazer: será **refatorado e
  revitalizado** em projeções por consumidor.
- **As classes `[Entidade]Documento` saem dos repositórios.** Elas continuam valendo como **documentação da
  estrutura** — mas, no nível do código, *"podiam ser só `Map`s de chave e valor que funcionariam do mesmo
  jeito"*. A fronteira de dados passa a ser **dinâmica, com `Map`**.
- **O domínio define a relação; o caso de uso define o uso.** O Firestore é a camada de dados que **reflete**
  o domínio — não o que o dita.
- **A consequência de método é a maior:** ao definir uma tela, **do domínio nascem as fronteiras** (mappers,
  ViewModel) **e as camadas** (dados, apresentação). É *domain-driven* no sentido literal — a tela é a última
  a ser desenhada, não a primeira.

**Por que isso encaixa no que o código já mostra.** O estudo mediu que a projeção única cobra `N+1` de
leitura para preencher campo que a tela descarta (§2a) e que o SDK não permite projeção no servidor (§3):
com `Map` na fronteira, **o repositório para de instanciar 25 campos tipados para o mapper usar 11** — a
tipagem passa a acontecer **onde o uso é conhecido**, que é exatamente o DTO de caso de uso.

**O que se paga, e vale estar escrito:** perde-se a checagem do compilador na leitura do documento (`Map`
não tem campo errado — tem chave ausente). A mitigação é a mesma convenção que o domínio já usa em toda
fronteira: **conversão explícita com fail-closed** (`de(valor)` → `null` quando desconhecido), concentrada no
mapper de cada caso de uso, e teste de mapeamento como rede. As classes `*Documento` que ficam como
documentação passam a ter **um leitor humano, não um compilador** — se envelhecerem em silêncio, o custo
volta.

> Isto merece **ADR próprio** (a camada de dados muda de regime, e o ADR-0003 previa exatamente este passo 2
> — "DTO-cêntrico" — sem decidi-lo). Ele conversa com o ADR-0017 (que já tira o Room) e com o ADR-0016
> (entidade = lei · DTO = trânsito · documento = armazenamento).

## 8. Perguntas para o analista

*(A pergunta 1 — "um por entidade ou por consumidor" — foi respondida em §7: por consumidor.)*

2. **Formatação: onde?** Hoje o mapper devolve `String` formatada. Manter (a tela só desenha) ou o DTO passa
   a carregar **tipo** (`BigDecimal`, `LocalDate`) e a formatação desce para o Composable? A inferência
   tarifária vai pedir número.
3. **Os derivados duplicados** (`ehVeiculo`, `ehRede`, `tem2Pessoas`) somem do DTO e passam a vir da
   entidade, agora que ela é rica e sem framework (ADR-0016 §9)?
4. **Ordem:** isto entra **junto** da F5/F6 do ADR-0017 (quando a passagem sai do Room) ou é fatia própria,
   depois? Fazer junto evita mexer duas vezes; separar mantém a fatia pequena.