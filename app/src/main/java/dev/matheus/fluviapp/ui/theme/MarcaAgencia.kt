package dev.matheus.fluviapp.ui.theme

import androidx.annotation.DrawableRes
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.Agencia

/**
 * Identidade visual da agência emissora no bilhete (ADR-0015 §5).
 *
 * Duas peças, com papéis diferentes: o [logoTopo] assina o documento (quem vendeu) e a [marcaDagua]
 * ocupa o fundo, que é o lugar de marca sem competir com o conteúdo.
 */
data class MarcaAgencia(
    @DrawableRes val logoTopo: Int,
    @DrawableRes val marcaDagua: Int,
)

/**
 * Mapa **bundle fixo** agência → marca (ADR-0015 §5): drawables no app, não Storage. O Storage por
 * agência entra quando houver agência cadastrando o próprio logo — hoje seria infraestrutura para um
 * dado que ninguém edita.
 *
 * `null` significa **sem marca própria** e é o caso comum, não a exceção: a agência coringa
 * ([Agencia.AUTONOMO]) e todas as agências que ainda são texto livre no cadastro caem aqui. Quem não
 * tem marca emite com a marca do **FluviApp** — o bilhete nunca sai sem assinatura.
 */
fun marcaDaAgencia(agencia: String?): MarcaAgencia? = when (Agencia.de(agencia?.trim()?.uppercase())) {
    Agencia.MATRIZ -> MarcaAgencia(
        logoTopo = R.drawable.agencia_matriz_logo1,
        marcaDagua = R.drawable.agencia_matriz_logo2,
    )
    // AUTONOMO (a coringa) e desconhecidas: FluviApp.
    else -> null
}
