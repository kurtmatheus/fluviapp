package dev.matheus.fluviapp.domain.passagem

import dev.matheus.fluviapp.domain.viagem.OcorrenciaViagem

/**
 * **A passagem — um tipo fechado por categoria** ([ADR-0023] D1).
 *
 * Antes disso ela era *uma* coisa com blocos opcionais: 49 campos planos onde o veículo "existia" quando a placa
 * não estava vazia (`ehVeiculo`). Essa forma custava três coisas — estados ilegais representáveis (placa **e**
 * três passageiros de suíte), regra espalhada por tela, e, a mais cara, uma **categoria nova entrando em
 * silêncio**: a carga chegaria como mais um punhado de campos nulos e nenhuma tela saberia que existe.
 *
 * Com um tipo fechado, o **compilador vira a lista de tarefas**: acrescentar `PassagemDeCarga` faz cada `when`
 * exaustivo apontar exatamente os lugares que precisam decidir algo sobre ela. É o oposto do nullable, onde
 * acrescentar é grátis e descobrir é caro. *A estrutura precisa estar pronta para receber a carga, e isso começa
 * no domínio* — a prontidão não é um campo reservado, é o formato.
 *
 * ### O que é comum, e é ele que recebe a carga (D2)
 *
 * Comum é o que descreve **a travessia vendida**: a [ocorrencia] (para onde e quando), os [lancamentos] (quanto
 * entrou), a [observacao] e os [metadados]. Específico é **o que ocupa o espaço**. É esse corte que faz o
 * terceiro sub-domínio caber sem reforma: a carga muda o que ocupa, não o resto.
 *
 * ### Nada aqui é cópia (D8)
 *
 * Não há nome de empresa, de embarcação, de porto, de cliente nem do emissor: **só ids**. Congelar virou decisão
 * da camada de dados, a tomar adiante e apenas se tiver relevância demonstrada — e o que pagou essa inversão foi
 * a imutabilidade que a F7/F8 conquistou: Rota e Viagem **não têm editar**, então o *rename* de que o snapshot
 * protegia praticamente não existe mais no modelo.
 *
 * ### O que este tipo torna impossível
 *
 * Passagem de veículo com três passageiros de suíte; suíte para três com dois clientes; meia numa suíte; carreta
 * exigindo modelo; carimbo de embarque meio-preenchido. Nenhum desses se escreve — e é por isso que a
 * **passagem incompleta** (o atendimento em curso, nota lateral do [ADR-0026]) terá de ser **outro tipo**:
 * admitir nulos aqui para servir ao incompleto desfaria este D1 por dentro.
 */
sealed interface Passagem {
    val id: String

    /** A identidade **exibida**, por ocorrência ([ADR-0018] D10). Distinta do [id], que é o que o QR carrega. */
    val numero: String

    /** `(viagemId, data)` — a travessia concreta, não a viagem semanal (D2). */
    val ocorrencia: OcorrenciaViagem

    /** O que **entrou**, por forma. O total é inferido daqui ([ADR-0024] D4) — não há campo de total. */
    val lancamentos: List<Lancamento>

    val observacao: String?

    val metadados: MetadadosPassagem

    /** A categoria como **valor**, para a fronteira gravar e a tela agrupar (ADR-0024 D1). */
    val categoria: CategoriaPassagem
}

/**
 * A passagem de **pessoa**: um espaço vendido a um ou mais clientes.
 *
 * A [acomodacao] declara o **limite** de ocupação e quais tipos tarifários admite; a lista de [clientes] declara
 * o **fato**. O titular é a **posição 0** — decisão de fronteira que voltou atrás de um campo próprio, porque o
 * array único responde *"em que passagens esta pessoa viajou"* numa consulta só (ADR-0024 D3), e o significado da
 * ordem é deste domínio.
 *
 * ### O par tipo + gratuidade ([ADR-0028] D2)
 *
 * O [tipo] diz *qual redução*; a [gratuidade] diz *por quê*. A F9.1 tinha ficado só com o primeiro, e isso fazia
 * o app gravar **"gratuidade" sem dizer qual** — um rótulo que não serve à fiscalização, e sobre o qual a **cota
 * do ADR-0013 §8** (máximo 2 por categoria) não tem o que contar.
 *
 * Os dois andam juntos: gratuidade sem subtipo e subtipo sem gratuidade são **as duas incoerências**, e as duas
 * aparecem em [pendencias]. Elas não viraram um tipo selado (`Gratuidade(subtipo)`) por uma razão de alcance: o
 * [TipoPassagem] é o eixo que a [Acomodacao] usa para declarar o que admite, e transformá-lo em soma com dado
 * dentro obrigaria a reescrever essa regra — e o codec, e a tabela do ADR-0013 — para ganhar o que a pendência
 * já garante em quatro linhas.
 */
data class PassagemDePassageiro(
    override val id: String = "",
    override val numero: String,
    override val ocorrencia: OcorrenciaViagem,
    override val lancamentos: List<Lancamento>,
    override val observacao: String? = null,
    override val metadados: MetadadosPassagem,
    val acomodacao: Acomodacao,
    val tipo: TipoPassagem,
    /** **Só** quando o [tipo] é `GRATUIDADE` — é ela que a cota conta e a fiscalização confere. */
    val gratuidade: TipoGratuidade? = null,
    /** Ids do pool de clientes, **ordenados**: o primeiro é o titular. */
    val clientes: List<String>,
) : Passagem {
    override val categoria: CategoriaPassagem get() = CategoriaPassagem.PASSAGEIRO

    val titularId: String? get() = clientes.firstOrNull()

    val acompanhantesIds: List<String> get() = clientes.drop(1)

    /**
     * O que **impede** esta passagem de ser coerente. Vazio = coerente.
     *
     * Devolve o que falta em vez de um `Boolean` pela mesma razão de `Veiculo.pendencias()`: quem chama precisa
     * saber **qual** regra falhou para apontar o campo, e um booleano obrigaria a repetir a regra para descobrir.
     */
    fun pendencias(): Set<Pendencia> = buildSet {
        if (clientes.isEmpty()) add(Pendencia.SEM_TITULAR)
        if (clientes.size > acomodacao.ocupacaoMaxima) add(Pendencia.EXCEDE_OCUPACAO)
        if (clientes.distinct().size != clientes.size) add(Pendencia.CLIENTE_REPETIDO)
        if (!acomodacao.admite(tipo)) add(Pendencia.TIPO_NAO_ADMITIDO)
        if (tipo == TipoPassagem.GRATUIDADE && gratuidade == null) add(Pendencia.GRATUIDADE_SEM_SUBTIPO)
        if (tipo != TipoPassagem.GRATUIDADE && gratuidade != null) add(Pendencia.SUBTIPO_SEM_GRATUIDADE)
    }

    val coerente: Boolean get() = pendencias().isEmpty()

    enum class Pendencia {
        SEM_TITULAR,
        EXCEDE_OCUPACAO,
        CLIENTE_REPETIDO,
        TIPO_NAO_ADMITIDO,
        GRATUIDADE_SEM_SUBTIPO,
        SUBTIPO_SEM_GRATUIDADE,
    }
}

/**
 * A passagem de **veículo**: o veículo é o sujeito do próprio bilhete.
 *
 * O [responsavelRetirada] é **opcional por regra de negócio, não por descuido**: ele nem sempre é informado, e
 * costuma ser definido na hora, informalmente, entre despachante, transportadora e quem retira. Bilhete de veículo
 * **sem ninguém nomeado é a forma normal**.
 *
 * Não há tipo tarifário aqui: *meia* e *gratuidade* são categorias de **pessoa**, e o veículo não tem idade nem
 * condição que as justifique.
 */
data class PassagemDeVeiculo(
    override val id: String = "",
    override val numero: String,
    override val ocorrencia: OcorrenciaViagem,
    override val lancamentos: List<Lancamento>,
    override val observacao: String? = null,
    override val metadados: MetadadosPassagem,
    /** Id no pool de veículos, cuja chave natural é a placa ([ADR-0018] D5). */
    val veiculoId: String,
    /** Id no pool de clientes — pode não haver. */
    val responsavelRetirada: String? = null,
) : Passagem {
    override val categoria: CategoriaPassagem get() = CategoriaPassagem.VEICULO

    fun pendencias(): Set<Pendencia> = buildSet {
        if (veiculoId.isBlank()) add(Pendencia.SEM_VEICULO)
    }

    val coerente: Boolean get() = pendencias().isEmpty()

    enum class Pendencia { SEM_VEICULO }
}

// A `PassagemDeCarga` entra aqui quando a carga for planejada (ADR-0023 D9). O que ela vai exigir — unidade
// (peso? volume?), remetente, destinatário — é matéria do ADR dela; o que já está garantido é que **cabe sem
// reforma**: ela declara o que ocupa o espaço e herda os cinco campos comuns.