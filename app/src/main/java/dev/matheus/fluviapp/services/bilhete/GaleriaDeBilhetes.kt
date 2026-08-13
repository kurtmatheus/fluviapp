package dev.matheus.fluviapp.services.bilhete

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * **Onde o bilhete mora depois de virar imagem** ([ADR-0017] D5).
 *
 * ### O nome do arquivo é a decisão inteira
 *
 * `bilhete_<idPassagem>.png`. O caminho antigo usava `passagem_<timestamp>.png` — e era **o timestamp** que
 * obrigava uma tabela do Room a existir: o arquivo não era localizável a partir do id, então a linha do banco
 * era o único mapa `idPassagem → caminho`. Com o nome derivado, **procurar é consultar o MediaStore**, e o
 * índice perde a função. Destino e nome mudaram juntos de propósito: trocar só o destino deixaria a
 * reabertura cega.
 *
 * ### Galeria, e não o diretório privado do app
 *
 * O bilhete é do **passageiro**. Em `getExternalFilesDir` ele ficava num canto que só o app enxerga e que
 * some com a desinstalação; na galeria ele aparece onde as pessoas procuram foto — e sobrevive ao app.
 *
 * ### Nenhuma permissão é pedida, e isso é ganho e não descuido
 *
 * Da API 29 em diante, gravar na coleção de imagens do próprio app **não exige permissão**: o `MediaStore`
 * dá a URI e o sistema resolve o resto. O `WRITE_EXTERNAL_STORAGE` que o manifesto ainda declara não vale
 * desde então — é resíduo do caminho antigo, e sai com ele.
 */
@Singleton
class GaleriaDeBilhetes @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * O bilhete já gravado, se houver — **procurar antes de gerar** (decisão do analista, 2026-08-13).
     *
     * Reabrir um bilhete é o caso comum do balcão ("perdi o meu"), e regenerar sempre desenharia de novo o
     * que já existe. Regenerar continua sendo **legítimo** e é o que acontece quando isto devolve `null`: o
     * arquivo é cache de conveniência, e o dado de origem está no Firestore.
     */
    fun procurar(idPassagem: String): Uri? {
        val nome = nomeDoArquivo(idPassagem)

        val projecao = arrayOf(MediaStore.Images.Media._ID)
        val selecao = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"

        return runCatching {
            context.contentResolver.query(
                colecaoDeImagens(),
                projecao,
                selecao,
                arrayOf(nome),
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                Uri.withAppendedPath(colecaoDeImagens(), id.toString())
            }
        }.getOrNull()
    }

    /**
     * Grava o bilhete e devolve a URI. **Salvar acontece ao ver** — decisão do analista: pré-visualizar e ter
     * o arquivo são o mesmo ato, então não há um segundo gesto de "salvar" a ser esquecido no balcão.
     *
     * Regravar por cima do que já existe é o comportamento correto quando o mesmo bilhete é reaberto e
     * regenerado: o conteúdo é o mesmo documento, e dois arquivos para uma passagem seriam duas verdades.
     */
    fun salvar(idPassagem: String, imagem: Bitmap): Uri? = runCatching {
        procurar(idPassagem)?.let { existente ->
            context.contentResolver.openOutputStream(existente, "wt")?.use { saida ->
                imagem.compress(Bitmap.CompressFormat.PNG, QUALIDADE, saida)
            }
            return@runCatching existente
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) salvarPeloMediaStore(idPassagem, imagem)
        else salvarNoDiretorioPublico(idPassagem, imagem)
    }.getOrNull()

    /**
     * A URI para **compartilhar** ([Intent.ACTION_SEND]) — o gesto que entrega o bilhete ao passageiro, e que
     * continua existindo por decisão do analista.
     *
     * Vem do próprio MediaStore quando o arquivo está na galeria; o `FileProvider` só entra no caminho
     * antigo (API 28 e abaixo), onde o arquivo é um `File` de verdade.
     */
    fun paraCompartilhar(idPassagem: String): Uri? = procurar(idPassagem) ?: arquivoLegado(idPassagem)

    private fun salvarPeloMediaStore(idPassagem: String, imagem: Bitmap): Uri? {
        val valores = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, nomeDoArquivo(idPassagem))
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$PASTA")
        }

        val uri = context.contentResolver.insert(colecaoDeImagens(), valores) ?: return null
        context.contentResolver.openOutputStream(uri)?.use { saida ->
            imagem.compress(Bitmap.CompressFormat.PNG, QUALIDADE, saida)
        }
        return uri
    }

    /** Caminho da API 28 e abaixo, onde a galeria ainda é um diretório e a permissão ainda existe. */
    private fun salvarNoDiretorioPublico(idPassagem: String, imagem: Bitmap): Uri? {
        val pasta = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            PASTA,
        ).apply { if (!exists()) mkdirs() }

        val arquivo = File(pasta, nomeDoArquivo(idPassagem))
        FileOutputStream(arquivo).use { saida -> imagem.compress(Bitmap.CompressFormat.PNG, QUALIDADE, saida) }

        return FileProvider.getUriForFile(context, "${context.packageName}.provider", arquivo)
    }

    private fun arquivoLegado(idPassagem: String): Uri? {
        val arquivo = File(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), PASTA),
            nomeDoArquivo(idPassagem),
        )
        if (!arquivo.exists()) return null
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", arquivo)
    }

    private fun colecaoDeImagens(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

    /** `bilhete_<idPassagem>.png` — derivado do id, que é o que dispensa o índice local. */
    private fun nomeDoArquivo(idPassagem: String) = "bilhete_$idPassagem.png"

    private companion object {
        const val PASTA = "FluviApp"
        const val QUALIDADE = 100
    }
}