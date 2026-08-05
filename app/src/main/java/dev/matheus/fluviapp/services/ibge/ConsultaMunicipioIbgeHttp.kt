package dev.matheus.fluviapp.services.ibge

import android.util.Log
import dev.matheus.fluviapp.domain.localidade.Uf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A única chamada HTTP a terceiro do app: `servicedados.ibge.gov.br`, público e sem autenticação.
 *
 * ### Sem cliente HTTP novo, e por quê
 *
 * `HttpURLConnection` + `org.json` já vêm na plataforma. É **um** GET, de resposta pequena, sem
 * autenticação, sem interceptadores e sem reuso previsto — trazer Retrofit/OkHttp para isso adicionaria
 * dependência, configuração e superfície de build para resolver um problema que a biblioteca padrão
 * resolve em vinte linhas. Se um dia houver um segundo endpoint com autenticação, aí a conta muda.
 *
 * ### A forma da resposta
 *
 * ```json
 * { "id": 1501402, "nome": "Belém",
 *   "microrregiao": { "mesorregiao": { "UF": { "sigla": "PA" } } } }
 * ```
 *
 * A UF aparece **duas vezes** na resposta (por microrregião e por região imediata). Lê-se a primeira e,
 * se ela faltar, a segunda — as duas dizem a mesma coisa, e depender de uma só quebraria à toa se o IBGE
 * reorganizar um dos recortes.
 *
 * Qualquer desvio — HTTP != 200, JSON diferente, sigla que não é UF — vira
 * [ResultadoConsultaIbge.Indisponivel] ou [ResultadoConsultaIbge.NaoEncontrado], **nunca** exceção: isto
 * é preenchimento, e preenchimento que derruba o formulário é pior que preenchimento nenhum.
 */
@Singleton
class ConsultaMunicipioIbgeHttp @Inject constructor() : ConsultaMunicipioIbge {

    override suspend fun consultar(codigoIbge: String): ResultadoConsultaIbge = withContext(Dispatchers.IO) {
        val codigo = codigoIbge.filter(Char::isDigit)
        if (codigo.length != TAMANHO_CODIGO) return@withContext ResultadoConsultaIbge.NaoEncontrado

        var conexao: HttpURLConnection? = null
        try {
            conexao = (URL("$BASE/$codigo").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }

            when (conexao.responseCode) {
                HttpURLConnection.HTTP_OK -> interpretar(conexao.inputStream.bufferedReader().use { it.readText() })
                // O IBGE devolve 404 para código inexistente — a única resposta que fala sobre o CÓDIGO.
                HttpURLConnection.HTTP_NOT_FOUND -> ResultadoConsultaIbge.NaoEncontrado
                else -> ResultadoConsultaIbge.Indisponivel
            }
        } catch (e: Exception) {
            Log.w(TAG, "consultar($codigo): ${e.message}")
            ResultadoConsultaIbge.Indisponivel
        } finally {
            conexao?.disconnect()
        }
    }

    private fun interpretar(corpo: String): ResultadoConsultaIbge = try {
        // Corpo vazio ou `[]` acontece quando o código não é de município: o serviço responde 200 sem dado.
        if (corpo.isBlank() || corpo.trimStart().startsWith("[")) {
            ResultadoConsultaIbge.NaoEncontrado
        } else {
            val raiz = JSONObject(corpo)
            val municipio = raiz.optString("nome").orEmpty()
            val uf = Uf.de(siglaDaUf(raiz))
            if (municipio.isBlank() || uf == null) {
                ResultadoConsultaIbge.NaoEncontrado
            } else {
                ResultadoConsultaIbge.Encontrado(municipio, uf)
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "interpretar: ${e.message}")
        ResultadoConsultaIbge.Indisponivel
    }

    /** Os dois caminhos até a sigla; o segundo é rede de segurança, não redundância acidental. */
    private fun siglaDaUf(raiz: JSONObject): String? =
        raiz.optJSONObject("microrregiao")
            ?.optJSONObject("mesorregiao")
            ?.optJSONObject("UF")
            ?.optString("sigla")
            ?.takeIf { it.isNotBlank() }
            ?: raiz.optJSONObject("regiao-imediata")
                ?.optJSONObject("regiao-intermediaria")
                ?.optJSONObject("UF")
                ?.optString("sigla")
                ?.takeIf { it.isNotBlank() }

    private companion object {
        const val TAG = "consultaIbge"
        const val BASE = "https://servicodados.ibge.gov.br/api/v1/localidades/municipios"
        const val TIMEOUT_MS = 8_000
        const val TAMANHO_CODIGO = 7
    }
}