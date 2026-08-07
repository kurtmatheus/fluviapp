package dev.matheus.fluviapp.ui.theme

import androidx.annotation.DrawableRes
import dev.matheus.fluviapp.R

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
 * **A chave é o nome, e desde a F6.3 ele vem da empresa do vínculo** (antes vinha do enum `Agencia`, que
 * morreu com o cadastro por agência). O mapa não ficou mais frouxo por isso: ele sempre foi um bundle
 * casado por rótulo — o que mudou é de onde o rótulo vem.
 *
 * `null` significa **sem marca própria**, e é o caso comum: quem não tem marca emite com a marca do
 * **FluviApp** — o bilhete nunca sai sem assinatura.
 */
fun marcaDaAgencia(agencia: String?): MarcaAgencia? = when (agencia?.trim()?.uppercase()) {
    "MATRIZ" -> MarcaAgencia(
        logoTopo = R.drawable.agencia_matriz_logo1,
        marcaDagua = R.drawable.agencia_matriz_logo2,
    )

    else -> null
}
