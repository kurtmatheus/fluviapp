# E2 — o Painel Principal, e o fim do Catálogo

**Status:** **fechado → [ADR-0020](../adr/0020-fim-do-catalogo-e-o-contexto-do-painel.md)** (`2026-08-03`,
2ª rodada). Análise da frente **E2** do [roadmap](mvp-roadmap.md), refeita a partir de uma decisão do
analista. Substitui [e3-catalogo.md](e3-catalogo.md), que mapeou a mesma superfície e parou um passo antes da
conclusão.

> **A decisão que motiva esta revisão:** *o `Catalogo` não nasce.* Máscara de CPF/CNPJ/RG/passaporte é regra —
> por LGPD, mais do que formatação —, forma de pagamento é do lançamento e atuação é da empresa. Os três são
> **tipo de domínio**, não linha de tabela; a camada de dados apenas reflete a estrutura. E a palavra
> *Constante* volta a significar o que diz: invariante do sistema (extensão de arquivo, MIME), não vocabulário
> de negócio. Com isso o domínio fecha, e a primeira seção depois do Painel Principal passa a ser **Empresa**.

---

## 1. O teste do próprio critério

O [ADR-0016 §3](../adr/0016-dominio-da-plataforma.md) escreveu a régua: **quem tem regra vira tipo de domínio;
quem é só rótulo vira linha de catálogo.** A régua está certa. O que esta análise faz é aplicá-la sem exceção
às cinco categorias que sobreviveram ao [mapa da E3](e3-catalogo.md) §1 — e nenhuma passa.

### 1.1 `DOCUMENTO` — o rótulo que já governa comportamento

O mapa da E3 classificou `DOCUMENTO` como *"rótulo puro, sem regra — e cresce por país/negócio"*. **O código
diz o contrário, e diz há muito tempo:**

```kotlin
// extensions/UtilExtensions.kt:10-20
fun visualTransformation(tipoDocumento: String) =
    when (tipoDocumento) {
        Constante.Descricao.CPF.name -> CpfVisualTransformation()
        Constante.Descricao.CNPJ.name -> CnpjVisualTransformation()
        Constante.Descricao.PASSAPORTE.name -> PassaporteVisualTransformation()
        else -> VisualTransformation.None
    }

fun keyboardType(tipoDocumento: String) =
    if (tipoDocumento == Constante.Descricao.PASSAPORTE.name) KeyboardType.Text
    else KeyboardType.Number
```

Dois `when` sobre valores de catálogo, decidindo **máscara** e **teclado**. Isso é o valor da linha governando
o comportamento da tela — exatamente o que a régua manda tipificar.

E, **como catálogo, já está quebrado**: um item novo cadastrado pelo `ADM` (`RNE`, `CNH estrangeira`,
`Cédula de identidade estrangeira`) cai no `else` — renderiza sem máscara, com teclado numérico, **em
silêncio**. O catálogo promete uma extensibilidade que o código não honra. Pelo mesmo *fail-closed* que o §3
usou para manter `Categoria` como tipo fechado, `DOCUMENTO` nunca foi rótulo.

**A LGPD eleva o argumento de conveniência a requisito.** A máscara não é enfeite: ela é o ponto onde o app
decide **como um identificador pessoal aparece na tela** — formatar (`123.456.789-00`) e, onde couber, ocultar
parcialmente (`***.456.789-**`) na consulta, no card e no bilhete. Política de exibição de dado pessoal é
regra de tratamento; não pode morar numa linha do Firestore que um administrador edita. `TipoDocumento` passa
a carregar, junto do rótulo: **máscara, teclado, validação e política de exibição**.

> **Achado colateral, hoje:** não há validação nenhuma. `CpfVisualTransformation` só formata
> (`formatarCampoCPF`), e `CNH`/`RG` não têm sequer transformação. O campo `documento` é texto livre. Tipificar
> é o que cria o lugar onde a validação de dígito passa a caber — hoje ela não tem onde morar.

### 1.2 `PAGAMENTO` — o lançamento é o fato, a forma é o tipo

O mapa da E3 deixou esta em aberto com um bom contra-argumento: *"meio de pagamento novo é fato de mercado,
não de código"*. Ele não sobrevive ao teste do PIX — que chegou trazendo QR, conciliação e liquidação
imediata, tudo código. Uma linha `VOUCHER` no catálogo dá uma entrada de dropdown **e nada mais**: não diz se
o valor é equivalente a caixa, se liquida no ato ou em D+30, nem como o
[balanço financeiro](../adr/0014-balanco-financeiro-da-travessia.md) deve tratá-lo.

O [ADR-0018 D11](../adr/0018-agregado-passagem-participantes-modo-e-lancamentos.md) já tinha percebido: com
lançamentos `{id, forma, valor}`, *"`forma` quer ser tipo"*. Vira `FormaPagamento`, dentro do lançamento, onde
o fato acontece.

### 1.3 `ATUACAO` — o ADR-0016 já a tratava como tipo em três lugares

Esta é a mais clara, porque a contradição está dentro de um documento só:

| Onde | O que o ADR-0016 diz | Natureza que isso exige |
|---|---|---|
| §4 (mapa de coleções) | `atuacoes/{ATUACAO}` — **"o id É o nome da atuação"** | conjunto fechado |
| §2 | as famílias de menu **derivam** da atuação | conjunto fechado |
| §6.1 | o cargo é **qualificado** pela atuação | conjunto fechado |
| §3 | atuação é **categoria do catálogo** | conjunto aberto |
| §8 | *"valor novo no catálogo não ganha painel sozinho"* | admite que é fechado |

Quatro linhas contra uma. Um valor que é **id de documento**, **eixo de permissão** e **seletor de menu** é
tipo. `Atuacao` — `AGENCIAMENTO`, `TRANSPORTE`, `PORTUARIA_OPERACAO`, `PORTUARIA_ARRENDAMENTO`.

### 1.4 `TIPO_EMBARCACAO` — a exceção que era o sintoma

O [§8](../adr/0016-dominio-da-plataforma.md) nomeou o desconforto e conviveu com ele: o tipo de embarcação é
*"catálogo **e** tem comportamento"*; a resolução foi *"o catálogo guarda a lista, a capacidade é código"*, com
a consequência assumida de que **"Catamarã" cadastrado nasce só-passageiro até o código dizer**.

Essa resolução é o argumento contra si mesma. Se o código precisa falar antes de o valor significar alguma
coisa, **a lista não é a fonte — o tipo é**. Cadastrar um valor que só produz uma oferta vazia não é
extensibilidade; é uma linha inerte com aparência de configuração. `TipoEmbarcacao` — `FERRY_BOAT`, `NAVIO`,
`LANCHA` —, com o conjunto de `ClasseVeiculo` admitida como propriedade do tipo, que é onde a regra pura do §8
já queria estar.

**Confirmado pelo analista** *(2ª rodada, 2026-08-03)*, com a razão dita em uma linha: **não se vende veículo
para uma lancha se a cadastrarmos.** É o mesmo fail-closed do §8 lido pelo lado certo — o tipo não é um rótulo
que o código *interpreta*, é o que decide o que a embarcação carrega. E o tipo é **da embarcação**: enum
aninhado, não categoria solta.

> **Consequência de vocabulário, decidida na mesma rodada:** ***"navio" é semântica muito limitada — deve ser
> `Embarcacao`.*** Lancha não é navio, balsa não é navio; a entidade que hoje se chama `Navio` é a
> generalização, e o nome está errado desde o começo — do mesmo jeito que `Viagem` estava (ADR-0016 §7). **O
> rename não é desta frente:** entra quando a estrutura de embarcações for mexida, e o `TipoEmbarcacao` nasce
> já com o nome certo, dentro dela.

### 1.5 `MUNICIPIO` / `UF` — o resíduo estrutural, e o maior ganho

Esta não foi mencionada na decisão e é a que mais mexe no desenho, porque o catálogo **está embutido em outra
entidade**. O ADR-0016 §5 criou a `Localidade` e a definiu como *"UF + município, ambos `Catalogo`
embutidos"*, com um parágrafo inteiro justificando embutir `id` + `descricao` **"para reresolver contra o
catálogo se a grafia mudar"**.

Sem catálogo, a `Localidade` fica mais simples do que estava:

| Campo | Era | Fica | Por quê |
|---|---|---|---|
| `uf` | `Catalogo` embutido | **enum `Uf`** | 27 unidades federativas, fechadas por constituição — o conjunto mais estável do domínio |
| `municipio` | `Catalogo` embutido | **campo da própria `Localidade`** | não é rótulo de outra tabela: é *o nome desta entidade*, e a autoridade sobre ele é o IBGE (`codigoIbge`), não o gestor |

Some junto a regra de unicidade `(categoria, descricao)` que o §3 criou só para impedir *"dois Belém em
`MUNICIPIO`"* — sem a tabela genérica, a duplicidade é impedida pelo `codigoIbge`, que é a chave natural.

## 2. A conclusão: a coleção nasce vazia

| Categoria | Destino |
|---|---|
| `STATUS_PASSAGEM`, `TIPO_PASSAGEM`, `GRATUIDADE` | já eram tipo (ADR-0012, ADR-0013) |
| `ACOMODACAO`, `CATEGORIA_PASSAGEM`, `VEICULO` | viram `ModoPassagem` / `ClasseVeiculo` (ADR-0018 D6/D7) |
| `DOCUMENTO` | **`TipoDocumento`** — máscara, teclado, validação, exibição (§1.1) |
| `PAGAMENTO` | **`FormaPagamento`**, no lançamento (§1.2) |
| `TIPO_EMBARCACAO` | **`TipoEmbarcacao`**, com as classes admitidas (§1.4) |
| `ATUACAO` | **`Atuacao`**, da empresa (§1.3) |
| `MUNICIPIO` / `UF` | **`Localidade`** + enum `Uf` (§1.5) |

**Não sobra linha.** O `Catalogo` não seria um `Constante` menor — seria uma coleção sem documentos, um CRUD
sem o que criar, uma seção de menu com tela vazia, uma regra de servidor guardando nada e uma
`IObjetoSimplificado` sem implementador. E como `domain/catalogo/Catalogo.kt` **nunca foi escrito** (passo 2 do
[mapa da E3](e3-catalogo.md) §5), matá-lo agora não demole nada: é o momento mais barato que existirá.

**A regra que fica, e que é o conteúdo do ADR:** *vocabulário que o código consome é **tipo**; dado que o
negócio cria é **entidade**. Não há terceira categoria.* A tabela `Constante` era a terceira categoria — e
cada tentativa de sustentá-la produziu uma exceção nomeada no próprio ADR-0016: a `Categoria` que continua
tipo fechado (§3), o tipo de embarcação que é "os dois" (§8), o catálogo embutido dentro da `Localidade` (§5).
Três exceções numa decisão só é o modelo avisando que a categoria não fecha.

**O nome se liberta.** `Constante` volta a significar invariante de sistema — hoje, concretamente, o par
`"passagem_….png"` / `"image/png"` em `PassagemDigitalHelper.kt:47,76`. `REDE`, `CPF` e `PIX` nunca foram
constantes; eram domínio guardado numa tabela.

**O contra-argumento honesto, e a resposta.** Documento *muda por país*: um dia entra `DNI`, `RUT`, `CI`. Esse
dia exige máscara, teclado e validação novos — ou seja, **deploy de qualquer forma**. O catálogo só adiantaria
o rótulo, que é a parte que não serve sozinha. Quando um valor realmente não tem comportamento, ele não é
vocabulário: é dado de uma entidade, e vai para a entidade.

### 2.1 O ganho que se mede

O mapa da E3 §4 contou **16 chamadas** de `obterTodosPorCategoria` em 11 ViewModels e helpers, alimentando
dropdowns. Todas viram `TipoDocumento.entries` e companhia:

- **os seletores deixam de depender de rede** — sem listener, sem suspensão, sem espelho Room;
- **"dropdown vazio" deixa de ser um estado possível** — hoje é o que aparece quando a coleção não sincronizou,
  sem erro e sem log (o §3 do ADR-0016 já temia isso para categoria errada; vale igual para coleção ausente);
- somem `ConstanteDao`, `ConstanteRepository`, `ConstanteFirestoreRepository`, `ConstanteDocumento`, a coleção
  `constants` e a regra em `firestore.rules:183`;
- **`IObjetoSimplificado` sai de uso, não de existência** *(precisão do analista, 2ª rodada)*: a forma
  "id + descrição" é um **value object de rótulo** legítimo, e pode voltar a ser relevante se a expansão da
  plataforma trouxer catálogos que sejam *de fato* rótulo. O que morre é **o uso dela neste app**, consumido
  pelas implementações e revitalizações — junto com as duas armadilhas que o mapa da E3 §2 encontrou
  (`first { }` que lança em vez de devolver `null`, e `extrairPorDescricao`, que é o casamento por nome que o
  [ADR-0008](../adr/0008-relacionamentos-por-identidade.md) mata). A diferença importa: não se está dizendo
  que a forma é errada, e sim que **hoje não há dado com essa natureza**.

## 3. A E2 refeita

Com o domínio fechado por tipos, a leitura da frente muda de natureza. Os quatro achados do
[roadmap](mvp-roadmap.md#e2--mainscreen-vira-painelprincipal-da-plataforma) continuam válidos; o que muda é
que **dois deles deixam de depender de tela alguma**.

| Achado da E2 | Como fica |
|---|---|
| `SecaoMenu` é enum fixo de cinco seções; as seções deviam **derivar da atuação** | com `Atuacao` como tipo, "derivar" é uma **função pura** `Atuacao → Set<SecaoMenu>` — testável em JVM, sem Firestore e **sem esperar cadastro nenhum** |
| a política de menu mora na navegação (`acoesDe(secao)`, `MainScreenNavComposable:66-93`, 13 callbacks) | inalterado como problema — mas agora tem **destino**: a mesma função pura acima é a casa dela |
| `DadosBotoesMenus` carrega `onClick` (DTO com comportamento) | inalterado — cai pelo ADR-0019 §7 |
| **o painel é o molde** | inalterado, e reforçado: o molde fica mais firme quando a estrutura do menu é tipo e não dado |

**O passo *domínio* da E2 passa a ser o fechamento do domínio inteiro** — e é um diff só de JVM: cinco tipos
(`TipoDocumento`, `FormaPagamento`, `TipoEmbarcacao`, `Atuacao`, `Uf`), a derivação do menu, e a remoção de
`Constante` + `IObjetoSimplificado`. Sem Firestore, sem Room, sem tela. É a fatia mais barata e mais
estruturante que o projeto tem em aberto, e é ela que sustenta as três frentes.

### 3.1 A primeira seção depois do Painel Principal é **Empresa**

A E3 troca de sujeito. O `Catalogo` tinha sido escolhido por ser *"a mais boba do inventário"* e *"o lugar
mais barato do app"* ([ADR-0017 §7.1](../adr/0017-eixo-de-storage-firestore-only.md)) — critério legítimo que
deixou de existir junto com a entidade. **Empresa** é a sucessora, e por razões melhores que o preço:

- **já existe** — `domain/viagem/Empresa.kt`, `EmpresaDao`, repositório, `FormEmpresaUiState`,
  `ContentEmpresaAreaForm` (com `CnpjVisualTransformation` já no lugar). O crivo *"reaproveitar o que existe"*
  tem o que reaproveitar; com o catálogo, tudo nasceria do zero;
- **é onde a atuação nasce** (`empresas/{id}/atuacoes/{ATUACAO}`, ADR-0016 §4). O menu da E2 deriva da atuação;
  sem empresa cadastrada, ele deriva de nada. A ordem **E2 → Empresa** é autoconsistente: a E2 define a
  estrutura, a E3 produz o dado que a torna visível;
- **tem domínio de verdade** — CNPJ, razão social, atuações, concessão de portos e navios. O trilho
  *domínio → dados → lógica → apresentação → regra → teste observável* passa a ser provado numa fatia que o
  **exerce**, em vez de atravessá-lo vazio.

**Trade-off nomeado:** a E3 fica mais cara. Empresa tem espelho Room a remover (o piloto do
[ADR-0017](../adr/0017-eixo-de-storage-firestore-only.md) deixa de ser *"coleção que nasce sem espelho"* e
passa a ser *"coleção que perde o espelho"*) e uma subcoleção a criar. Em compensação é **representativa**:
todas as coleções restantes têm espelho a remover, e nenhuma nasce do nada. Um piloto barato que não prova o
caso geral custa mais do que economiza.

### 3.2 A separação `ADM` × `GESTOR` — respondida: **não, por enquanto**

Hoje `PermissoesUsuario.kt:33` é `ehPapelPlataforma(papel) = papel == ADM || papel == GESTOR` — os dois andam
sempre juntos, e o **único** ponto onde iam se separar era o CRUD do catálogo (ADR-0016 §6: *"Catálogo — só
`ADM`"*; ADR-0017 §7.1). Sem catálogo, não sobra superfície `ADM`-only.

**Decisão do analista** *(2ª rodada)*: *"só teria `ADM`-only para o catálogo, então o princípio de 'mais perto
do código' pode valer no futuro, mas por enquanto não."* Ou seja:

- `ADM` e `GESTOR` **continuam idênticos em capacidade**, e `ehPapelPlataforma` continua sendo a pergunta
  certa — não há eixo novo a construir;
- `ADM` fica sendo o **papel de bootstrap** provisionado no console (ADR-0016 §10), distinto por origem e não
  por permissão;
- o critério do §6 (*"quanto mais perto o dado está da semântica do código, mais restrito é quem o escreve"*)
  **não é revogado — fica disponível**. Ele volta a ter uso quando existir dado dessa natureza; hoje não
  existe, e inventar uma restrição para ter onde aplicá-la seria construir a regra antes do caso.

Isso também **fecha o `firestore.rules` sem trabalho novo**: `papel() in ['ADM','GESTOR']` (`:34`) segue
descrevendo o painel inteiro.

## 3.3 Atuação: a plataforma **cadastra** a atuação; o conjunto de valores é que é tipo

Precisão que o analista fez na 2ª rodada, e que evita ler §1.3 como algo que ele não diz: **a atuação
continua sendo cadastrada** — é o painel da plataforma que declara *que esta empresa atua assim*, criando
`empresas/{id}/atuacoes/{ATUACAO}` com as concessões dela. O que vira tipo é o **conjunto de valores
possíveis**, não o fato.

| | O que é | Natureza | Quem cria |
|---|---|---|---|
| `Atuacao` | os valores que existem (`AGENCIAMENTO`, `TRANSPORTE`, `PORTUARIA_*`) | **tipo de domínio** — o código deriva menu, cargo e permissão deles | o deploy |
| `atuacoes/{ATUACAO}` | *esta empresa exerce esta atuação*, com `portoIds[]`/`navioIds[]` | **dado** | o painel (`ADM`/`GESTOR`) |

É a mesma distinção do §1: o **vocabulário** é código, o **fato** é dado. E é ela que faz o cadastro de
Empresa ser a E3: cadastrar a empresa e suas atuações é o que dá conteúdo ao menu que a E2 estrutura.

### 3.4 O painel muda conforme a atuação — e por isso a splash volta a carregar

**Decisão do analista** *(2ª rodada)*: *"muda-se o painel quando detectar que a atuação da empresa do
funcionário que logou; é preciso ter uma splashscreen de carregamento de informações para nenhum processo ou
informação ser omitida ou esquecida enquanto carrega o painel da empresa."*

Isto **não contradiz a E1.1** — completa-a. O comentário que ficou em `SplashScreenViewModel.kt:26-28` diz que
*"a splash existe para cobrir o tempo real de decidir, não para ser vista"*. O que a E1.1 removeu foi o
`delay(Random)`, espera **artificial**; o que entra agora é espera **real**, e é a primeira vez que existe uma.

O código mostra por que ela não existia e por que passa a existir:

| | Hoje | Com o painel por atuação |
|---|---|---|
| O que a splash resolve | `firebaseAuth.currentUser != null` (`SplashScreenViewModel.kt:32`) | o **contexto inteiro**: usuário → funcionário → vínculo → empresa → atuações |
| Onde o dado está | sessão persistida do Firebase, local | `empresas/{id}/atuacoes` — **vai à rede** |
| Custo | instantâneo | tempo real de I/O, com falha possível |

E a porta já existe: `SessaoUsuario.atual()` é `suspend` e resolve `usuário → funcionarioId → funcionário` em
um lugar só. **A splash hoje não a usa** — pergunta ao `FirebaseAuth` direto. A mudança é fazê-la usar, e
estender `ContextoUsuario` com o vínculo ativo (empresa + atuação). `SessaoUsuarioRoom` diz de si mesma que
*"as duas leituras são locais, não vão à rede"*: é essa frase que deixa de valer, e é ela que justifica a tela
de carregamento.

Três coisas que a decisão implica e precisam estar no desenho:

1. **`SplashScreenState` ganha `Erro`.** O sealed já tem `Carregando` — que hoje é um estado que nunca se
   observa. Passa a ser o estado normal, e falha de rede deixa de ser invisível: sem estado de erro, a splash
   ficaria presa em `Carregando` para sempre, que é exatamente a "informação omitida" que a decisão quer
   impedir.
2. **Multi-vínculo é o caso em que a splash não decide sozinha.** O ADR-0016 (8ª rodada) põe *a escolha do
   vínculo no login*: com **um** vínculo, a splash resolve e entra; com **mais de um**, ela carrega e
   **apresenta a escolha** — carregar é dela, escolher não.
3. **`ADM`/`GESTOR` não têm funcionário nem vínculo** — e isso é estado válido, já documentado em
   `ContextoUsuario.kt:9-10`. Para eles a splash resolve como hoje, direto para o painel da plataforma. **O
   caminho rápido continua existindo**; o carregamento é do painel *da empresa*.

## 4. Onde o catálogo foi documentado, e como cada lugar fica

Levantamento completo, para o ADR marcar como superado.

### ADRs

| Documento | Trecho | Como fica |
|---|---|---|
| [0016](../adr/0016-dominio-da-plataforma.md) **§3** | *"`Constante` vira `Catalogo`, e `IObjetoSimplificado` fica só nele"* — `ordem`, `ativo`, `(categoria, descricao)` único, `Categoria` como tipo fechado | **superado.** Não há `Catalogo`: `Constante` e a interface morrem inteiras, e os valores viram tipo. A régua do §3 permanece — é ela que produz este resultado |
| 0016 **§4** | mapa de coleções: `catalogo/{itemId}` | **superado** — a coleção sai do mapa. `atuacoes/{ATUACAO}` fica como está: o id sempre foi o valor fechado |
| 0016 **§5** | `Localidade` = `uf` + `municipio`, dois `Catalogo` embutidos; o parágrafo do *"reresolver contra o catálogo"* | **revisado** (§1.5): `uf` vira enum `Uf`, `municipio` é campo da própria entidade, `codigoIbge` é a chave natural |
| 0016 **§6** | tabela *"quem cadastra"*: **Catálogo — só `ADM`** | **superado** — a linha some, e com ela a única superfície `ADM`-only (§3.2). O critério da tabela sobrevive |
| 0016 **§8** | tipo de embarcação é *"catálogo **com** regra"* — a exceção nomeada ao §3 | **superado** (§1.4). A tabela `tipo → classes admitidas` vira propriedade do enum; o ponto de aplicação (a emissão) não muda |
| 0016 **F1** (plano de fases) | *"F1 — Catálogo"* | **substituída**: F1 passa a ser **os tipos do domínio** (puro, JVM), e a primeira tela é Empresa |
| 0016 (alternativas) | *"catálogo por categoria em coleções separadas — rejeitada"* | fica como registro histórico; a alternativa vencedora passa a ser **"não é coleção"** |
| [0017](../adr/0017-eixo-de-storage-firestore-only.md) **§"O piloto: `Catalogo`"** e **F1** | o piloto do Firestore-only nos três tempos | **substituído por Empresa** (§3.1) — deixa de ser "coleção sem espelho" e passa a ser "coleção que perde o espelho" |
| [0019](../adr/0019-camada-de-dados-dinamica-e-dto-por-caso-de-uso.md) **F1** | *"`Catalogo`, junto da E3 do roadmap"* — o primeiro DTO por caso de uso | **substituído por Empresa**; a decisão (fronteira `Map`, DTO por consumidor) não muda |
| [0011](../adr/0011-regras-firestore-por-cargo.md) §"Catálogos" | a coleção `constants` na regra | a coleção deixa de existir; a regra sai **na fase em que o código sai** (descarte progressivo) |
| [`docs/adr/README.md`](../adr/README.md) (índice de vigência) | linha `Constante → Catalogo (e IObjetoSimplificado fica só nele)` | vira `Constante → tipos de domínio; o Catalogo não chega a nascer` |

### Estudos

| Documento | Trecho | Como fica |
|---|---|---|
| [e3-catalogo.md](e3-catalogo.md) | o documento inteiro | **superado por este.** Mas o §1 dele é **a prova**: foi ele que mediu que "some quase tudo". Este estudo só levou a mesma conta até o fim |
| [dominio-da-plataforma.md](dominio-da-plataforma.md) | **§3.8** (*"Catálogo — as informações adjuntas"*), **§4.8.1** (*"`Catalogo.Categoria` — o índice"*), as linhas de `Catalogo` embutido em `Localidade` (§3.4 / linhas 299-300), o mapa de coleções e a tabela de deltas | **superado nesses pontos** — é o catálogo do domínio e precisa refletir os tipos novos |
| [eixo-de-storage-firestore-only.md](eixo-de-storage-firestore-only.md) **§7.1** | a escolha do piloto | **superado** — piloto passa a ser Empresa |
| [dominio-passagem.md](dominio-passagem.md):186 | *"`STATUS_PASSAGEM` some com a F1 do ADR-0016"* | **continua verdade, muda a razão**: some porque o catálogo inteiro some |
| [mvp-roadmap.md](mvp-roadmap.md) | **E3** (*"`Catalogo` primeiro, e como última opção do menu"*), **P3.A** (a cadeia `catálogo → porto/trecho e empresa → …`) e a caixa de sequência | **reescritos**: a cadeia começa em **Empresa**, e o passo *domínio* da E2 fecha os tipos |
| [README.md](README.md) | linha de `e3-catalogo.md` (*"em execução"*) | vira **superado**, apontando para este |

### Código e regras

| Peça | Onde | Como fica |
|---|---|---|
| Entidade + enums | `domain/cadastro/constantes/Constante.kt` (`Descricao` 31 valores, `Categoria` 9) | **remoção** |
| Interface | `domain/IObjetoSimplificado.kt` + `mapDescricao` / `extrairPorId` / `extrairPorDescricao` | **sai de uso** — some conforme as revitalizações passam; a *forma* fica registrada como válida (§2.1) |
| Porta e impl | `ConstanteRepository`, `ConstanteFirestoreRepository`, `ConstanteDao`, `ConstanteDocumento` | **remoção** |
| Consumo | 16 chamadas de `obterTodosPorCategoria` em 11 ViewModels/helpers | viram `.entries` (§2.1) |
| Máscara/teclado | `extensions/UtilExtensions.kt:10-20` — dois `when` sobre `Constante.Descricao` | vira comportamento de `TipoDocumento` (§1.1) |
| Regra de servidor | `firestore.rules:183` (`match /constants/{doc}`) + os casos em `firestore-tests/` | saem junto com o código |
| Seed | `SeedFirestore` popula `constants` | já morria pelo ADR-0016 §1 |

---

## 5. As três pendências do estudo — **fechadas na 2ª rodada** (`2026-08-03`)

| Pendência | Resposta do analista |
|---|---|
| A separação `ADM` × `GESTOR` | **não se cria** — o princípio "mais perto do código" fica disponível para o futuro (§3.2) |
| `TIPO_EMBARCACAO` como tipo | **confirmado**, e o tipo é da embarcação; junto veio `Navio` → `Embarcacao` como rename futuro (§1.4) |
| `IObjetoSimplificado` | **não morre como forma** — morre o uso neste app (§2.1) |

**Contador de bilhete — respondido junto, e a resposta já existia.** O [mapa da E3](e3-catalogo.md) §3 deixou
`ContadorBilhete` "anotado para a fase da numeração". Não há o que projetar: ele **já vira `count` por
ocorrência** — `count(passagens where viagemId = X and data = D)`, que é o mecanismo que o
[ADR-0016 §7.1](../adr/0016-dominio-da-plataforma.md) (9ª rodada) criou ao dar identidade única à partida
física, e que o [ADR-0018 D10](../adr/0018-agregado-passagem-participantes-modo-e-lancamentos.md) já usa. O
contador global único não é substituído por um contador melhor: **é substituído por uma contagem**. Sai com a
fase, sem desenho novo. Vizinho no mesmo saco: `ViagemDao.obterContagem()`, sem chamador.

**O que continua sem decisão:** nada nesta frente. O que falta é o ADR e a execução.
