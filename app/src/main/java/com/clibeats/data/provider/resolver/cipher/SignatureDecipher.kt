package com.clibeats.data.provider.resolver.cipher

import javax.inject.Inject
import javax.inject.Singleton

interface SignatureDecipher {
    suspend fun decipher(signature: String): String?
}

@Singleton
class DefaultSignatureDecipher
    @Inject
    constructor() : SignatureDecipher {
        override suspend fun decipher(signature: String): String? {
            // Fallback cipher implementation or placeholder for JS execution
            return signature
        }
    }
