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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/** YouTube video ids are exactly 11 URL-safe characters; anything else must not reach a URL. */
private val VIDEO_ID_REGEX = Regex("^[A-Za-z0-9_-]{11}$")
private const val YOUTUBE_EMBED_PREFIX = "https://www.youtube.com/embed/"
private const val DEFAULT_EMBED_VIDEO_ID = "NJAv_7lHUIU"

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

        // One WebView at a time: parallel stream resolutions must not race on the same bridge.
        private val generationMutex = Mutex()
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
                generationMutex.withLock {
                    withTimeoutOrNull(tokenTimeoutMs) {
                        generatePoTokenInWebView(traceId, videoOrVisitorHint)
                    }
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
                var webView: WebView? = null
                val handler = Handler(Looper.getMainLooper())

                try {
                    WebView.setWebContentsDebuggingEnabled(false)
                    val wv = WebView(context).also { webView = it }
                    wv.settings.javaScriptEnabled = true
                    wv.settings.domStorageEnabled = true
                    wv.settings.allowFileAccess = false
                    wv.settings.allowContentAccess = false
                    wv.settings.userAgentString =
                        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

                    val jsInterface =
                        object {
                            @JavascriptInterface
                            fun onTokenGenerated(
                                token: String,
                                visitorData: String,
                            ) {
                                if (token.isNotBlank() && deferred.isActive) {
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

                    wv.addJavascriptInterface(jsInterface, "PoTokenBridge")

                    wv.webViewClient =
                        object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: android.webkit.WebResourceRequest?,
                            ): Boolean {
                                val host = request?.url?.host ?: return true
                                return !(host == "youtube.com" || host.endsWith(".youtube.com"))
                            }

                            override fun onPageFinished(
                                view: WebView?,
                                url: String?,
                            ) {
                                var attempts = 0
                                val maxAttempts = 20 // 20 * 500ms = 10s max

                                val pollRunnable =
                                    object : Runnable {
                                        override fun run() {
                                            if (!deferred.isActive) return
                                            attempts++

                                            val jsExtractor =
                                                """
                                                (function() {
                                                    try {
                                                        var v = (window.ytcfg && window.ytcfg.get ? window.ytcfg.get('VISITOR_DATA') : '') ||
                                                                (window.ytcfg && window.ytcfg.data_ ? window.ytcfg.data_.VISITOR_DATA : '') || '';
                                                        var t = (window.ytcfg && window.ytcfg.get ? window.ytcfg.get('PO_TOKEN') : '') ||
                                                                (window.ytcfg && window.ytcfg.data_ ? window.ytcfg.data_.PO_TOKEN : '') || '';
                                                        window.PoTokenBridge.onTokenGenerated(t || '', v || '');
                                                    } catch (e) {
                                                        window.PoTokenBridge.onTokenGenerated('', '');
                                                    }
                                                })();
                                                """.trimIndent()

                                            view?.evaluateJavascript(jsExtractor, null)

                                            if (attempts < maxAttempts && deferred.isActive) {
                                                handler.postDelayed(this, 500L)
                                            } else if (attempts >= maxAttempts && deferred.isActive) {
                                                deferred.complete(null)
                                            }
                                        }
                                    }

                                handler.post(pollRunnable)
                            }
                        }

                    // The hint comes from provider data: only a well-formed video id may be embedded.
                    val videoId = hint?.takeIf { VIDEO_ID_REGEX.matches(it) } ?: DEFAULT_EMBED_VIDEO_ID
                    wv.loadUrl(YOUTUBE_EMBED_PREFIX + videoId)

                    // Overall timeout fallback
                    handler.postDelayed({
                        if (deferred.isActive) {
                            deferred.complete(null)
                        }
                    }, tokenTimeoutMs)

                    deferred.await()
                } catch (e: CancellationException) {
                    throw e // let a cancelled lookup cancel instead of reporting "no token"
                } catch (e: Exception) {
                    DiagnosticLogger.logError(traceId, "PO_TOKEN_FAILED", e.message ?: "WebView error")
                    null
                } finally {
                    handler.removeCallbacksAndMessages(null)
                    runCatching {
                        webView?.apply {
                            stopLoading()
                            removeJavascriptInterface("PoTokenBridge")
                            destroy()
                        }
                    }
                }
            }

        fun invalidate() {
            cachedToken.set(null)
        }
    }
