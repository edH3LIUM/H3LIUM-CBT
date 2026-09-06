package com.h3lium.cbt; // <-- Apni app ka exact package name yahan rehne dena

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private final String HOME_URL = "https://h3lium-cbt.netlify.app/";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Hardware Acceleration for smooth experience
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);

        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // External Deep Links Handling (Telegram, WhatsApp, Intent schemes)
                if (url.startsWith("intent://") || url.startsWith("tg:") || url.startsWith("whatsapp://")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent != null) {
                            startActivity(intent);
                            return true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                }

                // PDF Print Trigger
                if (url.endsWith(".pdf")) {
                    createWebPrintJob(view);
                    return true;
                }

                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // Prevent Firebase real-time / background script leaks
                if (request.isForMainFrame()) {
                    String failingUrl = request.getUrl().toString();
                    if (!failingUrl.contains("firebaseio.com") && !failingUrl.endsWith(".js")) {
                        showCustomOfflinePage(view, failingUrl);
                    }
                }
            }
        });

        if (isNetworkAvailable()) {
            webView.loadUrl(HOME_URL);
        } else {
            showCustomOfflinePage(webView, HOME_URL);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    private void createWebPrintJob(WebView webView) {
        PrintManager printManager = (PrintManager) this.getSystemService(Context.PRINT_SERVICE);
        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter("H3LIUM_Document");
        String jobName = getString(R.string.app_name) + " Document";
        if (printManager != null) {
            printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());
        }
    }

    // Interactive Multi-Theme Offline Page (Dark, Light, Vintage) + Moving Target Game
    private void showCustomOfflinePage(WebView view, String currentTargetUrl) {
        String target = (currentTargetUrl != null && !currentTargetUrl.contains("firebaseio.com")) 
                ? currentTargetUrl : HOME_URL;

        String offlineHtml = "<!DOCTYPE html><html><head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">" +
                "<style>" +
                ":root { --bg: #0b0f19; --card: rgba(15,23,42,0.9); --text: #f8fafc; --sub: #94a3b8; --accent: #3b82f6; --border: rgba(255,255,255,0.12); }" +
                "body.theme-light { --bg: #f1f5f9; --card: rgba(255,255,255,0.95); --text: #0f172a; --sub: #475569; --accent: #2563eb; --border: rgba(0,0,0,0.12); }" +
                "body.theme-vintage { --bg: #f4ebd0; --card: rgba(230,219,192,0.95); --text: #362f2d; --sub: #6b5e59; --accent: #b85b32; --border: rgba(54,47,45,0.15); }" +
                "* { box-sizing: border-box; margin: 0; padding: 0; user-select: none; }" +
                "body { background: var(--bg); color: var(--text); font-family: system-ui, -apple-system, sans-serif; min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; transition: background 0.3s ease; }" +
                ".card { width: 100%; max-width: 440px; background: var(--card); border: 1px solid var(--border); border-radius: 20px; padding: 24px; text-align: center; box-shadow: 0 20px 40px rgba(0,0,0,0.3); }" +
                ".theme-bar { display: flex; justify-content: center; gap: 8px; margin-bottom: 18px; }" +
                ".theme-btn { padding: 6px 14px; font-size: 12px; font-weight: 600; border-radius: 20px; border: 1px solid var(--border); background: transparent; color: var(--text); cursor: pointer; }" +
                "h1 { font-size: 20px; margin-bottom: 8px; font-weight: 700; }" +
                "p { font-size: 13px; color: var(--sub); margin-bottom: 16px; }" +
                ".game-box { background: rgba(0,0,0,0.1); border: 1px solid var(--border); border-radius: 12px; height: 130px; margin-bottom: 20px; position: relative; overflow: hidden; }" +
                "#target { width: 38px; height: 38px; background: var(--accent); border-radius: 50%; position: absolute; cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: bold; color: #fff; box-shadow: 0 4px 10px rgba(0,0,0,0.2); }" +
                ".btn-refresh { width: 100%; padding: 14px; border-radius: 12px; font-size: 15px; font-weight: 600; background: var(--accent); color: #fff; border: none; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; }" +
                "</style></head><body class=\"theme-dark\">" +
                "<div class=\"card\">" +
                "<div class=\"theme-bar\">" +
                "<button class=\"theme-btn\" onclick=\"setTheme('dark')\">Dark</button>" +
                "<button class=\"theme-btn\" onclick=\"setTheme('light')\">Light</button>" +
                "<button class=\"theme-btn\" onclick=\"setTheme('vintage')\">Vintage</button>" +
                "</div>" +
                "<h1>Connection Lost</h1>" +
                "<p>Tap the circle to play while waiting for internet!</p>" +
                "<div class=\"game-box\" id=\"box\"><div id=\"target\" onclick=\"hit()\">0</div></div>" +
                "<button class=\"btn-refresh\" onclick=\"location.href='" + target + "'\">⚡ Refresh Current Page</button>" +
                "</div>" +
                "<script>" +
                "let score = 0;" +
                "function setTheme(t) { document.body.className = 'theme-' + t; localStorage.setItem('h3_theme', t); }" +
                "const st = localStorage.getItem('h3_theme'); if(st) setTheme(st);" +
                "function hit() { score++; const t = document.getElementById('target'), b = document.getElementById('box'); t.innerText = score; t.style.left = Math.floor(Math.random()*(b.clientWidth-40))+'px'; t.style.top = Math.floor(Math.random()*(b.clientHeight-40))+'px'; }" +
                "hit();" +
                "</script></body></html>";

        view.loadDataWithBaseURL(null, offlineHtml, "text/html", "UTF-8", null);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
