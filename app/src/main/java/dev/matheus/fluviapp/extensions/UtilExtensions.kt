package dev.matheus.fluviapp.extensions

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import dev.matheus.fluviapp.model.cadastro.constantes.Constante
import dev.matheus.fluviapp.util.visualtransformation.CnpjVisualTransformation
import dev.matheus.fluviapp.util.visualtransformation.CpfVisualTransformation
import dev.matheus.fluviapp.util.visualtransformation.PassaporteVisualTransformation

fun visualTransformation(tipoDocumento: String) =
    when (tipoDocumento) {
        Constante.Descricao.CPF.name -> CpfVisualTransformation()
        Constante.Descricao.CNPJ.name -> CnpjVisualTransformation()
        Constante.Descricao.PASSAPORTE.name -> PassaporteVisualTransformation()
        else -> VisualTransformation.None
    }

fun keyboardType(tipoDocumento: String) =
    if (tipoDocumento == Constante.Descricao.PASSAPORTE.name) KeyboardType.Text
    else KeyboardType.Number

fun <T> filtrarPor(
    isChecked: Boolean,
    listNaoFiltrada: List<T>,
    comparador: (T) -> Boolean,
): List<T> {
    val listaFiltrada = mutableListOf<T>()

    if (isChecked) listNaoFiltrada.filterTo(listaFiltrada, comparador) else emptyList()

    return if ((isChecked && listaFiltrada.isEmpty()) || listaFiltrada.isNotEmpty()) {
        listaFiltrada
    } else {
        listNaoFiltrada
    }
}

fun Double?.preencherCampo(): String {
    return this?.let {
        toInt().toString()
    } ?: ""
}