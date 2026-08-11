# Desenho de domínio — a plataforma inteira (entidades, campos, enums e regras)

**Status:** Documento de referência do domínio. Consolida o que está **no código hoje** com o que os
ADR-0015, [ADR-0016](../adr/0016-dominio-da-plataforma.md) e
[ADR-0017](../adr/0017-eixo-de-storage-firestore-only.md) **decidiram e ainda não foi construído**.
Ancorado no código em `2026-07-31`, depois do pacote `model` virar **`domain`**.

> ✅ **Rodada de decisões em 2026-07-31 — já incorporada ao ADR-0016 como 7ª rodada.** Os dois documentos
> estão alinhados; aqui fica o detalhe de campos e enums, lá a decisão e o porquê. Os quatro pontos:
> (a) a **lei é o domínio**, entidade pode conter
> entidade e a entity do Room vira **DTO** (§1.1), com o critério embutir/referenciar/congelar que a teoria
> impõe (§1.2); (b) **`Trecho` deixa de existir** — o par de cidades sai de Porto e Rota (§3.5); (c) nasce
> **`Localidade`** (UF + município, dois `Catalogo` embutidos), que é a dimensão do eixo analítico (§3.4);
> (d) **`Porto` tem `localidade`**, não `cidade: String`. Os pontos afetados do ADR-0016 são §5, §7 e o
> plano F4/F7.

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
| **Operação (negócio)** | quem é a pessoa na operação e o que ela faz *no negócio* | `Funcionario` | `Cargo` — **aberto, dentro de uma atuação** (§4.2) |

O elo entre eles é 1-1 e explícito: `Usuario.funcionarioId`. `ContextoUsuario` é o objeto que carrega os
dois lados juntos, porque a pergunta "o que este usuário pode fazer aqui" precisa dos dois.

**A linha entre os contextos é o que cada um cria** (ADR-0016 §2): a plataforma cria o **universo** —
quais cidades se ligam, quais lugares existem, quais empresas existem e em que atuam. A empresa cria a
**oferta** — qual rota sai de qual porto, em qual embarcação, a que preço, em que dias. O ponto de
contato é a **concessão** (§3.2).

Consequência que ainda não está no código: **`ADM`/`GESTOR` não emitem passagem** — eles não têm registro
na operação. Emitir passa a exigir **vínculo de funcionário** (`funcionarioId` não vazio), não papel
**[alvo]**. Hoje `PermissoesUsuario.podeCriarPassagem` devolve `true` para qualquer papel conhecido.

## 1.1 A lei é o domínio (decisão do analista, 2026-07-31)

Duas regras de camada que governam todo o resto deste documento:

**1. Uma entidade pode ter outra entidade como atributo, e o domínio decide isso sozinho.** Se `Porto` tem
uma cidade e cidade é item de `Catalogo`, então `Porto.cidade` é do tipo `Catalogo` — não uma String que
por acaso veio de lá. Como isso vira documento no Firestore (embutido, referenciado por id, denormalizado)
é **problema de outra camada**, resolvido caso a caso pelo que o Firestore faz melhor. O domínio não se
deforma para caber no banco; o banco se organiza para servir o domínio.

> Isto **revisa** o ADR-0016 §5, que gravava `cidade` como String por valor justificando que "rótulo é dado
> por valor". A justificativa era de persistência, e persistência não decide forma de domínio. O que ela
> continua decidindo — e legitimamente — é **como** aquele `Catalogo` aterrissa no documento.

**2. A entity do Room vira DTO, e o DTO é a figura central entre as camadas.** A classe plana que hoje
carrega `@Entity` deixa de ser "a forma do banco" e passa a ser **a forma de trânsito**: é o que atravessa
datasource → repositório → caso de uso → UI, porque o dado será requisitado e usado entre elas. O ADR-0003
já chamava o modelo de "DTO-cêntrico"; aqui isso se completa — o DTO perde o vínculo com o Room (que sai
pelo ADR-0017) e ganha o papel que já exercia de fato.

Ficam **três formas**, com fronteiras nomeadas, e é isso que impede uma de contaminar a outra:

| Forma | Onde vive | Quem a conhece | Característica |
|---|---|---|---|
| **Entidade de domínio** | `domain/` | ninguém acima; todo o resto abaixo | rica e **composta** (pode conter outra entidade), sem anotação de framework |
| **DTO** | camada de dados | datasource, repositório, caso de uso, UI | **plano**, transportável, comparável, serializável |
| **Documento** | Firestore | só o repositório/mapper | a forma que o Firestore serve melhor — embutida ou por referência |

A tradução entre elas é trabalho de **mapper**, e mapper é infraestrutura. Nenhuma das três é "a
verdadeira": a entidade é a **lei**, o DTO é o **trânsito**, o documento é o **armazenamento**.

## 1.2 Onde o modelo de documentos aperta — e o critério de embutir, referenciar ou congelar

O §1.1 libera o domínio para compor entidades, o que levanta a pergunta certa: *isto não está ficando
estruturado demais para NoSQL?* A teoria responde que **o critério não é quão estruturado, é quão
conectado**.

Kleppmann (*DDIA*, cap. 2) situa a fraqueza do modelo de documentos em **muitos-para-muitos e junção** —
não em profundidade nem em rigor de esquema. Bancos de documentos são o modelo hierárquico revivido, e o
que derrubou o hierárquico foi exatamente N:N. Fowler & Sadalage dizem o mesmo pelo outro lado: são bancos
**orientados a agregado**, ótimos em "me dá esse agregado inteiro por chave" e ruins em perguntas que
atravessam agregados. Domínio profundo é de graça no Firestore; ele não cobra por riqueza de modelo, cobra
por **forma de consulta**.

**Os três sinais de alerta, testados neste modelo:**

| Sinal | Neste domínio | Veredito |
|---|---|---|
| **Muitos-para-muitos** | três: `Funcionario ↔ Empresa` (`empresaIds[]`), `Agência ↔ Navio` (`navioIds[]`), `Empresa ↔ Porto` (`portoIds[]`) | **passa** — os arrays são pequenos por natureza e a consulta é de um lado só; o outro lado sai com `array-contains`. Aperta em "empresas que têm o porto X **e** o Y": uma cláusula `array-contains` por consulta, logo duas consultas + interseção no cliente |
| **Junção de 3+ entidades** | ~~uma: rota → navio → empresa dona → `armadorIds`~~ | **nenhuma** — a concessão por navio (§3.2) tornou a checagem direta e eliminou o único caso |
| **Agregação e consulta ad-hoc sobre eventos** | balanço, contagem e faturamento sobre `passagens` | **é aqui que aperta.** O Firestore tem `count()`/`sum()`/`average()`, mas **não tem `GROUP BY`**: "faturamento por agência por dia" não é uma consulta, é uma por grupo |

**A conclusão é que a pressão não vem da estrutura — vem de dois regimes de carga no mesmo banco.** A
emissão é OLTP (escrita pontual, leitura por chave, offline, alto volume) e o Firestore a serve muito bem;
o relatório é analítico (agregação por múltiplos eixos sobre o histórico) e ele o serve mal **por
desenho**. Os cadastros — empresa, localidade, porto, navio, catálogo, rota — são **dado de referência**:
pequeno, lido muito, escrito pouco, relações rasas. É o caso de uso favorito do modelo de documentos, e
podem ficar estruturados à vontade.

Isso converge com a decisão de plataforma já tomada (ADR-0017): quando o eixo analítico pesar, a resposta é
**um segundo lugar para o dado analítico** — o back-end centralizador —, não desmontar o modelo de
documentos que serve bem a operação. Em vocabulário de Kleppmann, o Firestore segue como **sistema de
registro** e o analítico é **dado derivado**, mantido a partir dele.

### O critério, que é a guarda que o §1.1 precisa

Vernon (*Effective Aggregate Design*, regra 3) é explícito: **agregados referenciam outros agregados por
identidade, nunca por contenção** — e isso vale para o agregado *persistido*. Sem esta guarda escrita, o
§1.1 autorizaria aninhar agregado dentro de agregado no documento, que é o caminho mais curto para o
problema real do modelo: **duplicata viva a manter em N lugares**.

| Fazer | Quando | Exemplo aqui |
|---|---|---|
| **Embutir** | é *value object* ou rótulo: pequeno, estável, sempre lido junto, sem vida própria | os dois `Catalogo` dentro de `Localidade` (§3.4) |
| **Referenciar por id** | é agregado com ciclo de vida próprio | o `Porto` visto pela `Rota`; a `Empresa` vista pelo `Navio` |
| **Congelar (duplicar de propósito)** | a cópia é **imutável por desenho** *e a imutabilidade não vem do modelo* | **nenhum, no domínio** — era o snapshot da `Passagem` (ADR-0008), e ele saiu (ADR-0023 D8) |

O terceiro caso é o único em que denormalizar **não tem custo de manutenção** — porque a cópia *não deve* ser
atualizada. Era o que a `Passagem` fazia: congelava empresa, embarcação, origem e destino porque um bilhete
emitido é fato histórico.

**Ele deixou de ter exemplo no domínio em 2026-08-11** (ADR-0023 D8), e a razão instrui o critério: o
congelamento protegia contra *rename do dado de referência*, e Rota e Viagem **não têm editar** desde a
F7/F8 — são imutáveis por desenho, e Localidade e Porto têm delete lógico. Quando a **fonte** já é imutável,
a cópia não protege de nada e ainda cria o par que pode discordar de si mesmo.

Então o terceiro regime continua válido como **regra**, e passa a ter uma condição explícita: **congela-se
quando a fonte pode mudar e a leitura não pode** — e essa avaliação é da **camada de dados**, com o caso
concreto, não do domínio.

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
localidades/{localidadeId}     [alvo]    uf (Catalogo), municipio (Catalogo)   ← embutidos
portos/{portoId}               [alvo]    nome, localidade                      ← referência
catalogo/{itemId}              [alvo]    categoria, descricao        (hoje: constants/{id})
rotas/{rotaId}                 [alvo]    portoOrigem, portoDestino, distanciaMn, tempoMedioH
viagens/{viagemId}             [alvo]    rotaId, navioId, diaSemana, hora   ← ATÔMICA
   ↑ as duas: criadoPor, criadoEm, ativo — compartilhadas, imutáveis, sem exclusão (§3.6)

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
| `AGENCIAMENTO` | `portoIds[]`, `navioIds[]` (concessão) + `rotasNegadas[]`, `viagensNegadas[]` (preferência) | ativa (*`trechoIds[]` e `armadorIds[]` saíram — §3.5 e §3.3*) |
| `TRANSPORTE` | — (dona da frota; **cadastra os próprios navios**, §3.3) | ativa — qualifica quem pode ter navio |
| `PORTUARIA_OPERACAO` | `portoIds[]` | **dormente** — quem opera o cais |
| `PORTUARIA_ARRENDAMENTO` | `portoIds[]` | **dormente** — a arrendatária, quem faz check-in |

Os arrays da atuação de agenciamento são a **concessão**: o recorte que a plataforma concede à empresa —
**onde** (portos) e **em quê** (navios). É o mesmo mecanismo aplicado a duas capacidades: **uma peça, dois
usos.**

*Revisão de 2026-07-31: a segunda dimensão era `armadorIds` — concessão por **empresa transportadora**,
herdando a frota inteira dela. Passou a ser **por navio**, que é o que a agência de fato vende. "Armador"
fica como vocabulário de papel (a empresa com atuação `TRANSPORTE`, dona da embarcação), não como campo.*

Duas consequências da troca, e a primeira é a que importa:

- **A checagem da rota deixa de ser indireta.** Com `armadorIds`, conferir a concessão exigia ler o navio
  (`navios/{id}.empresaId`) para só então comparar — um `get()` a mais por escrita. Com `navioIds` é
  `rota.navioId ∈ atuacao.navioIds`, comparação direta: **desaparece a única junção de três saltos do
  domínio** (§1.2).
- **Frota nova nasce não-concedida.** O armador compra um navio e a agência que o representa não vende nele
  até a plataforma conceder. Com `armadorIds` era o contrário — a frota nova entrava sozinha. É fail-closed,
  e troca conveniência por controle explícito.

A **linha** que a empresa pode vender continua não sendo concedida diretamente: é **consequência dos portos
que ela recebeu** — quem tem os portos de Manaus e de Parintins pode montar a rota entre eles, e quem não
tem, não pode.

**Conceder não é cadastrar:** o form da atuação só *seleciona*; quem cria porto é o módulo dele.

**Concessão e negada não são a mesma coisa e não podem compartilhar mecanismo** *(2026-07-31)*:

| | O que é | Direção | Onde vale |
|---|---|---|---|
| **Concessão** (`portoIds`, `navioIds`) | **segurança** — o que a agência pode **vender** | *allow-list*, **fail-closed** | servidor (regra) |
| **Negadas** (`rotasNegadas`, `viagensNegadas`) | **conforto** — o que ela escolhe não **ver** | *deny-list*, **fail-open** | tela |

**Visualização = pool − negadas; venda = concessão.** Filtrar a visualização pela concessão faria a agência
nova ver tela vazia, e o ganho do pool compartilhado (§3.6) se perderia. Tratar a negada como autorização
inverteria o default de um sistema que é fail-closed em todo o resto.

*Escala:* a deny-list funciona enquanto o pool é pequeno; com centenas de viagens, o movimento natural é
inverter para adoção explícita ("as minhas rotas").

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

**Quem cadastra: a plataforma OU a empresa dona** **[alvo]** *(decisão de 2026-07-31)*. No ato do cadastro o
navio **pertence a uma empresa existente com atuação `TRANSPORTE`** — não há navio sem dono.

É o **único caso de uma parte escrevendo numa coleção da raiz**, e ele revela que "capacidade da
plataforma" vinha misturando dois eixos independentes:

| | **cadastrado pela plataforma** | **cadastrado pela parte** |
|---|---|---|
| **sem dono** | catálogo, localidade, porto | — (e é certo que esteja vazio) |
| **com dono** | navio *(também)* | navio, rota |

O navio é **ativo e capacidade da plataforma ao mesmo tempo**, e não há contradição: os dois eixos
respondem perguntas diferentes — *de quem é* e *quem pode criar*.

*Por que deixar a empresa cadastrar a própria frota não abre brecha:* com a concessão por **navio** (§3.2),
cadastrar um navio **não concede nada** — quem decide em quais embarcações uma agência vende é a
plataforma. Enquanto a concessão era por armador, a empresa cadastrando a própria frota auto-atestaria
exatamente o fato que a regra conferia; conceder por navio **removeu a razão de restringir**.

### 3.4 Localidade e Porto — as âncoras geográficas **[alvo]** · *revisado em 2026-07-31*

**`localidades/{localidadeId}`** — capacidade da plataforma, sem dono.

| Campo | Tipo | Natureza | Notas |
|---|---|---|---|
| `id` | String | identidade | opaco — a identidade não se amarra a cadastro de terceiro |
| `uf` | **`Catalogo`** | **value object embutido** | embute `id` + `descricao` (`"PA"`); `categoria` não embute — o nome do campo já diz |
| `municipio` | **`Catalogo`** | **value object embutido** | embute `id` + `descricao` (`"Belém"`) |
| `codigoIbge` | String? | **chave natural** | opcional no cadastro, **único quando presente** |
| `ativo` | Boolean | estado | desativar em vez de remover |

A `Localidade` é o **par UF + município como uma coisa só**, e ela existe por três razões que se somam:

1. **É a aplicação exata do critério do §1.2.** Item de catálogo é *value object de referência* — pequeno,
   estável, sempre lido junto, sem vida própria. Logo **embute**, e é o único caso da tabela em que embutir
   é a resposta certa.
2. **É a dimensão do eixo analítico.** Em modelagem dimensional, `Localidade` é uma **dimensão**, e é o que
   torna possível perguntar "passagens por UF" ou "por município" sem agrupar String solta. Com `cidade`
   como texto no porto, a UF não existia em lugar nenhum e o agrupamento seria por igualdade de rótulo —
   frágil e sem hierarquia. Isto é o que prepara o terreno para o back-end analítico do §1.2.
3. **Enriquece a exibição sem consulta extra.** "Porto de Val-de-Cães — Belém/PA" sai de uma leitura só,
   porque UF e município vieram embutidos.

**Invariantes que nascem com ela:** o par `(uf, municipio)` é **único**, e o `codigoIbge` é **único quando
presente**. Não é preciosismo — é dimensão de análise: duas localidades para o mesmo município fragmentam
todo relatório que agrupe por elas, e o erro só aparece depois, no número errado. São as primeiras regras
de unicidade do domínio, e valem no cadastro **e** na regra do servidor.

**Por que o `codigoIbge`:** é a **chave natural** do município. Resolve a unicidade de graça e — o que pesa
mais — é o que permite cruzar esta dimensão com **dado externo** (censo, malha, tarifa regulada) quando o
eixo analítico existir. Não é o id do documento, é campo: o id continua opaco para não amarrar a identidade
a um cadastro de terceiro. Opcional no cadastro, porque exigi-lo poria fricção no painel para quem não tem o
número à mão — e o preço, escrito: localidade sem código não cruza com fonte externa até alguém preenchê-lo.

**`portos/{portoId}`**

| Campo | Tipo | Natureza | Notas |
|---|---|---|---|
| `id` | String | identidade | |
| `nome` | String | valor | **único dentro da localidade** — dois "Porto Central" em Belém são o mesmo problema um nível abaixo |
| `localidade` | **`Localidade`** | **referência a agregado** | substitui o antigo `cidade` |
| `ativo` | Boolean | estado | desativar em vez de remover |

`Porto.localidade` é **referência**, não embutido — pelo critério do §1.2, `Localidade` tem coleção,
identidade e ciclo de vida próprios. Embutir uma cópia viva dela em cada porto criaria N cópias do mesmo
município para manter. (Como a referência aterrissa no documento — só o id, ou id + um par de rótulos para
exibição — é a pergunta §8.7.)

**Desativar, não remover — e isto fecha um ponto aberto.** Remover um porto invalida as rotas e as
concessões que o referenciam, e verificar "nenhuma rota usa" exigiria *collection group*. Com `ativo`, o
porto desativado **some dos seletores e continua resolvendo** as rotas e os bilhetes que já apontam para
ele — que é o comportamento certo para dado referenciado por fato histórico. Vale igual para `Localidade` e
`Catalogo`.

**Quem cadastra — e as três não são iguais nisto:**

| Capacidade | Quem cadastra |
|---|---|
| **Catálogo** | **só `ADM`** (ADR-0017 §7.1) |
| **Localidade** | papel de plataforma — `ADM` + `GESTOR` |
| **Porto** | papel de plataforma — `ADM` + `GESTOR` |

O critério, que vale para o painel inteiro: **quanto mais perto o dado está da semântica do código, mais
restrito é quem o escreve.** O catálogo é o mais perto de todos — as categorias são tipo fechado (§3.8), e
um item novo muda o que os seletores oferecem e o que a regra do tipo de embarcação admite (§4.8); erro ali
é sistêmico. Localidade e porto são cadastro de gestão corrente, e prendê-los ao `ADM` criaria gargalo sem
ganhar segurança.

**Substitui a decisão anterior:** o ADR-0016 §5 gravava `cidade` como String por valor, justificando que
"rótulo é dado por valor". A justificativa era de persistência, e persistência não decide forma de domínio
(§1.1).

Com a dissolução do Trecho (§3.5), estas duas entidades passam a ser **a única âncora geográfica do
domínio**: a localidade dá o lugar no mapa, o porto dá o lugar físico, e é do par de portos de uma rota que
sai a linha.

**Escalabilidade, já que a coleção vai começar pequena.** Localidade é dado de referência clássico: cresce
até o tamanho do recorte geográfico atendido e para. Dois pontos ficam registrados desde já:

- **Ela é a primeira coleção de referência que pode deixar de ser "pequena".** O ADR-0017 D1 transforma cada
  coleção espelhada num `StateFlow` em memória, com a premissa explícita de que "são coleções pequenas". Um
  recorte municipal ou estadual mantém a premissa; uma cobertura nacional (milhares de municípios) **a
  quebra** — e a resposta então não é voltar ao espelho, é essa coleção deixar de ser observada por inteiro
  e passar a ser consultada sob demanda (`whereEqualTo` por UF, busca por prefixo). **É a exceção prevista
  ao D1, e o gatilho é o tamanho.**
- **A hierarquia para em dois níveis de propósito.** País e região não entram enquanto não houver operação
  que os peça; quando entrarem, entram como mais dois `Catalogo` embutidos, sem mudar a forma.

Porto é **lugar físico**, e lugar físico não pertence a quem navega — o cais de Manaus é o mesmo cais para
todas as empresas que atracam nele. O que a empresa tem no porto não é o porto: é a **atuação** nele.

### 3.5 Trecho — **dissolvido** (decisão do analista, 2026-07-31)

**O `Trecho` deixa de existir como entidade.** O que ele guardava — o par de cidades — passa a estar em
**Porto** (que tem a cidade) e em **Rota** (que tem os dois portos). Não há coleção `trechos/`, não há
`trechoId` na rota, não há `trechoIds[]` na concessão e não há módulo de cadastro de trecho.

**Por que era dispensável:** a rota já referencia dois portos, e cada porto já sabe sua cidade — logo o par
`(cidadeOrigem, cidadeDestino)` é **derivável**. Guardá-lo à parte era manter, num documento próprio, uma
informação que os outros dois já determinam, com o risco clássico da redundância: um trecho dizendo
Manaus → Parintins e um par de portos dizendo outra coisa. Uma linha deixa de ser **cadastro** e passa a ser
**leitura sobre os portos**.

**O argumento do compartilhamento sobrevive, num nível abaixo.** O ADR-0016 §7 defendia o trecho como bem
comum: duas empresas que vendem a mesma linha não deveriam refazer o cadastro. Isso continua verdade — só
que o que é comum e da plataforma agora é o **porto**, e as duas empresas compartilham *os dois portos* em
vez da linha que eles formam. Nada é duplicado que devesse ser comum.

**O que se paga, e precisa estar escrito:** sem `trechoId`, "quais rotas fazem Manaus → Parintins" deixa de
ser igualdade num campo e vira consulta sobre **o par de portos**; e se a pergunta for por *cidade* em vez
de por porto, ela se resolve no cliente ou por denormalização, porque o Firestore não faz junção. É custo
de **leitura**, não de modelo, e só aparece quando existir relatório por linha (§8.6).

### 3.6 Rota e Viagem — capacidades compartilhadas **[alvo]** · *redefinidas em 2026-07-31*

**`rotas/{rotaId}`** — o **onde** e o **quanto longe**.

| Campo | Tipo | Natureza | Notas |
|---|---|---|---|
| `id` | String | identidade | |
| `portoOrigem` | `Porto` | referência | a cidade é inferida dele (§3.4) |
| `portoDestino` | `Porto` | referência | idem |
| `distanciaMn` | Double | valor | milhas náuticas — **estético hoje** |
| `tempoMedioH` | Double | valor | tempo médio em horas — **estético hoje** |
| `criadoPor` / `criadoEm` | String / data | **assinatura** | quem criou e quando |
| `ativo` | Boolean | estado | não se exclui |

**`viagens/{viagemId}`** — o **quando** e **em quê**. **É atômica: uma saída = um documento.**

| Campo | Tipo | Natureza |
|---|---|---|
| `id` | String | identidade |
| `rotaId` | String | referência |
| `navioId` | String | referência |
| `diaSemana` | enum | valor — **anda junto com a hora** |
| `hora` | String | valor |
| `criadoPor` / `criadoEm` / `ativo` | | assinatura + estado |

A Viagem não é uma rota com agenda dentro: é o par `(navio, horário)` sobre uma rota. **A ocorrência
concreta é `(viagemId, data)`** — e com isso `Passagem.viagemId` **deixa de mentir**: hoje aponta para uma
entidade chamada Viagem que é um trecho, e a viagem concreta é reconstruída de data e hora **digitadas no
formulário**.

**Sem dono, universalmente acessíveis, e imutáveis.** Quem cria assina; ninguém exclui. Se a saída muda de
horário, **desativa-se e cria-se outra** — versionamento por substituição. É a imutabilidade que torna
seguro compartilhar sem dono: nenhuma agência quebra o que a outra vende. Passagem antiga apontando viagem
desativada é o comportamento correto — e **é essa imutabilidade que, em 2026-08-11, pagou o fim do snapshot**
(ADR-0023 D8): quando a fonte não muda, a cópia não protege de nada.

**A partida física ganha identidade — e o conflito da capacidade some.** Duas agências que vendem o mesmo
navio na mesma saída têm **a mesma viagem**. Ocupação = `count(passagens where viagemId = X and data = D)`,
atravessando empresas sem *collection group*; faturamento = o mesmo conjunto filtrado por empresa. Ocupação
(do navio) e faturamento (da agência) deixam de disputar a mesma entidade.

**Por que isto não ressuscita o Trecho:** o Trecho morreu por ser **derivável**. A Rota carrega `distanciaMn`
e `tempoMedioH`, fatos que nenhuma outra entidade tem. Entidade compartilhada se justifica quando não é
derivável. *(Os dois campos são de exibição hoje, não por natureza: hora de chegada é `hora + tempoMedioH`, e
distância é a base de qualquer tarifa por milha.)*

**Unicidade é condição, não higiene:** par de portos na Rota, `(rotaId, navioId, diaSemana, hora)` na Viagem,
**no servidor**. O pool sem dono prolifera por natureza — duas agências criam "Belém → Manaus" duas vezes
porque não acharam a existente —, e pool duplicado refragmenta a ocupação, que é o ganho principal se
perdendo pela porta dos fundos.

**A lista de negadas** (`rotasNegadas[]` / `viagensNegadas[]` na atuação, §3.2) é do supervisor da agência:
o que não interessa some da tela. **Não é autorização** — ver o quadro no §3.2.

---

*O texto abaixo é do desenho anterior, quando a Rota era da empresa e carregava a tarifa. Preservado como
registro; onde conflitar, vale o de cima.*

`empresas/{empresaId}/rotas/{rotaId}`

*Revisada em 2026-07-31: sem `trechoId` (§3.5).*

| Campo | Tipo | Natureza | Notas |
|---|---|---|---|
| `id` | String | identidade | |
| `navioId` | String | referência | qual embarcação opera — governa as tarifas (§4.8) |
| `embarquePorto` | `Porto` | **entidade composta** | de onde sai — **é o porto**, e dele vem a cidade de origem |
| `desembarquePorto` | `Porto` | **entidade composta** | onde atraca — dele vem a cidade de destino |
| `tarifas` | mapa | valor | a tabela do ADR-0013, **da empresa** (§3.7) |
| `agenda` | lista de `{diaSemana, hora}` | valor | dias em que opera e a hora de cada dia |

A rota é **como uma empresa realiza aquela ligação**: de qual porto sai, onde atraca, com qual embarcação, a
que preço e em que dias. **A tarifa é dela** — é por isso que mora na parte. Duas empresas que vendem a
mesma linha **compartilham os portos** e têm **rotas próprias**.

**A linha é derivada, não guardada:** `origem = embarquePorto.localidade`,
`destino = desembarquePorto.localidade` — e agora ela vem com UF junto, o que a torna agrupável (§3.4). Os
dois portos são **referências** pelo critério do §1.2 (agregado com ciclo de vida próprio); como a
referência aterrissa no documento é o §8.7.

**As viagens concretas não são persistidas:** as ocorrências da semana são **calculadas** a partir da
agenda. Não há coleção de ocorrências no MVP, e o custo é nomeado — a ocupação continua contada a partir
dos bilhetes, sem o O(1) de um contador. *Agenda é modelo; contador é otimização.*

O que existe hoje no lugar disso é a **`Viagem`** (`domain/viagem/Viagem.kt`), que nunca foi uma viagem:

| Campo | Tipo | Destino |
|---|---|---|
| `id` | String | permanece |
| `codigo` | String | derivado (origem/destino/empresa) — sem equivalente na Rota |
| `origem`, `destino` | String | viram os **dois portos** (e a cidade sai de cada um) |
| `empresaId` | String | vira o **caminho** (`empresas/{id}/rotas`) |
| `navioId` | String | permanece |

### 3.7 Tarifa — **cadastro dormente desde 2026-07-31** (ADR-0013)

> **A tabela cadastrada não será construída.** A `Rota` virou capacidade compartilhada sem dono (§3.6), e
> entidade sem dono não tem de quem ter tarifa. O dado passa a ser o **valor informado** na emissão; base,
> desconto e resultado são **inferidos por agregação** de passagens por rota e viagem.
>
> **O que morre é a fonte da base, não a matemática.** `TipoPassagem`, `TipoGratuidade`,
> `TipoPassagem.tarifaDevida`, `descontoDerivado`, `tarifaMotoBase` e o dinheiro em `BigDecimal` scale 2
> sobrevivem intactos — muda de onde vem o argumento `tarifaBase`. E a base inferida só significa algo
> agrupada por **(viagem, acomodação)** e **(viagem, classe)**, que são **os dois eixos da tabela**: ela não
> morre, deixa de ser *cadastrada* e passa a ser *observada*.
>
> Consequências: **`ResultadoEmissao.SemTarifa` deixa de existir** — nada bloqueia a emissão por falta de
> cadastro, o que dissolve a premissa do ADR-0017 D7; **`MEIA` vira classificação** e a aritmética migra
> para a agregação (a base é inferida só das INTEIRAS, então as meias não a poluem); e há **cold start** — a
> primeira passagem de uma viagem nova não tem base. `Passagem.tarifaBase` nasce nulo, caso que o
> `PassagemDadosPassagemMapper` já trata.

*O desenho abaixo fica como registro do que foi decidido e não construído.*

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
| `categoria` | String (do enum `Categoria`) | String na fronteira, **do tipo fechado `Categoria`** — ver abaixo |
| `ordem` | — | Int — **novo** |
| `ativo` | — | Boolean — **novo**, desativar em vez de remover |

**Invariante:** `(categoria, descricao)` é **único**. É a mesma regra da `Localidade` um nível abaixo — dois
"Belém" em `MUNICIPIO` fragmentam a dimensão geográfica antes mesmo de ela ser montada.

**`ordem`** existe porque hoje o item nasce por `.add()` com id gerado e a lista sai na ordem em que o
Firestore devolver: "Rede, Suíte, Camarote" apareceria embaralhado.

É a tabela do que o negócio precisa **nomear mas não precisa modelar**: UF, município, tipo de documento,
tipo de veículo, acomodação, forma de pagamento, tipo de passagem, e — novos **[alvo]** —
`TIPO_EMBARCACAO` e `ATUACAO`.

**O item de catálogo é um *value object de referência*** (§1.2): não tem ciclo de vida próprio, é pequeno,
estável e sempre lido junto de quem o usa. Por isso é o **único tipo que embute** em vez de ser
referenciado — e a `Localidade` (§3.4) é a primeira entidade a exercer isso, guardando dois deles
(`UF` e `MUNICIPIO`) como a sua própria substância.

**O critério que separa catálogo de tipo de domínio:** quem tem **regra** vira tipo (`StatusPassagem`,
`TipoPassagem`, `TipoGratuidade`); quem é **só rótulo** vira linha de catálogo. Manter os dois vocabulários
para a mesma coisa é a receita de divergirem — e foi o que aconteceu com `Constante.Descricao`, que ainda
lista `CORTESIA`, `A_EMITIR` e `EMITIDA` **[morre]**.

**Mas `Categoria` não cai nesse critério — e a decisão anterior de matá-la junto com `Descricao` tratava as
duas como iguais por engano.** Ela não é rótulo de usuário: é o **índice do catálogo**, e o código depende
dela. Seis chamadores consultam por categoria — `ViagemDadosViagemMapper.kt:31` (`MUNICIPIO`),
`ContagemPassagensMapper.kt:77` (`GRATUIDADE`), `FormFuncionarioViewModel` (`MUNICIPIO`) e os helpers do
form de passagem (`ACOMODACAO`, `TIPO_PASSAGEM`, `GRATUIDADE`, `DOCUMENTO`, `PAGAMENTO`). Como String
livre, cada um carregaria um literal — e um erro de digitação **devolve lista vazia em silêncio**: seletor
sem itens, sem erro, sem log.

Então **`Categoria` continua tipo fechado** **[alvo]**. Isso não impede acrescentar **item** sem deploy, que
é o ganho que interessa; impede acrescentar **categoria** sem deploy — e isso é o correto, pelo mesmo
fail-closed do §4.8: categoria nova sem código que a consuma não serve para nada. Valores: `MUNICIPIO`,
`DOCUMENTO`, `VEICULO`, `ACOMODACAO`, `TIPO_PASSAGEM`, `GRATUIDADE`, `PAGAMENTO`, e os novos `UF`,
`TIPO_EMBARCACAO` e `ATUACAO`. Saem `CATEGORIA_PASSAGEM` e `STATUS_PASSAGEM`, que viraram tipos.

`IObjetoSimplificado` (`id` + `descricaoNome`) **fica exclusivo do catálogo** **[alvo]**: um item de
catálogo *é* um par id/descrição — é tudo que ele é. `Funcionario` e `Navio` deixam de implementá-la.

### 3.9 Funcionario — a pessoa na operação

`domain/operacoes/Funcionario.kt` **[hoje]** · `funcionarios/{id}`

| Campo | Tipo | Natureza | Notas |
|---|---|---|---|
| `id` | String | identidade | |
| `descricaoNome` | String | valor | **o nome da pessoa** (o `Usuario` não tem nome) → `nome` **[alvo]** |
| `cargo` | String (`Cargo`) | tipo fechado na fronteira | **[morre no documento]** — migra para dentro do vínculo |
| `email` | String | valor | **chave de descoberta, uma vez só**: casa o pré-cadastro com a conta do Auth no primeiro acesso; depois o elo permanente é o id |
| `agencia` | String | valor | **[morre]** — a agência vira uma empresa, e a relação vira id |
| `lotacao` | String (`Lotacao`) | valor | **[morre]** |
| `vinculos` | lista de `{empresaId, atuacao, cargo}` | **[alvo]** | onde atua, **em quê** e como |
| `empresaIds` | lista | **[alvo]**, derivado | denormalização deliberada, **só para consulta** |

**O cargo mora no vínculo, e o vínculo é o par `(empresa, atuação)`** *(decisão de 2026-07-31)* — não só a
empresa, porque uma mesma empresa exerce mais de uma atuação e a pessoa pode ter papel diferente em cada
uma. Isso substitui a decisão anterior (`cargo` um só, da pessoa), que era a escolha mínima enquanto havia
um segmento operante só.

**Por que `empresaIds` sobrevive ao lado:** o Firestore **não consulta campo de dentro de elemento de
array** — `array-contains` casa o elemento inteiro —, então "quem trabalha na empresa X" não sai de
`vinculos`. O array chato de ids fica ao lado, derivado, **no mesmo documento e na mesma escrita**. É o caso
mais barato de dado derivado que existe (sem sincronia entre documentos), e está escrito aqui para não ser
lido como redundância acidental.

**O contexto ativo é o vínculo:** escolhê-lo determina de uma vez o cargo em vigor, as seções do menu e o
recorte das listagens. É também a resposta para "sob qual vínculo se emite" — a agência do bilhete vem do
vínculo ativo, não do funcionário.

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

É o **único agregado** do app — o fato transacional que consome os dados de referência. As demais entidades
são referência; a Passagem é o evento. Hoje são **49 campos planos**, agrupados por natureza:

| Grupo | Campos | Natureza |
|---|---|---|
| Identidade | `id`, `numero` | o `numero` vem do `ContadorBilhete` |
| Referências vivas | `viagemId`, `navioId`, `empresaId`, `funcionarioId` | ids estáveis (ADR-0008) |
| **Snapshot** | `codigoViagem`, `empresa`, `embarcacao`, `origem`, `destino`, `dataViagem`, `horaViagem`, `agencia`, `funcionarioResponsavel`, `embarcadaPor` | congelados na emissão — **saem inteiros** no `[alvo]` (ADR-0023 D8) |
| Dinheiro | `valorPix`, `valorDinheiro`, `valorDebito`, `valorCredito`, `tarifaBase` | `Double?` na fronteira |
| Categoria tarifária | `tipoPassagem`, `gratuidade`, `acomodacao` | Strings de tipos fechados |
| Participantes | `nome/documento/numeroDocumento/dataNascimento` × **3 passageiros** | passageiro 1 = titular |
| Veículo | `tipoVeiculo`, `modeloVeiculo`, `placaVeiculo`, `corVeiculo`, `cilindrada` + responsável pela retirada (3 campos) | participante de mesmo nível |
| Ciclo de vida | `status`, `embarcadaPorId`, `embarcadaPor`, `embarcadaEm` | FSM do ADR-0012 |
| Derivados | `temPassageiro2`, `temPassageiro3`, `ehVeiculo` | calculados, não persistidos |

**[alvo]** *(planejamento de domínio de 2026-08-11 —
[ADR-0023](../adr/0023-passagem-por-categoria-e-referencia.md))* A forma acima é reformada **na raiz**:

- **a categoria é o eixo**, e os sub-domínios são um **tipo fechado**: `PassagemDePassageiro` |
  `PassagemDeVeiculo`, com **`PassagemDeCarga` previsto** — a estrutura tem de estar pronta para recebê-la, e
  a prontidão é o formato, não um campo reservado. `ehVeiculo` some por construção;
- **o comum é a travessia vendida**: a **ocorrência** `(viagemId, data)`, o **lançamento** (formas × valores),
  a observação e os **metadados** (`status` · `funcionarioId` · `agenciaId` · `criadoEm` · `alteradoEm` ·
  `embarcadaPorId` · `embarcadaEm`) — os dois ids **inferidos** do vínculo ativo, e **só o `status` aparece em
  tela**;
- **nada é congelado** (D8): onde havia par *id + valor*, fica só o id — 10 campos de snapshot saem. Congelar
  virou decisão da camada de dados, e só se tiver relevância demonstrada;
- **participantes por referência**: os três passageiros viram **uma lista ordenada de `clienteId`** (titular =
  primeiro), e o responsável pela retirada, um `clienteId?`. O **`Cliente`** é entidade com cadastro próprio e
  ganha **telefone**;
- **a regra sobe para o tipo**: a acomodação (Rede | Suíte | Camarote) declara que **tipos tarifários** admite
  — suíte e camarote são sempre inteira, meia e gratuidade só na rede —, e a ocupação de suíte/camarote **é** o
  tamanho da lista de clientes. No veículo, o **tipo governa**: carreta e caminhão não pedem modelo (o tipo já
  é o modelo), moto exige cilindrada;
- **`ModoPassagem` se dissolve**: era um eixo de quatro valores com o veículo dentro; viram dois níveis,
  categoria × acomodação.

Detalhado em [dominio-passagem.md](dominio-passagem.md) (§1, §2 e §5 reescritos; §11 fica como registro do
caminho).

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
do `ADM`. Hoje a política trata os dois como um bloco (`ehPapelPlataforma`), e ele **continua valendo para
todo o resto do painel** — a separação é do catálogo, não do papel (§3.4).

### 4.2 `Cargo` — o eixo aberto, **qualificado pela atuação** **[hoje + alvo]**

| Valor | Atuação | Significado |
|---|---|---|
| `SUPERVISOR` | `AGENCIAMENTO` | responde pela operação onde atua; monta rotas **[alvo]**; edita qualquer passagem |
| `AGENTE` | `AGENCIAMENTO` | emite passagem; edita as próprias |
| *(a definir)* | `TRANSPORTE` | **[alvo]** — gere a frota da empresa dona |
| *(a definir)* | `PORTUARIA_*` | **[alvo]** — check-in, quando a atuação acordar |

**Cada atuação tem a sua lista de cargos** *(decisão de 2026-07-31)*. `SUPERVISOR` e `AGENTE` nunca foram
"os cargos do sistema" — são os cargos do **agenciamento**; quem gere frota faz outra coisa.

**A forma:** um `Cargo` só, em que **cada valor declara a que atuação pertence**. Mantém `Cargo.de(String)`
na fronteira, mantém a política com uma entrada só, e torna o par `(atuação, cargo)` explícito e testável.

**Continua sendo tipo de código, não linha de catálogo** — pelo mesmo motivo do tipo de embarcação (§4.8):
**cargo concede permissão**, e cargo cadastrável seria escalonamento de privilégio por cadastro. O catálogo
pode guardar o rótulo; a capacidade é código. **Sem default na fronteira, de propósito.**

Isso **supera parcialmente o ADR-0015**, que fixou o cargo como eixo aberto e **plano**: ele continua
crescendo com a operação, mas cresce **dentro de uma atuação**. E é o que faz a plataforma ser
multi-segmento de verdade — acrescentar um segmento vira declarar atuação + cargos + seção, sem tocar o
modelo de permissão.

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

### 4.8.1 `Catalogo.Categoria` — o índice do catálogo **[hoje, sobrevive]**

`MUNICIPIO` · `UF` **[alvo]** · `DOCUMENTO` · `VEICULO` · `ACOMODACAO` · `TIPO_PASSAGEM` · `GRATUIDADE` ·
`PAGAMENTO` · `TIPO_EMBARCACAO` **[alvo]** · `ATUACAO` **[alvo]**

Saem `CATEGORIA_PASSAGEM` e `STATUS_PASSAGEM` — viraram tipos de domínio. **É o único enum do catálogo que
sobrevive**, e a razão está no §3.8: categoria é vocabulário de **código**, não rótulo de usuário.

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

**[alvo]** passa a ter **uma família por atuação, mais o painel da plataforma** — não duas, como a versão
anterior do ADR-0016 §2 supunha:

| Família | Seções |
|---|---|
| Painel da plataforma | `EMPRESA`, `NAVIO`, `LOCALIDADE`, `PORTO`, `CATALOGO` |
| `AGENCIAMENTO` | `PASSAGEM`, `ROTA` |
| `TRANSPORTE` | frota **[alvo]** |
| `PORTUARIA_*` | check-in **[alvo, dormente]** |

`EQUIPE` aparece no painel e em cada atuação — é a única que atravessa. **`VIAGEM` sai**: o nome estava
errado desde o começo, e `TRECHO` nunca chega a existir (§3.5).

A divisão em duas famílias era o que se enxergava com um segmento operante só. Com o cargo qualificado pela
atuação (§4.2), a família **deriva da atuação** em vez de ser enumerada à mão.

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

**[alvo]** passa a tomar **`(papel, atuação, cargo)`** — o cargo só significa alguma coisa dentro de uma
atuação (§4.2). Continua sendo **uma política só** (ADR-0010): é a mesma com uma pergunta a mais, não uma
segunda.

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

### 5.4 Coerência da rota **[alvo]** — *revisada em 2026-07-31*

Com o Trecho dissolvido (§3.5), a regra **geográfica desaparece por construção**: ela existia para impedir
que o par de portos contradissesse o par de cidades declarado no trecho, e **não há mais dois lugares para
discordar**. A cidade de origem *é* a do porto de embarque. Um invariante que some porque a redundância que
o exigia sumiu é o melhor tipo de simplificação.

Restam duas checagens, ambas puras:

1. **De concessão:** os dois portos têm que estar em `portoIds[]` da atuação, e o **navio** em `navioIds[]`.
   Sem isso o recorte concedido seria decorativo — bastaria digitar um id de fora. Tem que valer **também no
   servidor**, e desde a concessão por navio é **comparação direta**: os três ids estão na atuação, nenhum
   documento extra precisa ser lido. *(Antes era indireta — a rota guarda `navioId`, não o armador, então
   descobrir o dono exigia ler o navio: um lookup a mais na UI e um `get()` a mais por escrita na regra.)*
2. **De sentido:** embarque e desembarque não podem ser o mesmo porto. É trivial e é nova: enquanto havia
   trecho, o par de cidades distintas garantia isso de graça.

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

**A decisão do §1.1 muda o desfecho da primeira linha, e para melhor.** A saída não é "apagar as anotações
e ficar com a mesma classe plana": é que **aquela classe plana vira o DTO** e a entidade de domínio nasce
ao lado dela, rica e composta. As dez classes anotadas não desaparecem — elas **mudam de camada e de
nome**, e é o mapper que passa a ligar as duas pontas. O que some é a coincidência de hoje, em que uma
única classe é simultaneamente a lei do negócio, a linha da tabela e o objeto que a tela lê.

Isso reordena o trabalho: o §1.1 não é mais uma consequência do ADR-0017 — é o **desenho de destino** que
diz para onde a classe vai quando o Room sair.

## 7. O que muda quando os ADRs forem implementados

| Hoje | Alvo | ADR |
|---|---|---|
| `Constante` (`constants`) | `Catalogo` (`catalogo`), sem enums internos | 0016 §3 |
| `Viagem` (raiz) | `Rota` (`empresas/{id}/rotas`), com os dois portos | 0016 §7, **revisado** (§3.5) |
| `Viagem` = trecho, da empresa | `Rota` + `Viagem` **na raiz**, compartilhadas, imutáveis, assinadas | §3.6, **novo** |
| Tarifa cadastrada na viagem | tarifa **dormente**; base inferida por agregação | §3.7 |
| Data e hora **digitadas** na emissão | ocorrência = `(viagemId, data)` selecionada | §3.6 |
| — | `Localidade` (raiz): `uf` + `municipio` embutidos, `codigoIbge`, `ativo` | §3.4, **novo** |
| — | `Porto` (raiz), com `localidade` por referência e `ativo` | 0016 §5, **revisado** (§3.4) |
| `Constante` sem ordem nem estado | `Catalogo` com `ordem` e `ativo`, `(categoria, descricao)` único | §3.8, **novo** |
| Remover cadastro de referência | **desativar** (`ativo`) — some do seletor, segue resolvendo | §3.4 |
| — | `empresas/{id}/atuacoes/{ATUACAO}` + concessão | 0016 §4 |
| `Funcionario.agencia` + `lotacao` + `cargo` no documento | `vinculos: [{empresaId, atuacao, cargo}]` + `empresaIds` derivado | 0016 §6/§6.1 |
| Política `(papel, cargo)` | Política `(papel, atuação, cargo)`; cargo declara sua atuação | 0016 §6.1 |
| `SecaoMenu` uma lista só | uma família **por atuação** + o painel da plataforma | 0016 §2 |
| `Navio` sem tipo | `Navio.tipoEmbarcacao` governando as tarifas | 0016 §8 |
| `ADM`/`GESTOR` emitem passagem | emitir exige `funcionarioId`; painel exige papel | 0016 §2 |
| Entidades anotadas com Room | entidades puras; cache do SDK | 0017 |
| `PassagemDigital` (tabela) | arquivo na galeria, nome derivado do `idPassagem` | 0017 D5 |
| `Usuario.ultimoUsuarioLogado` | DataStore | 0017 D4 |
| Seed escreve o dado | painel administrativo escreve o dado | 0016 §1 |

## 8. Perguntas em aberto

Herdadas dos ADRs, e que o domínio não resolve sozinho:

1. ~~**Onde se escolhe o vínculo ativo**~~ — **RESOLVIDO: no login.** A escolha determina cargo, seções e
   recorte de uma vez, então tem de estar feita antes de qualquer tela existir. Trocar de vínculo é trocar de
   sessão de trabalho, não uma opção dentro da emissão.
2. ~~**Remoção de porto/navio concedido**~~ — **RESOLVIDO.** Catálogo, localidade e porto: não se remove,
   **desativa-se** (`ativo` — §3.4). Navio saindo de `navioIds`: é **caso de sincronização, não de
   modelo** — a concessão é referência viva, a regra de escrita da rota barra as escritas seguintes e nada
   se invalida retroativamente. Não há estado a inventar.
3. ~~**`cargo` por pessoa ou por vínculo**~~ — **RESOLVIDO: por vínculo**, e o vínculo é `(empresa, atuação)`
   (§3.9, §4.2).
4. **Onde mora a validação de coerência da rota** (§5.4) no código: função pura no domínio, como a tarifa,
   ou caso de uso? *Inclinação: função pura no domínio — ela só precisa dos dados, não de repositório.*
5. **A chave tarifária vira tipo fechado?** (§4.7) Hoje é String canônica por convenção, o que contraria o
   critério do §3.8: ela **tem regra** (define eixo e admissibilidade), logo deveria ser tipo.
6. *(nova — decisões de 2026-07-31)* **A busca por linha vai existir?** Sem `Trecho`, "quais rotas fazem Manaus → Parintins" é consulta sobre
   o par de portos (§3.5). Se um relatório por linha entrar no escopo, é aí que se decide entre resolver no
   cliente e denormalizar as cidades na rota. **Não** é motivo para reviver o trecho — é escolha de leitura.
7. *(nova)* **Quanto de rótulo viaja da `Rota` para o `Porto`?** Já resolvido para a `Localidade` (embute
   `id` + `descricao` de cada `Catalogo`) e para `Porto.localidade` (referência). Falta a rota: guarda só
   `embarquePortoId`, ou guarda `{id, nome, municipio, uf}` para desenhar a linha sem uma segunda leitura?
   *Inclinação: guardar os rótulos como **cache de leitura** — não como verdade —, com a advertência de que
   renomear um porto deixa cópias velhas até serem reescritas. Se isso incomodar, a alternativa honesta é só
   o id.*
8. *(nova)* **O DTO é um por entidade ou um por caso de uso?** (§1.1) Um por entidade é o que existe hoje (a entity
   plana do Room vira o DTO); um por caso de uso evita carregar campo que a tela não usa, mas multiplica as
   classes. *Inclinação: um por entidade agora, especializando só quando doer.*