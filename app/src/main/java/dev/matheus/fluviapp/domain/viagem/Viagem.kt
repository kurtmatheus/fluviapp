package dev.matheus.fluviapp.domain.viagem

import dev.matheus.fluviapp.domain.rota.Rota
import java.time.DayOfWeek
import kotlin.math.roundToInt

/**
 * **A partida física** (ADR-0016 §7.1, ADR-0022 F8) — o *quando* e o *em quê*, sobre uma [Rota].
 *
 * Uma saída = um documento. Ela **não é uma rota com uma agenda dentro**: `diaSemana` e `hora` andam
 * juntos porque juntos é que significam alguma coisa, e "terça e sexta às 18h" são **duas** viagens. A
 * ocorrência concreta é `(viagemId, data)`, calculada — não persistida.
 *
 * É esta entidade que faz a ocupação ser uma conta só. Duas agências que vendem o mesmo navio na mesma
 * saída não têm duas viagens: têm **a mesma**, e `count(passagens where viagemId = X and data = D)`
 * atravessa empresas sem *collection group*. Fragmentá-la em uma viagem por agência seria perder isso.
 *
 * ### Imutável, como a Rota, e pela mesma razão elevada
 *
 * Não se edita e não se apaga — **cria-se outra e inativa-se esta**. Aqui o argumento é mais forte do que
 * na Rota, porque a viagem é o que a passagem aponta: reescrever o horário mudaria, retroativamente, a
 * hora impressa em bilhetes já emitidos por terceiros. Passagem antiga apontando viagem inativa é o
 * comportamento correto, e da mesma natureza do snapshot (ADR-0008).
 *
 * ### O dia é invariante, não campo opcional
 *
 * [diaSemana] é `DayOfWeek` **não-nulo**, pelo precedente de `Embarcacao.tipo`: não existe partida sem
 * dia. Documento sem dia conhecido é **recusado** na fronteira — some da lista em vez de derrubar a
 * coleção. `java.time` é nativo aqui (minSdk 26), então o tipo não custa dependência nenhuma.
 *
 * [horaMin] é minutos desde a meia-noite (ver [formatarHora]) — a hora é o único horário do app sobre o
 * qual se faz conta.
 */
data class Viagem(
    val id: String,
    val rotaId: String,
    val embarcacaoId: String,
    val diaSemana: DayOfWeek,
    /** Minutos desde a meia-noite. Texto só na fronteira de tela — ver [HoraDoDia]. */
    val horaMin: Int = 0,
    /** Quem criou — o `funcionarioId`, ou vazio para quem administra a plataforma. */
    val criadoPor: String = "",
    /** Quando, em ISO-8601. Texto porque é registro, não conta: ninguém soma datas de criação. */
    val criadoEm: String = "",
    val ativo: Boolean = true,
) {

    /**
     * A **chave da partida** — os quatro campos que, juntos, dizem que duas viagens são a mesma saída
     * (§7.1). É por ela que a unicidade do pool é verificada.
     */
    val chave: Chave get() = Chave(rotaId, embarcacaoId, diaSemana, horaMin)

    data class Chave(
        val rotaId: String,
        val embarcacaoId: String,
        val diaSemana: DayOfWeek,
        val horaMin: Int,
    )
}

/**
 * **Quando a travessia termina** — e a resposta tem duas partes porque no rio ela costuma ter.
 *
 * Uma linha de 30 horas chega numa hora do relógio e num **outro dia**. Devolver só o horário seria
 * mostrar "às 03:00" para quem sai às 21:00 sem dizer que são dois dias depois — a informação que falta é
 * exatamente a que decide a viagem de quem compra.
 */
data class Chegada(
    val horaMin: Int,
    val diasDepois: Int,
    val diaSemana: DayOfWeek,
)

/**
 * **Chegada estimada** = saída + tempo médio da rota (§7.1).
 *
 * É a razão de a hora ser número: aqui `tempoMedioH` é decimal (2,5h é meia hora e meia), e somá-lo a um
 * `"HH:mm"` exigiria um parser em cada leitor. Arredonda para o minuto — precisão maior seria falsa numa
 * medida que já é média.
 */
fun Viagem.chegadaEstimada(rota: Rota): Chegada {
    val total = horaMin + (rota.tempoMedioH * 60).roundToInt()
    val diasDepois = Math.floorDiv(total, MINUTOS_POR_DIA)

    return Chegada(
        horaMin = Math.floorMod(total, MINUTOS_POR_DIA),
        diasDepois = diasDepois,
        diaSemana = diaSemana.plus(diasDepois.toLong()),
    )
}

/**
 * A viagem **ativa** com esta chave, se houver — a checagem de duplicidade do pool compartilhado.
 *
 * Sem ela o pool degrada em pool duplicado, e a ocupação volta a se fragmentar: duas agências criando "a
 * saída de terça às 18h" separadamente teriam dois documentos para uma partida, e nenhuma das duas
 * contagens diria quantas vagas restam no navio (§7.1).
 *
 * Compara só as **ativas**, pelo mesmo motivo da Rota: uma viagem inativada com a mesma chave é registro
 * do passado, e recusar por causa dela impediria de recriar exatamente o que se acabou de corrigir.
 */
fun List<Viagem>.ativaComChave(chave: Viagem.Chave): Viagem? =
    firstOrNull { it.ativo && it.chave == chave }

/**
 * **Coerência da partida**: aponta para uma rota e uma embarcação, numa hora do relógio.
 *
 * O par correspondente do `Rota.temSentido()`, e igualmente trivial de propósito — o que a viagem tem de
 * difícil não é a forma, é a **autoridade** (quem pode criar) e o **recorte** (quem pode ver), e essas
 * duas moram em [AtuacaoDaEmpresa] e em [EscopoDoPool].
 */
fun Viagem.temSentido(): Boolean =
    rotaId.isNotBlank() && embarcacaoId.isNotBlank() && horaValida(horaMin)