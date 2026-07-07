package br.com.gruponaveg.extensions

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast

fun Context.toastMessage(mensagem: String) {
    Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
}

fun Context.temConexao(): Boolean {
    val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return connectivityManager.activeNetwork?.let { network ->
        connectivityManager.getNetworkCapabilities(network)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } ?: false
}