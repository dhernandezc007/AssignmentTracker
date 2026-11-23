package com.example.assignmenttracker.ui.fiesta;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.assignmenttracker.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mark-done fiesta: a shower of fiesta-colored confetti. Ribbons, cempasúchil petals and tiny
 * papel picado flags rain from the top, while two cañones fire from the bottom corners.
 * Draw-only: it never takes touches or focus, and it does nothing when animations are off.
 */
public class ConfettiView extends View {

    private static final int SHOWER = 70;
    private static final int PER_CANNON = 28;
    private static final long MAX_MS = 4200;
    private static final long FADE_MS = 700;
    /** dp/s² and 1/s: confetti falls at about GRAVITY / DRAG = 300 dp/s. */
    private static final float GRAVITY = 900f;
    private static final float DRAG = 3f;

    private static final int RIBBON = 0;
    private static final int PETAL = 1;
    private static final int FLAG = 2;

    private static final class Piece {
        float x, y, vx, vy;
        float rotation, spin;
        float flip, flipSpeed;
        float sway, swaySpeed;
        float size;
        int color;
        int shape;
    }

    private final List<Piece> pieces = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path flag = new Path();
    private final Random random = new Random();
    private final float dp;
    private final int[] colors;
    private final int[] petalColors;
    private long startNanos;
    private long lastNanos;

    public ConfettiView(@NonNull Context context) {
        this(context, null);
    }

    public ConfettiView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        colors = new int[]{
                color(R.color.mx_pink), color(R.color.mx_marigold), color(R.color.mx_yellow),
                color(R.color.mx_orange), color(R.color.mx_turquoise), color(R.color.mx_green),
                color(R.color.mx_red), color(R.color.mx_purple)};
        petalColors = new int[]{color(R.color.mx_marigold), color(R.color.mx_orange), color(R.color.mx_yellow)};
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setWillNotDraw(false);
    }

    private int color(int res) {
        return ContextCompat.getColor(getContext(), res);
    }

    /** ¡Fiesta! Adds a new burst (a running one keeps going). */
    public void celebrate() {
        if (!Motion.enabled(getContext()) || getWidth() == 0 || getHeight() == 0) return;
        float w = getWidth();
        float h = getHeight();
        for (int i = 0; i < SHOWER; i++) {
            Piece p = newPiece();
            p.x = random.nextFloat() * w;
            p.y = -random.nextFloat() * h * 0.35f - 12 * dp;
            p.vx = (random.nextFloat() - 0.5f) * 120 * dp;
            p.vy = random.nextFloat() * 200 * dp;
            pieces.add(p);
        }
        for (int side = 0; side < 2; side++) {
            float dir = side == 0 ? 1f : -1f;
            for (int i = 0; i < PER_CANNON; i++) {
                Piece p = newPiece();
                p.x = side == 0 ? 0 : w;
                p.y = h * 0.82f;
                double angle = Math.toRadians(58 + random.nextFloat() * 24); // up and inward
                float speed = (1300 + random.nextFloat() * 900) * dp;
                p.vx = dir * (float) Math.cos(angle) * speed;
                p.vy = -(float) Math.sin(angle) * speed;
                pieces.add(p);
            }
        }
        startNanos = System.nanoTime();
        lastNanos = startNanos;
        postInvalidateOnAnimation();
    }

    @NonNull
    private Piece newPiece() {
        Piece p = new Piece();
        int roll = random.nextInt(10);
        p.shape = roll < 5 ? RIBBON : roll < 8 ? PETAL : FLAG;
        p.color = p.shape == PETAL ? petalColors[random.nextInt(petalColors.length)]
                : colors[random.nextInt(colors.length)];
        p.size = (p.shape == FLAG ? 11 : 8 + random.nextFloat() * 5) * dp;
        p.rotation = random.nextFloat() * 360;
        p.spin = (random.nextFloat() - 0.5f) * 600;
        p.flip = random.nextFloat() * 6.28f;
        p.flipSpeed = 4 + random.nextFloat() * 8;
        p.sway = random.nextFloat() * 6.28f;
        p.swaySpeed = 1.5f + random.nextFloat() * 2.5f;
        return p;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (pieces.isEmpty()) return;
        long now = System.nanoTime();
        float dt = Math.min((now - lastNanos) / 1e9f, 1 / 30f);
        lastNanos = now;
        long elapsedMs = (now - startNanos) / 1_000_000;
        int alpha = elapsedMs < MAX_MS - FADE_MS ? 255
                : (int) Math.max(0, 255 * (MAX_MS - elapsedMs) / (float) FADE_MS);

        float bottom = getHeight() + 24 * dp;
        boolean anyVisible = false;
        for (Piece p : pieces) {
            p.vy += GRAVITY * dp * dt;
            p.vx -= p.vx * DRAG * dt;
            p.vy -= p.vy * DRAG * dt;
            p.sway += p.swaySpeed * dt;
            p.x += (p.vx + (float) Math.sin(p.sway) * 40 * dp) * dt;
            p.y += p.vy * dt;
            p.rotation += p.spin * dt;
            p.flip += p.flipSpeed * dt;
            if (p.y > bottom) continue;
            anyVisible = true;
            draw(canvas, p, alpha);
        }

        if (anyVisible && elapsedMs < MAX_MS) {
            postInvalidateOnAnimation();
        } else {
            pieces.clear();
        }
    }

    private void draw(@NonNull Canvas canvas, @NonNull Piece p, int alpha) {
        paint.setColor(p.color);
        paint.setAlpha(alpha);
        canvas.save();
        canvas.translate(p.x, p.y);
        canvas.rotate(p.rotation);
        // Tumbling: squash one axis, so each piece looks like it turns over in the air.
        canvas.scale(Math.max(0.15f, Math.abs((float) Math.cos(p.flip))), 1f);
        float s = p.size;
        switch (p.shape) {
            case PETAL:
                canvas.drawOval(-s * 0.5f, -s * 0.32f, s * 0.5f, s * 0.32f, paint);
                break;
            case FLAG:
                drawFlag(canvas, s);
                break;
            default:
                canvas.drawRect(-s * 0.5f, -s * 0.22f, s * 0.5f, s * 0.22f, paint);
        }
        canvas.restore();
    }

    /** A tiny papel picado flag: a square with a zigzag bottom and a hole punched in it. */
    private void drawFlag(@NonNull Canvas canvas, float s) {
        float half = s / 2;
        flag.reset();
        flag.setFillType(Path.FillType.EVEN_ODD);
        flag.moveTo(-half, -half);
        flag.lineTo(half, -half);
        flag.lineTo(half, half * 0.6f);
        int teeth = 4;
        float step = s / teeth;
        for (int i = 0; i < teeth; i++) {
            float right = half - i * step;
            flag.lineTo(right - step / 2, half);
            flag.lineTo(right - step, half * 0.6f);
        }
        flag.close();
        flag.addCircle(0, -half * 0.1f, s * 0.16f, Path.Direction.CW);
        canvas.drawPath(flag, paint);
    }

    @Override
    protected void onDetachedFromWindow() {
        pieces.clear();
        super.onDetachedFromWindow();
    }
}
