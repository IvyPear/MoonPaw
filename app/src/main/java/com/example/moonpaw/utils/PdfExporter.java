package com.example.moonpaw.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PdfExporter {

    // Kích thước chuẩn A4 (595 x 842 pixel ở 72 DPI)
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;

    public static void createReport(Context context, Calendar currentCal, float totalHours, int goodDays, int badDays, String catComment) {
        PdfDocument document = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        // --- TRANG 1: TỔNG QUAN ---
        PdfDocument.PageInfo pageInfo1 = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page1 = document.startPage(pageInfo1);
        Canvas canvas = page1.getCanvas();

        // 1. Header & Logo
        titlePaint.setColor(Color.parseColor("#6366f1")); // Màu tím chủ đạo
        titlePaint.setTextSize(36);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("MoonPaw Report", 50, 80, titlePaint);

        paint.setColor(Color.GRAY);
        paint.setTextSize(14);
        String dateStr = "Báo cáo tháng: " + new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(currentCal.getTime());
        canvas.drawText(dateStr, 50, 110, paint);

        // 2. Trạng thái giấc ngủ (Vẽ vòng tròn điểm số)
        paint.setColor(Color.parseColor("#e0e7ff")); // Nền tròn nhạt
        canvas.drawCircle(PAGE_WIDTH / 2, 300, 100, paint);

        paint.setColor(Color.parseColor("#194cb3")); // Chữ đậm
        paint.setTextSize(60);
        paint.setTextAlign(Paint.Align.CENTER);

        float avg = (goodDays + badDays) > 0 ? totalHours / (goodDays + badDays) : 0;
        canvas.drawText(String.format("%.1f", avg), PAGE_WIDTH / 2, 300, paint);

        paint.setTextSize(20);
        canvas.drawText("Giờ / Đêm", PAGE_WIDTH / 2, 330, paint);

        // 3. Nhận xét tổng quát (Box)
        drawTextBox(canvas, "NHẬN XÉT CỦA MÈO MUN", catComment, 450);

        document.finishPage(page1);

        // --- TRANG 2: THỐNG KÊ CHI TIẾT & BIỂU ĐỒ ---
        PdfDocument.PageInfo pageInfo2 = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create();
        PdfDocument.Page page2 = document.startPage(pageInfo2);
        Canvas canvas2 = page2.getCanvas();
        paint.setTextAlign(Paint.Align.LEFT);

        // Header trang 2
        titlePaint.setTextSize(24);
        canvas2.drawText("Thống kê chi tiết", 50, 80, titlePaint);

        // Bảng số liệu
        int startY = 150;
        drawRow(canvas2, "Tổng giờ ngủ:", String.format("%.1f giờ", totalHours), startY);
        drawRow(canvas2, "Ngày ngủ tốt:", goodDays + " ngày", startY + 40);
        drawRow(canvas2, "Ngày cần cải thiện:", badDays + " ngày", startY + 80);

        // Vẽ Biểu đồ cột đơn giản (Mô phỏng)
        titlePaint.setTextSize(18);
        canvas2.drawText("Biểu đồ giấc ngủ (Mô phỏng)", 50, 350, titlePaint);

        // Trục tọa độ
        Paint chartPaint = new Paint();
        chartPaint.setColor(Color.LTGRAY);
        chartPaint.setStrokeWidth(2);
        canvas2.drawLine(50, 550, 550, 550, chartPaint); // Trục X
        canvas2.drawLine(50, 550, 50, 400, chartPaint); // Trục Y

        // Vẽ vài cột mẫu (Cột tím)
        chartPaint.setColor(Color.parseColor("#818cf8"));
        chartPaint.setStyle(Paint.Style.FILL);
        // Cột 1
        canvas2.drawRect(80, 450, 120, 550, chartPaint);
        // Cột 2 (cao hơn)
        canvas2.drawRect(150, 420, 190, 550, chartPaint);
        // Cột 3 (thấp)
        canvas2.drawRect(220, 480, 260, 550, chartPaint);

        paint.setColor(Color.BLACK);
        paint.setTextSize(12);
        canvas2.drawText("Tuần 1", 80, 570, paint);
        canvas2.drawText("Tuần 2", 150, 570, paint);
        canvas2.drawText("Tuần 3", 220, 570, paint);

        document.finishPage(page2);

        // --- TRANG 3: LỜI KHUYÊN & KHOA HỌC ---
        PdfDocument.PageInfo pageInfo3 = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 3).create();
        PdfDocument.Page page3 = document.startPage(pageInfo3);
        Canvas canvas3 = page3.getCanvas();

        titlePaint.setTextSize(24);
        canvas3.drawText("Góc Khoa Học & Lời Khuyên", 50, 80, titlePaint);

        drawTextBox(canvas3, "XU HƯỚNG", "So với tháng trước, bạn đang ngủ nhiều hơn 12%. Hãy duy trì đà này để cơ thể phục hồi tốt nhất.", 150);

        drawTextBox(canvas3, "GỢI Ý CẢI THIỆN", "• Hạn chế ánh sáng xanh trước khi ngủ 1 tiếng.\n• Thử bài tập thở '4-7-8' trong mục Thư Giãn.\n• Giữ nhiệt độ phòng mát mẻ (khoảng 20-25 độ C).", 350);

        drawTextBox(canvas3, "BẠN CÓ BIẾT?", "Mèo dành 70% cuộc đời để ngủ. Con người cần ít hơn, nhưng chất lượng quan trọng hơn số lượng!", 550);

        // Footer
        paint.setColor(Color.GRAY);
        paint.setTextSize(10);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas3.drawText("MoonPaw - Ứng dụng chăm sóc giấc ngủ", PAGE_WIDTH / 2, 800, paint);

        document.finishPage(page3);

        // --- LƯU FILE ---
        savePdf(context, document);
    }

    private static void drawRow(Canvas canvas, String label, String value, int y) {
        Paint p = new Paint();
        p.setTextSize(16);
        p.setColor(Color.DKGRAY);
        canvas.drawText(label, 50, y, p);

        p.setFakeBoldText(true);
        p.setColor(Color.BLACK);
        canvas.drawText(value, 250, y, p);
    }

    private static void drawTextBox(Canvas canvas, String title, String content, int y) {
        Paint p = new Paint();

        // Tiêu đề box
        p.setColor(Color.parseColor("#6366f1"));
        p.setTextSize(16);
        p.setFakeBoldText(true);
        canvas.drawText(title, 50, y, p);

        // Nội dung (Vẽ nhiều dòng)
        p.setColor(Color.BLACK);
        p.setTextSize(14);
        p.setFakeBoldText(false);

        int currentY = y + 30;
        String[] lines = content.split("\n");
        for (String line : lines) {
            // Cắt dòng đơn giản (Nếu dài quá sẽ bị tràn)
            if (line.length() > 65) {
                String line1 = line.substring(0, 65);
                String line2 = line.substring(65);
                canvas.drawText(line1 + "-", 50, currentY, p);
                currentY += 20;
                canvas.drawText(line2, 50, currentY, p);
            } else {
                canvas.drawText(line, 50, currentY, p);
            }
            currentY += 25;
        }
    }

    private static void savePdf(Context context, PdfDocument document) {
        String fileName = "MoonPaw_Report_" + System.currentTimeMillis() + ".pdf";

        try {
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                fos = context.getContentResolver().openOutputStream(uri);
            } else {
                Toast.makeText(context, "Chức năng hỗ trợ Android 10 trở lên", Toast.LENGTH_SHORT).show();
                return;
            }

            document.writeTo(fos);
            document.close();
            if (fos != null) fos.close();

            Toast.makeText(context, "✅ Đã xuất PDF: " + fileName, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Lỗi khi lưu PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}