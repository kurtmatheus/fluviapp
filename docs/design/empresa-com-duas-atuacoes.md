# A atividade como concessão, o cargo como registro

> **Estudo** (2026-08-18). Mapeia o domínio como ele está e prepara decisão; quem decide é o analista, e o que
> ele decidir vira ADR. Estudo não tem autoridade: onde ele e um ADR discordarem, **o ADR vence**.
>
> **Recorte deliberado: só domínio.** Tela, navegação, formulário e regra de servidor **não entram** — decisão
> do analista: *"essas terão que se adequar de um jeito ou de outro"*. Decidido o que o negócio é, apresentação
> e fronteira são consequência.
>
> **Todas as decisões chegaram** (2026-08-18): as seis da §2 e mais três (D7–D9), que fecharam os três limites
> de inferência medidos na §6.3. **O estudo está fechado**; o que resta é o ADR, e a §8 diz o que ele precisa
> escrever.
>
> Marcadores: **[hoje]** o que está no ar · **[alvo]** o que o caso pede · **[morre]** o que sai.

## 1. O caso

A empresa matriz — a que a plataforma serve, e de quem o negócio foi traduzido para o sistema — faz **mais de
uma coisa no mesmo porto**: vende a travessia (`AGENCIAMENTO`), **arrenda o espaço no cais e faz o check-in**
(`PORTUARIA_ARRENDAMENTO`) e **embarca no próprio navio** (`TRANSPORTE`, a navegação). Não são três empresas,
não é um tipo de empresa novo: é **uma parte exercendo várias atuações**. E desce para a pessoa: quem é agente
também opera o check-in e o embarque.

O segundo caso do enunciado é o que dá a régua: **uma empresa dona de porto usando a plataforma precisa
gerenciar cargos e atividades para delegar demandas intraportuárias**. Não é o mesmo pedido em outra escala — é
o pedido que exige que a estrutura de pessoal seja **do negócio**, e não do código.

Três frases do enunciado valem como restrição:

- **os dois vínculos estão superados** — um funcionário opera por **uma** empresa;
- o que existe é **uma empresa com várias atuações**, com a pessoa atuando nelas;
- a plataforma vai abarcar **cadeias diferentes da logística**, então o domínio tem de saber **se rearranjar a
  partir das atividades**.

E o enquadramento que ordena o resto: **o que importa é a gestão da informação — como esses dados são
rastreados e/ou inferidos** (§6).

## 2. As decisões

*Do analista, 2026-08-18.*

**D1 — A atividade concede; o cargo registra.** *"Com atividades definidas, o cargo passa a ser um registro e
atividade passa a ser concessão."* Não é abrir a permissão para cadastro: é separar duas coisas que estavam na
mesma palavra — *o que o sistema sabe fazer* (fechado, código) e *quem, nesta empresa, faz o quê* (dado, gestão
de pessoas).

**D2 — As quatro primeiras atividades, com a fonte e o alcance de cada uma:**

| Atividade | De onde deriva | Alcance |
|---|---|---|
| `emitir_passagem` | cargo, na atuação de `AGENCIAMENTO` | emite **e gerencia as próprias** |
| `gerenciar_passagem` | cargo, na atuação de `AGENCIAMENTO` | **poderes emergenciais** sobre qualquer uma, numa eventual análise |
| `fazer_check_in` | atuação `PORTUARIA_ARRENDAMENTO` — a arrendatária | o porto arrendado |
| `confirmar_embarque` | atuação `TRANSPORTE` — a navegação | a embarcação da atuação |

Duas precisões que vieram com elas, e ambas mudam estrutura:

- **a posse entra na atividade.** `emitir_passagem` *"pressupõe que se emite e gerencia somente as próprias"* —
  então posse deixa de ser coordenada da política e passa a ser **parte da definição** da atividade. O eixo
  *"ação sobre a Passagem **com posse**"* do ADR-0010 se dissolve aí;
- **`gerenciar_passagem` não é fino, é excepcional.** Não é a permissão de rotina de quem supervisiona: é
  **poder emergencial exercido numa análise**. Isso muda menos a autorização e mais o rastro — uma atividade de
  exceção que não registra que foi usada é o único ponto em que a inferência não recupera o fato (§6.4).

**D3 — A empresa cadastra os cargos.** O quadro é dela; a plataforma não o escreve por ela.

**D4 — Um cargo por vínculo.** Acumulação de função entra como cargo próprio, com as duas listas de atividades
dentro dele.

**D5 — Nada de cargo vai no bilhete: *"só quem emitiu já tem essa informação, tudo pode ser inferido"*.** É o
ADR-0023 D8 (*no domínio nada é congelado*) aplicado à camada nova, e é o que a §6 desdobra.

**D6 — `operante` passa a ser derivado da atividade.** Atuação com atividade declarada é atuação operante — o
marcador deixa de ser um `Boolean` mantido à mão.

As três seguintes respondem aos limites da §6.3, e é o que torna a auditoria possível **por inferência**:

**D7 — A delegação é imutável.** A lista de atividades de um cargo **não muda**: redelegar é **criar outro
cargo e inativar o anterior**, o regime que Rota e Viagem já usam. Com isso, *"o que esta pessoa podia fazer
quando fez isto"* volta a ser exato sem log e sem versionamento — o id que o vínculo aponta nunca muda de
significado.

**D8 — O poder emergencial registra autor.** `alteradoEm` deixa de andar sozinho: quem exerce
`gerenciar_passagem` fica registrado. A exceção passa a ser um **ato**, com autor e instante, como o embarque
já é.

**D9 — A autoria de negócio unifica no `funcionarioId`.** O `uid` sai dos registros de negócio: emissão,
alteração, check-in e embarque passam a ser atribuídos à **mesma chave**, a da pessoa na operação. É a
separação dos dois contextos do ADR-0015 §8.1 chegando ao registro — o `Usuario` responde por credencial, o
`Funcionario` responde por trabalho feito.

## 3. O que o modelo já suporta

**A parte já é multi-atuação, por construção.** `AtuacaoDaEmpresa`
([`AtuacaoDaEmpresa.kt:24`](../../app/src/main/java/dev/matheus/fluviapp/domain/viagem/AtuacaoDaEmpresa.kt))
é `empresas/{id}/atuacoes/{ATUACAO}` — **um por atuação** —, com `exerce(atuacao)` e `de(atuacao)` (`:69`,
`:72`). O KDoc já dá a razão de não ser subtipo nem campo: *"uma parte exerce várias atuações ao mesmo tempo e
muda de conjunto ao longo do tempo"*. É o que faz `agenciaId` e `empresaId` serem o mesmo id — e é onde a
multiplicidade do caso já estava modelada antes de o caso existir.

**As atuações do caso existem como valores.** `TRANSPORTE` é operante; `PORTUARIA_ARRENDAMENTO` e
`PORTUARIA_OPERACAO` estão lá com `operante = false`
([`Atuacao.kt:24-27`](../../app/src/main/java/dev/matheus/fluviapp/domain/operacoes/Atuacao.kt)) — a segunda é a
da **dona do porto**, e o KDoc já aponta o destino: *"é onde o módulo de check-in vai morar"*.

**A concessão de recursos já é por atuação.** Cada `AtuacaoDaEmpresa` carrega **onde** (`portoIds`) e **em quê**
(`embarcacaoIds`), com `operaNoPorto`, `concedeu` e `podeOfertar` (`:38-65`).

**O princípio que a decisão reusa já está escrito.** O ADR-0016 §6.1 fixou que *"cada valor declara a que
atuação pertence"* — foi assim que `Cargo.SUPERVISOR(AGENCIAMENTO)` nasceu. D1 move o princípio de portador:
quem declara a atuação passa a ser a **atividade**.

**E a inferência já é regime, não novidade.** A tarifa é inferida por agregação (ADR-0016 §7.2), a ocorrência
`(viagemId, data)` é calculada e não persistida (F8.4), o total dos lançamentos é inferido (ADR-0024) e
`MetadadosPassagem` já registra **dois ids e dois instantes** para que o resto se derive. D5 não abre um regime
novo: aplica o que existe.

## 4. Onde o domínio não cabe [hoje]

### 4.1 O que concede é um enum de dois valores, de uma atuação só

`Funcionario.Cargo` tem `SUPERVISOR` e `AGENTE`, ambos `AGENCIAMENTO`
([`Funcionario.kt:80-82`](../../app/src/main/java/dev/matheus/fluviapp/domain/operacoes/Funcionario.kt)). Para a
dona de porto isto não é *insuficiente*, é **inexpressável**: o quadro dela (quem confere, quem opera pátio,
quem responde por turno) não é valor a acrescentar num enum — é **cadastro dela**, que muda sem o app mudar. E
`Vinculo.de` é fail-closed em cargo desconhecido (`Vinculo.kt:42-46`): hoje um cargo que o código não conhece
**derruba o vínculo inteiro** — proteção correta enquanto o cargo concede, e a barreira exata que impede a
gestão de pessoas de existir.

### 4.2 A concessão não é enumerável — só interrogável

`PermissoesUsuario` tem treze perguntas nomeadas por ato (`podeCriarRota`, `podeEditarQualquerPassagem`,
`podeConfirmarEmbarque`…), e **cinco reabrem a mesma decisão por dentro** (`vinculo?.cargo ==
Cargo.SUPERVISOR` —
[`PermissoesUsuario.kt`](../../app/src/main/java/dev/matheus/fluviapp/domain/operacoes/PermissoesUsuario.kt)).
Os atos já são o vocabulário do negócio; falta o **tipo**. Sem tipo, são nomes de função: não se enumeram, não
se atribuem a ninguém e não declaram de onde derivam — e *"o que esta pessoa pode fazer?"* não tem resposta, só
treze respostas para *"ela pode isto?"*. `podeConfirmarEmbarque(papel)` (`:269`) é o extremo: concede a
**qualquer papel conhecido**, sem olhar empresa nem atuação.

### 4.3 A cardinalidade do vínculo — o problema que a decisão dissolve

`Vinculo` é `(empresaId, cargo)` com a atuação derivada, e a identidade dele **é a empresa**: `naEmpresa` é
`firstOrNull` (`Vinculo.kt:60`), `unicoOuNenhum` é `singleOrNull` (`:69`), `resolverVinculoAtivo` casa por
`empresaId` (`:86-90`). Enquanto o cargo era a concessão e havia um por vínculo, duas atuações exigiriam **dois
vínculos na mesma empresa** — e a busca por chave devolveria o primeiro, descartando o segundo em silêncio.
Registro dissolvido, como medida do que a decisão evitou: **repetir a chave para variar o conjunto é o que faz
uma busca por chave mentir.**

### 4.4 A atuação em vigor é singular a jusante

`ContextoUsuario.atuacao: Atuacao?`
([`ContextoUsuario.kt:73`](../../app/src/main/java/dev/matheus/fluviapp/domain/operacoes/ContextoUsuario.kt)),
`atuacaoEmVigor(vinculo)` (`PermissoesUsuario.kt:233`), `secoesDa(atuacao)`
([`MenuDaAtuacao.kt:65`](../../app/src/main/java/dev/matheus/fluviapp/domain/screendata/MenuDaAtuacao.kt)) e
`escopoDoPool(papel, atuacao)`
([`EscopoDoPool.kt:49`](../../app/src/main/java/dev/matheus/fluviapp/domain/viagem/EscopoDoPool.kt)) recebem
**uma** atuação. Com a atividade como unidade, o singular não é limitação — está no **lugar errado**: não é a
sessão que tem uma atuação em vigor, é **cada atividade** que tem a sua, sempre a mesma, por definição.

## 5. O modelo que as decisões produzem

### 5.1 Três camadas, e cada uma responde uma pergunta diferente [alvo]

| Camada | Pergunta | Natureza | Dono |
|---|---|---|---|
| **`Atuacao`** | o que esta empresa faz no mundo | tipo fechado + cadastro do exercício | plataforma declara o valor; painel cadastra o exercício |
| **`Cargo`** | quem, nesta empresa, faz o quê | **dado — entidade da empresa** | a empresa (D3) |
| **`Atividade`** | o que o sistema sabe fazer | **tipo fechado, código** | plataforma |

A composição é uma **interseção fail-closed nas duas pontas**:

```
atividades em vigor  =  as delegadas ao cargo da pessoa  ∩  as das atuações que a empresa exerce
```

Cada ponta protege de uma coisa diferente, e é por isso que as duas existem. A **atuação** impede a empresa de
delegar o que ela não faz — quem não arrenda cais não delega `fazer_check_in`, e a atividade nem aparece na
lista de delegáveis. O **cargo** impede que todo mundo da empresa faça tudo o que a empresa faz: sem essa
ponta, a dona de porto não consegue *delegar demanda*, porque não há a quem delegar diferente.

`Atividade` é tipo fechado pelo mesmo argumento que sempre valeu — **atividade concede, e concessão cadastrável
é escalonamento de privilégio por cadastro**. O que muda é *onde* ele se aplica: protegia o `Cargo` enquanto o
cargo concedia; agora protege a `Atividade`, e **libera** o cargo.

### 5.2 A política passa a ter uma entrada só [alvo]

As treze funções `podeX` viram **valores** de `Atividade`, e a política responde uma pergunta:

```
atividade in atividadesDe(contexto)
```

`atividadesDe` é função pura sobre `(papel, cargo do vínculo, atuações da empresa)` — mesma natureza de
`secoesDa` e `resolverVinculoAtivo`, testável em JVM sem infraestrutura. Continua **única** (ADR-0010): a mesma
política com uma pergunta em vez de treze.

Duas fronteiras ficam de fora da delegação: **atividade de plataforma** (`ADM`/`GESTOR`) não é delegável por
empresa nenhuma — é o que separa administrar a plataforma de operar um negócio nela —, e **posse** não é
atividade, é o alcance de `emitir_passagem` (D2).

### 5.3 O `Cargo` vira entidade da empresa [alvo] · o enum **[morre]**

Com a concessão fora dele, o cargo pode ser dado sem abrir privilégio — e a forma já está definida pelo regime
do projeto. O ADR-0020 fechou que **vocabulário é tipo, dado é entidade, e não há terceira categoria**: o cargo
não é linha de catálogo, é **entidade**, com id, nome, dono (`empresaId`), delete lógico e as atividades
delegadas. Mesma anatomia de `Porto`, `Localidade` e `Embarcacao`; o que muda é o dono, que aqui é a empresa.

Consequências no domínio:

- `Funcionario.Cargo` (enum) **morre**; `Vinculo` referencia o cargo **por id** (ADR-0008: id para relacionar),
  e a atuação deixa de ser derivada do cargo — passa a vir das **atividades** dele;
- `SUPERVISOR` e `AGENTE` deixam de ser valores de código e viram **cargos cadastrados** de cada agência. O
  conjunto fixo de hoje é o **cadastro inicial** de quem agencia;
- o fail-closed muda de gesto: cargo desconhecido não derruba mais o vínculo (dado se resolve por id) —
  **cargo sem atividade** passa a ser o estado que não concede nada. É mais fiel ao negócio: existe função que
  registra papel e não dá poder de sistema.

### 5.4 As duas invariantes da delegação — onde o anti-escalonamento passa a morar [alvo]

Tirar a concessão do enum move o risco, não o elimina: quem delega pode delegar demais. Duas regras o cercam, e
as duas são função pura sobre o contexto e o cargo:

1. **Ninguém delega o que a empresa não exerce.** O universo do delegável é o das atuações da parte — a mesma
   allow-list do ADR-0016 §7, aplicada a trabalho em vez de a porto e embarcação.
2. **Ninguém delega o que não possui.** Quem monta um cargo só põe nele atividades que ele mesmo tem — sem
   isso, quem gere pessoal se promove escrevendo um cargo. Com isso, a delegação só desce.

O poder de delegar é ele mesmo uma atividade (`gerenciar_cargo`): é o que faz a dona de porto organizar o
trabalho dela sem a plataforma no meio, e o que mantém **uma** régua — quem pode delegar responde à mesma
pergunta que todo o resto.

### 5.5 O vínculo fica com a forma de hoje [hoje = alvo]

`Vinculo(empresaId, cargoId)` — **um por empresa** (D4), trocando o enum pelo id. É resultado, não omissão: a
multiplicidade que pressionava a forma dele era a das atuações, e ela voltou para a parte. A régua: **o que é
múltiplo na empresa se modela na empresa; o que é múltiplo na pessoa se modela na pessoa.** O par `(empresa,
atuação)` que o ADR-0016 §6.1 previa **não precisa existir como dado** — a atuação se deduz das atividades do
cargo.

### 5.6 A concessão de recursos é escolhida pela atividade [alvo]

`escopoDoPool` deixa de receber *a* atuação e passa a receber **a atuação da atividade**: emitir lê a concessão
do agenciamento, fazer check-in lê o porto do arrendamento, confirmar embarque lê a embarcação do transporte.
Substitui a ideia de um "modo de trabalho" guardado na sessão — **a atuação de um trabalho é propriedade do
trabalho** —, e o código já sabia disso num lugar: quem resolve concessão hoje fixa `AGENCIAMENTO` no próprio
corpo da função, porque vender travessia é agenciamento e isso não depende de estado.

### 5.7 O rearranjo por cadeia logística [alvo]

Abarcar uma cadeia nova passa a ser **um ato declarativo em código, mais cadastro no negócio**: declara-se a
atuação e as atividades dela (fechado, enumerável); a empresa cadastra os cargos e delega. Não se toca
`Usuario.Papel`, não se reescreve a política, não se mexe na forma do vínculo. Duas propriedades sustentam
isso, e as duas já são regime — só trocam de portador: **o que concede é tipo, o que registra é dado**; e
**derivação em vez de estado** — atuação em vigor, cargo em vigor, escopo e `operante` (D6) se deduzem a cada
leitura, e o que não se guarda não divergir.

## 6. Gestão da informação: o que se registra e o que se infere

É o eixo do enunciado, e o que ele pede é uma régua explícita. A que o projeto já usa, dita em uma linha:
**registra-se o ato — quem o fez e quando —, e todo o resto se infere dos ids que o ato ancora.**

### 6.1 O que hoje se registra, medido

`MetadadosPassagem`
([`MetadadosPassagem.kt:25-33`](../../app/src/main/java/dev/matheus/fluviapp/domain/passagem/MetadadosPassagem.kt))
guarda **`status`, `funcionarioId`, `agenciaId`, `criadoEm`, `alteradoEm`** e o `embarque`; `CarimboEmbarque`
(`:47-51`) guarda **`porId` e `em`**. Os dois ids da emissão *"saem do vínculo ativo de quem emite"* e nunca são
digitados — o KDoc registra por quê: um campo de agência no formulário seria a chance permanente de discordar
de quem está logado.

Note o que **não** está lá, e é decisão: nome, cargo, atuação, nome da agência, nome de quem embarcou. D5
confirma a régua — só quem emitiu, e o resto se deriva.

### 6.2 A cadeia de inferência que D5 assume

```
passagem.funcionarioId  →  funcionario  →  vinculo (empresaId, cargoId)  →  cargo.atividades  →  atuações
passagem.agenciaId      →  empresa      →  atuacoes/{ATUACAO}            →  concessões
```

Dela saem: a pessoa, a empresa, o cargo do momento da leitura, as atividades, a atuação, a agência do bilhete.
E, por agregação sobre os próprios bilhetes, saem ocupação, faturamento e a tarifa praticada — que já é assim
desde o ADR-0016 §7.2.

**O que faz essa cadeia ser segura é o `agenciaId` estar registrado.** Sem ele, uma pessoa que trocasse de
empresa mudaria **retroativamente** a agência de todos os bilhetes que já emitiu, porque a inferência responde
sempre *"agora"*. Com ele, a inferência de identidade é estável, e o vínculo pode mudar sem reescrever o
passado. É o exemplo mais limpo da régua: registra-se o mínimo que ancora, e o ancorado se deriva.

### 6.3 Onde a inferência não alcançava — os três limites, e como cada um fecha

Um dado inferido responde pelo estado de hoje. Três perguntas de auditoria não são de hoje, e cada uma exigiu
uma decisão.

**1. "O que esta pessoa podia fazer quando fez isto?"** As atividades vêm do cargo, e cargo passou a ser dado
da empresa: redelegar mudaria, retroativamente, a resposta sobre todo ato passado. **D7 fecha com
imutabilidade** — o regime que Rota e Viagem já usam, e pela mesma razão que o ADR-0022 dá para elas: um
`update` livre mudaria o significado de registros de terceiros depois do fato.

O que isso custa, dito com precisão: **redelegar deixa de ser editar e passa a ser reorganizar**. Cria-se o
cargo novo, inativa-se o antigo e **reaponta-se cada vínculo** — que é, em gestão de pessoas, exatamente o que
uma mudança de atribuições é. E enquanto ninguém reaponta, a pessoa continua com o cargo antigo, que é o
comportamento correto: autoridade não muda por efeito colateral de um cadastro.

Duas notas que o ADR precisa fixar. O cargo inativo **continua resolvendo para leitura** — é o mesmo par que a
Rota já tem (`ativo` filtra o que se oferece, não o que se lê), e a lição do defeito de 2026-08-17 é que os dois
lados precisam ser ditos. E a imutabilidade é **da lista de atividades**, não necessariamente do nome: o nome é
rótulo que gente lê, a lista é autoridade — *inclinação: nome mutável, atividades imutáveis*, porque corrigir
uma grafia não deveria obrigar a reorganizar o quadro.

**2. "Quem exerceu o poder emergencial?"** `alteradoEm` existe e `alteradoPorId` **não**: a passagem sabia
*quando* foi alterada e não sabia *por quem*. Enquanto editar era coisa do dono ou do supervisor, o instante
quase bastava; com `gerenciar_passagem` sendo excepcional por definição (D2), a exceção sem autor era o único
ponto do modelo em que o fato não era recuperável de forma nenhuma. **D8 fecha: registra autor.**

**Limite que fica declarado**, porque autor e instante não o cobrem: *o que* mudou não é recuperável. O
documento é sobrescrito e o valor anterior não sobrevive em lugar nenhum. Depois de D8 sabe-se **que** houve
exceção, **quem** a exerceu e **quando** — e reconstruir o conteúdo anterior exigiria histórico, que não existe
e não está sendo pedido. Declarar isso é o que impede a auditoria de prometer mais do que entrega.

**3. "Quem embarcou, comparável a quem emitiu?"** A emissão era atribuída ao **funcionário** e o embarque ao
**uid** (`CarimboEmbarque.porId`): dois espaços de id para dois atos da mesma pessoa, o que obrigava o salto
`uid → users/{uid}.funcionarioId` e fazia uma contagem por pessoa não fechar por chave. **D9 fecha unificando no
`funcionarioId`.**

Duas consequências, e a segunda é a mais interessante. O elo `uid → funcionarioId` **já existe como caminho
conhecido** no sistema, então atribuir o ato à pessoa da operação não deixa a autoria sem prova — muda a chave
registrada, não a verificabilidade. E, como papel puro de plataforma **não tem funcionário**, unificar torna o
ato de negócio **exclusivo de quem tem registro na operação**: um `ADM` deixa de poder carimbar embarque. Isso
não é efeito colateral a tolerar — é o que o ADR-0016 já dizia ao afirmar que `ADM`/`GESTOR` não emitem
passagem, agora valendo para todo ato de negócio, e é o fecho do `podeConfirmarEmbarque` = *qualquer papel
conhecido* que a §4.2 media como o extremo.

### 6.4 A régua que sai daí, e a peça que ela pede

Se atividade é a unidade da concessão, ela é também a unidade do rastro: **toda atividade exercida sobre dado de
terceiro deixa ato registrado — autor e instante —, e toda atividade sobre dado próprio já está ancorada no ato
que o criou.** É por isso que `emitir_passagem` não precisa de nada novo (a passagem *é* o registro), o embarque
já tem carimbo, o check-in nasce com um, e `gerenciar_passagem` — a única que alcança o que não é seu — era a
que não deixava rastro.

Com D8 e D9 juntos, os quatro atos passam a ter a **mesma forma**: `(quem, quando)`, com o mesmo espaço de id. É
uma peça só de domínio — um **carimbo** — usada quatro vezes, em vez de um par de campos planos aqui e um
sub-objeto ali. O `CarimboEmbarque` de hoje já é exatamente essa forma, e o KDoc dele já dá a razão de ser
sub-objeto em vez de dois campos soltos: *"o default vazio deixava representável o meio-preenchido — autoria sem
instante, ou o inverso"*. Generalizá-lo é aplicar o argumento que já está escrito aos outros três atos.

## 7. O que isto muda em decisões tomadas

**ADR-0016 §6.1 — "cargo é tipo de código" cai, e cai pela própria justificativa.** O ADR diz: *"o cargo
continua sendo tipo de código, não linha de catálogo — pelo mesmo motivo do tipo de embarcação: **cargo concede
permissão**. Um cargo cadastrável seria escalonamento de privilégio por cadastro."* A premissa era verdadeira e
deixou de ser: com a `Atividade` concedendo, cargo não concede mais, e a conclusão se solta. O argumento não
foi contrariado, foi **transferido** — e protege exatamente a mesma coisa. O que sobrevive inteiro do §6.1 é
*"cada valor declara a que atuação pertence"*, agora encarnado na atividade; e o par `(empresa, atuação)` no
vínculo fica sem necessidade.

**ADR-0010 — a política troca de coordenada e perde um eixo.** Continua única, mas deixa de responder por
`(papel, cargo)`: responde por **atividade**, com `(papel, cargo, atuações)` como insumo. E o eixo *"ação sobre
a Passagem **com posse**"* se dissolve: posse virou o alcance de `emitir_passagem` (D2), não uma coordenada.

**ADR-0015 §8 — a tese do eixo aberto se confirma e muda de portador.** *"Amanhã quem faz check-in, quem valida
embarque"* estava certo; o que cresce, porém, é a **atividade** — e o cargo passa a crescer **no cadastro**, sem
tocar código.

**ADR-0012 — a premissa do embarque cross-empresa muda, e a conclusão fica mais forte.** O ADR justifica
`podeConfirmarEmbarque` = qualquer papel conhecido dizendo que *"quem faz o check-in é a arrendatária, que é
**outra empresa**"*. Aqui ela é a mesma parte, com outra atuação. Conferir bilhete continua não se recortando
por quem vendeu, mas passa a ter dono — e a ser **delegado a quem faz o trabalho** em vez de concedido a todos.

**ADR-0012 — o `CHECADA` deixa de ser hipótese.** `fazer_check_in` e `confirmar_embarque` derivam de atuações
**distintas**: são dois eventos do ciclo de vida, não dois nomes para um. A FSM `EMITIDA → CHECADA →
EMBARCADA` ganha as duas mãos que a executam — e, pela régua da §6.4, o `CHECADA` nasce **com carimbo**.

**ADR-0012 — o carimbo do embarque troca de chave.** A dúvida 3 do ADR (*"o carimbo registra o uid; deve
registrar também a empresa do validador?"*) se resolve por outro caminho: **não é a empresa que entra, é o uid
que sai**. Com D9, `porId` passa a ser o `funcionarioId`, e empresa, cargo e atuação do validador passam a ser
**inferíveis** dele — que é a resposta que o ADR-0023 D8 daria, e mais barata que congelar mais um id.

**ADR-0015 §8.1 — a separação dos dois contextos chega ao registro.** O `uid` responde por credencial e o
`funcionarioId` por trabalho feito; depois de D9 **nenhum ato de negócio é atribuído a um `uid`**. O efeito
prático fecha uma frouxidão: papel puro de plataforma não tem funcionário, então não pode ser autor de ato de
negócio — o que o ADR-0016 já afirmava para a emissão passa a valer para embarque, check-in e gerência.

**ADR-0020 é reforçado.** *"Vocabulário é tipo, dado é entidade, não há terceira categoria"* é a régua que
decide a forma do cargo novo: entidade da empresa, não catálogo ressuscitado.

**ADR-0023 D8 se estende.** *No domínio nada é congelado* passa a valer também para cargo e atividade — e a §6.2
mostra o limite exato em que congelar continua sendo certo: os **ids que ancoram o ato**.

**Nada disto é migração.** O dado nasce de cadastro: os cargos de hoje reaparecem como o cadastro inicial de
quem agencia.

## 8. O que o ADR tem de escrever

Nada ficou em aberto no domínio. O que o ADR fixa, em ordem de dependência:

1. **`Atividade` como tipo fechado**, cada valor declarando a atuação de que deriva e o **alcance** (o caso de
   `emitir_passagem`, que carrega a posse). As treze perguntas de hoje entram como valores no mesmo ato — é o
   que torna o conjunto enumerável e o que prova a régua em casos que já existem.
2. **`Cargo` como entidade da empresa**: id, nome, `empresaId`, `ativo`, atividades. Com **D7**, a lista é
   imutável e redelegar é criar + inativar + reapontar; o cargo inativo continua resolvendo para leitura.
3. **`Vinculo(empresaId, cargoId)`**, um por empresa, e a morte do enum `Funcionario.Cargo`. A atuação passa a
   vir das atividades do cargo; o par `(empresa, atuação)` do ADR-0016 §6.1 não nasce.
4. **`atividadesDe(contexto)`** como a entrada única da política — interseção cargo ∩ atuações, fail-closed nas
   duas pontas —, mais as duas invariantes da delegação (§5.4) e a fronteira do que é indelegável (atividade de
   plataforma).
5. **O carimbo como peça única** (§6.4), com **D8** e **D9**: `(funcionarioId, instante)` para emissão,
   gerência, check-in e embarque. Aqui entram os dois limites declarados — *o que* mudou não é recuperável, e
   ato de negócio passa a exigir registro na operação.
6. **`operante` derivado** (D6) e as atuações portuárias acordando com as atividades que as justificam.

Duas precisões que o ADR resolve por escrito, e que este estudo deixa com inclinação em vez de decisão: se a
imutabilidade do cargo alcança o **nome** ou só a lista de atividades (§6.3.1 — *inclinação: só a lista*), e a
**granularidade** com que as treze permissões de hoje viram atividades, que é o que limita o que a delegação vai
conseguir recortar (D2 já fixou que `gerenciar_passagem` não se abre).