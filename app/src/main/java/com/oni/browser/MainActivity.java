package com.oni.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private LinearLayout tabStrip;
    private FrameLayout webViewContainer;
    private EditText urlBar;
    private ProgressBar progressBar;
    private ImageButton btnBack, btnForward, btnReload, btnAddTab, btnMenu;

    private final List<TabData> tabs = new ArrayList<>();
    private int currentIndex = -1;

    private boolean desktopMode = false;
    private BookmarkStore bookmarkStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bookmarkStore = new BookmarkStore(this);

        tabStrip = findViewById(R.id.tabStrip);
        webViewContainer = findViewById(R.id.webViewContainer);
        urlBar = findViewById(R.id.urlBar);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);
        btnForward = findViewById(R.id.btnForward);
        btnReload = findViewById(R.id.btnReload);
        btnAddTab = findViewById(R.id.btnAddTab);
        btnMenu = findViewById(R.id.btnMenu);

        btnBack.setOnClickListener(v -> {
            WebView wv = currentWebView();
            if (wv != null && wv.canGoBack()) wv.goBack();
        });

        btnForward.setOnClickListener(v -> {
            WebView wv = currentWebView();
            if (wv != null && wv.canGoForward()) wv.goForward();
        });

        btnReload.setOnClickListener(v -> {
            WebView wv = currentWebView();
            if (wv != null) wv.reload();
        });

        btnAddTab.setOnClickListener(v -> openNewTab(getString(R.string.home_url)));

        btnMenu.setOnClickListener(this::showMenu);

        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadFromUrlBar();
                return true;
            }
            return false;
        });

        // Mở tab đầu tiên
        openNewTab(getString(R.string.home_url));
    }

    // ---------- Tab management ----------

    private void openNewTab(String url) {
        WebView webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setupWebView(webView);

        View stripView = LayoutInflater.from(this).inflate(R.layout.tab_item, tabStrip, false);
        TextView titleView = stripView.findViewById(R.id.tabTitle);
        ImageButton closeBtn = stripView.findViewById(R.id.tabClose);

        TabData tab = new TabData(webView, stripView, titleView);
        tabs.add(tab);
        int index = tabs.size() - 1;

        stripView.setOnClickListener(v -> switchToTab(index));
        closeBtn.setOnClickListener(v -> closeTab(index));

        tabStrip.addView(stripView);
        webViewContainer.addView(webView);

        switchToTab(index);
        webView.loadUrl(url);
    }

    private void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        TabData tab = tabs.get(index);
        webViewContainer.removeView(tab.webView);
        tabStrip.removeView(tab.stripView);
        tab.webView.destroy();
        tabs.remove(index);

        if (tabs.isEmpty()) {
            finish();
            return;
        }
        int newIndex = Math.max(0, index - 1);
        switchToTab(newIndex);
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        currentIndex = index;
        for (int i = 0; i < tabs.size(); i++) {
            TabData t = tabs.get(i);
            t.webView.setVisibility(i == index ? View.VISIBLE : View.GONE);
            t.stripView.setBackgroundResource(i == index ? R.drawable.tab_bg_active : R.drawable.tab_bg_inactive);
        }
        urlBar.setText(tabs.get(index).url);
    }

    private WebView currentWebView() {
        if (currentIndex < 0 || currentIndex >= tabs.size()) return null;
        return tabs.get(currentIndex).webView;
    }

    private TabData currentTab() {
        if (currentIndex < 0 || currentIndex >= tabs.size()) return null;
        return tabs.get(currentIndex);
    }

    // ---------- WebView setup ----------

    private void setupWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        if (desktopMode) {
            settings.setUserAgentString(getString(R.string.ua_desktop));
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                TabData tab = findTab(view);
                if (tab != null) {
                    tab.url = url;
                    String title = view.getTitle();
                    tab.title = TextUtils.isEmpty(title) ? url : title;
                    tab.titleView.setText(tab.title);
                    if (view == currentWebView()) {
                        urlBar.setText(url);
                    }
                }
                progressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (view == currentWebView()) {
                    progressBar.setProgress(newProgress);
                    if (newProgress >= 100) progressBar.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                TabData tab = findTab(view);
                if (tab != null && !TextUtils.isEmpty(title)) {
                    tab.title = title;
                    tab.titleView.setText(title);
                }
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimetype);
                request.addRequestHeader("User-Agent", userAgent);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                String filename = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype);
                request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, filename);
                DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                dm.enqueue(request);
                Toast.makeText(this, "downloading: " + filename, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "download failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private TabData findTab(WebView view) {
        for (TabData t : tabs) if (t.webView == view) return t;
        return null;
    }

    // ---------- Address bar ----------

    private void loadFromUrlBar() {
        String input = urlBar.getText().toString().trim();
        if (input.isEmpty()) return;
        String url = normalizeUrl(input);
        WebView wv = currentWebView();
        if (wv != null) wv.loadUrl(url);
        hideKeyboard();
    }

    private String normalizeUrl(String input) {
        if (input.startsWith("http://") || input.startsWith("https://")
                || input.startsWith("file://") || input.startsWith("about:")) {
            return input;
        }
        // Nhìn giống domain (có dấu chấm, không có khoảng trắng) -> coi là URL
        if (!input.contains(" ") && input.contains(".")) {
            return "https://" + input;
        }
        // Ngược lại -> search
        return "https://duckduckgo.com/html/?q=" + Uri.encode(input);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
    }

    // ---------- Menu ----------

    private void showMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("New tab");
        menu.getMenu().add("Close tab");
        menu.getMenu().add("Bookmark this page");
        menu.getMenu().add("Bookmarks");
        menu.getMenu().add(desktopMode ? "Desktop mode: ON" : "Desktop mode: OFF");
        menu.getMenu().add("Clear browsing data");
        menu.getMenu().add("Home");

        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            switch (title) {
                case "New tab":
                    openNewTab(getString(R.string.home_url));
                    return true;
                case "Close tab":
                    closeTab(currentIndex);
                    return true;
                case "Bookmark this page":
                    bookmarkCurrentPage();
                    return true;
                case "Bookmarks":
                    showBookmarks();
                    return true;
                case "Clear browsing data":
                    clearBrowsingData();
                    return true;
                case "Home":
                    WebView wv = currentWebView();
                    if (wv != null) wv.loadUrl(getString(R.string.home_url));
                    return true;
                default:
                    if (title.startsWith("Desktop mode")) {
                        toggleDesktopMode();
                        return true;
                    }
                    return false;
            }
        });
        menu.show();
    }

    private void bookmarkCurrentPage() {
        TabData tab = currentTab();
        if (tab == null || TextUtils.isEmpty(tab.url)) return;
        bookmarkStore.add(tab.title, tab.url);
        Toast.makeText(this, "bookmarked: " + tab.title, Toast.LENGTH_SHORT).show();
    }

    private void showBookmarks() {
        Set<String> entries = bookmarkStore.all();
        if (entries.isEmpty()) {
            Toast.makeText(this, "no bookmarks yet", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] titles = new String[entries.size()];
        String[] urls = new String[entries.size()];
        int i = 0;
        for (String e : entries) {
            titles[i] = BookmarkStore.titleOf(e);
            urls[i] = BookmarkStore.urlOf(e);
            i++;
        }
        new AlertDialog.Builder(this)
                .setTitle("Bookmarks")
                .setItems(titles, (dialog, which) -> {
                    WebView wv = currentWebView();
                    if (wv != null) wv.loadUrl(urls[which]);
                })
                .setNegativeButton("close", null)
                .show();
    }

    private void toggleDesktopMode() {
        desktopMode = !desktopMode;
        for (TabData t : tabs) {
            WebSettings s = t.webView.getSettings();
            s.setUserAgentString(desktopMode ? getString(R.string.ua_desktop) : null);
            t.webView.reload();
        }
        Toast.makeText(this, "Desktop mode: " + (desktopMode ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
    }

    private void clearBrowsingData() {
        WebView wv = currentWebView();
        if (wv != null) {
            wv.clearCache(true);
            wv.clearHistory();
            wv.clearFormData();
        }
        android.webkit.CookieManager.getInstance().removeAllCookies(null);
        Toast.makeText(this, "cleared", Toast.LENGTH_SHORT).show();
    }

    // ---------- Back button ----------

    @Override
    public void onBackPressed() {
        WebView wv = currentWebView();
        if (wv != null && wv.canGoBack()) {
            wv.goBack();
        } else if (tabs.size() > 1) {
            closeTab(currentIndex);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        for (TabData t : tabs) t.webView.destroy();
        super.onDestroy();
    }
}
