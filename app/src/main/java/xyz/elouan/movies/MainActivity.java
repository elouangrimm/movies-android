package xyz.elouan.movies;

import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView myWebView;
    // CHANGE THIS URL TO YOUR SITE
    private static final String TARGET_URL = "https://movies.elouan.xyz";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myWebView = findViewById(R.id.webview);
        WebSettings webSettings = myWebView.getSettings();

        // 1. Enable JavaScript (Required for video players)
        webSettings.setJavaScriptEnabled(true);
        
        // 2. Enable DOM Storage (Required for saving settings/history)
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        // 3. Media Playback settings
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // ---------------------------------------------------------
        // THE POPUP BLOCKER LOGIC
        // ---------------------------------------------------------
        
        // A. Tell the WebView NOT to support multiple windows.
        // This is the strongest way to kill window.open()
        webSettings.setSupportMultipleWindows(false);
        
        // B. Prevent JS from opening windows automatically
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);

        // C. Ensure all normal links load INSIDE the app, not Chrome
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Return false means "Let the WebView handle this URL"
                // We allow all navigation within the current window
                return false; 
            }
        });

        // D. WebChromeClient is needed for full-screen video support
        myWebView.setWebChromeClient(new WebChromeClient());

        // 4. Load the site
        myWebView.loadUrl(TARGET_URL);
    }

    // Handle the Android "Back" button so it navigates browser history
    // instead of closing the app immediately.
    @Override
    public void onBackPressed() {
        if (myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}