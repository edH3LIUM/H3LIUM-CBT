package com.h3lium.cbt;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JsResult;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView myWebView;
    private static final String CHANNEL_ID = "h3lium_app_notifications";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        myWebView = findViewById(R.id.webview);
        
        myWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        WebSettings webSettings = myWebView.getSettings();
        
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                Dialog dialog = new Dialog(MainActivity.this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.dialog_custom);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.setCancelable(false);

                TextView tvMessage = dialog.findViewById(R.id.dialog_message);
                Button btnOk = dialog.findViewById(R.id.dialog_button_ok);
                Button btnCancel = dialog.findViewById(R.id.dialog_button_cancel);

                tvMessage.setText(message);
                btnCancel.setVisibility(View.VISIBLE);

                btnOk.setOnClickListener(v -> {
                    result.confirm();
                    dialog.dismiss();
                });

                btnCancel.setOnClickListener(v -> {
                    result.cancel();
                    dialog.dismiss();
                });

                dialog.show();
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                Dialog dialog = new Dialog(MainActivity.this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.dialog_custom);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.setCancelable(false);

                TextView tvMessage = dialog.findViewById(R.id.dialog_message);
                Button btnOk = dialog.findViewById(R.id.dialog_button_ok);
                Button btnCancel = dialog.findViewById(R.id.dialog_button_cancel);

                tvMessage.setText(message);
                btnCancel.setVisibility(View.GONE);

                btnOk.setOnClickListener(v -> {
                    result.confirm();
                    dialog.dismiss();
                });

                dialog.show();
                return true;
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView newWebView = new WebView(MainActivity.this);
                WebSettings newSettings = newWebView.getSettings();
                newSettings.setJavaScriptEnabled(true);
                newSettings.setDomStorageEnabled(true);
                
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();

                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        if (handleExternalLinks(url)) {
                            return true;
                        }
                        myWebView.loadUrl(url);
                        return true;
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        String url = request.getUrl().toString();
                        if (handleExternalLinks(url)) {
                            return true;
                        }
                        myWebView.loadUrl(url);
                        return true;
                    }

                    @Override
                    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                        showCustomErrorPage(view, request.getUrl().toString());
                    }
                });
                return true;
            }
        });

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (handleExternalLinks(url)) {
                    return true;
                }
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (handleExternalLinks(url)) {
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (handleExternalLinks(url)) {
                    view.stopLoading();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.loadUrl("javascript:window.print = function() { window.AndroidPrint.print(); };");
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                showCustomErrorPage(view, failingUrl);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                String failingUrl = request.getUrl().toString();
                showCustomErrorPage(view, failingUrl);
            }
        });

        // Native Print / PDF bridge
        myWebView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void print() {
                runOnUiThread(() -> {
                    PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                    PrintDocumentAdapter printAdapter = myWebView.createPrintDocumentAdapter("Analysis_Report");
                    if (printManager != null) {
                        printManager.print("Analysis_Report", printAdapter, new PrintAttributes.Builder().build());
                    }
                });
            }
        }, "AndroidPrint");

        // General file downloads
        myWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    String cookies = CookieManager.getInstance().getCookie(url);
                    request.addRequestHeader("cookie", cookies);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setDescription("Downloading file...");
                    
                    String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
                    request.setTitle(filename);
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                    
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    dm.enqueue(request);
                    
                    Toast.makeText(getApplicationContext(), "Downloading Analysis...", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Download failed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        myWebView.loadUrl("https://h3lium-cbt.netlify.app/"); 
    }

    // Interactive & Animated Fullscreen Offline Experience
    private void showCustomErrorPage(WebView view, String failedUrl) {
        String retryTarget = (failedUrl != null && !failedUrl.startsWith("data:") && !failedUrl.contains("firebaseio.com") && !failedUrl.endsWith(".js")) 
        ? failedUrl : "https://h3lium-cbt.netlify.app/";
        String errorHtml = "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">" +
                "<style>" +
                "* { box-sizing: border-box; margin: 0; padding: 0; user-select: none; }" +
                "body { background: #060913; color: #f8fafc; font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; height: 100vh; width: 100vw; overflow: hidden; display: flex; align-items: center; justify-content: center; position: relative; }" +
                
                "/* Animated Ambient Background Blobs */" +
                ".blob { position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.35; animation: float 10s infinite alternate ease-in-out; }" +
                ".blob-1 { width: 350px; height: 350px; background: #3b82f6; top: -10%; left: -10%; }" +
                ".blob-2 { width: 400px; height: 400px; background: #6366f1; bottom: -15%; right: -10%; animation-delay: -5s; }" +
                ".blob-3 { width: 250px; height: 250px; background: #ec4899; top: 40%; left: 30%; opacity: 0.15; animation-duration: 8s; }" +
                "@keyframes float { 0% { transform: translate(0, 0) scale(1); } 100% { transform: translate(40px, 50px) scale(1.1); } }" +

                "/* Interactive Card Canvas */" +
                ".container { position: relative; z-index: 10; width: 90%; max-width: 520px; background: rgba(15, 23, 42, 0.75); border: 1px solid rgba(255, 255, 255, 0.12); backdrop-filter: blur(20px); border-radius: 24px; padding: 36px 28px; text-align: center; box-shadow: 0 25px 50px -12px rgba(0,0,0,0.7); display: flex; flex-direction: column; align-items: center; }" +

                "/* Animated Radar / Wifi Pulse Icon */" +
                ".radar-box { position: relative; width: 90px; height: 90px; display: flex; align-items: center; justify-content: center; margin-bottom: 24px; }" +
                ".pulse { position: absolute; width: 100%; height: 100%; border-radius: 50%; background: rgba(59, 130, 246, 0.2); animation: pulseWave 2s infinite ease-out; }" +
                ".pulse:nth-child(2) { animation-delay: 0.6s; }" +
                ".icon-center { position: relative; z-index: 2; width: 64px; height: 64px; background: linear-gradient(135deg, #1e293b, #0f172a); border: 1px solid rgba(59, 130, 246, 0.5); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #60a5fa; box-shadow: 0 0 20px rgba(59, 130, 246, 0.3); }" +
                "@keyframes pulseWave { 0% { transform: scale(0.6); opacity: 0.8; } 100% { transform: scale(1.6); opacity: 0; } }" +

                "h1 { font-size: 22px; font-weight: 700; color: #ffffff; margin-bottom: 8px; letter-spacing: -0.02em; }" +
                "p { font-size: 14px; color: #94a3b8; line-height: 1.5; margin-bottom: 24px; max-width: 380px; }" +

                "/* Interactive Pulse Tag */" +
                ".status-pill { display: inline-flex; align-items: center; gap: 8px; background: rgba(239, 68, 68, 0.12); border: 1px solid rgba(239, 68, 68, 0.3); color: #f87171; padding: 6px 14px; border-radius: 20px; font-size: 12px; font-weight: 600; margin-bottom: 24px; }" +
                ".status-dot { width: 8px; height: 8px; background: #ef4444; border-radius: 50%; animation: blink 1.2s infinite ease-in-out; }" +
                "@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.2; } }" +

                "/* Buttons */" +
                ".btn-group { width: 100%; display: flex; flex-direction: column; gap: 12px; }" +
                ".btn { width: 100%; padding: 14px; border-radius: 14px; font-size: 15px; font-weight: 600; cursor: pointer; border: none; transition: all 0.2s ease; outline: none; }" +
                ".btn-primary { background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%); color: #ffffff; box-shadow: 0 8px 20px rgba(37, 99, 235, 0.35); position: relative; overflow: hidden; }" +
                ".btn-primary:active { transform: scale(0.98); opacity: 0.9; }" +
                ".btn-secondary { background: rgba(255, 255, 255, 0.05); color: #cbd5e1; border: 1px solid rgba(255, 255, 255, 0.1); }" +
                ".btn-secondary:active { transform: scale(0.98); background: rgba(255, 255, 255, 0.1); }" +
                "</style></head><body>" +

                "<div class=\"blob blob-1\"></div>" +
                "<div class=\"blob blob-2\"></div>" +
                "<div class=\"blob blob-3\"></div>" +

                "<div class=\"container\">" +
                "<div class=\"radar-box\">" +
                "<div class=\"pulse\"></div>" +
                "<div class=\"pulse\"></div>" +
                "<div class=\"icon-center\">" +
                "<svg width=\"28\" height=\"28\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><line x1=\"1\" y1=\"1\" x2=\"23\" y2=\"23\"></line><path d=\"M16.72 11.06A10.94 10.94 0 0 1 19 12.55\"></path><path d=\"M5 12.55a10.94 10.94 0 0 1 5.17-2.39\"></path><path d=\"M10.71 5.05A16 16 0 0 1 22.58 9\"></path><path d=\"M1.42 9a15.91 15.91 0 0 1 4.7-2.88\"></path><path d=\"M8.53 16.11a6 6 0 0 1 6.95 0\"></path><line x1=\"12\" y1=\"20\" x2=\"12.01\" y2=\"20\"></line></svg>" +
                "</div>" +
                "</div>" +

                "<div class=\"status-pill\"><div class=\"status-dot\"></div> You're Offline</div>" +
                "<h1>Connection Interrupted</h1>" +
                "<p>H3LIUM CBT requires an active internet connection to load test questions and sync your responses.</p>" +

                "<div class=\"btn-group\">" +
                "<button class=\"btn btn-primary\" onclick=\"reloadPage()\">⚡ Reconnect Now</button>" +
                "<button class=\"btn btn-secondary\" onclick=\"location.href='https://h3lium-cbt.netlify.app/'\">Return to Home</button>" +
                "</div>" +
                "</div>" +

                "<script>" +
                "function reloadPage() {" +
                "  document.querySelector('.btn-primary').innerHTML = 'Connecting...';" +
                "  setTimeout(() => { location.href = '" + retryTarget + "'; }, 300);" +
                "}" +
                "</script>" +
                "</body></html>";

        view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
    }

    private boolean handleExternalLinks(String url) {
        if (url == null) return false;

        if (url.startsWith("whatsapp:") || url.contains("wa.me") || 
            url.contains("whatsapp.com") || url.contains("chat.whatsapp.com") ||
            url.startsWith("tg:") || url.contains("t.me") || url.contains("telegram.me") || 
            url.startsWith("intent://")) {
            
            try {
                Intent intent;
                if (url.startsWith("intent://")) {
                    intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                } else if (url.contains("t.me/") || url.contains("telegram.me/")) {
                    Uri parsedUri = Uri.parse(url);
                    String path = parsedUri.getPath();
                    
                    if (path != null && path.startsWith("/")) {
                        path = path.substring(1);
                    }

                    if (path != null && !path.isEmpty()) {
                        if (path.startsWith("+")) {
                            String inviteCode = path.substring(1);
                            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tg://join?invite=" + inviteCode));
                        } else if (path.startsWith("joinchat/")) {
                            String inviteCode = path.replace("joinchat/", "");
                            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tg://join?invite=" + inviteCode));
                        } else {
                            String[] parts = path.split("/");
                            if (parts.length >= 2) {
                                String domain = parts[0];
                                String postId = parts[1];
                                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=" + domain + "&post=" + postId));
                            } else {
                                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=" + parts[0]));
                            }
                        }
                    } else {
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    }
                } else {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                }

                if (intent != null) {
                    startActivity(intent);
                    return true;
                }
            } catch (Exception e) {
                try {
                    Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(fallbackIntent);
                    return true;
                } catch (Exception ex) {
                    Toast.makeText(this, "App not installed", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
            return true;
        }
        return false;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "H3LIUM Notifications";
            String description = "Channel for H3LIUM app notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
