package dev.matheus.fluviapp.telemetry

import javax.inject.Inject

/**
 * Semântica de observabilidade do **primeiro acesso** ([ADR-0032] D4) — o momento em que alguém
 * pré-cadastrado cria a própria senha e ganha perfil (ADR-0015 §2.1).
 *
 * Nasce em 2026-09-07 porque as duas falhas desse fluxo eram **as mais caras do app e as mais silenciosas
 * ao mesmo tempo**: definir a senha e criar o perfil. Quem esbarra numa delas **não entra** — e o único
 * rastro era um `Log.e` no aparelho de quem não conseguiu entrar, que é o lugar mais inútil possível para
 * guardá-lo.
 *
 * ### Por que registrador próprio
 *
 * Porque a etapa é a informação. *"Falhou no primeiro acesso"* não diz o que consertar; *"falhou ao criar
 * o perfil"* aponta para as regras do servidor, e *"falhou ao definir a senha"* aponta para o Auth. São
 * dois problemas com donos diferentes.
 */
class RegistroAcesso @Inject constructor(
    private val telemetry: Telemetry,
) {

    /**
     * O primeiro acesso não se completou, na [etapa] indicada.
     *
     * O motivo vai como parâmetro e o erro vira não-fatal: contar quantas pessoas travam, e em qual etapa,
     * é a pergunta agregada; o rastro do erro é o que permite consertar.
     */
    fun falhou(etapa: String, motivo: String) {
        telemetry.evento(EVENTO_FALHA, mapOf(PARAM_ETAPA to etapa, PARAM_MOTIVO to motivo))
    }

    /** A variante com exceção — mesma contagem, mais o não-fatal navegável. */
    fun falhou(etapa: String, erro: Throwable) {
        falhou(etapa, erro.message ?: DESCONHECIDO)
        telemetry.naoFatal(erro, mapOf(PARAM_ETAPA to etapa))
    }

    companion object {
        const val EVENTO_FALHA = "primeiro_acesso_falha"

        const val PARAM_ETAPA = "etapa"
        const val PARAM_MOTIVO = "motivo"

        /** O Auth recusou a troca de senha. */
        const val ETAPA_SENHA = "definir_senha"

        /** A senha foi trocada e o `users/{uid}` não nasceu — o pior dos dois, porque deixa pela metade. */
        const val ETAPA_PERFIL = "criar_perfil"

        const val DESCONHECIDO = "desconhecido"
    }
}
