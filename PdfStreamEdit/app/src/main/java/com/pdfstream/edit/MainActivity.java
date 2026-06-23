package com.pdfstream.edit;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.rendering.PDFRenderer;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.text.TextPosition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQUEST_OPEN_PDF = 1001;
    private static final int REQUEST_PERMISSIONS = 1002;

    private PDDocument document;
    private int currentPageIndex = 0;
    private File currentFile;
    private ImageView pdfPreview;
    private TextView textPreview;
    private TextView pageInfo;
    private TextView objInfo;
    private EditText editFind;
    private EditText editReplace;
    private Button btnPrev, btnNext, btnReplace, btnOpen, btnNew, btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pdfPreview = findViewById(R.id.pdfPreview);
        textPreview = findViewById(R.id.textPreview);
        pageInfo = findViewById(R.id.pageInfo);
        objInfo = findViewById(R.id.objInfo);
        editFind = findViewById(R.id.editFind);
        editReplace = findViewById(R.id.editReplace);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnReplace = findViewById(R.id.btnReplace);
        btnOpen = findViewById(R.id.btnOpen);
        btnNew = findViewById(R.id.btnNew);
        btnSave = findViewById(R.id.btnSave);

        btnOpen.setOnClickListener(v -> openPdf());
        btnNew.setOnClickListener(v -> createNewPdf());
        btnPrev.setOnClickListener(v -> navigatePage(-1));
        btnNext.setOnClickListener(v -> navigatePage(1));
        btnReplace.setOnClickListener(v -> replaceText());
        btnSave.setOnClickListener(v -> savePdf());

        requestPermissions();
        createNewPdf();
    }

    private void requestPermissions() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, REQUEST_PERMISSIONS);
        }
    }

    private void openPdf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        startActivityForResult(intent, REQUEST_OPEN_PDF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN_PDF && resultCode == RESULT_OK && data != null) {
            onPdfSelected(data.getData());
        }
    }

    private void onPdfSelected(Uri uri) {
        if (uri == null) return;
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (document != null) document.close();
            File tempFile = File.createTempFile("pdf_edit_", ".pdf", getCacheDir());
            try (OutputStream os = new FileOutputStream(tempFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
            }
            document = PDDocument.load(tempFile);
            currentFile = tempFile;
            currentPageIndex = 0;
            refreshView();
            Toast.makeText(this, "已加载: " + uri.getLastPathSegment(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "加载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void createNewPdf() {
        try {
            if (document != null) document.close();
            document = new PDDocument();

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.setFont(PDType1Font.HELVETICA_BOLD, 22);
                cs.beginText();
                cs.newLineAtOffset(50, 760);
                cs.showText("PDF Text Editor");
                cs.endText();

                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.beginText();
                cs.newLineAtOffset(50, 720);
                cs.showText("This PDF was created with PdfBox-Android.");
                cs.newLineAtOffset(0, -20);
                cs.showText("You can find and replace text in original position.");
                cs.newLineAtOffset(0, -20);
                cs.showText("Enter the text to find and replace below.");
                cs.newLineAtOffset(0, -20);
                cs.showText("Test text: Hello World!");
                cs.endText();
            }

            currentFile = File.createTempFile("pdf_new_", ".pdf", getCacheDir());
            document.save(currentFile);
            currentPageIndex = 0;
            refreshView();
            Toast.makeText(this, "已创建示例 PDF", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "创建失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void refreshView() {
        if (document == null) return;
        try {
            PDFRenderer renderer = new PDFRenderer(document);
            Bitmap bitmap = renderer.renderImageWithDPI(currentPageIndex, 150);
            pdfPreview.setImageBitmap(bitmap);

            int totalPages = document.getNumberOfPages();
            pageInfo.setText("第 " + (currentPageIndex + 1) + " / " + totalPages + " 页");
            btnPrev.setEnabled(currentPageIndex > 0);
            btnNext.setEnabled(currentPageIndex < totalPages - 1);

            extractPageText();
        } catch (IOException e) {
            Toast.makeText(this, "渲染失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void extractPageText() {
        if (document == null) return;
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(currentPageIndex + 1);
            stripper.setEndPage(currentPageIndex + 1);
            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                textPreview.setText("(本页无可提取的文字)");
                objInfo.setText("本页文字内容 — 无文字");
            } else {
                textPreview.setText(text.trim());
                int charCount = text.trim().length();
                objInfo.setText("本页文字内容 — " + charCount + " 字");
            }
        } catch (IOException e) {
            textPreview.setText("(提取文字失败: " + e.getMessage() + ")");
        }
    }

    /**
     * 原位置覆盖替换文本
     * 1. 用自定义 TextStripper 获取文本位置
     * 2. 找到匹配文本的坐标
     * 3. 用白色矩形覆盖原文本
     * 4. 在同一位置写入新文本
     */
    private void replaceText() {
        if (document == null) {
            Toast.makeText(this, "请先打开或新建 PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        String findText = editFind.getText().toString();
        String replaceWith = editReplace.getText().toString();

        if (findText.isEmpty()) {
            Toast.makeText(this, "请输入要查找的文字", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            PDPage page = document.getPage(currentPageIndex);
            
            // 获取文本位置信息
            PositionTextStripper stripper = new PositionTextStripper();
            stripper.setStartPage(currentPageIndex + 1);
            stripper.setEndPage(currentPageIndex + 1);
            stripper.getText(document);
            List<TextPosition> positions = stripper.getTextPositions();

            // 找到匹配的文本区域
            List<TextRegion> regions = findTextRegions(positions, findText);
            
            if (regions.isEmpty()) {
                Toast.makeText(this, "未找到 \"" + findText + "\"", Toast.LENGTH_SHORT).show();
                return;
            }

            // 在原位置覆盖替换
            PDRectangle mediaBox = page.getMediaBox();
            
            // 使用 append 模式添加内容（覆盖在原有内容之上）
            try (PDPageContentStream cs = new PDPageContentStream(document, page, 
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                
                for (TextRegion region : regions) {
                    // 用白色矩形覆盖原文本
                    cs.setNonStrokingColor(Color.WHITE);
                    cs.addRect(region.x, mediaBox.getHeight() - region.y - region.height,
                               region.width, region.height + 2);
                    cs.fill();
                    
                    // 在原位置写入新文本
                    cs.setNonStrokingColor(Color.BLACK);
                    cs.setFont(PDType1Font.HELVETICA, region.fontSize);
                    cs.beginText();
                    cs.newLineAtOffset(region.x, mediaBox.getHeight() - region.y - region.height + 2);
                    cs.showText(replaceWith);
                    cs.endText();
                }
            }

            document.save(currentFile);
            refreshView();

            Toast.makeText(this, "已替换 " + regions.size() + " 处: \"" + findText + "\" → \"" + replaceWith + "\"", 
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "替换失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 从 TextPosition 列表中找到匹配文本的区域
     */
    private List<TextRegion> findTextRegions(List<TextPosition> positions, String findText) {
        List<TextRegion> regions = new ArrayList<>();
        String allText = "";
        List<Integer> charStartIndices = new ArrayList<>();
        
        // 构建完整文本字符串，记录每个字符在 positions 中的起始索引
        for (int i = 0; i < positions.size(); i++) {
            TextPosition pos = positions.get(i);
            String ch = pos.getUnicode();
            if (ch != null && !ch.isEmpty()) {
                charStartIndices.add(i);
                allText += ch;
            }
        }
        
        // 查找所有匹配位置
        int searchIndex = 0;
        while (searchIndex < allText.length()) {
            int foundIndex = allText.indexOf(findText, searchIndex);
            if (foundIndex < 0) break;
            
            // 计算匹配文本的区域
            int startPosIndex = charStartIndices.get(foundIndex);
            int endPosIndex = charStartIndices.get(Math.min(foundIndex + findText.length() - 1, charStartIndices.size() - 1));
            
            TextPosition startPos = positions.get(startPosIndex);
            TextPosition endPos = positions.get(endPosIndex);
            
            TextRegion region = new TextRegion();
            region.x = startPos.getX();
            region.y = startPos.getY();
            region.width = endPos.getX() + endPos.getWidth() - startPos.getX();
            region.height = startPos.getHeight();
            region.fontSize = startPos.getFontSize();
            regions.add(region);
            
            searchIndex = foundIndex + findText.length();
        }
        
        return regions;
    }

    /**
     * 自定义 TextStripper，收集文本位置信息
     */
    private static class PositionTextStripper extends PDFTextStripper {
        private List<TextPosition> textPositions = new ArrayList<>();

        public PositionTextStripper() throws IOException {
            super();
        }

        @Override
        protected void processTextPosition(TextPosition text) {
            textPositions.add(text);
            super.processTextPosition(text);
        }

        public List<TextPosition> getTextPositions() {
            return textPositions;
        }
    }

    /**
     * 文本区域信息
     */
    private static class TextRegion {
        float x;
        float y;
        float width;
        float height;
        float fontSize;
    }

    private void savePdf() {
        if (document == null) return;
        try {
            File outDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File outFile = new File(outDir, "pdf_text_edited.pdf");
            document.save(outFile);
            Toast.makeText(this, "已保存: " + outFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            try {
                File outFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "pdf_text_edited.pdf");
                document.save(outFile);
                Toast.makeText(this, "已保存: " + outFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } catch (IOException ex) {
                Toast.makeText(this, "保存失败: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void navigatePage(int delta) {
        if (document == null) return;
        int newIndex = currentPageIndex + delta;
        if (newIndex >= 0 && newIndex < document.getNumberOfPages()) {
            currentPageIndex = newIndex;
            refreshView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (document != null) {
            try { document.close(); } catch (IOException ignored) {}
        }
    }
}