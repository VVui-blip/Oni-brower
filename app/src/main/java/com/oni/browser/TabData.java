package com.oni.browser;

import android.webkit.WebView;
import android.widget.TextView;

/**
 * Đại diện cho 1 tab: giữ WebView riêng + view hiển thị trên tab strip.
 */
public class TabData {
    public WebView webView;
    public android.view.View stripView;
    public TextView titleView;
    public String title = "tab~$";
    public String url = "";

    public TabData(WebView webView, android.view.View stripView, TextView titleView) {
        this.webView = webView;
        this.stripView = stripView;
        this.titleView = titleView;
    }
}
