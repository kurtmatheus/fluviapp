package br.com.gruponaveg.util

import android.content.Context
import java.util.Properties

class Fachada {
    companion object {
        fun getProperties(context: Context, properties: String): String? {
            val prop = Properties()
            try {
                val inputStream = context.assets.open("app.properties")
                prop.load(inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
            return prop.getProperty(properties)
        }
    }
}