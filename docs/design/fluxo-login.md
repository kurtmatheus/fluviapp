# Fluxo de Login — Estudo de Design

Documenta o fluxo de autenticação atual e o alvo, antes de implementar. Base de arquitetura:
[ADR-0005](../adr/0005-autenticacao-sessao-firebase-datastore.md) (sessão Firebase + DataStore,
sem senha local).

## Estado atual (concreto)

- **Splash** (`SplashScreenViewModel`): lê DataStore `LOGADO` → `Logado`/`Deslogado`.
- **Login** (`LoginViewModel.validarLogin`): tenta Room (`obterPorEmailSenha`, quebrado) → senão
  `signInWithEmailAndPassword` → sucesso: `salvarUsuarioAutenticado` (perfil por e-mail no Room,
  vindo do Firestore `users`) → `logarUsuario` grava DataStore → `sincronizar` liga os listeners
  e navega.
- **Sem** cadastro in-app, **sem** Google, **sem** recuperação de senha.
- Provisionamento de usuário é manual (console) + `SeedFirestore` (`users`).

Limitações: senha guardada localmente (ADR-0005 remove); perfil precisa existir em `users`
antes do 1º login (fonte do bug "signed in mas não navegou").

## Estado alvo — máquina de estados

```
Splash
  ├─ currentUser != null (+ e-mail verificado) ─────────────► Main
  └─ senão ─► Login
                 ├─ [Entrar] email/senha ──► verifica e-mail ─► Main
                 ├─ [Criar conta] ─► Cadastro ─► envia verificação ─► aguarda confirmação ─► Login
                 ├─ [Entrar com Google] ─► Credential Manager ─► Main
                 └─ [Esqueci a senha] ─► Recuperação ─► e-mail de reset ─► Login
```

Fonte de verdade da sessão = `FirebaseAuth.currentUser`; DataStore = cache p/ roteamento
instantâneo/offline (ADR-0005).

## Capacidades

### 1. Login (email/senha) — refino do existente
- `signInWithEmailAndPassword`. Após sucesso: `currentUser.reload()` → **bloquear se
  `!isEmailVerified`** (mensagem "confirme seu e-mail") e oferecer reenviar verificação.
- Perfil: buscar `users` por e-mail; se ausente, **criar on-the-fly** (ver §2) em vez de falhar.
- Offline: 1º login exige rede — detectar e mostrar mensagem clara (não simular local).

### 2. Cadastro + verificação de e-mail (novo)
- `createUserWithEmailAndPassword(email, senha)` → `currentUser.sendEmailVerification()`
  (envia **link** de verificação — template do Firebase).
- Criar o doc de perfil em Firestore `users` (email/nome/cargo) no cadastro → **elimina o
  provisionamento manual/seed de usuários**.
- Cargo default de auto-cadastro: definir (ex.: um perfil de menor privilégio) — decisão de
  negócio a confirmar.
- Fluxo: cadastrou → "enviamos um link de confirmação" → só libera login após `isEmailVerified`.

### 3. Google Sign-In (novo)
- **Credential Manager** (moderno): `androidx.credentials` + `credentials-play-services-auth` +
  `googleid` (`GetGoogleIdOption`) → `GoogleAuthProvider.getCredential(idToken, null)` →
  `signInWithCredential`. (Evitar o `GoogleSignInClient` legado/deprecado.)
- 1º login Google → criar perfil em `users` (§2).
- **Pré-requisitos de console (seus):** habilitar provedor Google; adicionar **SHA-1** (debug e
  release) no app Android do `fluvi-app-dev`; o `web client id` sai no `google-services.json`
  (`oauth_client`). Sem SHA-1 → token inválido.

### 4. Recuperação de senha (novo) — **trade-off importante**
- **Built-in (simples, recomendado):** `sendPasswordResetEmail(email)` → o Firebase manda um
  **LINK** de redefinição (página hospedada) — **zero backend**.
- **"Código no e-mail" (o que você pediu):** o Firebase **não** oferece código de 6 dígitos
  pronto. Um fluxo por código exige **Cloud Functions** (gerar/guardar código, e-mail custom,
  verificar) — um backend, bem mais trabalho.
- **Recomendação:** começar com o **link** built-in (custa quase nada e resolve o caso de uso);
  deixar o fluxo por código como opção futura, se o requisito de UX exigir.

## Prerequisitos de console (checklist do usuário)

- [ ] Authentication → habilitar **Email/Password** (feito) e **Google**.
- [ ] Adicionar **SHA-1** (debug + release) no app Android (para o Google).
- [ ] Templates de e-mail (verificação + reset) — revisar remetente/idioma.
- [ ] Regras do Firestore: escrita em `users` no cadastro (test mode hoje; produção = regra por
      `request.auth`).

## Impacto no código (resumo, sem implementar ainda)

- `FirebaseAuthRepository`: + `cadastrar`, `enviarVerificacao`, `recuperarSenha`,
  `autenticarComGoogle`, `usuarioAtual()`, `sair()`.
- `Usuario` (Room): remover `senha` (migração) — ADR-0005.
- `LoginViewModel`: estados p/ cadastro/recuperação; gate de e-mail verificado; roteamento por
  `currentUser`.
- Splash: preferir `currentUser`, DataStore como dica.
- Perfil auto-criado em `users` no cadastro/Google → `SeedFirestore` deixa de precisar semear
  usuários (mantém agents/constants/empresas/navios).
- Deps novas: `androidx.credentials` + `credentials-play-services-auth` + `googleid`.

## Faseamento sugerido

1. **Base ADR-0005**: remover senha local, sessão por `currentUser` + DataStore, gate de e-mail
   verificado. (Refator + migração; testável.)
2. **Cadastro + verificação** (+ auto-criação de perfil em `users`).
3. **Recuperação** (link built-in).
4. **Google** (depende do SHA-1/console).

Depois: demais funcionalidades e, por fim, foco total em passagem.