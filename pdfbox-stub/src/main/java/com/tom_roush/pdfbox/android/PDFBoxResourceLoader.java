package com.tom_roush.pdfbox.android;

import android.content.Context;

public class PDFBoxResourceLoader {
    private static Context context;
    public static void init(Context ctx) { context = ctx.getApplicationContext(); }
    public static Context getContext() { return context; }
}
