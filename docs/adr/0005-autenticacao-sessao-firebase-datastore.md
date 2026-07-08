# ADR-0005: Autenticação por sessão Firebase + DataStore, sem senha local

**Status:** Aceita (direção); execução após documentação do fluxo (ver `docs/design/fluxo-login.md`)

**Contexto**

O login hoje usa **dois armazenamentos** para o usuário:
- **Firebase Auth** — a credencial (`signInWithEmailAndPassword`).
- **Room `Usuario`** — perfil (nome/cargo) **e a senha** (`senha.encrypt()`), gravada em
  `salvarUsuarioAutenticado`; mais o DataStore (`LOGADO`/`USUARIO_ATUAL`/`CARGO_ATUAL`).

A intenção declarada da senha local era **offline-first**: `LoginViewModel.validarLogin` tenta
primeiro `usuarioRepository.obterPorEmailSenha(email, senha)` (Room) e só cai no Firebase se não
achar. Três problemas concretos:

1. **Redundante.** O "continuar logado" NÃO depende da senha local: o `SplashScreenViewModel`
   roteia lendo o DataStore `LOGADO`. E o Firebase Auth **persiste a sessão** (`currentUser` +
   tokens) no device entre reinícios — offline inclusive.
2. **Cheiro de segurança.** Guardar senha no device (mesmo "encriptada" com chave local) é
   exatamente o que o Firebase Auth existe para evitar.
3. **Quebrado.** Grava `senha.encrypt()` mas consulta com a senha crua (`_uiState.value.senha`)
   → o fast-path nunca casa; todo login vai ao Firebase de qualquer forma.

Fato que fecha a análise: o Firebase Auth **não** autentica um **login novo offline** (o
`signInWithEmailAndPassword` precisa de rede). Logo, a senha local não entrega offline-first
real — só reimplementa mal o que a sessão persistida já dá, adicionando risco.

**Decisão**

- **Descartar o storage de senha local.** Remover `senha` do modelo persistido `Usuario` e o
  fast-path `obterPorEmailSenha`.
- **Fonte de verdade da sessão = `FirebaseAuth.currentUser`** (persistido, offline-capaz). O
  **DataStore** guarda o estado derivado para roteamento instantâneo/offline no Splash
  (`logado`, `usuario_atual`, `cargo_atual`) — reconciliado com `currentUser` na inicialização.
- **Room `Usuario` = cache de perfil** (nome/cargo), chaveado por e-mail, **sem senha** —
  espelho do Firestore `users` (coerente com o modelo de memória do [[ADR-0003]]).
- **Primeiro login exige rede** (limitação do Firebase Auth): tratar explicitamente (mensagem
  clara quando offline), não simular com verificação local.
- **Logout** = `FirebaseAuth.signOut()` + limpar DataStore.

**Consequências**

- Some a superfície de risco (nenhuma senha no device) e o bug do fast-path.
- "Continuar logado offline" segue funcionando (sessão do Firebase + DataStore), agora sem
  ilusão de "re-login offline".
- Migração: remover `senha` da entidade `Usuario` (migração Room — coluna some) e o método
  `obterPorEmailSenha`/`salvarUsuarioAutenticado(senha)`; ajustar `logarUsuario` para derivar o
  perfil de `currentUser.email`. `CriptografiaUtil` para senha deixa de ser usado nesse caminho.
- O Splash passa a preferir `currentUser`; DataStore vira dica instantânea (evita flicker).

**Alternativas consideradas**

- **Manter a senha local** (status quo): rejeitado — risco + redundância + já quebrado.
- **DataStore criptografado guardando credenciais**: rejeitado — mesmo problema conceitual
  (guardar credencial), sem ganho sobre a sessão nativa do Firebase.

**Alternativas futuras**

- Multi-conta / troca rápida de usuário: o DataStore de sessão evolui para uma lista de perfis
  em cache, com `currentUser` decidindo o ativo.
- Ver `docs/design/fluxo-login.md` para as capacidades novas (cadastro + verificação de e-mail,
  Google, recuperação de senha) que se apoiam nesta base.