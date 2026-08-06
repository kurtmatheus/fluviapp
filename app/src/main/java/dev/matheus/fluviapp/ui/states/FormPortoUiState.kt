package dev.matheus.fluviapp.ui.states

import androidx.annotation.StringRes
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.porto.Porto

/**
 * Estado do formulário de porto — puro (só dados + flags), no molde do ADR-0006.
 *
 * Duas escolhas merecem nome:
 *
 * - a localidade vive aqui como **id** ([localidadeId]), e não como o texto do dropdown. É o vínculo que
 *   será persistido (ADR-0016 §5), e mantê-lo já resolvido evita o passo que a Embarcação ainda faz —
 *   procurar, na hora de salvar, qual empresa tinha aquele nome. O rótulo exibido sai de [localidades];
 * - [outrosPortos] são os portos **já cadastrados menos este**, e existem para a unicidade
 *   `(nome, localidade)` do §5. Vêm no estado porque a validação é pura: ela não consulta nada, decide
 *   sobre o que lhe deram.
 */
data class FormPortoUiState(
    val titulo: Int = R.string.subtitle_cadastrar_novo_porto,

    val nome: String = "",
    val erroNome: ErroNomePorto = ErroNomePorto.NENHUM,

    val localidadeId: String = "",
    val isLocalidadeError: Boolean = false,

    val localidades: List<LocalidadeOpcao> = emptyList(),
    val outrosPortos: List<Porto> = emptyList(),

    val isProcessing: Boolean = false,
) {

    /** O que o dropdown mostra como escolhido — resolvido do id, e vazio enquanto ninguém escolheu. */
    val rotuloLocalidade: String
        get() = localidades.firstOrNull { it.id == localidadeId }?.rotulo.orEmpty()
}

/**
 * Uma localidade **como opção de escolha**: o id que será gravado e o rótulo que a pessoa lê
 * (ADR-0019 — DTO por caso de uso, e este caso é escolher). O formulário não precisa da UF nem do código
 * do IBGE, e não os carrega.
 */
data class LocalidadeOpcao(
    val id: String,
    val rotulo: String,
)

/**
 * Por que o erro do nome é **tipo** e não `Boolean`: o campo tem duas maneiras de estar errado, e elas
 * pedem frases diferentes. "Campo obrigatório" no lugar de "já existe um porto com este nome nesta
 * localidade" manda a pessoa preencher o que ela acabou de preencher.
 *
 * [NENHUM] carrega a mensagem padrão porque a tela precisa de um `@StringRes` sempre — e nunca a exibe,
 * já que o campo só mostra a mensagem quando [existe].
 */
enum class ErroNomePorto(@StringRes val mensagem: Int) {
    NENHUM(R.string.error_camp_obrig),
    OBRIGATORIO(R.string.error_camp_obrig),
    DUPLICADO(R.string.error_porto_duplicado);

    val existe: Boolean get() = this != NENHUM
}