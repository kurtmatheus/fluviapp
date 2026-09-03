# A classe do veículo: o que o pedido de cadastro reabre

> **Estudo** (2026-09-03). Mapeia o domínio como ele está e prepara decisão; quem decide é o analista, e o
> que ele decidir vira ADR. Estudo não tem autoridade: onde ele e um ADR discordarem, **o ADR vence**.
>
> **Origem:** a [issue #4](https://github.com/kurtmatheus/fluviapp/issues/4), da apresentação institucional
> de 2026-09-03 — *"possibilidade do Supervisor cadastrar novas classes de Veículos como opção no menu
> principal"*.
>
> **Recorte deliberado: só domínio e a régua.** Tela, navegação e regra de servidor não entram — elas são
> consequência; o §7 mede o custo delas sem propô-las, e o §9 devolve à decisão o único ponto de tela que
> nasce daqui.
>
> **A parte que já foi entregue** (`73fd609`): *liberar o preenchimento de modelo para todas as classes*, a
> segunda metade da issue. Ela não depende de nada deste estudo, e o §8 explica por quê.
>
> **Revisado em 2026-09-03, no mesmo dia**, depois de a primeira versão ser **rejeitada no enquadramento**.
> Ela oferecia três saídas — apelido, gabarito, entidade — e as três eram catálogo disfarçado ou cadastro.
> A direção do analista foi outra, e é a do §6: *"melhor não planejar como entidade nem catálogo, mas unir
> o poder do enum ao registro de classe… no final, a classe é só registro"*, com o fato operacional de que
> há **pelo menos dezesseis** classes, cada uma com o seu valor, definidas na operação. **A lista veio no
> mesmo dia** e está no §6.4: são **dezessete**, e ela corrige o desenho do §6.
>
> **FECHADO em 2026-09-03 → [ADR-0031](../adr/0031-classe-de-veiculo-natureza-e-casco-por-exclusao.md).**
> As três perguntas do §9 foram respondidas, e o ADR **supera este estudo em dois pontos**: o §6.4 concluía
> por **dois eixos** (porte × propulsão) e sobrou **um** — a natureza —, porque o casco passou a admitir
> **por exclusão** (D4) e o porte perdeu o consumidor que o justificava (D6); e o §6.4 pedia renomear a
> classe `Lancha`, o que foi recusado (D5). O resto do documento vale como o caminho até a decisão.
>
> Marcadores: **[hoje]** o que está no ar · **[alvo]** o que o pedido pede · **[cai]** o que teria de sair.

---

## 1. O pedido encontra uma decisão, não um vazio

O pedido chega como funcionalidade nova, e é aqui que o estudo tem de começar sendo desagradável: **a
pergunta já foi respondida, e a resposta foi não.**

`docs/design/dominio-passagem.md` §11.5 registra, como direção do analista de **2026-08-01**:

> Se a agregação por classe é o ativo, então **a classe do veículo não pode ser texto de catálogo
> editável**.

E a lista de decisões do mesmo documento (§11, item 8) a dá como aceita: *"A classe do veículo vira tipo
fechado do domínio — aceito em direção (§11.5/§11.6)"*. O [ADR-0018](../adr/0018-agregado-passagem-participantes-modo-e-lancamentos.md)
D7 e o [ADR-0023](../adr/0023-passagem-por-categoria-e-referencia.md) D4 a implementaram, e ela está no ar
desde `5580b48`.

Isso não desqualifica o pedido. Pedido de apresentação nasce de quem opera, e quem opera é quem sente falta
— o mesmo canal trouxe o modelo do caminhão, que era um defeito real. Mas muda o que este estudo é: não é
o mapa de um terreno virgem, é **a decisão de 2026-08-01 posta contra o pedido de 2026-09-03**, para que
reabri-la seja um ato explícito e não um esquecimento.

O que o pedido tem a seu favor, e que a decisão antiga não pesou: ela foi tomada **antes** de existir
painel de empresa, cargo de supervisor e a ideia de que a estrutura de uma empresa possa ser dela. É
possível que o mundo tenha mudado sob a decisão — é isso que o §5 e o §6 testam.

### 1.1 A pergunta estava errada **[revisão de 2026-09-03]**

A primeira versão deste estudo pôs a questão como *tipo ou entidade*, e por isso todas as suas saídas
custavam caro: uma coleção, um CRUD, uma regra de servidor, uma seção de menu — ou então um apelido que o
ADR-0020 já havia rejeitado por nome.

A pergunta certa é **onde mora o comportamento**. Feita assim, ela não põe a decisão de 2026-08-01 em
xeque: **confirma-a por outro caminho**. A classe continua sendo tipo fechado, e continua não sendo
catálogo. O que muda é que ela deixa de ser uma *tabela de propriedades* e passa a ser o que o analista
disse que ela é — **um registro**: uma lista de nomes com identidade estável, cujo comportamento é lido de
uma família acima dela.

E o §2 mostra que o código já caminhou sozinho nessa direção: das quatro coisas que a classe governava
quando foi desenhada, **duas não são mais lidas por ninguém**.

## 2. O que a `ClasseVeiculo` é hoje **[hoje]**

`domain/passagem/ClasseVeiculo.kt` — enum de seis valores, com **três propriedades e um derivado**. A
coluna que importa é a última:

| Membro | O que decide | Leitores em produção |
|---|---|---|
| `rotulo` | o texto na escolha e no bilhete | **2** — `ConteudosDeEscolha.kt:136`, `BilheteDigitalMapper.kt:51` |
| `exigeCilindrada` | se o formulário pergunta cilindrada | **3** — `FormularioDeVeiculo.kt:69`, `Veiculo.kt:47`, `ValidacaoEmissao.kt:133` |
| `exigeModelo` | se o modelo é **obrigatório** (desde `73fd609`, só isso) | **2** — `Veiculo.kt:46`, `ValidacaoEmissao.kt:132` |
| `ehPesado` | *(o recorte que separaria a balsa — ADR-0016 §8)* | **0** |

### 2.1 Dois membros já morreram, e ninguém reparou **[hoje]**

- **`ehPesado` não tem leitor** (`ClasseVeiculo.kt:42`). O recorte da balsa acabou sendo feito por
  **pertencimento explícito** em `TipoEmbarcacao.classesAdmitidas`, classe a classe, e o derivado que
  existia para isso nunca foi consultado.
- **`tarifaMotoBase` não tem chamador em produção** (`CalculoTarifa.kt:32`; só `CalculoTarifaTest`). Era o
  último ponto em que a classe tocava dinheiro, e ele caiu quando **preço virou I/O** (2026-08-11): a
  emissão não calcula valor, o operador informa o praticado.

**Isto é a medição que sustenta a frase do analista.** A classe já não carrega preço, já não carrega o
recorte da balsa, e desde `73fd609` já não decide o que a tela mostra. O que restou dela é rótulo, duas
perguntas de formulário e a identidade — ou seja: **ela já é quase só registro**, e o que sobra de
comportamento não pertence a cada classe, e sim a um punhado de famílias (§6).

E, acima de tudo, ela é **conteúdo de outro tipo**: `TipoEmbarcacao.classesAdmitidas: Set<ClasseVeiculo>`
(`domain/viagem/TipoEmbarcacao.kt:33`). A execução do ADR-0020 registrou exatamente isto ao explicar por
que ela precisou nascer junto:

> **`ClasseVeiculo`** — sem ela `TipoEmbarcacao` não tem o que admitir — a regra do D4 **é** um conjunto de
> classes.

### Os nove consumidores medidos

| Camada | Arquivo | O que faz com a classe |
|---|---|---|
| domínio | `domain/veiculo/Veiculo.kt:28,46-47` | é o campo `tipo`, e é quem responde `pendencias()` |
| domínio | `domain/viagem/TipoEmbarcacao.kt:33,65` | `classesAdmitidas` e `admite(classe)` |
| domínio | `domain/passagem/CalculoTarifa.kt:32` | `tarifaMotoBase(cilindradaCc)` — a moto por faixa de 100 cm³ |
| fronteira | `services/.../documents/VeiculoDocumento.kt:48` | `ClasseVeiculo.de(texto("tipo")) ?: return null` — **recusa o documento** |
| lógica | `ui/viewmodel/helpers/passagem/ValidacaoEmissao.kt:132-133` | pergunta ao tipo o que cobrar |
| apresentação | `ui/states/passagem/EmissaoUiState.kt:191` | `VeiculoEmEdicao.classe` |
| apresentação | `ui/states/passagem/RoteiroDaEmissao.kt:38,100-104` | é o **passo 2** do fluxo, e decide o passo 3 |
| apresentação | `ui/screens/.../ConteudosDeEscolha.kt:133,172` | a lista de botões e o `when` **exaustivo** de ícone |
| apresentação | `ui/viewmodel/helpers/passagem/BilheteDigitalMapper.kt:51` | o rótulo que vai ao bilhete |

Vale reter dois desses, porque são os que mais doem numa migração: o `when` de ícone é **exaustivo e
protegido pelo compilador** (o comentário em `ConteudosDeEscolha.kt:145` diz que é de propósito — classe
nova *"vai parar aqui e cobrar a decisão de como ela se mostra"*), e o codec **recusa a passagem inteira**
se a classe for desconhecida.

## 3. A cascata de três réguas

O projeto não tem uma régua para isto; tem três, acumuladas em ordem cronológica. A classe teria de passar
por todas.

### 3.1 A forma — [ADR-0020](../adr/0020-fim-do-catalogo-e-o-contexto-do-painel.md) D1

> **Vocabulário que o código consome é *tipo*. Dado que o negócio cria é *entidade*. Não há terceira
> categoria.**

O teste operacional que essa régua embute é: **o que acontece se alguém criar um valor novo?** O `Catalogo`
morreu por causa da resposta — um documento novo caía no `else` e o app seguia *"em silêncio"*: *"o catálogo
promete uma extensibilidade que o código não honra, e falha fail-open"*.

**Aplicado à classe de veículo, hoje:** uma classe cadastrada nasceria admitida por **nenhum**
`TipoEmbarcacao` — logo invendável em qualquer embarcação —, sem ícone e sem regra de cilindrada. É o
*"Catamarã"* do ADR-0016 §8 um nível abaixo:

> Um "Catamarã" cadastrado que só produz oferta vazia não é extensibilidade; é uma linha inerte com
> aparência de configuração.

Duas observações honestas contra esse veredito, para não fazer disso um julgamento de uma parte só:

- o modo de falha aqui é **fail-closed**, não fail-open — a classe nova não vende, mas também não vende
  errado. É melhor do que o `DOCUMENTO` sem máscara, que era o caso que matou o catálogo;
- o ADR-0020 rejeitou nominalmente a saída de meio-termo (*"catálogo como fallback do enum… cria duas
  fontes para o mesmo vocabulário e reintroduz o fail-open"*), mas rejeitou-a para **vocabulário com
  comportamento**. Se o comportamento sair da classe, a rejeição não a alcança — que é exatamente o que o
  §5 explora.

### 3.2 O poder — `empresa-com-duas-atuacoes.md` §5.1

> **o que concede é tipo, o que registra é dado.**

Esta é a régua que **virou** o `Cargo`, e é o precedente que o pedido pode invocar. Ver §5.

A classe de veículo **não concede permissão**. Ela concede outra coisa: **preço e exigência**. Se essa
concessão conta como "poder" para efeito da régua é uma das perguntas abertas (§9), e o §3.3 é o argumento
de que conta.

### 3.3 A série histórica — `dominio-passagem.md` §11.5

É o argumento mais forte contra, e o que o pedido não enxerga, porque ele não é sobre a emissão — é sobre o
que a plataforma vende depois:

> O produto do modo veículo não é o cadastro da pessoa: é a série **contagem × classe × preço**. É o que a
> plataforma vende como informação.
>
> Uma entrada nova ("CARRO PEQUENO") ou um rename e **a série histórica se parte em duas sem ninguém
> perceber**.

Isso encontra o [ADR-0008](../adr/0008-relacionamentos-por-identidade.md) pelo outro lado, e o §11.5 já
fazia a ligação: *"relação viva se faz por identidade, não por nome — e agregação é relação"*. A classe
como enum tem identidade estável por construção (o `name`, que é o que o Firestore grava); a classe como
cadastro precisaria de id **e** de disciplina para nunca reaproveitá-lo.

**A revisão inverte o sinal deste argumento, e o fortalece.** Com o preço fora da classe (§2.1), os
dezessete valores não existem *apesar* de a série precisar deles — existem **precisamente para serem a
chave dela**. É a única coisa que a classe faz que nada mais faz: dizer *que espécie de veículo foi este*,
de forma que a soma de amanhã reconheça a de ontem. Identidade estável deixa de ser preferência de
engenharia e passa a ser a razão de o conceito existir.

O ADR-0013, ainda que superado na tabela, deixou o precedente do custo: `TarifaViagem.chave` era canônica, e
*"um valor fora dessa lista simplesmente não encontra célula"*.

## 4. O que a decisão de 2026-08-01 **não** pesou

Justiça com o pedido. Em 2026-08-01 não existiam:

- o **painel da empresa** (ADR-0022 é de 2026-08-07) — não havia o lugar onde um cadastro de supervisor
  moraria;
- o **cargo como coisa da empresa** — o estudo que o decidiu é de 2026-08-18;
- a **F9 inteira**, que só depois mostrou quantos lugares a classe governa.

Ou seja: a decisão foi tomada contra *"linha de catálogo editável por administrador"*, que era a forma que
existia então. O pedido de agora propõe outra coisa — **cadastro da empresa, pelo supervisor** —, e essa
forma não foi avaliada. É por isso que reabrir é legítimo, e não teimosia.

## 5. O precedente que atravessou ao contrário: o `Cargo`

`docs/design/empresa-com-duas-atuacoes.md` é a prova de que a régua do ADR-0020 **não é uma porta de mão
única**. O `Cargo` era tipo fechado, com um argumento explícito no ADR-0016 §6.1: *"cargo concede
permissão; um cargo cadastrável seria escalonamento de privilégio por cadastro"*. Ele virou **entidade da
empresa**. E virou sem contrariar nada:

> A premissa era verdadeira e deixou de ser: com a `Atividade` concedendo, cargo não concede mais, e a
> conclusão se solta. O argumento não foi contrariado, foi **transferido** — e protege exatamente a mesma
> coisa.

**O mecanismo, que é o que interessa aqui:** não se afrouxou a régua; **tirou-se o poder de dentro do
conceito** e pôs-se num tipo fechado novo (`Atividade`). O que sobrou — o nome, a estrutura de quem faz o
quê — não tinha mais comportamento, e aí virou dado por consequência.

E o "valor novo nasce inerte", que no `TIPO_EMBARCACAO` era o defeito fatal, ali virou comportamento
correto:

> cargo sem atividade passa a ser o estado que não concede nada. É mais fiel ao negócio: **existe função que
> registra papel e não dá poder de sistema**.

A diferença entre os dois casos é a que este estudo tem de aplicar à classe de veículo: **um cargo sem
atividade é um fato de negócio real; um Catamarã sem oferta é uma linha inerte com cara de configuração.**

## 6. A via: o enum **é** o registro, e o comportamento sobe para a família **[alvo]**

> **Direção do analista, 2026-09-03**, que substitui as três saídas da primeira versão deste estudo:
> *"melhor não planejar como entidade nem catálogo, mas unir o poder do enum ao registro de classe… no
> final, a classe é só registro"* — havendo **pelo menos dezesseis** classes, cada uma com o seu valor,
> definidas na operação.

O mecanismo do §5 se aplica **sem** a segunda metade dele. No `Cargo`, tirar o poder de dentro do conceito
o fez virar dado — mas foi *consequência*, não requisito: o que a régua exigia era que **poder e registro
deixassem de morar na mesma palavra**. Aqui a separação basta, e o registro pode continuar sendo um enum,
porque ele tem uma propriedade que o cargo não tinha: **é a chave de uma série histórica** (§3.3), e séries
não convivem com identidade que se renomeia.

Então: cada um dos dezessete valores declara **uma** coisa — a família a que pertence —, e
`exigeCilindrada`, `exigeModelo` e a admissibilidade por casco passam a ser lidos da família, que tem três
ou quatro valores.

### 6.1 A pergunta de eficiência, respondida com número

*"Um enum de dezesseis valores é o mais eficiente?"* — a pergunta certa não é **dezesseis é demais**, é
**dezesseis vezes o quê**. O custo de um enum não cresce com o número de valores; cresce com o produto
**valores × comportamentos**.

Hoje, com seis valores, existem **três** assinaturas de comportamento distintas:

| Assinatura | Classes | Quantas |
|---|---|---|
| exige cilindrada · exige modelo | `MOTO` | 1 |
| não exige cilindrada · exige modelo | `CARRO`, `VAN`, `SUV` | 3 |
| não exige nada · só a balsa leva | `CAMINHAO`, `CARRETA` | 2 |

**Metade da tabela já é repetição.** Com dezessete seria em torno de quatro quintos — e não porque
as dezessete sejam iguais, mas porque **elas não diferem naquilo que o código lê**. Diferem no valor, e o
valor saiu da classe em 2026-08-11 (§2.1).

O que de fato multiplicaria por dezessete, se nada mudar:

- dezessete linhas de **quatro** colunas na tabela do enum;
- dezessete ramos no `when` de ícone (`ConteudosDeEscolha.kt:172`) para talvez quatro ícones distintos;
- cerca de **vinte e oito** pertencimentos escritos à mão nos três conjuntos de `TipoEmbarcacao` — e sobre
  esses o próprio código já confessa a fragilidade (`TipoEmbarcacao.kt:35-38`): a atribuição é *"leitura
  minha sobre o que cada casco carrega, não decisão registrada"*. Com seis é uma leitura; com dezessete
  são vinte e oito apostas, e nenhuma o compilador confere.

Com o eixo no meio, os mesmos dezessete custam **dezessete linhas de uma coluna**, três ou quatro ramos
de ícone e três ou quatro pertencimentos por casco. O comprimento deixa de importar: o Kotlin não se
incomoda com dezessete constantes, e quem abre o arquivo passa a ver **uma lista de nomes**, que é o que um
registro deve parecer.

### 6.2 O precedente de forma já existe no projeto

`Acomodacao` (`domain/passagem/Acomodacao.kt`) é exatamente este formato: um enum cujos valores declaram um
**conjunto de outro enum** (`tiposPermitidos: Set<TipoPassagem>`) e derivam o comportamento dele —
`admite(tipo)` e `temEscolhaDeTipo`. A forma existe e é a convenção da casa; falta aplicá-la na altitude
certa.

Vale notar que `Acomodacao` sofre do mesmo mal em miniatura: `SUITE` e `CAMAROTE` são linhas **idênticas**
exceto pelo rótulo. Com três valores isso passa despercebido. É a mesma redundância que, com dezessete,
deixa de passar.

### 6.3 O que a via preserva, e que catálogo e entidade perderiam

- **identidade estável** — o `name` do enum é o que o Firestore grava, e a série de §3.3 continua íntegra
  sem depender de disciplina de cadastro;
- **fail-closed na fronteira** — `VeiculoDocumento.kt:48` recusa o documento cuja classe não existe, e
  continua recusando;
- **exaustividade do compilador** — mas sobre **três famílias**, onde ela é útil, em vez de sobre dezessete
  nomes, onde é burocracia;
- **nada de coleção, codec, porta, CRUD, regra de servidor ou seção de menu** (§7).

E a decisão de 2026-08-01 sai **confirmada**: a classe segue tipo fechado, segue não sendo catálogo
editável. O que muda é que ela para de fingir ser uma tabela de propriedades.

### 6.4 A lista da operação, e a correção que ela impõe **[alvo]**

Lista dada pelo analista em 2026-09-03, em resposta à pergunta *"quais são as dezesseis?"*. São **dez de
uma vez, mais uma acrescentada em seguida** — `Van` já existia, e o total fecha em **dezessete**:

| Já existem (6) | A acrescentar (11) |
|---|---|
| Carro · Moto · Van · SUV · Caminhão · Carreta | Trator · Trailer · **Lancha** · Carretilha · Jet-Ski · Quadriciclo · Empilhadeira · Retroescavadeira · Motorhome · Ônibus · **Carreta Cavalinho** |

A lista não é só um número maior. Ela **corrige o desenho do §6**, e traz três achados que nenhum documento
anterior tinha.

#### As quatro naturezas

*Agrupamento é leitura minha sobre a lista, não decisão registrada — é exatamente o tipo de leitura que o
§6.1 diz que não deve ficar espalhada por vinte e dois pertencimentos escritos à mão.*

| Natureza | Da lista |
|---|---|
| automotor rodoviário | Ônibus · Motorhome · Van · Carro · SUV · Caminhão |
| motor medido em **cilindrada** | Jet-Ski · Quadriciclo · Moto |
| máquina autopropelida | Trator · Empilhadeira · Retroescavadeira |
| **rebocado — sem propulsão própria** | Trailer · Carretilha · Lancha · Carreta |
| **não sei classificar** | Carreta Cavalinho |

A quarta linha é a que informa mais: **trailer, carretilha e lancha não entram andando**. É fato
operacional — manobra, rampa, quem conduz —, e hoje o domínio não tem onde registrá-lo.

E a quinta linha vale mais do que parece. *"Cavalinho"* remete ao **cavalo mecânico**, que é a unidade
**tratora** — motorizada —, enquanto *"carreta"* nomeia o semirreboque, que é **rebocado**. O nome carrega
as duas metades, e sem a definição da operação eu não sei em qual linha pô-la.

**Isso não é lacuna do estudo; é o argumento dele.** Se quem escreve o código não consegue derivar a
natureza a partir do nome, então essa natureza **tem de ser declarada** — que é exatamente o que o eixo
abaixo faz, e o que os vinte e dois pertencimentos escritos à mão não fariam: eles registrariam o meu
palpite como se fosse regra.

#### A correção: **o eixo não é um só**

O §6 propôs *uma* família (porte). A lista quebra isso num caso específico, e o caso é a prova:

> o **jet-ski exige cilindrada** *e* **é rebocado**. O trailer é rebocado e não tem cilindrada; a moto tem
> cilindrada e não é rebocada.

Duas propriedades que se cruzam não colapsam numa família única. **Isto responde a pergunta 1 do §9 — e
responde contra o que o §6 sugeria.** O que fecha é o eixo declarado em duas colunas pequenas:

- **porte** — quanto de convés ocupa. É o que o casco admite, e é o que substitui os pertencimentos
  escritos à mão em `TipoEmbarcacao.classesAdmitidas`;
- **propulsão** — entra andando × rebocado. É o que a lista trouxe de novo, e o que separa lancha, trailer
  e carretilha de tudo o mais.

Mais `exigeCilindrada`, que sobrevive como terceira coluna, pequena e óbvia (moto, quadriciclo, jet-ski).
Continua sendo muitíssimo menos que a tabela de hoje multiplicada por dezessete — e, ao contrário dela,
**cada coluna tem um critério que se pode discutir em vez de adivinhar**.

#### Uma colisão de vocabulário e uma armadilha de nome

- **`Lancha` já é `TipoEmbarcacao.LANCHA`.** Como classe de veículo ela é carga sobre carretilha; como tipo
  de embarcação, é o casco que transporta. É o mesmo choque que forçou `Navio` → `Embarcacao`
  ([ADR-0020](../adr/0020-fim-do-catalogo-e-o-contexto-do-painel.md) D4: *"gênero e espécie deixaram de
  disputar a mesma palavra"*). Com ironia mensurável: hoje `LANCHA.classesAdmitidas = emptySet()` — a
  lancha-casco não leva veículo nenhum, inclusive lancha.
- **Três nomes com o mesmo radical:** `Carreta`, `Carretilha` e `Carreta Cavalinho` — semirreboque de
  caminhão, carrinho de rebocar lancha, e uma terceira configuração que a operação distingue. Numa lista de
  dezessete botões isso é erro de seleção esperando acontecer, e ele vai parar **na série de agregação**
  (§3.3) sem deixar rastro. É a primeira razão de forma para o §9.3 (o totem) não ser detalhe de UI.

#### Duas perguntas de negócio que a lista levanta

**Ônibus e motorhome carregam pessoas dentro.** Um ônibus embarcado ocupa uma vaga de veículo e **quantos
lugares de passageiro**? A capacidade da embarcação é contada nos dois eixos ([ADR-0018](../adr/0018-agregado-passagem-participantes-modo-e-lancamentos.md)
D8 — *o tipo diz o que cabe, a capacidade diz quanto*), e hoje nada liga um ao outro. A pergunta não existia
enquanto a maior classe era a carreta.

#### O custo de fazer isto no formato de hoje

Acrescentar as onze sem mexer na forma custa onze rótulos e cerca de **cinquenta e cinco decisões de
comportamento escritas à mão**: vinte e duas de propriedade, onze ramos de ícone e **vinte e duas de
pertencimento** — cada classe nova entra ou não no conjunto do Ferry Boat e no do Navio.

As vinte e duas últimas são as piores, porque **não há critério registrado para tomá-las**. O teste é
direto: *um Navio leva retroescavadeira? leva carretilha com lancha?* Cada resposta dessas, hoje, seria uma
linha de código sem argumento por trás — e é o que o porte, declarado, resolve de uma vez.

## 7. O custo desta via, e o único preço real

O custo é **pequeno e local**, e é o argumento prático a favor dela. Comparado ao que a entidade exigiria
— coleção, codec, porta, repositório, regra de servidor, suíte de emulador, seção de menu com seis paradas
e cinco suítes de teste —, aqui não nasce **nenhuma** dessas peças.

**No domínio.** Nasce o tipo da família; `ClasseVeiculo` perde três colunas e ganha uma;
`TipoEmbarcacao.classesAdmitidas` passa a listar famílias em vez de classes (de dez pertencimentos para
seis, e de ~vinte e dois para ~seis quando as dezessete existirem); `ehPesado` ou ganha leitor ou sai
(§2.1); `Veiculo.pendencias()` e `ValidacaoEmissao` passam a perguntar à família — **uma indireção, mesma
forma**.

**Na fronteira.** `VeiculoDocumento` continua gravando o `name` da classe. **Nada muda**: é a vantagem de o
registro seguir sendo enum.

**Na apresentação.** O `when` de ícone desce para a família. A escolha do passo 2 da emissão ganha um
problema novo, que é de UI e está no §9: dezessete botões numa tela de totem.

**O único preço real: classe nova exige deploy.** Vale enfrentá-lo de frente, porque é a objeção que o
pedido original levanta com razão.

A resposta que o projeto já tem, do ADR-0020: *"acrescentar um valor de vocabulário passa a exigir deploy…
é aceitável porque, em todos os casos medidos, o valor novo já exigia código para significar alguma
coisa"*. Aqui há uma razão a mais, e ela é do negócio: **classe de veículo é fato da estrada, não da
agência.** "Bitrem" existe no Brasil, não na empresa X — e as duas classes que entraram por pedido (`VAN` e
`SUV`, ADR-0023 D4) entraram exatamente assim, por deploy, sem que ninguém sentisse falta de um cadastro.

Onde essa resposta **não** serviria: se cada agência definisse recortes próprios — se a A vendesse
"caminhão pequeno" onde a B vende só "caminhão". Aí a lista não seria da estrada. Mas note que, nesse
cenário, o que difere entre elas é o **preço**, que já é I/O desde 2026-08-11: a mesma lista de dezessete
atende as duas, e cada uma pratica o seu valor. É o §9 que precisa confirmar isso contra a operação real.

## 8. A metade que não depende disto: o modelo **[entregue]**

A issue #4 pedia duas coisas, e a segunda foi entregue em `73fd609` sem tocar em nada acima.

`exigeModelo` decidia **duas** coisas com a mesma flag — se o campo era obrigatório e se ele existia na
tela. Em carreta e caminhão isso virava proibição. Agora a flag mede só obrigação, o campo é oferecido em
toda classe, e `Veiculo.pendencias()` e `ValidacaoEmissao` ficaram como estavam.

A cilindrada **não** acompanhou, e a assimetria é a régua que ficou escrita: **some o campo que não se
aplica** (numa carreta, cilindrada não é opcional — é sem sentido) e **fica o campo que se aplica e apenas
não se cobra**.

Por que isso não arranha a decisão de 2026-08-01: o §11.5 fecha a classe **e**, na mesma frase, diz que o
livre continua livre — *"com o catálogo servindo, no máximo, para rotular e para o que é livre de fato
(modelo, cor)"*. Liberar o modelo é dar vazão à variedade **fora** do eixo de agregação. É o oposto de
abrir a classe.

## 9. O que fica para decisão

As quatro perguntas da primeira versão caíram com o enquadramento dela (§1.1) — *de quem é a classe*, *o
que acontece com a série* e *se o mecanismo do `Cargo` se aplica* estão respondidas em §6 e §3.3. Das três
que sobraram, **duas foram respondidas pela lista** de 2026-09-03 (§6.4):

- ~~*qual é o eixo da família*~~ → **são dois** (porte × propulsão), mais `exigeCilindrada`. O jet-ski é a
  prova, e a resposta contraria o que o §6 sugeria;
- ~~*quais são as dezesseis*~~ → **dezessete**, listadas no §6.4.

Ficam estas, e uma delas nasceu da própria lista:

1. **Os valores do porte.** Quantas faixas a operação distingue de fato, e por qual medida — comprimento,
   área de convés, peso? É a coluna que substitui os vinte e dois pertencimentos escritos à mão, e ela só
   vale a troca se tiver um critério dizível. *Retroescavadeira e carreta são o mesmo porte?* é a pergunta
   que separa uma coluna útil de um rótulo novo.
   **E junto vai uma definição que falta:** o que é a `Carreta Cavalinho` — unidade tratora (motorizada) ou
   semirreboque (rebocado)? O nome tem as duas metades, e ela é hoje a única da lista que não consigo pôr
   numa natureza (§6.4).
2. **Ônibus e motorhome contra a capacidade de passageiros** (§6.4). Um veículo que leva gente dentro
   ocupa vaga nos **dois** eixos que o ADR-0018 D8 conta, e hoje nada liga um ao outro. Pode ser que a
   resposta seja *ninguém viaja dentro do ônibus na travessia* — mas isso é decisão, e não está escrita.
3. **Como dezessete escolhas cabem no totem.** A classe é o **passo 2** da emissão
   (`RoteiroDaEmissao.kt:38`), e o ADR-0029 desenhou cada passo como *uma pergunta cuja resposta é um
   toque*. Dezessete botões numa tela não é escolha, é catálogo impresso — e com `Carreta`, `Carretilha` e
   `Carreta Cavalinho` na mesma lista, é também erro de seleção que contamina a série (§3.3). Ou a lista
   vem agrupada pelo porte, ou o **porte vira o passo e a classe o sub-passo** — o que o roteiro derivado
   já sabe fazer, porque é assim que o subtipo de gratuidade entra.

E fica **fora de decisão, mas dentro de execução**: `Lancha` como classe colide com `TipoEmbarcacao.LANCHA`
(§6.4). Não é escolha de arquitetura, é escolha de nome — e o precedente do `Navio` → `Embarcacao` diz que
ela se paga se não for feita na hora.

---

## Anexo — o que já foi decidido, e onde

| Decisão | Onde | Quando |
|---|---|---|
| A classe do veículo é tipo fechado do domínio | `dominio-passagem.md` §11.5/§11.6 | 2026-08-01 |
| A classe governa exigências (modelo, cilindrada) | ADR-0018 D7, ADR-0023 D4 | 2026-08-11 |
| Vocabulário é tipo, dado é entidade; não há terceira categoria | ADR-0020 D1 | 2026-08-02 |
| O tipo de embarcação admite um conjunto de classes | ADR-0016 §8, ADR-0020 D4 | 2026-08-05 |
| O que concede é tipo, o que registra é dado | `empresa-com-duas-atuacoes.md` §5.1 | 2026-08-18 |
| O modelo é oferecido em toda classe (obrigação segue no tipo) | issue #4, `73fd609` | 2026-09-03 |
| Preço é I/O — a emissão não calcula valor, e a classe não o carrega | ADR-0016 §7.2 · índice de vigência | 2026-08-11 |
| **Nem entidade nem catálogo: o enum é o registro, e o comportamento sobe para a família** | este estudo §6 | 2026-09-03 |
| **As dezessete classes**, e a correção que elas impõem: o eixo é **duplo** (porte × propulsão) | este estudo §6.4 | 2026-09-03 |
