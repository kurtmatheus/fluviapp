# Linha do tempo — em que ordem as decisões aconteceram

Este documento **não decide nada**. Os dois índices respondem *o que vale* ([ADRs](adr/README.md)) e *o que
cada estudo mediu* ([estudos](design/README.md)); aqui se responde a pergunta que nenhum dos dois responde:
**por que essa decisão veio nessa hora.**

As datas são as de entrada no repositório (`git log --diff-filter=A`), não as do texto — quando as duas
divergem, é porque a decisão foi tomada antes de ser escrita, e o ADR diz isso na linha de status.

> **Escala.** Do primeiro commit (2026-07-07) a hoje: **trinta ADRs**, **vinte e nove estudos**, duas
> versões em produção e cerca de trezentos commits. Julho concentrou o desenho; agosto, a construção.

---

## Julho — a fundação, e a virada de escopo no fim do mês

### 07 a 16/07 · o alicerce técnico (0002 → 0009)

A primeira leva não decide **o que o app é** — decide **como ele se sustenta**. Modelo de memória do dado
(0003), sessão (0005), molde de cadastro (0006), telemetria (0007), relacionamento por identidade (0008) e
o pipeline reativo (0009).

Duas dessas continuam sendo régua diária: o **molde de cadastro** (0006), que é a convenção de todo
formulário até hoje, e o **relacionar por id, não por nome** (0008) — que voltará a decidir coisas em
agosto, inclusive a forma da classe de veículo.

### 20 a 26/07 · quem pode o quê (0010 → 0015)

Autorização por cargo (0010) e a fronteira dela no servidor (0011). No meio, o **ciclo de vida da passagem
com embarque por QR** (0012) — a FSM `A_EMITIR → EMITIDA → EMBARCADA` que sobrevive a tudo o que vem
depois e é, hoje, uma das poucas peças que a F9 **não** precisou refazer.

Fecha com o **0015**, que é a primeira grande reescrita do passado: *o agente é o usuário*. `Agente` vira
`Funcionario`, cargos são renomeados, e a agência do bilhete passa a vir do emissor em vez de ser digitada.

### 31/07 · o dia que mudou o produto (0016 e 0017)

**O ADR-0016 troca um app de uma empresa por uma plataforma multi-empresa e multi-segmento.** É a decisão
de maior alcance do projeto, e o efeito colateral é maior que ela: **o seed morre**, o painel
administrativo vira a porta de entrada do dado, rota e viagem viram capacidades **sem dono**, e a tarifa
cadastrada adormece — uma entidade sem dono não tem de quem ter tarifa.

No mesmo dia, o **0017** tira o Room do caminho (*Firestore-only*) e, ao fazê-lo, **destrava o 0016**: dois
pontos que estavam abertos deixam de existir.

---

## Agosto — construir, e descobrir construindo

### 01 a 04/08 · o agregado, a fronteira e o fim do Catálogo (0018 → 0021)

O **0018** dá identidade aos participantes da passagem (pools `Cliente` e `Veiculo`), tipa o modo, troca
quatro campos de pagamento por **lançamentos** e transforma cancelamento em estado — *manter histórico é
prioridade*. O **0019** fecha o *passo 2* que o 0003 deixara aberto desde o começo: a fronteira vira `Map`
e o DTO passa a ser por caso de uso.

E o **0020** faz a conta até o fim: aplicada sem exceção, a régua do 0016 **não deixa nenhuma categoria de
catálogo de pé**. Nasce a frase que decide forma no projeto inteiro daí em diante —

> Vocabulário que o código consome é *tipo*. Dado que o negócio cria é *entidade*. Não há terceira
> categoria.

**04/08 — `v0.0.3`, a primeira versão distribuída.**

### 07 a 10/08 · o painel da empresa, e a execução reescrevendo o plano (0022)

**07/08 — `v0.0.4` em produção**, com o painel da plataforma completo (Empresas, Flotilha, Localidades,
Portos). O **0022** redivide o resto por seção — Equipe → Rotas → Viagens → Passagens → Início — e põe a
**Equipe primeiro**, porque é ela que muda a política de `(papel, cargo)` para `(papel, atuação, cargo)`.

As três fases seguintes saem em quatro dias, e **a execução emenda o próprio ADR em três lugares**: o
recorte do pool pela atuação mata a *deny-list* antes de ela existir; *criar vira subconjunto de ver*; e
nasce uma seção que nenhum ADR previa (`USUARIOS`). É a primeira vez que o passado é reescrito **por
levantar código**, e não por escrever documento.

### 11 a 13/08 · a Passagem em cinco ADRs num dia (0023 → 0030)

Cinco ADRs entram em **11/08**: domínio (0023), fronteira (0024), camada (0025), orquestração e
apresentação (0026) e o faseamento da F9 (0027). No mesmo dia cai uma decisão que atravessa tudo — **preço
é I/O**: a emissão não calcula valor, o operador informa o praticado, e a inferência tarifária vira eixo de
análise.

Em **13/08**, as etapas da emissão (0028) e, horas depois, os fluxos (0029) — que **superam o 0028 em
granularidade**: os três blocos viram muitos passos pequenos, no modelo de um totem de restaurante. E o
0030 fecha o bilhete digital, implementado na mesma leva.

### 17 a 18/08 · dois defeitos ensinam mais que dois documentos

Uma viagem e uma rota inativadas pelo painel **continuaram no card da tela inicial**. O rastro cruzou três
ADRs e produziu a lição mais citada do índice: **reativo no repositório não é reativo na tela** — o
consumidor tem de assinar. O segundo defeito era de domínio: `Rota.ativo` existia e ninguém lia; *estado
que existe sem ninguém que o leia é igual a não existir*.

As duas correções foram checadas **por mutação** — removida a correção, os testes falham —, e é assim que o
projeto passou a provar regressão.

Em **18/08**, a menor emenda que o índice registra: o leitor de QR troca de biblioteca, e a troca **paga uma
dívida anotada em outro documento** (o `.so` do CameraX era o único bloqueio de 16 KB do `targetSdk 35`).

---

## Setembro — o retorno da operação

### 03/09 · a apresentação institucional vira cinco issues, e um ADR

Cinco pedidos entram como issues, e o repositório ganha as primeiras. Três viram código no mesmo dia — a
empresa passa a exigir só o nome, o Início destaca a saída de hoje, e o **embarque volta a ter porta** (a
funcionalidade estava inteira e testada desde a F9; faltava a entrada, comentada na revitalização).

O quinto pedido — *o supervisor cadastrar classes de veículo* — produz o **0031**, e o percurso dele vale
como método: o pedido foi medido contra o que já estava decidido, a primeira leitura foi **rejeitada no
enquadramento**, e a pergunta certa era *onde mora o comportamento*. O ADR termina **confirmando** uma
decisão de 2026-08-01 por outro caminho, e três medições sustentaram a virada — duas delas de peças que
**ninguém lia** (`ehPesado` sem leitor, `tarifaMotoBase` sem chamador).

---

## O que a linha do tempo mostra

**As decisões que mais reescreveram o passado não foram as maiores — foram as que mudaram o eixo.** O 0016
trocou o produto; o 0020 trocou a régua de forma; o 0019 trocou o método (*da tela nascem as fronteiras e as
camadas*). Nenhum dos três falava do assunto que acabou mudando.

**Documento não é o único jeito de reescrever o passado.** A execução do 0022 emendou o próprio ADR três
vezes ao levantar código, e os defeitos de 17/08 emendaram outros três sem que nenhum documento novo fosse
escrito. Por isso o índice de vigência registra **emendas de execução** e não só de decisão.

**O intervalo entre decidir e construir encurtou.** Julho decidia semanas antes de construir; em agosto,
cinco ADRs e a implementação deles couberam em dias. O risco que isso cria está nomeado no próprio projeto:
foi assim que sete linhas do índice de estudos envelheceram sem ninguém notar.
