package com.example.sgnzst.webview;

import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import static com.example.sgnzst.webview.MainActivity.domain;
import static com.example.sgnzst.webview.MainActivity.mainContext;

public class WebClient extends WebViewClient {
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (Uri.parse(url).getHost().endsWith(domain)) {
            if (!Connection.hasConnectivity(mainContext)) {
                Toast.makeText(mainContext, "No Connection !", Toast.LENGTH_LONG).show();
                view.loadUrl("file:///android_asset/noconnection.html");
            }else {
                if (view.getUrl().equals("file:///android_asset/noconnection.html")){
                    MainActivity act = new MainActivity();
                    act.onBackPressed();
                }
            }
            return false;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        view.getContext().startActivity(intent);
        return true;
    }

}
