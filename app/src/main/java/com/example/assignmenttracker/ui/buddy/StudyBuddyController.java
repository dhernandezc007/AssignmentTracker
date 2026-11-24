package com.example.assignmenttracker.ui.buddy;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;

import com.example.assignmenttracker.R;
import com.example.assignmenttracker.ui.assignments.HomeItem;
import com.example.assignmenttracker.ui.fiesta.Motion;

import java.util.List;
import java.util.Random;

/**
 * The study buddy strip at the bottom of Home: abuelita or abuelo, and what they're saying.
 * Tap the avatar to switch between them (remembered), tap the bubble to hear something else.
 * The line follows the home list's mood. After a mark done they cheer for a few seconds, then
 * go back to the mood. The bubble is not a live region: the Undo snackbar stays the only thing
 * spoken on mark done (DES-3-3).
 */
public final class StudyBuddyController {

    private static final String PREFS = "study_buddy";
    private static final String KEY_WHO = "who";
    private static final long CHEER_MS = 4500;

    private final ImageView avatar;
    private final TextView bubble;
    private final SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final Runnable endCheer = this::endCheer;

    @NonNull private StudyBuddy.Who who;
    @Nullable private StudyBuddy.Mood mood;
    @Nullable private StudyBuddy.Mood shown;
    private int lastIndex = -1;
    private boolean cheering;

    public StudyBuddyController(@NonNull ImageView avatar, @NonNull TextView bubble) {
        this.avatar = avatar;
        this.bubble = bubble;
        this.prefs = avatar.getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.who = StudyBuddy.Who.parse(prefs.getString(KEY_WHO, null));
        bindWho();
        avatar.setOnClickListener(v -> switchWho());
        bubble.setOnClickListener(v -> say(shown != null ? shown : currentMood(), true));
        ViewCompat.replaceAccessibilityAction(bubble, AccessibilityActionCompat.ACTION_CLICK,
                res().getString(R.string.buddy_action_next), null);
    }

    /** The home list changed. A new mood gets a new line, unless they're mid-cheer. */
    public void onHomeItems(@Nullable List<HomeItem> items) {
        StudyBuddy.Mood next = StudyBuddy.moodFor(items);
        if (next == mood) return;
        mood = next;
        if (!cheering) say(next, false);
    }

    /** An assignment was just marked done: ¡a celebrar! */
    public void celebrate() {
        cheer(StudyBuddy.Mood.CELEBRATE);
    }

    /** That mark done earned a milpa seed: this line wins over the plain cheer. */
    public void announceSeed() {
        cheer(StudyBuddy.Mood.SEED);
    }

    private void cheer(@NonNull StudyBuddy.Mood m) {
        cheering = true;
        say(m, false);
        bounce();
        handler.removeCallbacks(endCheer);
        handler.postDelayed(endCheer, CHEER_MS);
    }

    /** Call from onDestroy. */
    public void release() {
        handler.removeCallbacks(endCheer);
        avatar.animate().cancel();
    }

    private void endCheer() {
        cheering = false;
        say(currentMood(), false);
    }

    @NonNull
    private StudyBuddy.Mood currentMood() {
        return mood != null ? mood : StudyBuddy.Mood.STEADY;
    }

    private void switchWho() {
        who = who.other();
        prefs.edit().putString(KEY_WHO, who.name()).apply();
        bindWho();
        bounce();
    }

    private void bindWho() {
        boolean abuelita = who == StudyBuddy.Who.ABUELITA;
        avatar.setImageResource(abuelita ? R.drawable.buddy_abuelita : R.drawable.buddy_abuelo);
        avatar.setContentDescription(res().getString(abuelita ? R.string.buddy_cd_abuelita : R.string.buddy_cd_abuelo));
        // TalkBack: "Double-tap to switch to Abuelo".
        ViewCompat.replaceAccessibilityAction(avatar, AccessibilityActionCompat.ACTION_CLICK,
                res().getString(abuelita ? R.string.buddy_action_switch_to_abuelo
                        : R.string.buddy_action_switch_to_abuelita), null);
    }

    /** Shows a line for {@code m}. Same mood again (a tap) picks a different line. */
    private void say(@NonNull StudyBuddy.Mood m, boolean fresh) {
        String[] lines = res().getStringArray(linesFor(m));
        int avoid = (m == shown || fresh) ? lastIndex : -1;
        lastIndex = StudyBuddy.pick(lines.length, avoid, random);
        shown = m;
        bubble.setText(lines[lastIndex]);
    }

    @ArrayRes
    private static int linesFor(@NonNull StudyBuddy.Mood m) {
        switch (m) {
            case EMPTY:
                return R.array.buddy_lines_empty;
            case CAUGHT_UP:
                return R.array.buddy_lines_caught_up;
            case OVERDUE:
                return R.array.buddy_lines_overdue;
            case DUE_TODAY:
                return R.array.buddy_lines_today;
            case CELEBRATE:
                return R.array.buddy_lines_celebrate;
            case SEED:
                return R.array.buddy_lines_seed;
            default:
                return R.array.buddy_lines_steady;
        }
    }

    /** A happy little hop. Skipped when animations are off. */
    private void bounce() {
        if (!Motion.enabled(avatar.getContext())) return;
        float lift = -10 * res().getDisplayMetrics().density;
        avatar.animate().cancel();
        avatar.setPivotY(avatar.getHeight());
        avatar.animate()
                .scaleX(1.12f).scaleY(1.12f).translationY(lift).rotation(-8)
                .setDuration(160)
                .withEndAction(() -> avatar.animate()
                        .scaleX(1f).scaleY(1f).translationY(0).rotation(0)
                        .setInterpolator(new OvershootInterpolator(3f))
                        .setDuration(420)
                        .start())
                .start();
    }

    @NonNull
    private Resources res() {
        return avatar.getResources();
    }
}
