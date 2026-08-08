/**
 * Testes das regras do Firestore (ADR-0011) no emulador.
 *
 * Objetivo: TRAVAR A PARIDADE entre `firestore.rules` e a política Kotlin `PermissoesUsuario`
 * (ADR-0010 + ADR-0015 §8). Cada `describe` abaixo corresponde a uma linha da matriz de autorização;
 * se alguém afrouxar uma regra sem querer, o teste correspondente quebra.
 *
 * Depois da revisão estrutural são DOIS eixos e DOIS documentos por pessoa: `users/{uid}` guarda o
 * **papel** de sistema e o elo `funcionarioId`; `funcionarios/{id}` guarda o **cargo** de negócio. A
 * posse da passagem é do FUNCIONÁRIO (§8.4) — por isso os `funcionarioId` das passagens abaixo são
 * ids de funcionário, não uids.
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
// porque a regra lê o papel via get(users/$(request.auth.uid)).
const AGENTE_A = 'agente-a';
const AGENTE_B = 'agente-b';
const SUPERVISOR = 'supervisor';
const ADM = 'adm';
// Papel puro de plataforma: existe no sistema, NÃO existe na operação (sem funcionário) — ADR-0015 §8.1.
const GESTOR = 'gestor';

// ids de FUNCIONÁRIO (o outro contexto). É isto que a passagem congela como dono.
const F_A = 'func-a';
const F_B = 'func-b';
const F_SUPERVISOR = 'func-supervisor';
const F_ADM = 'func-adm';
// Funcionário de OUTRA agência: é ele que prova o isolamento do supervisor (ADR-0015 §2.1).
const F_OUTRA_AGENCIA = 'func-outra';
// Pré-cadastrado que ainda não fez o primeiro acesso: tem funcionário, não tem users/{uid}.
const EMPRESA = "empresa-modelo";
const F_NOVO = 'func-novo';
const EMAIL_F_NOVO = 'novo@x.com';

// Empresas e vínculos (ADR-0016 §6): desde a F6.3 é o VÍNCULO que dá autoridade, e não a String de
// agência. Os literais abaixo têm de casar **campo a campo** com o que a regra compara em `hasOnly` —
// é essa igualdade estrutural que substitui a iteração que a linguagem de regras não tem.
const E_MATRIZ = 'empresa-matriz';
const E_MARE = 'empresa-mare';
const AGENTE_NA_MATRIZ = { empresaId: E_MATRIZ, cargo: 'AGENTE' };
const SUPERVISOR_NA_MATRIZ = { empresaId: E_MATRIZ, cargo: 'SUPERVISOR' };
const AGENTE_NA_MARE = { empresaId: E_MARE, cargo: 'AGENTE' };

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
    // Contexto SISTEMA: users/{uid} com papel + elo.
    await setDoc(doc(db, 'users', AGENTE_A), { username: 'a', email: 'a@x.com', papel: 'OPERADOR', funcionarioId: F_A });
    await setDoc(doc(db, 'users', AGENTE_B), { username: 'b', email: 'b@x.com', papel: 'OPERADOR', funcionarioId: F_B });
    await setDoc(doc(db, 'users', SUPERVISOR), { username: 'c', email: 'c@x.com', papel: 'OPERADOR', funcionarioId: F_SUPERVISOR });
    await setDoc(doc(db, 'users', ADM), { username: 'adm', email: 'adm@x.com', papel: 'ADM', funcionarioId: F_ADM });
    await setDoc(doc(db, 'users', GESTOR), { username: 'g', email: 'g@x.com', papel: 'GESTOR' });
    // Contexto NEGÓCIO: funcionarios/{id}. A autoridade é o VÍNCULO desde a F6.3 (ADR-0016 §6);
    // `agencia` e `cargo` seguem gravados como legado derivado, e nenhuma regra os consulta mais.
    await setDoc(doc(db, 'funcionarios', F_A), { nome: 'Agente A', agencia: 'MATRIZ', cargo: 'AGENTE', vinculos: [AGENTE_NA_MATRIZ], empresaIds: [E_MATRIZ] });
    await setDoc(doc(db, 'funcionarios', F_B), { nome: 'Agente B', agencia: 'MATRIZ', cargo: 'AGENTE', email: 'b@x.com', vinculos: [AGENTE_NA_MATRIZ], empresaIds: [E_MATRIZ] });
    await setDoc(doc(db, 'funcionarios', F_SUPERVISOR), { nome: 'Supervisor', agencia: 'MATRIZ', cargo: 'SUPERVISOR', vinculos: [SUPERVISOR_NA_MATRIZ], empresaIds: [E_MATRIZ] });
    await setDoc(doc(db, 'funcionarios', F_ADM), { nome: 'Adm', agencia: 'MATRIZ', cargo: 'AGENTE', vinculos: [AGENTE_NA_MATRIZ], empresaIds: [E_MATRIZ] });
    await setDoc(doc(db, 'funcionarios', F_OUTRA_AGENCIA), { nome: 'De Outra', agencia: 'MARE', cargo: 'AGENTE', vinculos: [AGENTE_NA_MARE], empresaIds: [E_MARE] });
    await setDoc(doc(db, 'funcionarios', F_NOVO), { nome: 'Novo', agencia: 'MATRIZ', cargo: 'AGENTE', email: EMAIL_F_NOVO, vinculos: [AGENTE_NA_MATRIZ], empresaIds: [E_MATRIZ] });
    // Catálogo de exemplo (para os testes de leitura).
    await setDoc(doc(db, 'embarcacoes', 'embarcacao-1'), { nome: 'Embarcação 1' });
    await setDoc(doc(db, 'localidades', 'loc-1'), {
      municipio: 'Parintins',
      uf: 'AM',
      codigoIbge: '1303205',
      ativo: true,
    });
    await setDoc(doc(db, 'portos', 'porto-1'), {
      nome: 'Porto de Val-de-Cães',
      localidadeId: 'loc-1',
      ativo: true,
    });
    // Rota (F7): o pool compartilhado, sem dono.
    await setDoc(doc(db, 'rotas', 'rota-1'), {
      portoOrigemId: 'porto-1', portoDestinoId: 'porto-2',
      distanciaMn: 420, tempoMedioH: 30, criadoPor: F_SUPERVISOR, ativo: true,
    });
    // Convites (F6.6): o id É o e-mail — é assim que o convidado acha o próprio antes de ter perfil.
    await setDoc(doc(db, 'convites', 'convidado@x.com'), {
      nome: 'Convidado', papel: 'OPERADOR', empresaId: E_MATRIZ, cargo: 'AGENTE', usado: false,
    });
    await setDoc(doc(db, 'convites', 'convidado-adm@x.com'), {
      nome: 'Convidado Adm', papel: 'ADM', empresaId: '', cargo: '', usado: false,
    });
    // Passagem alheia (dono = funcionário B) e o contador.
    await setDoc(doc(db, 'passagens', 'alheia'), { funcionarioId: F_B, valor: 10 });
    await setDoc(doc(db, 'passagens', 'contador'), { numeroBilhete: 100 });
    // Passagens em estados conhecidos do ciclo de vida (ADR-0012), donas do funcionário B (alheias ao A).
    await setDoc(doc(db, 'passagens', 'aemitir-b'), { funcionarioId: F_B, status: 'A_EMITIR' });
    await setDoc(doc(db, 'passagens', 'emitida-b'), { funcionarioId: F_B, status: 'EMITIDA' });
    await setDoc(doc(db, 'passagens', 'embarcada-b'), {
      funcionarioId: F_B,
      status: 'EMBARCADA',
      embarcadaPorId: AGENTE_A,
      embarcadaPor: 'Agente A',
      embarcadaEm: '01/07/2026 08:00',
    });
    await setDoc(doc(db, 'passagens', 'aemitir-a'), { funcionarioId: F_A, status: 'A_EMITIR' });
    // A PARTE e o que ela FAZ (ADR-0016 §4): a subcoleção de atuações, com a concessão pendurada.
    await setDoc(doc(db, "empresas", EMPRESA), { nome: "EMPRESA MODELO", cnpj: "11222333000181" });
    await setDoc(doc(db, "empresas", EMPRESA, "atuacoes", "AGENCIAMENTO"), { embarcacaoIds: ["embarcacao-1"] });
  });
});

/**
 * **Recorte da revitalização** (ADR-0020), gêmeo do `@Category(ForaDoEscopo)` da suíte JVM: roda o que
 * cobre a entidade viva — hoje a Empresa — e pula o resto.
 *
 * O critério é o mesmo: um bloco está dentro quando a regra que ele exercita é atravessada por alguma
 * parte da jornada da Empresa. Por isso `users/{uid}` fica: o login lê `users/{uid}` para resolver papel
 * e cargo, e foi justamente uma leitura dessa coleção que quebrou o acesso nesta sessão. Já `funcionarios`
 * sai — a jornada só **lê** de lá, e o que o bloco cobre é a escrita e o cargo, que são da Equipe.
 *
 * Vale mais aqui do que na suíte JVM, e por uma razão específica: estas regras **nunca foram deployadas**.
 * Uma suíte que só fala do que está vivo é a que dá coragem de finalmente subir `firestore.rules`.
 *
 *   npm test                    → só o escopo
 *   SUITE_COMPLETA=1 npm test   → tudo, para medir o que falta revitalizar
 */
const foraDoEscopo = process.env.SUITE_COMPLETA ? describe : describe.skip;

// Atalhos para os bancos autenticados por persona.
const asAgenteA = () => testEnv.authenticatedContext(AGENTE_A).firestore();
const asAgenteB = () => testEnv.authenticatedContext(AGENTE_B).firestore();
const asSupervisor = () => testEnv.authenticatedContext(SUPERVISOR).firestore();
const asAdm = () => testEnv.authenticatedContext(ADM).firestore();
const asGestor = () => testEnv.authenticatedContext(GESTOR).firestore();
const asAnon = () => testEnv.unauthenticatedContext().firestore();

// --- users/{uid}: anti-escalonamento de papel e de vínculo ---
describe('users/{uid} — anti-escalonamento', () => {
  // O nascimento do perfil é o PRIMEIRO ACESSO (ADR-0015 §2.1): quem autoriza o vínculo é o e-mail
  // que a gestão gravou no pré-cadastro. `novo` autentica com o e-mail do funcionário F_NOVO.
  const asNovo = () => testEnv.authenticatedContext('novo', { email: EMAIL_F_NOVO }).firestore();

  test('primeiro acesso cria o próprio perfil vinculado ao funcionário do MESMO e-mail → OK', async () => {
    await assertSucceeds(setDoc(doc(asNovo(), 'users', 'novo'), {
      username: 'novo', email: EMAIL_F_NOVO, papel: 'OPERADOR', funcionarioId: F_NOVO,
    }));
  });

  test('cria o próprio perfil já como ADM → NEGADO', async () => {
    await assertFails(setDoc(doc(asNovo(), 'users', 'novo'), {
      username: 'novo', email: EMAIL_F_NOVO, papel: 'ADM', funcionarioId: F_NOVO,
    }));
  });

  test('cria perfil vinculado ao funcionário de OUTRO e-mail → NEGADO', async () => {
    // Sem esta regra, escrever o próprio funcionarioId seria escolher de quem são as passagens que se
    // "possui" (§8.4) — bastaria apontar para o funcionário de outra pessoa.
    await assertFails(setDoc(doc(asNovo(), 'users', 'novo'), {
      username: 'novo', email: EMAIL_F_NOVO, papel: 'OPERADOR', funcionarioId: F_B,
    }));
  });

  test('cria o próprio perfil SEM vínculo → NEGADO (não existe acesso sem pré-cadastro)', async () => {
    await assertFails(setDoc(doc(asNovo(), 'users', 'novo'), {
      username: 'novo', email: EMAIL_F_NOVO, papel: 'OPERADOR',
    }));
  });

  test('cria perfil apontando para funcionário inexistente → NEGADO', async () => {
    await assertFails(setDoc(doc(asNovo(), 'users', 'novo'), {
      username: 'novo', email: EMAIL_F_NOVO, papel: 'OPERADOR', funcionarioId: 'nao-existe',
    }));
  });

  test('cria perfil de OUTRO uid → NEGADO', async () => {
    await assertFails(setDoc(doc(asNovo(), 'users', AGENTE_A), {
      username: 'x', email: EMAIL_F_NOVO, papel: 'OPERADOR', funcionarioId: F_NOVO,
    }));
  });

  test('operador tenta se auto-promover a ADM (update do papel) → NEGADO', async () => {
    await assertFails(updateDoc(doc(asAgenteA(), 'users', AGENTE_A), { papel: 'ADM' }));
  });

  test('operador tenta trocar o próprio vínculo (roubar a posse do outro) → NEGADO', async () => {
    await assertFails(updateDoc(doc(asAgenteA(), 'users', AGENTE_A), { funcionarioId: F_B }));
  });

  test('operador edita o próprio username (papel e vínculo inalterados) → OK', async () => {
    await assertSucceeds(updateDoc(doc(asAgenteA(), 'users', AGENTE_A), { username: 'novo.nome' }));
  });

  test('delete do próprio perfil → NEGADO', async () => {
    await assertFails(deleteDoc(doc(asAgenteA(), 'users', AGENTE_A)));
  });

  test('leitura de perfil por autenticado → OK', async () => {
    await assertSucceeds(getDoc(doc(asAgenteA(), 'users', AGENTE_B)));
  });
});

// --- rotas/{id}: o pool compartilhado (ADR-0016 §7.1, F7) ---
//
// Três verbos, três autoridades — e o caso mais interessante é o do meio: **editar não existe**.
describe('rotas — pool sem dono, imutável, e o inativar que é da plataforma', () => {
  const novaRota = { portoOrigemId: 'porto-1', portoDestinoId: 'porto-2', distanciaMn: 420, tempoMedioH: 30, ativo: true };

  test('operador LÊ rota (o pool é de todos) → OK', async () => {
    await assertSucceeds(getDoc(doc(asAgenteA(), 'rotas', 'rota-1')));
  });

  test('não autenticado LÊ rota → NEGADO', async () => {
    await assertFails(getDoc(doc(asAnon(), 'rotas', 'rota-1')));
  });

  test('plataforma cria rota → OK', async () => {
    await assertSucceeds(setDoc(doc(asAdm(), 'rotas', 'rota-nova'), novaRota));
  });

  /** Decisão do analista: o supervisor cria **qualquer** rota — a ligação existe independente de quem vende. */
  test('SUPERVISOR cria rota em qualquer par de portos → OK', async () => {
    await assertSucceeds(setDoc(doc(asSupervisor(), 'rotas', 'rota-nova'), novaRota));
  });

  test('AGENTE cria rota → NEGADO (vê o pool, não o monta)', async () => {
    await assertFails(setDoc(doc(asAgenteA(), 'rotas', 'rota-nova'), novaRota));
  });

  /** **Editar não existe para ninguém** — nem para o ADM. Corrigir é criar outra e inativar esta. */
  test('plataforma EDITA a distância de uma rota → NEGADO', async () => {
    await assertFails(updateDoc(doc(asAdm(), 'rotas', 'rota-1'), { distanciaMn: 999 }));
  });

  test('plataforma inativa rota → OK', async () => {
    await assertSucceeds(updateDoc(doc(asAdm(), 'rotas', 'rota-1'), { ativo: false }));
  });

  /** Tirar do pool afeta quem nem sabe que a rota existe: é o único ato daqui que atinge terceiros. */
  test('SUPERVISOR inativa rota do pool → NEGADO', async () => {
    await assertFails(updateDoc(doc(asSupervisor(), 'rotas', 'rota-1'), { ativo: false }));
  });

  /** Inativar não pode virar cavalo de Troia para editar o resto. */
  test('plataforma inativa E muda a distância na mesma escrita → NEGADO', async () => {
    await assertFails(updateDoc(doc(asAdm(), 'rotas', 'rota-1'), { ativo: false, distanciaMn: 999 }));
  });

  test('ninguém apaga rota — o descartado vira registro', async () => {
    await assertFails(deleteDoc(doc(asAdm(), 'rotas', 'rota-1')));
  });

  /**
   * **Limite conhecido**: a unicidade do par não é imposta pelo servidor. Regra não consulta coleção, e
   * derivar o id do par brigaria com a recriação que a imutabilidade exige. A verificação vive no
   * cadastro — impede o acidente, não a corrida.
   */
  test('plataforma cria rota duplicada do mesmo par → OK (limite conhecido)', async () => {
    await assertSucceeds(setDoc(doc(asAdm(), 'rotas', 'rota-duplicada'), novaRota));
  });
});

// --- convites/{email}: quem pode entrar, e com que papel (F6.6) ---
//
// É a coleção que substitui o console para tudo menos o primeiro ADM. O que estes casos travam é a
// razão de ela existir sem afrouxar nada: **o cliente continua não escolhendo o próprio papel**.
describe('convites — só o ADM escreve, e o papel do perfil tem de bater com ele', () => {
  const EMAIL_CONVIDADO = 'convidado@x.com';

  /** O convidado lê o PRÓPRIO convite antes de existir `users/{uid}` — não há papel a consultar ali. */
  test('convidado lê o próprio convite → OK', async () => {
    const db = testEnv.authenticatedContext('novo-uid', { email: EMAIL_CONVIDADO }).firestore();
    await assertSucceeds(getDoc(doc(db, 'convites', EMAIL_CONVIDADO)));
  });

  test('operador lê convite alheio → NEGADO', async () => {
    await assertFails(getDoc(doc(asAgenteA(), 'convites', EMAIL_CONVIDADO)));
  });

  test('ADM lê qualquer convite → OK (é ele quem administra o acesso)', async () => {
    await assertSucceeds(getDoc(doc(asAdm(), 'convites', EMAIL_CONVIDADO)));
  });

  test('ADM cria convite → OK', async () => {
    await assertSucceeds(setDoc(doc(asAdm(), 'convites', 'outro@x.com'), { papel: 'GESTOR', nome: 'X' }));
  });

  /** GESTOR administra o negócio da plataforma, não o acesso a ela (ADR-0021 D1). */
  test('GESTOR cria convite → NEGADO', async () => {
    await assertFails(setDoc(doc(asGestor(), 'convites', 'outro@x.com'), { papel: 'ADM', nome: 'X' }));
  });

  test('SUPERVISOR cria convite → NEGADO', async () => {
    await assertFails(setDoc(doc(asSupervisor(), 'convites', 'outro@x.com'), { papel: 'OPERADOR' }));
  });

  /** O convite vira registro: usado ou não, ele responde "por que esta pessoa tem este papel?". */
  test('ADM apaga convite → NEGADO', async () => {
    await assertFails(deleteDoc(doc(asAdm(), 'convites', EMAIL_CONVIDADO)));
  });

  test('convidado marca o próprio convite como usado → OK', async () => {
    const db = testEnv.authenticatedContext('novo-uid', { email: EMAIL_CONVIDADO }).firestore();
    await assertSucceeds(updateDoc(doc(db, 'convites', EMAIL_CONVIDADO), { usado: true }));
  });

  /** A brecha óbvia, fechada: usar o convite não é editar o convite. */
  test('convidado muda o PAPEL do próprio convite → NEGADO', async () => {
    const db = testEnv.authenticatedContext('novo-uid', { email: EMAIL_CONVIDADO }).firestore();
    await assertFails(updateDoc(doc(db, 'convites', EMAIL_CONVIDADO), { papel: 'ADM' }));
  });
});

// --- users/{uid}: o papel do perfil vem do convite (F6.6) ---
describe('perfil no primeiro acesso — o papel vem do convite, não do cliente', () => {
  const EMAIL_CONVIDADO = 'convidado@x.com';
  const EMAIL_CONVIDADO_ADM = 'convidado-adm@x.com';

  /**
   * O caso que a F6.6 destrava: `ADM`/`GESTOR` passam a poder nascer pelo app — **desde que exista um
   * convite**, que só o ADM escreve. Antes disso, só pelo console.
   */
  test('perfil de plataforma COM convite → OK, e sem exigir funcionário', async () => {
    const db = testEnv.authenticatedContext('uid-adm-novo', { email: EMAIL_CONVIDADO_ADM }).firestore();
    await assertSucceeds(setDoc(doc(db, 'users', 'uid-adm-novo'), {
      email: EMAIL_CONVIDADO_ADM, username: 'convidado-adm', papel: 'ADM', funcionarioId: '',
    }));
  });

  /** O anti-escalonamento, agora dito de outro jeito: o papel tem de **bater** com o convite. */
  test('perfil com papel DIFERENTE do convite → NEGADO', async () => {
    const db = testEnv.authenticatedContext('uid-novo', { email: EMAIL_CONVIDADO }).firestore();
    await assertFails(setDoc(doc(db, 'users', 'uid-novo'), {
      email: EMAIL_CONVIDADO, username: 'convidado', papel: 'ADM', funcionarioId: '',
    }));
  });

  test('sem convite, o único papel possível continua sendo OPERADOR', async () => {
    const db = testEnv.authenticatedContext('uid-sem-convite', { email: EMAIL_F_NOVO }).firestore();
    await assertFails(setDoc(doc(db, 'users', 'uid-sem-convite'), {
      email: EMAIL_F_NOVO, username: 'novo', papel: 'GESTOR', funcionarioId: F_NOVO,
    }));
    await assertSucceeds(setDoc(doc(db, 'users', 'uid-sem-convite'), {
      email: EMAIL_F_NOVO, username: 'novo', papel: 'OPERADOR', funcionarioId: F_NOVO,
    }));
  });
});

// --- funcionarios/{id}: a Equipe (ADR-0015 §8.5, reescrita sobre VÍNCULOS na F6.3) ---
//
// Entrou no escopo na F6.2 (definição de pronto, ADR-0022 D6) e **mudou de coordenada na F6.3**: onde a
// regra lia a String `agencia` do autor, agora ela procura o par `{empresaId, cargo}` dentro do array de
// vínculos. Os três invariantes são os mesmos; o que muda é como cada um é dito.
describe('funcionarios — escrita por vínculo, e o supervisor que não fabrica par', () => {
  const novoAgenteNaMatriz = { nome: 'X', vinculos: [AGENTE_NA_MATRIZ], empresaIds: [E_MATRIZ] };

  test('operador LÊ funcionário (a UI resolve nome e vínculos por aqui) → OK', async () => {
    await assertSucceeds(getDoc(doc(asAgenteA(), 'funcionarios', F_B)));
  });

  test('operador cria funcionário → NEGADO (cadastro é da gestão)', async () => {
    await assertFails(setDoc(doc(asAgenteA(), 'funcionarios', 'novo'), novoAgenteNaMatriz));
  });

  test('SUPERVISOR cria agente na PRÓPRIA empresa → OK', async () => {
    await assertSucceeds(setDoc(doc(asSupervisor(), 'funcionarios', 'novo'), novoAgenteNaMatriz));
  });

  test('SUPERVISOR cria agente em OUTRA empresa → NEGADO', async () => {
    await assertFails(setDoc(doc(asSupervisor(), 'funcionarios', 'novo'), {
      nome: 'X', vinculos: [AGENTE_NA_MARE], empresaIds: [E_MARE],
    }));
  });

  /**
   * **Mudou na F6.7**: o supervisor gere a equipe dele por inteiro, e promover concede poder **dentro
   * de uma empresa só** — decisão de negócio dela. O que segura é o resto: só na empresa dele, e nunca
   * nos próprios vínculos.
   */
  test('SUPERVISOR cria alguém já como SUPERVISOR na PRÓPRIA empresa → OK', async () => {
    await assertSucceeds(setDoc(doc(asSupervisor(), 'funcionarios', 'novo'), {
      nome: 'X', vinculos: [SUPERVISOR_NA_MATRIZ], empresaIds: [E_MATRIZ],
    }));
  });

  test('SUPERVISOR promove membro da própria empresa → OK', async () => {
    await assertSucceeds(updateDoc(doc(asSupervisor(), 'funcionarios', F_A), {
      vinculos: [SUPERVISOR_NA_MATRIZ], empresaIds: [E_MATRIZ],
    }));
  });

  test('SUPERVISOR remove membro da própria empresa → OK', async () => {
    await assertSucceeds(deleteDoc(doc(asSupervisor(), 'funcionarios', F_A)));
  });

  test('SUPERVISOR remove membro de OUTRA empresa → NEGADO', async () => {
    await assertFails(deleteDoc(doc(asSupervisor(), 'funcionarios', F_OUTRA_AGENCIA)));
  });

  /** Um supervisor que se removesse deixaria a empresa sem quem a gerisse, sem ninguém decidir isso. */
  test('SUPERVISOR remove a SI MESMO → NEGADO', async () => {
    await assertFails(deleteDoc(doc(asSupervisor(), 'funcionarios', F_SUPERVISOR)));
  });

  test('AGENTE remove membro da própria empresa → NEGADO (não gere ninguém)', async () => {
    await assertFails(deleteDoc(doc(asAgenteA(), 'funcionarios', F_B)));
  });

  /**
   * O caso que só existe com vínculos: alguém que serve a **duas** empresas não é gerível por um
   * supervisor, porque metade dos vínculos dessa pessoa não é dele. `hasOnly` diz isso numa linha.
   */
  test('SUPERVISOR cria alguém com vínculo na dele E em outra → NEGADO', async () => {
    await assertFails(setDoc(doc(asSupervisor(), 'funcionarios', 'novo'), {
      nome: 'X', vinculos: [AGENTE_NA_MATRIZ, AGENTE_NA_MARE], empresaIds: [E_MATRIZ, E_MARE],
    }));
  });

  test('SUPERVISOR edita membro da própria empresa (nome) → OK', async () => {
    await assertSucceeds(updateDoc(doc(asSupervisor(), 'funcionarios', F_A), { nome: 'Agente A2' }));
  });

  test('SUPERVISOR edita membro de OUTRA empresa → NEGADO', async () => {
    await assertFails(updateDoc(doc(asSupervisor(), 'funcionarios', F_OUTRA_AGENCIA), { nome: 'Outro' }));
  });

  test('SUPERVISOR transfere membro para outra empresa → NEGADO (não exporta gente)', async () => {
    await assertFails(updateDoc(doc(asSupervisor(), 'funcionarios', F_A), {
      vinculos: [AGENTE_NA_MARE], empresaIds: [E_MARE],
    }));
  });

  test('SUPERVISOR traz membro de outra empresa para a dele → NEGADO (nem importa)', async () => {
    await assertFails(updateDoc(doc(asSupervisor(), 'funcionarios', F_OUTRA_AGENCIA), {
      vinculos: [AGENTE_NA_MATRIZ], empresaIds: [E_MATRIZ],
    }));
  });

  /** O que NÃO mudou na F6.7: ninguém mexe nos próprios vínculos — nem quem gere a equipe. */
  test('SUPERVISOR se promove (mexe nos próprios vínculos) → NEGADO', async () => {
    await assertFails(updateDoc(doc(asSupervisor(), 'funcionarios', F_SUPERVISOR), {
      vinculos: [AGENTE_NA_MARE], empresaIds: [E_MARE],
    }));
  });

  test('AGENTE edita membro da própria empresa → NEGADO (não é cargo de gestão)', async () => {
    await assertFails(updateDoc(doc(asAgenteA(), 'funcionarios', F_B), { nome: 'B2' }));
  });

  test('plataforma cria funcionário em qualquer empresa → OK', async () => {
    await assertSucceeds(setDoc(doc(asAdm(), 'funcionarios', 'novo'), {
      nome: 'X', vinculos: [AGENTE_NA_MARE], empresaIds: [E_MARE],
    }));
  });

  test('plataforma promove OUTRO funcionário a SUPERVISOR → OK', async () => {
    await assertSucceeds(updateDoc(doc(asAdm(), 'funcionarios', F_A), {
      vinculos: [SUPERVISOR_NA_MATRIZ], empresaIds: [E_MATRIZ],
    }));
  });

  test('plataforma altera os PRÓPRIOS vínculos → NEGADO (anti-escalonamento do eixo de negócio)', async () => {
    await assertFails(updateDoc(doc(asAdm(), 'funcionarios', F_ADM), {
      vinculos: [SUPERVISOR_NA_MATRIZ], empresaIds: [E_MATRIZ],
    }));
  });

  test('plataforma edita o próprio funcionário sem tocar nos vínculos → OK', async () => {
    await assertSucceeds(updateDoc(doc(asAdm(), 'funcionarios', F_ADM), { nome: 'Adm Silva' }));
  });
});

// --- Catálogos (embarcação como representante): ler todos autenticados, escrever só papel de plataforma ---
describe('flotilha (embarcações) — leitura ampla, escrita só papel de plataforma', () => {
  test('operador LÊ embarcação (a venda precisa) → OK', async () => {
    await assertSucceeds(getDoc(doc(asAgenteA(), 'embarcacoes', 'embarcacao-1')));
  });

  test('não autenticado LÊ embarcação → NEGADO', async () => {
    await assertFails(getDoc(doc(asAnon(), 'embarcacoes', 'embarcacao-1')));
  });

  test('operador ESCREVE embarcação → NEGADO', async () => {
    await assertFails(setDoc(doc(asAgenteA(), 'embarcacoes', 'embarcacao-2'), { nome: 'Embarcação 2' }));
  });

  test('SUPERVISOR escreve embarcação (cargo de agência não é papel de plataforma) → NEGADO', async () => {
    await assertFails(setDoc(doc(asSupervisor(), 'embarcacoes', 'embarcacao-2'), { nome: 'Embarcação 2' }));
  });

  test('plataforma (ADM) escreve embarcação → OK', async () => {
    await assertSucceeds(setDoc(doc(asAdm(), 'embarcacoes', 'embarcacao-2'), { nome: 'Embarcação 2' }));
  });
});

// --- Localidades: capacidade da plataforma, e a coleção onde NÃO se apaga (ADR-0016 §5) ---
describe('localidades — escrita só de plataforma, e delete físico impossível', () => {
  test('operador LÊ localidade (a venda vai precisar do porto, e o porto dela) → OK', async () => {
    await assertSucceeds(getDoc(doc(asAgenteA(), 'localidades', 'loc-1')));
  });

  test('não autenticado LÊ localidade → NEGADO', async () => {
    await assertFails(getDoc(doc(asAnon(), 'localidades', 'loc-1')));
  });

  test('operador ESCREVE localidade → NEGADO', async () => {
    await assertFails(setDoc(doc(asAgenteA(), 'localidades', 'loc-2'), { municipio: 'Belém', uf: 'PA' }));
  });

  test('SUPERVISOR escreve localidade (cargo de agência não é papel de plataforma) → NEGADO', async () => {
    await assertFails(setDoc(doc(asSupervisor(), 'localidades', 'loc-2'), { municipio: 'Belém', uf: 'PA' }));
  });

  test('plataforma (ADM) cria localidade → OK', async () => {
    await assertSucceeds(
      setDoc(doc(asAdm(), 'localidades', 'loc-2'), { municipio: 'Belém', uf: 'PA', codigoIbge: '1501402', ativo: true }),
    );
  });

  /**
   * O delete lógico com dentes de servidor: inativar é `update`, e passa. Apagar é `delete`, e não passa
   * NEM para o ADM — a única coleção do app assim. A razão é referencial: o porto aponta para a
   * localidade, e a rota para o porto; um `delete` que escapasse deixaria histórico apontando para o
   * nada, e nenhuma regra de aplicativo recupera um documento que já não existe.
   */
  test('plataforma INATIVA localidade (delete lógico) → OK', async () => {
    await assertSucceeds(updateDoc(doc(asAdm(), 'localidades', 'loc-1'), { ativo: false }));
  });

  test('plataforma APAGA localidade → NEGADO (não existe apagar aqui)', async () => {
    await assertFails(deleteDoc(doc(asAdm(), 'localidades', 'loc-1')));
  });
});

// --- Portos: a mesma forma da localidade, um elo adiante na cadeia de referências (ADR-0016 §5) ---
describe('portos — escrita só de plataforma, e delete físico impossível', () => {
  test('operador LÊ porto (quem vende precisa saber de onde sai) → OK', async () => {
    await assertSucceeds(getDoc(doc(asAgenteA(), 'portos', 'porto-1')));
  });

  test('não autenticado LÊ porto → NEGADO', async () => {
    await assertFails(getDoc(doc(asAnon(), 'portos', 'porto-1')));
  });

  test('operador ESCREVE porto → NEGADO', async () => {
    await assertFails(
      setDoc(doc(asAgenteA(), 'portos', 'porto-2'), { nome: 'Porto Central', localidadeId: 'loc-1' }),
    );
  });

  test('SUPERVISOR escreve porto (cargo de agência não é papel de plataforma) → NEGADO', async () => {
    await assertFails(
      setDoc(doc(asSupervisor(), 'portos', 'porto-2'), { nome: 'Porto Central', localidadeId: 'loc-1' }),
    );
  });

  test('plataforma (ADM) cria porto → OK', async () => {
    await assertSucceeds(
      setDoc(doc(asAdm(), 'portos', 'porto-2'), {
        nome: 'Porto Central',
        localidadeId: 'loc-1',
        ativo: true,
      }),
    );
  });

  /**
   * O mesmo par da localidade, e aqui a razão referencial é ainda mais direta: a **rota** e a
   * **concessão** guardam o id do porto. Inativar é `update` e passa; apagar não passa nem para o ADM.
   */
  test('plataforma INATIVA porto (delete lógico) → OK', async () => {
    await assertSucceeds(updateDoc(doc(asAdm(), 'portos', 'porto-1'), { ativo: false }));
  });

  test('plataforma APAGA porto → NEGADO (não existe apagar aqui)', async () => {
    await assertFails(deleteDoc(doc(asAdm(), 'portos', 'porto-1')));
  });

  /**
   * **A unicidade `(nome, localidade)` não é do servidor hoje** (ADR-0016 §5, paridade na F8): o
   * cadastro a verifica em memória, e a regra deixa passar. O caso está aqui como *documentação
   * executável* do limite — quando a F8 fechar isso, ele vira `assertFails` e é este arquivo que
   * cobra a mudança.
   */
  test('plataforma cria porto homônimo na mesma localidade → OK (limite conhecido, F8)', async () => {
    await assertSucceeds(
      setDoc(doc(asAdm(), 'portos', 'porto-3'), {
        nome: 'Porto de Val-de-Cães',
        localidadeId: 'loc-1',
        ativo: true,
      }),
    );
  });
});

// --- Passagens: emissão, posse e imutabilidade do dono ---
foraDoEscopo('passagens — emissão sem forjar dono', () => {
  test('agente cria passagem com funcionarioId = o do próprio perfil → OK', async () => {
    await assertSucceeds(setDoc(doc(asAgenteA(), 'passagens', 'p1'), { funcionarioId: F_A, valor: 5 }));
  });

  test('agente cria passagem carimbando OUTRO funcionário (forjar) → NEGADO', async () => {
    await assertFails(setDoc(doc(asAgenteA(), 'passagens', 'p1'), { funcionarioId: F_B, valor: 5 }));
  });

  test('agente cria passagem carimbando o próprio UID (vocabulário antigo) → NEGADO', async () => {
    // Lock do ADR-0015 §8.4: o campo mudou de significado; uid não é mais dono válido.
    await assertFails(setDoc(doc(asAgenteA(), 'passagens', 'p1'), { funcionarioId: AGENTE_A, valor: 5 }));
  });

  test('plataforma SEM funcionário emite passagem → NEGADO (quem emite é da operação)', async () => {
    await assertFails(setDoc(doc(asGestor(), 'passagens', 'p1'), { funcionarioId: '', valor: 5 }));
  });
});

// --- Isolamento por agência: por UI, NÃO pelo servidor (ADR-0015 §3, débito registrado) ---
foraDoEscopo('passagens — o servidor NÃO isola por agência (lock do débito)', () => {
  test('agente lê passagem de OUTRA agência → OK, e isso é o débito, não um bug', async () => {
    // O recorte por agência da listagem (P2.6) vive na consulta do app. Um cliente adulterado ainda
    // lê a passagem de outra agência — aceitável enquanto todas são do mesmo operador (§3). Este teste
    // existe para que a promoção da regra ao servidor seja uma decisão, e não uma descoberta.
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'passagens', 'de-outra-agencia'), {
        funcionarioId: F_OUTRA_AGENCIA, agencia: 'AGENCIA MARE', valor: 10,
      });
    });
    await assertSucceeds(getDoc(doc(asAgenteA(), 'passagens', 'de-outra-agencia')));
  });
});

foraDoEscopo('passagens — editar/deletar por posse', () => {
  test('agente edita a PRÓPRIA passagem → OK', async () => {
    await assertSucceeds(setDoc(doc(asAgenteB(), 'passagens', 'alheia'), { funcionarioId: F_B, valor: 20 }));
  });

  test('agente edita passagem ALHEIA → NEGADO', async () => {
    await assertFails(updateDoc(doc(asAgenteA(), 'passagens', 'alheia'), { valor: 99 }));
  });

  test('SUPERVISOR edita passagem alheia pelo CARGO (papel dele é só OPERADOR) → OK', async () => {
    await assertSucceeds(updateDoc(doc(asSupervisor(), 'passagens', 'alheia'), { valor: 99 }));
  });

  test('plataforma (ADM) edita passagem alheia → OK', async () => {
    await assertSucceeds(updateDoc(doc(asAdm(), 'passagens', 'alheia'), { valor: 99 }));
  });

  test('plataforma sem vínculo (GESTOR) edita passagem alheia → OK (vale pelo papel)', async () => {
    await assertSucceeds(updateDoc(doc(asGestor(), 'passagens', 'alheia'), { valor: 99 }));
  });

  test('editar-qualquer NÃO pode reatribuir o dono (funcionarioId imutável) → NEGADO', async () => {
    await assertFails(updateDoc(doc(asSupervisor(), 'passagens', 'alheia'), { funcionarioId: F_SUPERVISOR }));
  });

  test('agente deleta passagem alheia → NEGADO', async () => {
    await assertFails(deleteDoc(doc(asAgenteA(), 'passagens', 'alheia')));
  });

  test('dono deleta a própria passagem → OK', async () => {
    await assertSucceeds(deleteDoc(doc(asAgenteB(), 'passagens', 'alheia')));
  });
});

// --- Contador (passagens/contador): monotônico, sem delete (endurecimento ADR-0011) ---
foraDoEscopo('passagens/contador — incremento monotônico e indestrutível', () => {
  test('papel conhecido incrementa (100 → 101) → OK', async () => {
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
// Carimbo de embarque que o app grava: status + o próprio UID como embarcadaPorId + nome + quando.
// Aqui continua sendo uid: é auditoria de acesso, e só o uid o servidor confere contra request.auth.
const carimboEmbarque = (uid, nome) => ({
  status: 'EMBARCADA',
  embarcadaPorId: uid,
  embarcadaPor: nome,
  embarcadaEm: '02/07/2026 09:30',
});

foraDoEscopo('passagens — confirmação de embarque (eixo novo, qualquer papel)', () => {
  test('agente NÃO-dono confirma embarque (EMITIDA→EMBARCADA) carimbando o próprio uid → OK', async () => {
    // AGENTE_A embarca a passagem 'emitida-b' (dono = funcionário B): quem está na doca valida.
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

foraDoEscopo('passagens — arestas legais da FSM (avança, nunca retrocede nem pula)', () => {
  test('dono emite a própria passagem (A_EMITIR→EMITIDA) → OK', async () => {
    await assertSucceeds(updateDoc(doc(asAgenteA(), 'passagens', 'aemitir-a'), { status: 'EMITIDA' }));
  });

  test('agente NÃO-dono emite passagem alheia (A_EMITIR→EMITIDA) → NEGADO (não é o eixo de embarque)', async () => {
    await assertFails(updateDoc(doc(asAgenteA(), 'passagens', 'aemitir-b'), { status: 'EMITIDA' }));
  });

  test('pulo A_EMITIR→EMBARCADA (sem passar por EMITIDA) → NEGADO', async () => {
    await assertFails(updateDoc(doc(asAdm(), 'passagens', 'aemitir-b'), carimboEmbarque(ADM, 'Adm')));
  });

  test('retrocesso EMBARCADA→EMITIDA (mesmo papel de plataforma) → NEGADO (embarque é irreversível)', async () => {
    await assertFails(updateDoc(doc(asAdm(), 'passagens', 'embarcada-b'), { status: 'EMITIDA' }));
  });

  test('retrocesso EMITIDA→A_EMITIR → NEGADO', async () => {
    await assertFails(updateDoc(doc(asAdm(), 'passagens', 'emitida-b'), { status: 'A_EMITIR' }));
  });
});
// --- empresas/{id}/atuacoes/{ATUACAO}: o que a PARTE faz (ADR-0016 §4, ADR-0020 F5d) ---
describe('empresas/atuacoes — cadastro de plataforma, id fechado', () => {
  const atuacao = (db, id) => doc(db, 'empresas', EMPRESA, 'atuacoes', id);

  test('operador LÊ as atuações → OK (é daqui que a família do menu deriva)', async () => {
    await assertSucceeds(getDoc(atuacao(asAgenteA(), 'AGENCIAMENTO')));
  });

  test('anônimo lê → NEGADO', async () => {
    await assertFails(getDoc(atuacao(asAnon(), 'AGENCIAMENTO')));
  });

  test('ADM declara que a parte passa a TRANSPORTAR → OK', async () => {
    await assertSucceeds(setDoc(atuacao(asAdm(), 'TRANSPORTE'), { embarcacaoIds: [] }));
  });

  test('GESTOR também cadastra → OK (painel é dos dois papéis de plataforma)', async () => {
    await assertSucceeds(setDoc(atuacao(asGestor(), 'TRANSPORTE'), { embarcacaoIds: [] }));
  });

  test('operador cadastra atuação → NEGADO (quem monta a parte é o painel)', async () => {
    await assertFails(setDoc(atuacao(asAgenteA(), 'TRANSPORTE'), { embarcacaoIds: [] }));
  });

  test('SUPERVISOR cadastra atuação → NEGADO (ele opera a agência, não cria a parte)', async () => {
    await assertFails(setDoc(atuacao(asSupervisor(), 'TRANSPORTE'), { embarcacaoIds: [] }));
  });

  test('ADM concede um embarcação à atuação existente → OK', async () => {
    await assertSucceeds(updateDoc(atuacao(asAdm(), 'AGENCIAMENTO'), { embarcacaoIds: ['embarcacao-1', 'embarcacao-2'] }));
  });

  test('operador se autoconcede um embarcação → NEGADO (concessão é allow-list de segurança)', async () => {
    await assertFails(updateDoc(atuacao(asAgenteA(), 'AGENCIAMENTO'), { embarcacaoIds: ['embarcação-9'] }));
  });

  // A segunda dimensão da concessão (F7): ONDE a parte pode operar. Mesmo eixo de escrita da primeira —
  // e é preciso que seja, porque é dela que a linha ofertável passa a ser deduzida: quem consegue
  // escrever `portoIds` sozinho oferta a travessia que quiser, sem nunca tocar em `embarcacaoIds`.
  test('ADM concede um porto à atuação existente → OK', async () => {
    await assertSucceeds(updateDoc(atuacao(asAdm(), 'AGENCIAMENTO'), { portoIds: ['porto-manaus', 'porto-parintins'] }));
  });

  test('operador se autoconcede um porto → NEGADO (a outra metade da allow-list)', async () => {
    await assertFails(updateDoc(atuacao(asAgenteA(), 'AGENCIAMENTO'), { portoIds: ['porto-manaus'] }));
  });

  test('SUPERVISOR se autoconcede um porto → NEGADO (a concessão é dada, não tomada)', async () => {
    await assertFails(updateDoc(atuacao(asSupervisor(), 'AGENCIAMENTO'), { portoIds: ['porto-manaus'] }));
  });

  test('ADM cria atuação com id DESCONHECIDO → NEGADO (fail-closed na origem)', async () => {
    await assertFails(setDoc(atuacao(asAdm(), 'ARMAZENAGEM'), { embarcacaoIds: [] }));
  });

  test('ADM cria atuação com id em caixa errada → NEGADO (o id é o name canônico)', async () => {
    await assertFails(setDoc(atuacao(asAdm(), 'agenciamento'), { embarcacaoIds: [] }));
  });

  test('ADM remove a atuação (a parte deixa de exercê-la) → OK', async () => {
    await assertSucceeds(deleteDoc(atuacao(asAdm(), 'AGENCIAMENTO')));
  });

  test('operador remove atuação → NEGADO', async () => {
    await assertFails(deleteDoc(atuacao(asAgenteA(), 'AGENCIAMENTO')));
  });
});
