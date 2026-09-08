package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.operacoes.ContextoUsuario
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Perfil
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.domain.operacoes.Vinculo
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake da porta [SessaoUsuario] — é o que torna o **recorte por papel/cargo** testável sem Room nem
 * Firestore. Os construtores nomeados abaixo são as três personas que a política distingue (ADR-0015
 * §8.1); usá-los nos testes evita montar `Usuario`/`Funcionario` à mão em cada caso.
 */
class FakeSessaoUsuario(var contexto: ContextoUsuario? = null) : SessaoUsuario {

    /**
     * O contexto como fluxo ([ADR-0032] D2). Emite **uma vez** e fica: o fake não simula a reatividade do
     * DataStore, porque nenhum teste de hoje depende dela — quem quiser a próxima emissão troca o
     * `contexto` e coleta de novo.
     *
     * `atual()` não é sobrescrito: a interface já o define como `observar().first()`, e ter duas
     * implementações no fake seria a chance de elas discordarem.
     */
    override fun observar(): Flow<ContextoUsuario?> = flowOf(contexto)

    /**
     * A troca de perfil ([ADR-0032] D5) aplicada **ao contexto**, como o DataStore real faria: quem decide
     * se a escolha vale continua sendo o domínio, na leitura seguinte.
     */
    override suspend fun trocarPerfil(perfil: Perfil) {
        contexto = contexto?.copy(perfilEscolhido = perfil)
    }

    override suspend fun encerrar() {
        contexto = null
    }

    companion object {
        /** Papel puro de plataforma: existe no sistema, não existe na operação (sem funcionário). */
        fun plataforma(papel: String = Usuario.Papel.ADM.name) = FakeSessaoUsuario(
            ContextoUsuario(
                usuario = Usuario(id = "u-adm", email = "adm@x.com", username = "adm", papel = papel),
                funcionario = null,
            )
        )

        /**
         * O operador chega com **vínculo** (ADR-0016 §6): é dele que saem a empresa e o cargo em vigor.
         * `empresaId` continua tendo um default para não obrigar todo teste a inventar um id — o que
         * importa em quase todos é *ter* vínculo, não qual.
         *
         * A persona `comDoisVinculos` saiu com a [ADR-0032] D5: ela existia para a seleção de contexto,
         * e não há mais quem sirva a duas empresas para representar.
         */
        fun supervisor(empresaId: String = "empresa-1") =
            operador(Funcionario.Cargo.SUPERVISOR, empresaId)

        fun agente(empresaId: String = "empresa-1") =
            operador(Funcionario.Cargo.AGENTE, empresaId)

        private fun operador(cargo: Funcionario.Cargo, empresaId: String) = FakeSessaoUsuario(
            ContextoUsuario(
                usuario = Usuario(
                    id = "u-op",
                    email = "op@x.com",
                    username = "op",
                    papel = Usuario.Papel.OPERADOR.name,
                    funcionarioId = "f-op",
                ),
                funcionario = Funcionario(
                    id = "f-op",
                    descricaoNome = "Operador",
                    cargo = cargo.name,
                    vinculo = Vinculo(empresaId, cargo),
                ),
            )
        )
    }
}