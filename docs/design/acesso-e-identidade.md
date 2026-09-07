# Acesso e identidade — quem entra, quem é, e o que pode

> **Estudo, não decisão.** Mede o eixo de acesso do app (autenticação, sessão, autorização,
> observabilidade e provisionamento), nomeia o que está torto e **para nas perguntas**. Onde há
> recomendação, ela vem marcada como minha.
>
> **Complementa** o [`usuario-e-funcionario.md`](usuario-e-funcionario.md), que tratou de *quem a
> plataforma gere × quem a empresa gere* e foi executado na F6.6. Este cobre o eixo que ficou de fora:
> **o acesso**.
>
> Medido em **2026-09-07**, com o Room recém-removido (ADR-0017 F6) — a sessão mudou de casa ontem, e é
> boa hora de olhar as costuras.

---

## 1. O terreno, medido

| | |
|---|---|
| Porta de autenticação | 6 métodos; **3 ViewModels** são os únicos consumidores de toda a porta |
| Sessão | **10 chaves** no DataStore `"login"`, em 3 classes (`SessaoLocal`, `EscolhaDeVinculo`, `PreferencesKey`) |
| Porta `SessaoUsuario` | **12 ViewModels** a injetam — a peça mais bem-sucedida do eixo |
| Acesso cru | **3** ViewModels injetam `DataStore<Preferences>`; **2** injetam `FirebaseAuth` |
| Política | `PermissoesUsuario`: 286 linhas, **22 funções públicas** |
| Regras do servidor | `firestore.rules`: 533 linhas, 13 coleções, **160 casos** de emulador |
| Telemetria | porta de 3 métodos, **3 pontos de emissão** em produção |
| Testes de sessão/auth | **33 casos** JVM em 6 arquivos |

O eixo tem **duas peças que funcionam bem**, e vale dizer isso antes das críticas:

- **a porta `SessaoUsuario`** é o acerto do desenho. Doze ViewModels perguntam *"quem está operando"* a
  um lugar só, e recebem os dois contextos resolvidos (`ContextoUsuario`) — o caminho `usuário →
  funcionarioId → funcionário` não é refeito em lugar nenhum;
- **a decisão da splash é função pura.** `destinoDaSplash()` tem 9 casos de teste e uma tabela de decisão
  no KDoc; é o único ponto do eixo em que "o que acontece quando" está escrito e provado.

---

## 2. Quatro pontos de acoplamento, e o que cada um custa

### 2.1 Duas escritas para o mesmo fato

`LoginViewModel.logarUsuario()` chama `sessaoLocal.registrarLogin(usuario)` e, **na linha seguinte**,
escreve `LOGADO`/`USUARIO_ATUAL`/`CARGO_ATUAL` cru no DataStore. São dois donos do mesmo acontecimento —
"o login aconteceu" — e a divisão entre eles não é de conceito: `CARGO_ATUAL` ficou de fora do
`SessaoLocal` porque o cargo vem do **funcionário**, não do `Usuario`. É uma razão real, mal expressa:
o que falta é uma peça que represente **a sessão inteira**, e não o usuário de um lado e três chaves do
outro.

### 2.2 `LOGADO` é chave morta

Escrita no login (`true`) e no logout (`false`), **lida por ninguém**.

Ela é resíduo de uma decisão do [ADR-0005](../adr/0005-autenticacao-sessao-firebase-datastore.md): *"o
DataStore guarda o estado derivado para roteamento instantâneo/offline no Splash (`logado`,
`usuario_atual`, `cargo_atual`)"*. O código foi por outro caminho — hoje `destinoDaSplash` decide por
`firebaseAuth.currentUser` + `sessaoUsuario.atual()`, que é **melhor** (pergunta à autoridade em vez de a
uma cópia). A decisão foi superada pela execução, e ninguém voltou para apagar a chave.

### 2.3 O menu lê o DataStore cru — e isso tem uma razão

`MainScreenViewModel.obterUsuario()` lê `USUARIO_ATUAL`, `PAPEL_ATUAL` e `CARGO_ATUAL` direto do
DataStore, sem passar pela porta. É o único ViewModel que faz isso, e **não é desleixo**: ele
`collect`a `dataStore.data`, então o menu **reage** a mudança de chave. A porta expõe
`suspend fun atual(): ContextoUsuario?` — que é **foto, não fluxo**.

Trocar a leitura crua pela porta hoje **perderia reatividade**. Para o acoplamento sair, a porta precisa
ganhar um `Flow` antes — e aí a pergunta deixa de ser "quem lê o quê" e passa a ser *"a sessão é um
estado observável ou uma consulta?"*, que é decisão de desenho.

### 2.4 O logout não usa a porta

`MainScreenViewModel.deslogar()` chama `firebaseAuth.signOut()` diretamente, quando
`autenticacaoRepository.sair()` existe, faz exatamente isso, e é o caminho que `LoginViewModel` e
`PrimeiroAcessoViewModel` usam. Dois caminhos para a mesma chamada de SDK, e o de fora da porta é o único
lugar do app que injeta `FirebaseAuth` num ViewModel sem precisar.

*(O logout, em compensação, ficou correto no que importava: desde ontem ele limpa a projeção local junto
— antes a linha do Room sobrevivia à sessão.)*

### 2.5 O pequeno

`LoginViewModel:14` importa `PreferencesKey.PAPEL_ATUAL` e não a usa — resíduo da fatia de ontem, que
moveu essa chave para o `SessaoLocal`. O Kotlin avisa e não recusa.

---

## 3. A política que quase ninguém consulta

**Este é o achado central do estudo.**

Das 22 funções de `PermissoesUsuario`, **12 não têm um único chamador de produção**. Não são as
periféricas — é a **cadeia inteira da Passagem**:

| função | chamadores em produção | chamadores em teste |
|---|---|---|
| `podeCriarPassagem` | **0** | 3 |
| `podeEditarQualquerPassagem` | **0** | 6 |
| `podeEditarPassagem` | **0** | 10 |
| `podeDeletarPassagem` | **0** | 2 |
| `podeVerTodasPassagens` | **0** | 3 |
| `podeConfirmarEmbarque` | **0** | 4 |
| `podeCadastrarFuncionario` | **0** | 4 |
| `podeCadastrarMembro` | **0** | 4 |
| `podeCriarRota` · `podeCriarViagem` | **0** | 1 · 3 |
| `atuacaoEmVigor` | **0** | 2 |

`EmissaoViewModel.emitir()` e `EmbarqueViewModel.confirmarEmbarque()` **agem sem perguntar à política**.
`FormFuncionarioViewModel.salvar()` também: ele usa a política para decidir **quais campos mostrar**
(`podeEscolherAgencia`, `podeDefinirCargo`) e não para decidir **se pode gravar**.

### 3.1 O que isso significa, dito com precisão

**A única fronteira de autorização ativa para a passagem é o servidor.** As regras impõem tudo
(`papelConhecido()`, `podeEditarQualquerPassagem()`, `ehDonoPassagem()`, `ehConfirmacaoEmbarque()`), e
elas estão certas — mas o freio do cliente que o desenho previa não está ligado.

Vale ser justo com o histórico: o [ADR-0010](../adr/0010-autorizacao-por-cargo.md) declarou o escopo como
**"segurança por UI"** (esconder menu e botão), e o [ADR-0011](../adr/0011-regras-firestore-por-cargo.md)
pôs a fronteira real no servidor. Ou seja, a arquitetura **nunca prometeu** que o cliente barraria a
ação. O que a medição mostra é mais estreito e mais concreto: **parte da política existe apenas como
teste de si mesma** — 41 casos em `PermissoesUsuarioTest` provando funções que nenhum caminho de produção
executa.

Isso tem dois nomes possíveis, e a diferença importa:

- ou é **antecipação** — a política descreve capacidades que o app ainda não tem (não existe tela de
  editar/deletar passagem: `PassagemRepository` declara a ausência de propósito), e o dia em que
  existirem ela estará pronta;
- ou é **política sem consumidor**, que envelhece sem ninguém perceber, como o `observarDocumento` que
  saiu esta semana — a porta que ninguém chamava e que o fake de teste tinha de cumprir mesmo assim.

Para **três** delas eu não vejo a defesa da antecipação: `podeCriarPassagem` e `podeConfirmarEmbarque`
têm o consumidor **hoje** (a emissão e o embarque existem, funcionam e são usados), e `podeAcessar`
governa a navegação que hoje não tem guarda.

### 3.2 A navegação não tem guarda

Nenhum `NavGraphBuilder` consulta permissão. A única barreira é **a ausência do botão no menu** — o que
significa que um destino alcançado por outro caminho (back-stack, deep link, um `navigate` que alguém
escreva amanhã) não é reautorizado no cliente.

### 3.3 O mesmo fato derivado três vezes

A **atuação** (o segmento em que a pessoa opera) é derivada de forma independente em três lugares:

1. `ContextoUsuario.atuacao` — `Funcionario.Cargo.de(cargo)?.atuacao`;
2. `MainScreenViewModel:135` — a mesma expressão, sobre a chave crua do DataStore;
3. `PermissoesUsuario.atuacaoEmVigor(vinculo)` — que existe **exatamente para isso**, tem **zero**
   chamadores, e cujo KDoc diz textualmente que *"é esta função que substitui a derivação de hoje no
   `ContextoUsuario`"*. A substituição documentada nunca aconteceu.

E `Convite.ehDePlataforma` (`papel == ADM || papel == GESTOR`) reimplementa
`PermissoesUsuario.ehPapelPlataforma` no domínio do convite — mais três comparações
`papel == OPERADOR` espalhadas entre `FormUsuarioUiState`, `ValidacaoUsuario` e `FormUsuarioViewModel`.

Nenhuma delas está *errada*. O custo é o de sempre com regra duplicada: no dia em que a política mudar —
um terceiro papel de plataforma, por exemplo —, esses pontos não acompanham, e o sintoma aparece longe da
causa.

---

## 4. O gate de regras passa pulando o que mais importa

`firestore-tests/rules.test.js:190`:

```js
const foraDoEscopo = process.env.SUITE_COMPLETA ? describe : describe.skip;
```

E o `npm test` — o comando que **os dois workflows** do CI executam — não define `SUITE_COMPLETA`.

**Medido: 103 casos rodam, 57 são pulados.** Os pulados são `passagens` (posse, FSM de embarque, cota de
gratuidade), `clientes` e `veiculos` (assinatura, PII) — ou seja, **a autorização mais nova e mais
complexa do sistema é a que o gate não exercita**.

O mecanismo é o mesmo recorte da revitalização que a suíte JVM usa, e foi legítimo enquanto o app estava
sendo refeito seção a seção. O ponto é que **o andaime já se esvaziou**: `SECOES_REVITALIZADAS` tem 9
valores e `SecaoMenu.entries` tem 9 — o filtro do menu é hoje um no-op, exatamente como o ADR-0027
previu ao dizer que ele se esvaziaria ao fim da F9.6.

A assimetria mede o quanto isso ficou para trás:

| suíte | no escopo | fora | fora do escopo |
|---|---:|---:|---:|
| JVM | 828 | 14 | **1,7%** |
| regras (emulador) | 103 | 57 | **36%** |

O `firestore-tests/README.md` descreve a cobertura de passagens como parte normal da suíte, sem mencionar
que ela está desligada por padrão. Documentação e comportamento divergem.

---

## 5. Observabilidade: a porta existe, e ela não diz quem

O app **já tem** a peça: `Telemetry` (`evento` / `rastro` / `naoFatal`) sobre Analytics + Crashlytics, com
três registradores semânticos por cima — `RegistroCadastro`, `RegistroEmissao`, `RegistroSincronizacao`.
O desenho é bom: o ViewModel não conhece Firebase, e o vocabulário do evento é do negócio
(`passagem_salva`, `sync_erro`), não do SDK.

Três achados sobre **o uso** dela:

### 5.1 Três pontos de emissão

Só `ColecaoFirestore` (todo CRUD genérico), `PassagemFirestoreRepository.emitir()` e
`SincronizacaoFirestore` emitem. **Não emitem nada**: login, logout, primeiro acesso, convite criado e —
o mais relevante para o seu pedido — **a confirmação de embarque**.

### 5.2 Nenhum evento diz quem fez

Os `params` levam `numero`, `motivo`, `fase`. **O operador nunca entra.**

Quem fez existe, mas **dentro do documento**: `MetadadosPassagem.funcionarioId` na emissão e
`CarimboEmbarque.porId` no embarque — ambos por id, nunca por nome, como o ADR-0008 manda. Ou seja, hoje
o sistema **sabe** quem fez cada coisa e **não consegue contar** o que aconteceu sem varrer as passagens.

### 5.3 Vinte e três `Log.e` fora da telemetria

Todo `Form*ViewModel` termina o `catch` com `Log.e(TAG, "salvar: ...")` e **nunca** `naoFatal`. Falha ao
salvar um cadastro é invisível no Crashlytics — fica no logcat do aparelho de quem viu o erro.

### 5.4 A pergunta que isto levanta é de domínio, não de instrumentação

"Observabilidade orientada a evento para operadores" pode significar duas coisas muito diferentes, e a
escolha entre elas não é técnica:

- **evento analítico** (Analytics): barato, agregado, cego a PII por obrigação — responde *"quantas
  emissões por agência nesta semana"*, e não *"o que a Ana fez às 14h"*;
- **fato do negócio** (documento): caro, consultável, auditável — responde a segunda pergunta, e por isso
  mesmo é dado pessoal com tudo o que isso implica.

O app hoje tem o **segundo, parcial** (os carimbos) e o **primeiro, sem o operador**. Elas não competem:
a pergunta é qual delas o negócio precisa, e para quê.

---

## 6. O provisionamento que já existe, e os três textos que o negam

**A capacidade de criar perfil de `ADM` e `GESTOR` pelo app está construída — e provada no servidor.**

O caminho fecha ponta a ponta:

| etapa | onde | o que faz |
|---|---|---|
| o formulário | `FormUsuarioUiState:35` | oferece `Usuario.Papel.entries` — **os três**, sem filtro |
| o convite | `Convite.papel` | carrega o papel; `ehDePlataforma` já distingue |
| o primeiro acesso | `PrimeiroAcessoViewModel.nascerPerfil(papel, …)` | repassa o papel do convite |
| a regra | `firestore.rules:159-163` | exige `request.resource.data.papel == papelDoConvite()` |

E a suíte do emulador tem o caso, com o comentário: *"o que a F6.6 destrava: `ADM`/`GESTOR` passam a
poder nascer pelo app — desde que exista um convite, que só o ADM escreve."*

### 6.1 Três documentos afirmam o contrário

- **ADR-0021 D0**: *"não existe caminho, dentro do app, para fabricar quem administra"*;
- **`usuario-e-funcionario.md`**, tabela: *"Quem cria ADM/GESTOR | **ninguém pelo app** — console"*;
- **`firestore.rules:145`**: *"sempre no menor privilégio — nunca nasce ADM/GESTOR pelo cliente (esses
  vêm do console)"* — **três linhas acima da regra que o contradiz**, num arquivo de segurança.

A cronologia explica sem culpar ninguém: o D0 é de **04/08**; o commit do convite é de **08/08**, quatro
dias depois. **Nenhum ADR revisou o 0021**, e o `docs/esteira.md` segue instruindo cadastrar tester pelo
console — foi o que fiz esta semana ao adicionar um testador.

### 6.2 O que de fato continua bloqueado

Só o **primeiro** `ADM`: convidar exige `ehAdm()` na regra, e para ser ADM é preciso um convite que só um
ADM escreve. O bootstrap continua sendo ato de ambiente, e isso é coerente com o D0 no que ele tem de
mais forte.

### 6.3 O que ninguém decidiu

- **nada restringe o papel do convidado.** Um `ADM` convida outro `ADM`. Isso pode ser exatamente o
  desejado (é assim que se sai de um administrador só, que é o gatilho de revisitação que o próprio D0
  nomeia) — mas é decisão, e hoje está implícita;
- **não há revogação.** A seção é somente-leitura (ADR-0021 D2): dá para convidar, não dá para desfazer.
  Quem entrou, entrou; tirar exige console;
- **o convite não expira** e o campo `usado` é marcado pelo próprio convidado.

---

## 7. As respostas (analista, 2026-09-07)

As cinco perguntas foram respondidas no mesmo dia. Ficam abaixo **pergunta e decisão juntas** — a
pergunta é o que a medição levantou, a decisão é dele.

### 7.1 O freio do cliente **volta**

A política volta a ser consultada antes do gesto, e não só antes de desenhar o menu. As doze funções sem
chamador deixam de ser "antecipação ou entulho": as que têm consumidor hoje passam a tê-lo de fato —
`podeCriarPassagem` na emissão, `podeConfirmarEmbarque` no embarque, `podeCadastrarMembro` no
`salvar()` do funcionário, `podeAcessar` na navegação.

Isto **estende o [ADR-0010](../adr/0010-autorizacao-por-cargo.md)** no ponto em que ele se declarava
"segurança por UI": a fronteira do servidor continua sendo a que vale, e o cliente passa a ter o segundo
freio — que serve para duas coisas que a regra do servidor não faz: **falhar antes** (a pessoa não tenta
o que não pode) e **falhar explicando** (o servidor devolve *permission denied*, não um motivo).

### 7.2 A sessão é **estado observável**

`SessaoUsuario` deixa de ser foto e passa a ser fluxo. É a decisão que dissolve três dos quatro
acoplamentos de uma vez: o menu deixa de ler o DataStore cru **sem perder reatividade**, a chave
`LOGADO` perde a última desculpa, e a escrita dupla do login some porque passa a haver um dono só do
estado de sessão.

### 7.3 O recorte da suíte de regras: **liga, roda e analisa**

`SUITE_COMPLETA` passa a ser o padrão. A ordem importa e está na decisão: **ligar, rodar e analisar** —
o que ficar vermelho é medição, não regressão, e cada caso vermelho é uma pergunta a responder antes de
qualquer conserto. É o mesmo método que o ADR-0031 usou quando um teste ficou vermelho ao acrescentar as
classes: o vermelho *era* o argumento.

### 7.4 Observabilidade: **analytics agregado**

O evento é analítico, não auditoria. Responde *"quantas emissões por agência nesta semana"*, e **não**
*"o que a Ana fez às 14h"* — o operador entra como **coordenada agregável** (agência, papel, cargo), não
como identidade.

Duas consequências que essa escolha carrega, e que ela resolve de graça:

- **fica cego a PII por construção**, que é o que se quer de um evento que sai do aparelho;
- **não compete com o carimbo.** `MetadadosPassagem.funcionarioId` e `CarimboEmbarque.porId` continuam
  sendo o registro de quem fez, dentro do documento, onde a auditoria mora. São camadas com papéis
  diferentes: o evento conta, o documento prova.

### 7.5 O ciclo de vida do usuário da plataforma

A resposta foi maior que a pergunta, e reorganiza a §6 inteira.

**Não há convite de `ADM`.** O administrador entra por **console + Firestore**, e só. Hoje há um; se
houver outro, entra pelo mesmo caminho. Isso **preserva o [ADR-0021](../adr/0021-usuarios-da-plataforma-adm-only.md)
D0 no que ele tem de mais forte** — *não existe caminho, dentro do app, para fabricar quem administra* —
e **corrige o código**, que hoje oferece os três papéis no formulário e deixa a regra aceitar um convite
de `ADM`. A lacuna que a §6.3 apontou (*"nada restringe o papel do convidado"*) fecha por decisão: o
convite carrega **`GESTOR` e `OPERADOR`**, nunca `ADM`.

**O `ADM` gere os gestores e os operadores**, e a seção Usuários deixa de ser somente-leitura — o que
**supera o ADR-0021 D2**. Ele passa a poder:

- **editar** o registro;
- **desativar** e **reativar** o acesso — os dois, e o par importa: desativar sem reativar transformaria
  um engano em ida ao console;
- definir **data de expiração de acesso**, que é conceito novo no domínio: o acesso deixa de ser
  permanente-até-alguém-lembrar e passa a ter fim declarado;
- ver **métricas de gestão de usuários** — e é aqui que a §7.4 encosta nesta: o agregado que o evento
  produz é o que alimenta essa tela.

**O que isto abre, e que o ADR terá de responder:** desativar um acesso é escrever no `users/{uid}`, e
hoje a regra permite ao dono editar o próprio perfil **menos** papel e vínculo — não há caminho para um
terceiro escrever ali. Expiração e desativação precisam ser lidas **pelo servidor** para valerem, senão
são enfeite de UI: um acesso "desativado" que o Firestore continua aceitando não está desativado.

---

## 8. Adendo — "Adicionar Perfil existente"

**O pedido:** `ADM` e `GESTOR` podem *adicionar um perfil existente* e **trocar de perfil sem
relogar**, só carregando as informações do perfil.

Antes de desenhar, uma restrição do código que decide entre leituras muito diferentes — e é por isso que
este adendo não propõe solução.

### 8.1 A restrição

**O servidor decide por `request.auth.uid`.** Toda regra do `firestore.rules` parte daí: `papel()`
lê `users/{request.auth.uid}`, `cargoDoAutor()` faz o segundo salto pelo `funcionarioId` desse
documento. E o cliente tem **um `currentUser` por vez** — o SDK do Firebase Auth não guarda duas sessões
na mesma instância.

Disso decorre o que qualquer desenho aqui tem de encarar: **se a troca de perfil não trocar o uid
autenticado, o servidor continua vendo a pessoa original.** A tela mostraria um perfil e o Firestore
aplicaria outro — e o sintoma seria a UI oferecendo seções cujas escritas voltam negadas.

Isso não é impedimento; é a linha que separa três coisas que o mesmo pedido pode significar.

### 8.2 As três leituras

**(a) Perfis do mesmo dono.** A pessoa tem mais de um perfil e escolhe qual está ativo. O app **já faz
isso um nível abaixo**: `EscolhaDeVinculo` guarda em nome de qual empresa alguém opera, e
`ContextoUsuario.vinculoAtivo` revalida a escolha contra os vínculos atuais a cada leitura — *"um dado
que não é consultado como autoridade não precisa de invalidação ativa"*. Subir isso um andar (do vínculo
para o perfil) é o caminho mais curto, **mas exige que o servidor saiba qual perfil está ativo**, e hoje
`users/{uid}.papel` é um valor só.

**(b) Ver como outro.** O `ADM` carrega o perfil de alguém para ver o que essa pessoa vê. O servidor
**não se engana** (continua indo pelo uid), então isto é honestamente uma **prévia de UI** — útil para
diagnóstico (*"por que o supervisor não enxerga a viagem?"*), e que precisa dizer na tela que é prévia,
porque nenhuma escrita vai funcionar ali.

**(c) Troca rápida de conta.** Duas contas de verdade, alternando sem repetir a senha toda vez. É o que o
**[ADR-0005](../adr/0005-autenticacao-sessao-firebase-datastore.md) já previu** em *Alternativas
futuras*: *"multi-conta / troca rápida de usuário: o DataStore de sessão evolui para uma lista de perfis
em cache, com `currentUser` decidindo o ativo"*. Note a segunda metade — **o `currentUser` decide**: o
cache é conveniência, a autoridade continua sendo o Auth. Tecnicamente dá para manter sessões paralelas
com instâncias separadas de `FirebaseApp`, e o custo disso é real (duas árvores de dependência, dois
caches offline, e o "quem está logado" deixando de ter resposta única).

### 8.3 O que muda conforme a leitura

| | (a) perfis do mesmo dono | (b) ver como outro | (c) troca de conta |
|---|---|---|---|
| troca o uid? | não | não | **sim** |
| o servidor acompanha? | só se o perfil ativo virar dado que a regra lê | não — e não precisa | sim, naturalmente |
| escreve? | sim | **não** (prévia) | sim |
| precedente no app | `EscolhaDeVinculo` | nenhum | `ADR-0005`, alternativas futuras |
| risco principal | perfil ativo que a regra ignora | ser confundido com permissão | "quem está logado" sem resposta única |

### 8.4 A pergunta

**Qual delas é o fluxo?** Descrito como a pessoa o vive — quem abre, o que vê na tela, o que acontece
quando ela toca em "adicionar", e o que ela consegue *fazer* depois de trocar (só olhar, ou também
emitir/gravar). É essa última metade que decide se o servidor precisa entrar na conversa.

Minha leitura do enunciado — *"sem relogar, só carregando informações do perfil"* — aponta para **(b)**,
e nesse caso o desenho é o mais barato dos três e o mais fácil de errar: barato porque não toca em Auth
nem em regra; fácil de errar porque uma prévia que não se anuncia como prévia vira, na cabeça de quem
usa, uma promessa de permissão.

---

## 9. O que vem depois

O processo passa a ter uma etapa a mais, decidida junto com estas respostas:

**estudo → perguntas → ADR → perguntas → issues → implementação de issue**

As **issues nascem do ADR**, e não do código: elas escopam e organizam as tarefas que o estudo levantou,
mantêm a documentação do que foi feito e **é para elas que se volta** quando aparecer defeito. O que este
estudo mediu vira, portanto, um conjunto de issues rastreáveis — e não uma fatia grande e opaca.
