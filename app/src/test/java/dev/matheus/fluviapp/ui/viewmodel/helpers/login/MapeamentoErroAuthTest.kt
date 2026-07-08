package dev.matheus.fluviapp.ui.viewmodel.helpers.login

import dev.matheus.fluviapp.R
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Só o ramo `else` é JVM-puro. Os ramos específicos (FirebaseAuthInvalidCredentials/InvalidUser)
 * NÃO são testáveis aqui: construir essas exceções chama `android.text.TextUtils.isEmpty`
 * ("not mocked" fora do device/Robolectric) — são tipos da borda de rede/Android. Cobertura
 * plena dessa regra depende de mapear na fronteira para um enum de domínio (item 2 / porta
 * ResultadoAutenticacao), aí o mapeamento enum→mensagem vira 100% puro.
 */
class MapeamentoErroAuthTest {

    @Test
    fun `excecao generica mapeia para falha_auth`() {
        assertEquals(R.string.error_falha_auth, mapearMensagemErroAuth(RuntimeException("x")))
    }
}