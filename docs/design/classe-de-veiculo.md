# A classe do veículo: o que o pedido de cadastro reabre

> **Estudo** (2026-09-03). Mapeia o domínio como ele está e prepara decisão; quem decide é o analista, e o
> que ele decidir vira ADR. Estudo não tem autoridade: onde ele e um ADR discordarem, **o ADR vence**.
>
> **Origem:** a [issue #4](https://github.com/kurtmatheus/fluviapp/issues/4), da apresentação institucional
> de 2026-09-03 — *"possibilidade do Supervisor cadastrar novas classes de Veículos como opção no menu
> principal"*.
>
> **Recorte deliberado: só domínio e a régua.** Tela, navegação e regra de servidor não entram — elas são
> consequência, e o §7 mede o custo delas sem propô-las.
>
> **A parte que já foi entregue** (`73fd609`): *liberar o preenchimento de modelo para todas as classes*, a
> segunda metade da issue. Ela não depende de nada deste estudo, e o §8 explica por quê.
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

## 2. O que a `ClasseVeiculo` é hoje **[hoje]**

`domain/passagem/ClasseVeiculo.kt` — enum de seis valores, com **três propriedades e um derivado**:

| Membro | O que decide | Onde é lido |
|---|---|---|
| `rotulo` | o texto na escolha e no bilhete | `ConteudosDeEscolha.kt:136`, `BilheteDigitalMapper.kt:51` |
| `exigeCilindrada` | se o formulário pergunta cilindrada, e se a tarifa depende dela | `FormularioDeVeiculo.kt:65`, `Veiculo.kt:47`, `ValidacaoEmissao.kt:133` |
| `exigeModelo` | se o modelo é **obrigatório** (desde `73fd609`, só isso) | `Veiculo.kt:46`, `ValidacaoEmissao.kt:132` |
| `ehPesado` | o recorte que separa a balsa das demais embarcações | `ClasseVeiculo.kt:36` (ADR-0016 §8) |

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

## 6. O caminho de escape canônico, aplicado

Quando um conceito parece exigir os dois lados, o projeto **não inventa uma terceira categoria** — ele
separa o **conjunto de valores possíveis** (tipo, deploy) do **exercício** (entidade, painel). Foi assim
com `Atuacao` × `empresas/{id}/atuacoes/{ATUACAO}` (ADR-0020 D5), e com `Cargo` depois que a `Atividade`
levou a concessão.

Aplicando o mecanismo do §5 à classe de veículo, a pergunta fica concreta e desconfortável:

**Se o eixo de agregação (§3.3) e as exigências (§2) continuam no tipo, o que sobra para o supervisor
cadastrar?**

Três respostas possíveis, e o que cada uma custa:

| Resposta | O que seria | O que custa |
|---|---|---|
| **a) só o nome de exibição** | um apelido de agência sobre uma classe canônica | é a alternativa que o ADR-0020 **já rejeitou por nome** — duas fontes para o mesmo vocabulário. E não atende o pedido: quem quer cadastrar "CARRO PEQUENO" quer cobrar diferente, não escrever diferente |
| **b) um gabarito de porte** | nasce um tipo fechado novo (o que a embarcação admite e a tarifa indexa), e a "classe" vira dado que **aponta** para ele | é o desenho do `Cargo`. Resolve o §3.1 (a classe nova nasce vendável porque herda o gabarito) e **não** resolve o §3.3 sozinho: duas classes com o mesmo gabarito continuam partindo a série, a menos que a agregação passe a ser pelo gabarito |
| **c) a classe inteira vira entidade** | com `exigeModelo`, `exigeCilindrada`, porte e ícone como campos | é o que o pedido literalmente pede. Custo em §7, e o §3.3 fica em aberto: a régua da tarifa passa a ser configurável, e a série passa a depender de disciplina de cadastro |

A **(b)** é a que segue o precedente. Ela também é a que exige a pergunta mais incômoda: se o gabarito é o
que decide oferta e preço, **ele** é a classe, e o que o supervisor cadastraria é um rótulo comercial — o
que devolve a discussão à **(a)**.

## 7. O custo, se a decisão cair **[cai]**

Medido, não estimado. Não é plano — é o preço a saber antes de decidir.

**No domínio.** `ClasseVeiculo` deixa de ser enum; `TipoEmbarcacao.classesAdmitidas` deixa de ser
`Set<ClasseVeiculo>` e vira conjunto de ids (ou some, se o porte assumir); `Veiculo.pendencias()` passa a
ler campos de um cadastro; `ehPesado` precisa de portador. O `when` exaustivo de ícone perde a proteção do
compilador e ganha um `else`.

**Na fronteira.** `VeiculoDocumento` grava hoje o `name` do enum. Com entidade, grava id — e **os
documentos já escritos carregam o nome**. Isto não é migração de produção (o app é portfólio, sem dado
real), mas é reescrita de codec e de fixtures.

**Na plataforma.** Nasce coleção, codec, porta, repositório, regra de servidor e suíte de emulador — a
anatomia completa de uma entidade, como Localidade e Porto.

**No menu.** Nova `SecaoMenu` (+ string + ícone), `AcaoMenu`, braço no `when` de
`PermissoesUsuario.podeAcessar`, entrada em `MenuDaAtuacao.secoesDa`, entrada em `SECOES_REVITALIZADAS` e
destino no `when` de `navegar`. Mais os testes: `AcaoMenuTest`, `MenuDaAtuacaoTest`,
`EscopoRevitalizadoTest`, `PermissoesUsuarioTest`, `PainelRevitalizadoTest`.

**E o preço que o ADR-0020 assumiu na direção contrária**, que aqui se paga de volta: *"acrescentar um
valor de vocabulário passa a exigir deploy… é aceitável porque, em todos os casos medidos, o valor novo já
exigia código para significar alguma coisa"*. Se a classe voltar a ser dado, o deploy sai — e volta a
pergunta de o que o valor novo significa sem código.

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

1. **Qual é a demanda real.** Falta uma classe específica — e aí a resposta é um valor novo no enum, que é
   deploy e não arquitetura —, ou falta *poder cadastrar sem depender de deploy*? A pergunta não é retórica:
   das seis classes de hoje, `VAN` e `SUV` entraram por pedido, e entraram assim.
2. **De quem seria a classe.** O pedido diz `SUPERVISOR`, logo a empresa. Mas `Veiculo` é **pool
   compartilhado**, com placa como chave natural e `agenciaIds` como *"assinatura das agências que já o
   atenderam"* (`Veiculo.kt:35`): um veículo comum a várias agências apontando para uma classe que pertence
   a uma delas é contradição de modelo. A forma do pool diz **plataforma**, como Localidade e Porto — e aí
   o pedido é atendido pela metade, porque o supervisor não cadastra.
3. **O que acontece com a série** (§3.3). Aceitar que ela se parta, ou pagar identidade estável — id em vez
   de nome, e agregação pelo que não muda?
4. **Se o mecanismo do `Cargo` se aplica** (§5, §6b): existe um "porte" que possa levar embora o
   comportamento, deixando a classe como registro? E se existir, **ele** não passa a ser a classe?

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
