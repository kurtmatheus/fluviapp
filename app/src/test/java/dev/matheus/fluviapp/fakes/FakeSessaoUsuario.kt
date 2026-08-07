package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.operacoes.ContextoUsuario
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.domain.operacoes.Vinculo
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario

/**
 * Fake da porta [SessaoUsuario] — é o que torna o **recorte por papel/cargo** testável sem Room nem
 * Firestore. Os construtores nomeados abaixo são as três personas que a política distingue (ADR-0015
 * §8.1); usá-los nos testes evita montar `Usuario`/`Funcionario` à mão em cada caso.
 */
class FakeSessaoUsuario(var contexto: ContextoUsuario? = null) : SessaoUsuario {
    /** A escolha guardada, como no DataStore real — e aplicada ao contexto na leitura seguinte. */
    var empresaEscolhida: String? = null
        private set

    override suspend fun atual(): ContextoUsuario? =
        contexto?.copy(empresaAtivaId = empresaEscolhida ?: contexto?.empresaAtivaId)

    override suspend fun escolherEmpresa(empresaId: String) { empresaEscolhida = empresaId }

    override suspend fun limparEscolha() { empresaEscolhida = null }

    companion object {
        /** Papel puro de plataforma: existe no sistema, não existe na operação (sem funcionário). */
        fun plataforma(papel: String = Usuario.Papel.ADM.name) = FakeSessaoUsuario(
            ContextoUsuario(
                usuario = Usuario(id = "u-adm", email = "adm@x.com", username = "adm", papel = papel),
                funcionario = null,
            )
        )

        /**
         * O operador agora chega com **vínculo** (ADR-0016 §6): é dele que saem a empresa e o cargo em
         * vigor. `empresaId` continua tendo um default para não obrigar todo teste a inventar um id — o
         * que importa em quase todos é *ter* vínculo, não qual.
         */
        fun supervisor(empresaId: String = "empresa-1") =
            operador(Funcionario.Cargo.SUPERVISOR, empresaId)

        fun agente(empresaId: String = "empresa-1") =
            operador(Funcionario.Cargo.AGENTE, empresaId)

        /** Quem serve a duas empresas — a persona que a seleção de contexto (F6.4) existe para atender. */
        fun comDoisVinculos(
            primeira: String = "empresa-1",
            segunda: String = "empresa-2",
            escolhida: String? = null,
        ) = FakeSessaoUsuario(
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
                    vinculos = listOf(
                        Vinculo(primeira, Funcionario.Cargo.SUPERVISOR),
                        Vinculo(segunda, Funcionario.Cargo.AGENTE),
                    ),
                ),
                empresaAtivaId = escolhida,
            )
        )

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
                    vinculos = listOf(Vinculo(empresaId, cargo)),
                ),
            )
        )
    }
}