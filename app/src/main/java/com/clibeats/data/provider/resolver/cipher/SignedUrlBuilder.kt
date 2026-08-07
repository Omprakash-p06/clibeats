package com.clibeats.data.provider.resolver.cipher

object SignedUrlBuilder {
    fun build(
        parsedCipher: ParsedCipher,
        decipheredSignature: String?,
    ): String {
        if (decipheredSignature.isNullOrEmpty()) {
            return parsedCipher.url
        }
        val separator = if (parsedCipher.url.contains("?")) "&" else "?"
        return "${parsedCipher.url}$separator${parsedCipher.sp}=$decipheredSignature"
    }
}
