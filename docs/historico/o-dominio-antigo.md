# O domínio antigo — o catálogo, a relação por nome e a data

> **Documento de histórico.** Não tem autoridade sobre nada: sintetiza **três estudos superados**, e existe
> para preservar o *achado* de cada um sem manter em circulação o texto que descrevia um app que não existe
> mais. Onde ele e um ADR discordarem, o ADR vence — e onde ele e o
> [catálogo do domínio](../design/dominio-da-plataforma.md) discordarem, o catálogo vence.
>
> **Absorve, e substitui:**
>
> | Estudo | Era | Superado por |
> |---|---|---|
> | `dominio-relacionamentos-e-camadas.md` | 2026-07 | [ADR-0008](../adr/0008-relacionamentos-por-identidade.md), depois [ADR-0016](../adr/0016-dominio-da-plataforma.md) |
> | `viagem-vs-trecho.md` | 2026-07-28 | [ADR-0016](../adr/0016-dominio-da-plataforma.md) §7.1 |
> | `e3-catalogo.md` | 2026-08-02 | [ADR-0020](../adr/0020-fim-do-catalogo-e-o-contexto-do-painel.md) D1 |
>
> O texto integral dos três está no histórico do git, até o commit que os removeu (2026-09-03).

---

## 1. A relação por nome — o achado que gerou o ADR-0008

O primeiro estudo do projeto mediu nove entidades e desenhou os relacionamentos. O achado central cabe numa
frase, e ela é a razão de o ADR-0008 existir:

> **Todo relacionamento vivo era feito por NOME, não por id.**

Concretamente: `Navio.empresa` guardava o **nome** da empresa, porque o dropdown fazia
`listaEmpresas.map { it.nome }` — o que se selecionava e o que se persistia era o rótulo. `Viagem.empresa` e
`Viagem.navio`, idem. A `Passagem` copiava `empresa`, `navio`, `origem`, `destino`, `codigoViagem`, `agente`
e `agencia` como strings soltas.

**O que sobrevive disso:** a régua *id para relacionar, valor para lembrar*, hoje no ADR-0008 e emendada
pelo ADR-0023 D8 (na Passagem há **só referência**; congelar virou decisão da camada de dados).

**O que morreu junto com o texto:** o vocabulário inteiro. `Agente` virou `Funcionario` (ADR-0015);
passageiro e veículo eram tratados como *value objects* e viraram **participantes de mesmo nível**
(ADR-0023); e as "nove entidades" não tinham Porto, Rota, Atuação nem Localidade.

## 2. A data era Arquitetura da Informação, não bug

A segunda nota partiu de um incômodo de operação — *"qual terça?"* — e chegou a um diagnóstico que não era
técnico: a `Viagem` de então descrevia **um trajeto**, não **uma travessia**, e por isso a data era digitada
na emissão em vez de vir da saída escolhida.

A nota propunha dois conceitos: *Trecho* (com tarifas dentro) e *Viagem = Trecho + data*. **O desenho
vigente tem três, e nenhum se chama Trecho:**

| Hoje | O que é |
|---|---|
| **Rota** | o **onde** — dois portos, distância, tempo médio. Sem tarifa, sem embarcação, sem agenda |
| **Viagem** | o **quando e em quê** — `(rota, embarcação, diaSemana, hora)`, atômica |
| **Ocorrência** | a travessia concreta — `(viagemId, data)`, calculada, sem coleção |

Três correções que a execução impôs à nota, e que valem como aviso de leitura para qualquer documento de
julho:

1. a **agenda não ficou na rota** — `diaSemana` e `hora` são da Viagem, e a embarcação também;
2. a **tarifa saiu de cena** — rota e viagem são compartilhadas, e *uma entidade sem dono não tem de quem
   ter tarifa* (ADR-0016 §7.1); o preço passou a ser **inferido** dos valores praticados, e depois virou I/O;
3. a previsão de que "várias agências operam sobre os mesmos trechos" **se confirmou e foi além**: elas
   compartilham **a mesma viagem**, e é isso que faz a ocupação parar de se fragmentar.

## 3. O catálogo já era quase todo domínio

O terceiro estudo foi mandado mapear o que sobraria da tabela `Constante` — nove categorias, trinta e um
valores — quando ela virasse a coleção `Catalogo`. A medição respondeu outra coisa:

> `Constante` não era "a tabela do catálogo": era **a tabela de tudo que era lista** — e a maior parte do
> que estava lá tinha regra, logo era **domínio disfarçado de dado**.

Sete das nove categorias já eram, ou viraram, tipo fechado. O estudo achou que duas sobreviveriam —
`DOCUMENTO`, por parecer "rótulo puro", e `PAGAMENTO`, porque *"meio de pagamento novo é fato de mercado,
não de código"*. **As duas caíram** semanas depois, e por argumentos que o próprio estudo não tinha: a
máscara do documento é **tratamento de dado pessoal** (LGPD, não organização de código), e o PIX provou que
forma de pagamento nova chega trazendo QR, conciliação e liquidação — tudo código.

**Não sobrou linha.** A coleção não nasceu, e a régua que substituiu a régua é hoje a mais citada do
projeto: *vocabulário que o código consome é tipo; dado que o negócio cria é entidade; não há terceira
categoria.*

### Dois resíduos que o estudo catalogou, e o destino de cada um

- **`IObjetoSimplificado`** — interface guarda-chuva `{id, descricaoNome}` sobre três coisas de naturezas
  diferentes (`Constante`, `Funcionario`, `Navio`). Era resíduo da era REST — *"condensaria catálogos vindos
  por Retrofit"* —, e não havia Retrofit no projeto. Sem catálogo, **a interface ficou sem ninguém** e saiu.
  Duas armadilhas que ela carregava, e que valem como lição: `extrairPorDescricao` casava **por nome** (o
  que o ADR-0008 mata) e as duas extensões usavam `first { }`, que **lança exceção** em vez de devolver
  `null`.
- **O contador de bilhete global** (`id = 1`, em cinco lugares). Defasado por dois ADRs ao mesmo tempo: a
  numeração passou a ser **por ocorrência** com incremento atômico (ADR-0018 D10), e sem Room a entidade e o
  DAO sumiriam de qualquer forma (ADR-0017). Hoje é subcoleção `viagens/{id}/ocorrencias/{data}`.

## 4. O que os três ensinam juntos

**Os três achados eram sobre a mesma coisa vista de ângulos diferentes: o app guardava *rótulos* onde devia
guardar *identidade*.** O nome da empresa dentro do navio, o par de cidades digitado dentro da viagem, e o
vocabulário inteiro numa tabela editável são a mesma decisão repetida três vezes — e as três correções
(ADR-0008, 0016 §7.1 e 0020 D1) são a mesma correção aplicada em três camadas.

Vale reter também **como** os três caíram: nenhum foi derrubado por um documento melhor. O primeiro caiu ao
medir o código, o segundo ao construir a tela, e o terceiro ao levar a própria régua até o fim — *"três
exceções numa decisão só é o modelo avisando que a categoria não fecha"*.
