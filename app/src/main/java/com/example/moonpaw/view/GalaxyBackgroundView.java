package com.example.moonpaw.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;

import com.example.moonpaw.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GalaxyBackgroundView extends View {

    private Paint starPaint;
    private Paint glowPaint;
    private List<Star> stars;
    private int width, height;
    private float density;

    // Các kiểu ngôi sao khác nhau
    private enum StarType {
        GLOW_CIRCLE,    // Hình tròn có glow
        PLUS,           // Hình dấu +
        CROSS,          // Hình chữ X
        DIAMOND,        // Hình kim cương
        SMALL_DOT       // Chấm nhỏ sắc nét
    }

    private static class Star {
        float x, y, size, alpha;
        StarType type;

        Star(float x, float y, float size, float alpha, StarType type) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.alpha = alpha;
            this.type = type;
        }
    }

    public GalaxyBackgroundView(Context context) {
        super(context);
        init();
    }

    public GalaxyBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setColor(0xFFFFFFFF);
        starPaint.setStyle(Paint.Style.FILL);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(0xFFFFFFFF);
        glowPaint.setStyle(Paint.Style.FILL);

        stars = new ArrayList<>();
        density = getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        generateStars();
    }

    private void generateStars() {
        stars.clear();
        Random random = new Random(54321); // Fixed seed

        int rows = 14;
        int cols = 8;

        float cellWidth = (float) width / cols;
        float cellHeight = (float) height / rows;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (random.nextFloat() < 0.75f) {
                    float padding = 0.15f;
                    float x = col * cellWidth + cellWidth * padding +
                            random.nextFloat() * cellWidth * (1 - 2 * padding);
                    float y = row * cellHeight + cellHeight * padding +
                            random.nextFloat() * cellHeight * (1 - 2 * padding);

                    // Phân bố kích thước
                    float sizeRoll = random.nextFloat();
                    float size;
                    if (sizeRoll < 0.65f) {
                        size = 0.8f + random.nextFloat() * 1.2f; // 0.8-2dp
                    } else if (sizeRoll < 0.88f) {
                        size = 1.5f + random.nextFloat() * 1.5f; // 1.5-3dp
                    } else {
                        size = 2.5f + random.nextFloat() * 1.5f; // 2.5-4dp
                    }

                    // Phân bố opacity
                    float alphaRoll = random.nextFloat();
                    float alpha;
                    if (alphaRoll < 0.5f) {
                        alpha = 0.25f + random.nextFloat() * 0.35f; // 25-60%
                    } else if (alphaRoll < 0.85f) {
                        alpha = 0.55f + random.nextFloat() * 0.25f; // 55-80%
                    } else {
                        alpha = 0.75f + random.nextFloat() * 0.25f; // 75-100%
                    }

                    // Chọn kiểu ngôi sao
                    StarType type = getRandomStarType(random, size);
                    stars.add(new Star(x, y, size, alpha, type));
                }
            }
        }

        // Thêm ngôi sao ngẫu nhiên
        int extraStars = 25;
        for (int i = 0; i < extraStars; i++) {
            float x = random.nextFloat() * width;
            float y = random.nextFloat() * height;
            float size = 0.8f + random.nextFloat() * 2f;
            float alpha = 0.2f + random.nextFloat() * 0.6f;
            StarType type = getRandomStarType(random, size);
            stars.add(new Star(x, y, size, alpha, type));
        }
    }

    private StarType getRandomStarType(Random random, float size) {
        float roll = random.nextFloat();

        // Ngôi sao nhỏ: chủ yếu là dot và glow
        if (size < 1.5f) {
            return roll < 0.6f ? StarType.SMALL_DOT : StarType.GLOW_CIRCLE;
        }
        // Ngôi sao trung bình: mix các kiểu
        else if (size < 2.5f) {
            if (roll < 0.3f) return StarType.GLOW_CIRCLE;
            else if (roll < 0.55f) return StarType.PLUS;
            else if (roll < 0.8f) return StarType.CROSS;
            else return StarType.DIAMOND;
        }
        // Ngôi sao lớn: ưu tiên các kiểu phức tạp
        else {
            if (roll < 0.25f) return StarType.GLOW_CIRCLE;
            else if (roll < 0.5f) return StarType.PLUS;
            else if (roll < 0.75f) return StarType.CROSS;
            else return StarType.DIAMOND;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int bgColor = ContextCompat.getColor(getContext(), R.color.background_dark);
        canvas.drawColor(bgColor);

        // Gradient blobs
        drawGradientBlob(canvas, width * 0.15f, height * 0.12f, width * 0.55f, 0x266366f1);
        drawGradientBlob(canvas, width * 0.85f, height * 0.15f, width * 0.48f, 0x1A6366f1);
        drawGradientBlob(canvas, width * 0.12f, height * 0.45f, width * 0.46f, 0x15a855f7);
        drawGradientBlob(canvas, width * 0.78f, height * 0.50f, width * 0.43f, 0x0D6366f1);
        drawGradientBlob(canvas, width * 0.18f, height * 0.80f, width * 0.50f, 0x1Da855f7);
        drawGradientBlob(canvas, width * 0.82f, height * 0.88f, width * 0.53f, 0x1Aa855f7);
        drawGradientBlob(canvas, width * 0.50f, height * 0.60f, width * 0.35f, 0x0A6366f1);

        // Vẽ ngôi sao với các style khác nhau
        for (Star star : stars) {
            drawStar(canvas, star);
        }
    }

    private void drawStar(Canvas canvas, Star star) {
        float size = star.size * density;
        int alpha = (int) (star.alpha * 255);

        switch (star.type) {
            case GLOW_CIRCLE:
                drawGlowCircle(canvas, star.x, star.y, size, alpha);
                break;
            case PLUS:
                drawPlus(canvas, star.x, star.y, size, alpha);
                break;
            case CROSS:
                drawCross(canvas, star.x, star.y, size, alpha);
                break;
            case DIAMOND:
                drawDiamond(canvas, star.x, star.y, size, alpha);
                break;
            case SMALL_DOT:
                drawSmallDot(canvas, star.x, star.y, size, alpha);
                break;
        }
    }

    // Hình tròn với hiệu ứng glow mềm
    private void drawGlowCircle(Canvas canvas, float x, float y, float size, int alpha) {
        // Glow layer
        glowPaint.setAlpha((int) (alpha * 0.3f));
        RadialGradient glow = new RadialGradient(
                x, y, size * 2f,
                0xFFFFFFFF, 0x00FFFFFF,
                Shader.TileMode.CLAMP
        );
        glowPaint.setShader(glow);
        canvas.drawCircle(x, y, size * 2f, glowPaint);

        // Core circle
        starPaint.setAlpha(alpha);
        starPaint.setShader(null);
        canvas.drawCircle(x, y, size * 0.4f, starPaint);
    }

    // Dấu cộng (+) với glow nhẹ
    private void drawPlus(Canvas canvas, float x, float y, float size, int alpha) {
        float lineWidth = size * 0.2f;
        float length = size * 0.6f; // Giảm từ 1.2f xuống 0.6f

        // Glow
        glowPaint.setAlpha((int) (alpha * 0.15f));
        glowPaint.setStrokeWidth(lineWidth * 2.5f);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setShader(null);
        canvas.drawLine(x - length, y, x + length, y, glowPaint);
        canvas.drawLine(x, y - length, x, y + length, glowPaint);

        // Core lines
        starPaint.setAlpha(alpha);
        starPaint.setStrokeWidth(lineWidth);
        starPaint.setStyle(Paint.Style.STROKE);
        starPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(x - length, y, x + length, y, starPaint);
        canvas.drawLine(x, y - length, x, y + length, starPaint);

        starPaint.setStyle(Paint.Style.FILL);
    }

    // Chữ X (×) xoay 45 độ
    private void drawCross(Canvas canvas, float x, float y, float size, int alpha) {
        float lineWidth = size * 0.2f;
        float length = size * 0.5f;

        // Glow
        glowPaint.setAlpha((int) (alpha * 0.15f));
        glowPaint.setStrokeWidth(lineWidth * 2.5f);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setShader(null);
        canvas.drawLine(x - length, y - length, x + length, y + length, glowPaint);
        canvas.drawLine(x - length, y + length, x + length, y - length, glowPaint);

        // Core
        starPaint.setAlpha(alpha);
        starPaint.setStrokeWidth(lineWidth);
        starPaint.setStyle(Paint.Style.STROKE);
        starPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(x - length, y - length, x + length, y + length, starPaint);
        canvas.drawLine(x - length, y + length, x + length, y - length, starPaint);

        starPaint.setStyle(Paint.Style.FILL);
    }

    // Kim cương (◇)
    private void drawDiamond(Canvas canvas, float x, float y, float size, int alpha) {
        Path path = new Path();
        float halfSize = size * 0.65f; // Giảm từ 1.0f xuống 0.65f

        path.moveTo(x, y - halfSize);
        path.lineTo(x + halfSize, y);
        path.lineTo(x, y + halfSize);
        path.lineTo(x - halfSize, y);
        path.close();

        // Glow
        glowPaint.setAlpha((int) (alpha * 0.2f));
        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setShader(null);
        canvas.save();
        canvas.scale(1.4f, 1.4f, x, y); // Giảm từ 1.6f xuống 1.4f
        canvas.drawPath(path, glowPaint);
        canvas.restore();

        // Core
        starPaint.setAlpha(alpha);
        starPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, starPaint);
    }

    // Chấm nhỏ sắc nét
    private void drawSmallDot(Canvas canvas, float x, float y, float size, int alpha) {
        starPaint.setAlpha(alpha);
        starPaint.setShader(null);
        canvas.drawCircle(x, y, size * 0.5f, starPaint);
    }

    private void drawGradientBlob(Canvas canvas, float centerX, float centerY, float radius, int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        RadialGradient gradient = new RadialGradient(
                centerX, centerY, radius,
                color, 0x00000000,
                Shader.TileMode.CLAMP
        );
        paint.setShader(gradient);
        canvas.drawCircle(centerX, centerY, radius, paint);
    }
}