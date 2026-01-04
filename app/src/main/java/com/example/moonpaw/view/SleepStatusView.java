package com.example.moonpaw.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class SleepStatusView extends View {

    private Paint bgPaint, progressPaint, textPaint;
    private float progress = 0.75f;
    private String centerText = "✓";
    private int progressColor = Color.parseColor("#10b981");

    public SleepStatusView(Context context) {
        super(context);
        init();
    }

    public SleepStatusView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Background circle paint
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.parseColor("#1e293b"));
        bgPaint.setStyle(Paint.Style.FILL);

        // Progress arc paint
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(progressColor);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(12f);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        // Center text paint
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(60f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    public void setStatus(float progress, String text, int color) {
        this.progress = progress;
        this.centerText = text;
        this.progressColor = color;
        progressPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(width, height) / 2 - 20;

        // Draw background circle
        canvas.drawCircle(centerX, centerY, radius, bgPaint);

        // Draw progress arc
        if (progress > 0) {
            RectF oval = new RectF(
                    centerX - radius + 10,
                    centerY - radius + 10,
                    centerX + radius - 10,
                    centerY + radius - 10
            );
            float sweepAngle = 360 * progress;
            canvas.drawArc(oval, -90, sweepAngle, false, progressPaint);
        }

        // Draw center text
        float textY = centerY - ((textPaint.descent() + textPaint.ascent()) / 2);
        canvas.drawText(centerText, centerX, textY, textPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredSize = 200; // dp
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width, height;

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(desiredSize, widthSize);
        } else {
            width = desiredSize;
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredSize, heightSize);
        } else {
            height = desiredSize;
        }

        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }
}