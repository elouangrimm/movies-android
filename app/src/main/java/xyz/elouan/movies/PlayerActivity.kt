package xyz.elouan.movies

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import xyz.elouan.movies.data.EMBED_SOURCES
import xyz.elouan.movies.data.OmdbRepository
import xyz.elouan.movies.data.RecentlyWatched
import xyz.elouan.movies.data.WatchedItem
import xyz.elouan.movies.data.resolveUrl

class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMDB_ID = "imdb_id"
        const val EXTRA_TYPE    = "type"
        const val EXTRA_TITLE   = "title"
        const val EXTRA_POSTER  = "poster"
        const val EXTRA_SEASON  = "season"
        const val EXTRA_EPISODE = "episode"

        // Domains that our embed players are known to load content from.
        // Any navigation OUTSIDE these is silently dropped — the cage wall.
        private val ALLOWED_DOMAINS = setOf(
            "vidlink.pro",     "vidsrc-embed.ru",   "vidsrc.me",
            "2embed.cc",       "multiembed.mov",    "getsuperembed.link",
            // CDNs and common video delivery infrastructure
            "jsdelivr.net",    "cdnjs.cloudflare.com",
            "googleapis.com",  "gstatic.com",
            "jwplatform.com",  "jwpcdn.com",
            "akamaized.net",   "cloudfront.net",
            "fastly.net",      "llnwd.net",
            "vidcdn.pro",      "vidhide.com", "vidhidepre.com",
            "filemoon.sx",     "streamtape.com",     "doodstream.com",
            "voe.sx",          "mixdrop.co",         "upstream.to"
        )
    }

    private lateinit var webView: WebView
    private lateinit var controls: View
    private lateinit var sourceBar: LinearLayout
    private lateinit var seriesRow: View
    private lateinit var seasonSpinner: Spinner
    private lateinit var episodeSpinner: Spinner
    private lateinit var loadingLabel: TextView

    private lateinit var imdbId: String
    private lateinit var type: String
    private lateinit var title: String
    private var posterUrl: String = ""
    private var currentSeason  = 1
    private var currentEpisode = 1
    private var currentSourceIndex = 0

    private var episodeCounts = mutableListOf<Int>()

    // ─── Auto-fade ────────────────────────────────────────────────────────────
    private val hideHandler  = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable {
        controls.animate().alpha(0f).setDuration(500).start()
    }

    private fun showControls() {
        hideHandler.removeCallbacks(hideRunnable)
        controls.animate().cancel()
        controls.alpha = 1f
        scheduleHide()
    }

    private fun scheduleHide() {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, 2000L)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) showControls()
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on while watching
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // True full-screen
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        setContentView(R.layout.activity_player)

        imdbId    = intent.getStringExtra(EXTRA_IMDB_ID) ?: run { finish(); return }
        type      = intent.getStringExtra(EXTRA_TYPE)    ?: "movie"
        title     = intent.getStringExtra(EXTRA_TITLE)   ?: ""
        posterUrl = intent.getStringExtra(EXTRA_POSTER)  ?: ""
        currentSeason  = intent.getIntExtra(EXTRA_SEASON, 1)
        currentEpisode = intent.getIntExtra(EXTRA_EPISODE, 1)

        webView       = findViewById(R.id.web_view)
        controls      = findViewById(R.id.controls)
        sourceBar     = findViewById(R.id.source_bar)
        seriesRow     = findViewById(R.id.series_row)
        seasonSpinner  = findViewById(R.id.season_spinner)
        episodeSpinner = findViewById(R.id.episode_spinner)
        loadingLabel   = findViewById(R.id.loading_label)

        setupWebView()
        buildSourceButtons()

        val backBtn: View = findViewById(R.id.back_button)
        backBtn.setOnClickListener { finish() }

        if (type == "series") {
            seriesRow.isVisible = true
            lifecycleScope.launch { initSeriesSpinners() }
        } else {
            seriesRow.isVisible = false
            loadSource(currentSourceIndex)
        }

        scheduleHide()
    }

    // ─── WebView cage setup ───────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled          = true
            domStorageEnabled          = true
            allowContentAccess         = true
            allowFileAccess            = false            // not needed, reduce attack surface
            databaseEnabled            = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode           = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setSupportMultipleWindows(true)               // required to intercept window.open()
            useWideViewPort            = true
            loadWithOverviewMode       = true
            builtInZoomControls        = false
            displayZoomControls        = false
            cacheMode                  = WebSettings.LOAD_DEFAULT
        }

        webView.webViewClient = object : WebViewClient() {
            // ── The cage wall ──────────────────────────────────────────────
            // shouldOverrideUrlLoading is called whenever the TOP frame tries
            // to navigate. We allow only our embed sources; everything else
            // (ad redirects, tracker links, external sites) is silently eaten.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val host = request.url.host?.removePrefix("www.") ?: return true
                val allowed = ALLOWED_DOMAINS.any { domain -> host == domain || host.endsWith(".$domain") }
                // Return true = we handle it (by doing nothing) = navigation blocked.
                return !allowed
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                loadingLabel.isVisible = true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                loadingLabel.isVisible = false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            // ── Popup cage wall ────────────────────────────────────────────
            // window.open() / target="_blank" / new windows → silently dropped.
            // The embed player never knows the popup was refused — it just vanishes.
            override fun onCreateWindow(
                view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message
            ): Boolean = false  // false = block the window

            // Intercept JS alert/confirm/prompt — suppress them silently
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean {
                result?.confirm(); return true
            }
            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean {
                result?.cancel(); return true
            }
            override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: android.webkit.JsPromptResult?): Boolean {
                result?.cancel(); return true
            }

            // Suppress console spam from ad scripts
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?) = true

            // Allow full-screen video (e.g. native player on embedded site)
            private var customView: View? = null

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                customView?.let { (it.parent as? ViewGroup)?.removeView(it) }
                customView = view
                (window.decorView as ViewGroup).addView(view, ViewGroup.LayoutParams(-1, -1))
                webView.isVisible = false
                hideHandler.removeCallbacks(hideRunnable)
                controls.animate().alpha(0f).setDuration(200).start()
            }

            override fun onHideCustomView() {
                (customView?.parent as? ViewGroup)?.removeView(customView)
                customView        = null
                webView.isVisible = true
                showControls()
            }
        }
    }

    // ─── Source buttons ───────────────────────────────────────────────────────

    private fun buildSourceButtons() {
        sourceBar.removeAllViews()
        val isTv = type == "series"
        EMBED_SOURCES.forEachIndexed { index, source ->
            if ((isTv && source.tvUrl != null) || (!isTv && source.movieUrl != null)) {
                val btn = Button(this).apply {
                    text        = source.name
                    textSize    = 12f
                    isAllCaps   = false
                    minWidth    = 0
                    minimumWidth = 0
                    tag         = index
                    setOnClickListener { loadSource(index) }
                    setTextColor(resources.getColor(R.color.text_secondary, theme))
                    setBackgroundResource(R.drawable.source_button_bg)
                    setPaddingRelative(
                        resources.getDimensionPixelSize(R.dimen.source_gap),
                        0,
                        resources.getDimensionPixelSize(R.dimen.source_gap),
                        0
                    )
                }
                sourceBar.addView(btn, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.marginEnd = resources.getDimensionPixelSize(R.dimen.source_gap) })
            }
        }
        highlightSource(currentSourceIndex)
    }

    private fun highlightSource(index: Int) {
        currentSourceIndex = index
        for (i in 0 until sourceBar.childCount) {
            val btn = sourceBar.getChildAt(i) as? Button ?: continue
            val active = btn.tag as Int == index
            btn.setBackgroundResource(
                if (active) R.drawable.source_button_active_bg
                else        R.drawable.source_button_bg
            )
            btn.setTextColor(resources.getColor(
                if (active) R.color.text_primary else R.color.text_secondary, theme))
        }
    }

    private fun loadSource(index: Int) {
        val source = EMBED_SOURCES[index]
        val isTv   = type == "series"
        val url    = source.resolveUrl(imdbId, currentSeason, currentEpisode, isTv) ?: return
        highlightSource(index)
        webView.loadUrl(url)
        saveToRecent()
    }

    // ─── Series spinners ──────────────────────────────────────────────────────

    private suspend fun initSeriesSpinners() {
        val totalSeasons = OmdbRepository.getTotalSeasons(imdbId)
        val seasonLabels = (1..totalSeasons).map { "Season $it" }
        seasonSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, seasonLabels)
        seasonSpinner.setSelection(currentSeason - 1, false)

        seasonSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentSeason  = position + 1
                currentEpisode = 1
                lifecycleScope.launch { loadEpisodes(initPlay = true) }
            }
        }

        loadEpisodes(initPlay = false)

        episodeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentEpisode = position + 1
                loadSource(currentSourceIndex)
            }
        }
        // Initial playback
        loadSource(currentSourceIndex)
    }

    private suspend fun loadEpisodes(initPlay: Boolean) {
        val episodes = OmdbRepository.getEpisodes(imdbId, currentSeason)
        val labels   = episodes.map { ep ->
            if (ep.title.isNotBlank()) "E${ep.episode}: ${ep.title}" else "Episode ${ep.episode}"
        }
        episodeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        val selectIndex = (currentEpisode - 1).coerceIn(0, labels.size - 1)
        // Temporarily remove listener to avoid re-triggering playback
        episodeSpinner.onItemSelectedListener = null
        episodeSpinner.setSelection(selectIndex, false)
        episodeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentEpisode = position + 1
                loadSource(currentSourceIndex)
            }
        }
        if (initPlay) loadSource(currentSourceIndex)
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun saveToRecent() {
        RecentlyWatched.add(this, WatchedItem(
            imdbId    = imdbId,
            title     = title,
            type      = type,
            season    = currentSeason,
            episode   = currentEpisode,
            posterUrl = posterUrl
        ))
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        hideHandler.removeCallbacks(hideRunnable)
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
