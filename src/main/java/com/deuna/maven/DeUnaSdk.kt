package com.deuna.maven

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DeUnaSdk {
    private lateinit var apiKey: String
    private lateinit var orderToken: String
    private lateinit var environment: Environment
    private lateinit var elementType: ElementType
    private lateinit var userToken: String
    private var elementURL: String = "https://elements.euna"
    private var actionMillisecods = 5000L

    inner class Callbacks {
        var onSuccess: ((String) -> Unit)? = null
        var onError: ((String) -> Unit)? = null
        var onClose: ((WebView) -> Unit)? = null
    }

    enum class Environment {
        DEVELOPMENT,
        PRODUCTION
    }

    enum class ElementType(val value: String) {
        SAVE_CARD("saveCard"),
        EXAMPLE("example")
    }

    inner class JSBridge {
        @JavascriptInterface
        fun receiveMessage(message: String) {
            Log.d("received message from webview", message)
        }
    }

    companion object {
        private lateinit var instance: DeUnaSdk

        fun config(
            apiKey: String? = null,
            orderToken: String? = null,
            userToken: String? = null,
            environment: Environment,
            elementType: ElementType? = null
        ) {
            instance = DeUnaSdk().apply {
                if (apiKey != null) {
                    this.apiKey = apiKey
                }
                if (orderToken != null) {
                    this.orderToken = orderToken
                }

                if (userToken != null) {
                    this.userToken = userToken
                }
                this.environment = environment
                if (this.environment == Environment.DEVELOPMENT) {
                    this.elementURL = "https://pay.stg.deuna.com/elements"
                } else {
                    this.elementURL = "https://pay.deuna.com/elements"
                }
                if (elementType != null) {
                    this.elementType = elementType
                }
            }
        }

        @OptIn(DelicateCoroutinesApi::class)
        fun initCheckout(
            view: View
        ): Callbacks {
            instance.apply {
                val callbacks = Callbacks()
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)

                val webView: WebView = view.findViewById(R.id.deuna_webview)

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)

                        GlobalScope.launch(Dispatchers.Main) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                try {
                                    callbacks.onClose?.invoke(webView)
                                    callbacks.onSuccess?.invoke("")
                                } catch (e: Exception) {
                                    callbacks.onClose?.invoke(webView)
                                    e.localizedMessage?.let { callbacks.onError?.invoke(it) }
                                }
                            }, actionMillisecods)
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                    }
                }
                webView.settings.domStorageEnabled = true
                webView.settings.javaScriptEnabled = true
                webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                webView.loadUrl("https://develop.dlbinhdcmjzvl.amplifyapp.com/$orderToken")
                webView.addJavascriptInterface(JSBridge(), "Android")
                return callbacks
            }
        }

        @OptIn(DelicateCoroutinesApi::class)
        fun initElements(
            view: View
        ): Callbacks {
            instance.apply {
                val callbacks = Callbacks()
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)

                val webView: WebView = view.findViewById(R.id.deuna_webview)
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        GlobalScope.launch(Dispatchers.Main) {

                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                    }

                }
                webView.settings.domStorageEnabled = true
                webView.settings.javaScriptEnabled = true
                webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                val url = "$elementURL/${elementType.value}"
                webView.loadUrl(url)
                webView.addJavascriptInterface(JSBridge(), "Android")
                return callbacks
            }
        }
    }
}