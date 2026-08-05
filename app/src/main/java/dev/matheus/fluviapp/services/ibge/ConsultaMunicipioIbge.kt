package dev.matheus.fluviapp.services.ibge

import dev.matheus.fluviapp.domain.localidade.Uf

/**
 * Consulta o município pelo **código do IBGE** — para *preencher* o formulário, nunca para governá-lo.
 *
 * ### O que esta porta é, e o que ela não é
 *
 * Não é fonte de dado. O que a plataforma grava é a `Localidade` dela, no Firestore (ADR-0017: mobile
 * first, Firestore-only). O IBGE entra como **ajuda de digitação**: a pessoa informa o código, o app
 * devolve o município e a UF já escritos como o órgão os escreve — com acento, sem abreviação, sem duas
 * grafias para a mesma cidade. É exatamente a divergência de grafia que o ADR-0016 §5 tentava impedir com
 * unicidade, resolvida na origem.
 *
 * Por isso a consulta **nunca bloqueia o cadastro**: sem rede, com o serviço fora do ar ou com um código
 * que ele não conhece, a pessoa digita o município e escolhe a UF à mão, e o formulário salva igual. Um
 * cadastro que depende da disponibilidade de terceiro é um cadastro que para quando o terceiro para.
 *
 * ### Por que porta, e não chamada direta no ViewModel
 *
 * Pelo mesmo motivo de todo o resto do app: o VM depende da interface e os testes usam um fake — a lógica
 * de "preencheu / não achou / falhou" se prova em JVM, sem rede e sem esperar timeout de ninguém.
 */
interface ConsultaMunicipioIbge {

    /** Consulta o código; ver [ResultadoConsultaIbge] para os três desfechos possíveis. */
    suspend fun consultar(codigoIbge: String): ResultadoConsultaIbge
}

/**
 * Os três desfechos, separados de propósito: *achou*, *não existe* e *não deu para saber*.
 *
 * Colapsar os dois últimos num `null` seria dizer à pessoa que o código está errado quando o problema é a
 * rede — a mesma confusão que, no login, acusava alguém de não ser da casa quando o Firestore ainda não
 * havia respondido (ADR-0017 D1).
 */
sealed interface ResultadoConsultaIbge {

    /** O município do código, como o IBGE o escreve. */
    data class Encontrado(val municipio: String, val uf: Uf) : ResultadoConsultaIbge

    /** O serviço respondeu, e este código não é de município nenhum. */
    data object NaoEncontrado : ResultadoConsultaIbge

    /** Não foi possível perguntar (sem rede, timeout, resposta ilegível). Não diz nada sobre o código. */
    data object Indisponivel : ResultadoConsultaIbge
}