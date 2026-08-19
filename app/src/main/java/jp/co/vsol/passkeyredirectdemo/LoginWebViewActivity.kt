package jp.co.vsol.passkeyredirectdemo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.http.SslError
import androidx.appcompat.app.AppCompatActivity
import java.net.URLEncoder

class LoginWebViewActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_webview)

        val loginUrl = intent.getStringExtra(EXTRA_LOGIN_URL)
        val state = intent.getStringExtra(EXTRA_STATE)
        val redirectUrl = intent.getStringExtra(EXTRA_REDIRECT_URL)
        if (loginUrl.isNullOrBlank() || state.isNullOrBlank() || redirectUrl.isNullOrBlank()) {
            finish()
            return
        }

        val expectedRedirectUri = Uri.parse(redirectUrl)
        webView = findViewById(R.id.loginWebView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val requestUri = request?.url ?: return false
                if (!isRedirectUri(requestUri, expectedRedirectUri)) return false

                startActivity(Intent(Intent.ACTION_VIEW, requestUri))
                finish()
                return true
            }

            // STG環境の証明書エラーを無視（本番では削除すること）
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.proceed()
            }
        }
        webView.postUrl(loginUrl, buildPostData(state, redirectUrl).toByteArray(Charsets.UTF_8))
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun buildPostData(state: String, redirectUrl: String): String {
        return "state=${state.urlEncoded()}&redirect_uri=${redirectUrl.urlEncoded()}"
    }

    private fun isRedirectUri(uri: Uri, expected: Uri): Boolean {
        return uri.scheme == expected.scheme &&
            uri.host == expected.host &&
            uri.path == expected.path
    }

    private fun String.urlEncoded(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

    companion object {
        private const val EXTRA_LOGIN_URL = "extra_login_url"
        private const val EXTRA_STATE = "extra_state"
        private const val EXTRA_REDIRECT_URL = "extra_redirect_url"

        fun createIntent(
            context: Context,
            loginUrl: String,
            state: String,
            redirectUrl: String
        ): Intent {
            return Intent(context, LoginWebViewActivity::class.java)
                .putExtra(EXTRA_LOGIN_URL, loginUrl)
                .putExtra(EXTRA_STATE, state)
                .putExtra(EXTRA_REDIRECT_URL, redirectUrl)
        }
    }
}
