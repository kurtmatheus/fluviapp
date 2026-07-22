# Testes das regras do Firestore (ADR-0011 / ADR-0012)

Suíte que **trava a paridade** entre `../firestore.rules` e a política Kotlin `PermissoesUsuario`
(ADR-0010). Cada teste corresponde a uma linha da matriz de autorização — se alguém afrouxar uma
regra sem querer, o teste quebra.

Pasta isolada de propósito: não faz parte do build Gradle (regras não são Kotlin). É um projeto
Node/npm separado.

## Pré-requisitos

- **Node.js** (18+) e **npm**.
- **Firebase CLI** — o mesmo usado no `firebase deploy`. Instala o emulador sob demanda.
- **Java** (JDK 11+) — o emulador do Firestore roda sobre a JVM.

## Rodar

```bash
cd firestore-tests
npm install
npm test
```

`npm test` chama `firebase emulators:exec` (sobe o emulador do Firestore lendo `../firebase.json`,
roda o Jest contra ele e derruba tudo ao final). Não precisa de credenciais: usa o projeto
`demo-fluviapp` (o prefixo `demo-` faz o emulador não exigir login).

## O que é coberto (matriz ADR-0010/0011)

- **Anti-escalonamento** (`users/{uid}`): cria só o próprio perfil e só como `OPERADOR`; não altera
  o próprio cargo; não deleta perfil.
- **Catálogos** (`navios` como representante): todo autenticado lê; só gestor (ADM/DIRETOR) escreve;
  Colaborador Master **não** escreve catálogo.
- **Passagens**: não dá para forjar dono na emissão (`funcionarioId == uid`); operador só edita/deleta
  as próprias; Colaborador Master e gestor editam qualquer; o `funcionarioId` é imutável no update.
- **Contador** (`passagens/contador`): incremento monotônico (não retrocede) e sem delete.
- **Ciclo de vida da passagem** (ADR-0012 Fase 4): a FSM imposta no `update` — só arestas legais
  (`A_EMITIR→EMITIDA`/`EMITIDA→EMBARCADA`), sem retrocesso nem pulo; confirmação de embarque por
  qualquer cargo conhecido, mas carimbando o próprio uid (não forja autoria), tocando só os 4 campos
  do embarque (sem contrabandear edição) e nunca sem carimbo.

## Manutenção

Mudou um cargo na matriz? Altere **os dois** de propósito — `PermissoesUsuario.kt` e
`firestore.rules` — e ajuste o teste correspondente aqui. É a duplicação inevitável descrita no
ADR-0011 (a regra roda no servidor do Google, em outra linguagem); esta suíte é a rede que a segura.