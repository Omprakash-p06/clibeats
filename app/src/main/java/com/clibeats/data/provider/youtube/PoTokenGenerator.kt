@file:Suppress(
    "ForbiddenImport",
    "ReturnCount",
    "MaxLineLength",
    "MagicNumber",
    "TooManyFunctions",
    "TooGenericExceptionCaught",
)

package com.clibeats.data.provider.youtube

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.clibeats.util.DiagnosticLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

data class PoTokenResult(
    val poToken: String,
    val visitorData: String,
    val expiresAtMs: Long,
)

@Singleton
class PoTokenGenerator
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val cachedToken = AtomicReference<PoTokenResult?>(null)
        private val tokenTimeoutMs = 10_000L
        private val tokenTtlMs = 12 * 60 * 60 * 1000L // 12 hours

        suspend fun getPoToken(
            traceId: String,
            videoOrVisitorHint: String? = null,
        ): PoTokenResult? {
            val now = System.currentTimeMillis()
            val cached = cachedToken.get()
            if (cached != null && cached.expiresAtMs > now) {
                return cached
            }

            DiagnosticLogger.logPoTokenStarted(traceId)

            val result =
                withTimeoutOrNull(tokenTimeoutMs) {
                    generatePoTokenInWebView(traceId, videoOrVisitorHint)
                }

            if (result != null && result.poToken.isNotBlank()) {
                val expiresAt = now + tokenTtlMs
                val fullResult = result.copy(expiresAtMs = expiresAt)
                cachedToken.set(fullResult)
                DiagnosticLogger.logPoTokenSuccess(traceId)
                return fullResult
            }

            DiagnosticLogger.logError(traceId, "PO_TOKEN_FAILED", "Failed to generate PO token via WebView")
            return null
        }

        @SuppressLint("SetJavaScriptEnabled")
        private suspend fun generatePoTokenInWebView(
            traceId: String,
            hint: String?,
        ): PoTokenResult? =
            withContext(Dispatchers.Main) {
                val deferred = CompletableDeferred<PoTokenResult?>()
                try {
                    val webView = WebView(context)
                    webView.settings.javaScriptEnabled = true
                    webView.settings.domStorageEnabled = true
                    webView.settings.userAgentString =
                        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

                    val jsInterface =
                        object {
                            @JavascriptInterface
                            fun onTokenGenerated(
                                token: String,
                                visitorData: String,
                            ) {
                                if (deferred.isActive) {
                                    deferred.complete(
                                        PoTokenResult(
                                            poToken = token,
                                            visitorData = visitorData,
                                            expiresAtMs = 0L,
                                        ),
                                    )
                                }
                            }
                        }

                    webView.addJavascriptInterface(jsInterface, "PoTokenBridge")

                    webView.webViewClient =
                        object : WebViewClient() {
                            override fun onPageFinished(
                                view: WebView?,
                                url: String?,
                            ) {
                                // Extract botguard / poToken from page context if present
                                val jsExtractor =
                                    """
                                    (function() {
                                        try {
                                            var visitorData = window.ytInitialData?.responseContext?.webResponseContextExtensionData?.ytGスカ?.visitorData || "";
                                            var token = window.gapi?.auth?.getToken()?.access_token || "";
                                            window.PoTokenBridge.onTokenGenerated(token, visitorData);
                                        } catch (e) {
                                            window.PoTokenBridge.onTokenGenerated("", "");
                                        }
                                    })();
                                    """.trimIndent()
                                view?.evaluateJavascript(jsExtractor, null)
                            }
                        }

                    val targetUrl = "https://www.youtube.com/embed/" + (hint ?: "jNQXAC9IVRw")
                    webView.loadUrl(targetUrl)

                    // Handler fallback after 5s to avoid hanging if JS evaluate fails
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (deferred.isActive) {
                            deferred.complete(null)
                        }
                    }, 5000L)
                } catch (e: Exception) {
                    DiagnosticLogger.logError(traceId, "PO_TOKEN_FAILED", e.message ?: "WebView error")
                    if (deferred.isActive) {
                        deferred.complete(null)
                    }
                }

                deferred.await()
            }

        fun invalidate() {
            cachedToken.set(null)
        }
    }
