package dev.matheus.fluviapp.extensions

import android.content.Context
import android.widget.Toast

fun Context.toastMessage(mensagem: String) {
    Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
}