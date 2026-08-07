package com.clibeats.data.provider.resolver.cipher

import java.net.URLDecoder

data class ParsedCipher(
    val url: String,
    val s: String?,
    val sp: String = "sig",
)

object CipherParser {
    fun parse(cipherString: String): ParsedCipher? {
        if (cipherString.isBlank()) return null
        val params =
            cipherString.split("&").associate {
                val parts = it.split("=")
                if (parts.size == 2) parts[0] to URLDecoder.decode(parts[1], "UTF-8") else parts[0] to ""
            }

        val url = params["url"] ?: return null
        val s = params["s"]
        val sp = params["sp"] ?: "sig"

        return ParsedCipher(url = url, s = s, sp = sp)
    }
}
