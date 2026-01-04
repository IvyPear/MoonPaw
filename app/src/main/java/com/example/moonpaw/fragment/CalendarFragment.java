package com.example.moonpaw.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.moonpaw.R;
import com.example.moonpaw.utils.PdfExporter; // Class xuất PDF
import com.example.moonpaw.utils.SleepAnalyzer;

import java.util.Calendar;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private GridLayout gridCalendar;
    private TextView tvMonth, tvTotalHours, tvAvgSleep, tvGoodCount, tvOkCount, tvBadCount, tvBestStreak, tvCatComment;
    private ImageButton btnExportPdf; // Nút xuất PDF
    private Calendar calendar;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireContext().getSharedPreferences("SleepPrefs", Context.MODE_PRIVATE);

        // Lấy thời gian thực
        calendar = Calendar.getInstance();

        initViews(view);
        updateUI();

        // 1. Nút Back
        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            }
        });

        // 2. Chuyển tháng
        view.findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            updateUI();
        });
        view.findViewById(R.id.btn_next_month).setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1);
            updateUI();
        });

        // 3. Nút Xuất PDF (Logic mới)
        if (btnExportPdf != null) {
            btnExportPdf.setOnClickListener(v -> exportReportToPdf());
        }
    }

    private void initViews(View v) {
        gridCalendar = v.findViewById(R.id.grid_calendar);
        tvMonth = v.findViewById(R.id.tv_month);
        tvTotalHours = v.findViewById(R.id.tv_total_hours);
        tvAvgSleep = v.findViewById(R.id.tv_avg_sleep);
        tvGoodCount = v.findViewById(R.id.tv_good_count);
        tvOkCount = v.findViewById(R.id.tv_ok_count);
        tvBadCount = v.findViewById(R.id.tv_bad_count);
        tvBestStreak = v.findViewById(R.id.tv_best_streak);
        tvCatComment = v.findViewById(R.id.tv_cat_comment);

        // Ánh xạ nút Export
        btnExportPdf = v.findViewById(R.id.btn_export_pdf);
    }

    // Hàm xử lý logic Xuất PDF
    private void exportReportToPdf() {
        Toast.makeText(getContext(), "Đang tạo báo cáo...", Toast.LENGTH_SHORT).show();

        // Thu thập dữ liệu hiện tại đang hiển thị
        float total = 0;
        int good = 0, bad = 0;
        int days = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int d = 1; d <= days; d++) {
            Calendar temp = (Calendar) calendar.clone();
            temp.set(Calendar.DAY_OF_MONTH, d);
            String key = SleepAnalyzer.getDateKey(temp.getTimeInMillis());
            float h = prefs.getFloat(key, 0f);
            if (h > 0) {
                total += h;
                if (h >= 7) good++; else bad++;
            }
        }

        String currentComment = tvCatComment.getText().toString();

        // Gọi Helper để tạo file
        try {
            PdfExporter.createReport(
                    requireContext(),
                    calendar,
                    total,
                    good,
                    bad,
                    currentComment
            );
        } catch (Exception e) {
            Toast.makeText(getContext(), "Lỗi xuất PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void updateUI() {
        tvMonth.setText("Tháng " + (calendar.get(Calendar.MONTH) + 1) + "/" + calendar.get(Calendar.YEAR));
        populateCalendar();
        calculateStats();
    }

    private void populateCalendar() {
        gridCalendar.removeAllViews();
        Calendar cal = (Calendar) calendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int offset = (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) ? 6 : cal.get(Calendar.DAY_OF_WEEK) - 2;
        int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < offset; i++) addEmptyCell();

        Calendar today = Calendar.getInstance();

        for (int day = 1; day <= days; day++) {
            Calendar dayCal = (Calendar) calendar.clone();
            dayCal.set(Calendar.DAY_OF_MONTH, day);

            String key = SleepAnalyzer.getDateKey(dayCal.getTimeInMillis());
            float hours = prefs.getFloat(key, 0f);

            boolean isToday = (day == today.get(Calendar.DAY_OF_MONTH) &&
                    calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR));

            addDayCell(hours, isToday);
        }
    }

    private void calculateStats() {
        float total = 0;
        int count = 0, good = 0, ok = 0, bad = 0;
        int days = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        int maxStreak = 0;
        int currentStreak = 0;

        for (int d = 1; d <= days; d++) {
            Calendar dayCal = (Calendar) calendar.clone();
            dayCal.set(Calendar.DAY_OF_MONTH, d);
            String key = SleepAnalyzer.getDateKey(dayCal.getTimeInMillis());

            float h = prefs.getFloat(key, 0f);

            if (h > 0) {
                total += h;
                count++;
                if (h >= SleepAnalyzer.THRESHOLD_OK) good++;
                else if (h >= SleepAnalyzer.THRESHOLD_BAD) ok++;
                else bad++;

                currentStreak++;
                if (currentStreak > maxStreak) {
                    maxStreak = currentStreak;
                }
            } else {
                currentStreak = 0;
            }
        }

        tvTotalHours.setText(String.format(Locale.getDefault(), "%.1f", total));

        float avg = 0;
        if (count > 0) {
            avg = total / count;
            tvAvgSleep.setText(String.format(Locale.getDefault(), "%dh %dp", (int)avg, (int)((avg-(int)avg)*60)));
        } else {
            tvAvgSleep.setText("0h 0p");
        }

        tvGoodCount.setText(String.valueOf(good));
        tvOkCount.setText(String.valueOf(ok));
        tvBadCount.setText(String.valueOf(bad));
        tvBestStreak.setText(String.valueOf(maxStreak));

        // Logic Mèo Nhận Xét
        String comment;
        if (count < 5) comment = "Chưa đủ dữ liệu để đánh giá giấc ngủ tháng này.\n\nHãy ghi nhận thêm vài ngày để có kết quả chính xác hơn.";
        else if (avg < 6.5f) comment = "Thời lượng ngủ trung bình thấp hơn mức khuyến nghị.\n\nNgủ thiếu kéo dài có thể gây mệt mỏi.";
        else if (avg > 9.5f) comment = "Thời lượng ngủ trung bình cao hơn mức khuyến nghị.\n\nNgủ quá nhiều cũng có thể gây uể oải.";
        else if (bad > good) comment = "Giấc ngủ trong tháng này chưa ổn định.\n\nViệc ngủ trễ xảy ra khá thường xuyên.";
        else if (avg >= 7.0f && avg <= 9.0f) comment = "Giấc ngủ tháng này phù hợp với khuyến nghị sức khỏe.\n\nBạn đang duy trì thói quen tốt.";
        else comment = "Thời lượng ngủ đạt yêu cầu, nhưng chưa thực sự ổn định.";

        tvCatComment.setText(comment);
    }

    private void addDayCell(float hours, boolean isToday) {
        View cell = LayoutInflater.from(getContext()).inflate(R.layout.item_calendar_day_v2, gridCalendar, false);
        ImageView icon = cell.findViewById(R.id.img_sleep_icon);
        View highlight = cell.findViewById(R.id.view_today_highlight);
        View tooltip = cell.findViewById(R.id.tooltip_container);
        TextView tvTip = cell.findViewById(R.id.tv_tooltip_text);

        if (hours > 0) {
            icon.setColorFilter(SleepAnalyzer.getQualityColor(hours));
            icon.setAlpha(1.0f);
        } else {
            icon.setAlpha(0.1f);
        }

        if (isToday) {
            highlight.setVisibility(View.VISIBLE);
            if (hours > 0) {
                tooltip.setVisibility(View.VISIBLE);
                tvTip.setText(String.format("%.1fh", hours));
            }
        }

        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0; p.height = 70 * (int)getResources().getDisplayMetrics().density;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        gridCalendar.addView(cell, p);
    }

    private void addEmptyCell() {
        View v = new View(getContext());
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0; p.height = 70 * (int)getResources().getDisplayMetrics().density;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        gridCalendar.addView(v, p);
    }
}