package br.com.gruponaveg.util

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


class CriptografiaUtil {

    companion object {
        @OptIn(ExperimentalEncodingApi::class)
        @Synchronized
        fun String.encrypt(): String {
            val md: MessageDigest?
            try {
                md = MessageDigest.getInstance("SHA")
                md.update(this.toByteArray(StandardCharsets.UTF_8))
            } catch (e: NoSuchAlgorithmException) {
                throw NoSuchAlgorithmException(e.message)
            }

            return Base64.encode(md.digest())
        }
    }
}