# Usuário × Funcionário — quem a plataforma gere, quem a empresa gere

> **Status:** estudo, aberto. Nasce de um incômodo concreto do analista (2026-08-08): *"como ADM estou
> vendo **Equipe** — para mim isso devia ser **Usuários**"*. O incômodo é de vocabulário, mas a causa é
> de domínio, e ela precisa ser fechada **antes da F7** (Rotas).

## 1. O que existe hoje

Os dois contextos já estão separados desde o ADR-0015 §8.1, e cada um tem documento próprio:

| | `users/{uid}` — **Usuario** | `funcionarios/{id}` — **Funcionario** |
|---|---|---|
| Responde | *quem acessa o app, e o que compete nele* | *quem é a pessoa na operação* |
| Carrega | `email`, `username`, `papel`, `funcionarioId` | `nome`, `email`, `vinculos[{empresaId, cargo}]`, `empresaIds` |
| Eixo | **sistema**: `ADM` / `GESTOR` / `OPERADOR` (fechado) | **negócio**: `SUPERVISOR` / `AGENTE` por vínculo (aberto) |
| Quem escreve hoje | **só o próprio dono**, no primeiro acesso, e sempre como `OPERADOR` | plataforma em qualquer empresa; supervisor só na dele, e só como `AGENTE` |
| Quem cria `ADM`/`GESTOR` | **ninguém pelo app** — console (ADR-0021 D0) | — |

A regra do servidor é explícita sobre a parte que interessa: `users/{uid}` só nasce pelo próprio dono,
com `papel == 'OPERADOR'`, e o elo `funcionarioId` só é aceito se apontar para um funcionário **com o
mesmo e-mail** do autenticado. Papel e vínculo são **imutáveis** pelo cliente depois disso.

## 2. O problema: uma seção só para dois contextos

`EQUIPE` aparece para **plataforma e supervisor**, porque a política pergunta
`podeCadastrarFuncionario(papel, cargo) = ehPapelPlataforma(papel) || cargo == SUPERVISOR`.

O resultado é o que o analista viu: **o ADM abre "Equipe"** — o quadro de pessoal de uma empresa — como
se a plataforma tivesse equipe. Ela não tem. A plataforma tem **usuários**; a empresa tem
**funcionários**. São perguntas diferentes, com donos diferentes:

- **quem pode entrar no app, e com que papel** → é da **plataforma**, e vale para o sistema inteiro;
- **quem trabalha nesta empresa, e com que cargo** → é da **empresa**, e vale só dentro dela.

Isto não contradiz o ADR-0022 D2 — ele já dizia que `Equipe` é **exclusiva da empresa**. O que falta é a
consequência: se Equipe é da empresa, **a plataforma precisa da seção dela**, e não de um atalho para a
seção alheia.

## 3. A proposta

| Painel | Seção | O que faz |
|---|---|---|
| **Plataforma** | **Usuários** | quem acessa o app e com que papel — Novo Usuário / Pesquisar Usuário |
| **Empresa** | **Equipe** | quem trabalha nela, com que cargo — e **só dela** |

`EQUIPE` sai do painel da plataforma; `USUARIOS` entra. Isso **revive o ADR-0021**, que havia decidido
(D0) não implementar a seção *"porque não se paga agora"*. A razão de reabrir é outra e é melhor: não é
conveniência, é **corrigir um vocabulário que está mentindo na tela** — e o preço já mudou, porque a
Equipe existe e o eixo de vínculos está pronto.

## 4. O que precisa ser decidido para fechar

### 4.1 "Novo Usuário" não pode criar a conta — então o que ele cria?

Restrição técnica, não preferência: criar conta pelo SDK cliente do Firebase Auth **troca a sessão** para
a conta recém-criada. É a razão de o bootstrap do primeiro ADM ser manual, e ela não muda por decisão de
produto.

O caminho que **já funciona no app** é outro, e é o do §2.1: **pré-cadastro + primeiro acesso**. A gestão
grava o registro; a pessoa entra e cria a própria senha.

Proposta: `Novo Usuário` grava um **convite** — `convites/{email}` com `papel` (e, para operador, o
`funcionarioId`) —, escrito **só por `ADM`**. No primeiro acesso, `users/{uid}` nasce com o papel do
convite em vez de nascer sempre `OPERADOR`.

Isso preserva o anti-escalonamento inteiro: **o cliente continua não escolhendo o próprio papel** — ele
vem de um documento que só o ADM escreve. E tira o console do caminho para tudo, menos para o primeiro
ADM, que continua sendo bootstrap por princípio (ADR-0021 D0).

### 4.2 Quem cria o primeiro funcionário de uma empresa?

Se a Equipe é exclusiva da empresa e só o supervisor a gere, **ninguém cria o primeiro supervisor** —
galinha e ovo. Duas saídas:

- **(a)** a plataforma continua podendo cadastrar funcionário em qualquer empresa (como hoje), mas isso
  reabre a porta que a §3 fecha: o ADM voltaria a ver a seção da empresa;
- **(b)** o **convite** resolve: ao convidar um operador, o ADM informa a empresa e o cargo inicial, e o
  primeiro acesso cria o `Funcionario` **com o vínculo** junto do `users/{uid}`. A partir daí a empresa
  se gere sozinha.

Recomendo **(b)**: mantém cada painel com o seu, e faz do convite o único lugar onde os dois contextos
se encontram — que é exatamente o que eles são, um elo.

### 4.3 `Usuários` é `ADM`-only ou de toda a plataforma?

ADR-0021 D1 dizia `ADM`-only, porque papel concede tudo e erro ali é sistêmico. Recomendo **manter**:
`GESTOR` administra o negócio da plataforma, não o acesso a ela.

### 4.4 Um usuário de plataforma tem funcionário?

Não (§8.1): `ADM`/`GESTOR` existem sem registro na operação — e é por isso que não emitem passagem.
Consequência para o convite: **convite de plataforma não leva funcionário; convite de operador exige um**
(com o mesmo e-mail, que é o que a regra já valida).

### 4.5 Onde isto entra no plano

Proposta: **F6.6 — Usuários**, ainda dentro do bloco da Equipe, porque é a mesma dupla de conceitos vista
do outro lado. A F7 (Rotas) segue logo depois, sem renumerar nada.

## 5. O que muda em código (se a proposta for aceita)

| Onde | Mudança |
|---|---|
| `SecaoMenu` | nasce `USUARIOS`; `EQUIPE` sai de `SECOES_DO_PAINEL` |
| `PermissoesUsuario` | `podeAcessar(USUARIOS)` = `ADM`; `podeAcessar(EQUIPE)` = cargo de gestão da empresa |
| domínio | nasce `Convite(email, papel, empresaId?, cargo?)` |
| `firestore.rules` | `convites/{email}` — escrita só `ADM`; `users/{uid}` passa a aceitar o papel **do convite** |
| primeiro acesso | lê o convite: cria `users/{uid}` com o papel dele e, se houver, o `Funcionario` com vínculo |
| `UsuarioRepository` | ganha leitura da coleção para a tela de pesquisa (hoje é local puro) |