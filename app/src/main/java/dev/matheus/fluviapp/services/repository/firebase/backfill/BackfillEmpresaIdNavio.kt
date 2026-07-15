package dev.matheus.fluviapp.services.repository.firebase.backfill

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import dev.matheus.fluviapp.model.viagem.Empresa
import dev.matheus.fluviapp.model.viagem.Navio
import dev.matheus.fluviapp.services.repository.firebase.documents.EmpresaDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.NavioDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toEmpresa
import dev.matheus.fluviapp.services.repository.firebase.documents.toNavio
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Uma gravação planejada: preencher `navios/{navioId}.empresaId` com o id resolvido do nome. */
data class AtualizacaoEmpresaId(
    val navioId: String,
    val empresaId: String,
    val empresaNome: String,
)

/**
 * Plano observável do backfill (ADR-0008, Fase 0). Cada navio cai em exatamente um balde, e os
 * homônimos ficam explícitos para desempate manual — o backfill nunca "chuta" um id ambíguo.
 */
data class ResultadoBackfillEmpresaId(
    val atualizados: List<AtualizacaoEmpresaId> = emptyList(),
    val jaTinham: List<String> = emptyList(),   // idempotência: já tinham empresaId
    val semMatch: List<String> = emptyList(),    // nome não existe em /empresas (ou navio sem nome)
    val ambiguos: List<String> = emptyList(),    // nome homônimo (>1 empresa) — pulado
    val nomesHomonimos: Set<String> = emptySet(),
) {
    val totalNavios: Int get() = atualizados.size + jaTinham.size + semMatch.size + ambiguos.size
    val temHomonimos: Boolean get() = nomesHomonimos.isNotEmpty()
}

/**
 * Decisão pura do backfill (JVM-testável, sem Firestore): dado o retrato de navios e empresas,
 * decide o que preencher. Assume que o `id` da empresa é a chave estável; o nome é a chave natural
 * mutável que estamos aposentando como FK. Nome homônimo → ambíguo (não resolve).
 */
fun planejarBackfillEmpresaId(
    navios: List<Navio>,
    empresas: List<Empresa>,
): ResultadoBackfillEmpresaId {
    val porNome: Map<String, List<Empresa>> = empresas.groupBy(Empresa::nome)
    val homonimos: Set<String> = porNome.filterValues { it.size > 1 }.keys

    val atualizados = mutableListOf<AtualizacaoEmpresaId>()
    val jaTinham = mutableListOf<String>()
    val semMatch = mutableListOf<String>()
    val ambiguos = mutableListOf<String>()

    navios.forEach { navio ->
        when {
            navio.empresaId.isNotBlank() -> jaTinham += navio.id
            navio.empresa in homonimos -> ambiguos += navio.id
            else -> {
                val match = porNome[navio.empresa]?.singleOrNull()
                if (match != null) {
                    atualizados += AtualizacaoEmpresaId(navio.id, match.id, navio.empresa)
                } else {
                    semMatch += navio.id
                }
            }
        }
    }

    return ResultadoBackfillEmpresaId(
        atualizados = atualizados,
        jaTinham = jaTinham,
        semMatch = semMatch,
        ambiguos = ambiguos,
        nomesHomonimos = homonimos,
    )
}

/**
 * Backfill do `Navio.empresaId` nos docs já existentes do Firestore (ADR-0008, Fase 0). Roda contra
 * a VERDADE (Firestore), não o cache Room. **Idempotente** (pula quem já tem id) e **seguro para
 * homônimos** (pula + reporta). Disparo deliberado (não roda sozinho em todo launch).
 *
 * `aplicar = false` faz **dry-run**: planeja e loga (inclusive homônimos) sem escrever — use para o
 * check de homônimos antes de gravar.
 */
@Singleton
class BackfillEmpresaIdNavio @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    suspend fun executar(aplicar: Boolean = true): ResultadoBackfillEmpresaId {
        val empresas = firestore.collection(COLLECTION_EMPRESAS).get().await()
            .documents.mapNotNull { it.toObject<EmpresaDocumento>()?.toEmpresa(it.id) }
        val navios = firestore.collection(COLLECTION_NAVIOS).get().await()
            .documents.mapNotNull { it.toObject<NavioDocumento>()?.toNavio(it.id) }

        val plano = planejarBackfillEmpresaId(navios, empresas)

        if (plano.temHomonimos) {
            Log.w(
                TAG,
                "homônimos em /empresas (exigem desempate manual): ${plano.nomesHomonimos}; " +
                    "${plano.ambiguos.size} navio(s) ambíguo(s) pulado(s)",
            )
        }

        if (aplicar) {
            plano.atualizados.forEach { atualizacao ->
                firestore.collection(COLLECTION_NAVIOS).document(atualizacao.navioId)
                    .update(CAMPO_EMPRESA_ID, atualizacao.empresaId).await()
            }
        }

        Log.i(
            TAG,
            "backfill empresaId (aplicar=$aplicar): ${plano.atualizados.size} atualizados, " +
                "${plano.jaTinham.size} já tinham, ${plano.semMatch.size} sem match, " +
                "${plano.ambiguos.size} ambíguos (de ${plano.totalNavios} navios)",
        )
        return plano
    }

    private companion object {
        const val TAG = "backfillEmpresaId"
        const val COLLECTION_EMPRESAS = "empresas"
        const val COLLECTION_NAVIOS = "navios"
        const val CAMPO_EMPRESA_ID = "empresaId"
    }
}