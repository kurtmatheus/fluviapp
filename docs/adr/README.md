# Decisões de arquitetura — índice de vigência

Cada arquivo desta pasta é um **ADR**: uma decisão tomada, com o contexto que a justificou e as
consequências assumidas. ADR não se apaga e não se reescreve quando muda de ideia — **escreve-se outro**, e
o antigo passa a valer como história. Este índice existe para responder, sem abrir os vinte e seis arquivos,
**o que ainda está valendo e o que deixou de valer**.

> Regra de precedência: **ADR vence estudo** (`docs/design/`), e **ADR mais novo vence ADR mais velho** no
> ponto em que se cruzam — nunca no documento inteiro. Por isso a coluna *o que caiu* é específica: quase
> nenhum ADR foi superado por completo.

**Revisado em 2026-08-10**, depois da execução das fases **F6 (Equipe)**, **F7 (Rotas)** e **F8
(Viagens)** do [ADR-0022](0022-painel-da-empresa-e-fases.md). A revisão anterior (2026-08-01) media o
índice contra os ADRs recém-escritos; esta mede contra **código no ar**, e é por isso que ela derruba mais:
três decisões do [ADR-0016](0016-dominio-da-plataforma.md) §7.1 caíram ao serem construídas.

**Emendado em 2026-08-11** com os cinco ADRs da Passagem — **domínio**
([0023](0023-passagem-por-categoria-e-referencia.md)), **fronteira** ([0024](0024-fronteira-de-dados-da-passagem.md)),
**camada** ([0025](0025-camada-de-dados-da-passagem.md)), **orquestração e apresentação**
([0026](0026-orquestracao-e-apresentacao-da-passagem.md)) e o **faseamento da F9**
([0027](0027-faseamento-da-f9.md)) — mais a decisão de que **preço é I/O**. Eles mexem em oito linhas do quadro
abaixo, e duas mudanças valem além da Passagem: o **fim do snapshot** retira o exemplo que o critério *embutir /
referenciar / congelar* usava desde o ADR-0008, e o **rascunho fica no Room** (revogando o ADR-0017 D4), o que faz
o Firestore-only ganhar um limite declarado — ele vale para o **fato compartilhado**.

---

## Quadro geral

| Estado | Significa |
|---|---|
| **vigente** | vale inteiro |
| **vigente ·  parcial** | o essencial vale; um ponto específico caiu — está dito abaixo |
| **dormente** | a decisão continua correta, mas **não será construída** |
| **superada** | não vale mais; fica como história |

| ADR | Do que trata | Estado | O que caiu, e por quem |
|---|---|---|---|
| [0002](0002-capability-forma-pagamento.md) | Capability de forma de pagamento | **superada** | inteiro — ADR-0015 §4a: todo emissor escolhe a forma |
| [0003](0003-modelo-de-memoria-do-dado.md) | Modelo de memória (Room espelha Firestore) | **vigente · parcial** | o nível *cacheada* deixa de ser o Room — ADR-0017. A camada **entidade / DTO / documento** foi refinada pelo ADR-0016 (7ª rodada) |
| [0004](0004-snapshot-e-observabilidade-emissao.md) | Rascunho local + observabilidade da emissão | **vigente · parcial** | ~~o rascunho sai do Room e vai para o DataStore~~ — **revogado em 2026-08-11**: ele **fica no Room** ("com garantia do Room"), porque **deixou de ser resíduo**. Um snapshot passa a ser uma **passagem incompleta**, com **vários por agente** e **tela de recuperação** — caem o **slot único** e a invariante *existe ⇔ é rascunho*; a porta, a serialização e a telemetria seguem. Desenho em [`docs/design/apresentacao-passagem.md`](../design/apresentacao-passagem.md) §6 |
| [0005](0005-autenticacao-sessao-firebase-datastore.md) | Sessão Firebase + DataStore | **vigente** | — (o provisionamento fechado veio no ADR-0015 §2.1; o bootstrap do 1º ADM, no ADR-0016 §10) |
| [0006](0006-molde-de-cadastro.md) | Molde de cadastro | **vigente** | — é a convenção viva de todo form |
| [0007](0007-observabilidade-cadastros.md) | Telemetria dos cadastros | **vigente** | — |
| [0008](0008-relacionamentos-por-identidade.md) | Id para relacionar × valor para lembrar | **vigente · parcial** | a régua segue como **padrão de dados** — mas a `Passagem` **sai da metade *valor para lembrar*** (ADR-0023 D8): no domínio há **só referência**, e congelar virou decisão da camada de dados, a tomar adiante e só com relevância demonstrada. O que pagou a inversão: Rota e Viagem são **imutáveis** desde a F7/F8, então a mutação de que o snapshot protegia praticamente não existe |
| [0009](0009-sincronizacao-reativa-firestore-room.md) | Pipeline reativo único | **vigente · parcial** | o **destino** muda: o DAO deixa de ser a fonte reativa e vira `StateFlow` — ADR-0017 D1. Ciclo de vida, porta `FonteSnapshots` e telemetria seguem |
| [0010](0010-autorizacao-por-cargo.md) | Política única de autorização | **vigente · parcial** | os cargos foram **renomeados** (ADR-0015) e a política ganhou a **atuação** como terceira coordenada (ADR-0016, 8ª rodada) |
| [0011](0011-regras-firestore-por-cargo.md) | Regras no servidor | **vigente** | — cresce com cada coleção nova. Duas notas da execução: `funcionarios` foi **reescrita sobre vínculos** (F6.3) e `viagens` foi a primeira regra **corrigida** em vez de escrita (F8.2) — o `allow write` único que estava lá admitia editar e apagar. A **unicidade de rota/viagem não é imposta pelo servidor**: regra não consulta coleção, e derivar o id do documento da chave brigaria com a recriação que a imutabilidade exige; fica no cadastro, com caso de emulador documentando o limite |
| [0012](0012-ciclo-de-vida-passagem-e-embarque-qr.md) | FSM da passagem + embarque por QR | **vigente · parcial** | **cancelar deixa de ser *delete* físico** e vira estado — ADR-0018 D17. O carimbo de embarque vira sub-objeto — D14 — e **perde o nome** de quem embarcou: fica `embarcadaPorId` + `embarcadaEm` (ADR-0023 D8). A FSM e o QR como ponteiro seguem inteiros, e são a parte da F9 que **não se refaz** |
| [0013](0013-tabela-de-tarifa-e-tipo-tarifario.md) | Tarifa tabelada e tipo tarifário | **superada (a tabela) · vigente (as funções)** | a tabela cadastrada era **dormente** desde o ADR-0016 §7.2; a **F8.0 a apagou**: `TarifaViagem`, a tabela do Room e o mapa `tarifas` do documento saíram com a Viagem-trecho. As funções puras vivem — muda a **fonte** da base, que passa a ser inferida por agregação. O `tarifas` do `ViagemCongeladaDocumento` sobrevive como **resíduo de leitura**: os bilhetes antigos o carregam. `ResultadoEmissao.SemTarifa` **ainda existe em código** — o §7.2 o condena, e quem o remove é a F9. **Desde 2026-08-11 ele não tem mais nem razão:** preço é I/O, e com isso `tarifaBase` **perde o portador** — o bilhete registra o **valor praticado**, não uma tarifa devida congelada (ADR-0023 D6/D8) |
| [0014](0014-balanco-financeiro-da-travessia.md) | Balanço financeiro | **vigente · parcial** | a **régua** muda (esperada vem da inferência, não do tabelado — ADR-0016 §7.2); agrega por **ocorrência** e exclui canceladas — ADR-0018 D9/D18 |
| [0015](0015-rework-agente-equipe.md) | Equipe, agência, cargo | **vigente · parcial** | o cargo passa a ser **por vínculo `(empresa, atuação)`**, não por pessoa — ADR-0016 (8ª rodada) |
| [0016](0016-dominio-da-plataforma.md) | Domínio da plataforma | **vigente · parcial** | **o `Catalogo` não nasce** — ADR-0020 D1: caem §3 inteiro, a exceção do tipo de embarcação (§8), a coleção `catalogo/` do mapa (§4), o catálogo embutido na `Localidade` (§5), a linha "Catálogo — só `ADM`" (§6) e a **F1** do plano. O eixo, o critério de colocação e as 9 rodadas seguem. O **§8 saiu do papel**: o tipo de embarcação virou campo de `Embarcacao`, e o cadastro já esconde a capacidade de veículo de quem não leva veículo. A **§7.1 também**: a concessão (`atuacoes/AGENCIAMENTO.embarcacaoIds`) ganhou editor no form da Empresa — era allow-list de segurança sem ninguém que a escrevesse, e toda embarcação nascia invendável. A **F4 fechou as capacidades da plataforma**: a `Localidade` existe (com `Uf` tipado e `codigoIbge` obrigatório, o que dispensa a unicidade `(categoria, descricao)` que o §5 previa) e o **`Porto` também** — `nome` + `localidadeId` por referência (nunca cópia), delete lógico como o dela, e a unicidade `(nome, localidade)` verificada **no cadastro**; a paridade dessa unicidade no servidor continua pendente. **Com a v0.0.4 (produção, 2026-08-07), F4 e F5 estão feitas e o painel da plataforma está completo** — Empresas, Flotilha, Localidades e Portos. O plano de fases daqui em diante é redividido por seção do painel da **empresa** (Equipe → Início → Rotas → Viagens → Passagens), e a antiga "F8 — regras e suíte" deixa de ser fase: vira definição de pronto de cada fatia, lição cobrada pela rc.3 do Porto. Ver [`docs/design/painel-da-empresa.md`](../design/painel-da-empresa.md). **O §7.1 caiu em três pontos ao ser construído (F7/F8) — ver a seção *[O que a execução do §7.1 derrubou](#o-que-a-execução-do-71-derrubou)* abaixo.** O §6 também se ajustou: o `Vinculo` tem **dois** campos (`empresaId`, `cargo`) e não três — a atuação é **derivada** do cargo (§6.1, conjuntos disjuntos), e o campo ao lado seria contraditório, não redundante |
| [0017](0017-eixo-de-storage-firestore-only.md) | Firestore-only | **vigente · em execução** | o **piloto** deixa de ser `Catalogo` e passa a ser **Empresa** — ADR-0020 D10. F1 vira "coleção que *perde* o espelho", não "que nasce sem". O CRUD comum virou `ColecaoFirestore<T>` + `CodecFirestore<T>` — cada entidade nova declara um codec e compõe, e o codec pode **recusar** um documento (`deDocumento` devolve `T?`), que é como um invariante de domínio chega à fronteira sem derrubar a coleção. **Nenhuma tabela do Room espelha coleção**: Empresa (v3), Embarcação (v4), Funcionário (v5) e, com a demolição da Viagem-trecho, `Viagem`+`TarifaViagem` (**v7**, F8.0) — as duas últimas. O que resta no banco ou só existe ali (rascunho, bilhete digital) ou espera a vez (`Usuario`, `Constante`, `Passagem`, contador). Desde a v0.0.4 as migrações são **escritas**, não recriadas: o `fallbackToDestructiveMigration` levaria o rascunho de quem tem o app instalado. **O D4 caiu em 2026-08-11** e com ele a ideia de que o Room morre inteiro: o rascunho **fica**, porque virou **passagem incompleta** (local por natureza). O eixo se precisa em vez de se enfraquecer — **Firestore-only vale para o fato compartilhado**; atendimento em curso é local, e é por isso que sobrevive a app fechado e rede ausente |
| [0018](0018-agregado-passagem-participantes-modo-e-lancamentos.md) | O agregado Passagem | **vigente · parcial** | o **ADR-0023 reforma a raiz**: **D1** perde os *valores congelados* (fica só a chave), **D6** se **dissolve** (`ModoPassagem`, um eixo de quatro valores, vira `Categoria` × `Acomodacao` — veículo não é acomodação, é sub-domínio), **D13** fica só com o id da agência e **D14** perde o nome `embarcadaPor`. Seguem inteiros D2/D3 (pool `Cliente`, que ganha **telefone**), D5, D11 (lançamentos), D17/D18 (cancelar desativa) e D19 — cuja primeira divergência o ADR-0023 D4 resolve **no tipo**, não na validação. O plano de migração de oito passos passa a ser insumo do faseamento da F9, não o plano dela. Nota histórica: **D6 e D7 foram implementados** junto do ADR-0020, antes das fases do próprio 0018 |
| [0019](0019-camada-de-dados-dinamica-e-dto-por-caso-de-uso.md) | `Map` na fronteira, DTO por caso de uso | **vigente · parcial** | a **F1** deixa de ser `Catalogo` e passa a ser Empresa — ADR-0020 D10. O regime não muda. Realiza o *passo 2* que o ADR-0003 previu. **A pergunta que ele deixava aberta fechou** (ADR-0024 D8): o DTO carrega **tipo**, não `String` formatada |
| [0020](0020-fim-do-catalogo-e-o-contexto-do-painel.md) | O fim do Catálogo; o painel deriva da atuação | **vigente** · F1 e F2 feitas | o **D2 foi emendado** na execução (a máscara do CPF esconde os 6 primeiros dígitos, não as pontas). **F2 fechada em 2026-08-03**: o `SeedFirestore` foi removido. O rename `Navio` → **`Embarcacao`**, que o ADR adiava, foi executado em 2026-08-04 e foi até a fronteira (coleção `embarcacoes`, campo `embarcacaoIds`); a seção do menu chama-se **Flotilha**. O **D4 fechou** em 2026-08-05: `TipoEmbarcacao` deixou de ser tipo sem portador e virou campo **não-nulo** da entidade — *não existe embarcação sem tipo* —, com o formulário exigindo e a fronteira **recusando** o documento que não o declara |
| [0021](0021-usuarios-da-plataforma-adm-only.md) | Usuários da plataforma (`ADM`-only) | **direção · FORA DO MVP** (D0) | **não implementar**: o cadastro no console vira **princípio** — a administração da plataforma vive fora do app, somando P2.2c + anti-escalonamento + fim do seed + ADR-0016 §10. D1–D4 valem como desenho de quando a seção nascer: primeira divergência entre `ADM` e `GESTOR`, só leitura, e `allow read` de `users` restrito |
| [0022](0022-painel-da-empresa-e-fases.md) | O painel da empresa e as fases da F5 em diante | **vigente · direção** (2026-08-07) | registra que **F4 e F5 fecharam** com a v0.0.4 (painel da plataforma completo) e divide o resto: menu = **núcleo compartilhado** (Início, Rotas, Viagens) + **duas exclusivas da empresa** (Passagens, Equipe), pelo critério *entidade com dono → seção da empresa*. **Revisa o ADR-0016 §2** (`VIAGEM` volta ao menu, com o sentido do §7.1). Escrita de rota/viagem = plataforma + `SUPERVISOR`, e **escrever é criar**: editar não existe (imutabilidade do §7.1) e **desativar é da plataforma**; `AGENTE` só lê. Fases **F6 Equipe → F7 Rotas → F8 Viagens → F9 Passagens → F10 Início**, com a Equipe primeiro por ser onde a política vira `(papel, atuação, cargo)` e o **Início por último** (emenda do analista: é *sumário* por papel/empresa/cargo, e sumário vem depois do que resume). A antiga **F8 "regras e suíte" deixa de ser fase** e vira definição de pronto. **F6, F7 e F8 estão FEITAS** (2026-08-07 a 2026-08-10) — e a execução emendou o próprio ADR em três lugares: (1) a **D3 foi revisada** — o supervisor deixou de criar rota em qualquer par, porque com a lista recortada pela atuação isso criava uma travessia que sumia da própria lista (**criar virou subconjunto de ver**); (2) a **F6.6 abriu uma seção que o ADR não previa** (`USUARIOS`, ADM-only) ao desfazer o nó *usuário é da plataforma, funcionário é da empresa*, e com ela `SECOES_TRANSVERSAIS` ficou vazio; (3) o **Início foi parcialmente antecipado** para a F8.4 — o da empresa é a lista de `ViagemSemana` sob *Viagens Disponíveis*, cumprindo o "cada seção trata do seu próprio início na vez dela"; o da plataforma continua sendo a F10; e (4) a **ocupação**, que a tabela de fases punha na F8, **não foi entregue nela** — contar ocupação é contar bilhete, e `PASSAGEM`/`CONTAGEM` seguem fora de `SECOES_REVITALIZADAS`: ela vai com a **F9**, sobre a chave que a F8.4 já deixou pronta (`ViagemSemana`) |
| [0023](0023-passagem-por-categoria-e-referencia.md) | A Passagem por categoria — sub-domínios e referência | **vigente · direção** (2026-08-11) | novo. Reforma a **raiz** do agregado: a **categoria é o eixo** (`PassagemDePassageiro` \| `PassagemDeVeiculo`, com `PassagemDeCarga` previsto — e a prontidão para ela é o **formato**, não um campo reservado); o **comum** é a travessia vendida (ocorrência, lançamento, observação, metadados) e o **específico** é o que ocupa o espaço; **nada é congelado no domínio** (D8); os participantes viram **lista de `clienteId`** e o `Cliente` ganha **telefone**; a regra sobe para o tipo (a acomodação limita o tipo tarifário; o tipo do veículo diz se exige modelo e cilindrada). **`Cliente` e `Veiculo` estão definidos campo a campo** (o cliente com documento obrigatório por D4, `dataNascimento` como `LocalDate` e `telefone` novo; o veículo com a placa como chave natural e `ClasseVeiculo` ganhando `VAN`, `SUV` e `exigeModelo`). Quatro pontos ficam **assumidos, não decididos**: o ponteiro ser a ocorrência, o número do bilhete sobreviver, a rede ter um cliente por bilhete e o telefone ser opcional — **os quatro foram aceitos no mesmo dia**, e o ADR não tem mais nada em aberto |
| [0024](0024-fronteira-de-dados-da-passagem.md) | A fronteira de dados da Passagem | **vigente · direção** (2026-08-11) | novo. Como o agregado do 0023 atravessa para o Firestore: **uma coleção com `categoria`** e o codec **despachando e recusando**; a ocorrência em `viagemId` + **`data` como texto ISO** — escolhido por consulta, não por convenção (igualdade exata sem normalização, faixa nativa, e a data **é o id** do documento da ocorrência), o que arrasta **todo instante do agregado para ISO** e corrige o `embarcadaEm`, que hoje **não ordena**; `clientes` volta a ser **array com o titular na posição 0** (uma consulta em vez de duas); **lançamentos** como lista imutável e **sem total denormalizado** — o total é **inferido**; lançamento ilegível **recusa a passagem inteira** (inverte o tratamento do vínculo, porque é dinheiro); contador em **subcoleção** `viagens/{id}/ocorrencias/{data}` com `increment`, e ele **não persiste a ocorrência**; a **consulta recortada** entra no contrato (a Passagem não é `observarTodos`). Limite declarado: **as regras não iteram lista**, então a consistência do dinheiro é do cliente. Uma consequência foi **corrigida no mesmo dia**: *lookup em memória* vale para a referência, **não para os pools** — a junção tem dois regimes. **D11: delete físico não existe** — só cancelamento; a regra vira `allow delete: if false` (como as **seis** coleções que já o fazem), e o caso de emulador *"dono deleta a própria passagem → OK"* **inverte de sinal** na mesma fatia |
| [0025](0025-camada-de-dados-da-passagem.md) | A camada de dados da Passagem | **vigente · direção** (2026-08-11) | novo, e fecha a trinca **domínio → fronteira → camada**. Nasce a **porta `PassagemRepository`** — a Passagem era a **única entidade sem porta**, com a classe concreta injetada em **dez lugares**, que é a razão de **nunca ter havido teste de ViewModel de passagem**; ela se define pelas **ausências** (sem `editar`, sem `deletar`, sem `observarTodas`) e o cancelamento **não** ganha método, porque é transição e a política é do ADR-0010. A consulta é **objeto de critério** (`CriterioPassagem`), com o efeito de *"ver tudo"* deixar de ser uma **string vazia**. A junção vira **função pura** — *coletar é da camada de dados, traduzir é do domínio*, com o `ContagemPassagensMapper` como precedente —, e a interface `Mapper<E,O>` **sai**. DTO **por consumidor** como critério, sem nomear projeções de consumidor não planejado. Telemetria com **três desfechos renomeados** (`aplicadaLocalmente`…), porque o desfecho local mede *o que o operador pode afirmar*, não qual banco gravou. Restrição de método declarada: **nada se justifica por desempenho projetado**. **Uma linha do D7 foi revista no mesmo dia**: o rascunho **não** vai para o DataStore — ele vira **passagem incompleta** e fica no Room, o que derruba o ADR-0017 D4 e faz o Room **não morrer inteiro** na F9 |
| [0026](0026-orquestracao-e-apresentacao-da-passagem.md) | Orquestração e apresentação da Passagem | **vigente · direção** (2026-08-11) | novo, e **fecha os quatro passos**. As duas camadas vêm juntas porque se cruzam no `UiState` e no evento one-shot. O **VM é o único escritor** e as classes auxiliares perdem **o handle mutável e o repositório** (viram transformação estado→estado); **um `UiState` por categoria**, o que apaga a limpeza reativa; a **sequência da emissão volta para o VM** e a navegação só reage — morrem o `Context` no VM, o `internal lateinit` exposto e o `scrollParaErro`; a emissão é **por etapas** (resolve os 47 parâmetros de uma vez); a formatação vai para uma **camada fina** por tipo; e o **argumento de rota vira opcional**, apagando a extensão `isTextoNaoNulo` e os **dois** usos dela — inclusive o que atravessou até a escrita no Firestore. Diz também **o que não muda**: badge, scanner, captura em Compose e os `@Preview`. **Nota lateral**: o snapshot vira *passagem incompleta* (revoga ADR-0017 D4, ADR-0004 slot único, e uma linha do 0025 D7) — mas **construí-la fica fora da F9** |
| [0027](0027-faseamento-da-f9.md) | O faseamento da F9 | **vigente · direção** (2026-08-11) | novo. **Supera o §7 do estudo do terreno**: são **seis** fatias, não oito — duas se dissolveram nas decisões (preço é I/O; a forma vem do tipo selado). Ordem: **domínio → dados → (camada + orquestração) → apresentação → a seção acende**. **Corte respondido pelo domínio**: os pools `Cliente`/`Veiculo` são **pré-requisito** (o agregado os referencia por id), o cancelamento entra **junto** com a saída do delete (tirar um sem o outro deixaria o operador sem saída), e ficam **fora** ocupação/balanço/análise (sem domínio planejado), o snapshot (nota lateral) e o papel. `Contagem` e `Balanço` ficam **marcados, não corrigidos** — precedente da F8.0. Definição de pronto por fatia: regra publicada + emulador + JVM, e **tela em aparelho** nas fatias com tela |
| [0028](0028-as-etapas-da-emissao.md) | As etapas da emissão | **vigente · direção** (2026-08-13) | novo, e **completa o D4 do 0026**, que fixou o eixo (*emissão por etapas*) e deixou o roteiro aberto. São **três passos** — o bilhete, quem viaja, o pagamento —, e a ordem é **imposta pelo domínio**: a acomodação vem antes da lista de clientes porque é ela quem declara quantos cabem, e o tipo tarifário fica no passo 1 porque é propriedade do **espaço vendido** (meia e gratuidade só existem na rede). **Bilhete unitário**, exceto suíte e camarote: família de três em redes são **três emissões**, e o `isVeiculoChecked` morre porque era a forma visível de um modelo que misturava dois sub-domínios. **`TipoGratuidade` volta ao agregado** — a F9.1 gravava "gratuidade" sem dizer qual, e sem o subtipo a **cota do 0013 §8** não tem o que contar. **Passageiro exige portador; veículo não** (o responsável é opcional *por regra de negócio*), e o registro no pool — a única operação que **exige rede** — tem de **tolerar falha sem perder o atendimento**. O cabeçalho da viagem vira **persistente** e **data/hora deixam de ser campos**: eram editáveis e podiam discordar da saída escolhida, o mesmo defeito que a agência digitada teve até a P2.3. Morre também o `scrollParaErro` — com passos, o erro **está no passo** |

---

## O que a execução do §7.1 derrubou

O [ADR-0016](0016-dominio-da-plataforma.md) §7.1 desenhou o **pool compartilhado** — rota e viagem sem
dono, para que duas agências que vendem a mesma linha usem *a mesma* viagem e a ocupação seja uma conta
só. **Esse núcleo vale inteiro.** O que caiu foi o mecanismo de *recorte* que ele propunha junto, e caiu
por decisão do analista em **2026-08-10**, ao ser construído:

> *"O painel da empresa é gerenciar informações relacionadas àquela empresa; nem faz sentido ver outras
> embarcações ou rotas ou viagens que não estão dentro da atuação da empresa."*

| O que o §7.1 dizia | O que vale | Por quê |
|---|---|---|
| *"Visualização = pool − negadas; venda = concessão"* | **visualização = venda = concessão** | eram duas perguntas com risco permanente de discordarem: a lista mostrava o que a emissão depois recusava |
| `rotasNegadas[]` / `viagensNegadas[]` na atuação | **não são construídas** | com a visualização já recortada, a *deny-list* ficou sem trabalho — e some o par assimétrico *allow-list no servidor × deny-list na tela* |
| *"filtrar a visualização pela concessão faria a agência nova ver tela vazia"* | **é exatamente o que acontece, e é aceito** | provisionar deixou de ser conveniência e virou **pré-requisito**; a tela diz isso em vez de mostrar seletor vazio |

O que substitui os três é um tipo: **`EscopoDoPool`** (`Todo` = plataforma · `Concedido(atuacao)` ·
`Nenhum`), com a porta `EscopoDaSessao` resolvendo `contexto → vínculo → empresa → atuação → escopo` num
lugar só. Ele **não é** o `EscopoEmpresa` da política — aquele recorta por *dono*, e o pool não tem dono;
este recorta por *concessão*. Uma assimetria sobrevive de propósito: **a plataforma vê o pool inteiro**,
inclusive a viagem órfã — quem cura precisa enxergar o que conserta. E some junto a *nota de escala* do
§7.1 (a previsão de que negar uma a uma viraria trabalho num pool grande): sem deny-list, o gatilho que
ela registrava não existe.

*Duas formas que o §7.1 não fixava, e que a construção fixou (decisões do analista, 2026-08-10):
`horaMin` em **minutos desde a meia-noite** — é o único horário do app sobre o qual se faz conta — e
`diaSemana` como `java.time.DayOfWeek` **não-nulo**, pelo precedente de `Embarcacao.tipo`.*

## O que mudou de nome ou de dono (o vocabulário)

A maior fonte de confusão ao ler um ADR antigo não é a decisão — é a **palavra**. Estas trocaram de sentido:

| Onde se lê… | Hoje é… | Desde |
|---|---|---|
| `Agente` (entidade) | `Funcionario`; e o "agente do bilhete" é o **emissor** (`funcionarioId`) | ADR-0015 |
| `DIRETOR`, `COLABORADOR_MASTER`, `OPERADOR` (como cargos) | **papel** `ADM`/`GESTOR`/`OPERADOR` × **cargo** `SUPERVISOR`/`AGENTE` | ADR-0015 |
| `agencia` como texto do formulário | derivada do **emissor**, e por **id** | ADR-0015 P2.3 · ADR-0018 D13 |
| `Constante` (a tabela de rótulos) | **tipos de domínio** — o `Catalogo` **não chega a nascer**. A palavra volta a significar *invariante de sistema* (extensão de arquivo, MIME) | ADR-0020 D1 |
| `Constante.Categoria.DOCUMENTO` / `PAGAMENTO` / `TIPO_EMBARCACAO` / `MUNICIPIO`+UF | `TipoDocumento` · `FormaPagamento` · `TipoEmbarcacao` · `Localidade`+`Uf` | ADR-0020 D2/D3/D4/D6 |
| `ACOMODACAO` (catálogo) + `isVeiculoChecked` + `CATEGORIA_PASSAGEM` | `ModoPassagem` — um eixo de quatro valores (rede/suíte/camarote/veículo) | ADR-0018 D6, implementado em `10dc514` |
| **`ModoPassagem`** (o eixo único) | **dois níveis**: `Categoria` (Passageiro \| Veículo \| Carga) × `Acomodacao` (Rede \| Suíte \| Camarote). **Veículo não é acomodação** — é sub-domínio | ADR-0023 D1 |
| "snapshot da `Passagem`", "valor para lembrar", par *id + valor* | **só a referência** — no domínio nada é congelado; congelar é decisão da camada de dados, e só com relevância demonstrada | ADR-0023 D8 |
| `nomePassageiro1..3` + documento + nascimento (12 campos) | **`clientes: List<clienteId>`**, ordenada, titular = primeiro | ADR-0023 D3 |
| `funcionarioResponsavel`, `agencia`, `embarcadaPor` (os nomes) | `funcionarioId`, `agenciaId`, `embarcadaPorId` — e os dois primeiros são **inferidos** do vínculo ativo, nunca digitados | ADR-0023 D7/D8 |
| `Constante.Categoria.VEICULO` | **`ClasseVeiculo`** | ADR-0018 D7, implementado em `5580b48` |
| `Navio` (a entidade) | **`Embarcacao`** — rename **executado** em 2026-08-04 (`4694a54`). `NAVIO` sobrevive como **valor** de `TipoEmbarcacao`: gênero e espécie deixaram de disputar a mesma palavra | ADR-0020 D4 |
| "atuação é categoria do catálogo" | `Atuacao` é **tipo**; a atuação *da empresa* continua **cadastrada** (`atuacoes/{ATUACAO}`) | ADR-0020 D5 |
| `Viagem` (a entidade antiga) | **Rota** (o onde) + **Viagem** (o quando e em quê, atômica) + **ocorrência** `(viagemId, data)`. A entidade antiga foi **demolida** em `07286a6`, não migrada | ADR-0016 §7.1 · F8.0 |
| a ocorrência `(viagemId, data)` | **`ViagemSemana`** — *"viagem_semana"* na palavra do analista. **Calculada, não persistida**: não há coleção | F8.4 |
| `ViagemDocumento` (o snapshot na Passagem) | **`ViagemCongeladaDocumento`** — o nome foi liberado para o documento da Viagem nova. Rename invisível no Firestore, que mapeia por nome de **campo** | F8.1 |
| "lista de negadas" (`rotasNegadas`, `viagensNegadas`) | **não existe** — a visualização já é a concessão | decisão do analista, 2026-08-10 |
| `Trecho` | **dissolvido** — o par de cidades é derivável dos portos | ADR-0016 (7ª rodada) |
| `Funcionario.agencia` / `.lotacao` / `Agencia` (entidade) | **`Vinculo(empresaId, cargo)`**; a agência do bilhete vem do **vínculo ativo** | ADR-0016 §6 · F6.1–F6.5 |
| `model/` (pacote) | `domain/` | rename de 2026-07-31 |
| Room como *datasource* | cache do SDK + `StateFlow` por coleção | ADR-0017 |
| `acomodacao` + `isVeiculoChecked` | **modo** da passagem, um eixo de quatro valores exclusivos | ADR-0018 D6 |
| tarifa **cadastrada** | tarifa **observada** (inferida por agregação) | ADR-0016 §7.2 |

## As três revisões que mais reescreveram o passado

**ADR-0016 — o domínio da plataforma.** Trocou o app de uma empresa por uma plataforma multi-empresa e
multi-segmento. Efeito colateral maior: **o seed morre** e o painel administrativo vira a porta de entrada
do dado; **rota e viagem viram capacidades compartilhadas sem dono**; e a **tarifa cadastrada adormece**,
porque uma entidade sem dono não tem de quem ter tarifa.

**ADR-0017 — Firestore-only.** Tirou o Room do caminho, e com isso **destravou** o ADR-0016 (dois pontos
abertos deixaram de existir) e barateou o ADR-0018 (o achatamento da Passagem perde a razão de ser).

**ADR-0019 — a camada de dados.** Fechou o *passo 2* do ADR-0003, que estava aberto desde o começo: a
fronteira vira `Map` e o DTO passa a ser por **caso de uso**. E trouxe o método que governa o resto do
roadmap: **da tela nascem as fronteiras e as camadas**, nunca o contrário.

**ADR-0018 — o agregado Passagem.** Deu identidade aos participantes (pools `Cliente` e `Veiculo`), tipou o
**modo**, pôs a capacidade no navio, fixou a numeração por ocorrência, trocou os quatro campos de pagamento
por **lançamentos** e transformou o cancelamento em estado, porque **manter histórico é prioridade**.

**A execução do ADR-0022 — a quarta, e a que reescreveu por construir.** As outras três reescreveram o
passado com outro documento; esta o reescreveu **ao levantar o código**. Três emendas que só apareceram
quando a tela existiu: o recorte do pool pela atuação (que matou a deny-list, acima), *criar virou
subconjunto de ver* (F8.3, revisando a própria D3), e a seção `USUARIOS` que nenhum ADR previa — ela
nasceu do incômodo de o `ADM` estar vendo a Equipe, e desfez o nó **usuário é da plataforma; funcionário
é da empresa**.

## O que está esperando decisão

- **O método da inferência tarifária** — janela, mínimo de bilhetes, viagem sem histórico, cálculo na
  leitura × materializado. Situado no **módulo faturamento** (ADR-0018, *o que não decide*). **O lugar dela
  ficou decidido em 2026-08-11** (decisão do analista): **preço é I/O** — a emissão **não calcula** valor, o
  operador informa o praticado, e a inferência é **eixo de análise sobre o agregado de passagens, por viagem
  ou por período**. Com isso morrem `ResultadoEmissao.SemTarifa`, `tarifaBase` e `tarifasViagem`, e a
  *esperada* do balanço (ADR-0014) passa a nascer do agregado. O que segue aberto é só o **método**.
- ~~**Se o DTO carrega tipo ou `String` formatada**~~ — **respondido em 2026-08-11: tipo** (ADR-0024 D8), e
  para a camada inteira. A formatação sobe para a apresentação; o que morre é o padrão do `DadosPassagem` (~58
  campos, quase todos texto já formatado, para uma lista que usa dez).
- **O módulo faturamento** — conciliação, taxa e prazo, conta corrente do pagador, estorno, fechamento de
  caixa.
- **O que o Início da *plataforma* mostra** (F10). O da empresa foi resolvido na F8.4 — *Viagens
  Disponíveis*, a lista de ocorrências da semana. O da plataforma continua sendo a pergunta aberta do
  ADR-0022 D5, e ela não tem entidade: nasce de *"o que eu faço agora?"*, não de um cadastro.
- **Se a Passagem da F9 é reescrita no molde ou adaptada** (ADR-0022 D5). A F8.0 deixou o
  `FormPassagemHelper` **podado**, com `// REVITALIZAÇÃO:` nos dois pontos que a F9 retoma: os nomes do
  snapshot (que passam a vir de Viagem → Rota → Portos) e a tarifa (que o §7.2 tornou inferida).

## A rede de tela executada — a F8 fecha (2026-08-11)

A pendência que era pré-requisito da F9 **está cumprida**. O `FormViagemScreenTest` — escrito em `9c2c386`
e até então nunca executado, porque o AVD travou na sessão — rodou num aparelho físico (SM-A566E) e passou
nos **quatro** casos, na primeira execução. Com ele, a suíte instrumentada inteira: **85 casos, 0 falhas**,
e o único `SKIPPED` é declarado — o `FluviAppNavigationTest`, `@Ignore` **com a razão escrita**, que
atravessa login e viagem e volta reescrito quando essas seções entrarem na revitalização.

As outras duas redes, medidas no mesmo commit: **516 testes JVM** no escopo (54 classes) e **103 casos de
emulador** (129 com os 26 que a revitalização deixa fora). E a metade da definição de pronto que suíte
nenhuma cobre: o job *Deploy das regras* rodou **depois** do último commit de `firestore.rules` — a regra
de `/viagens` está publicada, e não só versionada. A **D6 do ADR-0022 está satisfeita por inteiro**: regra
publicada, coberta no emulador, e tela medida no aparelho.

Vale registrar por que este item existiu: os dois únicos defeitos que chegaram ao tester nesta linha
passaram por todas as suítes de JVM e morreram só na tela — o dropdown vazio do Porto (rc.3 da fase
anterior) e o **teclado numérico sem dois-pontos** (rc.3 desta). Em nenhum dos dois uma camada mentia
isoladamente; o defeito estava no encontro delas com o aparelho. A F9 é a seção com mais formulário do
app, e entra com a rede de tela verificada em vez de suposta.

### Duas armadilhas de *chegar* à suíte

Nenhuma das duas é o código, e as duas custam a mesma hora:

- **Desinstale o `rc` antes de rodar.** A primeira tentativa foi recusada com
  `INSTALL_FAILED_VERSION_DOWNGRADE`: o build local nasce com `versionCode` **10** fixo — o fallback de
  quem compila sem a esteira —, e o instalador acusou **243** no aparelho, o número de um `rc` distribuído.
  Aparelho de homologação e de desenvolvimento são o mesmo, e o número da esteira (o run) é sempre maior
  que o fallback: o debug **nunca** instala sobre um `rc`.
- **A tela precisa estar acesa e desbloqueada**, ou a suíte falha em bloco com `No compose hierarchies
  found in the app` — a Activity não vai a foreground, e o erro não fala disso. `adb shell svc power
  stayon usb` resolve pelo tempo da sessão.

## Como escrever o próximo

1. O estudo vem antes (`docs/design/`), mapeando o código como está — com arquivo e linha.
2. O ADR registra **o que foi decidido e por quê**, não o que seria bonito.
3. Superou algo? Diga **onde** — seção e decisão —, nunca "supera o ADR-XX" inteiro.
4. Atualize **este índice** na mesma leva. Um índice que envelhece é pior que nenhum: ele mente com ar de
   autoridade. *(Foi o que aconteceu com a lista de pontos abertos do ADR-0016, que ficou para trás entre
   rodadas e induziu a leituras erradas em 2026-08-01.)*