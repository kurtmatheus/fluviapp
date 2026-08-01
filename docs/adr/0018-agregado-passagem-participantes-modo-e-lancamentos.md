# ADR-0018: O agregado Passagem — participantes com identidade, modo e lançamentos

**Status:** **Aceita (direção)** — decisões do analista em **2026-08-01**, ao longo de uma rodada única que
percorreu o agregado inteiro. Sem código: este ADR fixa o domínio da Passagem e o plano; a implementação é
faseada abaixo. Formaliza o [desenho de domínio do agregado](../design/dominio-passagem.md) (§11), que
registra a conversa decisão a decisão. **Supera o [ADR-0012](0012-ciclo-de-vida-passagem-e-embarque-qr.md)
no ponto do cancelamento** (D17): o *delete* físico dá lugar a um estado, porque manter histórico é
prioridade.

> Conversa com o [ADR-0008](0008-relacionamentos-por-identidade.md) (relacionar por id — cujo padrão este
> ADR estende aos participantes), o [ADR-0012](0012-ciclo-de-vida-passagem-e-embarque-qr.md) (a FSM de
> emissão, que **não** muda), o [ADR-0013](0013-tabela-de-tarifa-e-tipo-tarifario.md) (tarifa tabelada e
> categoria tarifária), o [ADR-0014](0014-balanco-financeiro-da-travessia.md) (o eixo financeiro por
> viagem), o [ADR-0015](0015-rework-agente-equipe.md) (agência e emissor), o
> [ADR-0016](0016-dominio-da-plataforma.md) (plataforma multi-empresa; Rota e agenda) e o
> [ADR-0017](0017-eixo-de-storage-firestore-only.md) (Firestore-only — que é o que torna este ADR barato).
> Ancorado no código em `2026-08-01`.

---

## Contexto

### Por que agora

Três forças se encontram, e é o encontro que abre a janela:

1. **O Room está saindo (ADR-0017).** A `Passagem` é plana — ~28 colunas de participante com sufixo
   numérico — **porque uma tabela exigia colunas**. O documento no Firestore **já nasce aninhado**
   (`PassageiroDocumento`, `VeiculoDocumento`); é o mapper que desmonta o que vinha montado. Sem o espelho,
   o achatamento perde a única razão que tinha.
2. **A plataforma virou multi-empresa (ADR-0016).** Um agregado que era de *uma* operação passa a ser de
   muitas, com catálogo dinâmico e painel administrativo. O que sobrevive a isso é tipo fechado e chave
   estável; o que não sobrevive é texto de catálogo servindo de eixo analítico.
3. **A emissão vai ser revitalizada por etapas.** Mas — palavras do analista — *"a apresentação é a camada
   que reflete o domínio primariamente, não o contrário"*. Daí a ordem fixada: **fechar o domínio inteiro
   antes de escrever código**, para que a tela não decida por omissão o que o domínio não disse.

### O que o agregado é hoje

A Passagem é o único agregado do app: o fato transacional que consome e congela o master data. Os
participantes — passageiros 1..3 e veículo — são **membros embutidos sem identidade própria**, achatados em
campos com sufixo, e o vínculo veículo→responsável existe só por convenção de nome de campo.

Um sintoma resume o custo disso: **`PassagemFirestoreRepository.getListaNome()` retorna `emptyList()`** — e
os três campos de nome de passageiro mais o do responsável pela retirada consomem essa lista como
autocomplete (`ContentPassageiroAreaForm.kt:111,146,180`). **A tela já promete reaproveitar pessoa; o
domínio não tem o que responder.**

## Decisão

### A. Os participantes ganham identidade

- **D1 — Participante passa a ser gravado como chave + valores.** Cada participante carrega no documento
  uma **chave para referência** (`clienteId`, `veiculoId`) e os **valores congelados** do bilhete. É o
  ADR-0008 aplicado ao participante: **o bilhete lê sempre pelos valores** — nunca por junção, que o
  Firestore não faz — e a chave serve para o que valor não serve: reaproveitar, agregar, corrigir cadastro
  sem reescrever histórico. **Chave vazia é estado válido** (fail-open): melhor um participante anônimo do
  que uma venda perdida na fila do porto.

- **D2 — O `Cliente` é um *pool*, não um master data.** Ele não entra no nível de Empresa, Navio ou Viagem
  — aquelas são referência governada, com dever de unicidade. O `Cliente` é **acumulativo**: garante que a
  informação **exista** e seja atribuível, não que seja canônica. A identidade é a **chave natural** — o
  documento apresentado —, e a mesma pessoa cadastrada com CPF numa agência e RG noutra vira **duas
  entradas**, o que é **aceito**. *"Não é redundância, é questão de análise de dados; o importante é ter os
  dados, pode ser até normalizado depois."* Consequências assumidas: **deduplicar é etapa analítica
  posterior**, jamais no caminho da emissão; **uma entrada identifica a credencial, não a pessoa**; e
  **"clientes únicos" é métrica aproximada por construção** — *atendimentos* é que é exato.

- **D3 — O pool é da plataforma, assinado pela agência; só a plataforma sobrescreve.** Uma pessoa é um
  documento (não um por tenant), com o metadado das agências que já a atenderam (`agenciaIds[]`,
  `array-contains`): **existência é global, visibilidade é local** — *"assim a agência não pega o
  `listaNome` de todo mundo, pode onerar"*. A escrita da agência tem **exatamente dois direitos**: **criar**
  a entrada que não existe e **assinar** a que existe; **corrigir conteúdo é curadoria da plataforma**
  (painel do ADR-0016, no eixo `ADM` × `GESTOR` que o ADR-0017 §7.1 abriu). Na regra: *create* com conteúdo
  (`resource == null`) e *update* restrito ao `arrayUnion` da assinatura — mesmo endurecimento de
  `ehConfirmacaoEmbarque()` (ADR-0011). Como quem emite não lê o que ainda não assinou, a mecânica é tentar
  um e cair no outro.

- **D4 — Todo passageiro tem documento.** *"Não existe criança sem documento nesse negócio."* Tipo **e**
  número passam a ser obrigatórios para o titular e para os acompanhantes marcados — hoje o número só é
  cobrado **se** um tipo foi escolhido (`ValidacaoPassageiro.kt:54,61-62,65-66`), de modo que se emite
  bilhete sem credencial alguma. É isto que faz a chave natural cobrir **100% dos passageiros**.

- **D5 — O veículo é o segundo pool, com a placa como chave natural.** Mesmo regime: pool, assinatura,
  curadoria da plataforma, e no bilhete `veiculoId` + valores congelados. A placa é **chave melhor** que o
  documento de pessoa — única por construção, sem o CPF × RG —, então este pool **não polui**: duplicata só
  nasce de digitação errada. Guarda o que identifica o veículo (classe, modelo, cor e, na moto, a
  cilindrada — que é atributo **do veículo**, ainda que congelada no bilhete por justificar a tarifa). O
  **responsável pela retirada não entra aqui**: é pessoa, vai para `clientes`, e o vínculo é da **passagem**
  — quem retira muda a cada travessia.

### B. O eixo do modo

- **D6 — Quatro modos exclusivos, e o titular é quem dá nome ao modo.** `REDE`, `SUITE`, `CAMAROTE` e
  `VEICULO` são valores de **um** eixo fechado; morre o par `acomodacao: String` + `isVeiculoChecked:
  Boolean`. **Veículo não tem acomodação, e acomodação já infere passageiro.** O titular é o **passageiro 1**
  nos três modos de pessoa e **o próprio veículo** no modo veículo — por isso o **responsável pela retirada
  é opcional para sempre**: na operação real *"nem sempre é informado, e é definido na hora e informalmente
  pelos interessados: despachante, transportadora, retirador"*; exigi-lo **travaria o processo como ele é**.
  Bilhete de veículo **sem nenhuma pessoa nomeada é a forma normal do modo**, não inconsistência tolerada.
  Quantos participantes existem é **propriedade do modo**, e **cada participante é um `Cliente`** com o seu
  `clienteId` e o seu botão de salvar.
  *A exclusividade já vigorava de fato* — `checkVeiculo()` zera acomodação, tipo, gratuidade e os três
  passageiros (`FormPassagemHelper.kt:53-79`); a tela troca a área inteira (`FormPassagemScreen.kt:202`) — e
  **`TarifaViagem.chave` sempre foi de uma dimensão** (`REDE|SUITE|CAMAROTE|CARRO|CARRETA|CAMINHAO`): nunca
  existiu célula `SUITE × CARRO`. Isto **corrige a descrição "dois eixos" do ADR-0013**; o modo não é a
  chave tarifária, é **quem decide de onde ela sai** (no veículo, da classe, com moto por regra de
  cilindrada).

- **D7 — O tipo tarifário é da acomodação: a unidade vendida é o espaço.** *"Um camarote vale um camarote"*,
  e comporta até a capacidade; a suíte, idem. Logo **meia e gratuidade só existem no modo rede**, o único em
  que a unidade vendida coincide com uma pessoa — não porque "todos saem inteira", mas porque **não há
  pessoa sendo tarifada** nos outros. `tipoPassagem`/`gratuidade` são **nulos por construção** fora da rede:
  valor preenchido em outro modo é ignorado, não interpretado. **A cota de gratuidade fecha sozinha**:
  contar por passagem já é contar por pessoa, porque gratuidade só existe onde uma passagem é uma pessoa. E
  **nomear ≠ tarifar**: os acompanhantes são nominados com documento obrigatório para o **manifesto de
  embarque**, sem efeito no preço.
  A **classe do veículo** ganha o mesmo estatuto — tipo fechado com regras próprias, não rótulo de catálogo:
  ela decide **o que o veículo precisa informar** (carro e moto exigem modelo; **caminhão e carreta não —
  na semântica do negócio a classe já é o modelo**; só a moto exige cilindrada) e de onde sai a tarifa.
  Placa e modelo seguem obrigatórios: são o que **identifica** o veículo.

- **D8 — A capacidade é do navio, atribuída no cadastro, e vira controle de estoque.** É o *ativo* do
  ADR-0016 que declara quantas redes, suítes e camarotes existem — *"assim se tem controle"*. Duas
  consequências: **a tela deixa de codificar o negócio** (o "até 3" constante vira leitura da capacidade do
  navio da viagem) e **ocupação deixa de ser só relatório** — a emissão confronta o estoque **antes**, pelo
  **mesmo padrão da cota de gratuidade** (contar o já emitido na travessia, caso tipado, banner
  fail-closed), herdando a mesma paridade parcial no servidor.

### C. Travessia, tempo e dinheiro

- **D9 — A viagem é atômica por dia da semana + hora; a passagem aponta para a ocorrência.** A Viagem é a
  **escala** — "terça-feira, 15:00" —, e o sistema **infere as ocorrências no intervalo de segunda a
  domingo**. O que a passagem consome é o **item de viagem**: a ocorrência que **grava o dia exato** e
  guarda o que é daquela travessia, para controle e apresentação. **Os atalhos de criação de passagem
  continuam como hoje** — muda o que a passagem aponta, não como o operador chega nela.
  Isto **precisa duas decisões anteriores**: a **numeração** e a **capacidade** são **por ocorrência**, não
  por escala (o navio tem 10 camarotes *em cada saída*), e o eixo financeiro do ADR-0014 passa a agregar por
  ocorrência — que é o que "receita da travessia" sempre quis dizer.

- **D10 — A numeração é por ocorrência, com incremento atômico.** Hoje `ContadorBilhete` é **um documento
  único** (`id = 1`): numa plataforma multi-empresa, todas dividiriam a mesma sequência e disputariam o
  mesmo documento. Passa a haver um contador por ocorrência; a unicidade vira o par `(ocorrência, numero)`,
  e o `id` do documento segue sendo a identidade técnica que o QR carrega (o embarque não muda). O
  ler-somar-gravar atual (`adicionarContador`, ainda com `runBlocking`) dá lugar a incremento atômico —
  **numerar e ocupar são o mesmo problema de corrida (D8), e resolvem-se juntos**. Reemissão não renumera.

- **D11 — Pagamento é lista de lançamentos embutida, com identidade própria: `{id, forma, valor}` — e só.**
  Somar por forma no momento da escrita **descarta informação irreversivelmente**: o lançamento é o **fato**,
  a soma é derivável dele, o inverso nunca. **NSU, txid, taxa e recebedor ficam de fora — seria
  over-engineering.** O `id` no item existe para que promover a lista a coleção `lancamentos` seja **mover,
  não redesenhar**. Duas lacunas se resolvem melhor **fora** do lançamento: **quando se pagou** vira
  **`criadoEm`/`alteradoEm` na passagem** (que hoje **não registra quando foi emitida** — só `dataViagem` e
  `embarcadaEm`), e **quem recebeu** já é o emissor congelado.

- **D11′ — A `tarifaBase` deixa de ser congelada no bilhete.** O que a passagem guarda de dinheiro são os
  **lançamentos** (o que entrou) e a **categoria** (modo, classe, tipo tarifário) — que deixa de ser chave
  para consultar um preço cadastrado e passa a ser **dimensão de análise**. A tarifa vira grandeza
  **inferida** do pool de valores praticados, não cadastrada (estudo §11.11a; a política inteira é ADR
  próprio). Consequência imediata a assumir: **cai o bloqueio de emissão por célula ausente**
  (`ResultadoEmissao.SemTarifa`, ADR-0013) — o que resta exigindo é o valor informado, que a validação já
  cobre.

- **D12 — A emissão é pós-pagamento; o faturamento é módulo à parte.** A passagem **nasce paga**; não existe
  bilhete "em aberto" dentro do agregado. Isso **dissolve por decisão** o eixo "a receber": a exigência de
  ao menos uma forma de pagamento (`ValidacaoDadosPassagem.kt:46-48`) **é a regra**, não limitação;
  `descontoDerivado` fica íntegro — um bilhete com `cobrado = 0` apareceria no ADR-0014 como **receita zero
  e desconto de 100%**, e esse risco não se materializa; e a FSM segue com **um eixo só**, sem a segunda
  máquina de estados que o §8 do estudo mostra custar paridade em três camadas. O **faturamento** — a
  transportadora que embarca vários veículos e acerta em conta — vira **módulo próprio**, a desenhar quando
  o negócio pedir, e é lá que voltam conciliação (NSU/txid), taxa e prazo por forma, conta corrente do
  pagador (pelo pool, via `CNPJ:…`), estorno/devolução e fechamento de caixa por turno.

### D. Forma do documento

- **D13 — A agência do emissor entra por id.** A passagem congela `agenciaId` + o nome como snapshot, o
  mesmo par de viagem, navio, empresa e funcionário. O recorte por agência (ADR-0015 P2.6) e a identidade
  visual do bilhete passam a depender de **id**, não de casamento de nome.

- **D14 — O carimbo de embarque vira sub-objeto ausente/presente.** O **estado continua sendo o
  `status = EMBARCADA`** (ADR-0012) — isto não muda. O que muda são os três campos do carimbo
  (`embarcadaPorId`, `embarcadaPor`, `embarcadaEm`), hoje planos com default `""`: viram um objeto que ou
  existe inteiro, ou não existe, tornando **irrepresentável** o estado meio-preenchido. Custo casado: a
  regra `ehConfirmacaoEmbarque()` endurece esses nomes (`hasOnly`) e a suíte de emulador cobre — **forma,
  regra e teste mudam no mesmo incremento** (dever de paridade).

- **D15 — A placa se normaliza por máscara na entrada.** *"Não é tão caro ouvir esse campo e corrigir."* O
  antigo `LLL-NNNN` (três letras + **quatro dígitos**) leva traço — inserido se o operador não digitar; o
  Mercosul `LLLNLNN` não leva. O canônico **é a grafia oficial de cada padrão**, não "tudo sem traço"; o que
  a máscara garante é **uma só grafia possível** ao entrar, que é o que permite à placa ser chave (D5).

### E. O histórico é prioridade

- **D17 — Cancelar desativa; não apaga. Isto supera o ADR-0012.** Lá, *"cancelar não é estado — continua
  sendo delete físico"*, e `CANCELADA`/`EXPIRADA` ficaram registradas como futuro. O futuro chegou por
  decisão de negócio: **manter histórico é prioridade**, e um bilhete apagado leva consigo o fato de que
  existiu, quem o emitiu, quanto se cobrou e por que deixou de valer.
  **`CANCELADA` entra como estado terminal da FSM** (ADR-0012 §3), alcançável de `A_EMITIR` e de `EMITIDA`
  e **nunca de `EMBARCADA`** — quem já embarcou não cancela a travessia; o que existe depois disso é acerto
  financeiro, e acerto é do módulo de faturamento (D12). Estado, e não um segundo campo `ativa`, pelo mesmo
  motivo que D12 recusou o eixo "a receber": **uma máquina de estados, não duas**. A máquina, a política de
  quem pode cancelar (o eixo de posse/cargo do ADR-0010) e a regra do servidor (ADR-0011, com a transição
  irreversível) andam juntas, pelo dever de paridade das três camadas.

- **D18 — Cancelada não ocupa, não fatura e não renumera.** O que a desativação implica, item a item:
  - **Estoque e cota** (D8, ADR-0013): a contagem de capacidade e a de gratuidade **excluem canceladas** —
    senão um cancelamento consome vaga para sempre. É a mesma consulta de hoje com o status de fora.
  - **Balanço** (ADR-0014): canceladas saem da receita esperada e da real, e são **contadas à parte** — o
    mesmo tratamento que o fallback sem `tarifaBase` já recebe lá ("não mascara"). **Cancelar não desfaz o
    pagamento:** a devolução é evento do faturamento (D12), não do bilhete.
  - **Numeração** (D10): o número **fica com o bilhete cancelado**. Sequência com buracos é o normal de uma
    numeração que registra fatos; reaproveitar número seria reescrever histórico — exatamente o que esta
    decisão proíbe.
  - **Embarque** (ADR-0012 §4): bilhete cancelado não embarca — a FSM já garante, e `ResultadoEmbarque`
    ganha o caso próprio, em vez de cair no `NaoEmitida` genérico.
  - **Deletar sai do fluxo do operador.** Se o histórico é prioridade, remoção física deixa de ser ação de
    tela; se sobrar algum caso legítimo, é privilégio da plataforma (D3), não da agência.

### F. O que cai por consequência

- **D19 — Três divergências código × regra a corrigir na validação pura** (que já é pura e JVM-testável):
  `modeloVeiculo` é exigido **sempre** (`ValidacaoVeiculo.kt:29`), de modo que **carreta e caminhão não
  passam sem modelo**, contra D7; o documento do passageiro é exigido **condicionalmente**, contra D4; e
  `getListaNome()` devolve lista vazia — a cova que D2/D3 preenchem com a consulta recortada por assinatura.

## Consequências

**O que se ganha**

- **O reaproveitamento passa a existir.** Digitar a placa preenche classe, modelo e cor (busca por **id**,
  sem lista); o nome sugere de verdade, recortado pela agência. O autocomplete deixa de ser promessa vazia.
- **O eixo analítico fica confiável.** *"Quantos e qual veículo e qual preço"* — a série contagem × classe ×
  preço vira ativo, com a classe estável no veículo em vez de digitada por venda, e "quantas travessias
  esta carreta fez" passa a ser respondível.
- **Estados ilegais deixam de ser representáveis** — modo tipado no lugar de booleano + limpeza reativa;
  carimbo de embarque inteiro ou ausente; capacidade barrando antes em vez de medindo depois.
- **A tela para de decidir domínio** — capacidade, quem existe e o que se exige passam a vir do modo e do
  ativo, não de constantes de form.

**O que se paga**

- **Duas coleções novas com PII**, cada uma exigindo regra de servidor e suíte de emulador **no mesmo
  incremento** — e placa é dado pessoal indireto tanto quanto documento. O recorte por assinatura é **alcance,
  não segredo**: quem tem a credencial na mão assina e passa a ver. É o degrau que o ADR-0017 já apontava
  para o back-end centralizador.
- **Duplicidade aceita no pool** (D2) e **métrica de pessoas únicas aproximada** — de propósito.
- **Curadoria vira trabalho da plataforma** (D3): entrada criada errada só o painel conserta. O operador
  segue emitindo, porque o bilhete carrega os valores dele e não depende do pool.
- **Duas escritas no pior caso** ao salvar participante (tentar criar, cair na assinatura).
- **Mais um dever de paridade** (D14): forma, regra e teste andam juntos.

## Plano de migração (faseado)

**Restrição de ordem contra o ADR-0017:** enquanto o Room existir, **cada campo novo na `Passagem` é DDL**.
Por isso as fases que mudam a **forma do documento** (F4, F6) entram **depois ou junto** da **F5 do
ADR-0017** (Passagem sai do Room). As coleções novas (F2, F3) **não** têm esse problema: nascem já no regime
Firestore-only, sem entidade espelho — são, aliás, a segunda prova do padrão que o piloto do catálogo
estabelece.

- **F1 — Tipos e regras puras, sem tocar persistência.** `ModoPassagem` (4 valores, capacidade e quem
  existe) e a classe de veículo como tipo fechado (exige modelo? exige cilindrada? de onde sai a tarifa?),
  mais as correções de D19. Tudo JVM-testável, isolado, reversível.
- **F1b — Cancelamento** (D17/D18): `CANCELADA` na FSM, política de quem cancela, `firestore.rules` +
  emulador e o caso próprio em `ResultadoEmbarque` — as três camadas do dever de paridade **no mesmo
  incremento**, mais a saída do *delete* do fluxo. Independe das demais fases e pode vir cedo: **enquanto
  ela não existir, cada cancelamento apaga histórico que a decisão diz ser prioridade.**
- **F2 — Pool `Cliente`** (D2/D3/D4): coleção, `firestore.rules` + emulador, assinatura por agência, botão
  de salvar no form e a consulta recortada que substitui `getListaNome()`.
- **F3 — Pool `Veiculo`** (D5/D15): máscara de placa, get por id, assinatura — mesmo molde da F2, menor.
- **F4 — Pagamento e carimbos de tempo** (D11): lançamentos embutidos, `criadoEm`/`alteradoEm`.
- **F5 — Ocorrência de viagem, numeração e capacidade** (D8/D9/D10) — as três **juntas**: a ocorrência é
  onde o contador e o estoque moram, e ambos são o mesmo problema de corrida.
- **F6 — Forma do documento** (D1/D13/D14): participantes com chave, agência por id, embarque como
  sub-objeto — a fase que se encontra com a F5 do ADR-0017.
- **F7 — A emissão por etapas.** Abas por modo, com animação: **consequência**, e por isso por último.

## O que este ADR não decide

- **O módulo de faturamento** (D12) — escopo, telas e conciliação ficam para ADR próprio.
- **A Rota do ADR-0016** — aqui só se fixa que a passagem consome a **ocorrência**; a forma da Rota e da
  agenda é de lá.
- **`EXPIRADA`** — o outro estado que o ADR-0012 deixou no futuro segue lá: expirar é **regra temporal**
  (bilhete que perdeu a validade sozinho), não ação de operador, e é decisão à parte de D17.
- **A devolução do dinheiro** — cancelar desativa o bilhete (D17); estornar o pagamento é do módulo de
  faturamento (D12).
- **A política tarifária.** O analista decidiu, ainda em 2026-08-01, que **a tarifa não será cadastrada: ela
  é inferida do pool de valores praticados** ([estudo §11.11a](../design/dominio-passagem.md)) — porque a
  Rota é reutilizável por várias empresas e uma tarifa cadastrada nela ficaria sem dono (é o que o ADR-0016
  já anunciara ao *adormecer a tarifa cadastrada*). O efeito aqui é pontual e está em **D11′**: a
  `tarifaBase` deixa de ser congelada. O resto — o que acontece com a tabela por célula, com a tarifa devida
  como cálculo, com o desconto derivado e com a régua do balanço — **supera parte do ADR-0013 e revisa o
  ADR-0014**, e fica **anotado para ADR futuro, provavelmente dentro do módulo faturamento**: inferir tarifa
  é análise de dinheiro através de muitas vendas, que é a definição daquele módulo.
- **Assento/cabine numerada** — a capacidade é contagem (D8), não inventário identificado.
- **A forma da UI de abas** — o eixo existe (D6); como ele se desenha é matéria da camada de apresentação.

## Referências

- [Desenho de domínio do agregado Passagem](../design/dominio-passagem.md) — o estudo que originou este ADR,
  com a conversa decisão a decisão (§11)
- [ADR-0017](0017-eixo-de-storage-firestore-only.md) — Firestore-only (a restrição de ordem sai daqui)
- [ADR-0016](0016-dominio-da-plataforma.md) — domínio da plataforma (ativo, capacidade, Rota, painel)
- [ADR-0015](0015-rework-agente-equipe.md) — equipe, agência e emissor
- [ADR-0014](0014-balanco-financeiro-da-travessia.md) — balanço financeiro (agregação por travessia)
- [ADR-0013](0013-tabela-de-tarifa-e-tipo-tarifario.md) — tarifa tabelada e categoria tarifária
- [ADR-0012](0012-ciclo-de-vida-passagem-e-embarque-qr.md) — ciclo de vida e embarque por QR
- [ADR-0011](0011-regras-firestore-por-cargo.md) / [ADR-0010](0010-autorizacao-por-cargo.md) — regras e política
- [ADR-0008](0008-relacionamentos-por-identidade.md) — relacionar por identidade