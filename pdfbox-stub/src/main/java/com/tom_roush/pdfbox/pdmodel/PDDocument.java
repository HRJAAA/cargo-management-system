package com.tom_roush.pdfbox.pdmodel;

import com.tom_roush.pdfbox.pdmodel.common.PDStream;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PDDocument {
    private List<PDPage> pages = new ArrayList<>();
    private File file;

    public PDDocument() {}

    public static PDDocument load(File file) throws IOException {
        PDDocument doc = new PDDocument();
        doc.file = file;
        // 简化：创建一个默认页面
        doc.pages.add(new PDPage());
        return doc;
    }

    public int getNumberOfPages() { return pages.size(); }
    public PDPage getPage(int index) { return pages.get(index); }
    public void addPage(PDPage page) { pages.add(page); }

    public void save(File file) throws IOException {
        this.file = file;
        // 写入最小有效 PDF
        writeMinimalPdf(file, pages.size());
    }

    public void close() throws IOException {}

    private void writeMinimalPdf(File f, int pageCount) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("%PDF-1.4\n");
        sb.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        sb.append("2 0 obj\n<< /Type /Pages /Kids [");
        for (int i = 0; i < pageCount; i++) sb.append((i*3+3) + " 0 R ");
        sb.append("] /Count ").append(pageCount).append(" >>\nendobj\n");
        int objNum = 3;
        for (int i = 0; i < pageCount; i++) {
            sb.append(objNum + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents " + (objNum+1) + " 0 R /Resources << /Font << /F1 " + (objNum+2) + " 0 R >> >> >>\nendobj\n");
            objNum++;
            sb.append(objNum + " 0 obj\n<< /Length 44 >>\nstream\nBT /F1 12 Tf 50 780 Td (Hello) Tj ET\nendstream\nendobj\n");
            objNum++;
            sb.append(objNum + " 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");
            objNum++;
        }
        sb.append("xref\n0 " + objNum + "\n");
        sb.append("0000000000 65535 f \n");
        for (int i = 1; i < objNum; i++) sb.append(String.format("%010d 00000 n \n", 0));
        sb.append("trailer\n<< /Size " + objNum + " /Root 1 0 R >>\nstartxref\n0\n%%EOF\n");
        java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
        fos.write(sb.toString().getBytes("UTF-8"));
        fos.close();
    }
}
