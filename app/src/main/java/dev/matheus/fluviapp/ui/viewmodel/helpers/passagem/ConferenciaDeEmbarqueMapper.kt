package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.domain.cliente.Cliente
import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.Passagem
import dev.matheus.fluviapp.domain.passagem.PassagemDePassageiro
import dev.matheus.fluviapp.domain.passagem.PassagemDeVeiculo
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.domain.rota.Rota
import dev.matheus.fluviapp.domain.veiculo.Veiculo
import dev.matheus.fluviapp.domain.viagem.Viagem
import dev.matheus.fluviapp.domain.viagem.formatarHora
import dev.matheus.fluviapp.domain.viagem.rotulo
import dev.matheus.fluviapp.ui.states.passagem.ConferenciaDeEmbarque
import dev.matheus.fluviapp.ui.viewmodel.helpers.inicio.rotuloCom
import java.time.format.DateTimeFormatter

/**
 * **As referências que a passagem aponta, já carregadas** — a entrada da junção ([ADR-0025] D3).
 *
 * O agregado guarda **só ids** (ADR-0023 D8): não há nome de cliente, de porto nem de embarcação dentro do
 * bilhete. Resolver esses ids é o que o fim do snapshot cobra, e o preço se paga aqui — **na montagem**, não
 * em cada tela.
 *
 * Existir como tipo, e não como cinco parâmetros soltos, é o que faz a assinatura da junção parar de crescer
 * a cada campo que uma projeção nova precisar.
 */
data class ReferenciasDaPassagem(
    val clientesPorId: Map<String, Cliente> = emptyMap(),
    val veiculosPorId: Map<String, Veiculo> = emptyMap(),
    val viagem: Viagem? = null,
    val rota: Rota? = null,
    /** Id do porto → rótulo já pronto ("Porto de Val-de-Cães · Belém/PA"). */
    val portosPorId: Map<String, String> = emptyMap(),
)

/**
 * `Passagem` + referências → [ConferenciaDeEmbarque]. **Função pura**, e é isso que este arquivo defende.
 *
 * ### Por que a tradução não busca
 *
 * O padrão anterior era um mapper com repositório dentro: `passagens.map { mapper.map(it) }` fazia **duas
 * buscas por volta** sem que a linha desse qualquer sinal disso. Quatro propriedades mudam quando a busca sai:
 *
 * 1. **testar deixa de exigir fake** — entram objetos, sai objeto;
 * 2. **acaba a contaminação por `suspend`**: traduzir não é buscar;
 * 3. **o carregamento fica visível** — não se trata de ficar mais rápido, e sim de **o que for lento ser
 *    lento à vista**;
 * 4. **a dependência volta a apontar para dentro**: o domínio para de importar a camada de dados.
 *
 * Quem carrega é quem chama. O precedente é o `ContagemPassagensMapper`, que já era uma classe com
 * repositório **envolvendo** uma função pura — o híbrido que agora é regra.
 *
 * ### Esta junção não toca em dado pessoal, e isso é decisão, não limitação
 *
 * *"O embarque confere bilhete e não pessoa"* (analista, 2026-08-13). Então aqui não entram nome nem placa —
 * e, com eles, **não entram os pools**. As referências que esta projeção usa são as da **travessia**: viagem,
 * rota e portos, todas coleções pequenas que a sessão já tem em memória.
 *
 * A alternativa que isso rejeita tinha um defeito de fundo: como a leitura do pool é recortada pela
 * assinatura e validar embarque é aberto a qualquer papel conhecido, mostrar o nome exigiria **afrouxar a
 * proteção da PII** ou aceitar uma conferência que funciona só para bilhete da própria agência. A decisão
 * corta o nó — e economiza uma leitura de dado pessoal por embarque.
 */
fun Passagem.paraConferencia(referencias: ReferenciasDaPassagem): ConferenciaDeEmbarque =
    ConferenciaDeEmbarque(
        numero = "#$numero",
        bilhete = descricaoDoBilhete(),
        travessia = referencias.rota?.rotuloCom(referencias.portosPorId).orEmpty(),
        partida = partidaCom(referencias.viagem),
        status = metadados.status.rotulo(),
    )

/**
 * **O que foi vendido**, por categoria — e o `when` exaustivo é de propósito: quando a **carga** existir, ela
 * vai aparecer aqui como erro de compilação, e não como um bilhete que se descreve sozinho por omissão.
 *
 * Na doca, o que decide é o espaço: "Suíte" e "Rede" não embarcam no mesmo lugar. O tipo tarifário entra
 * **só quando não é inteira**, porque meia e gratuidade são o que alguém eventualmente confere contra um
 * documento — e repetir "Inteira" em todo bilhete seria ruído.
 */
private fun Passagem.descricaoDoBilhete(): String = when (this) {
    is PassagemDePassageiro -> listOfNotNull(
        acomodacao.rotulo,
        tipo.rotulo().takeIf { tipo != TipoPassagem.INTEIRA },
    ).joinToString(" · ")

    is PassagemDeVeiculo -> CategoriaPassagem.VEICULO.rotulo
}

/**
 * "Terça-feira, 18/08 · 18:00": o **dia da semana** vem da viagem (é o que ela é — uma saída semanal), a
 * **data** vem da ocorrência (é ela que diz *qual* das terças), e a hora vem da viagem.
 *
 * Sem a viagem carregada sobra a data, que é o que o bilhete carrega por si. É degradação, não erro: uma
 * viagem inativada continua tendo bilhetes emitidos apontando para ela.
 */
private fun Passagem.partidaCom(viagem: Viagem?): String {
    val data = ocorrencia.data.format(DIA_E_MES)
    val diaSemana = ocorrencia.data.dayOfWeek.rotulo
    val hora = viagem?.let { formatarHora(it.horaMin) }

    return listOfNotNull("$diaSemana, $data", hora).joinToString(" · ")
}

private val DIA_E_MES: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")