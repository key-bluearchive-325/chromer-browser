package app.hatent.chromer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var etUrl: EditText
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        etUrl = findViewById(R.id.etUrl)
        progressBar = findViewById(R.id.progressBar)

        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnForward = findViewById<Button>(R.id.btnForward)
        val btnExtensions = findViewById<Button>(R.id.btnExtensions)
        val btnReload = findViewById<Button>(R.id.btnReload)

        val pillDrawable = GradientDrawable().apply {
            setColor(Color.parseColor("#EBEBEB"))
            cornerRadius = 30f
        }
        etUrl.background = pillDrawable

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            offscreenPreRaster = true
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                if (AdBlocker.isBlocked(url)) {
                    return AdBlocker.createEmptyResponse()
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                etUrl.setText(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE

                val hideAdCss = """
                    javascript:(function() {
                        var style = document.createElement('style');
                        style.innerHTML = 'ins.adsbygoogle, .ad-banner, .ad-container, [id^="google_ads"] { display: none !important; }';
                        document.head.appendChild(style);
                    })()
                """.trimIndent()
                view?.evaluateJavascript(hideAdCss, null)

                view?.let { ExtensionManager.applyExtensions(it) }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
            }
        }

        btnBack.setOnClickListener { if (webView.canGoBack()) webView.goBack() }
        btnForward.setOnClickListener { if (webView.canGoForward()) webView.goForward() }
        btnReload.setOnClickListener { webView.reload() }

        btnExtensions.setOnClickListener {
            showExtensionDialog()
        }

        etUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadInputUrl()
                true
            } else {
                false
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        webView.loadUrl("https://duckduckgo.com")
    }

    private fun loadInputUrl() {
        val input = etUrl.text.toString().trim()
        if (input.isEmpty()) return

        val url = if (input.startsWith("http://") || input.startsWith("https://")) {
            input
        } else if (input.contains(".") && !input.contains(" ")) {
            "https://$input"
        } else {
            "https://duckduckgo.com/?q=$input"
        }
        webView.loadUrl(url)
    }

    private fun showExtensionDialog() {
        val items = arrayOf(
            "🌙 強制ダークモード: ${if (ExtensionManager.isDarkModeEnabled) "ON" else "OFF"}",
            "📺 動画ピクチャーインピクチャー (PiP) サポート",
            "＋ カスタムJavaScriptスクリプトを追加"
        )

        AlertDialog.Builder(this)
            .setTitle("🧩 Chromer 拡張機能")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        ExtensionManager.isDarkModeEnabled = !ExtensionManager.isDarkModeEnabled
                        webView.reload()
                        Toast.makeText(this, "ダークモードを切り替えました", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        ExtensionManager.isPiPEnabled = true
                        ExtensionManager.applyExtensions(webView)
                        Toast.makeText(this, "動画PiP機能を実行しました", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        showAddCustomScriptDialog()
                    }
                }
            }
            .setNegativeButton("閉じる", null)
            .show()
    }

    private fun showAddCustomScriptDialog() {
        val input = EditText(this).apply {
            hint = "JavaScriptコードを入力 (例: alert('Hello!'))"
        }
        AlertDialog.Builder(this)
            .setTitle("カスタムスクリプト追加")
            .setView(input)
            .setPositiveButton("追加＆実行") { _, _ ->
                val script = input.text.toString().trim()
                if (script.isNotEmpty()) {
                    ExtensionManager.customScripts.add(script)
                    webView.evaluateJavascript(script, null)
                    Toast.makeText(this, "スクリプトを実行しました", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }
}