package com.tom_roush.pdfbox.pdmodel;

import com.tom_roush.pdfbox.pdmodel.common.PDStream;

public class PDPage {
    private PDStream contents;
    private PDRectangle mediaBox = new PDRectangle(595, 842);

    public PDPage() {}
    public PDPage(PDRectangle mediaBox) { this.mediaBox = mediaBox; }

    public PDStream getContents() { return contents; }
    public void setContents(PDStream stream) { this.contents = stream; }
    public PDRectangle getMediaBox() { return mediaBox; }
}
