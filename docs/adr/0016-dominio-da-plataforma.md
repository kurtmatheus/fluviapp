# ADR-0016: Domínio da plataforma — parte, atuação e ativo (multi-empresa e multi-segmento)

**Status:** **Aceita (direção)** — decisões do analista, 1ª a 6ª rodada (2026-07-28) e **7ª rodada
(2026-07-31)**, que **dissolveu o Trecho, criou a Localidade e fixou a lei do domínio sobre a persistência**.
Sem código: este ADR fixa o domínio e o mapa de coleções; a implementação é faseada abaixo. Supera a pergunta de provisionamento que estava
aberta no [roadmap do MVP](../design/mvp-roadmap.md) (Pilar 3) e promove a
[nota Viagem × Trecho](../design/viagem-vs-trecho.md) de "fora do MVP" para dentro dele, na versão do §7.

> Conversa com o [ADR-0003](0003-modelo-de-memoria-do-dado.md) (Room espelha Firestore),
> o [ADR-0008](0008-relacionamentos-por-identidade.md) (relacionar por id),
> o [ADR-0010](0010-autorizacao-por-cargo.md)/[ADR-0011](0011-regras-firestore-por-cargo.md) (política e regras),
> o [ADR-0013](0013-tabela-de-tarifa-e-tipo-tarifario.md) (tarifa tabelada),
> o [ADR-0015](0015-rework-agente-equipe.md) (os dois contextos: papel × cargo) e — desde a 7ª rodada —
> o [ADR-0017](0017-eixo-de-storage-firestore-only.md), que **supera o §9** e resolve os *Pontos abertos* 1 e 5.
> O catálogo vivo do domínio, com todos os campos e enums, é o
> [desenho de domínio](../design/dominio-da-plataforma.md).

---

## Contexto

O app nasceu como ferramenta de emissão de passagem para **uma** operação fluvial. O dado de negócio entra por
`SeedFirestore`, que só roda em debug (`SeedFirestore.kt:40` — `if (!BuildConfig.DEBUG) return`) e só se o
projeto estiver vazio. Isso tem três consequências que só apareceram quando o Pilar 3 (CI/CD) foi analisado:

1. **Um build release distribuído a testers não semeia nada.** Num projeto Firebase novo, o app abre vazio — e
   as regras do ADR-0011 negariam a escrita do seed de qualquer forma, porque ele roda antes da autenticação.
2. **Não existe caminho para cadastrar quem opera.** A P2.2c removeu `cadastrar` do `AutenticacaoRepository`
   (ADR-0015 §2.1): o provisionamento é fechado, e o SDK cliente não cria conta de terceiro. Hoje isso é passo
   manual no console do Firebase.
3. **O dado de negócio não tem dono.** Empresa, navio, agência e catálogo existem porque o seed os escreveu.
   Não há superfície de gestão — logo, nada que sustente uma plataforma com mais de uma empresa.

Ao mesmo tempo, o domínio vinha pedindo quatro correções acumuladas:

- `Constante` é uma tabela genérica de "tudo que é lista" com tipificações mortas dentro
  (`Constante.Descricao` ainda tem `CORTESIA`, `A_EMITIR` e `EMITIDA`, que os tipos do ADR-0012/0013 substituíram);
- `IObjetoSimplificado` virou interface-guarda-chuva de coisas que não são da mesma natureza (`Constante`,
  `Funcionario` e `Navio` a implementam);
- a entidade `Viagem` não é uma viagem — é um **trecho**, como a nota de arquitetura já registrou;
- e **o app nunca soube o que é uma agência**: `Funcionario.agencia` é String livre e `Agencia` é um enum de
  conjunto fixo cujo próprio comentário admite que "vira coleção cadastrável quando houver cadastro de agência"
  (`Agencia.kt:12-15`). O buraco não era de cadastro — era de modelo (§4).

A decisão a tomar é uma só, e é de escopo: **a plataforma deixa de ser um app de uma operação e passa a ser
multi-empresa e multi-segmento** — começando pelo agenciamento de passagens fluviais, montando apenas as bases
mínimas antes de expandir. **Multi-modal não entra** (§4).

## Opções consideradas

1. **Manter o seed e adiar o painel.** Distribuir builds debug para os testers, com o seed como fonte do dado.
   Barato, mas fixa o app como demonstração: nunca há dado que alguém tenha cadastrado de verdade, e o Pilar 3
   entrega uma esteira que distribui um app que não se alimenta.
2. **Painel administrativo + domínio de plataforma (escolhida).** Matar o seed, criar a superfície de gestão e
   reorganizar o Firestore em torno de **parte, atuação e ativo** (§4).
3. **Backend próprio / Cloud Functions para administração.** Resolveria o provisionamento de conta (Admin SDK
   cria usuário) e as agregações cross-empresa. Custo: um segundo runtime, deploy e linguagem no projeto —
   grande demais para o MVP, e o ADR-0001 já optou por não ter backend próprio.

## Decisão

### 1. O seed morre; o painel administrativo é a porta de entrada do dado

`SeedFirestore` e `sampledata` saem **de vez** — não ficam dormentes. O dado de negócio passa a entrar por uma
superfície de gestão dentro do app: o **painel administrativo**.

O princípio é o que o ADR-0003 já escolheu para o resto do sistema, aplicado agora ao cadastro: o Firestore é a
verdade, e a verdade tem que ter **autor**. Um dado semeado não tem autor; um dado cadastrado tem. É isso que
transforma o Pilar 3 de "distribuir um app de demonstração" em "distribuir um app que se alimenta".

Trade-off nomeado: **perde-se o app-que-abre-cheio**. Toda instalação nova exige alguém cadastrar catálogo,
localidade, porto, empresa, navio, agência, funcionário e rota antes de emitir a primeira passagem. É custo real de
demonstração, e é o preço de ter dado com dono. O §10 trata do único caso onde isso vira impasse.

### 2. Dois planos de acesso: quem cria o universo × quem cria a oferta

A separação que o ADR-0015 §8 desenhou em dois contextos (sistema × negócio) agora **também separa duas telas**:

| Quem | O que enxerga | O que faz |
|---|---|---|
| `ADM` / `GESTOR` (papel de plataforma, **sem** funcionário) | Só o **painel administrativo** | Cadastra empresa e suas atuações, navio, catálogo, funcionário e as capacidades da plataforma (**localidade** e **porto**). **Não emite passagem.** |
| `SUPERVISOR` (cargo, com funcionário) | A **operação** | Monta as **rotas** da empresa em que atua — com as tarifas dela — sobre os portos concedidos. Emite passagem. |
| `AGENTE` (cargo, com funcionário) | A **operação** | Emite passagem sobre as rotas que já existem. |

O primeiro plano não é regra nova: o `Usuario.kt:30-32` **já documenta** que `ADM`/`GESTOR` "existem sem registro
na operação — e, por isso mesmo, não emitem passagem (§8.4)". O que este ADR faz é **fechar a contradição entre o
comentário e o código**, porque hoje a política diz o contrário:

- `PermissoesUsuario.podeCriarPassagem(papel)` devolve `true` para qualquer papel conhecido — inclusive `ADM`.
- `podeAcessar(SecaoMenu.PASSAGEM, …)` devolve `true` incondicionalmente.

A correção é uma inversão do critério, e ela é elegante porque usa um elo que já existe: **emitir passagem passa
a exigir vínculo de funcionário** (`Usuario.funcionarioId` não vazio), não papel. O painel, simetricamente,
exige papel de plataforma. A política continua **única** (ADR-0010) e ganha perguntas novas em vez de uma
segunda política.

**A linha entre os dois planos é o que cada um cria.** A plataforma cria o **universo**: onde as coisas ficam
(localidade), quais lugares existem (porto), quais empresas existem e em que atuam. A empresa que agencia cria a
**oferta**: qual rota sai de qual porto, em qual embarcação, a que preço, em que dias — e **é ela que decide
quais cidades se ligam**, escolhendo o par de portos (§7). Nenhum dos dois invade o
outro, e o ponto de contato é a **concessão** (§7).

Isso mantém a política de seção **quase toda de sistema**. A `EQUIPE` continua sendo a única exceção que olha os
dois eixos (ADR-0015 §2.2): o supervisor gere os membros de onde atua.

Consequência de UI: `SecaoMenu` deixa de ser uma lista de seções de um menu só e passa a ter duas famílias.
`PASSAGEM` e `ROTA` ficam na operação; `EMPRESA`, `NAVIO`, `PORTO`, `TRECHO` e `CATALOGO` ficam no painel;
`EQUIPE` aparece nos dois. `VIAGEM` sai do menu — o nome estava errado desde o começo (§7).

### 3. `Constante` vira `Catalogo`, e `IObjetoSimplificado` fica só nele

`Constante` passa a se chamar **`Catalogo`** e tem, além do id: **`categoria`**, **`descricao`** e — desde a 7ª
rodada — **`ordem`** (Int) e **`ativo`** (Boolean). `ordem` existe porque hoje o item nasce por `.add()` com id
gerado e a lista sai na ordem em que o Firestore devolver: "Rede, Suíte, Camarote" apareceria embaralhado.
`ativo` é o par de `desativar em vez de remover` (§5). O par **`(categoria, descricao)` é único** — dois "Belém"
em `MUNICIPIO` fragmentariam a dimensão geográfica antes mesmo de ela ser montada (§5). Ele é a tabela das **informações adjuntas** — o que o negócio precisa nomear mas não precisa
modelar: UF, município, tipo de passagem, tipo de documento, tipo de veículo, acomodação, forma de pagamento,
**tipo de embarcação** (§8) e **atuação** (§4).

Duas remoções acompanham o rename:

- **A tipificação de `Descricao` sai; a de `Categoria` FICA** *(revisto na 7ª rodada)*. `Constante.Descricao` é
  um enum que duplica, com atraso, o que os tipos de domínio já dizem — ainda lista `CORTESIA` (aposentada pelo
  ADR-0013), `A_EMITIR` e `EMITIDA` (hoje são `StatusPassagem`, com FSM — ADR-0012). Manter dois vocabulários para
  a mesma coisa é a receita de divergirem: quem tem regra vira **tipo de domínio**, quem é só rótulo vira **linha
  de catálogo**.

  **`Categoria` é outra coisa, e a 6ª rodada as tratou como iguais por engano.** Ela não é rótulo de usuário — é
  o **índice do catálogo**, e o código depende dela: `ViagemDadosViagemMapper.kt:31` consulta `MUNICIPIO.name`,
  `ContagemPassagensMapper.kt:77` compara `GRATUIDADE.name`, e os helpers do form de passagem pedem `ACOMODACAO`,
  `TIPO_PASSAGEM`, `GRATUIDADE`, `DOCUMENTO` e `PAGAMENTO`. Como String livre, cada chamador carregaria um
  literal — e um erro de digitação **devolveria lista vazia em silêncio**: seletor sem itens, sem erro, sem log.
  Então **a categoria continua tipo fechado**. Isso não impede acrescentar **item** sem deploy, que é o ganho que
  interessa; impede acrescentar **categoria** sem deploy — e isso é correto pelo mesmo fail-closed do §8:
  categoria nova sem código que a consuma não serve para nada.
- **`IObjetoSimplificado` fica exclusivo do `Catalogo`.** Hoje `Constante`, `Funcionario` e `Navio` implementam
  a mesma interface `id` + `descricaoNome`, o que os iguala por acidente de forma, não por natureza: um item de
  catálogo *é* um par id/descrição — é tudo que ele é; um navio e uma pessoa têm identidade, atributos e ciclo
  de vida próprios. `Funcionario` e `Navio` deixam de implementá-la e passam a ser **entidades**, sem interface
  compartilhada. As helpers `extrairPorId`/`extrairPorDescricao`/`mapDescricao` seguem servindo o catálogo, que
  é onde "escolher de uma lista" é a operação natural.

Precisão importante sobre o limite do Room (§9): **remover a interface não toca o schema** — é tirar `override`
e a herança. O que tocaria é *renomear a coluna* `descricaoNome` → `nome`, e essa parte fica fora deste round.

O §8 abre uma exceção a este parágrafo, e ela está nomeada lá: o tipo de embarcação é catálogo **com regra**.

### 4. Parte, atuação e ativo — o eixo que organiza o domínio

Tecnicamente, `agenciaId` e `empresaId` são **o mesmo id**, e isso não é coincidência de implementação:
**agência é uma atuação que uma empresa exerce**, não um objeto contido nela. É o que a separa de navio — e essa
distinção é a fundação deste ADR, porque é ela que decide onde cada coisa mora.

- **Parte** — a `empresa`. Tem identidade, CNPJ e existe por si. É superentidade.
- **Atuação** — o que a parte faz num segmento: `AGENCIAMENTO`, `TRANSPORTE`, `PORTUARIA_*`. **Não é subtipo nem
  objeto contido:** uma parte exerce várias atuações ao mesmo tempo e muda de conjunto ao longo do tempo — e é
  nessas duas coisas que "tipo de empresa" e herança fracassam.
- **Ativo** — o que a parte possui com identidade própria: o `navio`.

Duas categorias já decididas nas rodadas anteriores ganham lugar no mesmo eixo: **capacidade da plataforma**
(localidade, porto, catálogo — sem dono) e **concessão** (o recorte que uma parte pode operar).

**`fluvial` é modal, não segmento — e por isso não pode nomear a estrutura.** O segmento de negócio é
*transporte*; fluvial é o *modo* de transportar. Batizar a subcoleção de `fluvial` comprometeria a estrutura com
o eixo modal, que este ADR **não** abre: multi-empresa e multi-segmento sim, **multi-modal não**. A atuação é
`TRANSPORTE`, e a frota é de **navios**, concretamente, até existir um segundo modal que justifique generalizar.

**A agência não tem dimensão física.** Agenciamento pode ser remoto, então não há filial nem endereço operacional
a modelar — o que é físico no domínio é o **porto** (§5). E se um dia a rede matriz/filial precisar existir, num
modelo de documentos ela é **um campo** no doc da empresa apontando para outra empresa. Não é estrutura a
preparar agora; é uma linha a acrescentar quando o caso aparecer.

#### O mapa de coleções

```
# PARTES — quem existe
empresas/{empresaId}                     nome, razaoSocial, cnpj, endereco, telefone1, telefone2
   ├── atuacoes/{ATUACAO}                um doc por atuação; o id É o nome da atuação
   │        AGENCIAMENTO           → portoIds[], armadorIds[]       ← concessões (§7)
   │        TRANSPORTE             → (é o armador; a frota dele é global)
   │        PORTUARIA_OPERACAO     → portoIds[]                     ← dormente (§5)
   │        PORTUARIA_ARRENDAMENTO → portoIds[]                     ← dormente (§5)
   └── rotas/{rotaId}                    a oferta — só existe com atuação AGENCIAMENTO (§7)

# CAPACIDADES DA PLATAFORMA — sem dono
localidades/{localidadeId}               uf (Catalogo), municipio (Catalogo)   ← embutidos (§5)
portos/{portoId}                         nome, localidade                      ← referência (§5)
catalogo/{itemId}                        categoria, descricao

# ATIVOS — dono por campo, endereçáveis globalmente
navios/{navioId}                         nome, capacidades, tipoEmbarcacao, empresaId   ← como já é hoje

# PESSOAS E EMISSÃO
users/{uid}                              papel, username, funcionarioId
funcionarios/{funcionarioId}             nome, email, cargo, empresaIds[]               (§6)
passagens/{passagemId}                   intocada (FSM, contador, consulta)
```

O critério de colocação passa a ser explícito, e é ele que faz o resto se decidir sozinho:

| Natureza | Onde | Por quê |
|---|---|---|
| Sem dono (lugar, linha, rótulo) | raiz | é de todos |
| Com dono, referenciado **entre** partes | raiz + campo de dono | precisa de id resolvível por quem não é o dono |
| Com dono, referenciado só **dentro** da parte | subcoleção da parte | isolamento por caminho, regra mais barata |
| A parte, as pessoas, a emissão | raiz | atravessam |

**O navio fica onde já está.** `navios/{id}` com `empresaId` é exatamente o que o código faz hoje
(`NavioFirestoreRepository.COLLECTION_NAVIOS`, e `Navio.empresaId` desde o ADR-0008 Fase 3), e está certo pela
segunda linha do critério: a agência vende passagem em navio que **não é dela**, então o navio é referenciado
entre partes e precisa de endereço global. Movê-lo para dentro da empresa obrigaria a rota a guardar o par
`(armadorId, navioId)` para resolver a referência — o mesmo problema que fez o porto subir para a raiz (§5).

**Atuação é documento, não nome de coleção** — e essa escolha paga uma dívida que as rodadas anteriores tinham
criado. Quando o segmento era o *nome* da subcoleção, o app não tinha como descobrir em que segmentos uma empresa
atua: `listCollections()` existe só nos Admin SDKs, e no Android não há equivalente. A saída era declarar um campo
`segmentos: []` no doc da empresa — denormalização a manter em sincronia. Como **documento**, a atuação volta a
ser dado consultável: `empresas/{id}/atuacoes` é uma query comum. O campo desaparece, e com ele a sincronia.

### 5. Localidade e Porto são capacidades da plataforma; a atuação portuária nasce dormente

*Revisado na 7ª rodada: nasce a `Localidade`, e `Porto.cidade` (String) vira `Porto.localidade`.*

```
localidades/{localidadeId}
    id
    uf          → Catalogo { id, descricao: "PA" }       ← embutido
    municipio   → Catalogo { id, descricao: "Belém" }    ← embutido
    codigoIbge                                           ← chave natural do município
    ativo

portos/{portoId}
    id, nome, localidade                     ← referência à Localidade
    ativo
```

**O que embute do `Catalogo`: `id` + `descricao`.** O id permite reresolver contra o catálogo se a grafia for
corrigida; a descrição permite exibir sem leitura extra. `categoria` não embute — é redundante, porque o **nome
do campo já diz** qual é (`uf` é `UF`, `municipio` é `MUNICIPIO`).

O porto mora na raiz, é da plataforma e não pertence a ninguém — como o `catalogo` e a `localidades`. Um porto é
um **lugar físico**, e lugar físico não é propriedade de quem navega: o cais de Manaus é o mesmo cais para todas
as empresas que atracam nele. Modelá-lo dentro da empresa produziria um documento por empresa para o mesmo lugar.

**A `Localidade` é o par UF + município como uma coisa só**, e existe por três razões que se somam:

1. **É o critério de composição aplicado** (7ª rodada): item de catálogo é *value object de referência* —
   pequeno, estável, sempre lido junto, sem vida própria —, então **embute**. É o único tipo do domínio em que
   embutir é a resposta certa.
2. **É a dimensão do eixo analítico.** Com a cidade como String no porto, a UF não existia em lugar nenhum e
   agrupar seria por igualdade de rótulo, sem hierarquia. Com a `Localidade`, "passagens por UF" e "por
   município" viram perguntas respondíveis — e é isso que prepara o terreno para o eixo analítico do ADR-0017.
3. **Enriquece a exibição sem consulta extra:** "Porto de Val-de-Cães — Belém/PA" sai de uma leitura só.

**Invariantes:** o par `(uf, municipio)` é **único**, e o `codigoIbge` também é único quando presente. Não é
preciosismo — é dimensão de análise: duas localidades para o mesmo município fragmentam todo relatório que agrupe
por elas, e o erro só aparece depois, no número errado. Valem no cadastro **e** na regra do servidor (F8). O mesmo
vale para o **nome do porto dentro da localidade**: dois "Porto Central" em Belém são o mesmo problema um nível
abaixo.

**`codigoIbge` é a chave natural da dimensão.** Ele resolve a unicidade de graça e — o que importa mais — é o que
permite cruzar esta dimensão com **dado externo** (censo, malha, tarifa regulada) quando o eixo analítico existir.
Não é o id do documento, é campo: o id do documento continua opaco, para não amarrar a identidade a um cadastro
de terceiro. Fica **opcional no cadastro** (exigi-lo poria fricção no painel para quem não tem o número à mão) e
**único quando presente** — o preço é que uma localidade sem código não cruza com fonte externa até alguém
preenchê-lo.

**Desativar, não remover** — e isto resolve um ponto aberto em vez de adiá-lo. Remover um porto invalida as rotas
e as concessões que o referenciam, e verificar "nenhuma rota usa" exigiria collection group. Com `ativo`, o porto
desativado **some dos seletores e continua resolvendo** as rotas e os bilhetes que já apontam para ele — que é o
comportamento correto para dado referenciado por fato histórico. Vale igual para `Localidade` e `Catalogo`.

**Quem cadastra — e as três não são iguais nisto:**

| Capacidade | Quem cadastra | Por quê |
|---|---|---|
| **Catálogo** | **só `ADM`** | é o **vocabulário que o código consome**: as categorias são tipo fechado (§3), e um item novo muda o que os seletores oferecem e o que a regra do tipo de embarcação admite (§8). Erro aqui é sistêmico |
| **Localidade** | papel de plataforma (`ADM` + `GESTOR`) | dado de referência operacional |
| **Porto** | papel de plataforma (`ADM` + `GESTOR`) | dado de referência operacional |

O critério que isto estabelece, e que vale para o painel inteiro: **quanto mais perto o dado está da semântica do
código, mais restrito é quem o escreve.** O catálogo é o mais perto de todos — daí ser o único `ADM`-only, como o
ADR-0017 §7.1 já havia fixado. Localidade e porto são cadastro de gestão corrente, e prendê-los ao `ADM` criaria
gargalo sem ganhar segurança.

`Porto.localidade` é **referência**, não cópia: `Localidade` tem coleção, identidade e ciclo de vida próprios, e
embutir uma cópia viva dela em cada porto criaria N cópias do mesmo município para manter.

> **O que isto substitui.** Até a 6ª rodada, `cidade` era String gravada a partir do Catálogo, justificada assim:
> *"o que o catálogo dá é um rótulo, e rótulo é dado por valor"*. A justificativa era de **persistência**, e
> persistência não decide forma de domínio (7ª rodada). O rótulo continua sendo dado por valor — só que agora ele
> mora **dentro da `Localidade`**, que é quem tem identidade.

O ganho de o porto ser capacidade da plataforma aparece no §7: a rota o referencia por **id simples**, válido
para qualquer empresa. Se o porto vivesse dentro de uma delas, a referência teria de carregar o `empresaId` junto,
e acordar a atuação portuária — quando o dono do documento mudasse — reescreveria toda referência existente.

**Escalabilidade da `localidades`.** É dado de referência: cresce até o tamanho do recorte geográfico atendido e
para. Enquanto for recorte municipal ou estadual, é coleção pequena; numa cobertura nacional (milhares de
municípios) ela **quebra a premissa do ADR-0017 D1** ("são coleções pequenas") — e a saída, então, não é voltar ao
espelho, é ela deixar de ser observada por inteiro e passar a ser consultada sob demanda. É a exceção prevista, e
o gatilho é o tamanho.

**O que a empresa tem no porto não é o porto — é a atuação nele.** Duas atuações, ambas dormentes:

```
empresas/{empresaId}/atuacoes/PORTUARIA_OPERACAO       → portoIds[]      ← quem opera o cais
empresas/{empresaId}/atuacoes/PORTUARIA_ARRENDAMENTO   → portoIds[]      ← a arrendatária
```

Uma **arrendatária** é a empresa que trabalha *dentro* do porto — quem arrenda o espaço e faz o **check-in**. É o
que fecha a distinção: o cais é o lugar (da plataforma), e a operação ou o arrendamento são o negócio que alguém
exerce naquele lugar (da parte). Fossem a mesma coisa, "porto" seria simultaneamente infraestrutura pública e
operação privada.

**As duas nascem dormentes** — valores de atuação reservados, sem UI, sem dado e sem nada que dependa deles. É o
padrão "dormente" do ADR-0003, aqui aplicado a um segmento inteiro em vez de a uma stack de leitura. E o destino
já tem nome: **é onde o check-in vai morar**, o módulo que o `briefing-projeto-checkin-navio.md` descreve e que o
ADR-0012 deixou pendente (a impressão física está dormente esperando por ele). Note que o custo de reservá-las é
quase nulo justamente porque são *valores*, não estrutura: não há coleção vazia a criar.

### 6. Funcionário serve uma ou mais empresas

```
funcionarios/{funcionarioId}
    nome, email, cargo
    empresaIds: [ … ]
```

Um funcionário serve mais de uma empresa, e o vínculo é a **assinatura do id da empresa** no próprio documento.

Nas rodadas anteriores isso era um par `{empresaId, agenciaId}`, porque a agência parecia ser outra coisa. Com o
§4, **o par colapsa**: os dois ids eram o mesmo. Saber em que empresa a pessoa atua já responde onde ela atua.

`cargo` permanece **um só, da pessoa** — não por empresa. É a escolha mínima, e mantém a política do ADR-0010 com
uma entrada de cargo em vez de N.

Consequências diretas:

- **`Funcionario.agencia: String` e `lotacao` saem.** `Agencia` e `Funcionario.Lotacao` — enums de conjunto fixo,
  um deles admitindo no comentário que viraria coleção cadastrável — morrem: a agência agora é uma empresa e a
  relação é por id.
- **`EscopoAgencia` volta a ter uma dimensão.** O sealed interface de hoje (`Todas` / `Apenas(agencia)` /
  `Nenhuma`) sobrevive quase intacto; o que muda é que o recorte passa a ser por **empresa**, não por String de
  agência. O caso "funcionário com dois vínculos" ainda precisa de uma **seleção de contexto** no login ou na
  emissão.
- **A emissão precisa saber sob qual vínculo emite.** Hoje a agência do bilhete vem do emissor (ADR-0015 §P2.3),
  o que é resposta única porque o funcionário tem uma agência. Com dois vínculos, deixa de ser.

### 7. A Rota é a oferta da agência — e o Trecho não precisa existir

*Revisado na 7ª rodada: o `Trecho` foi **dissolvido**.*

Aqui a nota [Viagem × Trecho](../design/viagem-vs-trecho.md) se resolve. O erro de vocabulário não era só "viagem
devia ser trecho": era que **duas coisas diferentes estavam espremidas numa entidade só** — a ligação entre dois
lugares e a oferta que uma empresa faz sobre ela. Até a 6ª rodada a separação produziu **duas** entidades
(Trecho + Rota); a 7ª mostrou que a primeira era redundante.

**O `Trecho` deixa de existir.** Não há coleção `trechos/`, `trechoId` na rota, `trechoIds[]` na concessão nem
módulo de cadastro de trecho. O que ele guardava — o par de cidades — passa a estar em **Porto** (que tem a
localidade) e em **Rota** (que tem os dois portos).

**Por que era dispensável:** a rota já referencia dois portos, e cada porto já sabe sua localidade — logo o par
`(origem, destino)` é **derivável**. Guardá-lo à parte era manter, num documento próprio, informação que os
outros dois já determinam, com o risco clássico da redundância: um trecho dizendo Manaus → Parintins e um par de
portos dizendo outra coisa. Uma linha deixa de ser **cadastro** e passa a ser **leitura sobre os portos**.

**O argumento do bem comum sobrevive, um nível abaixo.** A 2ª e a 4ª rodada defenderam o trecho como capacidade
compartilhada: duas empresas que vendem a mesma linha não deveriam refazer o cadastro. Isso continua verdade — só
que o que é comum e da plataforma agora é o **porto** (§5), e as duas empresas compartilham *os dois portos* em
vez da linha que eles formam. Nada é duplicado que devesse ser comum, e um cadastro a menos existe no painel.

**O que se paga, e fica escrito:** sem `trechoId`, "quais rotas fazem Manaus → Parintins" deixa de ser igualdade
num campo e vira consulta sobre **o par de portos**; se a pergunta for por município em vez de por porto, resolve-se
no cliente ou por denormalização, porque o Firestore não faz junção. É custo de **leitura**, não de modelo, e só
aparece quando existir relatório por linha.

**A concessão: a empresa recebe o recorte que pode operar.** A atuação de agenciamento guarda os portos e os
**armadores** concedidos:

```
empresas/{empresaId}/atuacoes/AGENCIAMENTO
    portoIds:   [ … ]        ← quais lugares
    armadorIds: [ … ]        ← para quais transportadores ela agencia
```

`armadorIds` aponta para **empresas com atuação `TRANSPORTE`** — e é aqui que a atuação de transporte deixa de ser
um rótulo vazio: ela é o que qualifica uma parte a ser agenciada. A relação agência↔armador, que num modelo
relacional seria uma tabela de associação entre partes, aqui é a **concessão** — o mesmo mecanismo que governa o
porto, aplicado a uma parte em vez de a uma capacidade. Uma peça, dois usos.

Com o trecho dissolvido, a concessão fica com **duas dimensões: onde** (portos) e **para quem** (armadores). A
linha que a empresa pode vender deixa de ser concedida diretamente e passa a ser **consequência dos portos que ela
recebeu** — quem tem os portos de Manaus e de Parintins pode montar a rota entre eles; quem não tem, não pode. O
recorte não perdeu força: perdeu uma dimensão redundante.

Isso resolve o impasse que as rodadas anteriores deixaram: se só a plataforma cadastra porto, o supervisor ficaria
bloqueado esperando alguém criar o que ele precisa. Não fica — **a empresa já chega provisionada**. Ele não
escolhe de um universo aberto; monta rotas dentro do recorte que recebeu, e esse recorte é **concessão
explícita**, não consequência de quem cadastrou primeiro.

É o mesmo mecanismo de "capacidades" do ADR-0015 (§2, agência e lotação como capacidades do usuário), um nível
acima — capacidades **da parte**, concedidas pela plataforma. O vocabulário coincidir não é acidente.

**Conceder não é cadastrar, e os processos não se misturam.** O form da empresa/atuação **só seleciona** — se o
porto não existe, cadastra-se no módulo dele, e depois volta-se aqui. E o supervisor **também não sai
deste processo**: quem cria funcionário é o módulo de funcionário. Cada cadastro faz **uma coisa**, no molde do
ADR-0006. Um form que criasse trecho, porto e funcionário de passagem seria três responsabilidades numa tela, com
escrita composta que pode falhar no meio e deixar empresa sem capacidade ou sem supervisor. Separados, não há
transação a orquestrar. E o vínculo do supervisor **já tem casa**: é o `empresaIds` do §6 — a pessoa sabe onde
atua, a empresa não precisa saber quem a supervisiona.

**Rota — `empresas/{empresaId}/rotas/{rotaId}`, da empresa que agencia. É a viagem.**

```
rotas/{rotaId}
    id, navioId                              ← qual embarcação opera (governa as tarifas — §8)
    embarquePortoId, desembarquePortoId      ← portos, por id simples (§5)
    tarifas                                  ← a tabela do ADR-0013, da empresa
    agenda: [ { diaSemana, hora }, … ]       ← dias em que opera e a hora de cada dia
```

A rota é **como uma empresa realiza aquela ligação**: de qual porto sai, em qual porto atraca, com qual
embarcação, a que preço e em que dias. É isso que a nota chamava de "Viagem" — a ligação com *quando* —, e é por
isso que ela mora na parte: **a tarifa é dela**. Note a profundidade: sem um nível de "agência" intermediário, a
rota fica a **quatro** níveis, e continua resolvível a partir da passagem, que já carrega `empresaId` e
`viagemId`.

**A linha é derivada, não guardada:** `origem = embarquePorto.localidade`, `destino = desembarquePorto.localidade`
— e vem com a UF junto (§5), o que a torna agrupável. É essa derivação que resolve o que a nota previa no §4: duas
empresas que vendem a mesma linha **compartilham os portos** e têm **rotas próprias**, com preços próprios. Nada é
duplicado que devesse ser comum, e nada é comum que devesse ser de alguém.

**As viagens concretas não são persistidas.** As ocorrências da semana são **calculadas** a partir da agenda da
rota e da semana corrente. Não existe coleção de ocorrências no MVP. Isso adia de propósito o item 3 da nota
(viagens geradas com contador por acomodação), e o custo é nomeado: **a ocupação continua sendo contada a partir
dos bilhetes** — o ganho de leitura O(1) fica para depois, e a contagem do Pilar 1 segue como está. É o certo
para um MVP: agenda é modelo, contador é otimização.

**A coerência da rota é regra pura** — e a 7ª rodada **eliminou a mais complicada das duas**:

> A checagem **geográfica** (o porto de embarque tem que estar na cidade de origem do trecho) **desapareceu por
> construção**. Ela existia para impedir que o par de portos contradissesse o par de cidades declarado no trecho,
> e sem trecho **não há dois lugares para discordar** — a origem *é* a localidade do porto de embarque. Um
> invariante que some porque a redundância que o exigia sumiu é o melhor tipo de simplificação.

Restam duas, ambas puras e testáveis sem device, no molde do ADR-0006:

1. **De concessão:** os dois portos têm que estar em `portoIds` da atuação, e o **dono do navio** tem que estar em
   `armadorIds`. Sem isso, o recorte concedido seria decorativo: bastaria digitar um id de fora. Esta tem que
   valer **também no servidor** (F8), porque é ela que impede uma empresa de operar o que não lhe foi concedido.
2. **De sentido:** embarque e desembarque não podem ser o mesmo porto. É trivial, e é **nova** — enquanto havia
   trecho, o par de cidades distintas garantia isso de graça.

A checagem do armador tem uma característica que a outra não tem, e vale registrar: ela é **indireta**. A rota
guarda `navioId`, não o armador; descobrir o dono exige ler o navio (`navios/{id}.empresaId`) e só então comparar
com `armadorIds`. Na UI isso é um lookup a mais; na regra do Firestore, um `get()` a mais por escrita.

### 8. O tipo de embarcação decide o que a rota pode vender

`Navio` ganha **`tipoEmbarcacao`** — String do Catálogo, categoria nova `TIPO_EMBARCACAO`. Três valores no
começo, e eles não são rótulo decorativo: definem **o que a embarcação carrega**.

| Tipo | Passageiros | Carro / Moto | Caminhão / Carreta |
|---|---|---|---|
| **F/B** (Ferry Boat) — é balsa | sim | sim | **sim** |
| **Navio** | sim | sim (limitado) | não |
| **Lancha** | sim | não | não |

A regra é **pura**: `tipoEmbarcacao` → conjunto de classes de veículo admitidas. Ela governa duas telas, e nas
duas o efeito é *não oferecer o impossível*:

- **cadastro da rota:** a tabela de tarifa (ADR-0013, eixo acomodação × classe de veículo) só oferece as células
  admissíveis — não se cadastra tarifa de carreta numa lancha;
- **emissão:** o form não oferece veículo que a embarcação da rota não leva.

Isso mata na origem uma classe inteira de erro que hoje seria só validação tardia: com a rota sabendo o navio
(§7) e o navio sabendo o tipo, o form nunca chega a montar uma passagem que a embarcação não pode cumprir.

**A exceção ao §3, nomeada.** O §3 diz "quem tem regra vira tipo de domínio; quem é só rótulo vira linha de
catálogo". O tipo de embarcação é **os dois** — vem do catálogo e tem comportamento. A resolução: o catálogo
guarda a **lista** (a gestão acrescenta "Catamarã" sem deploy), e a **capacidade** é código, chaveada pelo valor
canônico. A consequência é desconfortável e é a certa: **cadastrar um tipo novo no catálogo não lhe dá
capacidade de veículo até o código dizer** — ele nasce só-passageiro. É fail-closed (ADR-0010/0013), e é
preferível ao contrário, que seria um tipo novo levando carreta por omissão. O mesmo vale para a **atuação**
(§4): valor novo no catálogo não ganha painel sozinho.

Os **limites** ("navio leva carro e moto de forma limitada") ficam para a evolução. Quando entrarem, a
capacidade deixa de ser conjunto e passa a ser **quantidade** — e aí ela quer ser dado no documento, não código.
O caminho já está meio construído: `Navio` tem `capacidadeVeiculo`, `capacidadeSuite2`, `capacidadeSuite3` e
`capacidadeCamarote`.

### 9. ~~O Room não é tocado~~ — superado pelo ADR-0017

> **SUPERADO (7ª rodada).** O [ADR-0017](0017-eixo-de-storage-firestore-only.md) **aposentou o Room como
> datasource**, o que dissolve tanto o problema quanto a solução desta seção: não há mais "espelho parcial" a
> justificar, porque não há mais espelho. Dois efeitos diretos:
>
> - **O *Ponto aberto 1* deixa de existir.** Ele dizia que renomear `Constante` → `Catalogo` tocava
>   `FluviAppDatabase`, o `DDL_V2` e o `schemas/2.json`, e era *"o único bloqueio para começar"*. Sem espelho,
>   **não há entidade Room para renomear**. O ADR-0017 F1 faz exatamente esse rename como piloto.
> - **O *Ponto aberto 5* ("a Rota precisa de espelho?") responde-se sozinho:** não, por construção.
>
> E a 7ª rodada acrescenta o desenho de destino: **a entity plana do Room vira DTO**, a figura central entre as
> camadas (datasource → repositório → caso de uso → UI). A classe não some — muda de camada, e a entidade de
> domínio nasce ao lado dela, rica e composta (§5). A intuição desta seção continua correta e vira outra coisa:
> o painel é **online-only** porque o perfil de uso é de gestão, não porque o Room ficou de fora dele.

O raciocínio original, preservado como registro:

O ADR-0003 fixou que o Room espelha o Firestore. Se toda coleção nova exigisse espelho, este ADR seria um
rework de persistência. Não é, e a razão não é economia: é que **o painel administrativo tem outro perfil de
uso**. Cadastro de empresa, atuação, localidade, porto e catálogo é operação de gestão — baixo volume, feita
sentada, com rede, sem urgência offline. A emissão de passagem é o oposto: alto volume, na doca, com
conectividade instável (ADR-0001).

Então o painel é **online-only, sem cache**: lê e escreve Firestore direto. Isso acrescenta ao ADR-0003 uma
terceira categoria ao lado de "volátil / cacheada / sólida" — **dado administrativo, não espelhado** — e o
critério que a define é o perfil de uso, não a conveniência.

O caminho operacional (emissão) mantém o cache Room que já tem. O Room passa a ser um espelho **parcial** e
declaradamente assimétrico, e essa assimetria tem que estar documentada onde ela morde: `ConstanteDao` e
companhia continuam existindo para o que a operação lê offline. **Rota é o caso de fronteira** — é cadastro
(gestão) mas é lida na emissão (operação), então ela provavelmente precisa de espelho. Ver *Pontos abertos*.

### 10. Bootstrap: o primeiro ADM

Matar o seed cria um impasse que precisa de resposta explícita, porque sem ela o app fica inacessível num
projeto novo: **não há usuário para criar o primeiro usuário**. Sem seed, sem autocadastro (ADR-0015 §2.1) e
sem Admin SDK, um Firebase vazio não tem ninguém que possa logar — e portanto ninguém que possa cadastrar.

Decisão: **bootstrap manual e documentado**, não código. Criar o primeiro `ADM` é passo de *provisionamento de
ambiente*, não funcionalidade do app: criar a conta no Firebase Auth pelo console e o documento `users/{uid}`
com `papel: ADM` e `funcionarioId` vazio. A partir dele, todo o resto entra pelo painel.

Isso é o que responde, finalmente, a pergunta do Pilar 3 ("grupo de testers manual ou sincronizado dos
usuários?"): **manual, e não por preguiça** — é que a plataforma não tem, por decisão, caminho self-service. E
resolve o item 2 do Contexto: o passo pertence ao README/ambiente, que é exatamente onde a esteira de CI/CD
precisa que ele esteja escrito.

Alternativa rejeitada: uma tela de "primeiro ADM" liberada por regra quando `users` está vazia. Custa uma regra
permissiva que existe para ser usada uma vez na vida do projeto e fica lá para sempre — superfície de ataque
desproporcional ao que economiza.

## Plano de migração (faseado)

As fases são ordenadas por **dependência**, e as duas primeiras não têm pré-requisito nenhum.

- **F1 — Catálogo.** `Constante` → `Catalogo` (categoria + descricao); remover `Constante.Descricao` e
  `Constante.Categoria`; `IObjetoSimplificado` fica só no catálogo (remover de `Funcionario` e `Navio` — §3).
  Entram as categorias `UF`, `TIPO_EMBARCACAO` e `ATUACAO`. *Esta fase **é** a F1 do ADR-0017 (a
  coleção-piloto do eixo de storage) — as duas foram fundidas de propósito, e lá está a ordem interna.*
- **F2 — Matar o seed.** Remover `SeedFirestore`, `sampledata` e a chamada em `LoginViewModel:64`. Documentar o
  bootstrap do §10 no README. *Depois desta fase o app não abre cheio — F3 é o que devolve o caminho do dado.*
- **F3 — Painel administrativo (a base).** Separar as duas famílias de seção (§2) e corrigir a política:
  emitir exige `funcionarioId`; painel exige papel de plataforma. Reaproveitar o molde de cadastro do
  ADR-0006 — empresa e catálogo primeiro, que são as que destravam o resto.
- **F4 — Capacidades da plataforma.** `localidades` e `portos` na raiz, com cadastro no painel (§5). Nesta ordem:
  o porto seleciona uma localidade, então ela vem primeiro. Não dependem de empresa nenhuma; podem vir antes da
  F5. *(`trechos` saiu na 7ª rodada.)*
- **F5 — Parte e atuação.** Cadastro de empresa com suas **atuações** (`atuacoes/{ATUACAO}`) e a **concessão** de
  `portoIds`/`armadorIds` na atuação de agenciamento (§4, §7). `Navio` ganha `tipoEmbarcacao` e **fica onde está**.
  Aqui `Agencia` e `Funcionario.Lotacao` (enums) morrem.
- **F6 — Funcionário multi-empresa.** `empresaIds: [ … ]`; seleção de contexto quando houver mais de um;
  `EscopoAgencia` passa a recortar por empresa (§6).
- **F7 — Rota.** `Viagem` → `Rota` em `empresas/{id}/rotas`, com `navioId`, os dois portos por id, a tabela de
  tarifa do ADR-0013 e a agenda semanal; ocorrências calculadas (§7). A capacidade por tipo de embarcação (§8)
  entra aqui, governando as células de tarifa e o form de emissão. As duas validações de coerência da rota também.
- **F8 — Regras e suíte.** `firestore.rules` para as subcoleções, para o catálogo e para as capacidades da
  plataforma `localidades`/`portos` (escrita **só por papel de plataforma**), a **unicidade de `(uf, municipio)`**
  (§5), mais a regra da rota que confere a concessão (§7); reescrever a suíte de emulador. Esta fase acompanha F3–F7 incrementalmente, não fecha no fim —
  regra escrita depois é regra que passou um tempo aberta.

## Consequências

- **As regras do Firestore são reescritas, e a suíte de 57 casos com elas.** Subcoleção não herda regra: cada
  nível de `empresas/{id}/…` precisa de `match` próprio. É o maior item de custo deste ADR — mas menor do que nas
  rodadas anteriores: com `localidades` e `portos` sendo capacidades da plataforma, a escrita neles é papel puro
  (`ehPapelPlataforma`), sem regra híbrida papel-ou-cargo.
- **A regra mais delicada é a da rota:** validar a concessão (§7) obriga a regra a **ler dois outros documentos**
  durante o write — a atuação, para os portos, e o **navio**, porque o armador é conferido indiretamente
  (`navios/{id}.empresaId ∈ armadorIds`). São duas leituras por escrita de rota; sem elas o recorte concedido só
  existe na UI.
- **O isolamento fica misto:** rota e atuação por **caminho**; navio por **campo** (`empresaId`). Regra de campo é
  mais sutil — o cliente tem de filtrar a query, senão ela é negada inteira. É o preço de o navio ser referenciado
  entre partes (§4), e é assimetria consciente, não descuido.
- **"Agência" deixa de ter entidade.** Na UI a palavra continua (o operador diz "minha agência"), mas no modelo é
  a empresa na atuação de agenciamento. É confusão em potencial para quem for ler o código depois, e por isso o
  §4 existe — o vocabulário tem que estar escrito onde se lê.
- **`podeCriarPassagem` e `podeAcessar` mudam de critério**, e `SecaoMenu` deixa de ser uma família só. Quem
  depende delas hoje (menu, form de passagem, regras) muda junto. O ganho é fechar a contradição entre
  `Usuario.kt:30-32` e a política.
- **Um valor novo de catálogo — tipo de embarcação ou atuação — nasce sem capacidade** (§8). É fail-closed
  intencional, e vai surpreender quem cadastrar "Catamarã" esperando que ele leve carro.
- **O painel ganha uma ordem de dependência entre cadastros**, e ela não é imposta por wizard: catálogo →
  localidade → porto e empresa → atuação/concessão → funcionário → rota. Cada módulo faz uma coisa, então a ordem se
  manifesta no que há **para selecionar**.
- **O estado vazio passa a ser um estado de primeira classe do painel.** É o efeito direto de matar o seed (§1):
  quem abre o app pela primeira vez encontra vários seletores vazios, e cada um precisa dizer *o que cadastrar
  antes* em vez de só mostrar uma lista sem itens.
- **Remover um porto pode invalidar rotas já montadas** — e a *concessão* de quem o referencia. O MVP não remove,
  o que empurra o problema em vez de resolvê-lo. O mesmo vale para uma `Localidade` que portos já usam.
- ~~**O Room fica um espelho declaradamente parcial.**~~ Superado: o ADR-0017 tirou o Room do caminho, e com ele
  a exceção que esta consequência criava no ADR-0003 (§9).
- **Um cadastro a menos e um invariante a mais** (7ª rodada): o painel perde o módulo de trecho e ganha o de
  localidade, com a **unicidade de `(uf, municipio)`** — a primeira regra de unicidade do domínio, e ela precisa
  valer no servidor, não só na tela.
- **A ocupação continua O(n) sobre bilhetes**, por não persistir ocorrências (§7).
- **Aviso de mudança de porto pressupõe um canal que não existe.** Se o embarque muda de última hora, a decisão é
  **comunicar por mensagem no app**, não modelar — sem remarcação, reemissão nem versionamento de rota. Mas o
  projeto tem Analytics, Auth, Firestore e Crashlytics, e **não tem FCM**: o caminho mais barato é uma coleção de
  avisos lida pelo app, sem push. Fica fora deste ADR.

## Alternativas consideradas

- **Segmento como nome de subcoleção** (`fluvial`, `agenciamento`, `portuaria` — o desenho da 3ª rodada).
  Rejeitado na 6ª: empilhava **atuação** e **ativo** no mesmo nível (`agenciamento/1` não conteria agências,
  porque a agência é a própria empresa), comprometia a estrutura com o eixo **modal** que este ADR não abre, e
  tornava o segmento indescobrível pelo cliente — obrigando a um campo `segmentos` denormalizado (§4).
- **Navio dentro da empresa** (rodadas 1 a 5). Rejeitado na 6ª: o navio é referenciado **entre** partes (a
  agência vende passagem em navio que não é dela), então precisa de endereço global. Aninhá-lo obrigaria a rota a
  guardar o par `(armadorId, navioId)`. Ironia útil: o código já fazia certo.
- **Tipo de empresa / herança** em vez de atuações. Rejeitado: uma parte exerce **várias** atuações ao mesmo
  tempo e muda de conjunto no tempo — as duas coisas que subtipo não modela (§4).
- **Empresa como campo `empresaId` em tudo** (nada aninhado). Mantém queries simples e sem collection group.
  Rejeitada para rota e atuação: isolar por empresa passaria a depender de todo lugar filtrar corretamente, e a
  regra ficaria mais frágil que um recorte de caminho.
- **Trecho dentro da agência** (1ª rodada). Rejeitado na 2ª: tornava o trecho privado, duplicava a mesma linha
  por empresa e empurrava o cruzamento multi-empresa para uma agregação sobre documentos que só o nome relacionaria.
- **Trecho como competência compartilhada plataforma+supervisor** (2ª rodada). Substituído na 4ª pelo modelo de
  **capacidade concedida**: resolve o mesmo problema (o supervisor não pode ficar bloqueado) sem espalhar a
  escrita de um bem comum, e simplifica a regra do Firestore.
- **Porto dentro da empresa** (rodadas 1 e 2). Rejeitado na 3ª: um documento por empresa para o mesmo cais.
- **Um nível só** (trecho com portos e tarifas juntos). É o que existe hoje em `Viagem`, e é justamente a
  confusão que a nota de arquitetura descreveu.
- **Capacidade de veículo como dado no documento** (em vez de regra em código). Adiada, não rejeitada: com três
  tipos e sem limites numéricos, a regra em código é mais simples e fail-closed de graça (§8).
- **Catálogo por categoria em coleções separadas** (`municipios`, `documentos`, …). Rejeitada: multiplica
  coleções e regras para o que é, por natureza, par categoria/descrição.
- **Trecho como entidade** (rodadas 1 a 6, em quatro formas diferentes: dentro da agência, competência
  compartilhada, capacidade concedida e capacidade da plataforma). **Dissolvido na 7ª**, e por um motivo que
  nenhuma das rodadas anteriores tinha percebido: com a rota apontando dois portos e o porto sabendo sua
  localidade, o par de cidades é **derivável** — o trecho era uma terceira cópia da mesma informação, e o
  invariante geográfico existia só para mantê-la coerente com as outras duas (§7).
- **`cidade` como String no porto** (rodadas 3 a 6). Substituída na 7ª pela `Localidade`: a justificativa antiga
  ("rótulo é dado por valor") é de persistência, e persistência não decide forma de domínio. O rótulo continua por
  valor — dentro da `Localidade`, que é quem tem identidade (§5).
- **UF como campo solto no porto**, sem `Localidade`. Rejeitada na 7ª: resolveria a exibição, mas não daria
  dimensão ao eixo analítico — agrupar por UF continuaria sendo agrupar String repetida em N portos, com a
  divergência de grafia como consequência inevitável.
- **Persistir as ocorrências da semana** (nota §3.3). Daria contador de ocupação O(1). Fora do MVP.
- **Cloud Functions para provisionar conta.** Resolveria o §10 de verdade. Fora do MVP — ver Opção 3.

## Alternativas futuras

Revisitar quando:

- **Entrar o segundo modal** (rodoviário, aéreo). É aí que se decide se `navios` vira frota polimórfica com tipo,
  e **só aí** — o eixo modal está deliberadamente fechado neste ADR (§4). O `tipoEmbarcacao` (§8) é o ensaio da
  pergunta dentro de um modal só.
- **Entrar um segmento novo.** A aposta a validar é a atuação-documento: se o segmento novo entrar como valor de
  catálogo + doc de atuação + suas concessões, sem tocar estrutura, o §4 se paga.
- **A rede matriz/filial precisar existir.** Num modelo de documentos é **um campo** no doc da empresa apontando
  para outra empresa. Não há nada a preparar: hoje o agenciamento pode ser remoto, e a distinção física não é
  necessária (§4).
- **Os limites de veículo por embarcação entrarem** (§8): a capacidade sai do código e vira dado do documento.
- **O porto precisar de coordenadas** (7ª rodada, adiado): mapa e geolocalização chegam junto com a plataforma
  auto-gerenciável, e aí `Porto` ganha latitude/longitude. Fora agora — não há tela que use.
- **A atuação portuária acordar** (§5) — o gatilho é o **módulo de check-in**. Nenhuma rota precisa mudar; foi
  para isso que o porto ficou na raiz.
- **Cruzar ocupação entre empresas:** é o gatilho para persistir as ocorrências e ganhar o contador por
  acomodação. O ponto de encontro da agregação é o **porto** — e, acima dele, a `Localidade`, que é a dimensão
  geográfica de verdade (§5). *(Antes da 7ª rodada este papel era do trecho compartilhado.)*
- **O relatório por linha existir** (7ª rodada): sem `trechoId`, agrupar por "Manaus → Parintins" é consulta sobre
  o par de portos, ou uma denormalização das duas localidades na rota. É escolha de **leitura**, e não é motivo
  para reviver o trecho (§7).
- **Houver mais de um cargo por pessoa** (supervisor numa empresa, agente em outra): `cargo` sai do documento e
  entra no vínculo (§6), e a política ganha o cargo como parâmetro do par (empresa, cargo).

## Pontos abertos (analista decide)

1. ~~**O rename `Constante` → `Catalogo` toca o Room** … é o único bloqueio para começar.~~
   **RESOLVIDO pelo ADR-0017 (7ª rodada): o bloqueio deixou de existir.** Sem espelho, não há entidade Room para
   renomear — nem `DDL_V2`, nem `schemas/2.json`, nem `kspDebugKotlin`. A F1 daqui **é** a F1 de lá, e a coleção
   mais simples do app foi escolhida como piloto do eixo de storage justamente por isso (§9).
2. ~~**Remoção de porto/armador da concessão**~~ — **RESOLVIDO na 7ª rodada.** Para catálogo, localidade e porto:
   **não se remove, desativa-se** (`ativo`, §5) — o item some dos seletores e continua resolvendo o que já o
   referencia, sem collection group para verificar. Para o **armador saindo de `armadorIds`**: é **caso de
   sincronização, não de modelo**. A concessão é referência viva; tirar um armador não invalida
   retroativamente nada — a regra de escrita da rota (F8) barra as escritas seguintes, e as rotas já gravadas
   continuam legíveis até serem reconciliadas. Não há estado a inventar: nem `ativo` no array, nem
   versionamento de rota.
3. **A agenda semanal fica na Rota** (§7) — assumi, porque a rota é a viagem e a agenda é o *quando*.
4. **A Rota referencia o navio** (§7) — assumi, porque sem isso o §8 não sabe o tipo de embarcação para filtrar as
   tarifas. Confirma que a embarcação é definida na rota, e não só na emissão?
5. ~~**A Rota precisa de espelho no Room?**~~ **RESOLVIDO pelo ADR-0017 (7ª rodada): não, por construção** — não
   há Room a espelhar. O caminho offline da emissão passa a ser o cache do SDK (ADR-0017 D6).
6. **`SUPERVISOR` monta a rota, `AGENTE` só emite** (§2) — assumi, porque a rota carrega a tarifa e preço é
   decisão de quem responde pela operação. Junto: **a concessão é editável depois do cadastro** da atuação, ou só
   na criação? Assumi editável.
7. **Cargo por pessoa ou por vínculo** (§6); e **onde se escolhe o contexto** quando há dois vínculos — login ou
   emissão?
8. **`GESTOR` cadastra tudo que o `ADM` cadastra**, ou há algo exclusivo do `ADM` no painel (criar empresa, por
   exemplo)? Hoje `ehPapelPlataforma` trata os dois como um só. *Parcialmente respondido pelo ADR-0017 §7.1: o
   CRUD do catálogo é **só do `ADM`** — é a primeira vez que os dois papéis se separam. Falta dizer se a regra
   vale para o resto do painel.*
9. *(7ª rodada)* **Quanto de rótulo viaja da `Rota` para o `Porto`?** Já está decidido para a `Localidade` (ela
   embute `id` + `descricao` de cada `Catalogo`, §5) e para `Porto.localidade` (referência). Falta a rota: guarda
   só `embarquePortoId`, ou guarda `{id, nome, municipio, uf}` para desenhar a linha sem uma segunda leitura?
   Guardar rótulo é **cache de leitura**, não verdade — e renomear um porto deixa cópias velhas até serem
   reescritas.
10. *(7ª rodada)* **O DTO é um por entidade ou um por caso de uso?** (§9) Um por entidade é o que existe hoje —
    a classe plana do Room vira o DTO; um por caso de uso evita carregar campo que a tela não usa, mas multiplica
    as classes. Assumi **um por entidade**, especializando só quando doer.

## Decisões resolvidas na conversa (analista, 2026-07-28; 7ª rodada em 2026-07-31)

**2ª rodada — os dois níveis da viagem**

- ~~Trecho é competência de plataforma E de negócio~~ — **superado pela 4ª rodada**. Sobrevive a segunda metade:
  quem usa uma ligação aplica **as próprias tarifas** (§7).
- ~~**Trecho tem cidade origem e cidade destino**, do Catálogo — não portos~~ — **superado pela 7ª rodada**: o
  trecho foi dissolvido, e o par de cidades passou a ser derivado dos portos (§7).
- **Rota é a viagem nova de verdade:** embarque e desembarque são **portos por id**, mais as tarifas (§7).
- **Mudança repentina de porto é comunicada por mensagem no app**, não modelada.
- **Tipo de embarcação (por catálogo) influencia as tarifas exibidas:** F/B leva tudo (é balsa); Navio leva carro
  e moto, com limites para a evolução; Lancha só passageiro. Navio e Lancha não são balsas, logo **não levam
  veículo pesado** (§8).

**3ª rodada — segmentos e porto**

- ~~Toda subcoleção de `empresas` é um segmento~~ — **superado pela 6ª rodada** (atuação é documento, não nome de
  coleção; e `fluvial` era modal, não segmento). Sobrevive a intenção: o segmento é dimensão de primeira classe.
- **Porto é superentidade:** sai de dentro da empresa e vai para a raiz — é lugar físico, não propriedade de quem
  navega (§5). Efeito colateral bom: a rota o referencia por **id simples**.
- **Uma empresa pode ter vários portos** — relação N, não contenção (§5).

**4ª rodada — capacidade concedida**

- **Trecho e porto são capacidades da PLATAFORMA** — só o painel cadastra (§7). *(A 7ª rodada dissolveu o trecho;
  o princípio ficou, aplicado a porto e localidade.)*
- **A parte nasce provisionada:** o cadastro requisita os portos que ela opera (§7).
- **`empresas/{id}/portuaria/{arrendatariaId}`:** a unidade do segmento portuário não é o porto — é a
  **arrendatária**, quem trabalha dentro do porto e faz o **check-in**. Segmento com painel no futuro (§5).
  *(A forma mudou na 6ª rodada — virou valor de atuação —, o conceito não.)*

**5ª rodada — um processo por cadastro**

- **Conceder ≠ cadastrar.** O form **seleciona** portos já cadastrados; se não existem, cadastra-se no módulo de
  cada um — **não no mesmo processo** (§7).
- **O supervisor também não sai desse processo:** funcionário é o módulo de funcionário, e o vínculo é o
  `empresaIds` do §6.
- Consequência que isso **desfaz**: não há operação composta nem escrita em lote. O que sobra é **ordem de
  dependência** entre cadastros.

**6ª rodada — parte, atuação e ativo**

- **`agenciaId` e `empresaId` são o mesmo id:** agência é **atuação** de uma empresa, não objeto contido nela.
  `empresas/{id}/agenciamento/1` não conteria agências — ela **é** a empresa (§4).
- **Navio é ativo**, não atuação: continua em `navios/{id}` com `empresaId`, como o código já faz (§4).
- **Atuação futura com portos atrelados:** operação portuária e arrendamento entram como **valores de atuação**
  com `portoIds`, sem nível novo de estrutura (§5).
- **Multi-empresa e multi-segmento sim; multi-modal não** — `fluvial` é modal, e por isso não nomeia a estrutura
  (§4).
- **Matriz/filial, se um dia:** é **um campo** no doc da empresa apontando para outra empresa. Hoje o
  agenciamento pode ser **remoto**, então a distinção física não é necessária (§4).
- **A agência agencia para armadores:** a concessão inclui `armadorIds` — empresas com atuação `TRANSPORTE` (§7).
  A relação entre partes é expressa como concessão, não como estrutura nova.

**7ª rodada — a lei é o domínio (2026-07-31)**

- **O domínio manda na persistência, e não o contrário.** Uma entidade **pode ter outra entidade como atributo**;
  como isso vira documento (embutido, por referência, denormalizado) é decisão da **camada de dados**, tomada pelo
  que o Firestore serve melhor em cada caso. Isso derruba a justificativa da 3ª rodada para `cidade: String`
  ("rótulo é dado por valor"), que era um argumento de persistência decidindo forma de domínio (§5).
- **A entity do Room vira DTO, e o DTO é a figura central entre as camadas** — datasource → repositório → caso de
  uso → UI, porque o dado é requisitado e usado entre elas. Ficam três formas com fronteira nomeada: **entidade =
  lei** (rica, composta, sem framework), **DTO = trânsito** (plano), **documento = armazenamento**. A classe plana
  anotada **não some com o ADR-0017 — muda de camada** (§9).
- **O critério que a teoria impõe, e que a regra acima exigia** (Vernon, *Effective Aggregate Design*, regra 3 —
  agregado referencia agregado por identidade): **embutir** o que é value object/rótulo · **referenciar** o que é
  agregado com ciclo de vida próprio · **congelar** só quando a cópia é imutável por desenho (o snapshot da
  Passagem, ADR-0008). Sem ele, "entidade contém entidade" autorizaria aninhar agregado em agregado — que é o
  problema real do modelo de documentos: duplicata viva a manter em N lugares.
- **O `Trecho` é dissolvido.** A rota aponta dois portos e o porto sabe sua localidade, logo o par de cidades é
  **derivável**: o trecho era uma terceira cópia da mesma informação (§7). Some a coleção, o `trechoId`, o
  `trechoIds[]` da concessão, o módulo de cadastro **e** o invariante geográfico, que existia só para manter as
  três cópias coerentes.
- **Nasce a `Localidade`** (`localidades/{id}`): **UF + município como uma coisa só**, ambos `Catalogo`
  **embutidos**. É a aplicação do critério acima, é a **dimensão do eixo analítico** (agrupar por UF e por
  município, coisa que String solta não permite) e enriquece a exibição sem consulta extra. **`Porto.cidade` vira
  `Porto.localidade`**, por referência (§5).
- **Invariantes novos:** `(uf, municipio)` é único, `codigoIbge` é único quando presente, `(categoria, descricao)`
  do catálogo é único e o **nome do porto é único dentro da localidade**. Duplicata fragmenta todo relatório que
  agrupe por ela, e o erro só aparece depois, no número errado. Valem na tela e no servidor (F8).
- **`Catalogo.Categoria` continua tipo fechado** — a 6ª rodada a matou junto com `Descricao`, e eram coisas
  diferentes: descrição é rótulo, **categoria é o índice do catálogo**, e há seis chamadores que consultam por
  ela. String livre trocaria um enum por literais espalhados, com lista vazia silenciosa no erro de digitação
  (§3).
- **`codigoIbge` na `Localidade`** — chave natural do município: unicidade de graça e, principalmente, a junção
  com dado externo quando o eixo analítico existir. Campo, não id do documento; opcional no cadastro, único
  quando presente (§5).
- **Desativar em vez de remover** (`ativo` em catálogo, localidade e porto): o item some dos seletores e continua
  resolvendo o que já o referencia. **Fecha o Ponto aberto 2** sem collection group (§5). O **armador saindo da
  concessão** é o caso que não cabe nesse molde — e é **sincronização, não modelo**: a concessão é referência
  viva, a regra barra as escritas seguintes e nada se invalida retroativamente.
- **`ordem` no catálogo** — hoje o item nasce por `.add()` e a lista sai na ordem que o Firestore devolver (§3).
- **Só o catálogo é `ADM`-only; localidade e porto são papel de plataforma** (`ADM` + `GESTOR`). O critério:
  **quanto mais perto o dado está da semântica do código, mais restrito é quem o escreve** — o catálogo é o
  vocabulário que o código consome, localidade e porto são cadastro de gestão corrente (§5).
- **A pressão do modelo não vem da estrutura, vem de dois regimes de carga.** Testados os três sinais de alerta do
  modelo de documentos: as três relações N:N passam (arrays pequenos, consulta de um lado só), a junção tripla do
  armador passa (um `get()` a mais), e **a agregação sobre `passagens` é o único ponto que aperta de verdade** —
  o Firestore tem `count`/`sum`/`average`, mas não tem `GROUP BY`. A resposta é o eixo analítico do ADR-0017, não
  desmontar o modelo de documentos que serve bem a operação.
