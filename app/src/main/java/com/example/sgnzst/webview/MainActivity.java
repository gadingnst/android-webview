package com.example.sgnzst.webview;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    // set ssl and domain
    public final static String ssl = "https";
    public final static String domain = "sutanlab.js.org";

    public static Context mainContext;
    public static WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainContext = getApplicationContext();

        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.setWebViewClient(new WebViewClient());

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

//        webSettings.setAppCacheMaxSize( 5 * 1024 * 1024 ); // 5MB
//        webSettings.setAppCachePath(mainContext.getCacheDir().getAbsolutePath());
//        webSettings.setAllowFileAccess(true);
//        webSettings.setAppCacheEnabled(true);
//        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        if (Connection.hasConnectivity(this)) {
            mWebView.loadUrl(ssl+"://"+domain);
        } else {
            Toast.makeText(this, "No Connection !", Toast.LENGTH_LONG).show();
            mWebView.loadUrl("file:///android_asset/noconnection.html");
        }

        mWebView.setWebViewClient(new WebClient());
    }

    @Override
    public void onBackPressed() {
        if(mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
