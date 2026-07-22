/**
 * Testes das regras do Firestore (ADR-0011) no emulador.
 *
 * Objetivo: TRAVAR A PARIDADE entre `firestore.rules` e a política Kotlin `PermissoesUsuario`
 * (ADR-0010). Cada `describe` abaixo corresponde a uma linha da matriz de autorização; se alguém
 * afrouxar uma regra sem querer, o teste correspondente quebra.
 *
 * Roda via `npm test` (usa `firebase emulators:exec` — sobe o emulador, roda o Jest, derruba).
 * Pré-requisito: Firebase CLI instalado (o mesmo usado no `firebase deploy`).
 */

const { readFileSync } = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require('@firebase/rules-unit-testing');
const {
  doc,
  setDoc,
  getDoc,
  updateDoc,
  deleteDoc,
  setLogLevel,
} = require('firebase/firestore');

// uids de teste — o id do doc users/{uid} tem que casar com o uid do contexto autenticado,
// porque a regra lê o cargo via get(users/$(request.auth.uid)).
const OPERADOR_A = 'op-a';
const OPERADOR_B = 'op-b';
const COLAB = 'colab-master';
const ADM = 'adm';

let testEnv;

beforeAll(async () => {
  setLogLevel('error'); // silencia o ruído de "permission denied" esperado
  testEnv = await initializeTestEnvironment({
    projectId: 'demo-fluviapp',
    firestore: {
      rules: readFileSync(path.resolve(__dirname, '..', 'firestore.rules'), 'utf8'),
      host: '127.0.0.1',
      port: 8080,
    },
  });
});

afterAll(async () => {
  await testEnv.cleanup();
});

// Estado limpo e conhecido antes de cada teste (independência entre casos).
beforeEach(async () => {
  await testEnv.clearFirestore();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    // Perfis (users/{uid}) com os quatro cargos.
    await setDoc(doc(db, 'users', OPERADOR_A), { nome: 'Op A', email: 'a@x.com', cargo: 'OPERADOR' });
    await setDoc(doc(db, 'users', OPERADOR_B), { nome: 'Op B', email: 'b@x.com', cargo: 'OPERADOR' });
    await setDoc(doc(db, 'users', COLAB), { nome: 'Colab', email: 'c@x.com', cargo: 'COLABORADOR_MASTER' });
    await setDoc(doc(db, 'users', ADM), { nome: 'Adm', email: 'adm@x.com', cargo: 'ADM' });
    // Catálogo de exemplo (para os testes de leitura).
    await setDoc(doc(db, 'navios', 'navio-1'), { nome: 'Navio 1' });
    // Passagem alheia (dono = OPERADOR_B) e o contador.
    await setDoc(doc(db, 'passagens', 'alheia'), { funcionarioId: OPERADOR_B, valor: 10 });
    await setDoc(doc(db, 'passagens', 'contador'), { numeroBilhete: 100 });
    // Passagens em estados conhecidos do ciclo de vida (ADR-0012), donas do OPERADOR_B (alheias ao A).
    await setDoc(doc(db, 'passagens', 'aemitir-b'), { funcionarioId: OPERADOR_B, status: 'A_EMITIR' });
    await setDoc(doc(db, 'passagens', 'emitida-b'), { funcionarioId: OPERADOR_B, status: 'EMITIDA' });
    await setDoc(doc(db, 'passagens', 'embarcada-b'), {
      funcionarioId: OPERADOR_B,
      status: 'EMBARCADA',
      embarcadaPorId: OPERADOR_A,
      embarcadaPor: 'Op A',
      embarcadaEm: '01/07/2026 08:00',
    });
    await setDoc(doc(db, 'passagens', 'aemitir-a'), { funcionarioId: OPERADOR_A, status: 'A_EMITIR' });
  });
});

// Atalhos para os bancos autenticados por cargo.
const asOperadorA = () => testEnv.authenticatedContext(OPERADOR_A).firestore();
const asOperadorB = () => testEnv.authenticatedContext(OPERADOR_B).firestore();
const asColab = () => testEnv.authenticatedContext(COLAB).firestore();
const asAdm = () => testEnv.authenticatedContext(ADM).firestore();
const asAnon = () => testEnv.unauthenticatedContext().firestore();

// --- users/{uid}: anti-escalonamento de cargo ---
describe('users/{uid} — anti-escalonamento', () => {
  test('cria o próprio perfil como OPERADOR → OK', async () => {
    const db = testEnv.authenticatedContext('novo').firestore();
    await assertSucceeds(setDoc(doc(db, 'users', 'novo'), { nome: 'Novo', email: 'n@x', cargo: 'OPERADOR' }));
  });

  test('cria o próprio perfil já como ADM → NEGADO', async () => {
    const db = testEnv.authenticatedContext('novo').firestore();
    await assertFails(setDoc(doc(db, 'users', 'novo'), { nome: 'Novo', email: 'n@x', cargo: 'ADM' }));
  });

  test('cria perfil de OUTRO uid → NEGADO', async () => {
    const db = testEnv.authenticatedContext('novo').firestore();
    await assertFails(setDoc(doc(db, 'users', OPERADOR_A), { nome: 'X', email: 'x@x', cargo: 'OPERADOR' }));
  });

  test('operador tenta se auto-promover a ADM (update do cargo) → NEGADO', async () => {
    await assertFails(updateDoc(doc(asOperadorA(), 'users', OPERADOR_A), { cargo: 'ADM' }));
  });

  test('operador edita o próprio nome (cargo inalterado) → OK', async () => {
    await assertSucceeds(updateDoc(doc(asOperadorA(), 'users', OPERADOR_A), { nome: 'Novo Nome' }));
  });

  test('delete do próprio perfil → NEGADO', async () => {
    await assertFails(deleteDoc(doc(asOperadorA(), 'users', OPERADOR_A)));
  });

  test('leitura de perfil por autenticado → OK', async () => {
    await assertSucceeds(getDoc(doc(asOperadorA(), 'users', OPERADOR_B)));
  });
});

// --- Catálogos (navio como representante): ler todos autenticados, escrever só gestor ---
describe('catálogo (navios) — leitura ampla, escrita só gestor', () => {
  test('operador LÊ navio (a venda precisa) → OK', async () => {
    await assertSucceeds(getDoc(doc(asOperadorA(), 'navios', 'navio-1')));
  });

  test('não autenticado LÊ navio → NEGADO', async () => {
    await assertFails(getDoc(doc(asAnon(), 'navios', 'navio-1')));
  });

  test('operador ESCREVE navio → NEGADO', async () => {
    await assertFails(setDoc(doc(asOperadorA(), 'navios', 'navio-2'), { nome: 'Navio 2' }));
  });

  test('COLABORADOR_MASTER escreve navio (não é gestor) → NEGADO', async () => {
    await assertFails(setDoc(doc(asColab(), 'navios', 'navio-2'), { nome: 'Navio 2' }));
  });

  test('gestor (ADM) escreve navio → OK', async () => {
    await assertSucceeds(setDoc(doc(asAdm(), 'navios', 'navio-2'), { nome: 'Navio 2' }));
  });
});

// --- Passagens: emissão, posse e imutabilidade do dono ---
describe('passagens — emissão sem forjar dono', () => {
  test('operador cria passagem com funcionarioId = próprio uid → OK', async () => {
    await assertSucceeds(setDoc(doc(asOperadorA(), 'passagens', 'p1'), { funcionarioId: OPERADOR_A, valor: 5 }));
  });

  test('operador cria passagem carimbando OUTRO dono (forjar) → NEGADO', async () => {
    await assertFails(setDoc(doc(asOperadorA(), 'passagens', 'p1'), { funcionarioId: OPERADOR_B, valor: 5 }));
  });
});

describe('passagens — editar/deletar por posse', () => {
  test('operador edita a PRÓPRIA passagem → OK', async () => {
    await assertSucceeds(setDoc(doc(asOperadorB(), 'passagens', 'alheia'), { funcionarioId: OPERADOR_B, valor: 20 }));
  });

  test('operador edita passagem ALHEIA → NEGADO', async () => {
    await assertFails(updateDoc(doc(asOperadorA(), 'passagens', 'alheia'), { valor: 99 }));
  });

  test('COLABORADOR_MASTER edita passagem alheia (editar-qualquer) → OK', async () => {
    await assertSucceeds(updateDoc(doc(asColab(), 'passagens', 'alheia'), { valor: 99 }));
  });

  test('gestor (ADM) edita passagem alheia → OK', async () => {
    await assertSucceeds(updateDoc(doc(asAdm(), 'passagens', 'alheia'), { valor: 99 }));
  });

  test('editar-qualquer NÃO pode reatribuir o dono (funcionarioId imutável) → NEGADO', async () => {
    await assertFails(updateDoc(doc(asColab(), 'passagens', 'alheia'), { funcionarioId: COLAB }));
  });

  test('operador deleta passagem alheia → NEGADO', async () => {
    await assertFails(deleteDoc(doc(asOperadorA(), 'passagens', 'alheia')));
  });

  test('dono deleta a própria passagem → OK', async () => {
    await assertSucceeds(deleteDoc(doc(asOperadorB(), 'passagens', 'alheia')));
  });
});

// --- Contador (passagens/contador): monotônico, sem delete (endurecimento ADR-0011) ---
describe('passagens/contador — incremento monotônico e indestrutível', () => {
  test('cargo conhecido incrementa (100 → 101) → OK', async () => {
    await assertSucceeds(updateDoc(doc(asOperadorA(), 'passagens', 'contador'), { numeroBilhete: 101 }));
  });

  test('retroceder o contador (100 → 99) → NEGADO', async () => {
    await assertFails(updateDoc(doc(asOperadorA(), 'passagens', 'contador'), { numeroBilhete: 99 }));
  });

  test('deletar o contador → NEGADO', async () => {
    await assertFails(deleteDoc(doc(asAdm(), 'passagens', 'contador')));
  });
});

// --- Ciclo de vida da passagem (ADR-0012 Fase 4): FSM imposta no servidor ---
// Carimbo de embarque que o app grava: status + o próprio uid como embarcadaPorId + nome + quando.
const carimboEmbarque = (uid, nome) => ({
  status: 'EMBARCADA',
  embarcadaPorId: uid,
  embarcadaPor: nome,
  embarcadaEm: '02/07/2026 09:30',
});

describe('passagens — confirmação de embarque (eixo novo, qualquer cargo)', () => {
  test('operador NÃO-dono confirma embarque (EMITIDA→EMBARCADA) carimbando o próprio uid → OK', async () => {
    // OPERADOR_A embarca a passagem 'emitida-b' (dono = OPERADOR_B): quem está na doca valida.
    await assertSucceeds(updateDoc(doc(asOperadorA(), 'passagens', 'emitida-b'), carimboEmbarque(OPERADOR_A, 'Op A')));
  });

  test('gestor (ADM) confirma embarque de passagem alheia → OK', async () => {
    await assertSucceeds(updateDoc(doc(asAdm(), 'passagens', 'emitida-b'), carimboEmbarque(ADM, 'Adm')));
  });

  test('embarque carimbando OUTRO uid como quem embarcou (forjar autoria) → NEGADO', async () => {
    await assertFails(updateDoc(doc(asOperadorA(), 'passagens', 'emitida-b'), carimboEmbarque(OPERADOR_B, 'Op B')));
  });

  test('marcar EMBARCADA sem carimbo (só status) → NEGADO (embarque tem que ser auditado)', async () => {
    await assertFails(updateDoc(doc(asOperadorA(), 'passagens', 'emitida-b'), { status: 'EMBARCADA' }));
  });

  test('embarque contrabandeando edição de conteúdo (altera valor junto) por não-dono → NEGADO', async () => {
    await assertFails(updateDoc(doc(asOperadorA(), 'passagens', 'emitida-b'), {
      ...carimboEmbarque(OPERADOR_A, 'Op A'),
      valor: 999,
    }));
  });
});

describe('passagens — arestas legais da FSM (avança, nunca retrocede nem pula)', () => {
  test('dono emite a própria passagem (A_EMITIR→EMITIDA) → OK', async () => {
    await assertSucceeds(updateDoc(doc(asOperadorA(), 'passagens', 'aemitir-a'), { status: 'EMITIDA' }));
  });

  test('operador NÃO-dono emite passagem alheia (A_EMITIR→EMITIDA) → NEGADO (não é o eixo de embarque)', async () => {
    await assertFails(updateDoc(doc(asOperadorA(), 'passagens', 'aemitir-b'), { status: 'EMITIDA' }));
  });

  test('pulo A_EMITIR→EMBARCADA (sem passar por EMITIDA) → NEGADO', async () => {
    await assertFails(updateDoc(doc(asAdm(), 'passagens', 'aemitir-b'), carimboEmbarque(ADM, 'Adm')));
  });

  test('retrocesso EMBARCADA→EMITIDA (mesmo gestor) → NEGADO (embarque é irreversível)', async () => {
    await assertFails(updateDoc(doc(asAdm(), 'passagens', 'embarcada-b'), { status: 'EMITIDA' }));
  });

  test('retrocesso EMITIDA→A_EMITIR → NEGADO', async () => {
    await assertFails(updateDoc(doc(asAdm(), 'passagens', 'emitida-b'), { status: 'A_EMITIR' }));
  });
});