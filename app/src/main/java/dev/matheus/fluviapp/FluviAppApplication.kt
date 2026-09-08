package dev.matheus.fluviapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.matheus.fluviapp.telemetry.CoordenadasDaSessao
import javax.inject.Inject

/**
 * O ponto em que o Hilt monta o grafo — e o único lugar do app com uma injeção **eager**.
 */
@HiltAndroidApp
class FluviAppApplication : Application() {

    /**
     * **As coordenadas de quem opera** ([ADR-0032] D4), pedidas aqui para nascerem.
     *
     * O campo não é lido por ninguém, e é de propósito: o Dagger só cria o que alguém pede, e esta classe
     * existe pelo **efeito** de existir — ela observa a sessão e empurra as coordenadas para a telemetria.
     * Sem esta linha, ela seria um singleton que nunca nasce, e todo evento sairia sem dizer de onde veio.
     */
    @Inject
    lateinit var coordenadasDaSessao: CoordenadasDaSessao
}
