# ADR-0023: A Passagem por categoria — sub-domínios, tudo por referência, e a porta aberta para a Carga

**Status:** Aceita em direção (decisões do analista em 2026-08-11) · sem código

**Estudos que prepararam:** [`docs/design/f9-passagens-terreno.md`](../design/f9-passagens-terreno.md) (o
terreno medido) e [`docs/design/dominio-passagem.md`](../design/dominio-passagem.md) (o agregado em detalhe)

---

## Contexto

A F9 é a última seção da revitalização, e o terreno dela foi medido em 2026-08-11: **7.791 linhas em 75
arquivos** que ninguém alcança, **156 testes congelados que passam** contra um modelo que mudou por baixo
deles, e uma `Passagem` que é **`@Entity` do Room dentro de `domain/`** com **49 campos planos** — a última
entidade viva que mistura domínio e persistência.

Três decisões anteriores chegam aqui pedindo forma:

1. o [ADR-0018](0018-agregado-passagem-participantes-modo-e-lancamentos.md) desenhou o agregado novo
   (participantes com identidade, modo tipado, lançamentos) e um plano de oito passos, mas **nenhum deles
   tocou a raiz**: a passagem continuou sendo *uma* coisa com campos opcionais;
2. a [F8](0022-painel-da-empresa-e-fases.md) entregou a **Viagem como partida física** e a `ViagemSemana`
   como ocorrência calculada — a passagem passou a ter para onde apontar;
3. **preço é I/O** (decisão do analista, 2026-08-11, registrada no índice): a emissão não calcula valor, e a
   inferência é eixo de análise sobre o agregado. Com isso caiu o último cálculo que a emissão fazia.

O que faltava era o **eixo**: hoje "esta passagem é de veículo" é uma dedução sobre a presença de um campo
(`ehVeiculo = !placaVeiculo.isNullOrEmpty()`), e um terceiro caso — **carga** — não tem por onde entrar.

## Decisão

### D1 — A categoria é a raiz do agregado, e os sub-domínios são um tipo fechado

A Passagem deixa de ser uma estrutura com blocos opcionais e passa a ser **um tipo fechado por categoria**:
hoje **Passageiro** e **Veículo**; amanhã **Carga**.

```kotlin
sealed interface Passagem {
    val id: String
    val ocorrencia: OcorrenciaViagem      // para onde vai (D2)
    val lancamento: Lancamento            // quanto entrou (D6)
    val observacao: String?
    val metadados: MetadadosPassagem      // quem, quando, em que estado (D7)
}

data class PassagemDePassageiro(
    …,
    val acomodacao: Acomodacao,           // Rede | Suíte | Camarote
    val tipo: TipoTarifario,              // Inteira | Meia | Gratuidade
    val clientes: List<String>,           // 1..3 ids; o titular é o primeiro
) : Passagem

data class PassagemDeVeiculo(
    …,
    val veiculo: Veiculo,                 // tipo, modelo, placa, cor, cilindrada
    val responsavelRetirada: String?,     // clienteId — pode não haver (D4)
) : Passagem
```

**Por que tipo fechado, e não campos opcionais.** A forma de hoje é a segunda: um registro onde o veículo
"existe" quando a placa não está vazia. Ela custa três coisas — estados ilegais representáveis (passagem com
placa **e** três passageiros de suíte), regra espalhada (cada tela decide o que exigir) e, o mais caro, a
**categoria nova entra em silêncio**: a Carga chegaria como mais um punhado de campos nulos, e nenhuma tela
saberia que existe.

Com um tipo fechado, o **compilador vira a lista de tarefas**: adicionar `PassagemDeCarga` faz cada `when`
exaustivo apontar exatamente os lugares que precisam decidir algo sobre ela. É o oposto do nullable, onde
adicionar é grátis e descobrir é caro. Esse é o preço aceito e a razão de aceitá-lo: *a estrutura precisa
estar pronta para receber a carga, e isso começa no domínio* — a prontidão não é um campo reservado, é o
formato que obriga quem lê a considerar o caso.

### D2 — O comum é a travessia vendida; a categoria diz o que ocupa o espaço

O que é **comum** aos três sub-domínios: a **ocorrência** (para onde e quando), o **lançamento** (quanto), a
**observação** e os **metadados**. O que é **específico**: quem ou o que viaja, e sob que regra de espaço.

Esse é o critério de colocação, e é ele que responde de antemão onde a Carga se encaixa: ela é *uma
travessia vendida* como as outras — muda o que ocupa o espaço (peso, volume, remetente), não o resto.

**A ocorrência, não a viagem.** A `Viagem` é uma saída **semanal** ("terça às 18h"); a travessia concreta é
`(viagemId, data)` — a `ViagemSemana` da F8.4. O agregado aponta para a **ocorrência**, e é ela que dá
sentido à numeração, à ocupação e ao balanço. *(Ver o §"o que não decide": a forma exata desse ponteiro é
pressuposto meu, não decisão registrada.)*

### D3 — Passageiro: a acomodação é o espaço; o tipo tarifário é a regra sobre ele

**Acomodação**: `REDE`, `SUITE`, `CAMAROTE`. Suíte e camarote são vendidos para **uma, duas ou três**
pessoas — e essa ocupação **não é um campo à parte**: ela é a quantidade de clientes do bilhete. Um campo ao
lado da lista poderia discordar dela, e "suíte para três com dois clientes" deixa de ser representável se a
lista *é* a verdade. A acomodação declara o **limite**; a lista declara o **fato**.

**Tipo tarifário**: `INTEIRA`, `MEIA`, `GRATUIDADE` — e ele não é livre. Suíte e camarote são sempre
**inteira**; meia e gratuidade existem **só na rede**. A regra passa a morar na acomodação (que tipos ela
admite), e não espalhada na tela: é o mesmo movimento que o D4 faz no veículo.

**Clientes 1..3, o titular é o primeiro.** Não são "passageiro1, passageiro2, passageiro3" com quatro campos
cada — é **uma lista de referências**, ordenada, onde a posição 1 é o titular e é obrigatória. Some com isso
a assimetria que o terreno encontrou (`documentoPassageiro1` × `tipoDocumentoPassageiro3`: o mesmo fato com
dois nomes).

### D4 — Veículo: o tipo governa o que se exige

O veículo tem **tipo, modelo, placa, cor** e **cilindrada**, e o que muda entre os tipos não é opinião de
formulário — é o tipo:

| Tipo | Modelo | Cilindrada | Por quê |
|---|---|---|---|
| `CARRETA`, `CAMINHAO` | **não se pede** | não | o tipo **já é** o modelo |
| `MOTO` | se pede | **obrigatória** | a cilindrada é o que distingue uma moto de outra na travessia |
| `CARRO`, `VAN`, `SUV`, … | se pede | não | têm modelo nomeado |

Isso **corrige no domínio** a primeira divergência do ADR-0018 D19: hoje `modeloVeiculo` é exigido **sempre**
(`ValidacaoVeiculo.kt:50`), de modo que carreta e caminhão não passam sem modelo. A validação deixa de ter
regra própria e passa a **perguntar ao tipo** — quem sabe se exige modelo é o tipo, não o validador.

**O responsável pela retirada é opcional**, e isso é do negócio, não tolerância: existe veículo embarcado sem
ninguém nomeado para retirá-lo. Quando existe, é um **`Cliente` referenciado** — a mesma entidade do D5.

### D5 — O `Cliente` é entidade referenciada, e ganha telefone

O `Cliente` é **entidade completa, com cadastro próprio**, referenciada pela passagem por id — nos dois
sub-domínios (passageiros 1..3; responsável pela retirada). Ele confirma o pool que o ADR-0018 D2/D3 desenhou
e acrescenta um campo novo: **telefone**.

O telefone não é decorativo: é o primeiro dado do cliente que existe **para contato**, e não para identificar
quem viaja — o que o coloca junto do documento na conversa de LGPD (ADR-0020 D2) quando a coleção nascer.

### D6 — O lançamento do pagamento: formas e valores

O pagamento é **lançamento** — formas e valores —, e não quatro colunas fixas
(`valorPix`/`valorDinheiro`/`valorDebito`/`valorCredito`). Confirma o ADR-0018 D11 e casa com a decisão de
hoje sobre o preço: **o valor entra**; ninguém o calcula na emissão.

### D7 — Metadados: o sistema anota, e só o status aparece

`status` · `funcionarioId` · `agenciaId` · `criadoEm` · `alteradoEm` · `embarcadaPorId` · `embarcadaEm`.

Duas qualificações do analista, e as duas são de projeto:

- **`funcionarioId` e `agenciaId` são inferidos** — nunca digitados. Saem do vínculo ativo de quem emite
  (ADR-0015 P2.3, ADR-0016 §6). Um campo de agência na tela seria a chance de ele discordar de quem está
  logado;
- **só o `status` é visualizado em tela.** O resto é anotação do sistema: existe para auditar, não para
  compor a tela. Isso mantém o `StatusPassagem` (ADR-0012) como o único metadado com semântica de negócio, e
  é o que justifica ele ser tipo com FSM enquanto os outros são registro.

### D8 — Nada é congelado no domínio: tudo por referência

**O domínio não guarda cópia de nada.** Nem nome de empresa, nem de embarcação, nem origem e destino, nem
data e hora da viagem, nem o nome do emissor, nem o do cliente. Onde havia par *id + valor congelado*, fica
**só o id**.

Congelar é decisão da **camada de dados**, a ser tomada adiante e **somente se tiver relevância
demonstrada** — porque, em princípio, tudo pode ser por referência.

**O que paga essa decisão** (e é a razão de ela ser mais que preferência): o motivo original de congelar era
*o bilhete não pode mudar quando a viagem mudar* — e, desde a F7/F8, **a viagem não muda**. Rota e Viagem são
**imutáveis por desenho**: não têm editar, só criar e desativar (ADR-0022 D3). Localidade e Porto têm delete
**lógico**. A mutação que o snapshot protegia praticamente não existe mais no modelo, e proteger-se dela
custava 12 campos duplicados e a chance permanente de o par discordar de si mesmo.

**O que isso supera, ponto a ponto:**

| Onde estava | O que dizia | O que vale |
|---|---|---|
| [ADR-0008](0008-relacionamentos-por-identidade.md) | "`Passagem` = snapshot + ids" — id para relacionar, **valor para lembrar** | a régua geral segue válida como **padrão de dados**; o **agregado Passagem sai da exceção**: no domínio, só id |
| [ADR-0018](0018-agregado-passagem-participantes-modo-e-lancamentos.md) D1 | participantes com **chave + valores congelados no mesmo lugar** | **só a chave** |
| ADR-0018 D13 | `agenciaId` **+ o nome como snapshot** | só `agenciaId` (D7) |
| ADR-0018 D14 | carimbo de embarque como sub-objeto com `embarcadaPor` (nome) | o carimbo fica, o **nome sai**: `embarcadaPorId` + `embarcadaEm` |
| ADR-0018 D6 | **`ModoPassagem`**, um eixo de quatro valores (rede/suíte/camarote/**veículo**) | **dissolvido em dois níveis**: `Categoria` (Passageiro \| Veículo \| Carga) × `Acomodacao` (Rede \| Suíte \| Camarote). Veículo não é uma acomodação — é outro sub-domínio |
| [ADR-0013](0013-tabela-de-tarifa-e-tipo-tarifario.md) | `tarifaBase` congelada na emissão | já sem fonte desde o §7.2; agora **sem portador** — o que se registra é o valor praticado (D6) |

**O que isso custa, e não vou esconder:** um bilhete de referência é uma **leitura**, não um documento. Se um
dia um porto for renomeado, o bilhete antigo mostra o nome novo — e a resposta certa para isso não é
recongelar por reflexo, é decidir na camada de dados, com o caso concreto na mão. O segundo custo é de
consulta: ocupação e balanço deixam de poder agregar por campo congelado e passam a exigir junção; o remédio
está no eixo que já existe para isso, o **DTO por caso de uso** ([ADR-0019](0019-camada-de-dados-dinamica-e-dto-por-caso-de-uso.md)).

### D9 — E a Carga, quando vier, não abre este ADR de novo

A prontidão para a carga é o **D1 + D2**: um sub-domínio novo declara o que ocupa o espaço e herda ocorrência,
lançamento, observação e metadados. O que ela vai exigir de decisão própria — unidade (peso? volume?),
remetente e destinatário, se há responsável pela retirada como no veículo — é matéria do ADR dela. Aqui só
fica garantido que **cabe sem reforma**.

## Consequências

**O que se ganha**

- **Estados ilegais deixam de ser representáveis**: passagem com placa e três passageiros de suíte, suíte para
  três com dois clientes, carreta sem modelo, meia numa suíte — nenhum desses se escreve.
- **A regra sai da tela e vai para o tipo** (acomodação diz que tipos admite; tipo de veículo diz o que
  exige). O formulário volta a ser formulário.
- **O terceiro sub-domínio tem porta**, e a porta é verificada pelo compilador.
- **12 campos duplicados somem** com o snapshot, e com eles a assimetria de nomes que o terreno achou.
- A **correção do D19** deixa de ser um remendo na validação e passa a ser propriedade do tipo.

**O que se paga**

- **O bilhete passa a ser leitura** (D8) — com o risco de exibição que isso traz, aceito e adiado para a
  camada de dados.
- **Junção onde havia campo**: ocupação, balanço e listagem precisam de leitura composta.
- **Os 156 testes congelados** cobrem a forma antiga; parte deles se reescreve, e parte se apaga. É trabalho
  que a fase paga uma vez.
- **A migração de forma é grande** — e é por isso que a ordem técnica do estudo da F9 continua valendo: a
  `Passagem` sai do Room **antes** de a forma mudar, porque enquanto ela for tabela cada campo é DDL.

## O que este ADR não decide

- **A camada de dados**: como o agregado se serializa (um documento com discriminador de categoria? uma
  coleção por categoria?), o que se congela **se** algo se congelar, e os DTOs por caso de uso. É o passo
  seguinte, na ordem que o analista fixou: **domínio → fronteira com dados → camada de dados**.
- **O corte e a ordem da F9** — as perguntas 2 e 3 do estudo do terreno seguem abertas de propósito, e este
  ADR é o insumo delas.
- **A Carga** (D9).
- **O método da inferência tarifária** — segue no módulo faturamento; o que se decidiu hoje foi o **lugar**
  (análise sobre o agregado), não o método.

### Quatro pontos que assumi para o agregado fechar, e que preciso confirmar

Estão marcados porque **não foram decididos** — são leituras minhas, e cada uma muda código:

1. **O ponteiro é a ocorrência, não a viagem.** O modelo dado diz `viagemId`; a `Viagem` é semanal, então sem
   a **data** dois bilhetes de terças diferentes ficam indistinguíveis, e numeração, ocupação e balanço perdem
   o eixo. Assumi `(viagemId, data)` — a `ViagemSemana` da F8.4.
2. **O veículo é objeto do agregado, não entidade referenciada.** O `Cliente` foi declarado entidade com
   cadastro; o veículo foi descrito por atributos. Assumi que ele vive **dentro** da passagem, com a **placa
   como chave natural** — o que o deixa promovível a pool (ADR-0018 D5) quando o reaproveitamento justificar.
3. **O número do bilhete continua existindo.** Ele não aparece na lista de metadados. Assumi que segue como
   identidade **exibida** (e por ocorrência, ADR-0018 D10), distinta do `id` do documento — que é o que o QR
   carrega.
4. **Na rede, um cliente por bilhete.** A lista de 1..3 aparece na descrição do sub-domínio Passageiro como um
   todo; assumi que o intervalo é de **suíte e camarote**, e que rede é um por bilhete — que é o que a
   contagem de ocupação faz hoje. Se a rede admitir três, um bilhete passa a ocupar três redes, e a contagem
   muda.

## Referências

- [ADR-0018](0018-agregado-passagem-participantes-modo-e-lancamentos.md) — o agregado que este ADR reforma na
  raiz; D2/D3/D5/D11/D17/D18 seguem, D1/D6/D13/D14 mudam de forma
- [ADR-0008](0008-relacionamentos-por-identidade.md) — id × valor; a Passagem deixa de ser o exemplo do valor
- [ADR-0016](0016-dominio-da-plataforma.md) §7.1 e §7.2 — a viagem sem dono e a tarifa não cadastrada
- [ADR-0019](0019-camada-de-dados-dinamica-e-dto-por-caso-de-uso.md) — onde o custo de leitura do D8 é pago
- [`docs/design/f9-passagens-terreno.md`](../design/f9-passagens-terreno.md) — o terreno medido, e o
  faseamento que este ADR passa a alimentar