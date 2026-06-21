package com.tom_roush.pdfbox.rendering;

import android.graphics.Bitmap;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import java.io.IOException;

public class PDFRenderer {
    private PDDocument doc;
    public PDFRenderer(PDDocument doc) { this.doc = doc; }
    public Bitmap renderImageWithDPI(int pageIndex, int dpi) throws IOException {
        Bitmap bmp = Bitmap.createBitmap(595 * dpi / 72, 842 * dpi / 72, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(0xFFFFFFFF);
        return bmp;
    }
}
