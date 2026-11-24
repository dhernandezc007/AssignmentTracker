package com.example.assignmenttracker.ui.farm;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.assignmenttracker.R;
import com.example.assignmenttracker.data.Crop;

/**
 * One plot of the milpa: a soil mound and a crop drawn at any growth from 0 (a seed in the
 * ground) to 1 (fully grown). Drawn rather than a set of pictures, so a plant grows smoothly
 * day by day. Sizes are in units of 1/100 of the smaller side, so it scales to any cell.
 * Decorative: the cell around it carries the content description.
 */
public class PlantView extends View {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF oval = new RectF();
    private final ArgbEvaluator argb = new ArgbEvaluator();

    /** Null: an empty plot, just soil waiting for a seed. */
    @Nullable private Crop crop = Crop.MAIZ;
    private float growth;

    // Per-draw geometry.
    private float u;
    private float cx;
    private float ground;
    private float maxHeight;

    public PlantView(@NonNull Context context) {
        this(context, null);
    }

    public PlantView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** Just soil: a furrow waiting for its seed. */
    public void setEmpty() {
        if (crop == null) return;
        crop = null;
        invalidate();
    }

    public void setPlot(@NonNull Crop crop, float growth) {
        float g = Math.max(0f, Math.min(1f, growth));
        if (this.crop == crop && this.growth == g) return;
        this.crop = crop;
        this.growth = g;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w == 0 || h == 0) return;
        u = Math.min(w, h) / 100f;
        cx = w / 2f;
        ground = h * 0.80f;
        maxHeight = ground - 8 * u;

        drawSoil(canvas, w, h);
        Crop crop = this.crop;
        if (crop == null) return;
        if (growth < 0.1f) {
            drawSeed(canvas, crop);
        } else {
            // Growth past the seed stage, eased so early days show visible change.
            float t = ease((growth - 0.1f) / 0.9f);
            switch (crop) {
                case FRIJOL:
                    drawFrijol(canvas, t);
                    break;
                case CALABAZA:
                    drawCalabaza(canvas, w, t);
                    break;
                case CHILE:
                    drawChile(canvas, t);
                    break;
                default:
                    drawMaiz(canvas, t);
            }
        }
        if (growth >= 1f) drawSparkles(canvas, w);
    }

    // ---- ground and seed ----

    private void drawSoil(@NonNull Canvas canvas, float w, float h) {
        path.reset();
        path.moveTo(0, ground + 4 * u);
        path.quadTo(cx, ground - 5 * u, w, ground + 4 * u);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();
        fill.setColor(color(R.color.farm_soil));
        canvas.drawPath(path, fill);

        stroke.setColor(color(R.color.farm_soil_dark));
        stroke.setStrokeWidth(1.2f * u);
        for (int i = 1; i <= 2; i++) {
            float y = ground + 4 * u + i * (h - ground) / 3.2f;
            path.reset();
            path.moveTo(w * 0.08f, y);
            path.quadTo(cx, y - 3 * u, w * 0.92f, y);
            canvas.drawPath(path, stroke);
        }
    }

    private void drawSeed(@NonNull Canvas canvas, @NonNull Crop crop) {
        // A little dark hollow, the seed in it, and from halfway a curl about to break the ground.
        fill.setColor(color(R.color.farm_soil_dark));
        oval.set(cx - 10 * u, ground - 3 * u, cx + 10 * u, ground + 4 * u);
        canvas.drawOval(oval, fill);

        int seedColor;
        switch (crop) {
            case FRIJOL:
                seedColor = color(R.color.farm_bean);
                break;
            case CALABAZA:
            case CHILE:
                seedColor = color(R.color.farm_seed_light);
                break;
            default:
                seedColor = color(R.color.farm_corn);
        }
        fill.setColor(seedColor);
        canvas.save();
        canvas.rotate(-20, cx, ground);
        oval.set(cx - 4 * u, ground - 3 * u, cx + 4 * u, ground + 2.5f * u);
        canvas.drawOval(oval, fill);
        canvas.restore();

        if (growth >= 0.05f) {
            stroke.setColor(color(R.color.farm_leaf_light));
            stroke.setStrokeWidth(1.6f * u);
            path.reset();
            path.moveTo(cx + u, ground - 2 * u);
            path.quadTo(cx + 2 * u, ground - 9 * u, cx - 2 * u, ground - 8 * u);
            canvas.drawPath(path, stroke);
        }
    }

    // ---- maíz: a tall stalk, arching leaves, a tassel, then ears of corn ----

    private void drawMaiz(@NonNull Canvas canvas, float t) {
        float height = maxHeight * (0.12f + 0.88f * t);
        float top = ground - height;
        stroke.setColor(color(R.color.farm_stem));
        stroke.setStrokeWidth((1.4f + 2f * t) * u);
        path.reset();
        path.moveTo(cx, ground);
        path.quadTo(cx - 2 * u, ground - height / 2, cx, top);
        canvas.drawPath(path, stroke);

        int leaves = 2 + Math.round(t * 6);
        for (int i = 0; i < leaves; i++) {
            float along = (i + 0.6f) / (leaves + 0.4f);
            float y = ground - height * along;
            float side = i % 2 == 0 ? -1 : 1;
            float length = (12 + 22 * t) * u * (1.1f - 0.5f * along);
            leaf(canvas, cx, y, side, length, i % 3 == 0 ? R.color.farm_leaf_light : R.color.farm_leaf);
        }

        if (growth >= 0.7f) {
            float s = Math.min(1f, (growth - 0.7f) / 0.2f);
            stroke.setColor(color(R.color.farm_tassel));
            stroke.setStrokeWidth(1.2f * u);
            for (int i = 0; i < 5; i++) {
                double a = Math.toRadians(-130 + i * 20);
                float len = (4 + 6 * s) * u;
                canvas.drawLine(cx, top, cx + (float) Math.cos(a) * len, top + (float) Math.sin(a) * len, stroke);
            }
        }
        if (growth >= 0.8f) {
            float s = Math.min(1f, (growth - 0.8f) / 0.2f);
            ear(canvas, cx + 2 * u, ground - height * 0.45f, 1, s);
            if (growth >= 1f) ear(canvas, cx - 2 * u, ground - height * 0.32f, -1, 0.85f);
        }
    }

    /** A long blade from the stalk at (x, y) out to one side, drooping at the tip. */
    private void leaf(@NonNull Canvas canvas, float x, float y, float side, float length, @ColorRes int colorRes) {
        path.reset();
        path.moveTo(x, y);
        path.quadTo(x + side * length * 0.5f, y - length * 0.5f, x + side * length, y + length * 0.12f);
        path.quadTo(x + side * length * 0.45f, y - length * 0.2f, x, y + 2.5f * u);
        path.close();
        fill.setColor(color(colorRes));
        canvas.drawPath(path, fill);
    }

    private void ear(@NonNull Canvas canvas, float x, float y, float side, float size) {
        float len = (6 + 10 * size) * u;
        canvas.save();
        canvas.rotate(side * 28, x, y);
        // Husk, then the yellow cob peeking out of it, then a few kernel dots.
        fill.setColor(color(R.color.farm_husk));
        oval.set(x - len * 0.32f, y - len, x + len * 0.32f, y);
        canvas.drawOval(oval, fill);
        fill.setColor(color(R.color.farm_corn));
        oval.set(x - len * 0.2f, y - len * 1.05f, x + len * 0.2f, y - len * 0.35f);
        canvas.drawOval(oval, fill);
        fill.setColor(color(R.color.farm_tassel));
        for (int i = 0; i < 4; i++) {
            canvas.drawCircle(x, y - len * (0.95f - i * 0.15f), 0.6f * u, fill);
        }
        canvas.restore();
    }

    // ---- frijol: a vine climbing a stake, flowers, then pods ----

    private void drawFrijol(@NonNull Canvas canvas, float t) {
        float stakeTop = ground - maxHeight * 0.95f;
        fill.setColor(color(R.color.farm_stake));
        canvas.drawRect(cx - 1.2f * u, stakeTop, cx + 1.2f * u, ground + 2 * u, fill);

        float height = maxHeight * 0.92f * (0.1f + 0.9f * t);
        stroke.setColor(color(R.color.farm_stem));
        stroke.setStrokeWidth(1.4f * u);
        path.reset();
        path.moveTo(cx, ground);
        int steps = 40;
        for (int i = 1; i <= steps; i++) {
            float y = ground - height * i / steps;
            float x = cx + (float) Math.sin(i * 0.55f) * 5 * u;
            path.lineTo(x, y);
        }
        canvas.drawPath(path, stroke);

        int leaves = 2 + Math.round(t * 7);
        for (int i = 0; i < leaves; i++) {
            float along = (i + 0.7f) / (leaves + 0.3f);
            float y = ground - height * along;
            float side = i % 2 == 0 ? 1 : -1;
            trifoliate(canvas, cx + side * 5 * u, y, side, (5 + 3 * t) * u);
        }

        if (growth >= 0.65f) {
            fill.setColor(color(R.color.farm_bean_flower));
            for (int i = 1; i < leaves; i += 2) {
                float y = ground - height * (i + 0.2f) / (leaves + 0.3f);
                float side = (i / 2) % 2 == 0 ? -1 : 1;
                canvas.drawCircle(cx + side * 3 * u, y, 1.6f * u, fill);
            }
        }
        if (growth >= 0.8f) {
            float s = Math.min(1f, (growth - 0.8f) / 0.2f);
            stroke.setColor(color(R.color.farm_husk));
            stroke.setStrokeWidth(2.6f * u);
            for (int i = 0; i < 3; i++) {
                float y = ground - height * (0.3f + 0.22f * i);
                float side = i % 2 == 0 ? -1 : 1;
                float x = cx + side * 4 * u;
                float len = (5 + 8 * s) * u;
                path.reset();
                path.moveTo(x, y);
                path.quadTo(x + side * 4 * u, y + len * 0.5f, x + side * 1.5f * u, y + len);
                canvas.drawPath(path, stroke);
            }
        }
    }

    /** Three oval leaflets fanned from one point. */
    private void trifoliate(@NonNull Canvas canvas, float x, float y, float side, float size) {
        fill.setColor(color(R.color.farm_leaf));
        for (int i = -1; i <= 1; i++) {
            canvas.save();
            canvas.rotate(side * (60 + i * 45), x, y);
            oval.set(x - size * 0.35f, y - size, x + size * 0.35f, y);
            canvas.drawOval(oval, fill);
            canvas.restore();
        }
    }

    // ---- calabaza: a vine spreading over the ground, big leaves, a flower, then squash ----

    private void drawCalabaza(@NonNull Canvas canvas, float w, float t) {
        float spread = w * 0.44f * (0.1f + 0.9f * t);
        stroke.setColor(color(R.color.farm_stem));
        stroke.setStrokeWidth(1.6f * u);
        for (int side = -1; side <= 1; side += 2) {
            path.reset();
            path.moveTo(cx, ground - u);
            path.quadTo(cx + side * spread * 0.5f, ground - 8 * u, cx + side * spread, ground - 2 * u);
            canvas.drawPath(path, stroke);
        }

        // Two seed leaves early on, big round leaves as it spreads.
        int perSide = 1 + Math.round(t * 3);
        for (int i = 0; i < perSide; i++) {
            for (int side = -1; side <= 1; side += 2) {
                float along = (i + 0.6f) / (perSide + 0.2f);
                float x = cx + side * spread * along;
                float r = (5 + 9 * t) * u * (1.1f - 0.35f * along);
                roundLeaf(canvas, x, ground - r * 0.9f - 2 * u, r);
            }
        }

        if (growth >= 0.6f) {
            star(canvas, cx - spread * 0.55f, ground - 16 * u * (0.6f + 0.4f * t), 4 * u);
        }
        if (growth >= 0.75f) {
            float s = Math.min(1f, (growth - 0.75f) / 0.25f);
            float x = cx + spread * 0.25f;
            float rx = (6 + 12 * s) * u;
            float ry = (5 + 9 * s) * u;
            float y = ground + 2 * u - ry;
            fill.setColor(color(R.color.farm_stem));
            canvas.drawRect(x - u, y - ry - 3 * u, x + u, y - ry + u, fill);
            fill.setColor(color(R.color.farm_squash));
            oval.set(x - rx, y - ry, x + rx, y + ry);
            canvas.drawOval(oval, fill);
            stroke.setColor(color(R.color.farm_squash_rib));
            stroke.setStrokeWidth(1.2f * u);
            oval.set(x - rx * 0.45f, y - ry, x + rx * 0.45f, y + ry);
            canvas.drawOval(oval, stroke);
        }
    }

    private void roundLeaf(@NonNull Canvas canvas, float x, float y, float r) {
        fill.setColor(color(R.color.farm_leaf));
        canvas.drawCircle(x, y, r, fill);
        stroke.setColor(color(R.color.farm_leaf_light));
        stroke.setStrokeWidth(0.9f * u);
        for (int i = 0; i < 3; i++) {
            double a = Math.toRadians(-150 + i * 60);
            canvas.drawLine(x, y + r * 0.6f, x + (float) Math.cos(a) * r * 0.8f, y + (float) Math.sin(a) * r * 0.8f, stroke);
        }
    }

    /** The squash blossom: a five-point yellow star with an orange heart. */
    private void star(@NonNull Canvas canvas, float x, float y, float r) {
        path.reset();
        for (int i = 0; i < 10; i++) {
            double a = Math.toRadians(-90 + i * 36);
            float rr = i % 2 == 0 ? r : r * 0.45f;
            float px = x + (float) Math.cos(a) * rr;
            float py = y + (float) Math.sin(a) * rr;
            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        path.close();
        fill.setColor(color(R.color.mx_yellow));
        canvas.drawPath(path, fill);
        fill.setColor(color(R.color.farm_squash));
        canvas.drawCircle(x, y, r * 0.3f, fill);
    }

    // ---- chile: a small branching bush, white flowers, then chiles turning green to red ----

    private void drawChile(@NonNull Canvas canvas, float t) {
        float height = maxHeight * 0.68f * (0.15f + 0.85f * t);
        float fork = ground - height * 0.45f;
        float top = ground - height;
        float reach = (4 + 14 * t) * u;
        stroke.setColor(color(R.color.farm_stem));
        stroke.setStrokeWidth((1.2f + 1.2f * t) * u);
        canvas.drawLine(cx, ground, cx, fork, stroke);
        float[][] tips = {{cx - reach, top + 4 * u}, {cx + reach, top + 2 * u}, {cx, top}};
        for (float[] tip : tips) {
            path.reset();
            path.moveTo(cx, fork);
            path.quadTo((cx + tip[0]) / 2, fork - (fork - tip[1]) * 0.3f, tip[0], tip[1]);
            canvas.drawPath(path, stroke);
        }

        fill.setColor(color(R.color.farm_leaf));
        int leaves = 3 + Math.round(t * 6);
        for (int i = 0; i < leaves; i++) {
            float[] tip = tips[i % 3];
            int ring = i / 3;            // which leaf along its branch
            int rings = leaves / 3;
            float along = 0.35f + 0.65f * (ring + 1f) / (rings + 1f);
            float x = cx + (tip[0] - cx) * along;
            float y = fork + (tip[1] - fork) * along;
            canvas.save();
            canvas.rotate(i % 2 == 0 ? -35 : 35, x, y);
            float s = (3 + 3 * t) * u;
            oval.set(x - s * 0.5f, y - s * 1.3f, x + s * 0.5f, y);
            canvas.drawOval(oval, fill);
            canvas.restore();
        }

        if (growth >= 0.6f && growth < 0.95f) {
            fill.setColor(color(R.color.farm_flower_white));
            canvas.drawCircle(tips[0][0], tips[0][1], 1.8f * u, fill);
            canvas.drawCircle(tips[1][0], tips[1][1], 1.8f * u, fill);
        }
        if (growth >= 0.75f) {
            float ripe = Math.min(1f, (growth - 0.75f) / 0.25f);
            int c = (int) argb.evaluate(ripe, color(R.color.farm_leaf_light), color(R.color.mx_red));
            stroke.setColor(c);
            stroke.setStrokeWidth(2.6f * u);
            for (int i = 0; i < 3; i++) {
                float[] tip = tips[i];
                float x = cx + (tip[0] - cx) * 0.7f;
                float y = fork + (tip[1] - fork) * 0.7f + 2 * u;
                float len = (5 + 6 * ripe) * u;
                path.reset();
                path.moveTo(x, y);
                path.quadTo(x + 2 * u, y + len * 0.6f, x - u, y + len);
                canvas.drawPath(path, stroke);
            }
        }
    }

    // ---- fully grown ----

    /** Three little four-point sparkles: this one is done growing. */
    private void drawSparkles(@NonNull Canvas canvas, float w) {
        fill.setColor(color(R.color.mx_marigold));
        sparkle(canvas, w * 0.14f, 14 * u, 4 * u);
        sparkle(canvas, w * 0.86f, 22 * u, 3 * u);
        sparkle(canvas, w * 0.22f, 34 * u, 2.2f * u);
    }

    private void sparkle(@NonNull Canvas canvas, float x, float y, float r) {
        path.reset();
        path.moveTo(x, y - r);
        path.quadTo(x, y, x + r, y);
        path.quadTo(x, y, x, y + r);
        path.quadTo(x, y, x - r, y);
        path.quadTo(x, y, x, y - r);
        path.close();
        canvas.drawPath(path, fill);
    }

    // ---- helpers ----

    private int color(@ColorRes int res) {
        return ContextCompat.getColor(getContext(), res);
    }

    /** Ease-out: quick early change that settles as it matures. */
    private static float ease(float x) {
        float c = Math.max(0f, Math.min(1f, x));
        return 1f - (1f - c) * (1f - c);
    }
}
