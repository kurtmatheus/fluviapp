# ADR-0016: Domínio da plataforma — parte, atuação e ativo (multi-empresa e multi-segmento)

**Status:** **Aceita (direção)** — decisões do analista, 1ª a 6ª rodada (2026-07-28). Sem código: este ADR fixa o
domínio e o mapa de coleções; a implementação é faseada abaixo. Supera a pergunta de provisionamento que estava
aberta no [roadmap do MVP](../design/mvp-roadmap.md) (Pilar 3) e promove a
[nota Viagem × Trecho](../design/viagem-vs-trecho.md) de "fora do MVP" para dentro dele, na versão do §7.

> Conversa com o [ADR-0003](0003-modelo-de-memoria-do-dado.md) (Room espelha Firestore),
> o [ADR-0008](0008-relacionamentos-por-identidade.md) (relacionar por id),
> o [ADR-0010](0010-autorizacao-por-cargo.md)/[ADR-0011](0011-regras-firestore-por-cargo.md) (política e regras),
> o [ADR-0013](0013-tabela-de-tarifa-e-tipo-tarifario.md) (tarifa tabelada) e
> o [ADR-0015](0015-rework-agente-equipe.md) (os dois contextos: papel × cargo).

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
porto, trecho, empresa, navio, agência, funcionário e rota antes de emitir a primeira passagem. É custo real de
demonstração, e é o preço de ter dado com dono. O §10 trata do único caso onde isso vira impasse.

### 2. Dois planos de acesso: quem cria o universo × quem cria a oferta

A separação que o ADR-0015 §8 desenhou em dois contextos (sistema × negócio) agora **também separa duas telas**:

| Quem | O que enxerga | O que faz |
|---|---|---|
| `ADM` / `GESTOR` (papel de plataforma, **sem** funcionário) | Só o **painel administrativo** | Cadastra empresa e suas atuações, navio, catálogo, funcionário e as capacidades da plataforma (**trecho** e **porto**). **Não emite passagem.** |
| `SUPERVISOR` (cargo, com funcionário) | A **operação** | Monta as **rotas** da empresa em que atua — com as tarifas dela — sobre os trechos e portos concedidos. Emite passagem. |
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

**A linha entre os dois planos é o que cada um cria.** A plataforma cria o **universo**: quais cidades se ligam
(trecho), quais lugares existem (porto), quais empresas existem e em que atuam. A empresa que agencia cria a
**oferta**: qual rota sai de qual porto, em qual embarcação, a que preço, em que dias. Nenhum dos dois invade o
outro, e o ponto de contato é a **concessão** (§7).

Isso mantém a política de seção **quase toda de sistema**. A `EQUIPE` continua sendo a única exceção que olha os
dois eixos (ADR-0015 §2.2): o supervisor gere os membros de onde atua.

Consequência de UI: `SecaoMenu` deixa de ser uma lista de seções de um menu só e passa a ter duas famílias.
`PASSAGEM` e `ROTA` ficam na operação; `EMPRESA`, `NAVIO`, `PORTO`, `TRECHO` e `CATALOGO` ficam no painel;
`EQUIPE` aparece nos dois. `VIAGEM` sai do menu — o nome estava errado desde o começo (§7).

### 3. `Constante` vira `Catalogo`, e `IObjetoSimplificado` fica só nele

`Constante` passa a se chamar **`Catalogo`** e tem exatamente dois campos além do id: **`categoria`** e
**`descricao`**. Ele é a tabela das **informações adjuntas** — o que o negócio precisa nomear mas não precisa
modelar: UF, município, tipo de passagem, tipo de documento, tipo de veículo, acomodação, forma de pagamento,
**tipo de embarcação** (§8) e **atuação** (§4).

Duas remoções acompanham o rename:

- **As tipificações internas saem.** `Constante.Descricao` e `Constante.Categoria` são enums que duplicam, com
  atraso, o que os tipos de domínio já dizem: `Descricao` ainda lista `CORTESIA` (aposentada pelo ADR-0013),
  `A_EMITIR` e `EMITIDA` (hoje são `StatusPassagem`, com FSM — ADR-0012). Manter dois vocabulários para a mesma
  coisa é a receita de divergirem. Quem tem regra vira **tipo de domínio**; quem é só rótulo vira **linha de
  catálogo**. A categoria passa a ser String livre na fronteira, validada por quem consome.
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
(porto, trecho, catálogo — sem dono) e **concessão** (o recorte que uma parte pode operar).

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
   │        AGENCIAMENTO           → trechoIds[], portoIds[], armadorIds[]   ← concessões (§7)
   │        TRANSPORTE             → (é o armador; a frota dele é global)
   │        PORTUARIA_OPERACAO     → portoIds[]                     ← dormente (§5)
   │        PORTUARIA_ARRENDAMENTO → portoIds[]                     ← dormente (§5)
   └── rotas/{rotaId}                    a oferta — só existe com atuação AGENCIAMENTO (§7)

# CAPACIDADES DA PLATAFORMA — sem dono
portos/{portoId}                         nome, cidade
trechos/{trechoId}                       cidadeOrigem, cidadeDestino
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

### 5. Porto é capacidade da plataforma; a atuação portuária nasce dormente

```
portos/{portoId}
    id, nome, cidade                         ← cidade do Catálogo (MUNICIPIO), por valor
```

O porto mora na raiz, é da plataforma e não pertence a ninguém — como o `trechos` e o `catalogo`. Um porto é um
**lugar físico**, e lugar físico não é propriedade de quem navega: o cais de Manaus é o mesmo cais para todas as
empresas que atracam nele. Modelá-lo dentro da empresa produziria um documento por empresa para o mesmo lugar.

`cidade` é **String gravada a partir do Catálogo** (categoria `MUNICIPIO`) — não é id. A escolha contraria o
ADR-0008 de propósito: o que o catálogo dá é um **rótulo**, e rótulo é dado por valor. Relacionar por id vale
para o que tem vida própria e muda (empresa, navio, trecho, rota, porto); o nome de um município não muda, e
transformá-lo em referência custaria uma resolução de id a cada leitura para nada.

O ganho de o porto ser capacidade da plataforma aparece no §7: a rota o referencia por **id simples**, válido
para qualquer empresa. Se o porto vivesse dentro de uma delas, a referência teria de carregar o `empresaId` junto,
e acordar a atuação portuária — quando o dono do documento mudasse — reescreveria toda referência existente.

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

### 7. Trecho é capacidade da plataforma; Rota é a oferta da agência

Aqui a nota [Viagem × Trecho](../design/viagem-vs-trecho.md) se resolve, e em dois níveis em vez de um. O erro de
vocabulário não era só "viagem devia ser trecho": era que **duas coisas diferentes estavam espremidas numa
entidade só**.

**Trecho — `trechos/{trechoId}`, capacidade da plataforma.**

```
trechos/{trechoId}
    id, cidadeOrigem, cidadeDestino          ← ambas do Catálogo (MUNICIPIO), por valor
```

O trecho é o **par de cidades** — o que o mercado chama de linha (Manaus → Parintins). Não tem data, não tem
tarifa, não tem porto e **não tem dono**. Só o painel cadastra, e é isso que faz dele um bem comum de verdade em
vez de um cadastro que cada empresa refaz.

**A concessão: a empresa recebe o recorte que pode operar.** A atuação de agenciamento guarda os trechos, os
portos e os **armadores** concedidos:

```
empresas/{empresaId}/atuacoes/AGENCIAMENTO
    trechoIds:  [ … ]        ← quais linhas
    portoIds:   [ … ]        ← quais lugares
    armadorIds: [ … ]        ← para quais transportadores ela agencia
```

`armadorIds` aponta para **empresas com atuação `TRANSPORTE`** — e é aqui que a atuação de transporte deixa de ser
um rótulo vazio: ela é o que qualifica uma parte a ser agenciada. A relação agência↔armador, que num modelo
relacional seria uma tabela de associação entre partes, aqui é a **concessão** — o mesmo mecanismo que já governa
trecho e porto, aplicado a uma parte em vez de a uma capacidade. Uma peça, três usos.

Isso resolve o impasse que as rodadas anteriores deixaram: se só a plataforma cadastra trecho e porto, o
supervisor ficaria bloqueado esperando alguém criar o que ele precisa. Não fica — **a empresa já chega
provisionada**. Ele não escolhe de um universo aberto; monta rotas dentro do recorte que recebeu, e esse recorte é
**concessão explícita**, não consequência de quem cadastrou primeiro.

É o mesmo mecanismo de "capacidades" do ADR-0015 (§2, agência e lotação como capacidades do usuário), um nível
acima — capacidades **da parte**, concedidas pela plataforma. O vocabulário coincidir não é acidente.

**Conceder não é cadastrar, e os processos não se misturam.** O form da empresa/atuação **só seleciona** — se o
trecho ou o porto não existe, cadastra-se no módulo dele, e depois volta-se aqui. E o supervisor **também não sai
deste processo**: quem cria funcionário é o módulo de funcionário. Cada cadastro faz **uma coisa**, no molde do
ADR-0006. Um form que criasse trecho, porto e funcionário de passagem seria três responsabilidades numa tela, com
escrita composta que pode falhar no meio e deixar empresa sem capacidade ou sem supervisor. Separados, não há
transação a orquestrar. E o vínculo do supervisor **já tem casa**: é o `empresaIds` do §6 — a pessoa sabe onde
atua, a empresa não precisa saber quem a supervisiona.

**Rota — `empresas/{empresaId}/rotas/{rotaId}`, da empresa que agencia. É a viagem.**

```
rotas/{rotaId}
    id, trechoId                             ← qual par de cidades esta rota realiza
    navioId                                  ← qual embarcação opera (governa as tarifas — §8)
    embarquePortoId, desembarquePortoId      ← portos, por id simples (§5)
    tarifas                                  ← a tabela do ADR-0013, da empresa
    agenda: [ { diaSemana, hora }, … ]       ← dias em que opera e a hora de cada dia
```

A rota é **como uma empresa realiza aquele par de cidades**: de qual porto sai, em qual porto atraca, com qual
embarcação, a que preço e em que dias. É isso que a nota chamava de "Viagem" — o trecho com quando —, e é por
isso que ela mora na parte: **a tarifa é dela**. Note a profundidade: sem um nível de "agência" intermediário, a
rota fica a **quatro** níveis, e continua resolvível a partir da passagem, que já carrega `empresaId` e
`viagemId`.

É essa divisão que resolve o que a nota previa no §4: duas empresas que vendem a mesma linha **compartilham o
trecho** e têm **rotas próprias**, com portos e preços próprios. Nada é duplicado que devesse ser comum, e nada é
comum que devesse ser de alguém.

**As viagens concretas não são persistidas.** As ocorrências da semana são **calculadas** a partir da agenda da
rota e da semana corrente. Não existe coleção de ocorrências no MVP. Isso adia de propósito o item 3 da nota
(viagens geradas com contador por acomodação), e o custo é nomeado: **a ocupação continua sendo contada a partir
dos bilhetes** — o ganho de leitura O(1) fica para depois, e a contagem do Pilar 1 segue como está. É o certo
para um MVP: agenda é modelo, contador é otimização.

**A coerência da rota é regra pura, e tem duas camadas — vale escrever as duas:**

1. **Geográfica:** o porto de embarque tem que estar na `cidadeOrigem` do trecho, e o de desembarque na
   `cidadeDestino`. Como o porto guarda a cidade (§5), a checagem é local e barata — e é ela que impede uma rota
   de dizer que vai a Parintins atracando em Manaus.
2. **De concessão:** o `trechoId` e os dois portos têm que estar entre as capacidades da atuação
   (`trechoIds`/`portoIds`), e o **dono do navio** tem que estar em `armadorIds`. Sem isso, o recorte concedido
   seria decorativo: bastaria digitar um id de fora.

As duas são puras e testáveis sem device, no molde do ADR-0006 — e a segunda tem que valer **também no servidor**
(F8), porque é ela que impede uma empresa de operar o que não lhe foi concedido.

A checagem do armador tem uma característica que as outras não têm, e vale registrar: ela é **indireta**. A rota
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

### 9. O Room não é tocado — e é o painel que torna isso coerente

O ADR-0003 fixou que o Room espelha o Firestore. Se toda coleção nova exigisse espelho, este ADR seria um
rework de persistência. Não é, e a razão não é economia: é que **o painel administrativo tem outro perfil de
uso**. Cadastro de empresa, atuação, porto, catálogo e trecho é operação de gestão — baixo volume, feita
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
  `Constante.Categoria`; `IObjetoSimplificado` fica só no catálogo (remover de `Funcionario` e `Navio`, sem
  renomear coluna — §3). Entram as categorias `TIPO_EMBARCACAO` e `ATUACAO`.
- **F2 — Matar o seed.** Remover `SeedFirestore`, `sampledata` e a chamada em `LoginViewModel:64`. Documentar o
  bootstrap do §10 no README. *Depois desta fase o app não abre cheio — F3 é o que devolve o caminho do dado.*
- **F3 — Painel administrativo (a base).** Separar as duas famílias de seção (§2) e corrigir a política:
  emitir exige `funcionarioId`; painel exige papel de plataforma. Reaproveitar o molde de cadastro do
  ADR-0006 — empresa e catálogo primeiro, que são as que destravam o resto.
- **F4 — Capacidades da plataforma.** `portos` e `trechos` na raiz, com cadastro no painel (§5, §7). Não dependem
  de empresa nenhuma; podem vir antes da F5.
- **F5 — Parte e atuação.** Cadastro de empresa com suas **atuações** (`atuacoes/{ATUACAO}`) e a **concessão** de
  `trechoIds`/`portoIds` na atuação de agenciamento (§4, §7). `Navio` ganha `tipoEmbarcacao` e **fica onde está**.
  Aqui `Agencia` e `Funcionario.Lotacao` (enums) morrem.
- **F6 — Funcionário multi-empresa.** `empresaIds: [ … ]`; seleção de contexto quando houver mais de um;
  `EscopoAgencia` passa a recortar por empresa (§6).
- **F7 — Rota.** `Viagem` → `Rota` em `empresas/{id}/rotas`, com `trechoId`, `navioId`, portos por id, a tabela de
  tarifa do ADR-0013 e a agenda semanal; ocorrências calculadas (§7). A capacidade por tipo de embarcação (§8)
  entra aqui, governando as células de tarifa e o form de emissão. As duas validações de coerência da rota também.
- **F8 — Regras e suíte.** `firestore.rules` para as subcoleções, para o catálogo e para as capacidades da
  plataforma `trechos`/`portos` (escrita **só por papel de plataforma**), mais a regra da rota que confere a
  concessão (§7); reescrever a suíte de emulador. Esta fase acompanha F3–F7 incrementalmente, não fecha no fim —
  regra escrita depois é regra que passou um tempo aberta.

## Consequências

- **As regras do Firestore são reescritas, e a suíte de 57 casos com elas.** Subcoleção não herda regra: cada
  nível de `empresas/{id}/…` precisa de `match` próprio. É o maior item de custo deste ADR — mas menor do que nas
  rodadas anteriores: com `trechos` e `portos` sendo capacidades da plataforma, a escrita neles é papel puro
  (`ehPapelPlataforma`), sem regra híbrida papel-ou-cargo.
- **A regra mais delicada é a da rota:** validar a concessão (§7) obriga a regra a **ler dois outros documentos**
  durante o write — a atuação, para trecho e portos, e o **navio**, porque o armador é conferido indiretamente
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
  porto/trecho e empresa → atuação/concessão → funcionário → rota. Cada módulo faz uma coisa, então a ordem se
  manifesta no que há **para selecionar**.
- **O estado vazio passa a ser um estado de primeira classe do painel.** É o efeito direto de matar o seed (§1):
  quem abre o app pela primeira vez encontra vários seletores vazios, e cada um precisa dizer *o que cadastrar
  antes* em vez de só mostrar uma lista sem itens.
- **Remover um trecho ou porto pode invalidar rotas já montadas** — e a *concessão* de quem os referencia. O MVP
  não remove, o que empurra o problema em vez de resolvê-lo.
- **O Room fica um espelho declaradamente parcial.** Ganho: nenhuma migração neste round. Custo: a regra "Room
  espelha Firestore" do ADR-0003 passa a ter exceção, e exceção não escrita é dívida — daí o §9 existir.
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
- **A atuação portuária acordar** (§5) — o gatilho é o **módulo de check-in**. Nenhuma rota precisa mudar; foi
  para isso que o porto ficou na raiz.
- **Cruzar ocupação entre empresas:** é o gatilho para persistir as ocorrências e ganhar o contador por
  acomodação. O trecho compartilhado (§7) já preparou o terreno — é ele o ponto de encontro da agregação.
- **Houver mais de um cargo por pessoa** (supervisor numa empresa, agente em outra): `cargo` sai do documento e
  entra no vínculo (§6), e a política ganha o cargo como parâmetro do par (empresa, cargo).

## Pontos abertos (analista decide)

1. **O rename `Constante` → `Catalogo` toca o Room, e a restrição era não tocá-lo.** Remover a interface não
   toca (§3), mas renomear a *entidade/tabela* sim: mudam `FluviAppDatabase`, o `DDL_V2` e o
   `schemas/…/2.json`. Só que, pelo ADR-0015 §9, esse toque é **regenerar**, não migrar — não há versão
   publicada, então o schema é reescrito e o custo é rodar `:app:kspDebugKotlin` e trazer o `createSql` novo.
   Recomendação: fazer o rename completo em F1 e assumir o regenerate. Precisa da confirmação de que "sem mexer
   no Room" significa "sem redesenhar/migrar", não "sem regenerar". **É o único bloqueio para começar.**
2. **Remoção de trecho/porto/armador da concessão** — quem cadastra está resolvido (a plataforma), mas **remover**
   o que rotas já referenciam invalida rota e concessão. Assumi que o MVP não remove; se remover, a verificação de
   "nenhuma rota usa" exige collection group. Vale também para tirar um armador de `armadorIds`: as rotas com
   navios dele ficam órfãs de concessão.
3. **A agenda semanal fica na Rota** (§7) — assumi, porque a rota é a viagem e a agenda é o *quando*.
4. **A Rota referencia o navio** (§7) — assumi, porque sem isso o §8 não sabe o tipo de embarcação para filtrar as
   tarifas. Confirma que a embarcação é definida na rota, e não só na emissão?
5. **A Rota precisa de espelho no Room?** (§9) É cadastro, mas é lida na emissão — o caminho offline. Se sim, é a
   única coleção nova a tocar o Room, e a decisão do ponto 1 vale para ela também.
6. **`SUPERVISOR` monta a rota, `AGENTE` só emite** (§2) — assumi, porque a rota carrega a tarifa e preço é
   decisão de quem responde pela operação. Junto: **a concessão é editável depois do cadastro** da atuação, ou só
   na criação? Assumi editável.
7. **Cargo por pessoa ou por vínculo** (§6); e **onde se escolhe o contexto** quando há dois vínculos — login ou
   emissão?
8. **`GESTOR` cadastra tudo que o `ADM` cadastra**, ou há algo exclusivo do `ADM` no painel (criar empresa, por
   exemplo)? Hoje `ehPapelPlataforma` trata os dois como um só.

## Decisões resolvidas na conversa (analista, 2026-07-28)

**2ª rodada — os dois níveis da viagem**

- ~~Trecho é competência de plataforma E de negócio~~ — **superado pela 4ª rodada**. Sobrevive a segunda metade:
  quem usa um trecho da plataforma aplica **as próprias tarifas** (§7).
- **Trecho tem cidade origem e cidade destino**, do Catálogo — não portos (§7).
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

- **Trecho e porto são capacidades da PLATAFORMA** — só o painel cadastra (§7).
- **A parte nasce provisionada:** o cadastro requisita os trechos e portos que ela opera (§7).
- **`empresas/{id}/portuaria/{arrendatariaId}`:** a unidade do segmento portuário não é o porto — é a
  **arrendatária**, quem trabalha dentro do porto e faz o **check-in**. Segmento com painel no futuro (§5).
  *(A forma mudou na 6ª rodada — virou valor de atuação —, o conceito não.)*

**5ª rodada — um processo por cadastro**

- **Conceder ≠ cadastrar.** O form **seleciona** trechos e portos já cadastrados; se não existem, cadastra-se no
  módulo de cada um — **não no mesmo processo** (§7).
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
