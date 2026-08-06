package dev.matheus.fluviapp.ui.viewmodel.helpers.porto

import dev.matheus.fluviapp.ui.states.ErroNomePorto
import dev.matheus.fluviapp.ui.states.FormPortoUiState

/**
 * Validação do formulário de porto — pura e JVM-testável.
 *
 * Dois obrigatórios (nome e localidade) e **um invariante**: o par `(nome, localidade)` é único
 * (ADR-0016 §5). A Localidade resolvia a própria unicidade de graça, pelo código do IBGE; o porto não
 * tem chave natural, então o que o distingue é o nome *dentro do lugar* — dois "Porto Central" em Belém
 * são o mesmo dado duas vezes, e o erro só apareceria depois, na rota que escolheu o porto errado.
 *
 * Duas ressalvas ditas em voz alta, porque são limites e não descuidos:
 *
 * 1. **é verificação de cadastro, não garantia de servidor.** Ela impede o acidente (quem não viu que o
 *    porto já existia), não a corrida (dois ADMs cadastrando o mesmo porto ao mesmo tempo). A paridade
 *    na regra do Firestore é a F8 do ADR-0016, e sai daqui como dívida declarada;
 * 2. **compara portos ativos.** Um porto inativado com o mesmo nome não bloqueia o novo cadastro:
 *    recusar por causa de um registro que a pessoa não pode ver é um beco sem saída — a mensagem
 *    apontaria para algo que não está na tela e não há como consertar.
 */
data class ErrosPorto(
    val nome: ErroNomePorto = ErroNomePorto.NENHUM,
    val localidade: Boolean = false,
) {
    val valido: Boolean get() = !nome.existe && !localidade
}

fun validarPorto(state: FormPortoUiState): ErrosPorto = ErrosPorto(
    nome = erroDoNome(state),
    localidade = state.localidadeId.isBlank(),
)

private fun erroDoNome(state: FormPortoUiState): ErroNomePorto {
    val nome = state.nome.trim()
    if (nome.isBlank()) return ErroNomePorto.OBRIGATORIO

    // O `localidadeId` em branco não chega aqui como duplicidade: sem lugar, não há "dentro do lugar" —
    // o erro é o da localidade, e acusar o nome junto culparia o campo certo pelo problema do outro.
    if (state.localidadeId.isBlank()) return ErroNomePorto.NENHUM

    val jaExiste = state.outrosPortos.any {
        it.ativo &&
            it.localidadeId == state.localidadeId &&
            it.nome.trim().equals(nome, ignoreCase = true)
    }
    return if (jaExiste) ErroNomePorto.DUPLICADO else ErroNomePorto.NENHUM
}