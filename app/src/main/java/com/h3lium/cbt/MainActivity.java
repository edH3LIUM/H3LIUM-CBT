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

            // Catches errors on legacy Android devices
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                showCustomErrorPage(view, failingUrl);
            }

            // Catches ALL errors for any internal/external frames & main pages
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

    // Modern Branded Custom Offline Error Screen (Completely Hides internal URLs & offers smart retry)
    private void showCustomErrorPage(WebView view, String failedUrl) {
        String retryTarget = (failedUrl != null && !failedUrl.startsWith("data:")) ? failedUrl : "https://h3lium-cbt.netlify.app/";
        
        String errorHtml = "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">" +
                "<style>" +
                "* { box-sizing: border-box; margin: 0; padding: 0; }" +
                "body { background: #0b0f19; color: #e2e8f0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; padding: 24px; text-align: center; }" +
                ".card { background: rgba(30, 41, 59, 0.7); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 20px; padding: 36px 24px; max-width: 380px; width: 100%; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5), 0 8px 10px -6px rgba(0, 0, 0, 0.5); backdrop-filter: blur(10px); }" +
                ".icon-wrapper { width: 72px; height: 72px; background: rgba(239, 68, 68, 0.15); border: 1px solid rgba(239, 68, 68, 0.3); border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px auto; color: #f87171; font-size: 32px; }" +
                "h2 { font-size: 20px; font-weight: 700; color: #ffffff; margin-bottom: 8px; tracking: -0.01em; }" +
                "p { font-size: 14px; color: #94a3b8; line-height: 1.6; margin-bottom: 28px; }" +
                ".btn-group { display: flex; flex-direction: column; gap: 12px; }" +
                ".btn-primary { background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); color: #ffffff; border: none; padding: 14px 20px; font-size: 15px; font-weight: 600; border-radius: 12px; cursor: pointer; transition: all 0.2s ease; box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3); }" +
                ".btn-primary:active { transform: scale(0.97); opacity: 0.9; }" +
                ".btn-secondary { background: rgba(255, 255, 255, 0.05); color: #cbd5e1; border: 1px solid rgba(255, 255, 255, 0.1); padding: 12px 20px; font-size: 14px; font-weight: 500; border-radius: 12px; cursor: pointer; transition: all 0.2s ease; }" +
                ".btn-secondary:active { transform: scale(0.97); background: rgba(255, 255, 255, 0.1); }" +
                "</style></head><body>" +
                "<div class=\"card\">" +
                "<div class=\"icon-wrapper\">⚡</div>" +
                "<h2>Connection Lost</h2>" +
                "<p>Unable to load test content. Please check your internet connection and try reloading.</p>" +
                "<div class=\"btn-group\">" +
                "<button class=\"btn-primary\" onclick=\"location.href='" + retryTarget + "'\">Retry Loading</button>" +
                "<button class=\"btn-secondary\" onclick=\"location.href='https://h3lium-cbt.netlify.app/'\">Go to Dashboard</button>" +
                "</div>" +
                "</div>" +
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
