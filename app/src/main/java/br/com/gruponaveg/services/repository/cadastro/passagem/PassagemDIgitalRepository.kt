package br.com.gruponaveg.services.repository.cadastro.passagem

import br.com.gruponaveg.database.dao.passagem.PassagemDigitalDao
import br.com.gruponaveg.model.passagem.PassagemDigital
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