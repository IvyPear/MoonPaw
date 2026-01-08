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
import com.example.moonpaw.utils.PdfExporter;
import com.example.moonpaw.utils.SleepAnalyzer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private GridLayout gridCalendar;
    private TextView tvMonth, tvTotalHours, tvAvgSleep, tvGoodCount, tvOkCount, tvBadCount, tvBestStreak, tvCatComment;
    private ImageButton btnExportPdf;
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

        // 3. Nút Xuất PDF
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
        btnExportPdf = v.findViewById(R.id.btn_export_pdf);
    }

    // --- LOGIC XUẤT PDF (ĐÃ SỬA LỖI THIẾU THAM SỐ) ---
    private void exportReportToPdf() {
        Toast.makeText(getContext(), "Đang tạo báo cáo...", Toast.LENGTH_SHORT).show();

        float total = 0;
        int good = 0, bad = 0;
        int days = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Tạo list chứa dữ liệu từng ngày để vẽ biểu đồ
        List<Float> dailyData = new ArrayList<>();

        for (int d = 1; d <= days; d++) {
            Calendar temp = (Calendar) calendar.clone();
            temp.set(Calendar.DAY_OF_MONTH, d);
            String key = SleepAnalyzer.getDateKey(temp.getTimeInMillis());
            float h = prefs.getFloat(key, 0f);

            dailyData.add(h); // Thêm vào list cho biểu đồ

            if (h > 0) {
                total += h;
                // Đánh giá đơn giản cho PDF (Dùng hàm overload mặc định)
                if (SleepAnalyzer.evaluateSleepState(h, false) == SleepAnalyzer.SleepState.GOOD) good++;
                else bad++;
            }
        }

        try {
            // Truyền đủ 7 tham số bao gồm dailyData
            PdfExporter.createReport(
                    requireContext(),
                    calendar,
                    total,
                    good,
                    bad,
                    tvCatComment.getText().toString(),
                    dailyData
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

    // --- LOGIC TÍNH TOÁN & NHẬN XÉT CỦA MÈO ---
    private void calculateStats() {
        float total = 0;
        int count = 0, good = 0, ok = 0, bad = 0;

        // Biến đếm vấn đề cụ thể
        int lateCount = 0;
        int shortCount = 0;
        int overCount = 0;

        int maxStreak = 0;
        int currentStreak = 0;
        int days = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int d = 1; d <= days; d++) {
            Calendar dayCal = (Calendar) calendar.clone();
            dayCal.set(Calendar.DAY_OF_MONTH, d);
            String key = SleepAnalyzer.getDateKey(dayCal.getTimeInMillis());
            float h = prefs.getFloat(key, 0f);

            // Lấy giờ đi ngủ (Nếu không có dữ liệu lịch sử thì lấy mặc định hiện tại)
            String bedTime = prefs.getString("bedtime", "23:00");

            if (h > 0) {
                total += h;
                count++;

                // 1. Phân loại Good/OK/Bad
                SleepAnalyzer.SleepState state = SleepAnalyzer.evaluateSleepState(h, false, bedTime);
                if (state == SleepAnalyzer.SleepState.GOOD) good++;
                else if (state == SleepAnalyzer.SleepState.OK) ok++;
                else bad++;

                // 2. Phân tích nguyên nhân sâu xa
                SleepAnalyzer.SleepIssue issue = SleepAnalyzer.analyzeSleepIssue(h, bedTime);
                if (issue == SleepAnalyzer.SleepIssue.LATE_SLEEP || issue == SleepAnalyzer.SleepIssue.SHORT_AND_LATE) lateCount++;
                if (issue == SleepAnalyzer.SleepIssue.SHORT_SLEEP || issue == SleepAnalyzer.SleepIssue.SHORT_AND_LATE) shortCount++;
                if (issue == SleepAnalyzer.SleepIssue.OVER_SLEEP) overCount++;

                // 3. Tính Streak
                currentStreak++;
                if (currentStreak > maxStreak) maxStreak = currentStreak;
            } else {
                currentStreak = 0;
            }
        }

        // Hiển thị số liệu
        tvTotalHours.setText(String.format(Locale.getDefault(), "%.1f", total));
        float avg = (count > 0) ? total / count : 0;
        tvAvgSleep.setText(String.format(Locale.getDefault(), "%dh %dp", (int)avg, (int)((avg-(int)avg)*60)));

        tvGoodCount.setText(String.valueOf(good));
        tvOkCount.setText(String.valueOf(ok));
        tvBadCount.setText(String.valueOf(bad));
        tvBestStreak.setText(String.valueOf(maxStreak));

        // --- MÈO NHẬN XÉT (Logic thông minh) ---
        String comment;

        if (count == 0) {
            comment = "Chưa có dữ liệu giấc ngủ trong tháng này.\n\nHãy bắt đầu ghi lại giấc ngủ để Mèo Mun theo dõi nhé!";
        } else if (count < 5) {
            comment = "Dữ liệu chưa đủ để đánh giá xu hướng tháng này.\n\nHãy duy trì ghi nhận thêm vài ngày nữa để có lời khuyên chính xác.";
        } else {
            // Ưu tiên khen ngợi nếu tốt
            if (good >= count * 0.7) {
                comment = "Thật tuyệt vời! Bạn đang duy trì phong độ giấc ngủ rất ổn định.\n\nCơ thể bạn đang được phục hồi năng lượng tối ưu.";
            }
            // Nếu không tốt, tìm vấn đề lớn nhất
            else if (shortCount >= count * 0.4) { // > 40% ngày bị thiếu ngủ
                comment = "Cảnh báo: Bạn thường xuyên ngủ thiếu giờ trong tháng này.\n\nCố gắng sắp xếp ngủ sớm hơn hoặc ngủ thêm vào cuối tuần để bù đắp.";
            } else if (lateCount >= count * 0.4) { // > 40% ngày ngủ muộn
                comment = "Bạn có xu hướng \"cú đêm\" khá thường xuyên.\n\nNgủ muộn kéo dài sẽ ảnh hưởng đến nhịp sinh học, hãy thử đi ngủ trước 00:00.";
            } else if (overCount >= count * 0.3) { // > 30% ngày ngủ nhiều
                comment = "Bạn có khá nhiều ngày ngủ \"nướng\".\n\nNgủ quá nhiều đôi khi cũng gây mệt mỏi như thiếu ngủ vậy, hãy giữ nhịp độ điều độ hơn.";
            } else if (bad > good) {
                comment = "Chất lượng giấc ngủ tháng này chưa ổn định.\n\nĐừng lo lắng, hãy bắt đầu bằng việc thiết lập giờ đi ngủ cố định mỗi ngày.";
            } else {
                comment = "Giấc ngủ của bạn ở mức trung bình khá.\n\nMột chút điều chỉnh nhỏ về không gian ngủ và giờ giấc sẽ giúp bạn đạt trạng thái tốt nhất.";
            }
        }

        tvCatComment.setText(comment);
    }

    private void addDayCell(float hours, boolean isToday) {
        View cell = LayoutInflater.from(getContext()).inflate(R.layout.item_calendar_day_v2, gridCalendar, false);
        ImageView icon = cell.findViewById(R.id.img_sleep_icon);
        View highlight = cell.findViewById(R.id.view_today_highlight);
        View tooltip = cell.findViewById(R.id.tooltip_container);
        TextView tvTip = cell.findViewById(R.id.tv_tooltip_text);

        if (hours > 0) {
            // Dùng hàm getQualityColor để lấy màu chuẩn
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