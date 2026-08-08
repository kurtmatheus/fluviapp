package dev.matheus.fluviapp.domain.rota

/**
 * **A ligação entre dois portos** (ADR-0016 §7.1, ADR-0022 F7) — o *onde* e o *quão longe*.
 *
 * Ela mora na raiz e **não pertence a ninguém**: duas empresas que vendem a mesma linha usam a mesma
 * rota. É o que permite, um nível adiante, que usem a mesma **viagem** — e é daí que sai a ocupação como
 * uma conta só, atravessando empresas sem *collection group*.
 *
 * ### Imutável, e o descartado vira registro
 *
 * Não se edita e não se apaga. Errou o número, mudou a medição, deixou de operar? **Cria-se outra e
 * inativa-se esta** (decisão do analista, 2026-08-08). São duas razões somadas:
 *
 * 1. **ninguém quebra o que o outro vende.** A rota é compartilhada; editá-la mudaria, retroativamente,
 *    o significado de viagens e bilhetes de terceiros que apontam para ela;
 * 2. **o "lixo" é dado.** A rota fora de uso continua respondendo o que as passagens antigas
 *    significavam — e é matéria-prima da fase analítica, não sujeira a limpar.
 *
 * Por isso [criadoPor] e [criadoEm] existem desde o primeiro dia: num pool sem dono, a assinatura é o
 * que resta de responsabilidade.
 *
 * ### O que ela NÃO carrega
 *
 * Nem cidades, nem tarifa, nem embarcação. As cidades vêm dos portos (§5) — repeti-las aqui seria criar
 * duas verdades para a mesma pergunta. A tarifa é competência da **agência**, e uma entidade sem dono
 * não tem de quem ter tarifa (§7.1). A embarcação e o horário são da **Viagem** (F8).
 *
 * [distanciaMn] e [tempoMedioH] são o que justifica a Rota existir em vez de ser derivada: são fatos que
 * nenhuma outra entidade tem. O Trecho morreu por ser derivável; esta não é.
 */
data class Rota(
    val id: String,
    val portoOrigemId: String,
    val portoDestinoId: String,
    /** Distância em **milhas náuticas**. */
    val distanciaMn: Double = 0.0,
    /** Tempo médio de travessia, em **horas**. */
    val tempoMedioH: Double = 0.0,
    /** Quem criou — o `funcionarioId`, ou vazio para quem administra a plataforma. */
    val criadoPor: String = "",
    /** Quando, em ISO-8601. Texto porque é registro, não conta: ninguém soma datas de criação. */
    val criadoEm: String = "",
    val ativo: Boolean = true,
) {

    /**
     * O **par de portos**, na ordem — e a ordem importa: ida e volta são rotas diferentes, porque a
     * viagem que as percorre também é. É esta chave que a unicidade usa.
     */
    val par: Pair<String, String> get() = portoOrigemId to portoDestinoId
}

/**
 * **Coerência de sentido** (ADR-0016 §7): embarque e desembarque não podem ser o mesmo porto.
 *
 * É trivial e é a única das duas validações originais que sobrou — a geográfica morreu na 7ª rodada,
 * quando a cidade passou a vir do porto e deixou de haver o que cruzar. Está aqui, e não só na tela,
 * porque é o domínio que sabe o que é uma travessia.
 */
fun Rota.temSentido(): Boolean =
    portoOrigemId.isNotBlank() && portoDestinoId.isNotBlank() && portoOrigemId != portoDestinoId

/**
 * A rota **ativa** para este par, se houver. É a checagem de duplicidade do pool compartilhado: sem ela
 * o pool degrada em pool duplicado, e aí a ocupação volta a se fragmentar — que é o ganho principal se
 * perdendo pela porta dos fundos (§7.1).
 *
 * Compara só as **ativas**: uma rota inativada com o mesmo par é registro do passado, e recusar por
 * causa dela seria impedir de recriar exatamente o que se acabou de corrigir.
 */
fun List<Rota>.ativaComPar(origemId: String, destinoId: String): Rota? =
    firstOrNull { it.ativo && it.portoOrigemId == origemId && it.portoDestinoId == destinoId }