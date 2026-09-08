package dev.matheus.fluviapp.util

import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Porta fina para **o instante presente** (F8.4).
 *
 * Existe por uma razão só, e é de teste: "a saída das 06:00 já partiu às 18:00" é regra de negócio, e
 * regra de negócio que depende de `LocalDateTime.now()` embutido não tem como ser exercitada — o teste
 * passaria ou falharia conforme a hora em que rodasse.
 *
 * O domínio (`disponiveisAPartirDe`) já recebe o instante como parâmetro; esta porta é quem o entrega,
 * e é o único lugar do caminho vivo que lê o relógio do aparelho.
 */
interface Relogio {
    fun agora(): LocalDateTime

    /**
     * O mesmo instante em **millis de época** — a forma que o `expiraEm` do acesso usa ([ADR-0032] Q1,
     * porque é o que uma regra do Firestore sabe comparar).
     *
     * Definida em termos de [agora] de propósito: um fake que congela o relógio congela as duas leituras
     * juntas. Se fossem dois caminhos independentes, a diferença entre eles apareceria como um teste que
     * passa e um comportamento que não.
     */
    fun millis(): Long = agora().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

@Singleton
class RelogioDoSistema @Inject constructor() : Relogio {
    override fun agora(): LocalDateTime = LocalDateTime.now()
}