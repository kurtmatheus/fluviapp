# ADR-0032: O acesso — a política volta ao gesto, a sessão vira fluxo, e o `ADM` gere quem entra

**Status:** Aceita (decisões do analista em 2026-09-07) · **sem código** · **§Q respondida no mesmo dia**

**Estudo que preparou:** [`docs/design/acesso-e-identidade.md`](../design/acesso-e-identidade.md)

---

## Contexto

O eixo de acesso — autenticação, sessão, autorização, observabilidade e provisionamento — nunca tinha sido
medido junto. O estudo o mediu em 2026-09-07, dias depois de a sessão mudar de casa (o Room saiu na F6 do
[ADR-0017](0017-eixo-de-storage-firestore-only.md), e nasceu `preferences/SessaoLocal`), e encontrou
**quatro coisas que o código já não fazia como estava escrito**:

1. **12 das 22 funções de `PermissoesUsuario` não têm chamador de produção** — a cadeia inteira da
   Passagem inclusive. `emitir()` e `confirmarEmbarque()` agem sem perguntar, de modo que **a única
   fronteira de autorização ativa é o servidor**;
2. **o gate das regras passa pulando o que mais importa**: `npm test` roda 103 casos e **pula 57**, os de
   passagem, cliente e veículo;
3. **a telemetria não diz quem** — 3 pontos de emissão, e o operador nunca entra nos parâmetros;
4. **criar `ADM`/`GESTOR` pelo app já funcionava** desde a F6.6, com caso de emulador provando, enquanto
   o [ADR-0021](0021-usuarios-da-plataforma-adm-only.md) D0, o estudo `usuario-e-funcionario.md` e um
   comentário dentro do próprio `firestore.rules` diziam que era impossível.

As seis decisões abaixo respondem a isso. Elas vieram juntas e compartilham o eixo; o recorte fino é
trabalho das **issues**, que passam a existir entre este documento e o código.

## Decisão

### D1 — A política volta a ser consultada **antes do gesto**

Não basta esconder o botão: quem age pergunta antes. `podeCriarPassagem` entra na emissão,
`podeConfirmarEmbarque` no embarque, `podeCadastrarMembro` no `salvar()` do funcionário, e `podeAcessar`
passa a guardar a **navegação** — que hoje não tem guarda nenhuma, e cuja única barreira é a ausência do
botão no menu.

**Isto estende o [ADR-0010](0010-autorizacao-por-cargo.md)** no ponto em que ele declarava o escopo como
*"segurança por UI"*. A fronteira que **vale** continua sendo o servidor — não se está movendo autoridade
para o cliente. O que o freio do cliente entrega são duas coisas que a regra do servidor não faz:

- **falhar antes**, para que ninguém tente o que não pode;
- **falhar explicando**, porque o Firestore devolve *permission denied* e não um motivo.

E resolve o que o estudo mediu: parte da política existia **apenas como teste de si mesma** — 41 casos
provando funções que nenhum caminho executava.

> **O que não muda:** as funções que descrevem capacidade inexistente (editar e deletar passagem — o
> `PassagemRepository` declara a ausência de propósito) **continuam sem chamador**, e agora por escrito:
> são antecipação declarada, não entulho.

### D2 — A sessão é **estado observável**

`SessaoUsuario` deixa de ser foto (`suspend fun atual()`) e passa a ser fluxo.

É a decisão mais econômica do documento: **dissolve três dos quatro acoplamentos de uma vez**.

- o menu larga o DataStore cru **sem perder reatividade** — hoje `MainScreenViewModel` lê as chaves
  direto porque `collect`a e reage, e a porta só sabia responder uma vez. Não era desleixo, era a única
  saída;
- a chave **`LOGADO` morre** — escrita no login e no logout, lida por ninguém. Ela é resíduo de uma
  decisão do [ADR-0005](0005-autenticacao-sessao-firebase-datastore.md) (*"o DataStore guarda o estado
  derivado para roteamento no Splash"*) que a execução superou por um caminho melhor: `destinoDaSplash`
  pergunta a `currentUser` + `sessaoUsuario.atual()`, ou seja, à autoridade em vez de a uma cópia;
- a **escrita dupla do login** some, porque passa a haver um dono só do estado de sessão — hoje
  `logarUsuario` chama `sessaoLocal.registrarLogin()` e, na linha seguinte, grava três chaves cruas.

**O logout passa pela porta**: `autenticacaoRepository.sair()` no lugar de `firebaseAuth.signOut()` — que
é o único ponto do app que injeta `FirebaseAuth` num ViewModel sem precisar.

### D3 — O gate das regras **roda inteiro**

`SUITE_COMPLETA` deixa de ser opção e vira o padrão do `npm test`.

E a ordem faz parte da decisão: **liga, roda e analisa**. O que ficar vermelho é **medição, não
regressão** — cada caso vermelho é uma pergunta a responder antes de qualquer conserto, e vira issue
própria. É o mesmo método do [ADR-0031](0031-classe-de-veiculo-natureza-e-casco-por-exclusao.md), onde um
teste vermelho ao acrescentar as classes *era* o argumento.

O recorte tinha razão de ser enquanto o app era refeito seção a seção. Ele perdeu a razão: o andaime já
se esvaziou (`SECOES_REVITALIZADAS` tem 9 valores e `SecaoMenu.entries` também), e a assimetria mede o
quanto isso ficou para trás — na suíte JVM o recorte custa **1,7%**; na de regras, **36%**.

### D4 — O evento é **agregado**; o carimbo é a auditoria

A observabilidade de operador é **analítica**, não auditoria. O evento responde *"quantas emissões por
agência nesta semana"* e **não** *"o que a Ana fez às 14h"*: o operador entra como **coordenada
agregável** (agência, papel, cargo), nunca como identidade.

Duas consequências que a escolha carrega, e resolve:

- **fica cego a PII por construção**, que é o que se quer de um evento que sai do aparelho;
- **não compete com o carimbo.** `MetadadosPassagem.funcionarioId` e `CarimboEmbarque.porId` continuam
  sendo o registro de quem fez, dentro do documento, onde a auditoria mora. São camadas com papéis
  diferentes: **o evento conta, o documento prova.**

Entram na mesma decisão os dois buracos que o estudo mediu: **a confirmação de embarque passa a emitir
evento** (hoje não emite nenhum), e os **23 `Log.e` de produção** que passam ao largo da telemetria
passam por `naoFatal` — hoje uma falha ao salvar cadastro é invisível no Crashlytics.

### D5 — **Dois perfis por dono**, e a escolha entre empresas morre

Uma pessoa tem no máximo **dois perfis**: um com a **plataforma** e, se precisar, um com uma **empresa** —
ou o inverso. A troca é **opção no menu**, e a capacidade é **restrita a `ADM` e `GESTOR`**.

**O vínculo entre duas empresas é descartado.** A pessoa que operava em duas agências e escolhia entre
elas deixa de existir como caso; a troca de perfil **substitui** aquela escolha — mesma mecânica, outro
eixo. Isto **supera o [ADR-0016](0016-dominio-da-plataforma.md) §6 e a F6.4**.

**Por que não custa regra nova.** Com dois perfis no mesmo uid, o servidor lê os dois sem saber de troca
nenhuma — papel de `users/{uid}`, cargo de `funcionarios/{funcionarioId}` — e **concede a união**. A troca
no app é **lente, não redução de poder**. Isso seria inaceitável para um `OPERADOR`, porque prometeria uma
separação que o servidor não faz; e é aceitável exatamente para quem esta decisão autoriza. **É o recorte
que dispensa mudar a regra**, não uma sorte.

**O invariante do [ADR-0015](0015-rework-agente-equipe.md) §8.4 é esclarecido, não quebrado.**
*"`ADM`/`GESTOR` não emitem passagem"* parecia cair — e não cai, porque a regra nunca barrou por papel:

```
allow create: if autenticado() && papelConhecido()
              && funcionarioIdDoAutor() != ''
              && request.resource.data.funcionarioId == funcionarioIdDoAutor();
```

A emissão é barrada por **não ter funcionário**. Com o perfil de empresa, o `ADM` tem um e emite **como
ele** — e a passagem nasce com o dono certo, que é o funcionário. **A regra já estava escrita na forma que
a decisão precisa**; o que muda é a paráfrase do §8.4.

**O que sai:** ~284 linhas em 5 arquivos (`SelecaoVinculoScreen`, o ViewModel, o `UiState`, o grafo e o
`EscolhaDeVinculo`), mais `precisaEscolherVinculo()` e um dos cinco ramos de `destinoDaSplash`.

**O que muda de sentido em vez de sair:** o `EscolhaDeVinculo` guardava *"em nome de qual empresa opero"*
e passa a guardar *"qual perfil está ativo"* — mesma forma (preferência no DataStore, revalidada a cada
leitura, **nunca consultada como autoridade**), outro eixo.

### D6 — O `ADM` **gere** gestores e operadores

**Não há convite de `ADM`.** O administrador entra por **console + Firestore**, e só. Hoje há um; se
houver outro, entra pelo mesmo caminho.

Isto **preserva o ADR-0021 D0 no que ele tem de mais forte** — *não existe caminho, dentro do app, para
fabricar quem administra* — e **corrige o código**, que hoje oferece os três papéis no formulário
(`FormUsuarioUiState:35`) e deixa a regra aceitar um convite de `ADM`. O convite carrega **`GESTOR` e
`OPERADOR`**; nunca `ADM`.

**A seção Usuários deixa de ser somente-leitura**, o que **supera o ADR-0021 D2**. O `ADM` passa a poder:

- **editar** o registro;
- **desativar** e **reativar** — os dois, e o par importa: desativar sem reativar transforma um engano em
  ida ao console;
- definir **data de expiração de acesso** — o acesso deixa de ser permanente-até-alguém-lembrar;
- ver **métricas de gestão de usuários**, alimentadas pelo agregado da D4.

**E desativar tem de valer no servidor.** Um acesso "desativado" que o Firestore continua aceitando não
está desativado — está escondido na UI, que é precisamente o que a D1 decidiu não bastar. O **como** está
em aberto: ver §Q1.

---

## Consequências

- **A política ganha consumidor e o teste ganha sentido.** Os 41 casos de `PermissoesUsuarioTest` passam
  a provar caminho vivo, e não a si mesmos.
- **Três derivações da atuação viram uma.** `ContextoUsuario.atuacao`, `MainScreenViewModel:135` e
  `PermissoesUsuario.atuacaoEmVigor` (que existe para isso, tem zero chamadores e cujo KDoc diz que
  *deveria* substituir as outras duas) colapsam na terceira.
- **`Convite.ehDePlataforma` deixa de reimplementar `ehPapelPlataforma`**, e as três comparações
  `papel == OPERADOR` espalhadas por `FormUsuarioUiState`, `ValidacaoUsuario` e `FormUsuarioViewModel`
  passam a perguntar à política.
- **O `Funcionario` tem no máximo um vínculo de empresa** (D5) — e `vinculos: List<Vinculo>` com um
  elemento no máximo é uma lista que mente sobre o domínio. Ver §Q2.
- **Três documentos param de contradizer o código** (ADR-0021 D0, o estudo irmão e o comentário em
  `firestore.rules:145`), e o `docs/esteira.md` para de mandar cadastrar tester pelo console para tudo:
  o `OPERADOR` e o `GESTOR` passam pelo convite; só o `ADM` continua sendo ato de ambiente.
- **O ADR-0005 fica com uma alternativa futura resolvida pela negativa**: *"multi-conta / troca rápida de
  usuário"* não vai acontecer — o que a D5 escolheu é **um dono com dois perfis**, não dois donos.
- **A suíte de regras vai ficar vermelha** (D3), e isso é resultado esperado, não acidente.

---

## Q — As três perguntas, e as respostas

Ficam **pergunta e resposta juntas**: a pergunta é o que a decisão abriu, a resposta é dele.

### Q1 — O estado do acesso mora **no próprio `users/{uid}`**, e expirar **impede a próxima operação**

Três decisões abriam o mesmo buraco — desativar/reativar (D6), expirar (D6) e ligar um perfil de empresa a
um `ADM` que já existe (D5) —, porque as três são escrita em `users/{uid}` **por um terceiro**, e a
regra admite só o próprio dono, com `papel` e `funcionarioId` imutáveis desde a P2.2a′.

**Decidido:** os campos moram no próprio documento de perfil. A regra de `update` ganha um segundo ramo
para o `ADM`, restrito por lista fechada de chaves — e o **`papel` fica fora dela para todo mundo**,
inclusive para o `ADM`, que é o que mantém o anti-escalonamento inteiro e coerente com a D6 (*não há
convite de `ADM`*; também não há promoção a `ADM`).

O argumento que decidiu é de **custo de leitura**: `papel()` **já faz** `get(users/{uid})` em toda
autorização, então `ativo` e `expiraEm` viajam num documento que a regra já tem em mãos — a verificação
sai **de graça**. A alternativa (guardar fora, ou no `convites/{email}`) cobraria um salto a mais em
**toda operação autorizada do app**, para sempre.

**E a expiração impede a próxima operação — não derruba a sessão em curso.** É o que a regra dá
naturalmente (`request.time` comparado a `expiraEm` no momento da escrita), e a escolha tem uma
propriedade que vale nomear: **quem está no meio de um atendimento termina o atendimento**. Derrubar a
sessão no relógio seria interromper uma emissão pela metade para provar pontualidade.

### Q2 — `Funcionario.vinculos` **deixa de ser lista**

Vira `vinculo: Vinculo?`. Com uma empresa no máximo (D5), a lista tinha no máximo um elemento — e
estrutura que admite o que o domínio não reconhece é convite a estado inválido. É o mesmo princípio que o
[ADR-0031](0031-classe-de-veiculo-natureza-e-casco-por-exclusao.md) aplicou à cilindrada: quando o fato
tem uma forma, a estrutura segue a forma.

Toca a fronteira, e o ADR registra o custo em vez de escondê-lo: o array no Firestore, o derivado
`empresaIds` (que vira um id só), as funções de regra `souSupervisorDe()` e
`naoMexeNosPropriosVinculos()`, e os documentos já gravados.

### Q3 — A troca de perfil se anuncia como **carregamento**, e nada além disso

*"Carregando sessão e informações do perfil."* com um indicador circular padrão. Sem tela nova, sem
cerimônia.

E a modéstia é a decisão certa pelo que a D5 estabeleceu: a troca é **lente, não redução de poder** — o
servidor concede a união. Uma tela que celebrasse a troca sugeriria uma separação que não existe; um
carregamento honesto diz o que de fato acontece, que é **recarregar o contexto**.

---

## O que este ADR **não** decide

- **não sobe o `targetSdk`**, não toca em R8 nem em build;
- **não cria tela nova** além do que a D6 exige na seção Usuários;
- **não muda o bootstrap do primeiro `ADM`** — continua sendo console, por princípio (ADR-0021 D0);
- **não transforma o evento agregado em auditoria** — a D4 separa as duas de propósito.

## Plano

As fases viram **issues**, uma por unidade entregável — é o passo novo do fluxo
(`estudo → perguntas → ADR → perguntas → issues → implementação`). A ordem sugerida põe primeiro o que
**mede** e por último o que **depende de decisão pendente**:

1. **D3** — ligar o gate inteiro e catalogar o vermelho (uma issue por caso);
2. **D2** — a sessão vira fluxo (dissolve três acoplamentos de uma vez);
3. **D1** — a política no gesto, e a guarda de navegação;
4. **D4** — evento no embarque, `naoFatal` nos formulários, coordenadas no evento;
5. **D5** — dois perfis, o que morre, e a opção no menu;
6. **D6** — o ciclo de vida, **depois da Q1**, porque é ela que decide onde o estado mora.
