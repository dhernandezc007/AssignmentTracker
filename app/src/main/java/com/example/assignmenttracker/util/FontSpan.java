package com.example.assignmenttracker.util;

import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.NonNull;

/**
 * Sets a bundled typeface on a span of text. The platform's {@code TypefaceSpan(Typeface)}
 * needs API 28; minSdk is 24.
 */
public final class FontSpan extends MetricAffectingSpan {

    @NonNull private final Typeface typeface;

    public FontSpan(@NonNull Typeface typeface) {
        this.typeface = typeface;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint paint) {
        paint.setTypeface(typeface);
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint paint) {
        paint.setTypeface(typeface);
    }
}
