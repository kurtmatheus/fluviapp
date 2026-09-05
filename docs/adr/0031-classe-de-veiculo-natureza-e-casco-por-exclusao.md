# ADR-0031: A classe de veículo — o enum é o registro, a natureza é a propriedade, e o casco admite por exclusão

**Status:** Aceita (decisões do analista em 2026-09-03) · **implementada** no mesmo dia ·
**emendada no D2 em 2026-09-05** pela operação (o jet-ski é rebocado; só a moto exige cilindrada)

**Estudo que preparou:** [`docs/design/classe-de-veiculo.md`](../design/classe-de-veiculo.md)

---

## Contexto

A [issue #4](https://github.com/kurtmatheus/fluviapp/issues/4), vinda da apresentação institucional, pediu
que o `SUPERVISOR` pudesse **cadastrar novas classes de veículo**. O estudo mediu o pedido contra o que já
estava decidido e encontrou uma resposta anterior, não um vazio: em **2026-08-01** já se havia fechado que
*"a classe do veículo não pode ser texto de catálogo editável"* — porque o produto do modo veículo é a
série **contagem × classe × preço**, e nome editável parte série sem deixar rastro.

A primeira versão do estudo foi **rejeitada no enquadramento**: ela punha a questão como *tipo ou entidade*,
e toda saída custava coleção, CRUD, regra de servidor e seção de menu. A direção do analista foi outra —
*"melhor não planejar como entidade nem catálogo, mas unir o poder do enum ao registro de classe… no final,
a classe é só registro"* —, com o fato operacional de que a operação distingue **dezessete** classes.

Três medições do código sustentam essa leitura, e nenhuma delas era conhecida quando o assunto foi decidido
em agosto:

1. **`tarifaMotoBase` não tem chamador em produção** (`CalculoTarifa.kt:32`). Desde *preço é I/O*
   (2026-08-11), a classe **não carrega preço**;
2. **`ehPesado` não tem leitor** (`ClasseVeiculo.kt:42`). O recorte da balsa acabou sendo feito por
   pertencimento explícito, classe a classe;
3. desde `73fd609`, `exigeModelo` já **não decide o que a tela mostra** — só o que ela cobra.

Ou seja: a classe **já é quase só registro**. O que falta é a forma parar de fingir o contrário.

## Decisão

### D1 — A classe continua tipo fechado; o enum **é** o registro

Nem entidade, nem catálogo. A decisão de 2026-08-01 sai **confirmada por outro caminho**: não se afrouxa a
régua do [ADR-0020](0020-fim-do-catalogo-e-o-contexto-do-painel.md) D1, muda-se a pergunta — de *tipo ou
entidade* para **onde mora o comportamento**.

O `Cargo` é o precedente, e aplica-se **sem a segunda metade dele** ([`empresa-com-duas-atuacoes.md`](../design/empresa-com-duas-atuacoes.md)
§5.1): o que a régua exigia era que **poder e registro deixassem de morar na mesma palavra**. Ali, tirar o
poder fez o cargo virar dado — mas isso foi consequência, não requisito. Aqui a separação basta, e o
registro segue enum, porque ele tem uma propriedade que o cargo não tinha: **é a chave de uma série
histórica**, e série não convive com identidade que se renomeia.

O que o enum entrega de graça, e que o cadastro cobraria caro: identidade estável (o `name`, que é o que o
Firestore grava), fail-closed na fronteira (`VeiculoDocumento.kt:48` recusa classe desconhecida) e
exaustividade do compilador.

### D2 — A **natureza** é a propriedade, e ela existe pela análise, não pela arrumação

Cada classe declara **uma** coisa: a sua natureza. Quatro valores:

| Natureza | Classes |
|---|---|
| **automotor rodoviário** | Carro · Van · SUV · Caminhão · Motorhome · Ônibus · Carreta Cavalinho |
| **motociclo** | Moto · Quadriciclo |
| **máquina autopropelida** | Trator · Empilhadeira · Retroescavadeira |
| **rebocado** — sem propulsão própria | Carreta · Trailer · Lancha · Carretilha · **Jet-Ski** |

A razão registrada é do analista, e é maior que a higiene do código:

> O sentido de ter natureza como propriedade vai ao encontro da futura **capacidade analítica** do sistema,
> bem como do **data-intensive**: guardar todas as informações possíveis.

Isto muda o estatuto da coluna. Ela não está ali para encurtar uma tabela — está ali porque *"quantos
rebocados atravessaram em agosto"* é uma pergunta que o negócio vai fazer, e que hoje não teria como ser
respondida sem alguém reclassificar dezessete nomes à mão. **A natureza é dado analítico que, de quebra,
organiza o código.**

**Efeito imediato: `exigeCilindrada` deixa de ser declarada e passa a derivar** — é `natureza == MOTOCICLO`,
e cobre moto, jet-ski e quadriciclo. Uma coluna a menos, e sem exceção. **Este parágrafo caiu dois dias
depois — ver a emenda logo abaixo.**

### Emenda ao D2 — a cilindrada volta a ser declarada (2026-09-05)

A operação corrigiu **dois fatos** ao ver o passo no aparelho, e os dois derrubam a derivação acima:

1. **o jet-ski é moto aquática no nome e carga rebocada na doca** — ele chega sobre a carretilha, como a
   lancha. Passa de `MOTOCICLO` a `REBOCADO`;
2. **o quadriciclo não exige cilindrada** — só a moto exige.

Com isso a natureza `MOTOCICLO` fica com duas classes e **exigências diferentes**, e nenhuma coluna
derivada responde por duas. `exigeCilindrada` volta a ser **declarada na classe**, com um único `true` em
dezessete linhas — que é a forma honesta de uma exceção: visível, e só ela.

Vale registrar o que a derivação usava como prova, porque é o erro de método e não o de fato: **o jet-ski**.
Ele parecia confirmar que *"motor medido em cilindrada"* era traço de família — e era justamente o valor que
estava na família errada. Corrigido o fato, o padrão se desfez sozinho.

O nome já denunciava a confusão: este documento chamava a natureza pelo **comportamento** (*"motor medido em
cilindrada"*) enquanto o código a chamava pela **espécie** (`MOTOCICLO`). Prevalece a espécie — é o que a
natureza sempre foi, e é o que sobra quando o comportamento sai dela.

**O D2 não é revogado — é confirmado no que ele afirmava de si.** A natureza existe **pela capacidade
analítica**, e organizar o código era o efeito colateral. Perdido o efeito colateral, a razão continua de pé:
*"quantos rebocados atravessaram em agosto"* segue sendo a pergunta, e agora com o jet-ski do lado certo dela.

### D3 — As dezessete classes

Onze entram; seis já existem (a `Van` da lista já estava no enum):

**Já existem:** Carro · Moto · Van · SUV · Caminhão · Carreta
**Entram:** Trator · Trailer · Lancha · Carretilha · Jet-Ski · Quadriciclo · Empilhadeira · Retroescavadeira · Motorhome · Ônibus · Carreta Cavalinho

**A `Carreta Cavalinho` é a unidade tratora — motorizada** (decisão do analista). O estudo não conseguia
classificá-la: *"cavalinho"* remete ao cavalo mecânico e *"carreta"* ao semirreboque, e o nome carrega as
duas metades. Fica registrada como **automotor rodoviário**, e o episódio vale como argumento a favor do D2:
**se o nome não deixa derivar a natureza, a natureza tem de ser declarada.**

### D4 — O casco admite **por exclusão**, e não por enumeração

Decisão do analista, e é ela que dissolve o custo que o estudo mediu:

> Balsa (Ferry Boat) leva tudo, melhor por exclusão que se bater com o resto; caso o navio leve mais algo,
> só incluir. **O domínio deve dar esse suporte** — ser revitalizado com base nisso.

| Casco | O que admite |
|---|---|
| **Ferry Boat** | **todas** — sem enumerar |
| **Navio** | **apenas** Carro e Moto |
| **Lancha** | **nenhuma** |

Isto **revisa o [ADR-0016](0016-dominio-da-plataforma.md) §8 em dois pontos**: a forma (`classesAdmitidas:
Set<ClasseVeiculo>` deixa de ser um conjunto enumerado) e o conteúdo (hoje o `NAVIO` admite `CARRO`, `MOTO`,
`VAN` e `SUV`; passa a admitir **carro e moto**). A regra pura e o efeito de *não oferecer o impossível*
seguem inteiros.

A forma que o D4 pede é um tipo de três estados — *todas · apenas(conjunto) · nenhuma* —, e é ele que faz
uma classe nova **nascer vendável na balsa sem ninguém decidir nada**. Era exatamente esse "nascer inerte" o
defeito que matou o Catálogo (ADR-0016 §8: *"um Catamarã cadastrado que só produz oferta vazia não é
extensibilidade"*); aqui ele não acontece.

### D5 — `Lancha` fica como nome de classe

O estudo apontou a colisão com `TipoEmbarcacao.LANCHA` e sugeriu renomear, pelo precedente `Navio` →
`Embarcacao`. **Recusado:** *"Lancha não vai confundir mais; o preço que se paga por virar entidade já foi
aceito."* O rename de 2026-08-04 já separou gênero de espécie — a **embarcação é entidade**, o **tipo é
valor** —, e a classe de veículo vive num terceiro espaço. Três `Lancha` em três papéis explícitos não
disputam a mesma palavra; disputariam se um deles ainda fosse ambíguo, e nenhum é.

### D6 — Comprimento, área de convés e peso ficam **fora**

O estudo propôs um **porte** medido como eixo. Decisão: *"vale nota lateral, mas não são relevantes da
operação no momento, podendo ser incluídas futuramente"*.

Com o D4 resolvendo a admissibilidade por exclusão, o porte **perde o consumidor que o justificava** — ele
existia no estudo para substituir os vinte e dois pertencimentos escritos à mão, e esses deixaram de
existir. Fica como candidato a coluna futura, e o §7 diz em que cenário volta.

**Isto supera a conclusão do §6.4 do estudo**, que dizia serem necessários **dois** eixos (porte ×
propulsão). Com o porte fora, sobra um: a natureza.

### D7 — O ônibus não cobra nada da capacidade de passageiros

O estudo levantou que ônibus e motorhome levam gente dentro, e perguntou se ocupariam vaga nos dois eixos
que o [ADR-0018](0018-agregado-passagem-participantes-modo-e-lancamentos.md) D8 conta. Resposta:
**irrelevante — todo embarque gera uma passagem.**

Quem viaja dentro do ônibus tem bilhete próprio, e portanto já é contado onde os passageiros são contados.
Não há nada a ligar: a invariante que resolve a pergunta é anterior a ela.

### D8 — A emissão ganha um passo, e o subpasso é a classe

A escolha vira **natureza → classe**, e não uma lista de dezessete botões. É o formato que o
[ADR-0029](0029-os-fluxos-da-emissao.md) já pede — *cada passo é uma pergunta, e a resposta é um toque* —, e
o roteiro é derivado, então inserir um passo é acrescentar um valor, não escrever um `if`.

Além do tamanho da lista, há uma razão de forma: `Carreta`, `Carretilha` e `Carreta Cavalinho` compartilham
radical, e num seletor único isso é erro de seleção que **contamina a série de agregação** sem deixar
rastro. Separadas por natureza, as três nunca aparecem juntas: a Carreta é rebocado, a Cavalinho é automotor,
e a Carretilha é rebocado — as duas que sobram na mesma tela são as duas que a operação de fato distingue.

### D9 — A classe é capacidade **compartilhada**, sem dono

*"Classe de veículo vira capacidade da plataforma e da agência, reutilizável pelas outras — como rota e
viagem."*

O critério do [ADR-0022](0022-painel-da-empresa-e-fases.md) D2 se aplica: **entidade sem dono é de todos.**
E há uma diferença que vale nomear em relação a Rota e Viagem: aquelas são pool **com concessão** — a
agência só vê o que lhe foi concedido —, enquanto a classe é pool **sem concessão**: toda agência dispõe de
todas, sem provisionamento. Não há lista de negadas, pela mesma razão que matou a do §7.1: *ver e vender são
a mesma pergunta*, e uma agência que não vende carreta simplesmente não emite carreta.

Por ser vocabulário do código, a plataforma a declara e a agência a usa — o compartilhamento é consequência
da forma, não uma peça a construir.

## Consequências

- **`ClasseVeiculo` perde colunas e ganha a natureza.** Saem `ehPesado` (sem leitor desde sempre, e sem
  função depois do D4) e — por decisão do analista durante a execução — `exigeModelo`, cuja alternativa
  estava registrada em *Alternativas futuras* e foi escolhida: o modelo é **oferecido a toda classe e cobrado
  em nenhuma**. `exigeCilindrada` chegou a sair também, e **voltou em 2026-09-05** pela emenda ao D2 — é a
  única coluna de comportamento declarada por valor, e tem um `true` só.
- **`TipoEmbarcacao` muda de forma, e é a mudança mais visível do ADR.** O `Set<ClasseVeiculo>` dá lugar ao
  tipo de três estados do D4; `admite(classe)` e `levaVeiculo` continuam existindo com a mesma assinatura, e
  quem os chama não muda.
- **O `when` exaustivo de ícone desce para a natureza** (`ConteudosDeEscolha.kt:172`): quatro ramos em vez
  de dezessete. Perde-se a cobrança do compilador *por classe* — que hoje obriga a decidir como cada uma se
  mostra — e ganha-se que dezessete classes não exijam dezessete ícones. É troca consciente.
- **A balsa passa a admitir classe nova sem decisão humana.** É *fail-open* no casco que leva tudo, e é
  deliberado: uma balsa carrega o que couber, e "o que couber" é a **capacidade** (ADR-0018 D8), não o tipo.
  O risco que sobra não é vender o impossível, é vender o que não cabe — e esse eixo já tem dono.
- **A fronteira não muda.** `VeiculoDocumento` continua gravando o `name` da classe, e continua recusando o
  documento cuja classe não existe. É a vantagem de o registro seguir sendo enum: **nenhuma migração**.
- **Classe nova continua exigindo deploy**, e o preço é aceito pela razão do ADR-0020 mais uma do negócio:
  classe de veículo é **fato da estrada, não da agência**. `VAN` e `SUV` entraram exatamente assim
  ([ADR-0023](0023-passagem-por-categoria-e-referencia.md) D4), sem que ninguém sentisse falta de cadastro.
- **`tarifaMotoBase` fica sem razão de existir.** Já não tem chamador desde que *preço é I/O*, e não é a
  cilindrada que o sustentava: o vínculo entre classe e dinheiro é que morreu. Saiu na fatia que implementou
  este ADR.
- **A emissão sobre um Navio encolhe muito.** Com o D4, o casco Navio oferece duas classes; o passo da
  natureza passa a ter dois valores com uma classe cada. O roteiro derivado precisa **dissolver o subpasso
  quando ele tem uma resposta só** — do contrário o ADR-0029 é contrariado no caso mais comum, que é o de
  perguntar algo cuja resposta é única.
- **O estudo passa a ter um trecho superado**, e fica como história: o §6.4 conclui por dois eixos, e o D6
  reduz a um.

## Alternativas consideradas

- **Classe como entidade cadastrável pelo supervisor** (o pedido literal da issue). Recusada: custaria
  coleção, codec, porta, repositório, regra de servidor, suíte de emulador e seção de menu com seis paradas
  — e cobraria da série histórica a disciplina de nunca renomear nem reaproveitar id. O que se ganharia era
  dispensar deploy para um valor que é fato da estrada.
- **Classe como catálogo, com o tipo mandando e o catálogo só rotulando.** Recusada por nome pelo ADR-0020:
  *"cria duas fontes para o mesmo vocabulário e reintroduz o fail-open"*.
- **Manter a forma de hoje e só acrescentar as onze.** Recusada pelo custo medido: onze rótulos e cerca de
  **cinquenta e cinco decisões de comportamento escritas à mão**, das quais vinte e duas seriam de
  pertencimento — e sobre essas o próprio código admite que a atribuição atual é *"leitura minha sobre o que
  cada casco carrega, não decisão registrada"*.
- **Dois eixos: porte × propulsão** (a conclusão do §6.4 do estudo). Recusada pelo D6: o porte existia para
  substituir os pertencimentos, e o D4 os dissolveu antes.
- **Renomear a classe `Lancha`.** Recusada pelo D5.

## Alternativas futuras

- **Quando a lista crescer de novo.** A natureza absorve valores novos sem custo: uma classe nova é uma
  linha de uma coluna. O que **exige decisão** é uma classe que não caiba em nenhuma das quatro naturezas —
  aí a pergunta não é *onde encaixo*, é *o eixo ainda descreve o negócio?*, e ela volta ao domínio.
- **Se o porte voltar a ter consumidor.** O D6 o deixou fora por não haver quem o leia. Ele volta no
  primeiro destes casos: a **ocupação** passar a ser contada por área de convés em vez de por vaga; a
  **inferência tarifária** (o método continua em aberto desde o ADR-0013) usar dimensão como eixo; ou um
  casco novo cuja admissão não se descreva por *todas · apenas · nenhuma*.
- ~~**Se `exigeModelo` também deixar de derivar de alguma coisa útil.**~~ **Decidida na execução, no mesmo
  dia**: o modelo ficou **sempre opcional**, pela direção *data-intensive* do D2 (guarda-se o que se tem, não
  se cobra o que não se sabe). O que restou de exceção na tabela não é essa — é a **cilindrada da moto**, e
  ela é fato do negócio, não resíduo de forma.
- **Se alguma agência precisar não ofertar uma classe.** Hoje isso não existe por decisão (D9). Se passar a
  existir, o instrumento **não** é lista de negadas — é o mesmo argumento de 2026-08-10: *ver e vender são a
  mesma pergunta*, e a resposta seria concessão, com todo o peso que ela tem.
