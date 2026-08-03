package dev.matheus.fluviapp.domain.rascunho

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.experimental.categories.Category
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

/** Valida que o snapshot sobrevive ao round-trip JSON (a base do store Room-JSON). */
@Category(ForaDoEscopo::class)
class RascunhoSerializacaoGsonTest {

    private val gson = Gson()

    @Test
    fun `snapshot sobrevive ao round-trip JSON`() {
        val original = RascunhoPassagemSnapshot(
            dataViagem = "10/06/2024",
            horaViagem = "12:00",
            valorPix = "100",
            isPixChecked = true,
            nomePassageiro1 = "Passageiro Um",
            acomodacao = "REDE",
            tipoPassagem = "INTEIRA",
            placaVeiculo = "ABC1D23",
            isVeiculoChecked = true,
        )

        val json = gson.toJson(original)
        val restaurado = gson.fromJson(json, RascunhoPassagemSnapshot::class.java)

        assertEquals(original, restaurado)
    }
}
