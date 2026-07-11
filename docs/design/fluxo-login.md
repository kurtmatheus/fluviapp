# Fluxo de Autenticação — Documentação

Documentação completa do fluxo de autenticação **implementado**. Base de arquitetura:
[ADR-0005](../adr/0005-autenticacao-sessao-firebase-datastore.md) (sessão Firebase + DataStore,
sem senha local).

Quatro capacidades, todas em produção:

1. **Login** com e-mail/senha (com gate de e-mail verificado).
2. **Cadastro** + verificação de e-mail (auto-provisiona perfil; trata colisão de e-mail).
3. **Recuperação de senha** (link built-in do Firebase, em tela própria).
4. **Login com Google** (Credential Manager).

---

## Máquina de estados

```
Splash
  ├─ FirebaseAuth.currentUser != null ─────────────────────────► Main
  └─ senão ─► Login
                 ├─ [Entrar] e-mail/senha ──► gate e-mail verificado ─► Main
                 ├─ [Entrar com Google] ─► Credential Manager ─► Main
                 ├─ [Criar conta] ─► Cadastro ─► envia verificação ─► volta ao Login
                 └─ [Esqueci a senha] ─► Recuperação ─► e-mail de reset ─► volta ao Login
```

**Autoridade da sessão = `FirebaseAuth.currentUser`** (persistido pelo SDK, offline-capaz). O
DataStore é **cache** do perfil (nome/cargo) para roteamento e exibição rápida (ADR-0005).

---

## Arquitetura — a porta de autenticação (DIP)

O ViewModel depende de uma **interface de domínio**, nunca do `Task`/exceção do Firebase. Isso
mantém a regra (gate, cadastro, mapeamento de erro) **JVM-testável** com um fake.

```
ViewModel ──► AutenticacaoRepository (porta)      ◄── FakeAutenticacaoRepository (testes JVM)
                        ▲
                        └── FirebaseAutenticacaoRepository (única classe que toca Firebase)
```

### Contrato (`AutenticacaoRepository`)

| Operação | Efeito |
|---|---|
| `autenticar(email, senha)` | `signInWithEmailAndPassword`; `Sucesso.emailVerificado` reflete o estado |
| `cadastrar(email, senha)` | `createUserWithEmailAndPassword` + `sendEmailVerification` |
| `reenviarVerificacao(email, senha)` | login → reenvia verificação → `signOut` |
| `recuperarSenha(email)` | `sendPasswordResetEmail` (link built-in) |
| `autenticarComGoogle(idToken)` | `signInWithCredential` + auto-provisiona `users/{uid}` se ausente |
| `perfilAutenticado()` | lê `users/{uid}` (perfil autoritativo) → `PerfilAutenticado?` |
| `criarPerfil(email, nome, cargo)` | grava `users/{uid}` |
| `sair()` | `signOut` |

### Resultado de domínio

`ResultadoAutenticacao` = `Sucesso(emailVerificado)` | `Falha(motivo)`, com
`MotivoFalhaAuth` ∈ { `CREDENCIAL_INVALIDA`, `USUARIO_INEXISTENTE`, `EMAIL_JA_CADASTRADO`,
`DESCONHECIDO` }.

- **Tradução na borda:** `FirebaseAutenticacaoRepository.motivoDe(e)` converte a exceção do
  Firebase para o enum (o resto do app nunca vê `FirebaseAuthException`).
- **Mensagem na UI:** `mapearMensagemErroAuth(motivo)` mapeia o enum → `R.string` (puro,
  testável em todos os ramos).

`PerfilAutenticado(id, email, nome, cargo)` é o perfil de domínio lido do Firestore após o login
— usado para semear a sessão sem depender do listener do Room (ver §Google).

---

## Sessão e roteamento

- **Splash** (`SplashScreenViewModel`): decide por `firebaseAuth.currentUser != null` →
  `Logado`/`Deslogado`; `SplashGraphNavigation` navega (`navegaLimpo`, `popUpTo(0)`).
- **Perfil em sessão** (`LoginViewModel.logarUsuario`): grava no DataStore `LOGADO`,
  `USUARIO_ATUAL` (nome) e `CARGO_ATUAL`.
- **Main** (`MainScreenViewModel`): lê o DataStore para nome/cargo (ex.: `isDiretorOuAdm`) e
  religa os listeners do Firestore.
- **Logout** (`MainScreenViewModel.deslogar`): `firebaseAuth.signOut()` + limpa o DataStore.

---

## Perfil do usuário

`users` (Firestore) espelhado no Room (`UsuarioDao`), documento `UsuarioDocumento(email, nome,
cargo)` sob `users/{uid}`.

- **Provisionamento automático:** cadastro (e-mail/senha) e 1º login Google criam o doc de perfil
  → elimina o seed manual de usuários (`SeedFirestore` mantém só constants/empresas/navios/agents).
- **Cargo default de auto-cadastro:** `"OPERADOR"` (menor privilégio). No Google, o perfil só é
  criado **se ausente**, para não sobrescrever o cargo de quem já existe.

---

## Os quatro fluxos

### 1. Login e-mail/senha
`LoginScreen` → `LoginViewModel.validarLogin()` (valida o form via `LoginFormHelper`) →
`autenticar`. Em `Sucesso`:
- **e-mail verificado** → `salvarUsuarioAutenticado(email)` (Room) → `logarUsuario` (DataStore) →
  `state.logado = true`.
- **não verificado** → `sair()` + mensagem "confirme seu e-mail" + expõe **Reenviar verificação**.

Navegação: `LaunchedEffect(state.logado)` chama `sincronizar(context)`, que liga os listeners do
Firestore e então `onNavegaParaMainScreen()`.

### 2. Cadastro + verificação
`CadastroScreen` → `CadastroViewModel.cadastrar()` (valida via `CadastroFormHelper`) →
`cadastrar` (cria + envia verificação) → `criarPerfil` → `sair()`. Não entra logado: o gate (§1)
exige e-mail verificado.
- Sucesso → `cadastrado = true` → toast + volta ao login.
- **Colisão** (`EMAIL_JA_CADASTRADO`) → `irParaLoginComEmail` → volta ao login **com o e-mail
  pré-preenchido** (via `savedStateHandle[ARG_EMAIL_PREFILL]`).

### 3. Recuperação de senha
Tela própria (`RecuperarSenhaScreen` + `RecuperarSenhaViewModel`) — **não** reaproveita
silenciosamente o e-mail do login. O campo pode vir pré-preenchido (arg de navegação, por
conveniência) mas é editável/independente (lido via `SavedStateHandle`). `recuperar()` valida
campo vazio e chama `recuperarSenha` → feedback inline (accent = sucesso, vermelho = erro).

> **Trade-off (mantido):** usamos o **link** built-in (`sendPasswordResetEmail`) — zero backend.
> Um fluxo por "código de 6 dígitos" exigiria Cloud Functions; fica como opção futura.

### 4. Login com Google (Credential Manager)
Fluxo moderno (`androidx.credentials` + `googleid`), evitando o `GoogleSignInClient` legado.

```
LoginScreen [Entrar com Google]
  → GoogleCredentialProvider.obterIdToken(context, default_web_client_id)   (borda de UI)
  → LoginViewModel.autenticarComGoogle(idToken)
       → repo.autenticarComGoogle → signInWithCredential + auto-provisiona users/{uid}
       → repo.perfilAutenticado() → semeia Room → salvarUsuarioAutenticado → logarUsuario
  → state.logado = true → LaunchedEffect → sincronizar → Main
```

- **`GoogleCredentialProvider`** fica **fora da porta** porque precisa de `Context` de Activity
  para renderizar o seletor de contas. O ViewModel recebe apenas o `idToken` (String) e segue
  testável.
- **`serverClientId` = `R.string.default_web_client_id`**, gerado pelo plugin `google-services` a
  partir do `oauth_client` type 3 (web) do `google-services.json` — sem hardcode.
- **Cancelamento** do seletor (`GetCredentialCancellationException`) é silencioso; outras falhas
  → `falhaLoginGoogle()`.
- **Anti-corrida:** como o perfil pode ter acabado de ser criado, lemos o perfil autoritativo
  (`perfilAutenticado()`) e semeamos o Room **antes** de marcar a sessão — não dependemos do
  listener `carregarUsuarios`.

---

## Navegação

`FluviAppNavHost` compõe os grafos; destinos em `FluviAppGraphDestinations`.

| Grafo / destino | Telas |
|---|---|
| `splashGraph` | Splash → roteia Login/Main |
| `loginGraph` | Login, `recuperarSenha?email_prefill={...}`, Cadastro |
| `mainScreenGraph` | Main |

- **Prefill de e-mail** (`ARG_EMAIL_PREFILL`): usado em dois sentidos — cadastro→login (colisão,
  via `savedStateHandle`) e login→recuperação (arg de rota opcional, `defaultValue = ""`).
- Helpers em `NavControllerExtensions` (`navegaParaRecuperarSenha`, `navegaLimpo`, etc.).

---

## Pré-requisitos de console (checklist)

- [x] Authentication → **Email/Password** e **Google** habilitados (Google exige e-mail de suporte).
- [x] **SHA-1** (debug/release) no app Android + `google-services.json` atualizado.
  Debug atual: `BD:16:2A:8D:D9:A8:80:07:6E:8B:BB:D9:80:F3:F9:78:ED:F6:87:0C`.
- [x] **Cloud Firestore API habilitada** + banco criado + regras permitindo `request.auth != null`.
- [ ] Templates de e-mail (verificação + reset) — revisar remetente/idioma.

> **Pegadinha vivida (projeto recriado):** com a Firestore API desabilitada, o login Google
> autenticava (Auth persiste `currentUser`), mas o `get()` de `users/{uid}` falhava com
> `PERMISSION_DENIED`/"client is offline" → o app mostrava "Falha na Autenticação" e não navegava,
> porém reabria logado (Splash confia no `currentUser`). Sintoma clássico de **Auth OK + Firestore
> indisponível**. Correção: habilitar a Firestore API / criar o banco / ajustar regras.

---

## Diagnóstico (logging)

Falhas de auth eram engolidas na borda (`motivoDe` colapsa em `DESCONHECIDO`). Há logs em três
camadas para diagnosticar sem depender de reprodução às cegas:

| Tag | Camada | O que loga |
|---|---|---|
| `FirebaseAuthRepo` | borda Firebase | exceção crua de `autenticarComGoogle` |
| `loginGraph` | borda de UI | falha/cancelamento do Credential Manager |
| `loginViewModel` | ViewModel | `Falha.motivo` e perfil ausente pós-login |

Captura: `adb logcat -c` → reproduzir → `adb logcat -d | grep -E "FirebaseAuthRepo|loginGraph|loginViewModel"`.

---

## Testes

- **Fake da porta** (`FakeAutenticacaoRepository`): controla `resultado`/`perfil` e registra
  `perfilCriado`/`saiuVezes` — cobre os ramos do ViewModel sem rede.
- **Regras puras JVM:** `ValidacaoLogin`, `ValidacaoCadastro`, `mapearMensagemErroAuth`
  (apartadas da rede, todos os ramos).

---

## Impacto no código (mapa de arquivos)

| Arquivo | Responsabilidade |
|---|---|
| `…/autenticacao/AutenticacaoRepository.kt` | Porta (contrato de domínio) |
| `…/autenticacao/FirebaseAutenticacaoRepository.kt` | Impl Firebase (única borda) |
| `…/autenticacao/ResultadoAutenticacao.kt` | `ResultadoAutenticacao`, `MotivoFalhaAuth`, `PerfilAutenticado` |
| `…/autenticacao/GoogleCredentialProvider.kt` | Borda Credential Manager (idToken) |
| `ui/viewmodel/LoginViewModel.kt` | Login e-mail/senha + Google + gate + sessão |
| `ui/viewmodel/CadastroViewModel.kt` | Cadastro + verificação + colisão |
| `ui/viewmodel/RecuperarSenhaViewModel.kt` | Recuperação (tela própria) |
| `ui/viewmodel/SplashScreenViewModel.kt` | Roteamento por `currentUser` |
| `ui/screens/{Login,Cadastro,RecuperarSenha}Screen.kt` | UIs |
| `navigation/graphs/{Splash,Login}GraphNavigation.kt` | Grafos de navegação |

---

## Limitações conhecidas / futuro

- **Sessão órfã:** se o Auth sucede mas o carregamento do perfil falha, o `currentUser` fica
  persistido enquanto a tela mostra erro (reabrir loga). Endurecer com `signOut()` no erro
  pós-`signInWithCredential` + mensagem específica ("falha ao carregar perfil").
- **Recuperação por código** (6 dígitos): exigiria Cloud Functions.
- **Regras do Firestore:** hoje `request.auth != null`; produção pede regras por papel/cargo.