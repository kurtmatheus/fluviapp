package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.util.Relogio
import java.time.LocalDateTime

/**
 * Fake da porta [Relogio] — é ele que torna "a saída das 06:00 já partiu" um caso de teste em vez de uma
 * aposta sobre a hora em que a suíte roda.
 */
class FakeRelogio(var instante: LocalDateTime) : Relogio {
    override fun agora(): LocalDateTime = instante
}