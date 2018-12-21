package com.example.sgnzst.webview;

public class Statics {
    //Configuration variables
    static String WEB_URL = "https://sutanlab.js.org"; //complete URL of your website or webpage
    static String WEB_F_TYPE = "*/*";  //to upload any file type using "*/*"; check file type references for more

    //Permission variables
    static boolean WEB_JSCRIPT = true; //enable JavaScript for webview
    static boolean WEB_FUPLOAD = true; //upload file from webview
    static boolean WEB_CAMUPLOAD = true; //enable upload from camera for photos
    static boolean WEB_ONLYCAM = false; //incase you want only camera files to upload
    static boolean WEB_MULFILE = false; //upload multiple files in webview
    static boolean WEB_LOCATION = true; //track GPS locations
    static boolean WEB_RATINGS = true; //show ratings dialog; auto configured, edit method get_rating() for customizations
    static boolean WEB_PBAR = false; //show progress bar in app
    static boolean WEB_ZOOM = false; //zoom control for webpages view
    static boolean WEB_SFORM = false; //save form cache and auto-fill information
    static boolean WEB_OFFLINE = false; //whether the loading webpages are offline or online
    static boolean WEB_EXTURL = true; //open external url with default browser instead of app webview

    //Rating system variables
    static int SYS_DAYS = 3; //after how many days of usage would you like to show the dialoge
    static int SYS_TIMES = 10; //overall request launch times being ignored
    static int SYS_INTERVAL = 2; //reminding users to rate after days interval
}
