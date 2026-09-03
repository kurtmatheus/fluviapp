package dev.matheus.fluviapp.domain.viagem

import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tipo da embarcação como tipo de domínio (ADR-0020 D4). A tabela do ADR-0016 §8, agora executável:
 * F/B leva tudo, navio leva carro e moto, lancha só passageiro.
 *
 * **Entrou no escopo da revitalização** junto com o campo `Embarcacao.tipo`: até então o tipo existia sem
 * ninguém que o carregasse, e o `@Category(ForaDoEscopo)` dizia isso. Agora a Flotilha inteira depende
 * dele — o cadastro exige, a fronteira descarta quem não o tem, a busca o exibe.
 */
class TipoEmbarcacaoTest {

    @Test
    fun `de converte o name canonico e recusa desconhecido`() {
        assertEquals(TipoEmbarcacao.FERRY_BOAT, TipoEmbarcacao.de("FERRY_BOAT"))
        assertEquals(TipoEmbarcacao.NAVIO, TipoEmbarcacao.de("navio"))
        assertEquals(TipoEmbarcacao.LANCHA, TipoEmbarcacao.de(" Lancha "))
        assertNull(TipoEmbarcacao.de(null))
        // O "Catamarã" do §8: cadastrado no catálogo, nascia inerte. Agora simplesmente não existe.
        assertNull(TipoEmbarcacao.de("CATAMARA"))
    }

    @Test
    fun `de tolera espaco no lugar do underscore`() {
        assertEquals(TipoEmbarcacao.FERRY_BOAT, TipoEmbarcacao.de("FERRY BOAT"))
    }

    /** Documento sem o campo: a fronteira lê `""` e tem de recusar, não escolher um padrão. */
    @Test
    fun `de recusa texto vazio`() {
        assertNull(TipoEmbarcacao.de(""))
        assertNull(TipoEmbarcacao.de("   "))
    }

    // --- A outra fronteira: o rótulo que a tela devolve ---

    @Test
    fun `porRotulo converte o que o dropdown escolheu`() {
        assertEquals(TipoEmbarcacao.FERRY_BOAT, TipoEmbarcacao.porRotulo("Ferry Boat"))
        assertEquals(TipoEmbarcacao.NAVIO, TipoEmbarcacao.porRotulo("navio"))
        assertNull(TipoEmbarcacao.porRotulo(null))
        assertNull(TipoEmbarcacao.porRotulo("Catamarã"))
    }

    /**
     * As duas fronteiras não se confundem: o `name` é o que o Firestore grava, o rótulo é o que a pessoa
     * lê. `FERRY_BOAT` não é rótulo de nada, e é isso que permite reescrever "Ferry Boat" na tela sem
     * migrar documento.
     */
    @Test
    fun `porRotulo nao aceita o name, e de nao aceita o rotulo com espaco fora do padrao`() {
        assertNull(TipoEmbarcacao.porRotulo("FERRY_BOAT"))
        assertEquals(TipoEmbarcacao.FERRY_BOAT, TipoEmbarcacao.de("Ferry Boat")) // este ainda casa: vira FERRY_BOAT
    }

    @Test
    fun `todo tipo tem rotulo, e todo rotulo volta a ser tipo`() {
        TipoEmbarcacao.entries.forEach { tipo ->
            assertTrue(tipo.rotulo.isNotBlank())
            assertEquals(tipo, TipoEmbarcacao.porRotulo(tipo.rotulo))
        }
    }

    @Test
    fun `a balsa leva todas as classes, inclusive as pesadas`() {
        ClasseVeiculo.entries.forEach { assertTrue(TipoEmbarcacao.FERRY_BOAT.admite(it)) }
    }

    /**
     * **O caso que mede o D4.** Não basta a balsa levar as dezessete de hoje: ela tem de levar a
     * **dezoito**, quando ela existir, sem que ninguém a declare em lugar nenhum.
     *
     * É o que separa *"a lista está completa"* de *"não há lista"* — e é a diferença entre esta forma e a
     * anterior, em que cada classe nova nascia admitida por nenhum casco até alguém lembrar de acrescentá-la.
     */
    @Test
    fun `a balsa admite por exclusao, e nao por enumeracao`() {
        assertEquals(CargaAdmitida.Todas, TipoEmbarcacao.FERRY_BOAT.cargaAdmitida)
        assertEquals(ClasseVeiculo.entries.size, TipoEmbarcacao.FERRY_BOAT.classesAdmitidas.size)
    }

    /**
     * **O navio encolheu com o D4**: eram carro, moto, van e SUV; passam a ser carro e moto. O nome deste
     * caso já dizia isso antes de o código dizer — ele estava certo e as asserções é que não o
     * acompanhavam.
     */
    @Test
    fun `o navio leva carro e moto, mas nao carga pesada`() {
        assertTrue(TipoEmbarcacao.NAVIO.admite(ClasseVeiculo.CARRO))
        assertTrue(TipoEmbarcacao.NAVIO.admite(ClasseVeiculo.MOTO))
        assertFalse(TipoEmbarcacao.NAVIO.admite(ClasseVeiculo.CAMINHAO))
        assertFalse(TipoEmbarcacao.NAVIO.admite(ClasseVeiculo.CARRETA))
    }

    /**
     * E leva **só** esses dois: van e SUV saíram, e com elas as onze classes novas. O navio é a única
     * exceção enumerada do domínio, e por isso é o único lugar onde acrescentar uma classe exige decisão.
     */
    @Test
    fun `o navio leva exatamente duas classes`() {
        assertEquals(
            listOf(ClasseVeiculo.CARRO, ClasseVeiculo.MOTO),
            TipoEmbarcacao.NAVIO.classesAdmitidas,
        )
        listOf(ClasseVeiculo.VAN, ClasseVeiculo.SUV, ClasseVeiculo.ONIBUS, ClasseVeiculo.TRATOR)
            .forEach { assertFalse("$it não deveria entrar no navio", TipoEmbarcacao.NAVIO.admite(it)) }
    }

    /**
     * *"Não se vende veículo para uma lancha se a cadastrarmos."* Inclusive — e aqui a frase fica
     * involuntariamente literal — a `ClasseVeiculo.LANCHA`, que é a embarcação **transportada** sobre
     * carretilha: o casco lancha não leva nem a lancha rebocada.
     */
    @Test
    fun `nao se vende veiculo para uma lancha`() {
        ClasseVeiculo.entries.forEach { assertFalse(TipoEmbarcacao.LANCHA.admite(it)) }
        assertFalse(TipoEmbarcacao.LANCHA.levaVeiculo)
        assertEquals(CargaAdmitida.Nenhuma, TipoEmbarcacao.LANCHA.cargaAdmitida)
        assertTrue(TipoEmbarcacao.LANCHA.classesAdmitidas.isEmpty())
    }

    @Test
    fun `admite recusa classe nula`() {
        assertFalse(TipoEmbarcacao.FERRY_BOAT.admite(null))
    }

    @Test
    fun `levaVeiculo separa quem oferece o modo veiculo`() {
        assertTrue(TipoEmbarcacao.FERRY_BOAT.levaVeiculo)
        assertTrue(TipoEmbarcacao.NAVIO.levaVeiculo)
        assertFalse(TipoEmbarcacao.LANCHA.levaVeiculo)
    }
}