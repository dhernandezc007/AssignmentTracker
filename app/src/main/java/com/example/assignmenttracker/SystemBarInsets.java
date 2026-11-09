package com.example.assignmenttracker;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Keeps a screen's root view clear of the status bar, navigation bar, display
 * cutouts and the keyboard. targetSdk 35+ is always edge-to-edge on Android 15+,
 * which also makes {@code adjustResize} a no-op, so the IME is handled here too.
 *
 * <p>System bars become padding (the background still draws behind them). The
 * keyboard height becomes a bottom <em>margin</em> (with no bottom padding), so
 * the root really shrinks like it did under adjustResize. Padding alone isn't enough:
 * ScrollView ignores its bottom padding when it scrolls a focused child into view
 * (QA-0-1). After that layout pass, the focused view is asked to come fully on screen.
 */
public final class SystemBarInsets {

    private SystemBarInsets() {
    }

    public static void apply(@NonNull View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            int imeBottom = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            // While the keyboard is up it covers the nav bar, so the whole IME height
            // becomes margin and there is no bottom padding left for ScrollView to ignore.
            boolean imeUp = imeBottom > bars.bottom;
            int bottomMargin = imeUp ? imeBottom : 0;

            v.setPadding(bars.left, bars.top, bars.right, imeUp ? 0 : bars.bottom);
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                if (mlp.bottomMargin != bottomMargin) {
                    mlp.bottomMargin = bottomMargin;
                    v.setLayoutParams(mlp);
                }
            } else {
                // No margin support: fall back to padding (still scrollable by hand).
                v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, imeBottom));
            }

            if (imeUp) {
                revealFocusedAfterLayout(v);
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private static void revealFocusedAfterLayout(View root) {
        root.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        View focused = root.findFocus();
                        if (focused != null) {
                            Rect r = new Rect();
                            focused.getDrawingRect(r);
                            focused.requestRectangleOnScreen(r, false);
                        }
                    }
                });
    }
}
