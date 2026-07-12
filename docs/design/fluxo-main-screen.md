# Fluxo da Main Screen — Estudo de Design e Remodelação

Estudo do estado atual da Main Screen e proposta de remodelação: **bottom bar reduzida a
Início + Menu**, com um **menu lateral (drawer) à direita, arrastável**, concentrando as seções
Passagem, Viagem, Agente e Empresa. A opção "Operações" é dissolvida para dentro do menu e seus
componentes atuais são removidos.

> Documento de design (pré-implementação), no mesmo espírito de
> [fluxo-login.md](fluxo-login.md). Ancorado no código concreto.

---

## 1. Estado atual (concreto)

### Estrutura
```
MainActivity → FluviAppNavHost → mainScreenGraph → mainScreenNavComposable → MainScreen
MainScreen → CommonScreen → CommonScaffold(Scaffold: FluviTopAppBar + FluviBottomAppBar + PullToRefresh)
```
- `CommonScaffold.kt:49` monta o `Scaffold`; a bottom bar só aparece se `isShowBottomAppBar`.
- O conteúdo é dirigido por `MainScreenUiState.mainScreenState` (`MainScreenState.kt`):
  `LOADING | HOME | PASSAGENS(List<DadosBotoesMenus>) | OPERACOES(List<MenuBotoesCategoria>)`.
- `MainScreenViewModel.atualizaMainPage()` troca o estado + flags `homeActive/passagensActive/
  operacoesActive` + título.

### As "3 abas" NÃO são destinos de navegação
São **troca de conteúdo** dentro da própria MainScreen:
- **HOME** → `HomeContent` (lista de viagens disponíveis; card com "nova passagem").
- **PASSAGENS** → `MenuPassagem` (lista chata de cards: Pesquisar passagem, Balanço).
- **OPERAÇÕES** → `MenuOperacoes`/`MenuOperacoesCard` (categorias **expansíveis**; hoje só "Viagens":
  Nova/Pesquisar). A categoria **"Agentes" está comentada** em `MainScreenNavComposable.kt:135-150`.

Cada item de menu então **navega para fora** (forms/pesquisas) via os `onNavega…` do NavHost.

### Problemas identificados
1. **Bottom bar fora do tema novo** (queixa que motivou o trabalho): `FluviBottomAppBar.kt:41`
   fixa `NavyBlue`/`White` na mão, com `ButtonBottomAppBar` custom (divisores verticais), **sem**
   `MaterialTheme` — não acompanha o tema claro/escuro.
2. **Dois modelos de menu divergentes**: Passagens usa `List<DadosBotoesMenus>` (flat), Operações
   usa `List<MenuBotoesCategoria>` (expansível). `MenuBotoesCategoria` só existe para Operações.
3. **Nomeação enganosa**: o parâmetro da bottom bar chama-se `onClickMenuViagens`
   (`MainScreen.kt:50`) mas dispara Operações (`onClickMenuOperacoes`).
4. **Gate por cargo** acoplado à bottom bar: Operações só aparece se `isDiretorOuAdm`
   (`FluviBottomAppBar.kt:66`, `isShowMenuViagens`).
5. **Cadastro de Agente órfão**: telas/VM/nav existem e estão registrados
   (`FluviAppNavHost.kt:169-185`), mas **sem ponto de entrada** (bloco comentado).
6. **Empresa sem cadastro**: só `EmpresaRepository` read-only + seed.

### Modelos e cadastros (fatos que corrigem premissas)
| Entidade | Modelo | Cadastro hoje |
|---|---|---|
| **Viagem** | `id`(PK), codigo, empresa, navio, origem, destino | ✅ completo (form/VM/nav, criar+editar) |
| **Agente** | `id`(PK auto Firestore), `descricaoNome`(nome), `agencia`(campo, **não** PK), `lotacao`, `podeSelecionarFormaPagamento` | ✅ existe e registrado, porém **inacessível** |
| **Empresa** | `id`(PK), nome, razaoSocial, cnpj, endereco, telefone1, telefone2 | ❌ inexistente (repo read-only) |

- IDs: Viagem/Agente usam **auto-id do Firestore** quando id vazio/nulo
  (`ViagemFirestoreRepository.kt:64`, `AgenteRepository.kt:52`). `agencia` do Agente é campo comum.
- `EmpresaRepository` (`services/repository/cadastro/viagem/EmpresaRepository.kt`) é **read-only**
  (`sincronizar`, `obterTodas`, `obterPorId`, `obterPorNome`) — sem `salvar`.

> ⚠️ **Discrepância com a premissa**: o pedido descreve Agente como "apenas nome e agência, agência
> string e PK controlada pelo Firestore". No código, a PK é o `id` (doc id auto do Firestore) e
> `agencia` é campo comum; além disso há `lotacao` e `podeSelecionarFormaPagamento`. Ver
> [Decisões](#8-decisões-a-confirmar).

---

## 2. Remodelação alvo — visão

- **Bottom bar → 2 itens: Início | Menu** (temada via `MaterialTheme`).
- **Menu = drawer lateral à direita, arrastável ⟵**, contendo as seções.
- **Seções**: Passagem, Viagem, Agente, Empresa. "Operações" **deixa de existir** — seus filhos
  (Nova/Pesquisar Viagem) passam a ser a seção **Viagem**.
- **Conteúdo centralizado na MainScreen**: tocar uma seção fecha o drawer e mostra o **menu de
  ações** daquela seção (Cadastrar / Pesquisar) na área de conteúdo — reaproveitando o padrão de
  cards de `MenuPassagem`/`CardBotaoMenu`.

```
┌─────────────────────────────┐        drawer (direita, arrasta ⟵)
│  FluviWordmark      [user]   │        ┌───────────────────────┐
│                              │        │  Menu                 │
│   « conteúdo da seção »      │        │  ─────────────────    │
│   (Início = viagens;         │  ◄──── │  🎫 Passagem          │
│    seção = cards de ação)    │        │  🚢 Viagem            │
│                              │        │  👤 Agente            │
│                              │        │  🏢 Empresa           │
├──────────────┬──────────────┤        │  ─────────────────    │
│  ⌂ Início    │   ☰ Menu     │        │  (gate por cargo?)    │
└──────────────┴──────────────┘        └───────────────────────┘
```

### Drawer à direita (nota técnica)
`ModalNavigationDrawer` (M3) abre do **start** (esquerda). Para abrir da **direita** sem lib extra,
envolve-se **apenas o drawer** em layout RTL:
```kotlin
ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ModalDrawerSheet { /* conteúdo em Ltr novamente aqui dentro */ }
        }
    }
) { /* Scaffold da MainScreen */ }
```
O arraste da borda direita e o gesto de fechar vêm de graça do `ModalNavigationDrawer`.

---

## 3. Máquina de estados / navegação nova

### Conteúdo (implementado)
```
MainScreenState = LOADING | HOME       // sem SECAO: seções são acordeão no drawer
SecaoMenu       = PASSAGEM | VIAGEM | AGENTE | (EMPRESA na fase 2)
```
- `HOME` → `HomeContent` (inalterado). Único conteúdo da MainScreen.
- As seções e suas ações vivem no **drawer** (acordeão): expandir a seção lista os cards de ação,
  que navegam direto (`DadosBotoesMenus.onClick`) e fecham o drawer.
- Removidos `OPERACOES`, `MenuBotoesCategoria`, `MenuPassagem`, `CardBotaoMenu` e o duplo-modelo.

### Ações por seção
| Seção | Cards de ação | Navega para (rota existente / nova) |
|---|---|---|
| Passagem | Pesquisar, Balanço | `formPesquisarPassagem`, `listaRelatorios` |
| Viagem | Nova, Pesquisar | `formViagem/{id}`, `formPesquisarViagem` |
| Agente | Novo, Pesquisar | `formAgente/{id}`, `pesquisarAgente/{id}` *(reativar)* |
| Empresa | Nova, Pesquisar | `formEmpresa/{id}` *(novo)*, pesquisa *(novo/derivar)* |

### Drawer + seleção
- `drawerState = rememberDrawerState(Closed)` (UI). Botão **Menu** e arraste abrem; tocar seção →
  `viewModel.selecionarSecao(x)` + `scope.launch { drawerState.close() }`.
- Botão **Início** → `viewModel.atualizaMainPage(HOME)` + fecha drawer.

---

## 4. Cadastros novos/reaproveitados

### Agente — **reaproveitar** (só reconectar)
Telas/VM/nav já existem e funcionam. Basta:
- Adicionar os cards "Novo agente"/"Pesquisar agente" na seção Agente do menu (reusar os
  `onNavegaParaFormularioNovoAgente`/`…PesquisaAgente` que **já fluem** pelo NavHost, hoje unused).
- (Opcional) simplificar o modelo — ver [Decisões](#8-decisões-a-confirmar).

### Empresa — **novo** (espelhar Viagem)
Trabalho net-new, seguindo o padrão de Viagem:
- `EmpresaRepository.salvar(id: String?, …)` + auto-id (hoje é read-only; `obterPorId` recebe `Int`,
  inconsistente com `id: String` — corrigir).
- `Empresa.toDocumento()` (hoje o seed monta inline).
- `FormEmpresaScreen` + `FormEmpresaViewModel` + `FormEmpresaUiState` + `FormEmpresaHelper` +
  `ContentEmpresaAreaForm` (campos: nome, razaoSocial, cnpj, endereco, telefone1, telefone2).
- `formEmpresaNavComposable` (rota `formEmpresa/{idEmpresa}`) + destino em
  `FluviAppNavComposableDestinations` + wiring no `FluviAppNavHost`.
- Pesquisa de empresa: derivar do padrão de pesquisa existente (decidir se entra na fase 1).

---

## 5. Impacto no código

### Adicionar
- `ui/components/drawer/FluviMenuDrawer.kt` (drawer à direita + itens de seção).
- Empresa: form/VM/state/helper/content/nav (§4) + `EmpresaRepository.salvar` + `toDocumento`.
- `SecaoMenu` (enum) + renderer genérico de seção (evolução de `MenuPassagem`).

### Alterar
- `FluviBottomAppBar.kt` → **reescrever**: 2 itens (Início | Menu), temado (`MaterialTheme`,
  `secondary`/`HeaderNavy`).
- `CommonScaffold.kt` → envolver a MainScreen no `ModalNavigationDrawer`; expor `onClickMenu`/estado
  do drawer. (Avaliar manter o `isShowBottomAppBar` para as telas de form.)
- `MainScreen.kt` / `MainScreenViewModel` / `MainScreenState` → novo modelo de conteúdo
  (`HOME | SECAO`), `selecionarSecao`, remover flags `passagensActive/operacoesActive`.
- `MainScreenNavComposable.kt` → substituir `getListaBotoesMenuOperacoes`/`…Passagens` pela
  montagem por seção; reativar Agente; adicionar Empresa.
- Strings/ícones das seções.

### Remover
- `ui/components/contents/MenuOperacoes.kt`, `ui/components/cards/MenuOperacoesCard.kt`.
- `MainScreenState.OPERACOES`, `model/screendata/MenuBotoesCategoria.kt` (se sem outros usos).
- 3º botão/plumbing "Operações" da bottom bar; renomear o `onClickMenuViagens` enganoso.

---

## 6. Gate por cargo (proposta)
Preservar a semântica atual, mas por seção no drawer (não na bottom bar):
- **Passagem**: todos.
- **Viagem / Agente / Empresa**: só `ADM`/`DIRETOR` (herdando o gate atual de Operações).

Decidir se Agente/Empresa devem ou não ser restritos — ver Decisões.

---

## 7. Faseamento sugerido
1. **Bottom bar + drawer** (Início | Menu; drawer à direita) reaproveitando as seções **existentes**
   (Passagem, Viagem) e **reativando Agente**. Remoção do `OPERACOES`/`MenuOperacoes*`. — entrega o
   grosso da UX e resolve a queixa da bottom bar.
2. **Empresa (cadastro)**: repo `salvar`+`toDocumento`, form/VM/nav, card na seção.
3. **Pesquisa de Empresa** + refinos (gate, ícones, animações do drawer).

---

## 8. Melhorias de estrutura e arquitetura

Oportunidades surgidas na análise (ancoradas no código). Marcadas por **[remodel]** (fazer junto,
custo marginal) / **[oportuno]** (bom momento, opcional) / **[evitar]** (risco de over-engineering).

1. **[remodel] Estado único, sem flags derivadas.** `MainScreenUiState` carrega
   `homeActive/passagensActive/operacoesActive` **além** do `mainScreenState`
   (`MainScreenState.kt` + `MainScreenViewModel.atualizaMainPage`) — duas fontes para a mesma
   verdade. Derivar o "ativo" do próprio estado selado e **remover as flags**. Barato e elimina
   dessincronização.

2. **[remodel] Unificar o modelo de menu + tirar a composição da camada de navegação.**
   Hoje há dois modelos (`DadosBotoesMenus` flat vs `MenuBotoesCategoria` expansível) e os menus
   são montados dentro de `MainScreenNavComposable.kt` (`getListaBotoesMenuOperacoes/…Passagens`),
   misturando **wiring de navegação** com **definição de conteúdo**. Propor: um `SecaoMenu` (enum de
   domínio) + um mapeamento declarativo seção→ações; o nav composable só liga callbacks. Um modelo
   de menu, uma fonte.

3. **[oportuno] Normalizar o contrato dos repositórios espelhados no Firestore.**
   Viagem/Agente/Empresa compartilham o padrão "snapshot listener → Room + auto-id" (ADR-0003), mas
   os contratos divergem: `EmpresaRepository` é read-only e seu `obterPorId(idEmpresa: Int)` briga
   com `Empresa.id: String`; Viagem/Agente têm `salvar`+auto-id. Alinhar num contrato comum
   (`salvar(id: String?, …)`, `obterPorId(id: String)`, `sincronizar`, `toDocumento`) — e, se valer,
   um pequeno `RepositorioEspelhado<T>` base. Necessário de qualquer forma para o cadastro de
   Empresa.

4. **[oportuno] Centralizar autorização (política de Cargo).** O gate hoje é comparação de string
   espalhada (`MainScreenViewModel.atualizaEhDiretorOuAdm`: `cargo == ADM.name || DIRETOR.name`),
   com `cargo` persistido como String solta e um enum `Usuario.Cargo` + `temPermissaoEspecialPassagem()`
   já existentes. Extrair uma função/política única (`PermissoesUsuario`) e alimentar o gate por
   seção do drawer a partir dela. Evita regra de acesso duplicada quando as seções virarem 4.

5. **[oportuno] Refatorar o `CommonScaffold` dirigido por flags.** São ~17 parâmetros, vários
   booleanos de controle (`isMainTopAppBar`, `isShowBottomAppBar`, `isShowMenuViagens`,
   `hasRefresh`…) — control-coupling clássico, e o drawer só vai **aumentar** a lista. Momento certo
   para separar em **scaffold principal** (top bar + bottom bar 2-itens + drawer + refresh) vs
   **scaffold de formulário** (top bar com voltar, sem bottom), ou consolidar num objeto de config.

6. **[evitar por ora] Framework genérico de formulário CRUD.** Viagem/Agente/(Empresa) repetem
   Screen+VM+State+Helper+Content+Nav com a mesma forma "criar/editar por id". Tentador extrair um
   `FormCadastro` genérico, mas o risco de abstração prematura é alto (cada form tem validação e
   campos próprios). Recomendação: **não** abstrair agora; espelhar Viagem para Empresa e reavaliar
   quando houver 4+ forms estáveis.

7. **[remodel] Limpezas de nomeação.** Renomear o enganoso `onClickMenuViagens` (dispara Operações)
   e afins ao introduzir `SecaoMenu`.

## 9. Decisões (resolvidas)
1. **Modelo do Agente**: ✅ **reativar como está** (sem simplificar o modelo agora).
2. **Interação do drawer**: ✅ (revisado) **sub-menus expansíveis (acordeão) dentro do drawer** —
   a seção NÃO troca o conteúdo da MainScreen; expandir mostra as ações (cadastrar/pesquisar) que
   navegam direto. A MainScreen mantém só o Início (viagens). Removidos o estado `SECAO`, o
   `MenuPassagem` e o `CardBotaoMenu`.
3. **Gate por cargo**: ✅ Agente/Empresa **restritos** a ADM/DIRETOR — e **centralizar a estrutura de
   validação** numa política única (item 8.4: `PermissoesUsuario`), eliminando os `cargo == …`
   espalhados.
4. **Empresa**: ✅ **só Cadastrar** na fase 2; pesquisa fica para depois (fase 3).
5. **Navegação**: ✅ o **drawer lembra a última seção**; "Início" volta para HOME.
