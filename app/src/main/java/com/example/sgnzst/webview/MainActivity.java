package com.example.sgnzst.webview;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    static boolean WEB_JSCRIPT     = Statics.WEB_JSCRIPT;
    static boolean WEB_FUPLOAD     = Statics.WEB_FUPLOAD;
    static boolean WEB_CAMUPLOAD   = Statics.WEB_CAMUPLOAD;
    static boolean WEB_ONLYCAM		= Statics.WEB_ONLYCAM;
    static boolean WEB_MULFILE     = Statics.WEB_MULFILE;
    static boolean WEB_LOCATION    = Statics.WEB_LOCATION;
    static boolean WEB_RATINGS     = Statics.WEB_RATINGS;
    static boolean WEB_PBAR        = Statics.WEB_PBAR;
    static boolean WEB_ZOOM        = Statics.WEB_ZOOM;
    static boolean WEB_SFORM       = Statics.WEB_SFORM;
    static boolean WEB_OFFLINE		= Statics.WEB_OFFLINE;
    static boolean WEB_EXTURL		= Statics.WEB_EXTURL;

    //Configuration variables
    private static String WEB_URL      = Statics.WEB_URL;
    private static String WEB_F_TYPE   = Statics.WEB_F_TYPE;

    public static String WEB_HOST		= host(WEB_URL);

    //Careful with these variable names if altering
    WebView webview;
    ProgressBar progress;
    TextView loadingText;
    NotificationManager notification;
    Notification notificationNew;

    private String camMessage;
    private ValueCallback<Uri> fileMessage;
    private ValueCallback<Uri[]> filePath;
    private final static int fileReq = 1;

    private final static int locPerm = 1;
    private final static int filePerm = 2;

    private SecureRandom random = new SecureRandom();

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary));
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == fileReq) {
                    if (null == filePath) {
                        return;
                    }
                    if (intent == null || intent.getData() == null) {
                        if (camMessage != null) {
                            results = new Uri[]{Uri.parse(camMessage)};
                        }
                    } else {
                        String dataString = intent.getDataString();
                        if (dataString != null) {
                            results = new Uri[]{ Uri.parse(dataString) };
                        } else {
                            if(WEB_MULFILE) {
                                if (intent.getClipData() != null) {
                                    final int numSelectedFiles = intent.getClipData().getItemCount();
                                    results = new Uri[numSelectedFiles];
                                    for (int i = 0; i < numSelectedFiles; i++) {
                                        results[i] = intent.getClipData().getItemAt(i).getUri();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            filePath.onReceiveValue(results);
            filePath = null;
        } else {
            if (requestCode == fileReq) {
                if (null == fileMessage) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                fileMessage.onReceiveValue(result);
                fileMessage = null;
            }
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "WrongViewCast"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w("READ_PERM = ",Manifest.permission.READ_EXTERNAL_STORAGE);
        Log.w("WRITE_PERM = ",Manifest.permission.WRITE_EXTERNAL_STORAGE);
        //Prevent the app from being started again when it is still alive in the background
        if (!isTaskRoot()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        if (WEB_PBAR) {
            progress = findViewById(R.id.progress);
        } else {
            findViewById(R.id.progress).setVisibility(View.GONE);
        }
        loadingText = findViewById(R.id.loading_text);
        Handler handler = new Handler();

        //Launching app rating request
        if (WEB_RATINGS) {
            handler.postDelayed(new Runnable() { public void run() { getRating(); }}, 1000 * 60); //running request after few moments
        }

        //Getting basic device information
        getInfo();

        //Getting GPS location of device if given permission
        if(WEB_LOCATION && !checkPermission(1)){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, locPerm);
        }
        getLoc();

        webview = findViewById(R.id.webview);

        //Webview settings; defaults are customized for best performance
        WebSettings webSettings = webview.getSettings();

        if(!WEB_OFFLINE){
            webSettings.setJavaScriptEnabled(WEB_JSCRIPT);
        }
        webSettings.setSaveFormData(WEB_SFORM);
        webSettings.setSupportZoom(WEB_ZOOM);
        webSettings.setGeolocationEnabled(WEB_LOCATION);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setDomStorageEnabled(true);

        webview.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {

                if(!checkPermission(2)){
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, filePerm);
                }else {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                    request.setMimeType(mimeType);
                    String cookies = CookieManager.getInstance().getCookie(url);
                    request.addRequestHeader("cookie", cookies);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setDescription(getString(R.string.dl_downloading));
                    request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    assert dm != null;
                    dm.enqueue(request);
                    Toast.makeText(getApplicationContext(), getString(R.string.dl_downloading2), Toast.LENGTH_LONG).show();
                }
            }
        });

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
            webview.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        } else if (Build.VERSION.SDK_INT >= 19) {
            webview.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        webview.setVerticalScrollBarEnabled(false);
        webview.setWebViewClient(new Callback());

        //Rendering the default URL
        render(WEB_URL, false);

        webview.setWebChromeClient(new WebChromeClient() {
            //Handling input[type="file"] requests for android API 16+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture){
                if(WEB_FUPLOAD) {
                    fileMessage = uploadMsg;
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType(WEB_F_TYPE);
                    if(WEB_MULFILE) {
                        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    }
                    startActivityForResult(Intent.createChooser(i, getString(R.string.fl_chooser)), fileReq);
                }
            }
            //Handling input[type="file"] requests for android API 21+
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams){
                getFile();
                if(WEB_FUPLOAD) {
                    if (filePath != null) {
                        filePath.onReceiveValue(null);
                    }
                    filePath = filePathCallback;
                    Intent takePictureIntent = null;
                    if (WEB_CAMUPLOAD) {
                        takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
                            File photoFile = null;
                            try {
                                photoFile = createImage();
                                takePictureIntent.putExtra("PhotoPath", camMessage);
                            } catch (IOException ex) {
                                Log.e(TAG, "Image file creation failed", ex);
                            }
                            if (photoFile != null) {
                                camMessage = "file:" + photoFile.getAbsolutePath();
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                            } else {
                                takePictureIntent = null;
                            }
                        }
                    }
                    Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    if(!WEB_ONLYCAM) {
                        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                        contentSelectionIntent.setType(WEB_F_TYPE);
                        if (WEB_MULFILE) {
                            contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        }
                    }
                    Intent[] intentArray;
                    if (takePictureIntent != null) {
                        intentArray = new Intent[]{takePictureIntent};
                    } else {
                        intentArray = new Intent[0];
                    }

                    Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                    chooserIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.fl_chooser));
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                    startActivityForResult(chooserIntent, fileReq);
                }
                return true;
            }

            //Getting webview rendering progress
            @Override
            public void onProgressChanged(WebView view, int p) {
                if (WEB_PBAR) {
                    progress.setProgress(p);
                    if (p == 100) {
                        progress.setProgress(0);
                    }
                }
            }

            // overload the geoLocations permissions prompt to always allow instantly as app permission was granted previously
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if(Build.VERSION.SDK_INT < 23 || (Build.VERSION.SDK_INT >= 23 && checkPermission(1))){
                    // location permissions were granted previously so auto-approve
                    callback.invoke(origin, true, false);
                } else {
                    // location permissions not granted so request them
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, locPerm);
                }
            }
        });
        if (getIntent().getData() != null) {
            String path     = getIntent().getDataString();
            /*
            If you want to check or use specific directories or schemes or hosts

            Uri data        = getIntent().getData();
            String scheme   = data.getScheme();
            String host     = data.getHost();
            List<String> pr = data.getPathSegments();
            String param1   = pr.get(0);
            */
            render(path, false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //Coloring the "recent apps" tab header; doing it onResume, as an insurance
        if (Build.VERSION.SDK_INT >= 23) {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.mp_logo);
            ActivityManager.TaskDescription taskDesc;
            taskDesc = new ActivityManager.TaskDescription(getString(R.string.app_name), bm, getColor(R.color.colorPrimary));
            MainActivity.this.setTaskDescription(taskDesc);
        }
        getLoc();
    }

    //Setting activity layout visibility
    private class Callback extends WebViewClient {
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            getLoc();
        }

        public void onPageFinished(WebView view, String url) {
            findViewById(R.id.welcome).setVisibility(View.GONE);
            findViewById(R.id.webview).setVisibility(View.VISIBLE);
        }
        //For android below API 23
        @SuppressWarnings("deprecation")
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Toast.makeText(getApplicationContext(), getString(R.string.went_wrong), Toast.LENGTH_SHORT).show();
            render("file:///android_asset/noconnection.html", false);
        }

        //Overriding webview URLs
        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return urlAct(view, url);
        }

        //Overriding webview URLs for API 23+ [suggested by github.com/JakePou]
        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return urlAct(view, request.getUrl().toString());
        }
    }

    //Random ID creation function to help get fresh cache every-time webview reloaded
    public String randomId() {
        return new BigInteger(130, random).toString(32);
    }

    //Opening URLs inside webview with request
    void render(String url, Boolean tab) {
        if (tab) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } else {
            if(url.contains("?")){ // check to see whether the url already has query parameters and handle appropriately.
                url += "&";
            } else {
                url += "?";
            }
            url += "rid="+randomId();
            webview.loadUrl(url);
        }
    }

    //Actions based on shouldOverrideUrlLoading
    public boolean urlAct(WebView view, String url){
        boolean a = true;
        //Show toast error if not connected to the network
        if (!WEB_OFFLINE && !Connection.hasConnectivity(MainActivity.this)) {
            Toast.makeText(getApplicationContext(), getString(R.string.check_connection), Toast.LENGTH_SHORT).show();

            //Use this in a hyperlink to redirect back to default URL :: href="refresh:android"
        } else if (url.startsWith("refresh:")) {
            render(WEB_URL, false);

            //Use this in a hyperlink to launch default phone dialer for specific number :: href="tel:+919876543210"
        } else if (url.startsWith("tel:")) {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
            startActivity(intent);

            //Use this to open your apps page on google play store app :: href="rate:android"
        } else if (url.startsWith("rate:")) {
            final String app_package = getPackageName(); //requesting app package name from Context or Activity object
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + app_package)));
            } catch (ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + app_package)));
            }

            //Sharing content from your webview to external apps :: href="share:URL" and remember to place the URL you want to share after share:___
        } else if (url.startsWith("share:")) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, view.getTitle());
            intent.putExtra(Intent.EXTRA_TEXT, view.getTitle()+"\nVisit: "+(Uri.parse(url).toString()).replace("share:",""));
            startActivity(Intent.createChooser(intent, getString(R.string.share_w_friends)));

            //Use this in a hyperlink to exit your app :: href="exit:android"
        } else if (url.startsWith("exit:")) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            //Getting location for offline files
        } else if (url.startsWith("offloc:")) {
            String offloc = WEB_URL+"?loc="+getLoc();
            render(offloc,false);
            Log.d("OFFLINE LOC REQ",offloc);

            //Opening external URLs in android default web browser
        } else if (WEB_EXTURL && !host(url).equals(WEB_HOST)) {
            render(url,true);
        }else {
            a = false;
        }
        return a;
    }

    //Getting host name
    public static String host(String url){
        if (url == null || url.length() == 0) {
            return "";
        }
        int dslash = url.indexOf("//");
        if (dslash == -1) {
            dslash = 0;
        } else {
            dslash += 2;
        }
        int end = url.indexOf('/', dslash);
        end = end >= 0 ? end : url.length();
        int port = url.indexOf(':', dslash);
        end = (port > 0 && port < end) ? port : end;
        Log.w("URL Host: ",url.substring(dslash, end));
        return url.substring(dslash, end);
    }

    //Getting device basic information
    public void getInfo(){
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setCookie(WEB_URL, "DEVICE=android");
        cookieManager.setCookie(WEB_URL, "DEV_API=" + Build.VERSION.SDK_INT);
    }

    //Checking permission for storage and camera for writing and uploading images
    public void getFile(){
        String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

        //Checking for storage permission to write images for upload
        if (WEB_FUPLOAD && WEB_CAMUPLOAD && !checkPermission(2) && !checkPermission(3)) {
            ActivityCompat.requestPermissions(MainActivity.this, perms, filePerm);

            //Checking for WRITE_EXTERNAL_STORAGE permission
        } else if (WEB_FUPLOAD && !checkPermission(2)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, filePerm);

            //Checking for CAMERA permissions
        } else if (WEB_CAMUPLOAD && !checkPermission(3)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, filePerm);
        }
    }

    //Using cookies to update user locations
    public String getLoc(){
        String newloc = "0,0";
        //Checking for location permissions
        if (WEB_LOCATION && ((Build.VERSION.SDK_INT >= 23 && checkPermission(1)) || Build.VERSION.SDK_INT < 23)) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            GPSTrack gps;
            gps = new GPSTrack(MainActivity.this);
            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            if (gps.canGetLocation()) {
                if (latitude != 0 || longitude != 0) {
                    if(!WEB_OFFLINE) {
                        cookieManager.setCookie(WEB_URL, "lat=" + latitude);
                        cookieManager.setCookie(WEB_URL, "long=" + longitude);
                    }
                    //Log.w("New Updated Location:", latitude + "," + longitude);  //enable to test dummy latitude and longitude
                    newloc = latitude+","+longitude;
                } else {
                    Log.w("New Updated Location:", "NULL");
                }
            } else {
                showNotification(1, 1);
                Log.w("New Updated Location:", "FAIL");
            }
        }
        return newloc;
    }

    //Checking if particular permission is given or not
    public boolean checkPermission(int permission){
        switch(permission){
            case 1:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

            case 2:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

            case 3:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        }
        return false;
    }

    //Creating image file for upload
    private File createImage() throws IOException {
        @SuppressLint("SimpleDateFormat")
        String file_name    = new SimpleDateFormat("yyyy_mm_ss").format(new Date());
        String new_name     = "file_"+file_name+"_";
        File sd_directory   = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(new_name, ".jpg", sd_directory);
    }

    //Launching app rating dialoge [developed by github.com/hotchemi]
    public void getRating() {
        if (Connection.hasConnectivity(MainActivity.this)) {
            AppRate.with(this)
                    .setStoreType(StoreType.GOOGLEPLAY)     //default is Google Play, other option is Amazon App Store
                    .setInstallDays(Statics.SYS_DAYS)
                    .setLaunchTimes(Statics.SYS_TIMES)
                    .setRemindInterval(Statics.SYS_INTERVAL)
                    .setTitle(R.string.rate_dialog_title)
                    .setMessage(R.string.rate_dialog_message)
                    .setTextLater(R.string.rate_dialog_cancel)
                    .setTextNever(R.string.rate_dialog_no)
                    .setTextRateNow(R.string.rate_dialog_ok)
                    .monitor();
            AppRate.showRateDialogIfMeetsConditions(this);
        }
        //for more customizations, look for AppRate and DialogManager
    }

    //Creating custom notifications with IDs
    public void showNotification(int type, int id) {
        long when = System.currentTimeMillis();
        notification = (NotificationManager) MainActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent i = new Intent();
        if (type == 1) {
            i.setClass(MainActivity.this, MainActivity.class);
        } else if (type == 2) {
            i.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        } else {
            i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            i.setData(Uri.parse("package:" + MainActivity.this.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        }
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, "");
        switch(type){
            case 1:
                builder.setTicker(getString(R.string.app_name));
                builder.setContentTitle(getString(R.string.loc_fail));
                builder.setContentText(getString(R.string.loc_fail_text));
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.loc_fail_more)));
                builder.setVibrate(new long[]{350,350,350,350,350});
                builder.setSmallIcon(R.mipmap.mp_logo);
                break;

            case 2:
                builder.setTicker(getString(R.string.app_name));
                builder.setContentTitle(getString(R.string.loc_perm));
                builder.setContentText(getString(R.string.loc_perm_text));
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.loc_perm_more)));
                builder.setVibrate(new long[]{350, 700, 350, 700, 350});
                builder.setSound(alarmSound);
                builder.setSmallIcon(R.mipmap.mp_logo);
                break;
        }
        builder.setOngoing(false);
        builder.setAutoCancel(true);
        builder.setContentIntent(pendingIntent);
        builder.setWhen(when);
        builder.setContentIntent(pendingIntent);
        notificationNew = builder.build();
        notification.notify(id, notificationNew);
    }

    //Checking if users allowed the requested permissions or not
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults){
        switch (requestCode){
            case 1: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    getLoc();
                }
            }
        }
    }

    //Action on back key tap/click
    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webview.canGoBack()) {
                        webview.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState ){
        super.onSaveInstanceState(outState);
        webview.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        webview.restoreState(savedInstanceState);
    }
}
