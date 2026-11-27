package com.example.webapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String VERSION = "Autism-IT app v6";
    private static final String SITE_URL = "https://www.autismit.ie";
    private static final String SITE_DOMAIN = "autismit.ie";

    private WebView webView;
    private TextView status;

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

    private void setStatus(String message, boolean hideWeb) {
        status.setText(VERSION + "\n\n" + message);
        webView.setVisibility(hideWeb ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);

        status = new TextView(this);
        status.setTextColor(Color.parseColor("#17283d"));
        status.setTextSize(15);
        status.setGravity(Gravity.TOP);
        status.setPadding(48, 140, 48, 48);
        status.setText(VERSION + "\n\nLoading " + SITE_URL + " ...");
        root.addView(status, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.TRANSPARENT);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setUserAgentString(settings.getUserAgentString().replace("; wv", ""));

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimetype, long contentLength) {
                if (url.toLowerCase().contains(".apk")) {
                    openOutside(Uri.parse(url));
                } else {
                    setStatus("The website sent a file instead of a page.\n\n"
                            + "Address: " + url + "\n\n"
                            + "Type: " + mimetype, true);
                }
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
                    return false;
                }
                openOutside(uri);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                status.setText(VERSION + "\n\nLoading " + url + " ...");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().flush();
                final String shownUrl = url;
                view.evaluateJavascript(
                        "(function(){var h=document.documentElement.outerHTML;"
                                + "return 'Title: '+document.title+'\\nHTML length: '+h.length+'\\n\\n'+h.substring(0,500);})()",
                        new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                status.setText(VERSION + "\n\nPage finished:\n" + shownUrl
                                        + "\n\nWhat the page contains:\n" + value);
                            }
                        });
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    setStatus("Could not load the page:\n" + error.getDescription(), true);
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                if (request.isForMainFrame() && errorResponse.getStatusCode() >= 400) {
                    setStatus("The website answered with error " + errorResponse.getStatusCode(), true);
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                String host = Uri.parse(error.getUrl()).getHost();
                if (isOurHost(host)) {
                    handler.proceed();
                } else {
                    handler.cancel();
                    setStatus("Secure connection problem (SSL error " + error.getPrimaryError() + ")", true);
                }
            }
        });

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(SITE_URL);
        }
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
