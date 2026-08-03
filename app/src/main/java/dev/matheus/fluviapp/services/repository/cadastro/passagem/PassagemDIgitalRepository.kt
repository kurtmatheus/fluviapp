package dev.matheus.fluviapp.services.repository.cadastro.passagem

import dev.matheus.fluviapp.database.dao.passagem.PassagemDigitalDao
import dev.matheus.fluviapp.domain.passagem.PassagemDigital
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassagemDigitalRepository @Inject constructor(
    private val dao: PassagemDigitalDao
) {

    suspend fun salvar(passagemDigital: PassagemDigital) = dao.salvar(passagemDigital)

    suspend fun obterPorPassagem(idPassagem: String) = dao.obterPorPassagem(idPassagem).first()
}