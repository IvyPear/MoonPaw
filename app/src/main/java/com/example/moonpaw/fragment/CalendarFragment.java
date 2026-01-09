package com.example.moonpaw.fragment;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.moonpaw.R;
import com.example.moonpaw.utils.PdfExporter;
import com.example.moonpaw.utils.SleepAnalyzer;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
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

        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            }
        });

        view.findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            updateUI();
        });
        view.findViewById(R.id.btn_next_month).setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1);
            updateUI();
        });

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

    private void showEditDialog(Calendar dateCal) {
        String dateKey = SleepAnalyzer.getDateKey(dateCal.getTimeInMillis());
        String currentBedtime = prefs.getString("bedtime_" + dateKey, "23:00");
        String currentWakeup = prefs.getString("wakeup_" + dateKey, "07:00");

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Sửa dữ liệu ngày " + new SimpleDateFormat("dd/MM", Locale.getDefault()).format(dateCal.getTime()));

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 20);

        final TextView tvBed = new TextView(getContext());
        tvBed.setText("Đi ngủ lúc: " + currentBedtime);
        tvBed.setTextSize(18);
        tvBed.setPadding(30, 30, 30, 30);
        tvBed.setBackgroundColor(Color.parseColor("#1Affffff"));
        tvBed.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 30);
        tvBed.setLayoutParams(params);

        final TextView tvWake = new TextView(getContext());
        tvWake.setText("Dậy lúc: " + currentWakeup);
        tvWake.setTextSize(18);
        tvWake.setPadding(30, 30, 30, 30);
        tvWake.setBackgroundColor(Color.parseColor("#1Affffff"));
        tvWake.setGravity(Gravity.CENTER);

        layout.addView(tvBed);
        layout.addView(tvWake);
        builder.setView(layout);

        final String[] tempTimes = {currentBedtime, currentWakeup};

        tvBed.setOnClickListener(v -> {
            String[] parts = tempTimes[0].split(":");
            new TimePickerDialog(getContext(), (view, h, m) -> {
                tempTimes[0] = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                tvBed.setText("Đi ngủ lúc: " + tempTimes[0]);
            }, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), true).show();
        });

        tvWake.setOnClickListener(v -> {
            String[] parts = tempTimes[1].split(":");
            new TimePickerDialog(getContext(), (view, h, m) -> {
                tempTimes[1] = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                tvWake.setText("Dậy lúc: " + tempTimes[1]);
            }, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), true).show();
        });

        builder.setPositiveButton("Lưu lại", (dialog, which) -> {
            float newDuration = SleepAnalyzer.calculateDurationFromString(tempTimes[0], tempTimes[1]);
            prefs.edit()
                    .putFloat(dateKey, newDuration)
                    .putString("bedtime_" + dateKey, tempTimes[0])
                    .putString("wakeup_" + dateKey, tempTimes[1])
                    .apply();
            updateUI();
            Toast.makeText(getContext(), "Đã cập nhật: " + String.format("%.1f", newDuration) + "h", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Hủy", null);

        builder.setNeutralButton("Xóa dữ liệu", (dialog, which) -> {
            prefs.edit().remove(dateKey).remove("bedtime_" + dateKey).remove("wakeup_" + dateKey).apply();
            updateUI();
            Toast.makeText(getContext(), "Đã xóa dữ liệu ngày này", Toast.LENGTH_SHORT).show();
        });

        builder.show();
    }

    private void exportReportToPdf() {
        Toast.makeText(getContext(), "Đang tạo báo cáo...", Toast.LENGTH_SHORT).show();
        float total = 0;
        int good = 0, bad = 0;
        int days = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        List<Float> dailyData = new ArrayList<>();

        for (int d = 1; d <= days; d++) {
            Calendar temp = (Calendar) calendar.clone();
            temp.set(Calendar.DAY_OF_MONTH, d);
            String key = SleepAnalyzer.getDateKey(temp.getTimeInMillis());
            float h = prefs.getFloat(key, 0f);
            dailyData.add(h);

            if (h > 0) {
                total += h;
                if (SleepAnalyzer.evaluateSleepState(h, false) == SleepAnalyzer.SleepState.GOOD) good++;
                else bad++;
            }
        }

        try {
            PdfExporter.createReport(requireContext(), calendar, total, good, bad, tvCatComment.getText().toString(), dailyData);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Lỗi xuất PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

            addDayCell(hours, isToday, dayCal);
        }
    }

    private void calculateStats() {
        float total = 0;
        int count = 0, good = 0, ok = 0, bad = 0;
        int lateCount = 0, shortCount = 0, overCount = 0;
        int maxStreak = 0, currentStreak = 0;
        int days = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int d = 1; d <= days; d++) {
            Calendar dayCal = (Calendar) calendar.clone();
            dayCal.set(Calendar.DAY_OF_MONTH, d);
            String key = SleepAnalyzer.getDateKey(dayCal.getTimeInMillis());
            float h = prefs.getFloat(key, 0f);
            String bedTime = prefs.getString("bedtime_" + key, "23:00");

            if (h > 0) {
                total += h;
                count++;

                SleepAnalyzer.SleepState state = SleepAnalyzer.evaluateSleepState(h, false, bedTime);
                if (state == SleepAnalyzer.SleepState.GOOD) good++;
                else if (state == SleepAnalyzer.SleepState.OK) ok++;
                else bad++;

                SleepAnalyzer.SleepIssue issue = SleepAnalyzer.analyzeSleepIssue(h, bedTime);
                if (issue == SleepAnalyzer.SleepIssue.LATE_SLEEP || issue == SleepAnalyzer.SleepIssue.SHORT_AND_LATE) lateCount++;
                if (issue == SleepAnalyzer.SleepIssue.SHORT_SLEEP || issue == SleepAnalyzer.SleepIssue.SHORT_AND_LATE) shortCount++;
                if (issue == SleepAnalyzer.SleepIssue.OVER_SLEEP) overCount++;

                // --- [ĐÃ SỬA] LOGIC CHUỖI TỐT NHẤT ---
                // Chỉ đếm chuỗi khi ngủ GOOD (Xanh) hoặc OK (Vàng).
                // Nếu ngủ BAD (Đỏ) -> Reset chuỗi về 0
                if (state == SleepAnalyzer.SleepState.GOOD || state == SleepAnalyzer.SleepState.OK) {
                    currentStreak++;
                } else {
                    currentStreak = 0;
                }
                if (currentStreak > maxStreak) maxStreak = currentStreak;
                // -------------------------------------

            } else {
                currentStreak = 0;
            }
        }

        tvTotalHours.setText(String.format(Locale.getDefault(), "%.1f", total));
        float avg = (count > 0) ? total / count : 0;
        tvAvgSleep.setText(String.format(Locale.getDefault(), "%dh %dp", (int)avg, (int)((avg-(int)avg)*60)));

        tvGoodCount.setText(String.valueOf(good));
        tvOkCount.setText(String.valueOf(ok));
        tvBadCount.setText(String.valueOf(bad));
        tvBestStreak.setText(String.valueOf(maxStreak));

        String comment;
        if (count == 0) comment = "Chưa có dữ liệu giấc ngủ trong tháng này.\n\nNhấn vào một ngày bất kỳ trên lịch để thêm dữ liệu thủ công.";
        else if (count < 5) comment = "Dữ liệu chưa đủ để đánh giá xu hướng tháng này.\n\nHãy duy trì ghi nhận thêm vài ngày nữa.";
        else {
            if (good >= count * 0.7) comment = "Thật tuyệt vời! Bạn đang duy trì phong độ giấc ngủ rất ổn định.";
            else if (shortCount >= count * 0.4) comment = "Cảnh báo: Bạn thường xuyên ngủ thiếu giờ.\n\nHãy thử điều chỉnh giờ ngủ sớm hơn.";
            else if (lateCount >= count * 0.4) comment = "Bạn có xu hướng \"cú đêm\".\n\nNgủ muộn sẽ ảnh hưởng đến nhịp sinh học.";
            else if (overCount >= count * 0.3) comment = "Bạn có nhiều ngày ngủ quá nhiều.\n\nĐiều này có thể gây mệt mỏi ngang với thiếu ngủ.";
            else if (bad > good) comment = "Chất lượng giấc ngủ chưa ổn định.\n\nHãy thử thiết lập giờ đi ngủ cố định.";
            else comment = "Giấc ngủ của bạn ở mức trung bình khá.";
        }
        tvCatComment.setText(comment);
    }

    private void addDayCell(float hours, boolean isToday, Calendar dayDate) {
        View cell = LayoutInflater.from(getContext()).inflate(R.layout.item_calendar_day_v2, gridCalendar, false);
        ImageView icon = cell.findViewById(R.id.img_sleep_icon);
        View highlight = cell.findViewById(R.id.view_today_highlight);
        View tooltip = cell.findViewById(R.id.tooltip_container);
        TextView tvTip = cell.findViewById(R.id.tv_tooltip_text);

        if (hours > 0) {
            icon.setColorFilter(SleepAnalyzer.getQualityColor(hours));
            icon.setAlpha(1.0f);

            // --- [ĐÃ SỬA] HIỂN THỊ SỐ GIỜ TRÊN MỌI NGÀY CÓ DỮ LIỆU ---
            // Không cần điều kiện isToday nữa, cứ có giờ ngủ là hiện
            tooltip.setVisibility(View.VISIBLE);
            tvTip.setText(String.format("%.1fh", hours));
            // --------------------------------------------------------
        } else {
            icon.setAlpha(0.1f);
            tooltip.setVisibility(View.INVISIBLE); // Ẩn nếu không có dữ liệu
        }

        if (isToday) {
            highlight.setVisibility(View.VISIBLE);
        }

        cell.setOnClickListener(v -> {
            Calendar targetDate = (Calendar) dayDate.clone();
            showEditDialog(targetDate);
        });

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