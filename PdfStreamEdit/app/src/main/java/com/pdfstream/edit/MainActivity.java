package com.pdfstream.edit;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import com.tom_roush.pdfbox.pdmodel.common.PDStream;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.rendering.PDFRenderer;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

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
                cs.showText("You can find and replace text directly.");
                cs.newLineAtOffset(0, -20);
                cs.showText("Enter the text to find and replace below.");
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
            InputStream is = page.getContents();
            if (is == null) {
                Toast.makeText(this, "本页无内容", Toast.LENGTH_SHORT).show();
                return;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) > 0) baos.write(buf, 0, len);
            is.close();

            String streamContent = baos.toString("UTF-8");

            // 在内容流中查找替换：处理 PDF 文本操作符中的文字
            // 匹配 (text) Tj 和 [(text1)(text2)] TJ 两种格式
            String newStreamContent = replaceTextInStream(streamContent, findText, replaceWith);

            if (newStreamContent.equals(streamContent)) {
                Toast.makeText(this, "未找到 \"" + findText + "\"", Toast.LENGTH_SHORT).show();
                return;
            }

            // 写回修改后的内容流
            PDStream newStream = new PDStream(document);
            try (OutputStream os = newStream.createOutputStream()) {
                os.write(newStreamContent.getBytes(StandardCharsets.UTF_8));
            }
            page.setContents(newStream);

            document.save(currentFile);
            refreshView();

            Toast.makeText(this, "已替换: \"" + findText + "\" → \"" + replaceWith + "\"", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "替换失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 在 PDF 内容流中查找并替换文字。
     * PDF 内容流中的文字以 (text) Tj 或 [(text1)(text2)] TJ 格式出现。
     * 我们在括号内的文本中查找并替换。
     */
    private String replaceTextInStream(String stream, String find, String replace) {
        String result = stream;
        // 处理 (text) Tj 格式 — 括号内的文本
        // 需要处理转义括号 \(
        StringBuilder sb = new StringBuilder();
        int i = 0;
        boolean replaced = false;
        while (i < result.length()) {
            if (result.charAt(i) == '(' && isTextOperatorContext(result, i)) {
                // 找到文本字符串的起始
                int start = i;
                i++; // 跳过起始括号
                StringBuilder textInParens = new StringBuilder();
                int depth = 1;
                while (i < result.length() && depth > 0) {
                    char c = result.charAt(i);
                    if (c == '\\' && i + 1 < result.length()) {
                        textInParens.append(c);
                        textInParens.append(result.charAt(i + 1));
                        i += 2;
                        continue;
                    }
                    if (c == '(') depth++;
                    else if (c == ')') {
                        depth--;
                        if (depth == 0) break;
                    }
                    textInParens.append(c);
                    i++;
                }
                // i 现在指向结束的 )
                String originalText = textInParens.toString();
                if (originalText.contains(find)) {
                    String newText = originalText.replace(find, escapePdfString(replace));
                    sb.append('(').append(newText).append(')');
                    replaced = true;
                } else {
                    sb.append('(').append(originalText).append(')');
                }
                i++; // 跳过结束的 )
            } else {
                sb.append(result.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * 判断当前括号是否处于文本操作符上下文中
     * 简单启发式：检查括号后面是否跟着 Tj, TJ, Tc, Tw 等文本操作符
     */
    private boolean isTextOperatorContext(String s, int openParenIndex) {
        // 向后找到匹配的闭括号
        int depth = 1;
        int j = openParenIndex + 1;
        while (j < s.length() && depth > 0) {
            char c = s.charAt(j);
            if (c == '\\' && j + 1 < s.length()) {
                j += 2;
                continue;
            }
            if (c == '(') depth++;
            else if (c == ')') depth--;
            j++;
        }
        // j 现在指向闭括号之后
        // 跳过空白
        while (j < s.length() && (s.charAt(j) == ' ' || s.charAt(j) == '\n' || s.charAt(j) == '\r' || s.charAt(j) == '\t')) {
            j++;
        }
        // 检查接下来的操作符
        if (j < s.length()) {
            // 检查是否是 [ 开头（TJ 数组的一部分）
            if (s.charAt(j) == ')') return true; // 嵌套括号
            // 读取操作符
            int opStart = j;
            while (j < s.length() && !Character.isWhitespace(s.charAt(j)) && s.charAt(j) != '(' && s.charAt(j) != '[') {
                j++;
            }
            String op = s.substring(opStart, j);
            if (op.equals("Tj") || op.equals("TJ") || op.equals("Tc") || op.equals("Tw") ||
                op.equals("Tf") || op.equals("TL") || op.equals("Tr") || op.equals("Ts")) {
                return true;
            }
        }
        // 也可能是 TJ 数组内的元素，保守返回 true
        // 检查前面是否有 [ 符号（TJ 数组）
        int k = openParenIndex - 1;
        while (k >= 0 && (s.charAt(k) == ' ' || s.charAt(k) == '\n' || s.charAt(k) == '\r' || s.charAt(k) == '\t')) {
            k--;
        }
        if (k >= 0 && s.charAt(k) == '[') return true;
        // 默认对括号内容做替换（宁可多替换也不漏）
        return true;
    }

    /**
     * 转义 PDF 字符串中的特殊字符
     */
    private String escapePdfString(String text) {
        return text.replace("\\", "\\\\")
                   .replace("(", "\\(")
                   .replace(")", "\\)");
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
