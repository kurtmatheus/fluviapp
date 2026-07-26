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
const AGENTE_A = 'agente-a';
const AGENTE_B = 'agente-b';
const SUPERVISOR = 'supervisor';
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
    await setDoc(doc(db, 'users', AGENTE_A), { nome: 'Agente A', email: 'a@x.com', cargo: 'AGENTE' });
    await setDoc(doc(db, 'users', AGENTE_B), { nome: 'Agente B', email: 'b@x.com', cargo: 'AGENTE' });
    await setDoc(doc(db, 'users', SUPERVISOR), { nome: 'Supervisor', email: 'c@x.com', cargo: 'SUPERVISOR' });
    await setDoc(doc(db, 'users', ADM), { nome: 'Adm', email: 'adm@x.com', cargo: 'ADM' });
    // Catálogo de exemplo (para os testes de leitura).
    await setDoc(doc(db, 'navios', 'navio-1'), { nome: 'Navio 1' });
    // Passagem alheia (dono = AGENTE_B) e o contador.
    await setDoc(doc(db, 'passagens', 'alheia'), { funcionarioId: AGENTE_B, valor: 10 });
    await setDoc(doc(db, 'passagens', 'contador'), { numeroBilhete: 100 });
    // Passagens em estados conhecidos do ciclo de vida (ADR-0012), donas do AGENTE_B (alheias ao A).
    await setDoc(doc(db, 'passagens', 'aemitir-b'), { funcionarioId: AGENTE_B, status: 'A_EMITIR' });
    await setDoc(doc(db, 'passagens', 'emitida-b'), { funcionarioId: AGENTE_B, status: 'EMITIDA' });
    await setDoc(doc(db, 'passagens', 'embarcada-b'), {
      funcionarioId: AGENTE_B,
      status: 'EMBARCADA',
      embarcadaPorId: AGENTE_A,
      embarcadaPor: 'Agente A',
      embarcadaEm: '01/07/2026 08:00',
    });
    await setDoc(doc(db, 'passagens', 'aemitir-a'), { funcionarioId: AGENTE_A, status: 'A_EMITIR' });
  });
});

// Atalhos para os bancos autenticados por cargo.
const asAgenteA = () => testEnv.authenticatedContext(AGENTE_A).firestore();
const asAgenteB = () => testEnv.authenticatedContext(AGENTE_B).firestore();
const asSupervisor = () => testEnv.authenticatedContext(SUPERVISOR).firestore();
const asAdm = () => testEnv.authenticatedContext(ADM).firestore();
const asAnon = () => testEnv.unauthenticatedContext().firestore();

// --- users/{uid}: anti-escalonamento de cargo ---
describe('users/{uid} — anti-escalonamento', () => {
  test('cria o próprio perfil como AGENTE → OK', async () => {
    const db = testEnv.authenticatedContext('novo').firestore();
    await assertSucceeds(setDoc(doc(db, 'users', 'novo'), { nome: 'Novo', email: 'n@x', cargo: 'AGENTE' }));
  });

  test('cria o próprio perfil já como ADM → NEGADO', async () => {
    const db = testEnv.authenticatedContext('novo').firestore();
    await assertFails(setDoc(doc(db, 'users', 'novo'), { nome: 'Novo', email: 'n@x', cargo: 'ADM' }));
  });

  test('cria perfil de OUTRO uid → NEGADO', async () => {
    const db = testEnv.authenticatedContext('novo').firestore();
    await assertFails(setDoc(doc(db, 'users', AGENTE_A), { nome: 'X', email: 'x@x', cargo: 'AGENTE' }));
  });

  test('agente tenta se auto-promover a ADM (update do cargo) → NEGADO', async () => {
    await assertFails(updateDoc(doc(asAgenteA(), 'users', AGENTE_A), { cargo: 'ADM' }));
  });

  test('agente edita o próprio nome (cargo inalterado) → OK', async () => {
    await assertSucceeds(updateDoc(doc(asAgenteA(), 'users', AGENTE_A), { nome: 'Novo Nome' }));
  });

  test('delete do próprio perfil → NEGADO', async () => {
    await assertFails(deleteDoc(doc(asAgenteA(), 'users', AGENTE_A)));
  });

  test('leitura de perfil por autenticado → OK', async () => {
    await assertSucceeds(getDoc(doc(asAgenteA(), 'users', AGENTE_B)));
  });
});

// --- Catálogos (navio como representante): ler todos autenticados, escrever só cargo de plataforma ---
describe('catálogo (navios) — leitura ampla, escrita só cargo de plataforma', () => {
  test('agente LÊ navio (a venda precisa) → OK', async () => {
    await assertSucceeds(getDoc(doc(asAgenteA(), 'navios', 'navio-1')));
  });

  test('não autenticado LÊ navio → NEGADO', async () => {
    await assertFails(getDoc(doc(asAnon(), 'navios', 'navio-1')));
  });

  test('agente ESCREVE navio → NEGADO', async () => {
    await assertFails(setDoc(doc(asAgenteA(), 'navios', 'navio-2'), { nome: 'Navio 2' }));
  });

  test('SUPERVISOR escreve navio (não é cargo de plataforma) → NEGADO', async () => {
    await assertFails(setDoc(doc(asSupervisor(), 'navios', 'navio-2'), { nome: 'Navio 2' }));
  });

  test('plataforma (ADM) escreve navio → OK', async () => {
    await assertSucceeds(setDoc(doc(asAdm(), 'navios', 'navio-2'), { nome: 'Navio 2' }));
  });
});

// --- Passagens: emissão, posse e imutabilidade do dono ---
describe('passagens — emissão sem forjar dono', () => {
  test('agente cria passagem com funcionarioId = próprio uid → OK', async () => {
    await assertSucceeds(setDoc(doc(asAgenteA(), 'passagens', 'p1'), { funcionarioId: AGENTE_A, valor: 5 }));
  });

  test('agente cria passagem carimbando OUTRO dono (forjar) → NEGADO', async () => {
    await assertFails(setDoc(doc(asAgenteA(), 'passagens', 'p1'), { funcionarioId: AGENTE_B, valor: 5 }));
  });
});

describe('passagens — editar/deletar por posse', () => {
  test('agente edita a PRÓPRIA passagem → OK', async () => {
    await assertSucceeds(setDoc(doc(asAgenteB(), 'passagens', 'alheia'), { funcionarioId: AGENTE_B, valor: 20 }));
  });

  test('agente edita passagem ALHEIA → NEGADO', async () => {
    await assertFails(updateDoc(doc(asAgenteA(), 'passagens', 'alheia'), { valor: 99 }));
  });

  test('SUPERVISOR edita passagem alheia (editar-qualquer) → OK', async () => {
    await assertSucceeds(updateDoc(doc(asSupervisor(), 'passagens', 'alheia'), { valor: 99 }));
  });

  test('plataforma (ADM) edita passagem alheia → OK', async () => {
    await assertSucceeds(updateDoc(doc(asAdm(), 'passagens', 'alheia'), { valor: 99 }));
  });

  test('editar-qualquer NÃO pode reatribuir o dono (funcionarioId imutável) → NEGADO', async () => {
    await assertFails(updateDoc(doc(asSupervisor(), 'passagens', 'alheia'), { funcionarioId: SUPERVISOR }));
  });

  test('agente deleta passagem alheia → NEGADO', async () => {
    await assertFails(deleteDoc(doc(asAgenteA(), 'passagens', 'alheia')));
  });

  test('dono deleta a própria passagem → OK', async () => {
    await assertSucceeds(deleteDoc(doc(asAgenteB(), 'passagens', 'alheia')));
  });
});

// --- Contador (passagens/contador): monotônico, sem delete (endurecimento ADR-0011) ---
describe('passagens/contador — incremento monotônico e indestrutível', () => {
  test('cargo conhecido incrementa (100 → 101) → OK', async () => {
    await assertSucceeds(updateDoc(doc(asAgenteA(), 'passagens', 'contador'), { numeroBilhete: 101 }));
  });

  test('retroceder o contador (100 → 99) → NEGADO', async () => {
    await assertFails(updateDoc(doc(asAgenteA(), 'passagens', 'contador'), { numeroBilhete: 99 }));
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
  test('agente NÃO-dono confirma embarque (EMITIDA→EMBARCADA) carimbando o próprio uid → OK', async () => {
    // AGENTE_A embarca a passagem 'emitida-b' (dono = AGENTE_B): quem está na doca valida.
    await assertSucceeds(updateDoc(doc(asAgenteA(), 'passagens', 'emitida-b'), carimboEmbarque(AGENTE_A, 'Agente A')));
  });

  test('plataforma (ADM) confirma embarque de passagem alheia → OK', async () => {
    await assertSucceeds(updateDoc(doc(asAdm(), 'passagens', 'emitida-b'), carimboEmbarque(ADM, 'Adm')));
  });

  test('embarque carimbando OUTRO uid como quem embarcou (forjar autoria) → NEGADO', async () => {
    await assertFails(updateDoc(doc(asAgenteA(), 'passagens', 'emitida-b'), carimboEmbarque(AGENTE_B, 'Agente B')));
  });

  test('marcar EMBARCADA sem carimbo (só status) → NEGADO (embarque tem que ser auditado)', async () => {
    await assertFails(updateDoc(doc(asAgenteA(), 'passagens', 'emitida-b'), { status: 'EMBARCADA' }));
  });

  test('embarque contrabandeando edição de conteúdo (altera valor junto) por não-dono → NEGADO', async () => {
    await assertFails(updateDoc(doc(asAgenteA(), 'passagens', 'emitida-b'), {
      ...carimboEmbarque(AGENTE_A, 'Agente A'),
      valor: 999,
    }));
  });
});

describe('passagens — arestas legais da FSM (avança, nunca retrocede nem pula)', () => {
  test('dono emite a própria passagem (A_EMITIR→EMITIDA) → OK', async () => {
    await assertSucceeds(updateDoc(doc(asAgenteA(), 'passagens', 'aemitir-a'), { status: 'EMITIDA' }));
  });

  test('agente NÃO-dono emite passagem alheia (A_EMITIR→EMITIDA) → NEGADO (não é o eixo de embarque)', async () => {
    await assertFails(updateDoc(doc(asAgenteA(), 'passagens', 'aemitir-b'), { status: 'EMITIDA' }));
  });

  test('pulo A_EMITIR→EMBARCADA (sem passar por EMITIDA) → NEGADO', async () => {
    await assertFails(updateDoc(doc(asAdm(), 'passagens', 'aemitir-b'), carimboEmbarque(ADM, 'Adm')));
  });

  test('retrocesso EMBARCADA→EMITIDA (mesmo cargo de plataforma) → NEGADO (embarque é irreversível)', async () => {
    await assertFails(updateDoc(doc(asAdm(), 'passagens', 'embarcada-b'), { status: 'EMITIDA' }));
  });

  test('retrocesso EMITIDA→A_EMITIR → NEGADO', async () => {
    await assertFails(updateDoc(doc(asAdm(), 'passagens', 'emitida-b'), { status: 'A_EMITIR' }));
  });
});