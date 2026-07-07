package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import dev.matheus.fluviapp.extensions.formatarTimeStamp
import dev.matheus.fluviapp.model.passagem.PassagemDigital
import dev.matheus.fluviapp.services.repository.cadastro.passagem.PassagemDigitalRepository
import dev.matheus.fluviapp.ui.states.passagem.DetalhesPassagemState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime

class PassagemDigitalHelper(
    private val uiState: MutableStateFlow<DetalhesPassagemState>,
    private val passagemDigitalRepository: PassagemDigitalRepository,
    private val idPassagem: String
) {

    private fun verificarExistenciaPassagemDigital(): Boolean {
        return runBlocking { passagemDigitalRepository.obterPorPassagem(idPassagem) != null }
    }

    private fun salvarPassagemDigital(caminho: String) {
        val passagemDigital = PassagemDigital(
            idPassagem = idPassagem,
            caminho = caminho
        )
        runBlocking { passagemDigitalRepository.salvar(passagemDigital) }

    }

    fun processaImagemDigital(
        context: Context,
        imageBitmap: ImageBitmap
    ) {
        val arquivo = if (!verificarExistenciaPassagemDigital()) {
            val file = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "passagem_${LocalDateTime.now().formatarTimeStamp()}.png"
            )
            val fileOutputStream = FileOutputStream(file)


            try {
                val androidBitmap = imageBitmap.asAndroidBitmap()
                androidBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                fileOutputStream.flush()
                fileOutputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            salvarPassagemDigital(caminho = file.path)
            file
        } else {
            val caminho = runBlocking {
                passagemDigitalRepository.obterPorPassagem(idPassagem)!!.caminho
            }
            File(caminho)
        }

        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.provider", arquivo
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/png"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar imagem"))

    }
}
