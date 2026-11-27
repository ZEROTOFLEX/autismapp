package com.example.webapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;
    private static final String SITE_URL = "https://www.autismit.ie";
    private static final String SITE_DOMAIN = "autismit.ie";

    private boolean isOurHost(String host) {
        return host != null && (host.equals(SITE_DOMAIN) || host.endsWith("." + SITE_DOMAIN));
    }

    private void openOutside(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            // nothing on the phone can open this link
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        // Any file download (such as the APK) is handed to the phone's browser
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimetype, long contentLength) {
                openOutside(Uri.parse(url));
            }
        });

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase();

                boolean webLink = "http".equals(scheme) || "https".equals(scheme);
                boolean stayInApp = webLink && isOurHost(uri.getHost()) && !path.endsWith(".apk");

                if (stayInApp) {
                    return false; // load inside the app
                }
                openOutside(uri); // mailto:, tel:, other websites and the APK open outside
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    showError(view, "Could not load the page: " + error.getDescription());
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                String host = Uri.parse(error.getUrl()).getHost();
                if (isOurHost(host)) {
                    handler.proceed(); // temporary: remove once the website certificate chain is fixed
                } else {
                    handler.cancel();
                    showError(view, "Secure connection problem (SSL error " + error.getPrimaryError() + ")");
                }
            }
        });

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(SITE_URL);
        }
    }

    private void showError(WebView view, String message) {
        String html = "<html><body style='font-family:sans-serif;padding:24px;background:#fff;color:#17283d'>"
                + "<h2>Autism-IT</h2><p>" + message + "</p>"
                + "<p>Check your internet connection, then close and reopen the app.</p></body></html>";
        view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
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
