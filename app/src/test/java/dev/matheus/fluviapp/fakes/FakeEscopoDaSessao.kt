package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.viagem.AtuacaoDaEmpresa
import dev.matheus.fluviapp.domain.viagem.EscopoDoPool
import dev.matheus.fluviapp.services.repository.operacoes.EscopoDaSessao

/**
 * Fake da porta [EscopoDaSessao] — é o que torna o **recorte por concessão** testável sem Firestore.
 *
 * Os três construtores nomeados são os três estados do escopo, e existem para que o teste diga qual
 * situação está exercitando em vez de montar a atuação à mão: a plataforma que cura o pool, a empresa que
 * recebeu um recorte, e a que não recebeu nada — que é a que mais engana quando aparece como lista vazia.
 */
class FakeEscopoDaSessao(var escopo: EscopoDoPool = EscopoDoPool.Todo) : EscopoDaSessao {

    override suspend fun atual(): EscopoDoPool = escopo

    companion object {
        fun plataforma() = FakeEscopoDaSessao(EscopoDoPool.Todo)

        fun concedido(portoIds: Set<String>, embarcacaoIds: Set<String>) = FakeEscopoDaSessao(
            EscopoDoPool.Concedido(
                AtuacaoDaEmpresa(
                    atuacao = Atuacao.AGENCIAMENTO,
                    embarcacaoIds = embarcacaoIds,
                    portoIds = portoIds,
                )
            )
        )

        fun semNada() = FakeEscopoDaSessao(EscopoDoPool.Nenhum)
    }
}