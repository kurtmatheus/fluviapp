package dev.matheus.fluviapp.ui.viewmodel.helpers.empresa

import dev.matheus.fluviapp.domain.documento.TipoDocumento
import dev.matheus.fluviapp.ui.states.FormEmpresaUiState

/**
 * Validação do formulário de empresa — pura e JVM-testável ((state) -> resultado, sem mutar estado).
 *
 * **Entre os campos de texto, só o nome é obrigatório** (decisão de 2026-09-03): razão social, CNPJ,
 * endereço e telefone não pesam na organização nem na gestão das informações por enquanto — e destes,
 * endereço e telefone já eram opcionais desde sempre.
 *
 * Opcional **não é livre**, e é aí que o CNPJ se separa dos outros três: vazio passa, mas preenchido
 * continua tendo de ser CNPJ de verdade. Dispensar a obrigação não é dispensar o dígito verificador —
 * um campo que aceita qualquer coisa registra um documento que não existe, o que é pior do que não
 * registrar nenhum.
 */
data class ErrosEmpresa(
    val nome: Boolean = false,
    val cnpj: Boolean = false,
    val atuacoes: Boolean = false,
) {
    val valido: Boolean get() = !nome && !cnpj && !atuacoes
}

fun validarEmpresa(state: FormEmpresaUiState): ErrosEmpresa = ErrosEmpresa(
    nome = state.nome.isBlank(),
    cnpj = state.cnpj.isNotBlank() && !cnpjValido(state.cnpj),
    // **Ao menos uma atuação** — o domínio (§3.1) diz que a empresa não tem campo de segmento nem de
    // tipo: *o que ela faz vive nas atuações*. Uma parte sem nenhuma não é cadastro incompleto por
    // capricho — ela não pode ser escolhida em lugar nenhum: não tem cargo (§6.1), não abre seção
    // (ADR-0016 §2) e não recebe concessão (§7). Cadastrá-la seria criar uma parte que não age.
    //
    // É a única obrigação que sobrevive ao relaxamento acima, e sobrevive por não ser da mesma espécie:
    // razão social e CNPJ descrevem a parte, e uma descrição pobre ainda descreve; a atuação a habilita,
    // e sem ela não há o que habilitar.
    atuacoes = state.atuacoes.isEmpty(),
)

/**
 * Valida CNPJ: 14 dígitos + dígitos verificadores (mód. 11).
 *
 * A regra passou a morar em [TipoDocumento] (ADR-0020 D2), que é onde todo documento é validado. Esta
 * função vira um apelido — a duplicata existia porque o CNPJ da empresa já era validado de verdade
 * enquanto o CPF do passageiro, vindo do catálogo como String, não era validado de forma alguma.
 */
fun cnpjValido(digitos: String): Boolean = TipoDocumento.CNPJ.validar(digitos)
