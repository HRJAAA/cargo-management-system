package com.tom_roush.pdfbox.pdmodel;

import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import java.io.IOException;
import java.io.OutputStream;

public class PDPageContentStream implements AutoCloseable {
    private OutputStream os;
    private PDDocument doc;
    private PDPage page;

    public PDPageContentStream(PDDocument doc, PDPage page) throws IOException {
        this.doc = doc;
        this.page = page;
    }

    public void setFont(PDType1Font font, float size) throws IOException {}
    public void beginText() throws IOException {}
    public void endText() throws IOException {}
    public void newLineAtOffset(float x, float y) throws IOException {}
    public void showText(String text) throws IOException {}
    public void close() throws IOException {}
}
