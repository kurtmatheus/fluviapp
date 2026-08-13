package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.ui.states.passagem.BilheteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ClienteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ErroDeEmissao
import dev.matheus.fluviapp.ui.states.passagem.PagamentoEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ParticipanteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.PassoDaEmissao
import dev.matheus.fluviapp.ui.states.passagem.VeiculoEmEdicao

/**
 * **A validação da emissão — pura, por passo** ([ADR-0026] D1, e a direção que o estudo do form pedia).
 *
 * Três propriedades que a validação anterior não tinha, e cada uma corresponde a um defeito catalogado:
 *
 * 1. **é função, não método de helper com estado**: entra estado, saem erros. A anterior *escrevia* no
 *    `UiState` enquanto validava, o que a tornava monotônica — erro aceso não apagava sozinho;
 * 2. **é por passo**: o operador é interrompido no passo em que está, e não no fim de um formulário longo;
 * 3. **valida o que a acomodação admite**, em vez de conferir campo por campo com `if` espalhado — a regra
 *    mora no [dev.matheus.fluviapp.domain.passagem.Acomodacao].
 */
fun validarPasso(
    passo: PassoDaEmissao,
    bilhete: BilheteEmEdicao,
    participante: ParticipanteEmEdicao,
    pagamento: PagamentoEmEdicao,
): Set<ErroDeEmissao> = when (passo) {
    // Os passos de **escolha** não validam: a resposta é um toque, e o toque já é a resposta ([ADR-0029]
    // D1). O que existia de validação neles — "acomodação não escolhida" — deixou de ser alcançável quando
    // deixou de haver como avançar sem escolher.
    PassoDaEmissao.Categoria,
    PassoDaEmissao.EscolhaDeAcomodacao,
    PassoDaEmissao.EscolhaDeTipo,
    PassoDaEmissao.EscolhaDeGratuidade,
    PassoDaEmissao.QuantidadeDePessoas,
    PassoDaEmissao.ClasseDoVeiculo,
    PassoDaEmissao.Desfecho,
    -> emptySet()

    is PassoDaEmissao.DadosDoCliente -> validarCliente(passo, participante)

    PassoDaEmissao.DadosDoVeiculo -> validarVeiculo(
        (participante as? ParticipanteEmEdicao.DeVeiculo)?.veiculo ?: VeiculoEmEdicao(),
    )

    PassoDaEmissao.Pagamento -> validarPagamento(bilhete, pagamento)
}

/**
 * Um formulário de pessoa por vez — é o passo 4, que é o mesmo nos dois fluxos ([ADR-0029] D2).
 *
 * **O opcional pula quando está vazio**: o responsável pela retirada de um veículo não é exigido, e um
 * formulário em branco ali é a resposta *"não há"*, não um erro. Preenchido pela metade, sim: meia pessoa no
 * bilhete vira um passageiro que ninguém consegue identificar.
 */
fun validarCliente(
    passo: PassoDaEmissao.DadosDoCliente,
    participante: ParticipanteEmEdicao,
): Set<ErroDeEmissao> {
    val pessoa = when (participante) {
        is ParticipanteEmEdicao.DePassageiro -> participante.pessoas.getOrNull(passo.indice)
        is ParticipanteEmEdicao.DeVeiculo -> participante.responsavel
    }

    if (passo.opcional && (pessoa == null || pessoa.vazio)) return emptySet()
    if (pessoa != null && pessoa.paraCliente() != null) return emptySet()

    return setOf(
        when {
            passo.opcional -> ErroDeEmissao.RESPONSAVEL_INCOMPLETO
            passo.indice == 0 -> ErroDeEmissao.TITULAR_INCOMPLETO
            else -> ErroDeEmissao.ACOMPANHANTE_INCOMPLETO
        },
    )
}

/**
 * Passo 1. A acomodação é obrigatória **só para passageiro** — o veículo não ocupa acomodação de pessoa —, e
 * a gratuidade é cobrada **só onde ela existe**: fora da rede o tipo nem é perguntado.
 */
fun validarBilhete(bilhete: BilheteEmEdicao): Set<ErroDeEmissao> = buildSet {
    if (bilhete.categoria == CategoriaPassagem.PASSAGEIRO && bilhete.acomodacao == null) {
        add(ErroDeEmissao.ACOMODACAO_NAO_ESCOLHIDA)
    }
    if (bilhete.pedeGratuidade && bilhete.gratuidade == null) add(ErroDeEmissao.GRATUIDADE_NAO_ESCOLHIDA)
}

/**
 * Passo 2. O titular é **sempre** exigido no bilhete de pessoa — *"o negócio exige bilhete com portador
 * exceto de veículo"* ([ADR-0028] D3) —, e cada acompanhante **preenchido** tem de estar completo: meia
 * pessoa no bilhete é pior do que nenhuma, porque vira um passageiro que ninguém consegue identificar.
 *
 * O acompanhante **vazio não é erro**: ele é a linha que a tela oferece e o operador não usou.
 */
fun validarParticipante(
    bilhete: BilheteEmEdicao,
    participante: ParticipanteEmEdicao,
): Set<ErroDeEmissao> = when (participante) {
    is ParticipanteEmEdicao.DePassageiro -> buildSet {
        val preenchidas = participante.pessoas.filterNot { it.vazio }
        val titular = participante.pessoas.firstOrNull()

        if (titular == null || titular.vazio || titular.paraCliente() == null) {
            add(ErroDeEmissao.TITULAR_INCOMPLETO)
        }
        if (participante.pessoas.drop(1).any { !it.vazio && it.paraCliente() == null }) {
            add(ErroDeEmissao.ACOMPANHANTE_INCOMPLETO)
        }
        if (preenchidas.size > bilhete.ocupacaoMaxima) add(ErroDeEmissao.EXCEDE_OCUPACAO)
        if (temPessoaRepetida(preenchidas)) add(ErroDeEmissao.PESSOA_REPETIDA)
    }

    is ParticipanteEmEdicao.DeVeiculo -> buildSet {
        addAll(validarVeiculo(participante.veiculo))
        val responsavel = participante.responsavel
        // Responsável **ausente** é a forma normal; responsável **pela metade** é erro.
        if (responsavel != null && !responsavel.vazio && responsavel.paraCliente() == null) {
            add(ErroDeEmissao.RESPONSAVEL_INCOMPLETO)
        }
    }
}

/** O que falta ao veículo é o **tipo** quem diz: carreta não tem modelo a informar, e só moto tem cilindrada. */
fun validarVeiculo(veiculo: VeiculoEmEdicao): Set<ErroDeEmissao> = buildSet {
    if (veiculo.placa.isBlank()) add(ErroDeEmissao.VEICULO_SEM_PLACA)
    val classe = veiculo.classe
    if (classe == null) {
        add(ErroDeEmissao.VEICULO_SEM_CLASSE)
        return@buildSet
    }
    if (classe.exigeModelo && veiculo.modelo.isBlank()) add(ErroDeEmissao.VEICULO_SEM_MODELO)
    if (classe.exigeCilindrada && veiculo.cilindrada.filter { it.isDigit() }.toIntOrNull() == null) {
        add(ErroDeEmissao.VEICULO_SEM_CILINDRADA)
    }
}

/**
 * Duas pessoas com a **mesma credencial** no mesmo bilhete: é a `CLIENTE_REPETIDO` do agregado, cobrada aqui
 * na entrada — onde ainda dá para apontar o campo. A comparação é pela **chave natural**, não pelo nome:
 * dois "José da Silva" são duas pessoas; dois CPFs iguais são a mesma.
 */
private fun temPessoaRepetida(pessoas: List<ClienteEmEdicao>): Boolean {
    val chaves = pessoas.mapNotNull { it.paraCliente()?.chaveNatural }
    return chaves.size != chaves.distinct().size
}

/**
 * Passo 3. Exige **ao menos um lançamento válido** — a emissão é pós-pagamento, então bilhete pago sem
 * entrada registrada é caixa que não fecha.
 *
 * **Menos na gratuidade**, e é por isso que esta função precisa do bilhete: gratuidade é tarifa **zero** por
 * lei, não é um pagamento de valor zero. Exigir lançamento dela seria cobrar de quem não paga; e registrar
 * um lançamento de R$ 0,00 seria pior — poluiria a análise por forma com entradas que nunca existiram.
 *
 * Valor **mal digitado**, esse, é erro em qualquer caso: uma linha marcada com texto que não vira número é
 * dinheiro que ninguém sabe se entrou.
 */
fun validarPagamento(bilhete: BilheteEmEdicao, pagamento: PagamentoEmEdicao): Set<ErroDeEmissao> = buildSet {
    val comValor = pagamento.lancamentos.filter { it.valor.isNotBlank() }
    val ehGratuita = bilhete.tipo == TipoPassagem.GRATUIDADE

    if (comValor.isEmpty() && !ehGratuita) add(ErroDeEmissao.SEM_PAGAMENTO)
    if (comValor.any { it.valorEmReais() == null }) add(ErroDeEmissao.VALOR_INVALIDO)
}