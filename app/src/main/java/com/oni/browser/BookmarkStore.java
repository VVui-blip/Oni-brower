package com.oni.browser;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Lưu bookmark dạng "title|||url" trong 1 SharedPreferences StringSet.
 */
public class BookmarkStore {
    private static final String PREFS = "oni_bookmarks";
    private static final String KEY = "items";
    private static final String SEP = "|||";

    private final SharedPreferences prefs;

    public BookmarkStore(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void add(String title, String url) {
        Set<String> set = new LinkedHashSet<>(prefs.getStringSet(KEY, new LinkedHashSet<>()));
        set.add((title == null || title.isEmpty() ? url : title) + SEP + url);
        prefs.edit().putStringSet(KEY, set).apply();
    }

    public void remove(String entry) {
        Set<String> set = new LinkedHashSet<>(prefs.getStringSet(KEY, new LinkedHashSet<>()));
        set.remove(entry);
        prefs.edit().putStringSet(KEY, set).apply();
    }

    public Set<String> all() {
        return prefs.getStringSet(KEY, new LinkedHashSet<>());
    }

    public static String titleOf(String entry) {
        return entry.split(java.util.regex.Pattern.quote(SEP))[0];
    }

    public static String urlOf(String entry) {
        String[] p = entry.split(java.util.regex.Pattern.quote(SEP));
        return p.length > 1 ? p[1] : "";
    }
}
