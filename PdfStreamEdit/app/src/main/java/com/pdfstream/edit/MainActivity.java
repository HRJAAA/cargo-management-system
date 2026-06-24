package com.pdfstream.edit;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
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
    private Bitmap pdfBitmap;
    private float renderDPI = 150;
    private List<TextPosition> textPositions = new ArrayList<>();
    private PDRectangle mediaBox;

    private ImageView pdfPreview;
    private TextView textPreview;
    private TextView pageInfo;
    private TextView statusInfo;
    private Button btnPrev, btnNext, btnOpen, btnNew, btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pdfPreview = findViewById(R.id.pdfPreview);
        textPreview = findViewById(R.id.textPreview);
        pageInfo = findViewById(R.id.pageInfo);
        statusInfo = findViewById(R.id.objInfo);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnOpen = findViewById(R.id.btnOpen);
        btnNew = findViewById(R.id.btnNew);
        btnSave = findViewById(R.id.btnSave);

        btnOpen.setOnClickListener(v -> openPdf());
        btnNew.setOnClickListener(v -> createNewPdf());
        btnPrev.setOnClickListener(v -> navigatePage(-1));
        btnNext.setOnClickListener(v -> navigatePage(1));
        btnSave.setOnClickListener(v -> savePdf());

        // 点击PDF预览图片，编辑文字
        pdfPreview.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float touchX = event.getX();
                float touchY = event.getY();
                onPdfTouched(touchX, touchY);
            }
            return false;
        });

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

                cs.setFont(PDType1Font.HELVETICA, 14);
                cs.beginText();
                cs.newLineAtOffset(50, 720);
                cs.showText("Click on any text to edit it directly.");
                cs.newLineAtOffset(0, -25);
                cs.showText("Hello World!");
                cs.newLineAtOffset(0, -25);
                cs.showText("This is a sample PDF document.");
                cs.newLineAtOffset(0, -25);
                cs.showText("Edit me by clicking!");
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
            PDPage page = document.getPage(currentPageIndex);
            mediaBox = page.getMediaBox();

            PDFRenderer renderer = new PDFRenderer(document);
            pdfBitmap = renderer.renderImageWithDPI(currentPageIndex, renderDPI);
            pdfPreview.setImageBitmap(pdfBitmap);

            int totalPages = document.getNumberOfPages();
            pageInfo.setText("第 " + (currentPageIndex + 1) + " / " + totalPages + " 页");
            btnPrev.setEnabled(currentPageIndex > 0);
            btnNext.setEnabled(currentPageIndex < totalPages - 1);

            // 提取文字和位置信息
            extractTextWithPositions();
        } catch (IOException e) {
            Toast.makeText(this, "渲染失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void extractTextWithPositions() {
        if (document == null) return;
        try {
            PositionTextStripper stripper = new PositionTextStripper();
            stripper.setStartPage(currentPageIndex + 1);
            stripper.setEndPage(currentPageIndex + 1);
            stripper.getText(document);
            textPositions = stripper.getTextPositions();

            // 显示文字预览
            StringBuilder sb = new StringBuilder();
            for (TextPosition pos : textPositions) {
                String ch = pos.getUnicode();
                if (ch != null) sb.append(ch);
            }
            textPreview.setText(sb.toString().trim());
            statusInfo.setText("点击PDF上的文字可直接编辑");
        } catch (IOException e) {
            textPreview.setText("(提取文字失败)");
        }
    }

    /**
     * 处理PDF预览的点击事件
     * 将屏幕坐标转换为PDF坐标，找到附近的文字
     */
    private void onPdfTouched(float touchX, float touchY) {
        if (textPositions.isEmpty() || pdfBitmap == null || mediaBox == null) {
            Toast.makeText(this, "请先打开PDF文件", Toast.LENGTH_SHORT).show();
            return;
        }

        // 计算ImageView中的实际显示区域
        int imgWidth = pdfBitmap.getWidth();
        int imgHeight = pdfBitmap.getHeight();
        int viewWidth = pdfPreview.getWidth();
        int viewHeight = pdfPreview.getHeight();

        // 计算缩放比例和偏移（fitCenter模式）
        float scale = Math.min((float) viewWidth / imgWidth, (float) viewHeight / imgHeight);
        float scaledWidth = imgWidth * scale;
        float scaledHeight = imgHeight * scale;
        float offsetX = (viewWidth - scaledWidth) / 2;
        float offsetY = (viewHeight - scaledHeight) / 2;

        // 将触摸坐标转换为图片坐标
        float imgX = (touchX - offsetX) / scale;
        float imgY = (touchY - offsetY) / scale;

        // 将图片坐标转换为PDF坐标
        // 图片Y轴从上往下，PDF Y轴从下往上
        float pdfX = imgX * 72 / renderDPI;  // DPI转PDF单位(72dpi)
        float pdfY = mediaBox.getHeight() - (imgY * 72 / renderDPI);

        // 找到点击位置附近的文字
        TextPosition clickedText = findNearestText(pdfX, pdfY);
        if (clickedText == null) {
            Toast.makeText(this, "该位置没有文字", Toast.LENGTH_SHORT).show();
            return;
        }

        // 弹出编辑对话框
        showEditDialog(clickedText);
    }

    /**
     * 找到距离点击位置最近的文字
     */
    private TextPosition findNearestText(float pdfX, float pdfY) {
        TextPosition nearest = null;
        float minDistance = Float.MAX_VALUE;

        for (TextPosition pos : textPositions) {
            float textX = pos.getX();
            float textY = pos.getY();
            float textWidth = pos.getWidth();
            float textHeight = pos.getHeight();

            // 文字的中心点
            float centerX = textX + textWidth / 2;
            float centerY = textY - textHeight / 2;  // PDF的Y是baseline，往上才是文字区域

            // 计算距离
            float distance = Math.sqrt((pdfX - centerX) * (pdfX - centerX) + (pdfY - centerY) * (pdfY - centerY));

            // 点击是否在文字区域内（放宽范围便于点击）
            float hitRadius = Math.max(textWidth, textHeight) * 1.5f;
            if (distance < hitRadius && distance < minDistance) {
                minDistance = distance;
                nearest = pos;
            }
        }

        return nearest;
    }

    /**
     * 显示编辑对话框
     */
    private void showEditDialog(TextPosition textPos) {
        String originalText = textPos.getUnicode();
        if (originalText == null) originalText = "";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("编辑文字");

        final EditText input = new EditText(this);
        input.setText(originalText);
        input.selectAll();
        builder.setView(input);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String newText = input.getText().toString();
            if (!newText.equals(originalText)) {
                replaceTextAtPosition(textPos, newText);
            }
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    /**
     * 在指定位置替换文字
     */
    private void replaceTextAtPosition(TextPosition textPos, String newText) {
        try {
            PDPage page = document.getPage(currentPageIndex);

            // 用白色矩形覆盖原文字
            try (PDPageContentStream cs = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {

                // 覆盖原文字区域
                cs.setNonStrokingColor(Color.WHITE);
                cs.addRect(textPos.getX() - 1,
                           mediaBox.getHeight() - textPos.getY() - textPos.getHeight() - 1,
                           textPos.getWidth() + 2,
                           textPos.getHeight() + 2);
                cs.fill();

                // 写入新文字
                cs.setNonStrokingColor(Color.BLACK);
                cs.setFont(PDType1Font.HELVETICA, textPos.getFontSize());
                cs.beginText();
                cs.newLineAtOffset(textPos.getX(), mediaBox.getHeight() - textPos.getY() - textPos.getHeight());
                cs.showText(newText);
                cs.endText();
            }

            document.save(currentFile);
            refreshView();
            Toast.makeText(this, "已修改: \"" + textPos.getUnicode() + "\" → \"" + newText + "\"", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "修改失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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

    /**
     * 自定义TextStripper，收集文本位置信息
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
}