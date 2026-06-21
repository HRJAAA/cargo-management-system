package com.pdfstream.edit;

import android.app.Application;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PDFBoxResourceLoader.init(this);
    }
}
