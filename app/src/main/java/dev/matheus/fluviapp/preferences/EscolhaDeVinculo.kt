package dev.matheus.fluviapp.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Onde mora a **escolha de contexto**: em nome de qual empresa a pessoa está operando (ADR-0016 §6, F6.4).
 *
 * ### Por que DataStore, e não memória
 *
 * Porque a alternativa é perguntar de novo a cada abertura do app. A escolha é de quem opera e muda
 * pouco — é preferência de uso, da mesma natureza do tema —, e o lugar disso no projeto já existe
 * (ADR-0005, e o resíduo local do ADR-0017 §7.1).
 *
 * ### Por que ela é preferência, e nunca credencial
 *
 * O id guardado aqui **não concede nada**. Quem decide se ele vale é o domínio, revalidando-o contra os
 * vínculos atuais a cada leitura (`resolverVinculoAtivo`). É isso que impede o pior defeito possível
 * neste ponto: alguém perder o vínculo com uma empresa e continuar operando em nome dela porque o
 * aparelho ainda tinha o id gravado. Um dado que não é consultado como autoridade não precisa de
 * invalidação ativa — ele simplesmente deixa de casar.
 */
@Singleton
class EscolhaDeVinculo @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** `null` quando ninguém escolheu ainda — que é o estado normal de quem tem um vínculo só. */
    suspend fun empresaEscolhida(): String? =
        context.dataStore.data.first()[PreferencesKey.EMPRESA_ATIVA]?.takeIf { it.isNotBlank() }

    suspend fun guardar(empresaId: String) {
        context.dataStore.edit { it[PreferencesKey.EMPRESA_ATIVA] = empresaId }
    }

    suspend fun limpar() {
        context.dataStore.edit { it.remove(PreferencesKey.EMPRESA_ATIVA) }
    }
}