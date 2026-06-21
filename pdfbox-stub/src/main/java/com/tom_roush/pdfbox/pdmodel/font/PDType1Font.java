package com.tom_roush.pdfbox.pdmodel.font;

public class PDType1Font {
    public static final PDType1Font HELVETICA = new PDType1Font("Helvetica");
    public static final PDType1Font HELVETICA_BOLD = new PDType1Font("Helvetica-Bold");
    public static final PDType1Font TIMES_ROMAN = new PDType1Font("Times-Roman");
    public static final PDType1Font COURIER = new PDType1Font("Courier");
    private String name;
    public PDType1Font(String name) { this.name = name; }
    public String getBaseFont() { return name; }
}
