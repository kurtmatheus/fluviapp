package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.domain.cliente.Cliente
import dev.matheus.fluviapp.domain.passagem.Passagem
import dev.matheus.fluviapp.domain.passagem.PassagemDePassageiro
import dev.matheus.fluviapp.domain.passagem.PassagemDeVeiculo
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
 * ### A identificação pode faltar, e isso é uma decisão do servidor chegando à tela
 *
 * O pool é PII, e a leitura dele é **recortada pela assinatura** (ADR-0018 D3): quem embarca só lê a pessoa
 * se a própria agência já a tiver atendido. Como validar embarque é um eixo aberto a *qualquer papel
 * conhecido* — quem está na doca valida, mesmo sem ter vendido —, existe um caso legítimo em que o operador
 * **não pode** ver o nome de quem vendeu outra agência.
 *
 * A junção não contorna isso: devolve [IDENTIFICACAO_INDISPONIVEL], que é diferente de *"não carregou ainda"*
 * e diferente de *"não tem nome"*. O bilhete continua conferível pelo que não é PII — número, travessia,
 * partida e status —, e **se a conferência por nome deve ou não atravessar agências é decisão de negócio**,
 * não coisa que um mapper resolva afrouxando a regra.
 */
fun Passagem.paraConferencia(referencias: ReferenciasDaPassagem): ConferenciaDeEmbarque =
    ConferenciaDeEmbarque(
        numero = "#$numero",
        identificacao = identificacaoCom(referencias),
        travessia = referencias.rota?.rotuloCom(referencias.portosPorId).orEmpty(),
        partida = partidaCom(referencias.viagem),
        status = metadados.status.rotulo(),
    )

/**
 * Quem embarca, por categoria — e o `when` exaustivo é de propósito: quando a **carga** existir, ela vai
 * aparecer aqui como erro de compilação, e não como um bilhete que se identifica sozinho por omissão.
 */
private fun Passagem.identificacaoCom(referencias: ReferenciasDaPassagem): String = when (this) {
    is PassagemDePassageiro -> titularId
        ?.let { referencias.clientesPorId[it]?.nome }
        ?.takeIf { it.isNotBlank() }
        ?: IDENTIFICACAO_INDISPONIVEL

    // A placa **é** a identificação: ela está no documento do veículo, e o responsável pela retirada é
    // opcional por regra de negócio — bilhete de veículo sem ninguém nomeado é a forma normal.
    is PassagemDeVeiculo -> referencias.veiculosPorId[veiculoId]?.placa
        ?.takeIf { it.isNotBlank() }
        ?: IDENTIFICACAO_INDISPONIVEL
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

/** Não é "sem nome" nem "ainda carregando": é *não posso ver* — ver o KDoc da junção. */
const val IDENTIFICACAO_INDISPONIVEL = "—"

private val DIA_E_MES: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")