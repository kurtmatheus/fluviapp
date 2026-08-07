# ADR-0022: O painel da empresa — seções compartilhadas, autoridade de escrita e as fases a partir da F5

**Status:** Aceita em direção (decisões do analista em 2026-08-07) · sem código

**Estudo que preparou:** [`docs/design/painel-da-empresa.md`](../design/painel-da-empresa.md)

---

## Contexto

A **v0.0.4 foi para produção em 2026-08-07** com quatro seções vivas — Empresas, Flotilha, Localidades e
Portos —, cada uma refeita ponta a ponta e com regra de servidor publicada. Isso encerra a **F4** e a
**F5** do [ADR-0016](0016-dominio-da-plataforma.md) e, com elas, o **primeiro dos dois planos de acesso**
que o §2 daquele ADR desenhou: *quem cria o universo*.

O segundo plano — *quem cria a oferta* — nunca teve o menu desenhado. O ADR-0016 §2 disse de que família
cada seção deriva e parou aí, numa frase que hoje precisa de tradução: *"`VIAGEM` sai do menu — o nome
estava errado desde o começo"*. Estava mesmo, mas o §7.1 da 9ª rodada resolveu o nome depois disso: a
`Viagem` que sai é o antigo trecho disfarçado; a `Viagem` que fica é a **partida física atômica**
`(rota, embarcação, dia, hora)`, e ela é seção legítima.

Três fatos do código de hoje delimitam a decisão:

1. o menu já é **domínio testável** — `SecaoMenu`, `AcaoMenu` e `MenuDaAtuacao` decidem, em JVM puro, quais
   seções existem em cada família; a navegação só liga rota a destino;
2. a política é **uma só** (`PermissoesUsuario`), com dois eixos — seção e ação — e duas coordenadas,
   `(papel, cargo)`;
3. o recorte da revitalização (`SECOES_REVITALIZADAS`) é o que mantém o app coerente enquanto isso: seção
   fora do conjunto não aparece, e revitalizar é acrescentar um valor.

## Decisão

### D1 — O painel da plataforma está completo; F4 e F5 se encerram aqui

Registro, não plano: as quatro seções que a plataforma administra existem e são utilizáveis. O que restava
escrito na F5 e não é cadastro de empresa — a morte de `Agencia` e de `Funcionario.Lotacao` — **migra para
a fase da Equipe**, que é onde essas duas coisas de fato morrem.

Consequência de método que vale nomear: a partir daqui, **fase = seção do painel**. Foi assim que o
trabalho andou desde a revitalização (uma entidade viva por vez, do domínio à tela), e manter o plano numa
unidade diferente da unidade de entrega só produz fases que nunca fecham.

### D2 — O menu não são duas famílias disjuntas: é um núcleo compartilhado com duas pontas exclusivas

| Seção | Plataforma | Empresa | Por que deste lado |
|---|---|---|---|
| **Início** | sim | sim | é a porta; o que ela mostra é que muda com o contexto |
| **Rotas** | sim | sim | pool **sem dono** (ADR-0016 §7.1) |
| **Viagens** | sim | sim | idem — a partida física é uma só para todas as agências |
| **Passagens** | não | **sim** | emitir exige **vínculo de funcionário**, não papel (§2) |
| **Equipe** | não | **sim** | o quadro é de uma empresa; quem o gere é o supervisor dela |

**O critério não é de interface, é de posse no domínio.** Rota e Viagem foram postas na raiz sem dono
justamente para que duas empresas que vendem a mesma linha usem *a mesma* viagem — é isso que faz a
ocupação por partida física ser uma conta só, sem *collection group*. Uma seção compartilhada é a leitura
direta desse fato. Passagem e Equipe, ao contrário, têm dono por natureza: a passagem tem emissor, o
funcionário tem vínculo.

A regra que fica, e que serve de teste para qualquer seção futura: **se a entidade tem dono, a seção é da
empresa; se não tem, é compartilhada.** Uma seção que pareça pertencer aos dois lados com donos diferentes
é sintoma de erro no modelo, não de menu mal dividido.

Isto **revisa o ADR-0016 §2** em um ponto: `VIAGEM` volta ao menu, com o significado do §7.1.

### D3 — Ver é de todos; escrever é da plataforma e do supervisor — e "escrever" quer dizer **criar**

Decisão do analista: **plataforma e `SUPERVISOR` escrevem Rota e Viagem; o `AGENTE` só lê.** É o eixo de
ação que a política já tem (ADR-0010), aplicado a seções novas — não uma coordenada nova.

O que o desenho do §7.1 obriga a precisar, e que esta decisão herda: **rota e viagem são imutáveis**. Não
se reescreve e não se exclui; se a saída muda de horário, desativa-se e cria-se outra. Logo:

- **criar** é da plataforma e do supervisor — é o que povoa o pool, e é seguro justamente porque nada do
  que já existe é tocado;
- **editar** não existe para ninguém, e a ausência é a garantia de que uma agência não quebra o que a
  outra vende;
- **desativar** é da **plataforma**. Desativar um registro compartilhado afeta todo mundo, e dar esse
  poder ao supervisor de uma agência reintroduziria, pela porta dos fundos, exatamente o dano que a
  imutabilidade evita;
- o instrumento do supervisor para "isto não me interessa" continua sendo a **lista de negadas** na
  atuação (§7.1): *deny-list*, por agência, na tela — não no servidor.

O `AGENTE` só lê porque ele vende sobre o que existe; e porque a autoridade dele é a **emissão**, que já
tem eixo próprio.

### D4 — A ordem do menu não é a ordem de construção: a Equipe vem primeiro

O menu termina em `Início · Rotas · Viagens · Passagens · Equipe`. A construção começa pelo fim da lista.

A razão é que a Equipe é, ao mesmo tempo, **o cadastro mais fácil e o de mais regra** — e as duas coisas
apontam para o mesmo lugar. Fácil porque é o quinto cadastro no molde do ADR-0006. Densa porque é onde:

- `Funcionario` troca `cargo` por **vínculos** `[{empresaId, atuacao, cargo}]` (ADR-0016 §6), e uma pessoa
  passa a poder ser agente numa empresa e supervisor em outra;
- `PermissoesUsuario` vai de `(papel, cargo)` para **`(papel, atuação, cargo)`** — e é ela que todo o menu
  consulta, inclusive as quatro seções que já estão de pé;
- nasce a **seleção de contexto**: com mais de um vínculo, o app precisa perguntar em nome de quem se está
  operando, e é essa resposta que decide qual painel aparece;
- morrem `Agencia` e `Funcionario.Lotacao`.

**Fazer a Equipe primeiro é fazer o eixo antes das telas que dependem dele.** Rota, Viagem e Passagem todas
perguntam de qual empresa é quem está operando; construí-las antes seria escrevê-las contra uma política
marcada para mudar de forma — o mesmo erro que o ADR-0020 F2 evitou ao não migrar leitores de `MUNICIPIO`
para um domínio que ia mudar.

### D5 — O plano de fases se redivide por seção, da F6 à F10

| Fase | Entrega |
|---|---|
| ~~F4, F5~~ | **feitas** (D1) |
| **F6 — Equipe** | vínculos, cargo por atuação, política com três coordenadas, seleção de contexto, `EscopoAgencia` por empresa; `Agencia` e `Lotacao` morrem |
| **F7 — Início** | o painel que deriva do contexto escolhido, com o estado vazio como cidadão de primeira classe |
| **F8 — Rotas** | `rotas/{id}` na raiz, imutável, unicidade do par de portos no servidor, validação de concessão |
| **F9 — Viagens** | `viagens/{id}` atômica, unicidade `(rotaId, navioId, diaSemana, hora)`, lista de negadas, ocupação |
| **F10 — Passagens** | emissão sobre a viagem, e a revitalização do que já existe (ciclo de vida, QR, bilhete) |

**Rota e Viagem viram duas fases, e não uma.** Eram uma linha só quando "rota" era um cadastro; desde a 9ª
rodada são entidades de ciclos diferentes — a rota é a ligação (estável), a viagem é a partida (repetida, e
é sobre ela que se conta ocupação). Entregá-las juntas seria uma fatia grande demais para o ritmo de uma
entidade por vez, que é o ritmo que vem funcionando.

A **F10 é a única que revitaliza código antigo**, e por isso é a de tamanho menos previsível. Fica
declarado como ponto a decidir quando ela chegar: reescrever no molde ou adaptar.

### D6 — "Regras e suíte" deixa de ser fase e vira definição de pronto

A antiga F8 do ADR-0016 já se declarava incremental (*"regra escrita depois é regra que passou um tempo
aberta"*). A rc.3 do Porto cobrou o preço de ela existir como etapa separada: o `match /portos/{doc}` subiu
no repositório e **não foi publicado**; o servidor negou o listener, o primeiro snapshot nunca chegou, e o
formulário ficou esperando por ele — o sintoma apareceu como um dropdown vazio acusando a coleção errada.

Portanto: **uma fatia não está entregue enquanto a regra da coleção não estiver publicada e coberta na
suíte de emulador.** Não é rigor extra; é reconhecer que, num app sem back-end próprio, a regra *é* a
camada de serviço — e uma coleção sem regra não falha, **pendura**.

## Consequências

- **`SecaoMenu` muda de conteúdo e de ordem.** Entra `INICIO`, `VIAGEM` deixa de ser a herança do menu
  antigo e passa a ser a do §7.1, e a ordem do enum passa a ser a ordem acordada em D2 — que continua sendo
  também a ordem de dependência do cadastro.
- **`MenuDaAtuacao` deixa de ter o núcleo compartilhado como exceção.** Hoje `SECOES_DO_PAINEL` carrega
  `VIAGEM` por herança; passa a carregar o núcleo por decisão, e `secoesDa(AGENCIAMENTO)` ganha Rotas e
  Viagens ao lado de Passagens.
- **A política ganha a terceira coordenada na F6, e todo chamador de `cargo` muda junto.** É o item mais
  caro do plano, e ele foi **antecipado de propósito** (D4).
- **O `AGENTE` passa a ver duas seções que não pode escrever.** É a primeira vez que o app mostra uma seção
  em modo leitura; a UI precisa dizer isso sem parecer defeito — botão ausente, não botão desabilitado
  (mesma escolha do §8 do ADR-0016 para a capacidade de veículo).
- **Desativar rota ou viagem vira ato de plataforma** (D3), e isso cria uma dependência operacional nova: o
  supervisor que quiser algo fora do ar depende de quem administra. É custo aceito em troca de nenhuma
  agência poder derrubar o que a outra vende; a válvula de escape é a lista de negadas, que resolve o caso
  comum sem tocar no pool.
- **O `Início` é a primeira seção sem entidade.** Todas as outras nasceram de um cadastro; esta nasce de
  uma pergunta ("o que eu faço agora?"), e é por isso que ela é a fase mais fácil de adiar e a mais fácil
  de encher de coisa que não serve.
- **O andaime da revitalização encolhe até sumir.** `SECOES_REVITALIZADAS` ganha um valor por fase, e
  quando igualar `SecaoMenu.entries` o arquivo inteiro sai do projeto — junto com `ForaDoEscopo` e o
  `foraDoEscopo` da suíte de regras.
- **O plano deixa de ter uma fase de fechamento.** Com D6, não existe mais "a fase em que as regras
  ficam prontas": ou a fatia entrega a regra, ou a fatia não entrega. Em troca, some a possibilidade de
  chegar ao fim do plano com um passivo de segurança acumulado.

## Alternativas consideradas

- **Dois menus disjuntos** (plataforma × empresa, sem interseção). Recusada: obrigaria a duplicar Rotas e
  Viagens em dois lugares, ou a escondê-las de quem administra a plataforma — e quem administra precisa
  ver o pool para curá-lo, que é justamente quem D3 põe como responsável por desativar.
- **Só a plataforma escreve rota e viagem.** Recusada pelo analista: a oferta é da empresa, e o supervisor
  é quem sabe que saída existe. Manter a criação na plataforma transformaria cada linha nova num chamado.
- **O supervisor também desativa.** Recusada aqui (D3): é o único poder do conjunto que atinge terceiros, e
  a lista de negadas cobre o motivo legítimo de querer usá-lo.
- **Construir na ordem do menu** (Início → … → Equipe). Recusada: adiaria a mudança de política para
  depois de três telas que dependem dela.
