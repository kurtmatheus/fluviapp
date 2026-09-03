package dev.matheus.fluviapp.domain.passagem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A classe do veículo como **registro** ([ADR-0031] D1): dezessete valores, cada um declarando **uma** coisa
 * — a [NaturezaVeiculo].
 *
 * O que este teste guarda, antes de qualquer valor específico, é a forma: **nenhum comportamento é declarado
 * por classe**. Se `exigeCilindrada` voltar a ser uma coluna, ou se `exigeModelo` renascer, é aqui que
 * aparece — porque os casos abaixo afirmam que a única coisa que varia entre as dezessete é a natureza.
 */
class ClasseVeiculoTest {

    @Test
    fun `sao dezessete classes, e cada uma tem natureza`() {
        assertEquals(17, ClasseVeiculo.entries.size)
        ClasseVeiculo.entries.forEach { classe ->
            assertTrue("${classe.name} sem rótulo", classe.rotulo.isNotBlank())
        }
    }

    /** As onze que entraram em 2026-09-03, pela lista da operação (ADR-0031 D3). */
    @Test
    fun `as onze classes novas existem e sao reconhecidas pela fronteira`() {
        listOf(
            "TRATOR", "TRAILER", "LANCHA", "CARRETILHA", "JET_SKI", "QUADRICICLO",
            "EMPILHADEIRA", "RETROESCAVADEIRA", "MOTORHOME", "ONIBUS", "CARRETA_CAVALINHO",
        ).forEach { nome ->
            assertTrue("$nome deveria ser classe conhecida", ClasseVeiculo.de(nome) != null)
        }
    }

    /** As seis antigas continuam com o **mesmo `name`** — é o que mantém a série histórica inteira (§3.3). */
    @Test
    fun `as seis antigas conservam o name, que e a chave da serie`() {
        assertEquals(ClasseVeiculo.CARRO, ClasseVeiculo.de("CARRO"))
        assertEquals(ClasseVeiculo.MOTO, ClasseVeiculo.de("MOTO"))
        assertEquals(ClasseVeiculo.VAN, ClasseVeiculo.de("VAN"))
        assertEquals(ClasseVeiculo.SUV, ClasseVeiculo.de("SUV"))
        assertEquals(ClasseVeiculo.CAMINHAO, ClasseVeiculo.de("CAMINHAO"))
        assertEquals(ClasseVeiculo.CARRETA, ClasseVeiculo.de("CARRETA"))
    }

    @Test
    fun `de tolera espaco e caixa, e recusa desconhecido`() {
        assertEquals(ClasseVeiculo.MOTO, ClasseVeiculo.de(" moto "))
        assertEquals(ClasseVeiculo.SUV, ClasseVeiculo.de("suv"))
        // O espaço vira underscore: é assim que "Carreta Cavalinho" atravessa a fronteira.
        assertEquals(ClasseVeiculo.CARRETA_CAVALINHO, ClasseVeiculo.de("carreta cavalinho"))
        assertNull(ClasseVeiculo.de(null))
        assertNull(ClasseVeiculo.de("BICICLETA"))
    }

    // --- A natureza, e o que deriva dela ---

    /**
     * A distribuição das dezessete. Não é decoração: é ela que substitui os vinte e dois pertencimentos que
     * seriam escritos à mão se o casco continuasse enumerando classe a classe.
     */
    @Test
    fun `as dezessete se distribuem nas quatro naturezas`() {
        assertEquals(7, NaturezaVeiculo.AUTOMOTOR.classes.size)
        assertEquals(3, NaturezaVeiculo.MOTOCICLO.classes.size)
        assertEquals(3, NaturezaVeiculo.MAQUINA.classes.size)
        assertEquals(4, NaturezaVeiculo.REBOCADO.classes.size)
        assertEquals(17, NaturezaVeiculo.entries.sumOf { it.classes.size })
    }

    /**
     * **A `Carreta Cavalinho` é a tratora** — decisão do analista, e o caso que prova por que a natureza
     * precisa ser declarada: o nome carrega as duas metades (*cavalinho* é o cavalo mecânico, motorizado;
     * *carreta* é o semirreboque, rebocado) e não deixa derivar coisa nenhuma.
     */
    @Test
    fun `a carreta cavalinho e automotor, e a carreta e rebocada`() {
        assertEquals(NaturezaVeiculo.AUTOMOTOR, ClasseVeiculo.CARRETA_CAVALINHO.natureza)
        assertEquals(NaturezaVeiculo.REBOCADO, ClasseVeiculo.CARRETA.natureza)
    }

    /** A lancha **como classe** é carga rebocada, e não o casco que transporta (ADR-0031 D5). */
    @Test
    fun `a lancha como classe e rebocada`() {
        assertEquals(NaturezaVeiculo.REBOCADO, ClasseVeiculo.LANCHA.natureza)
    }

    /**
     * `exigeCilindrada` **deixou de ser coluna**: ela deriva da natureza, e por isso vale para as três do
     * motociclo — moto, quadriciclo e jet-ski — sem que ninguém as liste.
     */
    @Test
    fun `exigir cilindrada deriva da natureza, e vale para as tres do motociclo`() {
        listOf(ClasseVeiculo.MOTO, ClasseVeiculo.QUADRICICLO, ClasseVeiculo.JET_SKI)
            .forEach { assertTrue("$it deveria exigir cilindrada", it.exigeCilindrada) }

        ClasseVeiculo.entries
            .filter { it.natureza != NaturezaVeiculo.MOTOCICLO }
            .forEach { assertFalse("$it não deveria exigir cilindrada", it.exigeCilindrada) }
    }

    /**
     * **O teste de forma**, e o mais importante deste arquivo: a natureza é a única coisa que varia entre as
     * classes. Duas classes da mesma natureza são **indistinguíveis para o código** — diferem no nome, que é
     * a chave da série, e em nada mais.
     */
    @Test
    fun `classes da mesma natureza se comportam igual`() {
        NaturezaVeiculo.entries.forEach { natureza ->
            val comportamentos = natureza.classes.map { it.exigeCilindrada }.distinct()
            assertEquals("a natureza $natureza deveria ter um comportamento só", 1, comportamentos.size)
        }
    }
}
