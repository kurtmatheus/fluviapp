package dev.matheus.fluviapp.services.repository.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import dev.matheus.fluviapp.BuildConfig
import dev.matheus.fluviapp.model.cadastro.passagem.toDocumento
import dev.matheus.fluviapp.sampledata.listaAcomodacaoSample
import dev.matheus.fluviapp.sampledata.listaAgenteSample
import dev.matheus.fluviapp.sampledata.listaEmpresaSample
import dev.matheus.fluviapp.sampledata.listaFormaPagamentoSample
import dev.matheus.fluviapp.sampledata.listaMunicipioSample
import dev.matheus.fluviapp.sampledata.listaNavioSample
import dev.matheus.fluviapp.sampledata.listaStatusPassagemSample
import dev.matheus.fluviapp.sampledata.listaTipoDocumentosSample
import dev.matheus.fluviapp.sampledata.listaTipoGratuidadeSample
import dev.matheus.fluviapp.sampledata.listaTipoPassagemSample
import dev.matheus.fluviapp.sampledata.listaTipoVeiculoSample
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.cadastro.passagem.AgenteRepository
import dev.matheus.fluviapp.services.repository.firebase.documents.ConstanteDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.ContadorDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.EmpresaDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.NavioDocumento
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Semeadura fictícia do Firestore (a VERDADE — ADR-0003). Escreve nos docs de origem; o Room se
 * preenche sozinho pelos listeners de sync existentes. NÃO escreve no Room direto (isso faria o
 * cache divergir da verdade). Só em debug e só se o projeto está vazio (guarda por `users`).
 *
 * Pré-requisitos de runtime: regras do Firestore em test mode (escrita aberta) e — para o LOGIN
 * funcionar — o usuário do Firebase Auth (console) precisa ter um dos e-mails semeados aqui.
 */
@Singleton
class SeedFirestore @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    fun semearSeVazio() {
        if (!BuildConfig.DEBUG) return
        // Guarda por `constants` (catálogo que só o seed cria) — NÃO por `users`, que o cadastro
        // passa a popular (senão um cadastro antecipado faria o seed pular os catálogos).
        firestore.collection(ConstanteRepository.COLLECTION_CONSTANTS).limit(1).get()
            .addOnSuccessListener { snapshot -> if (snapshot.isEmpty) semear() }
            .addOnFailureListener { e -> Log.e(TAG, "guarda do seed falhou: ${e.message}", e) }
    }

    private fun semear() {
        // Usuários NÃO são semeados: o cadastro in-app (com verificação + perfil auto-criado)
        // provisiona operadores. Aqui só os catálogos.
        Log.i(TAG, "Semeando Firestore (projeto vazio, debug)")

        listaAgenteSample.forEach { a ->
            firestore.collection(AgenteRepository.COLLECTION_AGENTS).document(a.id).set(a.toDocumento())
        }

        // ids colidem entre categorias -> auto-id; o app filtra constantes por categoria/descrição.
        val constantes = listaTipoDocumentosSample + listaMunicipioSample + listaAcomodacaoSample +
            listaFormaPagamentoSample + listaStatusPassagemSample + listaTipoPassagemSample +
            listaTipoGratuidadeSample + listaTipoVeiculoSample
        constantes.forEach { c ->
            firestore.collection(ConstanteRepository.COLLECTION_CONSTANTS)
                .add(ConstanteDocumento(descricao = c.descricaoNome, categoria = c.categoria))
        }

        listaEmpresaSample.forEach { e ->
            firestore.collection(COLLECTION_EMPRESAS).document(e.id).set(
                EmpresaDocumento(
                    nome = e.nome,
                    razaoSocial = e.razaoSocial,
                    cnpj = e.cnpj,
                    endereco = e.endereco,
                    telefone1 = e.telefone1,
                    telefone2 = e.telefone2,
                ),
            )
        }

        listaNavioSample.forEach { n ->
            // ADR-0008 Fase 3: o navio nasce vinculado à Empresa só por id (empresaId).
            firestore.collection(COLLECTION_NAVIOS).document(n.id).set(
                NavioDocumento(
                    nome = n.descricaoNome,
                    capacidadeVeiculo = n.capacidadeVeiculo,
                    capacidadeSuite2 = n.capacidadeSuite2,
                    capacidadeSuite3 = n.capacidadeSuite3,
                    capacidadeCamarote = n.capacidadeCamarote,
                    empresaId = n.empresaId,
                ),
            )
        }

        firestore.collection(COLLECTION_PASSAGENS).document(DOCUMENT_CONTADOR)
            .set(ContadorDocumento(numeroBilhete = 0))
    }

    companion object {
        private const val TAG = "seedFirestore"
        private const val COLLECTION_EMPRESAS = "empresas"
        private const val COLLECTION_NAVIOS = "navios"
        private const val COLLECTION_PASSAGENS = "passagens"
        private const val DOCUMENT_CONTADOR = "contador"
    }
}
