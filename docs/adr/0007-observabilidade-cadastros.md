# ADR-0007: Observabilidade dos cadastros (telemetria genérica + registro semântico)

**Status:** Aceita

> Estende o padrão SRE da emissão ([ADR-0004](0004-snapshot-e-observabilidade-emissao.md)) aos
> módulos de cadastro do molde ([ADR-0006](0006-molde-de-cadastro.md)): Viagem, Agente, Empresa.

**Contexto**

Os cadastros só têm `Log` — sem eventos navegáveis, sem breadcrumb, sem erro não-fatal registrado.
Além disso, os repositórios do molde gravam **Room otimista + `documento.set(...)`** no Firestore
**sem aguardar** o `Task`: o `catch` só pega erro síncrono (Room/mapper), então a **falha de
transmissão é invisível** — o mesmo achado #2 do ADR-0004, agora no cadastro. A emissão já resolveu
isso com uma porta de telemetria + uma camada semântica; falta trazer o padrão para cá.

**Opções consideradas**

1. Reusar `EmissaoTelemetry` como está nos cadastros — pragmático, mas o nome "Emissao" fica enganoso
   para um uso geral.
2. Criar uma porta nova `CadastroTelemetry` — duplica as mesmas primitivas SRE.
3. **Generalizar** a porta: `EmissaoTelemetry` → `Telemetry` (as primitivas `evento`/`rastro`/`naoFatal`
   são agnósticas de domínio), reusada por emissão e cadastro; camada semântica própria por domínio.

**Decisão**

Opção 3. As primitivas são genéricas; o que é específico é a **semântica** (o que cada desfecho
significa), que fica numa camada por domínio.

- **Porta genérica `Telemetry`** (renomeia `EmissaoTelemetry`), impl `FirebaseTelemetry` (renomeia
  `FirebaseEmissaoTelemetry`; único ponto que toca Firebase — Analytics + Crashlytics + espelho no
  `Log`). `RegistroEmissao` passa a depender de `Telemetry`; `FirebaseModule` liga a impl à porta.
- **`RegistroCadastro`** (nova camada semântica, pura, injeta `Telemetry`) — traduz desfechos de
  persistência do cadastro, com a taxonomia do ADR-0004:
  | Sinal | Quando | Como |
  |---|---|---|
  | **Sucesso** | Room ok **e** Firestore confirmou (`.set().await()`) | `evento(cadastro_salvo{entidade,id})` + `rastro` |
  | **Warning** | Room ok, **Firestore rejeitou/offline** | `evento(cadastro_pendente_sync{entidade,motivo})` + `naoFatal` (degradado, não erro) |
  | **Falha** | Room falhou / erro não recuperável | `evento(cadastro_falha{entidade,motivo})` + `naoFatal` |
- **Aguardar o `.set()`** (`.set(...).await()`) nos repos do molde — converte o silêncio em Sucesso
  confirmado (online) ou Warning observável (offline). O **Room otimista** já garante o dado local,
  então aguardar o ack não custa offline-first.
- **Onde**: a telemetria vive no **repositório** (`salvar`), que conhece o desfecho Room/Firestore.
  O evento de UI (o `Channel` de sucesso do molde) segue no ViewModel. Telemetria = persistência (SRE);
  evento = navegação (UI).
- **Testes**: `RegistroCadastro` e os repos testáveis com um `FakeTelemetry` (sem Firebase).

**Consequências**

- O **rename da porta** (`EmissaoTelemetry`→`Telemetry`, `FirebaseEmissaoTelemetry`→`FirebaseTelemetry`)
  toca a emissão (`RegistroEmissao`) e o `FirebaseModule` — mudança mecânica, mas fora do escopo "só
  cadastro".
- **`salvar` passa a suspender até o ack do Firestore** (antes retornava após enfileirar o `set`). O
  Room otimista mantém o offline; o custo é o `salvar` esperar a confirmação (ou o erro) — que é
  justamente o que torna Sucesso/Warning observáveis.
- Mais uma dependência (`RegistroCadastro`/`Telemetry`) nos repos de cadastro.

**Alternativas futuras**

- **Latência/trace** (4º pilar SRE) — `suspend fun <T> trace(nome, bloco)` na porta, se quisermos medir
  o tempo de save/sync.
- **Catálogo de nomes de evento** centralizado, se a quantidade de eventos crescer.
- Rollout: ligar primeiro na **Empresa**, validar, depois Agente/Viagem. **Feito** — os três cadastros
  emitem `salvou`/`pendenteDeSync`/`falhou` no `salvar`.
- **Observabilidade da exclusão** (dívida): `ViagemFirestoreRepository.deletar` ainda faz `.delete()`
  fire-and-forget com só `Log.e` — o mesmo silêncio de transmissão do achado #2, agora na exclusão.
  A taxonomia atual (`salvou`/`pendenteDeSync`/`falhou`) é sobre *gravação de cadastro*; cobrir exclusão
  exige um desfecho semântico novo (ex.: `deletou`/`pendenteDeSyncDelecao`) + `.delete().await()`.
  Fica fora deste ADR (escopo = `salvar`); reavaliar quando exclusão precisar ser observável.
