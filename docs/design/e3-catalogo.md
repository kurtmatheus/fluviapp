# E3 — Catálogo: o mapa antes de mexer

> ## ⚠️ SUPERADO em `2026-08-03` — o `Catalogo` não nasce
>
> Leia [e2-painel-e-fim-do-catalogo.md](e2-painel-e-fim-do-catalogo.md). **Este documento continua sendo a
> prova**: foi o §1 abaixo que mediu que *"some quase tudo"*. O que mudou é que a conta foi levada até o fim —
> as duas categorias que aqui sobreviveram também têm regra (`DOCUMENTO` decide máscara e teclado em
> `UtilExtensions.kt:10-20`, e máscara é tratamento de dado pessoal; `PAGAMENTO` é do lançamento), e com elas
> caem `TIPO_EMBARCACAO` e o `Catalogo` embutido na `Localidade`. **Não sobra linha**, então a coleção não
> existe. A frente E3 troca de sujeito: a primeira seção depois do Painel Principal é **Empresa**.
>
> Onde este doc diz *"fica no catálogo"* (§1), *"o `Catalogo` que nasce na E3"* (§1) ou *"criar
> `domain/catalogo/Catalogo.kt`"* (§5, passo 2), leia **vira tipo de domínio**. O §2
> (`IObjetoSimplificado`), o §3 (contador de bilhete) e o §4 (a superfície de `Constante`) seguem válidos —
> o §2 com o desfecho já resolvido: sem catálogo, a interface não fica com ninguém.

**Status:** Passo 1 (**mapear**) da frente **E3** do [roadmap](mvp-roadmap.md), feito em `2026-08-02`. Este
documento é o rastreamento pedido pelo analista: **o que ficou como domínio e o que resta de `Catalogo`**,
mais o destino dos dois resíduos que ele apontou — `IObjetoSimplificado` e o contador de bilhete.

> Decisões-fonte: [ADR-0016](../adr/0016-dominio-da-plataforma.md) §3 (`Constante` → `Catalogo`),
> [ADR-0017](../adr/0017-eixo-de-storage-firestore-only.md) (F1: o piloto é esta coleção),
> [ADR-0018](../adr/0018-agregado-passagem-participantes-modo-e-lancamentos.md) (modo, classe, numeração) e
> [ADR-0019](../adr/0019-camada-de-dados-dinamica-e-dto-por-caso-de-uso.md) (`Map` na fronteira, DTO por
> caso de uso).

---

## 1. O achado principal: o catálogo já é quase todo domínio

`Constante(id, descricaoNome, categoria)` tem **9 categorias** e um enum `Descricao` com **31 valores**. Mas
a maior parte dessas categorias **já virou tipo fechado no domínio** — e as duas verdades convivem hoje:

| Categoria | O que é hoje | Destino | Por quê |
|---|---|---|---|
| `STATUS_PASSAGEM` | linha de catálogo **e** `StatusPassagem` | **morre do catálogo** | tipo desde o ADR-0012; sobrevive só como opção de dropdown de filtro |
| `TIPO_PASSAGEM` | linha **e** `TipoPassagem` | **morre do catálogo** | tipo desde o ADR-0013 |
| `GRATUIDADE` | linha **e** `TipoGratuidade` | **morre do catálogo** | tipo desde o ADR-0013 |
| `ACOMODACAO` | linha (`REDE`/`SUITE`/`CAMAROTE`) | **vira `ModoPassagem`** | ADR-0018 D6 — eixo de quatro valores exclusivos |
| `CATEGORIA_PASSAGEM` | linha (`VEICULO`/`PASSAGEIRO`) | **morre** | é o **modo** embrionário, com dois valores em vez de quatro (ADR-0018 §11.3) |
| `VEICULO` | linha (`CARRO`/`MOTO`/`CAMINHAO`/`CARRETA`) | **vira `ClasseVeiculo`** | ADR-0018 D7 — a classe governa exigências e tarifa |
| `MUNICIPIO` | linha | **vira `Localidade`** (entidade) | ADR-0016 §5 — UF + município, com `codigoIbge` |
| `DOCUMENTO` | linha (`CPF`/`CNPJ`/`RG`/`CNH`/`PASSAPORTE`) | **fica no catálogo** | é rótulo puro, sem regra — e cresce por país/negócio |
| `PAGAMENTO` | linha (`PIX`/`DINHEIRO`/`DEBITO`/`CREDITO`) | **a decidir** | com lançamentos `{id, forma, valor}` (ADR-0018 D11), `forma` quer ser tipo; mas meio de pagamento novo é fato de mercado, não de código |

**Some quase tudo.** Sobram `DOCUMENTO`, possivelmente `PAGAMENTO`, e o que o ADR-0016 §8 acrescenta:
`TIPO_EMBARCACAO` (F/B, Navio, Lancha) — a **exceção nomeada**, que é catálogo *e* tem capacidade em código.

> **Isto valida a decisão do ADR-0016 §3 pelo avesso:** `Constante` não era "a tabela do catálogo", era **a
> tabela de tudo que era lista** — e a maior parte do que estava lá tinha regra, logo era domínio disfarçado
> de dado. O `Catalogo` que nasce na E3 é bem menor que a `Constante` que morre.

## 2. `IObjetoSimplificado` — resíduo da era REST

```kotlin
interface IObjetoSimplificado { val id: String; val descricaoNome: String }
```

**Confirmado o diagnóstico do analista:** ele *"condensaria uma estrutura de catálogos vinda por
Retrofit/API, que por sua vez traria de um banco SQL estruturado"*. Esse mundo não existe — **não há
Retrofit no projeto** (`grep retrofit`: zero). O que sobrou é uma interface guarda-chuva sobre três coisas
que não são da mesma natureza: `Constante`, `Funcionario` e `Navio` (ADR-0016 §3 já apontava).

O que ela carrega junto, e o estado real de uso:

| Extensão | Usos | Observação |
|---|---|---|
| `mapDescricao()` | **15** | é o que a mantém viva: alimenta os dropdowns (`listaItens = …mapDescricao()`) |
| `extrairPorDescricao()` | **2** | `ViagemDadosViagemMapper` acha município **por nome** — o casamento por nome que o ADR-0008 mata |
| `extrairPorId()` | **0** | código morto |

**Duas armadilhas nas extensões:** as duas `extrair*` usam `first { }`, que **lança exceção** quando não
encontra — não devolvem `null`. E `extrairPorDescricao` é relação por nome, que a `Localidade` (§1) elimina
por construção.

**Destino:** o `Catalogo` fica com a interface (ADR-0016 §3) **ou** ela desaparece — decisão do passo 2.
`Funcionario` e `Navio` param de implementá-la: os dois têm identidade e nome próprios, e `descricaoNome`
para uma pessoa é justamente o tipo de nome que o ADR-0015 §8 corrigiu.

## 3. Contador de bilhete — defasado por dois lados

`ContadorBilhete(id = 1, contagem)` existe em **cinco lugares**: entidade Room, `ContadorDao`, tabela no
`DDL_V2`, `ContadorDocumento(numeroBilhete)` e um `Flow` gerenciado no `PassagemFirestoreRepository`.

Ele está defasado por dois ADRs ao mesmo tempo:

- **ADR-0018 D10** — a numeração passa a ser **por ocorrência** (`(viagemId, data)`), com **incremento
  atômico**. Um contador global único não sobrevive à plataforma multi-empresa: todas dividiriam a mesma
  sequência e disputariam o mesmo documento.
- **ADR-0017** — sem Room, a entidade e o DAO somem de qualquer forma.

Vizinho a ele, e no mesmo saco: **`ViagemDao.obterContagem()` não tem chamador** — código morto que o
ADR-0017 já registrou.

**Destino:** não é trabalho da E3. Fica anotado para a fase da numeração (ADR-0018 F5), **e sai naquela
fase** — pela regra de descarte progressivo do roadmap.

## 4. A superfície que a E3 vai tocar

O que existe hoje em torno de `Constante`:

| Peça | Onde |
|---|---|
| Entidade | `domain/cadastro/constantes/Constante.kt` (`@Entity`, implementa `IObjetoSimplificado`) |
| Porta | `services/repository/cadastro/ConstanteRepository.kt` — `sincronizar()`, `obterTodosPorCategoria()`, `obterTodas()`; coleção **`constants`** |
| Impl | `ConstanteFirestoreRepository` (listener + espelho no DAO) |
| Room | `ConstanteDao` + tabela no `DatabaseModule` |
| DTO | `ConstanteDocumento(descricao, categoria)` + `toConstanteDocumento()` no `DocumentoBrutoMappers` |
| Consumo | **16 chamadas** de `obterTodosPorCategoria` em 11 ViewModels/helpers · `obterTodas()`: **0 chamadas** |
| Seed | `SeedFirestore` popula a coleção — e o seed morre (ADR-0016 §1) |

**Nota de nomenclatura, já resolvida:** no documento o campo **já se chama `descricao`**
(`ConstanteDocumento.kt:6`); `descricaoNome` só existe do lado Kotlin. Sem Room, o nome do modelo acompanha o
do documento sem negociar com schema nenhum (ADR-0017).

## 5. Os passos seguintes (definidos pelo analista)

1. ~~**Mapear**~~ — este documento.
2. **Criar `domain/catalogo/Catalogo.kt`** — pacote e entidade novos, já sem Room e sem `@Entity`.
3. **Revisar, reaproveitar ou criar a camada de dados** — fronteira em `Map` (ADR-0019), coleção nova
   `catalogo` (regenera, não migra: o dado vem do seed, que morre).
4. **Painel da Plataforma com o menu só da seção Catálogo, acessível somente ao `ADM`** — a primeira vez que
   `ADM` e `GESTOR` se separam (ADR-0017 §7.1).
5. **Revisar as regras do servidor** para cobrir **só o que foi desenvolvido**, evoluindo a cada etapa.
6. **Suíte verde → teste Android observável** — o primeiro instrumentado em muito tempo.
7. **CI/CD sobre essa versão limpa**, com protótipo, teste, observabilidade e deploy integrados.

> O passo 6 é o que valida o **processo**, não só a tela: se uma fatia pequena passa por domínio → dados →
> lógica → apresentação → regra → teste observável, as próximas telas seguem o mesmo trilho.