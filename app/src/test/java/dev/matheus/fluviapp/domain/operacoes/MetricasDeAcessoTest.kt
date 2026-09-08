package dev.matheus.fluviapp.domain.operacoes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * As métricas de acesso do painel da plataforma ([ADR-0032] D6).
 *
 * O que estes casos travam não é a soma: é **de onde cada número vem**. A situação sai da mesma função que
 * a lista da seção Usuários usa (`situacaoDoAcesso`), e *convite pendente* sai da **ausência de perfil** —
 * não do `usado` do convite. Contar por um critério próprio faria o card e a lista discordarem sobre a
 * mesma pessoa, e o card é justamente onde ninguém notaria.
 */
class MetricasDeAcessoTest {

    private val agora = 1_800_000_000_000L
    private val ontem = agora - 86_400_000L
    private val amanha = agora + 86_400_000L
    private val emDuasSemanas = agora + 14 * 86_400_000L

    private fun usuario(
        email: String,
        ativo: Boolean = true,
        expiraEm: Long? = null,
    ) = Usuario(
        id = "uid-$email",
        email = email,
        username = email.substringBefore('@'),
        papel = Usuario.Papel.OPERADOR.name,
        ativo = ativo,
        expiraEm = expiraEm,
    )

    private fun convite(email: String, usado: Boolean = false) = Convite(
        email = email,
        nome = email.substringBefore('@'),
        papel = Usuario.Papel.OPERADOR,
        empresaId = "empresa-1",
        cargo = Funcionario.Cargo.AGENTE,
        usado = usado,
    )

    @Test
    fun `sem ninguem, todos os numeros sao zero e nao ha pendencia`() {
        val metricas = metricasDeAcesso(emptyList(), emptyList(), agora)

        assertEquals(MetricasDeAcesso(), metricas)
        assertEquals(0, metricas.comAcesso)
        assertFalse(metricas.temPendencia)
    }

    @Test
    fun `cada situacao cai no seu numero`() {
        val metricas = metricasDeAcesso(
            usuarios = listOf(
                usuario("ativo@x.com"),
                usuario("com-prazo@x.com", expiraEm = emDuasSemanas),
                usuario("desativado@x.com", ativo = false),
                usuario("vencido@x.com", expiraEm = ontem),
            ),
            convites = emptyList(),
            agora = agora,
        )

        assertEquals(2, metricas.ativos)
        assertEquals(1, metricas.desativados)
        assertEquals(1, metricas.expirados)
        assertEquals(4, metricas.comAcesso)
    }

    /**
     * **Convite pendente é a ausência de perfil**, e não o `usado` do convite.
     *
     * A diferença é de autoridade, e ela aparece aqui: o convite de *entrou* está marcado como não-usado
     * (uma escrita que falhou no meio do primeiro acesso), e mesmo assim não conta — porque o perfil
     * existe, e o perfil é o fato.
     */
    @Test
    fun `convite pendente e quem nao tem perfil, e o usado nao decide`() {
        val metricas = metricasDeAcesso(
            usuarios = listOf(usuario("entrou@x.com")),
            convites = listOf(
                convite("entrou@x.com", usado = false),
                convite("nao-veio@x.com"),
                convite("tambem-nao@x.com"),
            ),
            agora = agora,
        )

        assertEquals(2, metricas.convitesPendentes)
    }

    /** O e-mail casa **sem caixa**: o convite é gravado em minúsculas, o perfil vem do Auth. */
    @Test
    fun `a juncao por e-mail ignora a caixa`() {
        val metricas = metricasDeAcesso(
            usuarios = listOf(usuario("Ana@X.com")),
            convites = listOf(convite("ana@x.com")),
            agora = agora,
        )

        assertEquals(0, metricas.convitesPendentes)
    }

    // --- O número preventivo ---

    @Test
    fun `a vencer conta o prazo dentro da janela, e nao o de longe`() {
        val metricas = metricasDeAcesso(
            usuarios = listOf(
                usuario("amanha@x.com", expiraEm = amanha),
                usuario("em-duas-semanas@x.com", expiraEm = emDuasSemanas),
                usuario("sem-prazo@x.com"),
            ),
            convites = emptyList(),
            agora = agora,
        )

        assertEquals(1, metricas.aVencer)
    }

    /** **Expirado não é a vencer**: são estados sucessivos, e o card mostra os dois lado a lado. */
    @Test
    fun `quem ja venceu nao conta como a vencer`() {
        val metricas = metricasDeAcesso(listOf(usuario("vencido@x.com", expiraEm = ontem)), emptyList(), agora)

        assertEquals(1, metricas.expirados)
        assertEquals(0, metricas.aVencer)
    }

    /**
     * **Desativado com prazo a vencer não precisa de renovação, precisa de decisão.** Somá-lo ao número
     * preventivo inflaria o aviso com casos que já são pendência de outro tipo.
     */
    @Test
    fun `desativado com prazo proximo nao entra no aviso`() {
        val metricas = metricasDeAcesso(
            usuarios = listOf(usuario("x@x.com", ativo = false, expiraEm = amanha)),
            convites = emptyList(),
            agora = agora,
        )

        assertEquals(1, metricas.desativados)
        assertEquals(0, metricas.aVencer)
    }

    @Test
    fun `a janela do aviso e parametro`() {
        val usuarios = listOf(usuario("x@x.com", expiraEm = emDuasSemanas))

        assertEquals(0, metricasDeAcesso(usuarios, emptyList(), agora).aVencer)
        assertEquals(1, metricasDeAcesso(usuarios, emptyList(), agora, dias = 30).aVencer)
    }

    // --- O que o card pergunta ---

    /** É esta pergunta que decide se o card fala ou fica quieto — e ativo puro não é pendência. */
    @Test
    fun `so ha pendencia quando alguem espera um gesto`() {
        assertFalse(metricasDeAcesso(listOf(usuario("a@x.com")), emptyList(), agora).temPendencia)

        assertTrue(metricasDeAcesso(listOf(usuario("a@x.com", ativo = false)), emptyList(), agora).temPendencia)
        assertTrue(metricasDeAcesso(listOf(usuario("a@x.com", expiraEm = ontem)), emptyList(), agora).temPendencia)
        assertTrue(metricasDeAcesso(listOf(usuario("a@x.com", expiraEm = amanha)), emptyList(), agora).temPendencia)
        assertTrue(metricasDeAcesso(emptyList(), listOf(convite("b@x.com")), agora).temPendencia)
    }

    /**
     * **Nenhum número identifica ninguém**, e não é por disciplina de quem escreveu: o tipo só tem `Int`.
     * É a régua da D4 — *o evento conta, o documento prova* — sobrevivendo à troca de fonte.
     */
    @Test
    fun `as metricas sao so numeros`() {
        val campos = MetricasDeAcesso::class.java.declaredFields
            .filterNot { it.isSynthetic }
            .map { it.type.simpleName }

        assertTrue("campo que não é número: $campos", campos.all { it == "int" })
    }
}
