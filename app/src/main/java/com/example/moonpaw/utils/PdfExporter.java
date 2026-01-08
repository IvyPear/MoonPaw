package com.example.moonpaw.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import java.util.List;
import java.util.Locale;

public class PdfExporter {

    // Kích thước chuẩn A4 (595 x 842 pixel ở 72 DPI)
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;

    // Màu sắc chủ đạo (Khớp với App)
    private static final int COLOR_PRIMARY = Color.parseColor("#6366f1");
    private static final int COLOR_TEXT = Color.BLACK;
    private static final int COLOR_SUBTEXT = Color.DKGRAY;

    /**
     * Hàm tạo báo cáo PDF
     * @param dailyData: Danh sách giờ ngủ từng ngày trong tháng (để vẽ biểu đồ)
     */
    public static void createReport(Context context, Calendar currentCal, float totalHours, int goodDays, int badDays, String catComment, List<Float> dailyData) {
        PdfDocument document = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        // --- TRANG 1: TỔNG QUAN ---
        PdfDocument.PageInfo pageInfo1 = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page1 = document.startPage(pageInfo1);
        Canvas canvas = page1.getCanvas();

        // 1. Header & Logo
        titlePaint.setColor(COLOR_PRIMARY);
        titlePaint.setTextSize(36);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("MoonPaw Report", 50, 80, titlePaint);

        paint.setColor(COLOR_SUBTEXT);
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
        // Nếu số ngày có dữ liệu > 0 thì chia cho số ngày có dữ liệu, nếu không chia tổng ngày
        int daysWithData = 0;
        for(float h : dailyData) if(h > 0) daysWithData++;
        if(daysWithData > 0) avg = totalHours / daysWithData;

        canvas.drawText(String.format("%.1f", avg), PAGE_WIDTH / 2, 300, paint);

        paint.setTextSize(20);
        canvas.drawText("Giờ / Đêm (Trung bình)", PAGE_WIDTH / 2, 330, paint);

        // 3. Nhận xét tổng quát (Box)
        // Reset Align về Left cho Text Box
        paint.setTextAlign(Paint.Align.LEFT);
        drawTextBox(canvas, "NHẬN XÉT CỦA MÈO MUN", catComment, 480);

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
        int startY = 130;
        drawRow(canvas2, "Tổng giờ ngủ:", String.format("%.1f giờ", totalHours), startY);
        drawRow(canvas2, "Số ngày ngủ tốt (7.5-9h):", goodDays + " ngày", startY + 30);
        drawRow(canvas2, "Số ngày cần cải thiện:", badDays + " ngày", startY + 60);

        // Đánh giá sơ bộ
        String status = "Chưa xác định";
        if (avg >= 7.5 && avg <= 9) status = "Lý tưởng";
        else if (avg < 6.5) status = "Thiếu ngủ";
        else if (avg > 9) status = "Ngủ quá nhiều";
        else status = "Tạm ổn";
        drawRow(canvas2, "Đánh giá chung:", status, startY + 90);

        // --- VẼ BIỂU ĐỒ THỰC TẾ ---
        titlePaint.setTextSize(18);
        canvas2.drawText("Biểu đồ giấc ngủ trong tháng", 50, 350, titlePaint);

        drawChart(canvas2, dailyData, 50, 550, 500, 150);

        document.finishPage(page2);

        // --- TRANG 3: LỜI KHUYÊN & KHOA HỌC ---
        PdfDocument.PageInfo pageInfo3 = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 3).create();
        PdfDocument.Page page3 = document.startPage(pageInfo3);
        Canvas canvas3 = page3.getCanvas();

        titlePaint.setTextSize(24);
        canvas3.drawText("Góc Khoa Học & Lời Khuyên", 50, 80, titlePaint);

        drawTextBox(canvas3, "GIẤC NGỦ & SỨC KHỎE",
                "Giấc ngủ 7.5 - 9 tiếng giúp cơ thể phục hồi năng lượng, củng cố trí nhớ và điều hòa cảm xúc. " +
                        "Thiếu ngủ kéo dài có thể ảnh hưởng đến hệ miễn dịch và khả năng tập trung.", 150);

        drawTextBox(canvas3, "GỢI Ý CẢI THIỆN",
                "• Thiết lập giờ đi ngủ cố định (trước 23:30).\n" +
                        "• Tránh caffeine sau 14:00 chiều.\n" +
                        "• Hạn chế ánh sáng xanh từ điện thoại trước khi ngủ 1 tiếng.\n" +
                        "• Thử bài tập thở '4-7-8' trong mục Thư Giãn của MoonPaw.", 300);

        drawTextBox(canvas3, "BẠN CÓ BIẾT?",
                "Mèo dành tới 70% cuộc đời để ngủ! Tuy nhiên con người chỉ cần 1/3 cuộc đời. " +
                        "Chất lượng giấc ngủ quan trọng hơn số lượng.", 500);

        // Footer
        paint.setColor(Color.GRAY);
        paint.setTextSize(10);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas3.drawText("MoonPaw - Ứng dụng chăm sóc giấc ngủ", PAGE_WIDTH / 2, 800, paint);

        document.finishPage(page3);

        // --- LƯU FILE ---
        savePdf(context, document);
    }

    // Hàm vẽ biểu đồ cột đơn giản từ dữ liệu thật
    private static void drawChart(Canvas canvas, List<Float> data, float x, float y, float width, float height) {
        Paint p = new Paint();
        p.setColor(Color.LTGRAY);
        p.setStrokeWidth(2);

        // Vẽ trục
        canvas.drawLine(x, y, x + width, y, p); // Trục X
        canvas.drawLine(x, y, x, y - height, p); // Trục Y

        if (data == null || data.isEmpty()) return;

        float barWidth = (width - 20) / data.size(); // Độ rộng mỗi cột
        float maxVal = 12f; // Giả sử max là 12 tiếng để scale

        for (int i = 0; i < data.size(); i++) {
            float val = data.get(i);
            if (val > 0) {
                float barHeight = (val / maxVal) * height;
                if (barHeight > height) barHeight = height; // Cap lại nếu quá cao

                // Chọn màu cột dựa trên giờ ngủ (Khoa học)
                if (val >= 7.5 && val <= 9) p.setColor(Color.parseColor("#4ade80")); // Xanh (Good)
                else if (val < 6.5) p.setColor(Color.parseColor("#ef4444")); // Đỏ (Bad)
                else p.setColor(Color.parseColor("#facc15")); // Vàng (OK)

                float left = x + (i * barWidth) + 5;
                float top = y - barHeight;
                float right = left + barWidth - 2;
                float bottom = y - 1; // Trừ 1 để không đè lên trục

                canvas.drawRect(left, top, right, bottom, p);
            }
        }

        // Chú thích trục Y (Mốc 8h)
        p.setColor(Color.GRAY);
        p.setTextSize(10);
        float y8h = y - (8f / maxVal) * height;
        canvas.drawText("8h", x - 20, y8h, p);
        p.setStrokeWidth(1);
        p.setStyle(Paint.Style.STROKE);
        // Vẽ đường kẻ mờ mốc 8h
        canvas.drawLine(x, y8h, x + width, y8h, p);
    }

    private static void drawRow(Canvas canvas, String label, String value, int y) {
        Paint p = new Paint();
        p.setTextSize(14);
        p.setColor(COLOR_SUBTEXT);
        canvas.drawText(label, 50, y, p);

        p.setFakeBoldText(true);
        p.setColor(COLOR_TEXT);
        canvas.drawText(value, 250, y, p);
    }

    private static void drawTextBox(Canvas canvas, String title, String content, int y) {
        Paint p = new Paint();

        // Tiêu đề box
        p.setColor(COLOR_PRIMARY);
        p.setTextSize(16);
        p.setFakeBoldText(true);
        canvas.drawText(title, 50, y, p);

        // Nội dung (Vẽ nhiều dòng tự động xuống dòng)
        p.setColor(COLOR_TEXT);
        p.setTextSize(12);
        p.setFakeBoldText(false);

        int currentY = y + 25;
        int charsPerLine = 75; // Số ký tự ước lượng trên 1 dòng

        // Tách chuỗi thành các từ
        String[] words = content.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (line.length() + word.length() < charsPerLine) {
                line.append(word).append(" ");
            } else {
                canvas.drawText(line.toString(), 50, currentY, p);
                currentY += 18; // Khoảng cách dòng
                line = new StringBuilder(word).append(" ");
            }
            // Xử lý xuống dòng cứng (\n) trong đoạn văn
            if (word.contains("\n")) {
                // Logic này hơi phức tạp để xử lý \n, phiên bản đơn giản này chỉ wrap text theo độ dài
            }
        }
        // Vẽ dòng cuối
        canvas.drawText(line.toString(), 50, currentY, p);
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