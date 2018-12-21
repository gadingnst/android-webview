package com.example.sgnzst.webview;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Connection {

    public static boolean hasConnectivity(Context context){
        NetworkInfo info = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info == null){
            return false;
        }else{
            return true;
        }
    }

}
