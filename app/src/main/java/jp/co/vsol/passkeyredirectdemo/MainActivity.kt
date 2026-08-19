package jp.co.vsol.passkeyredirectdemo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import java.io.File
import java.io.FileWriter

private fun String.htmlAttributeEscape(): String {
    val builder = StringBuilder(length)
    for (character in this) {
        when (character) {
            '&' -> builder.append("&amp;")
            '<' -> builder.append("&lt;")
            '>' -> builder.append("&gt;")
            '"' -> builder.append("&quot;")
            '\'' -> builder.append("&#39;")
            else -> builder.append(character)
        }
    }
    return builder.toString()
}

class MainActivity : AppCompatActivity() {
    private lateinit var loginUrlEdit: EditText
    private lateinit var redirectUrlEdit: EditText
    private lateinit var launchButton: Button
    private lateinit var clearButton: Button
    private lateinit var pendingStateValue: TextView
    private lateinit var launchUrlValue: TextView
    private lateinit var callbackPreviewValue: TextView
    private lateinit var receivedShortTimeCodeValue: TextView
    private lateinit var receivedStateValue: TextView
    private lateinit var receivedErrorValue: TextView
    private lateinit var stateValidationValue: TextView
    private lateinit var callbackUrlValue: TextView
    private lateinit var statusValue: TextView

    private var pendingState: String = makeState()
    private var launchedLoginUrl: Uri? = null
    private var callbackUrl: Uri? = null
    private var receivedShortTimeCode: String? = null
    private var receivedState: String? = null
    private var receivedError: String? = null
    private var stateValidationMessage: String? = null
    private var statusMessage: String = "ログインを押すと外部ブラウザを開きます。"
    private var bridgeHtmlFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        bindDefaults()
        bindListeners()
        renderState()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun bindViews() {
        loginUrlEdit = findViewById(R.id.loginUrlEdit)
        redirectUrlEdit = findViewById(R.id.redirectUrlEdit)
        launchButton = findViewById(R.id.launchButton)
        clearButton = findViewById(R.id.clearButton)
        pendingStateValue = findViewById(R.id.pendingStateValue)
        launchUrlValue = findViewById(R.id.launchUrlValue)
        callbackPreviewValue = findViewById(R.id.callbackPreviewValue)
        receivedShortTimeCodeValue = findViewById(R.id.receivedShortTimeCodeValue)
        receivedStateValue = findViewById(R.id.receivedStateValue)
        receivedErrorValue = findViewById(R.id.receivedErrorValue)
        stateValidationValue = findViewById(R.id.stateValidationValue)
        callbackUrlValue = findViewById(R.id.callbackUrlValue)
        statusValue = findViewById(R.id.statusValue)
    }

    private fun bindDefaults() {
        loginUrlEdit.setText(defaultLoginPageUrl())
        redirectUrlEdit.setText(defaultRedirectUrl())
    }

    private fun bindListeners() {
        loginUrlEdit.doAfterTextChanged { renderState() }
        redirectUrlEdit.doAfterTextChanged { renderState() }

        launchButton.setOnClickListener {
            beginLoginAndLaunchBrowser()
        }

        clearButton.setOnClickListener {
            clearResult()
        }
    }

    private fun beginLoginAndLaunchBrowser() {
        val loginUrlText = loginUrlEdit.text?.toString().orEmpty()
        val redirectUrlText = redirectUrlEdit.text?.toString().orEmpty()
        pendingState = makeState()
        val loginUrl = buildLoginTargetUrl(loginUrlText, redirectUrlText) ?: return
        val bridgeUri = buildPostBridgeUri(
            loginUrl = loginUrl.toString(),
            state = pendingState,
            redirectUrl = redirectUrlText
        )

        launchedLoginUrl = loginUrl
        callbackUrl = null
        receivedShortTimeCode = null
        receivedState = null
        receivedError = null
        stateValidationMessage = null

        updateStatus("Chrome でログインを開始しました。認証完了後のコールバックを待機しています。")
        renderState()

        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(bridgeUri, "text/html")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW) return
        val data = intent.data ?: return

        callbackUrl = data
        val queryItems = data.queryParameterNames
        receivedShortTimeCode = queryItems.firstOrNull { it == "shortTimeCode" }?.let { data.getQueryParameter(it) }
            ?: queryItems.firstOrNull { it == "code" }?.let { data.getQueryParameter(it) }
        receivedState = data.getQueryParameter("state")
        receivedError = data.getQueryParameter("error")

        stateValidationMessage = when {
            receivedState == null -> "state を受信できませんでした。"
            receivedState == pendingState -> "state が一致しました。"
            else -> "state が一致しません。別セッションの戻り値か、改ざんの可能性があります。"
        }

        updateStatus(
            when {
                receivedError != null -> "コールバックでエラーを受信しました: $receivedError"
                receivedShortTimeCode == null -> "コールバックは受信しましたが、shortTimeCode が含まれていません。"
                else -> "コールバックを受信しました。"
            }
        )
        renderState()
    }

    private fun clearResult() {
        callbackUrl = null
        receivedShortTimeCode = null
        receivedState = null
        receivedError = null
        stateValidationMessage = null
        updateStatus("結果をクリアしました。再度ログインを開始できます。")
        renderState()
    }

    private fun buildLoginTargetUrl(loginUrlText: String, redirectUrlText: String): Uri? {
        val redirectUrl = Uri.parse(redirectUrlText)
        if (redirectUrl.scheme.isNullOrBlank() || redirectUrl.host.isNullOrBlank()) {
            updateStatus("redirect_uri の形式が不正です。")
            return null
        }

        val loginUrl = Uri.parse(loginUrlText)
        if (loginUrl.scheme.isNullOrBlank() || loginUrl.host.isNullOrBlank()) {
            updateStatus("ログイン先 URL の形式が不正です。")
            return null
        }

        return loginUrl
    }

    private fun renderState() {
        val redirectUrlText = redirectUrlEdit.text?.toString().orEmpty()
        pendingStateValue.text = pendingState
        launchUrlValue.text = launchedLoginUrl?.toString() ?: "未起動"
        callbackPreviewValue.text = buildCallbackPreview(redirectUrlText) ?: "未設定"
        receivedShortTimeCodeValue.text = receivedShortTimeCode ?: "未受信"
        receivedStateValue.text = receivedState ?: "未受信"
        receivedErrorValue.text = receivedError ?: "未受信"
        stateValidationValue.text = stateValidationMessage ?: "未検証"
        callbackUrlValue.text = callbackUrl?.toString() ?: "未受信"
        statusValue.text = statusMessage
    }

    private fun buildCallbackPreview(redirectUrlText: String): String? {
        val redirectUrl = Uri.parse(redirectUrlText)
        if (redirectUrl.scheme.isNullOrBlank() || redirectUrl.host.isNullOrBlank()) return null

        val builder = redirectUrl.buildUpon()
            .clearQuery()
            .appendQueryParameter("state", pendingState)

        receivedShortTimeCode?.let {
            builder.appendQueryParameter("shortTimeCode", it)
        }

        return builder.build().toString()
    }

    private fun updateStatus(message: String) {
        statusMessage = message
        statusValue.text = message
    }

    private fun buildPostBridgeUri(loginUrl: String, state: String, redirectUrl: String): Uri {
        val html = """
            <!doctype html>
            <html lang="ja">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>ログイン中</title>
            </head>
            <body>
              <noscript>JavaScript を有効にしてください。</noscript>
              <form id="bridgeForm" action="${loginUrl.htmlAttributeEscape()}" method="post">
                <input type="hidden" name="state" value="${state.htmlAttributeEscape()}">
                <input type="hidden" name="redirect_uri" value="${redirectUrl.htmlAttributeEscape()}">
              </form>
              <script>
                document.getElementById('bridgeForm').submit();
              </script>
            </body>
            </html>
        """.trimIndent()

        val file = File(cacheDir, "post-bridge.html")
        FileWriter(file, false).use { writer ->
            writer.write(html)
        }
        bridgeHtmlFile = file

        return FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
    }

    private fun defaultLoginPageUrl(): String = "https://sinfo.stg-trade.sbifxt.co.jp:1443/mpage/pf-login.html"
//    private fun defaultLoginPageUrl(): String = "https://ryunosuke-shinkubo.github.io/auth/test-login.html"

    private fun defaultRedirectUrl(): String = "https://ryunosuke-shinkubo.github.io/auth/callback.html"

    private fun makeState(): String = java.util.UUID.randomUUID().toString().replace("-", "").lowercase()

    override fun onDestroy() {
        bridgeHtmlFile?.delete()
        bridgeHtmlFile = null
        super.onDestroy()
    }
}
