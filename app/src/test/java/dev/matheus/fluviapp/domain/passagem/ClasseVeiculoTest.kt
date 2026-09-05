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
        assertEquals(2, NaturezaVeiculo.MOTOCICLO.classes.size)
        assertEquals(3, NaturezaVeiculo.MAQUINA.classes.size)
        // Cinco desde 2026-09-05: o jet-ski chega sobre carretilha, como a lancha.
        assertEquals(5, NaturezaVeiculo.REBOCADO.classes.size)
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
     * **Só a moto exige cilindrada** — uma exceção em dezessete linhas (correção da operação, 2026-09-05).
     *
     * Isto já foi derivado da natureza, e a derivação caiu com dois fatos: o quadriciclo não a exige, e o
     * jet-ski — que parecia provar que o traço era de família — é rebocado. O caso guarda a **exceção
     * única**: se um segundo `true` aparecer, é sinal de que a régua mudou e alguém tem de dizer por quê.
     */
    @Test
    fun `so a moto exige cilindrada, e e a unica excecao da tabela`() {
        assertTrue(ClasseVeiculo.MOTO.exigeCilindrada)
        assertEquals(listOf(ClasseVeiculo.MOTO), ClasseVeiculo.entries.filter { it.exigeCilindrada })
    }

    /** E o vizinho de natureza dela **não** exige — é o contraexemplo que derrubou a derivação. */
    @Test
    fun `o quadriciclo e da mesma natureza da moto e nao exige cilindrada`() {
        assertEquals(ClasseVeiculo.MOTO.natureza, ClasseVeiculo.QUADRICICLO.natureza)
        assertFalse(ClasseVeiculo.QUADRICICLO.exigeCilindrada)
    }

    /**
     * **O teste de forma**: fora da moto, a natureza é a única coisa que varia entre as classes. Duas
     * classes da mesma natureza são então **indistinguíveis para o código** — diferem no nome, que é a
     * chave da série, e em nada mais.
     */
    @Test
    fun `fora da moto, classes da mesma natureza se comportam igual`() {
        NaturezaVeiculo.entries.forEach { natureza ->
            val comportamentos = natureza.classes
                .filterNot { it == ClasseVeiculo.MOTO }
                .map { it.exigeCilindrada }
                .distinct()

            assertTrue("a natureza $natureza tem mais de um comportamento", comportamentos.size <= 1)
        }
    }
}
