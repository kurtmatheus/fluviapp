# Desenho de domínio — a plataforma inteira (entidades, campos, enums e regras)

**Status:** Documento de referência do domínio. Consolida o que está **no código hoje** com o que os
ADR-0015, [ADR-0016](../adr/0016-dominio-da-plataforma.md) e
[ADR-0017](../adr/0017-eixo-de-storage-firestore-only.md) **decidiram e ainda não foi construído**.
Ancorado no código em `2026-07-31`, depois do pacote `model` virar **`domain`**.

> Substitui, como visão geral, o [estudo transversal de domínio](dominio-relacionamentos-e-camadas.md)
> (escrito na era do ADR-0008, quando ainda havia `Agente` e relação por nome). Não substitui o
> [desenho do agregado Passagem](dominio-passagem.md), que continua sendo a fonte para o bilhete, nem a
> nota [Viagem × Trecho](viagem-vs-trecho.md). Decisões-fonte por assunto estão citadas em cada seção.

**Como ler os marcadores.** O domínio está em transição, e misturar o que existe com o que foi decidido é
a forma mais rápida de tornar um documento destes inútil:

| Marcador | Significa |
|---|---|
| **[hoje]** | está no código, funcionando |
| **[alvo]** | decidido em ADR, **ainda não construído** |
| **[morre]** | existe hoje e foi decidido remover |

**Premissa de leitura:** o negócio é **agenciamento e emissão de passagem fluvial**, e a plataforma
organiza **empresas** que fazem isso. Tudo abaixo é DDD aplicado a esse negócio: entidades com
identidade, um agregado com invariante própria (a Passagem), tipos fechados onde há regra, catálogo onde
há só rótulo, e referência por id onde há vida própria (ADR-0008).

---

## 1. Os dois contextos, e a linha entre eles

O domínio tem **dois contextos delimitados**, e quase toda confusão de modelagem deste projeto nasceu de
tratá-los como um só (ADR-0015 §8.1, ADR-0016 §2):

| Contexto | Pergunta que responde | Entidade central | Eixo de autorização |
|---|---|---|---|
| **Plataforma (sistema)** | quem acessa o app e o que compete a ele *no aplicativo* | `Usuario` | `Papel` — **fechado** |
| **Operação (negócio)** | quem é a pessoa na operação e o que ela faz *no negócio* | `Funcionario` | `Cargo` — **aberto** |

O elo entre eles é 1-1 e explícito: `Usuario.funcionarioId`. `ContextoUsuario` é o objeto que carrega os
dois lados juntos, porque a pergunta "o que este usuário pode fazer aqui" precisa dos dois.

**A linha entre os contextos é o que cada um cria** (ADR-0016 §2): a plataforma cria o **universo** —
quais cidades se ligam, quais lugares existem, quais empresas existem e em que atuam. A empresa cria a
**oferta** — qual rota sai de qual porto, em qual embarcação, a que preço, em que dias. O ponto de
contato é a **concessão** (§3.2).

Consequência que ainda não está no código: **`ADM`/`GESTOR` não emitem passagem** — eles não têm registro
na operação. Emitir passa a exigir **vínculo de funcionário** (`funcionarioId` não vazio), não papel
**[alvo]**. Hoje `PermissoesUsuario.podeCriarPassagem` devolve `true` para qualquer papel conhecido.

## 2. Mapa do domínio

O critério de colocação (ADR-0016 §4) é o que faz o resto se decidir sozinho:

| Natureza | Onde mora | Por quê |
|---|---|---|
| Sem dono (lugar, linha, rótulo) | raiz | é de todos |
| Com dono, referenciado **entre** partes | raiz + campo de dono | precisa de id resolvível por quem não é o dono |
| Com dono, referenciado só **dentro** da parte | subcoleção da parte | isolamento por caminho, regra mais barata |
| A parte, as pessoas, a emissão | raiz | atravessam tudo |

```
# PARTES — quem existe
empresas/{empresaId}                     nome, razaoSocial, cnpj, endereco, telefone1, telefone2
   ├── atuacoes/{ATUACAO}      [alvo]    um doc por atuação; o id É o nome da atuação
   └── rotas/{rotaId}          [alvo]    a oferta — só com atuação AGENCIAMENTO

# CAPACIDADES DA PLATAFORMA — sem dono
portos/{portoId}               [alvo]    nome, cidade
trechos/{trechoId}             [alvo]    cidadeOrigem, cidadeDestino
catalogo/{itemId}              [alvo]    categoria, descricao        (hoje: constants/{id})

# ATIVOS — dono por campo, endereçáveis globalmente
navios/{navioId}                         nome, capacidades, empresaId (+ tipoEmbarcacao [alvo])

# PESSOAS E EMISSÃO
users/{uid}                              papel, username, funcionarioId
funcionarios/{funcionarioId}             nome, email, cargo (+ empresaIds[] [alvo])
passagens/{passagemId}                   o agregado — intocado pelo ADR-0016
viagens/{viagemId}             [morre]   vira empresas/{id}/rotas/{rotaId}
```

**Parte, atuação e ativo** é o eixo que organiza tudo (ADR-0016 §4): a **parte** é a empresa (tem
identidade e CNPJ, existe por si); a **atuação** é o que ela faz num segmento (não é subtipo nem objeto
contido — uma parte exerce várias ao mesmo tempo e muda de conjunto no tempo, e é aí que herança
fracassa); o **ativo** é o que ela possui com identidade própria (o navio).

**Agência não é uma entidade** — é uma *atuação* que uma empresa exerce. `agenciaId` e `empresaId` são o
mesmo id, e isso não é coincidência de implementação.

## 3. Entidades e campos

### 3.1 Empresa — a parte

`domain/viagem/Empresa.kt` **[hoje]** · coleção `empresas/{id}`

| Campo | Tipo | Natureza | Notas |
|---|---|---|---|
| `id` | String | identidade | |
| `nome` | String | valor | nome fantasia; é o exibido |
| `razaoSocial` | String | valor | |
| `cnpj` | String | valor | identidade fiscal; **não validado hoje** |
| `endereco` | String | valor | |
| `telefone1`, `telefone2` | String | valor | |

Não tem campo de segmento nem de tipo: **o que a empresa faz vive nas atuações**, como documentos filhos.

### 3.2 Atuação e concessão — o que a parte faz **[alvo]**

Subcoleção `empresas/{empresaId}/atuacoes/{ATUACAO}` — **o id do documento é o nome da atuação**. Ser
documento (e não nome de coleção) é o que torna "em que a empresa atua" uma query comum: `listCollections()`
não existe no SDK Android, e a alternativa seria um campo `segmentos: []` denormalizado a manter em
sincronia.

| Atuação | Campos | Estado |
|---|---|---|
| `AGENCIAMENTO` | `trechoIds[]`, `portoIds[]`, `armadorIds[]` | ativa — é a que sustenta a rota |
| `TRANSPORTE` | — (é o armador; a frota é global, por `Navio.empresaId`) | ativa como qualificador |
| `PORTUARIA_OPERACAO` | `portoIds[]` | **dormente** — quem opera o cais |
| `PORTUARIA_ARRENDAMENTO` | `portoIds[]` | **dormente** — a arrendatária, quem faz check-in |

Os três arrays da atuação de agenciamento são a **concessão**: o recorte que a plataforma concede à
empresa. `armadorIds` aponta para empresas com atuação `TRANSPORTE` — a relação agência↔armador, que num
modelo relacional seria tabela de associação, aqui é o mesmo mecanismo de concessão que já governa trecho
e porto. **Uma peça, três usos.**

**Conceder não é cadastrar:** o form da atuação só *seleciona*; quem cria trecho e porto é o módulo deles.

### 3.3 Navio — o ativo

`domain/viagem/Navio.kt` **[hoje]** · coleção `navios/{id}`

| Campo | Tipo | Natureza | Notas |
|---|---|---|---|
| `id` | String | identidade | |
| `descricaoNome` | String | valor | nome do navio; renomeia para `nome` **[alvo]** |
| `capacidadeVeiculo` | Int | valor | |
| `capacidadeSuite2` | Int | valor | |
| `capacidadeSuite3` | Int | valor | |
| `capacidadeCamarote` | Int | valor | |
| `empresaId` | String | **referência** | dono (armador), por id desde o ADR-0008 F3 |
| `tipoEmbarcacao` | String | valor do catálogo | **[alvo]** — governa o que a rota pode vender (§4.8) |

**O navio fica na raiz**, não dentro da empresa: a agência vende passagem em navio que **não é dela**,
então é referenciado entre partes e precisa de endereço global. `IObjetoSimplificado` sai dele **[alvo]**
— um navio tem identidade, atributos e ciclo de vida; não é um par id/descrição.

### 3.4 Porto — capacidade da plataforma **[alvo]**

`portos/{portoId}`

| Campo | Tipo | Natureza | Notas |
|---|---|---|---|
| `id` | String | identidade | |
| `nome` | String | valor | |
| `cidade` | String | **valor do catálogo** (`MUNICIPIO`) | **não é id, de propósito** |

`cidade` por valor contraria o ADR-0008 conscientemente: o catálogo dá um **rótulo**, e rótulo é dado por
valor. Relacionar por id vale para o que tem vida própria e muda; o nome de um município não muda.

Porto é **lugar físico**, e lugar físico não pertence a quem navega — o cais de Manaus é o mesmo cais para
todas as empresas que atracam nele. O que a empresa tem no porto não é o porto: é a **atuação** nele.

### 3.5 Trecho — capacidade da plataforma **[alvo]**

`trechos/{trechoId}`

| Campo | Tipo | Natureza |
|---|---|---|
| `id` | String | identidade |
| `cidadeOrigem` | String | valor do catálogo (`MUNICIPIO`) |
| `cidadeDestino` | String | valor do catálogo (`MUNICIPIO`) |

O trecho é **só o par de cidades** — o que o mercado chama de linha (Manaus → Parintins). **Não tem data,
nem tarifa, nem porto, nem dono.** Só o painel cadastra, e é isso que faz dele bem comum de verdade em vez
de um cadastro que cada empresa refaz.

### 3.6 Rota — a oferta da agência **[alvo]** (hoje: `Viagem`)

`empresas/{empresaId}/rotas/{rotaId}`

| Campo | Tipo | Natureza | Notas |
|---|---|---|---|
| `id` | String | identidade | |
| `trechoId` | String | referência | qual par de cidades esta rota realiza |
| `navioId` | String | referência | qual embarcação opera — governa as tarifas (§4.8) |
| `embarquePortoId` | String | referência | por id simples |
| `desembarquePortoId` | String | referência | por id simples |
| `tarifas` | mapa | valor | a tabela do ADR-0013, **da empresa** (§3.7) |
| `agenda` | lista de `{diaSemana, hora}` | valor | dias em que opera e a hora de cada dia |

A rota é **como uma empresa realiza aquele par de cidades**: de qual porto sai, onde atraca, com qual
embarcação, a que preço e em que dias. **A tarifa é dela** — é por isso que mora na parte. Duas empresas
que vendem a mesma linha **compartilham o trecho** e têm **rotas próprias**.

**As viagens concretas não são persistidas:** as ocorrências da semana são **calculadas** a partir da
agenda. Não há coleção de ocorrências no MVP, e o custo é nomeado — a ocupação continua contada a partir
dos bilhetes, sem o O(1) de um contador. *Agenda é modelo; contador é otimização.*

O que existe hoje no lugar disso é a **`Viagem`** (`domain/viagem/Viagem.kt`), que nunca foi uma viagem:

| Campo | Tipo | Destino |
|---|---|---|
| `id` | String | permanece |
| `codigo` | String | derivado (origem/destino/empresa) — sem equivalente na Rota |
| `origem`, `destino` | String | viram `trechoId` + os dois portos |
| `empresaId` | String | vira o **caminho** (`empresas/{id}/rotas`) |
| `navioId` | String | permanece |

### 3.7 Tarifa — tabela de duas dimensões

`domain/viagem/TarifaViagem.kt` **[hoje]** (ADR-0013)

No Firestore é **mapa aninhado** no documento; no Room é **tabela-filha normalizada**, uma linha por
`(viagemId, chave)`, e o mapper achata mapa↔linhas. Com o ADR-0017 o lado Room desaparece e sobra só o
mapa.

| Campo | Tipo | Notas |
|---|---|---|
| `viagemId` | String | PK composta com `chave`; vira `rotaId` **[alvo]** |
| `chave` | String | chave tarifária canônica (§4.7) |
| `valor` | Double | tarifa da **inteira**; `Double` só na fronteira, `BigDecimal` scale 2 no cálculo |

A tabela tem dois eixos: **acomodação** (passageiro) × **classe de veículo**. **Moto é por regra**, não por
célula (§5.3). **Célula ausente é fail-closed**: sem tarifa tabelada, não emite (`ResultadoEmissao.SemTarifa`).

### 3.8 Catálogo — as informações adjuntas

`domain/cadastro/constantes/Constante.kt` **[hoje]** → `Catalogo` **[alvo]** · `constants/{id}` → `catalogo/{itemId}`

| Campo | Hoje | Alvo |
|---|---|---|
| `id` | String | String |
| `descricaoNome` | String | **`descricao`** — e o documento **já grava `descricao`**; `descricaoNome` só existe no Kotlin |
| `categoria` | String | String livre na fronteira, validada por quem consome |

É a tabela do que o negócio precisa **nomear mas não precisa modelar**: UF, município, tipo de documento,
tipo de veículo, acomodação, forma de pagamento, tipo de passagem, e — novos **[alvo]** —
`TIPO_EMBARCACAO` e `ATUACAO`.

**O critério que separa catálogo de tipo de domínio:** quem tem **regra** vira tipo (`StatusPassagem`,
`TipoPassagem`, `TipoGratuidade`); quem é **só rótulo** vira linha de catálogo. Manter os dois vocabulários
para a mesma coisa é a receita de divergirem — e foi o que aconteceu com `Constante.Descricao`, que ainda
lista `CORTESIA`, `A_EMITIR` e `EMITIDA` **[morre]**.

`IObjetoSimplificado` (`id` + `descricaoNome`) **fica exclusivo do catálogo** **[alvo]**: um item de
catálogo *é* um par id/descrição — é tudo que ele é. `Funcionario` e `Navio` deixam de implementá-la.

### 3.9 Funcionario — a pessoa na operação

`domain/operacoes/Funcionario.kt` **[hoje]** · `funcionarios/{id}`

| Campo | Tipo | Natureza | Notas |
|---|---|---|---|
| `id` | String | identidade | |
| `descricaoNome` | String | valor | **o nome da pessoa** (o `Usuario` não tem nome) → `nome` **[alvo]** |
| `cargo` | String (`Cargo`) | tipo fechado na fronteira | nasce `AGENTE`, o menor privilégio |
| `email` | String | valor | **chave de descoberta, uma vez só**: casa o pré-cadastro com a conta do Auth no primeiro acesso; depois o elo permanente é o id |
| `agencia` | String | valor | **[morre]** — a agência vira uma empresa, e a relação vira id |
| `lotacao` | String (`Lotacao`) | valor | **[morre]** |
| `empresaIds` | lista | referência | **[alvo]** — funcionário serve uma ou mais empresas |

O vínculo multi-empresa é a **assinatura do id da empresa no próprio documento**. Nas rodadas anteriores
era um par `{empresaId, agenciaId}`; com o §4 do ADR-0016 **o par colapsa**, porque os dois ids eram o
mesmo. `cargo` permanece **um só, da pessoa** — não por empresa: é a escolha mínima e mantém a política com
uma entrada de cargo em vez de N.

**Consequência ainda aberta:** com dois vínculos, a emissão precisa saber **sob qual vínculo emite** — hoje
a agência do bilhete vem do emissor, o que só é resposta única porque o funcionário tem uma agência.

### 3.10 Usuario — quem acessa

`domain/operacoes/Usuario.kt` **[hoje]** · `users/{uid}`

| Campo | Tipo | Natureza | Notas |
|---|---|---|---|
| `id` | String | identidade | é o `uid` do Firebase Auth |
| `email` | String | valor | |
| `username` | String | valor | **credencial, não identidade civil**: entrar por username é resolver `username → e-mail` antes do `signIn`, o que exige unicidade |
| `papel` | String (`Papel`) | tipo fechado na fronteira | |
| `funcionarioId` | String | **referência 1-1** | vazio para papel puro de plataforma |
| `ultimoUsuarioLogado` | Boolean | **estado local** | não é dado de negócio → vai para o DataStore (ADR-0017 D4) |

O que **não** mora aqui, e por quê: `nome` (é a pessoa, logo é do `Funcionario`), `agencia`/`lotacao`
(são onde a pessoa atua) e o `cargo` de negócio. Este documento não sabe nada sobre a operação; sabe quem
entrou.

`ContextoUsuario` (`usuario` + `funcionario?`) é o objeto que junta os dois lados. `funcionario == null` é
estado **válido**, não erro: ali quem decide é o papel.

### 3.11 Passagem — o agregado

`domain/passagem/Passagem.kt` **[hoje]** · `passagens/{id}` · detalhado em [dominio-passagem.md](dominio-passagem.md)

É o **único agregado** do app — o fato transacional que consome e **congela** o master data. As demais
entidades são dados de referência; a Passagem é o evento. ~50 campos planos, agrupados por natureza:

| Grupo | Campos | Natureza |
|---|---|---|
| Identidade | `id`, `numero` | o `numero` vem do `ContadorBilhete` |
| Referências vivas | `viagemId`, `navioId`, `empresaId`, `funcionarioId` | ids estáveis (ADR-0008) |
| **Snapshot** | `codigoViagem`, `empresa`, `navio`, `origem`, `destino`, `dataViagem`, `horaViagem`, `agencia`, `funcionarioResponsavel` | **congelados na emissão** — histórico imutável |
| Dinheiro | `valorPix`, `valorDinheiro`, `valorDebito`, `valorCredito`, `tarifaBase` | `Double?` na fronteira |
| Categoria tarifária | `tipoPassagem`, `gratuidade`, `acomodacao` | Strings de tipos fechados |
| Participantes | `nome/documento/numeroDocumento/dataNascimento` × **3 passageiros** | passageiro 1 = titular |
| Veículo | `tipoVeiculo`, `modeloVeiculo`, `placaVeiculo`, `corVeiculo`, `cilindrada` + responsável pela retirada (3 campos) | participante de mesmo nível |
| Ciclo de vida | `status`, `embarcadaPorId`, `embarcadaPor`, `embarcadaEm` | FSM do ADR-0012 |
| Derivados | `temPassageiro2`, `temPassageiro3`, `ehVeiculo` | calculados, não persistidos |

**A dualidade id × snapshot é decisão, não descuido** (ADR-0008): o id serve para agregar e navegar; o
snapshot serve para o bilhete **não mudar** quando a viagem for renomeada. Um bilhete emitido é um fato
histórico.

**Passageiro e veículo são participantes de mesmo nível** — não value objects descritivos. O veículo viaja
sob um responsável pela retirada que é **opcional**: o modelo **tolera** veículo sem responsável, e essa
inconsistência é aceita, não barrada.

### 3.12 Entidades de suporte

| Entidade | Campos | Papel |
|---|---|---|
| `ContadorBilhete` | `id = 1` (linha única), `contagem` | serviço de numeração; documento único no Firestore |
| `PassagemDigital` | `id`, `idPassagem`, `caminho` | **[morre]** — o arquivo vai para a galeria e o índice local acaba (ADR-0017 D5) |
| `RascunhoPassagemSnapshot` | ~30 campos de valor | rascunho do form (ADR-0004), **local e não-autoritativo**; só valores, sem lambdas, flags ou listas |

## 4. Tipos fechados — os enums do domínio

A convenção é uniforme e vale para todos: **String só na fronteira**. `de()` converte na leitura
(tolerante à grafia legada: normaliza espaços→underscore e caixa), `name` é o valor **canônico** gravado,
`rotulo()` formata para exibição. **Valor desconhecido → `null` → sem permissão / sem regra
(fail-closed).**

### 4.1 `Usuario.Papel` — o eixo fechado **[hoje]**

| Valor | Significado |
|---|---|
| `ADM` | administra a plataforma |
| `GESTOR` | opera a plataforma |
| `OPERADOR` | **o coringa e o elo** — todo usuário que não é ADM/GESTOR é OPERADOR, e é ele que corresponde a um funcionário |

Três papéis, e a tendência é continuar três. Quem cresce é o cargo.

**[alvo]** O ADR-0017 §7.1 acrescenta a primeira separação entre `ADM` e `GESTOR`: o CRUD do catálogo é só
do `ADM`. Hoje a política trata os dois como um bloco (`ehPapelPlataforma`).

### 4.2 `Funcionario.Cargo` — o eixo aberto **[hoje]**

| Valor | Significado |
|---|---|
| `SUPERVISOR` | responde pela operação onde atua; monta rotas **[alvo]**; edita qualquer passagem |
| `AGENTE` | emite passagem; edita as próprias |

Cresce sem tocar no `Papel`: amanhã quem faz check-in, quem valida embarque, quem responde por um navio.
**Sem default na fronteira, de propósito.**

### 4.3 `Agencia` e `Funcionario.Lotacao` — **[morre]**

`Agencia { AUTONOMO, MATRIZ }` e `Lotacao { PORTO_NORTE, ILHA_CENTRAL, PORTO_SUL }` são enums de conjunto
fixo — um deles **admitindo no próprio comentário** que viraria coleção cadastrável. Morrem no ADR-0016
§6: a agência passa a ser uma empresa e a relação vira id.

### 4.4 `StatusPassagem` — a FSM do bilhete **[hoje]** (ADR-0012)

```
A_EMITIR ──> EMITIDA ──> EMBARCADA (terminal)
```

| Valor | Transições possíveis |
|---|---|
| `A_EMITIR` | → `EMITIDA` |
| `EMITIDA` | → `EMBARCADA` |
| `EMBARCADA` | **nenhuma** — embarque é irreversível |

É a máquina que dá semântica à confirmação por QR: **não embarca bilhete não emitido, não reembarca o já
usado**. Transição ilegal é recusada no domínio *e* na regra do Firestore (ADR-0011).

### 4.5 `TipoPassagem` — o tipo tarifário **[hoje]** (ADR-0013)

| Valor | Tarifa devida a partir da `tarifaBase` |
|---|---|
| `INTEIRA` | a base |
| `MEIA` | **metade da tabelada** |
| `GRATUIDADE` | **zero** |

Cada tipo **sabe derivar a própria tarifa** — é comportamento no enum, não `when` espalhado. Meia e
gratuidade são **categorias**, não desconto: só o que se abre **abaixo** da tarifa devida é desconto (§5.2).

### 4.6 `TipoGratuidade` — os subtipos legais **[hoje]** (ADR-0013)

| Valor | Rótulo |
|---|---|
| `IDOSO` | Idoso |
| `PCD` | PcD |
| `CRIANCA_ATE_5` | Criança até 5 anos (faixa 0–5, **inclui o 5**) |
| `PASSE_FEDERAL` | Passe Federal |

`CORTESIA` foi **aposentada**: era redução comercial, não gratuidade — cabe como desconto.

### 4.7 Chave tarifária — os dois eixos da tabela **[hoje]** (ADR-0013)

Não é um enum no código; é o vocabulário canônico da `TarifaViagem.chave`:

| Eixo | Valores canônicos |
|---|---|
| Acomodação (passageiro) | `REDE`, `SUITE`, `CAMAROTE` |
| Classe de veículo | `CARRO`, `CAMINHAO`, `CARRETA` |
| Moto | **não tem célula** — é regra sobre a cilindrada (§5.3) |

*Candidato natural a virar tipo fechado quando a tabela for reescrita para a Rota.*

### 4.8 `TipoEmbarcacao` — catálogo **com regra** **[alvo]** (ADR-0016 §8)

| Tipo | Passageiros | Carro / Moto | Caminhão / Carreta |
|---|---|---|---|
| **F/B** (Ferry Boat, é balsa) | sim | sim | **sim** |
| **Navio** | sim | sim (limitado) | não |
| **Lancha** | sim | não | não |

**A exceção nomeada ao critério do §3.8:** o tipo de embarcação é catálogo **e** tem comportamento. A
resolução: o **catálogo guarda a lista** (a gestão acrescenta "Catamarã" sem deploy) e a **capacidade é
código**, chaveada pelo valor canônico. A consequência é desconfortável e é a certa: **um tipo novo no
catálogo não ganha capacidade de veículo até o código dizer** — nasce só-passageiro. Fail-closed, e
preferível ao contrário, que seria um tipo novo levando carreta por omissão.

A regra governa duas telas, e nas duas o efeito é **não oferecer o impossível**: no cadastro da rota, a
tabela de tarifa só oferece as células admissíveis; na emissão, o form não oferece veículo que a
embarcação não leva. Mata na origem uma classe inteira de erro que seria validação tardia.

Os **limites** ("navio leva carro de forma limitada") ficam para depois — quando entrarem, a capacidade
deixa de ser conjunto e vira **quantidade**, e aí quer ser dado no documento, não código.

### 4.9 `Atuacao` — os segmentos **[alvo]** (ADR-0016 §4)

`AGENCIAMENTO` · `TRANSPORTE` · `PORTUARIA_OPERACAO` · `PORTUARIA_ARRENDAMENTO`

As duas portuárias **nascem dormentes** — valores reservados, sem UI e sem dado. É o padrão "dormente" do
ADR-0003 aplicado a um segmento inteiro, e custa quase nada justamente porque são **valores**, não
estrutura: não há coleção vazia a criar. O destino já tem nome: **é onde o check-in vai morar**.

**`fluvial` não aparece aqui de propósito:** fluvial é *modal*, não segmento. O segmento é `TRANSPORTE`.
Batizar a estrutura de "fluvial" comprometeria o domínio com um eixo que a plataforma **não abre**
(multi-empresa e multi-segmento sim, **multi-modal não**).

### 4.10 `SecaoMenu` — as seções **[hoje]**, duas famílias **[alvo]**

Hoje: `PASSAGEM`, `VIAGEM`, `EQUIPE`, `EMPRESA`, `NAVIO`.

**[alvo]** passa a ter duas famílias (ADR-0016 §2): **operação** = `PASSAGEM`, `ROTA`; **painel** =
`EMPRESA`, `NAVIO`, `PORTO`, `TRECHO`, `CATALOGO`; `EQUIPE` aparece nos dois — é a única seção que olha os
dois eixos, porque o supervisor gere os membros de onde atua. **`VIAGEM` sai**: o nome estava errado desde
o começo.

### 4.11 Tipos-resultado (sealed) — o desfecho como dado **[hoje]**

| Tipo | Casos | Para quê |
|---|---|---|
| `ResultadoEmissao` | `Ok` · `SemTarifa` · `CotaGratuidadeAtingida(categoria)` | guardas de emissão, fail-closed: só `Ok` libera o salvamento |
| `ResultadoEmbarque` | `Confirmada(passagem)` · `JaEmbarcada(por, em)` · `NaoEmitida` · `NaoEncontrada` | desfecho da leitura do QR |
| `EscopoAgencia` | `Todas` · `Apenas(agencia)` · `Nenhuma` | recorte de listagem |

`EscopoAgencia` merece a nota que está no próprio código: existe **como tipo**, e não como String vazia
significando "sem filtro", porque **"não filtra nada" e "não tem agência nenhuma" pareceriam iguais** — e
o segundo abriria a listagem inteira para quem não deveria ver nada. **[alvo]** o recorte passa a ser por
**empresa**, não por String de agência.

## 5. Regras de negócio puras

O que o domínio sabe sozinho, sem device, sem rede e sem Firebase — e portanto o que é testável em JVM:

### 5.1 Autorização — política única de dois eixos (ADR-0010/0015)

`PermissoesUsuario` responde por **seção** (o que aparece no menu) e por **ação** (o que se pode fazer),
com **posse** onde faz sentido (`ehDono`). Toma `(papel, cargo)` e nunca compara `.name` solto: converte
para enum na fronteira, e desconhecido é sem permissão.

Regra que vale registrar: **validar embarque ≠ editar o bilhete**. Qualquer papel conhecido pode confirmar
embarque — é ação de doca, mesmo para quem não vendeu.

### 5.2 Tarifa e desconto (ADR-0013)

- **A tarifa tabelada é a referência**, e o desconto é o **resíduo** medido contra ela — não o contrário.
- `descontoDerivado = max(0, tarifaDevida − valorCobrado)`. Piso em zero: cobrar acima da devida não vira
  desconto negativo.
- Dinheiro em `BigDecimal` **scale 2 / RoundingMode.UP** no cálculo; `Double` só na fronteira.
- **Cota de gratuidade:** 2 por categoria por viagem, contada no Firestore.

### 5.3 Moto — tarifa por regra, não por célula (ADR-0013)

`floor(cc / 100) * 100`, 1:1 em reais: 125cc→100, 250cc→200, 300cc→300. **Abaixo de 100cc dá zero**, que é
consequência conhecida do piso. É regra provisória, substituível por célula tabelada.

### 5.4 Coerência da rota — duas camadas **[alvo]** (ADR-0016 §7)

1. **Geográfica:** o porto de embarque tem que estar na `cidadeOrigem` do trecho, e o de desembarque na
   `cidadeDestino`. Como o porto guarda a cidade, a checagem é local e barata — e é ela que impede uma rota
   de dizer que vai a Parintins atracando em Manaus.
2. **De concessão:** `trechoId` e os dois portos têm que estar nas capacidades da atuação, e **o dono do
   navio** tem que estar em `armadorIds`. Sem isso o recorte concedido seria decorativo: bastaria digitar
   um id de fora.

A segunda tem que valer **também no servidor**, e tem uma característica que as outras não têm: é
**indireta**. A rota guarda `navioId`, não o armador — descobrir o dono exige ler o navio. Na UI é um
lookup a mais; na regra do Firestore, um `get()` a mais por escrita.

### 5.5 Ciclo de vida do bilhete (ADR-0012)

A FSM do §4.4, com o embarque irreversível e a idempotência do QR (`JaEmbarcada` carrega quem e quando).

## 6. Onde o domínio ainda não é domínio

O pacote acabou de ser renomeado de `model` para `domain`, e o nome é uma promessa que o conteúdo ainda
não cumpre. Em Clean Architecture a regra é uma só — **o domínio não depende de nada** —, e hoje ele
depende de duas coisas. São 31 arquivos em `domain/`:

| Violação | Onde | Tamanho | Como sai |
|---|---|---|---|
| **`androidx.room` dentro da entidade** (`@Entity`, `@PrimaryKey`, `@Index`, `@Ignore`) | 10 arquivos: `Passagem`, `Usuario`, `Funcionario`, `Empresa`, `Navio`, `Viagem`, `TarifaViagem`, `Constante`, `ContadorBilhete`, `PassagemDigital` | o agregado inteiro | **ADR-0017** — as anotações somem com o Room. `Passagem.temPassageiro2` é **regra de negócio carregando um `@Ignore`** |
| **`import services.repository.firebase.documents`** — a entidade conhece o DTO do Firestore | 4 arquivos: `Empresa`, `Navio`, `Viagem`, `Funcionario` (funções `toDocumento()`) | 4 funções | mover o mapper para a camada de repositório: quem traduz para o Firestore é a infraestrutura, não a entidade |
| **`import services.repository.*Repository`** — mapper de domínio injetando repositório | 3 arquivos em `domain/mappers/` | 3 classes | são **casos de uso**, não domínio: pertencem a uma camada de aplicação |
| **`domain/screendata/`** — projeções de tela dentro do domínio | 6 arquivos (`DadosPassagem`, `DadosViagemCard`, `SecaoMenu`, …) | — | são modelos de apresentação; a exceção defensável é `SecaoMenu`, que carrega política |

**A direção da dependência está invertida nos dois primeiros casos**, e o segundo é o mais barato de
corrigir — são quatro funções de extensão que só precisam mudar de arquivo. O primeiro já tem ADR e plano.

Nada disso impede o app de funcionar; o que impede é o domínio **ser reutilizável e testável sozinho** —
que é o ponto de separá-lo.

## 7. O que muda quando os ADRs forem implementados

| Hoje | Alvo | ADR |
|---|---|---|
| `Constante` (`constants`) | `Catalogo` (`catalogo`), sem enums internos | 0016 §3 |
| `Viagem` (raiz) | `Trecho` (raiz) + `Rota` (`empresas/{id}/rotas`) | 0016 §7 |
| — | `Porto` (raiz) | 0016 §5 |
| — | `empresas/{id}/atuacoes/{ATUACAO}` + concessão | 0016 §4 |
| `Funcionario.agencia` + `lotacao` | `Funcionario.empresaIds[]` | 0016 §6 |
| `Navio` sem tipo | `Navio.tipoEmbarcacao` governando as tarifas | 0016 §8 |
| `ADM`/`GESTOR` emitem passagem | emitir exige `funcionarioId`; painel exige papel | 0016 §2 |
| Entidades anotadas com Room | entidades puras; cache do SDK | 0017 |
| `PassagemDigital` (tabela) | arquivo na galeria, nome derivado do `idPassagem` | 0017 D5 |
| `Usuario.ultimoUsuarioLogado` | DataStore | 0017 D4 |
| Seed escreve o dado | painel administrativo escreve o dado | 0016 §1 |

## 8. Perguntas em aberto

Herdadas dos ADRs, e que o domínio não resolve sozinho:

1. **Sob qual vínculo se emite** quando o funcionário serve duas empresas (§3.9) — escolha no login ou na
   emissão?
2. **Remoção de trecho/porto/armador concedido** que rotas já referenciam: invalida rota e concessão. O MVP
   assume que não remove.
3. **`cargo` por pessoa ou por vínculo** — hoje é por pessoa, e o ADR-0016 assume que continua.
4. **Onde mora a validação de coerência da rota** (§5.4) no código: função pura no domínio, como a tarifa,
   ou caso de uso? *Inclinação: função pura no domínio — ela só precisa dos dados, não de repositório.*
5. **A chave tarifária vira tipo fechado?** (§4.7) Hoje é String canônica por convenção, o que contraria o
   critério do §3.8: ela **tem regra** (define eixo e admissibilidade), logo deveria ser tipo.