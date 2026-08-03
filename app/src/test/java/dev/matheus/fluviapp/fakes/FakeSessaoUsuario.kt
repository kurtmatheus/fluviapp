package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.operacoes.ContextoUsuario
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario

/**
 * Fake da porta [SessaoUsuario] — é o que torna o **recorte por papel/cargo** testável sem Room nem
 * Firestore. Os construtores nomeados abaixo são as três personas que a política distingue (ADR-0015
 * §8.1); usá-los nos testes evita montar `Usuario`/`Funcionario` à mão em cada caso.
 */
class FakeSessaoUsuario(var contexto: ContextoUsuario? = null) : SessaoUsuario {
    override suspend fun atual(): ContextoUsuario? = contexto

    companion object {
        /** Papel puro de plataforma: existe no sistema, não existe na operação (sem funcionário). */
        fun plataforma(papel: String = Usuario.Papel.ADM.name) = FakeSessaoUsuario(
            ContextoUsuario(
                usuario = Usuario(id = "u-adm", email = "adm@x.com", username = "adm", papel = papel),
                funcionario = null,
            )
        )

        fun supervisor(agencia: String = "MATRIZ") = operador(Funcionario.Cargo.SUPERVISOR.name, agencia)

        fun agente(agencia: String = "MATRIZ") = operador(Funcionario.Cargo.AGENTE.name, agencia)

        private fun operador(cargo: String, agencia: String) = FakeSessaoUsuario(
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
                    agencia = agencia,
                    lotacao = "PORTO NORTE",
                    cargo = cargo,
                ),
            )
        )
    }
}