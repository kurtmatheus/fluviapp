package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao

import androidx.annotation.StringRes
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.documento.TipoDocumento

/**
 * Erro do par (tipo, número) de um documento: se há erro e **por quê**. O par existe porque as duas causas
 * pedem mensagens diferentes — "campo obrigatório" e "documento inválido" —, e mandar a primeira quando a
 * segunda é o caso foi o defeito que o estudo do form de passagem já tinha catalogado noutro campo.
 */
data class ErroDocumento(
    val erro: Boolean = false,
    @StringRes val texto: Int = 0,
)

/**
 * Regra pura do número do documento (ADR-0020 D2). Três estados:
 *
 * 1. **sem tipo escolhido** → não se cobra número (o documento inteiro é opcional no domínio);
 * 2. **tipo escolhido e número vazio** → obrigatório;
 * 3. **tipo escolhido e número preenchido** → tem de ser **válido para aquele tipo** — e para CPF e CNPJ
 *    isso significa dígito verificador conferido, não só comprimento.
 *
 * Até aqui só o estado 2 existia: o campo era texto livre e `000.000.000-00` passava. A validação não
 * tinha onde morar porque o tipo era uma String vinda do catálogo; agora mora no tipo.
 *
 * **Tipo desconhecido é erro** (fail-closed): se o valor gravado não corresponde a nenhum `TipoDocumento`,
 * não há regra que o valide, e deixar passar seria aceitar documento não verificado.
 */
fun validarDocumento(tipo: String, numero: String): ErroDocumento = when {
    tipo.isBlank() -> ErroDocumento()
    numero.isBlank() -> ErroDocumento(erro = true, texto = R.string.error_camp_obrig)
    TipoDocumento.de(tipo)?.validar(numero) == true -> ErroDocumento()
    else -> ErroDocumento(erro = true, texto = R.string.error_documento_invalido)
}