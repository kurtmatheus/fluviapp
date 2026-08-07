# O painel da empresa — divisão do menu e as fases a partir da F5

> **Status:** fechado → [**ADR-0022**](../adr/0022-painel-da-empresa-e-fases.md) (2026-08-07). Este estudo
> é o caminho; a decisão está lá, e é ela que vale. A pergunta 1 do §6 foi respondida — plataforma e
> `SUPERVISOR` escrevem, `AGENTE` só lê —, e o ADR precisou de um passo a mais que o estudo não tinha:
> como rota e viagem são imutáveis, **escrever quer dizer criar**, e **desativar ficou com a plataforma**.

## 1. O que fechou: o painel da plataforma está completo

A **v0.0.4** (produção, `versionCode` 219) entregou as quatro seções que a plataforma administra, cada uma
ponta a ponta — domínio isolado, camada de dados sem Room, ViewModel no molde do ADR-0006, tela e regra de
servidor publicada:

| Seção | O que ela cadastra | Fase |
|---|---|---|
| **Empresas** | a parte, suas `atuacoes` e a **concessão** de embarcações | F5 (§4, §7.1) |
| **Flotilha** | a embarcação, com tipo obrigatório | F5 (§8) |
| **Localidades** | o par UF + município, com código do IBGE | F4 (§5) |
| **Portos** | o lugar físico, com a localidade por referência | F4 (§5) |

Ou seja: **F4 e F5 estão feitas**, e com elas o primeiro dos dois planos de acesso do ADR-0016 §2 — *quem cria
o universo*. O que sobrou do texto original da F5 (a morte de `Agencia` e de `Funcionario.Lotacao`) não é
cadastro de empresa: é da Equipe, e migra para a fase dela.

O andaime já registra isso: `SECOES_REVITALIZADAS` tem exatamente essas quatro, e o `SECOES_DO_PAINEL` tem
elas mais `VIAGEM`, que ainda é herança do menu antigo.

## 2. A linha entre os dois painéis, e por onde ela passa

O ADR-0016 §2 já dizia o critério, e ele continua sendo o melhor que temos: **a plataforma cria o universo; a
empresa cria a oferta.** O que este estudo acrescenta é a consequência em menu — e a decisão do analista é que
a divisão não é "dois menus disjuntos", e sim **um núcleo compartilhado com duas pontas exclusivas**:

| Seção | Plataforma | Empresa | Por quê |
|---|---|---|---|
| **Início** | sim | sim | é a porta: o que aparece nela é que muda com o contexto |
| **Rotas** | sim | sim | pool **sem dono** (§7.1) — a linha existe para todo mundo |
| **Viagens** | sim | sim | idem: a viagem é `(rota, embarcação, dia, hora)`, compartilhada |
| **Passagens** | não | **sim** | emitir exige **vínculo de funcionário**, não papel (§2) |
| **Equipe** | não | **sim** | o quadro é de uma empresa, e quem o gere é o supervisor dela |

**O compartilhado não é generosidade de UI — é o domínio aparecendo no menu.** Rota e Viagem foram postas na
raiz, sem dono, na 9ª rodada do ADR-0016 (§7.1): duas empresas que vendem a mesma linha usam *a mesma* viagem,
e é isso que faz a ocupação por partida física ser uma conta só. Uma seção compartilhada é a leitura direta
disso. Já Passagem e Equipe têm dono por natureza — a passagem tem emissor, o funcionário tem vínculo —, e é
por isso que são as duas exclusivas.

E há uma simetria que vale registrar, porque é o que mantém o critério honesto: **a plataforma não emite e não
contrata; a empresa não inventa lugar nem cria parte.** Se um dia uma seção parecer pertencer aos dois lados
com donos diferentes, o erro estará no modelo, não no menu.

### O que ainda não está decidido

Compartilhar a *visão* de Rotas e Viagens não diz quem **escreve**. A leitura que este estudo assume, e que
precisa do seu aval antes de virar ADR: a plataforma e o `SUPERVISOR` escrevem; o `AGENTE` só lê (ele vende
sobre o que existe). É a mesma forma da política de hoje — seção × ação —, e não exige eixo novo.

## 3. Ordem do menu ≠ ordem de construção

O menu terminará nesta ordem — dependência primeiro, exclusivas por último:

```
Início · Rotas · Viagens · Passagens · Equipe
```

A construção começa pelo **fim da lista**: a **Equipe** é a primeira entidade do painel da empresa a ser
trabalhada. A razão dada pelo analista é a que importa e não é contraditória — **é a mais fácil de fazer e a
que tem mais regra**. Fácil porque é um cadastro no molde que já se repetiu quatro vezes; densa porque é onde a
política ganha a terceira coordenada:

- `Funcionario` deixa de ter `cargo` no documento e passa a ter **vínculos** `[{empresaId, atuacao, cargo}]`
  (§6), o que faz uma pessoa poder ser agente numa empresa e supervisor em outra;
- `PermissoesUsuario` passa de `(papel, cargo)` para **`(papel, atuação, cargo)`** — e é ela que o menu inteiro
  consulta, incluindo as seções que já existem;
- nasce a **seleção de contexto**: com mais de um vínculo, o app precisa perguntar "trabalhando como quem?", e
  é essa resposta que decide qual painel aparece;
- morrem `Agencia` e `Funcionario.Lotacao`, que sobraram da F5.

Fazer a Equipe primeiro é, portanto, **fazer o eixo antes das telas que dependem dele**. Rota, Viagem e
Passagem todas perguntam "de qual empresa é quem está operando" — construí-las antes seria escrevê-las contra
uma política que ainda vai mudar de forma.

## 4. As fases, redivididas a partir da F5

O plano do ADR-0016 tinha oito fases numeradas por dependência de **domínio**. Com F4 e F5 entregues, o que
resta se reorganiza por **seção do painel da empresa** — que é a unidade em que o trabalho vem sendo feito e
entregue desde a revitalização:

| Fase | Entrega | O que carrega do plano antigo |
|---|---|---|
| ~~F4, F5~~ | **feitas** — painel da plataforma completo | §4, §5, §7.1 (concessão), §8 |
| **F6 — Equipe** | vínculos, cargo por atuação, política com três coordenadas, seleção de contexto, `EscopoAgencia` por empresa; `Agencia` e `Lotacao` morrem | F6 inteira + o resíduo da F5 |
| **F7 — Início** | a casca: o painel que deriva do contexto escolhido, e o estado vazio como cidadão de primeira classe | consequência do §1 (matar o seed), nunca teve fase própria |
| **F8 — Rotas** | `rotas/{id}` na raiz, compartilhadas, imutáveis, unicidade no servidor; a validação de concessão | metade da F7 antiga (§7) |
| **F9 — Viagens** | `viagens/{id}` atômica `(rota, embarcação, dia, hora)`; a lista de negadas na atuação; ocupação | a outra metade da F7 antiga (§7.1) |
| **F10 — Passagens** | emissão sobre a viagem, tarifa inferida (ADR-0013 revisado), e a revitalização do que já existe (ciclo de vida, QR, bilhete) | não estava no plano — era pressuposta |

Duas observações sobre essa tabela, e as duas são consequência de trabalho já feito:

**A F8 antiga ("Regras e suíte") deixa de existir como fase.** Ela já se declarava incremental, e a rc.3 do
Porto cobrou o preço de tratá-la como etapa separada: as regras da coleção `portos` subiram no repositório e
não foram publicadas, o servidor negou o listener e o formulário ficou esperando um snapshot que nunca vinha.
Regra passa a ser **definição de pronto de cada fatia** — a fatia não está entregue enquanto a regra não está
publicada e coberta na suíte de emulador.

**Rota e Viagem viram duas fases, não uma.** Elas eram uma linha só quando "rota" era um cadastro; depois da 9ª
rodada são duas entidades com ciclos diferentes — a rota é a ligação (estável), a viagem é a partida física
(repetida, e é sobre ela que a ocupação é contada). Entregá-las juntas seria uma fatia grande demais para o
ritmo de uma entidade por vez.

## 5. O que muda no código, e onde

Nada disso é reescrita: a estrutura de menu já é domínio testável, e é nela que a divisão entra.

| Arquivo | O que muda |
|---|---|
| `domain/screendata/SecaoMenu.kt` | entra `INICIO`; `VIAGEM` deixa de ser a seção herdada e passa a ser a nova; a ordem do enum vira a ordem do menu acordada |
| `domain/screendata/MenuDaAtuacao.kt` | `SECOES_DO_PAINEL` perde `VIAGEM` como herança e ganha o núcleo compartilhado; `secoesDa(AGENCIAMENTO)` ganha Rotas e Viagens |
| `domain/operacoes/PermissoesUsuario.kt` | a coordenada da atuação, e o `podeAcessar` das seções compartilhadas |
| `domain/screendata/EscopoRevitalizado.kt` | um valor por fatia entregue — e o andaime some quando igualar `SecaoMenu.entries` |
| `firestore.rules` + `firestore-tests/` | por fatia, junto com ela |

## 6. Perguntas para a próxima rodada

1. ~~**Quem escreve Rota e Viagem**~~ — **respondida** em 2026-08-07: plataforma + `SUPERVISOR` escrevem,
   `AGENTE` só lê. Ver ADR-0022 D3, que precisa o alcance de "escrever" (criar sim, editar não existe,
   desativar é da plataforma).
2. **O que o Início mostra em cada painel.** Para a empresa é razoável supor as próximas viagens e a venda do
   dia; para a plataforma, o que exatamente? "Quantas empresas/portos existem" é inventário, não trabalho.
3. **Passagem revitalizada ou adaptada.** Ela é a única seção que já existe em código antigo — e é a maior.
   Vale decidir cedo se a F10 a reescreve no molde ou se a adapta, porque isso muda o tamanho da fase.