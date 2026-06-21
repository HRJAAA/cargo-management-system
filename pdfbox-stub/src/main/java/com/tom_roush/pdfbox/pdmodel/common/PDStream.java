package com.tom_roush.pdfbox.pdmodel.common;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PDStream {
    private PDDocument doc;
    public PDStream(PDDocument doc) { this.doc = doc; }
    public OutputStream createOutputStream() throws IOException { return new java.io.ByteArrayOutputStream(); }
    public InputStream createInputStream() throws IOException { return new java.io.ByteArrayInputStream(new byte[0]); }
}
