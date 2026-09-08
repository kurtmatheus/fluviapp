package dev.matheus.fluviapp.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.matheus.fluviapp.domain.operacoes.Perfil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Onde mora **qual perfil está ativo** ([ADR-0032] D5): o da plataforma ou o da empresa.
 *
 * ### A mesma forma de antes, outro eixo
 *
 * Isto era o `EscolhaDeVinculo`, que guardava *em nome de qual empresa opero* (ADR-0016 §6, F6.4). A D5
 * descartou a escolha entre empresas, e o que sobrou foi a **forma** — preferência no DataStore — servindo
 * a outra pergunta. Nada aqui é novidade técnica; a novidade é o que a preferência significa.
 *
 * ### Por que DataStore, e não memória
 *
 * Porque a alternativa é perguntar de novo a cada abertura do app. O perfil é de quem opera e muda pouco —
 * é preferência de uso, da mesma natureza do tema —, e o lugar disso no projeto já existe (ADR-0005, e o
 * resíduo local do ADR-0017 §7.1).
 *
 * ### Por que ela é preferência, e nunca credencial
 *
 * O valor guardado aqui **não concede nada**, e não é só uma frase: o servidor não sabe que existe troca de
 * perfil — ele lê papel e cargo do mesmo uid e concede a união (ver [Perfil]). Quem decide se a preferência
 * vale é o domínio, revalidando-a contra papel e vínculo a cada leitura (`resolverPerfilAtivo`). Quem
 * perdeu o vínculo volta ao perfil de plataforma sem que ninguém precise lembrar de limpar o aparelho.
 *
 * Valor ilegível — chave escrita por uma versão que não existe mais — vira `null`, que é "não escolheu": o
 * fail-closed aqui é voltar ao padrão, não travar a entrada.
 */
@Singleton
class PerfilAtivo @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** `null` quando ninguém escolheu — que é o estado normal de quem tem um perfil só. */
    fun observarPerfil(): Flow<Perfil?> =
        context.dataStore.data.map { Perfil.de(it[PreferencesKey.PERFIL_ATIVO]) }

    suspend fun guardar(perfil: Perfil) {
        context.dataStore.edit { it[PreferencesKey.PERFIL_ATIVO] = perfil.name }
    }

    suspend fun limpar() {
        context.dataStore.edit { it.remove(PreferencesKey.PERFIL_ATIVO) }
    }
}
