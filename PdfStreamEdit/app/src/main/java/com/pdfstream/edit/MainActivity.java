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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDStream;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

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
    private EditText streamEditor;
    private TextView pageInfo;
    private TextView objInfo;
    private Button btnPrev, btnNext, btnApply, btnOpen, btnNew, btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pdfPreview = findViewById(R.id.pdfPreview);
        streamEditor = findViewById(R.id.streamEditor);
        pageInfo = findViewById(R.id.pageInfo);
        objInfo = findViewById(R.id.objInfo);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnApply = findViewById(R.id.btnApply);
        btnOpen = findViewById(R.id.btnOpen);
        btnNew = findViewById(R.id.btnNew);
        btnSave = findViewById(R.id.btnSave);

        btnOpen.setOnClickListener(v -> openPdf());
        btnNew.setOnClickListener(v -> createNewPdf());
        btnPrev.setOnClickListener(v -> navigatePage(-1));
        btnNext.setOnClickListener(v -> navigatePage(1));
        btnApply.setOnClickListener(v -> applyStreamChanges());
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
                cs.showText("PdfBox-Android Content Stream Editor");
                cs.endText();

                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.beginText();
                cs.newLineAtOffset(50, 720);
                cs.showText("This PDF was created with PdfBox-Android 2.0.27");
                cs.newLineAtOffset(0, -20);
                cs.showText("You can edit the content stream directly.");
                cs.newLineAtOffset(0, -20);
                cs.showText("Tap 'Apply' to write changes back to the PDF.");
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
            pageInfo.setText("Page " + (currentPageIndex + 1) + " / " + totalPages);
            btnPrev.setEnabled(currentPageIndex > 0);
            btnNext.setEnabled(currentPageIndex < totalPages - 1);

            loadContentStream();
        } catch (IOException e) {
            Toast.makeText(this, "渲染失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadContentStream() {
        if (document == null) return;
        try {
            PDPage page = document.getPage(currentPageIndex);
            InputStream is = page.getContents();
            if (is == null) {
                streamEditor.setText("% (空内容流)");
                objInfo.setText("Page " + (currentPageIndex + 1) + " - 无内容流");
                return;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) > 0) baos.write(buf, 0, len);
            is.close();

            String streamText = baos.toString("UTF-8");
            streamEditor.setText(streamText);
            objInfo.setText("Page " + (currentPageIndex + 1) + " | Stream: " + baos.size() + " bytes");
        } catch (IOException e) {
            streamEditor.setText("% 读取内容流失败: " + e.getMessage());
        }
    }

    private void applyStreamChanges() {
        if (document == null) return;
        try {
            PDPage page = document.getPage(currentPageIndex);
            String newContent = streamEditor.getText().toString();

            PDStream newStream = new PDStream(document);
            try (OutputStream os = newStream.createOutputStream()) {
                os.write(newContent.getBytes(StandardCharsets.UTF_8));
            }
            page.setContents(newStream);

            document.save(currentFile);
            refreshView();
            Toast.makeText(this, "内容流已修改并保存", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "应用失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void savePdf() {
        if (document == null) return;
        try {
            File outDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File outFile = new File(outDir, "pdf_stream_edited.pdf");
            document.save(outFile);
            Toast.makeText(this, "已保存: " + outFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            try {
                File outFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "pdf_stream_edited.pdf");
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
