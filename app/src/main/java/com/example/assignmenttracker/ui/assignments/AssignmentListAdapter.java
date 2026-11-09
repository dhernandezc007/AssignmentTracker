package com.example.assignmenttracker.ui.assignments;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assignmenttracker.R;
import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.databinding.ItemAssignmentBinding;
import com.example.assignmenttracker.databinding.ItemCaughtUpBinding;
import com.example.assignmenttracker.databinding.ItemDoneHeaderBinding;
import com.example.assignmenttracker.databinding.ItemEmptyBinding;
import com.example.assignmenttracker.databinding.ItemSectionHeaderBinding;
import com.example.assignmenttracker.databinding.ItemSummaryBinding;
import com.example.assignmenttracker.util.DateFormats;
import com.example.assignmenttracker.util.DueBucket;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * One flat list with heterogeneous items (design.md §2.1): SUMMARY, CAUGHT_UP, SECTION_HEADER,
 * ROW, DONE_HEADER and EMPTY.
 */
public class AssignmentListAdapter extends ListAdapter<HomeItem, RecyclerView.ViewHolder> {

    /** What the home screen does with taps, checks and deletes. */
    public interface Listener {
        /** Tap on a row body: open it in Add/Edit. */
        void onRowClick(@NonNull Assignment assignment);

        /** The checkbox was toggled to {@code done}. */
        void onToggleDone(@NonNull Assignment assignment, boolean done);

        /** Swipe or the TalkBack "Delete" action. */
        void onDelete(@NonNull Assignment assignment);

        /** Tap on the Done header. */
        void onToggleDoneSection();
    }

    /** Marks an in-place change, so the item animator reuses the holder (no cross-fade). */
    private static final Object CHANGED_IN_PLACE = new Object();

    private static final long CHEVRON_MS = 200; // design.md §9

    /** The character an inline icon span replaces (never spoken: the row body cd replaces the text). */
    private static final String ICON_PLACEHOLDER = "￼";

    /** design.md §3.1: the overdue icon is textSize × 1.15 tall. */
    private static final float ICON_SCALE = 1.15f;

    private static final DiffUtil.ItemCallback<HomeItem> DIFF = new DiffUtil.ItemCallback<HomeItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull HomeItem a, @NonNull HomeItem b) {
            return a.sameItemAs(b);
        }

        @Override
        public boolean areContentsTheSame(@NonNull HomeItem a, @NonNull HomeItem b) {
            return a.sameContentAs(b);
        }

        @Nullable
        @Override
        public Object getChangePayload(@NonNull HomeItem oldItem, @NonNull HomeItem newItem) {
            return CHANGED_IN_PLACE;
        }
    };

    private final Listener listener;

    public AssignmentListAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getType();
    }

    /** The assignment at {@code position}, or null for a non-row item or a stale position. */
    @Nullable
    public Assignment assignmentAt(int position) {
        if (position < 0 || position >= getItemCount()) return null;
        return getItem(position).getAssignment();
    }

    /** The card position of the row at {@code position} (for the swipe background's corners). */
    @NonNull
    RowPosition rowPositionAt(int position) {
        if (position < 0 || position >= getItemCount()) return RowPosition.SINGLE;
        return getItem(position).getPosition();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case HomeItem.TYPE_EMPTY:
                return new PlainHolder(ItemEmptyBinding.inflate(inflater, parent, false).getRoot());
            case HomeItem.TYPE_SUMMARY:
                return new SummaryHolder(ItemSummaryBinding.inflate(inflater, parent, false));
            case HomeItem.TYPE_CAUGHT_UP:
                return new CaughtUpHolder(ItemCaughtUpBinding.inflate(inflater, parent, false));
            case HomeItem.TYPE_SECTION_HEADER:
                return new SectionHeaderHolder(ItemSectionHeaderBinding.inflate(inflater, parent, false));
            case HomeItem.TYPE_DONE_HEADER: {
                DoneHeaderHolder h = new DoneHeaderHolder(ItemDoneHeaderBinding.inflate(inflater, parent, false));
                h.itemView.setOnClickListener(v -> listener.onToggleDoneSection());
                return h;
            }
            default:
                return createRow(inflater, parent);
        }
    }

    @NonNull
    private RowHolder createRow(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        RowHolder h = new RowHolder(ItemAssignmentBinding.inflate(inflater, parent, false));
        View body = h.b.rowBody;
        body.setOnClickListener(v -> {
            Assignment a = assignmentAt(h.getBindingAdapterPosition());
            if (a != null) listener.onRowClick(a);
        });
        // A click (not a checked-change listener) so binding a row never fires it.
        h.b.rowCheck.setOnClickListener(v -> {
            Assignment a = assignmentAt(h.getBindingAdapterPosition());
            if (a != null) listener.onToggleDone(a, h.b.rowCheck.isChecked());
        });
        Context ctx = parent.getContext();
        // TalkBack: "Double-tap to edit" (design.md §10) instead of "to activate".
        ViewCompat.replaceAccessibilityAction(body, AccessibilityActionCompat.ACTION_CLICK,
                ctx.getString(R.string.action_edit), (view, args) -> view.performClick());
        // TalkBack users can't swipe: the same delete as a custom action (design.md §4).
        ViewCompat.addAccessibilityAction(body, ctx.getString(R.string.action_delete), (view, args) -> {
            Assignment a = assignmentAt(h.getBindingAdapterPosition());
            if (a == null) return false;
            listener.onDelete(a);
            return true;
        });
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        onBindViewHolder(holder, position, java.util.Collections.emptyList());
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position,
                                 @NonNull List<Object> payloads) {
        HomeItem item = getItem(position);
        if (holder instanceof RowHolder) {
            ((RowHolder) holder).bind(item);
        } else if (holder instanceof DoneHeaderHolder) {
            ((DoneHeaderHolder) holder).bind(item, !payloads.isEmpty());
        } else if (holder instanceof SectionHeaderHolder) {
            ((SectionHeaderHolder) holder).bind(item);
        } else if (holder instanceof SummaryHolder) {
            ((SummaryHolder) holder).bind(item);
        } else if (holder instanceof CaughtUpHolder) {
            ((CaughtUpHolder) holder).bind(item);
        }
    }

    @DrawableRes
    static int backgroundFor(@NonNull RowPosition p) {
        switch (p) {
            case TOP:
                return R.drawable.bg_row_top;
            case MIDDLE:
                return R.drawable.bg_row_middle;
            case BOTTOM:
                return R.drawable.bg_row_bottom;
            case SINGLE:
            default:
                return R.drawable.bg_row_single;
        }
    }

    @StringRes
    static int sectionName(@NonNull DueBucket b) {
        switch (b) {
            case OVERDUE:
                return R.string.section_overdue;
            case TODAY:
                return R.string.section_today;
            case THIS_WEEK:
                return R.string.section_this_week;
            case LATER:
                return R.string.section_later;
            case NO_DATE:
            default:
                return R.string.section_no_date;
        }
    }

    private static int attrColor(@NonNull View v, int attr) {
        return MaterialColors.getColor(v, attr);
    }

    private static void append(@NonNull SpannableStringBuilder sb, @NonNull CharSequence text, Object... spans) {
        int start = sb.length();
        sb.append(text);
        for (Object s : spans) sb.setSpan(s, start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    static final class RowHolder extends RecyclerView.ViewHolder {
        final ItemAssignmentBinding b;

        RowHolder(ItemAssignmentBinding b) {
            super(b.getRoot());
            this.b = b;
            // DES-1-4: clip the body's ripple to the bg_row_* corners. The XML attribute only
            // applies on API 31+, the setter works from API 21.
            b.getRoot().setClipToOutline(true);
        }

        void bind(HomeItem item) {
            Assignment a = item.getAssignment();
            if (a == null) return;
            Context ctx = itemView.getContext();
            boolean done = a.isDone();
            b.rowTitle.setText(a.title);
            // Done: muted title, no strikethrough; the green checked box marks the state (§3.1).
            b.rowTitle.setTextColor(attrColor(b.rowTitle,
                    done ? com.google.android.material.R.attr.colorOnSurfaceVariant
                            : com.google.android.material.R.attr.colorOnSurface));
            b.rowCheck.setChecked(done);
            b.rowCheck.setContentDescription(ctx.getString(R.string.checkbox_cd, a.title));

            // Line 2 (§3.1): due label first (only its span is red or pink), then the course.
            DueLabel label = DueLabel.of(item);
            String dueText = null;
            String dueSpoken = null;
            Object[] dueSpans = new Object[0];
            if (label != DueLabel.NONE && a.dueAt != null) {
                long now = System.currentTimeMillis();
                Locale locale = ctx.getResources().getConfiguration().getLocales().get(0);
                TimeZone tz = TimeZone.getDefault();
                switch (label) {
                    case OVERDUE:
                        dueText = ctx.getString(R.string.due_overdue, DateFormats.visible(a.dueAt, now, locale, tz));
                        dueSpoken = ctx.getString(R.string.due_overdue_spoken, DateFormats.spoken(a.dueAt, now, locale, tz));
                        dueSpans = new Object[]{new StyleSpan(Typeface.BOLD), new ForegroundColorSpan(
                                attrColor(b.rowMeta, androidx.appcompat.R.attr.colorError))};
                        break;
                    case TODAY:
                        dueText = dueSpoken = ctx.getString(R.string.due_today);
                        dueSpans = new Object[]{new TypefaceSpan("sans-serif-medium"),
                                new ForegroundColorSpan(ContextCompat.getColor(ctx, R.color.mx_pink_deep))};
                        break;
                    case TOMORROW:
                        dueText = dueSpoken = ctx.getString(R.string.due_tomorrow);
                        break;
                    default:
                        dueText = DateFormats.visible(a.dueAt, now, locale, tz);
                        dueSpoken = DateFormats.spoken(a.dueAt, now, locale, tz);
                        break;
                }
            }
            SpannableStringBuilder meta = new SpannableStringBuilder();
            // DES-4-1: the overdue icon is a glyph on line 1, so a wrapped line 2 returns to the
            // text edge (52dp) instead of hanging under a compound drawable.
            ImageSpan icon = label == DueLabel.OVERDUE ? overdueIconSpan() : null;
            if (icon != null) append(meta, ICON_PLACEHOLDER, icon);
            if (dueText != null) append(meta, dueText, dueSpans);
            if (a.course != null && !a.course.isEmpty()) {
                if (meta.length() > 0) meta.append(ctx.getString(R.string.meta_separator));
                meta.append(a.course);
            }
            boolean hasMeta = meta.length() > 0;
            b.rowMeta.setText(hasMeta ? meta : null);
            b.rowMeta.setVisibility(hasMeta ? View.VISIBLE : View.GONE);
            b.rowBody.setMinimumHeight(itemView.getResources().getDimensionPixelSize(
                    hasMeta ? R.dimen.row_min_two_line : R.dimen.row_min_one_line));
            // §10: title, then course, then the spoken due label, as one node.
            List<String> spoken = new ArrayList<>(3);
            spoken.add(a.title);
            if (a.course != null && !a.course.isEmpty()) spoken.add(a.course);
            if (dueSpoken != null) spoken.add(dueSpoken);
            b.rowBody.setContentDescription(android.text.TextUtils.join(
                    ctx.getString(R.string.summary_separator_spoken), spoken));

            RowPosition p = item.getPosition();
            b.getRoot().setBackgroundResource(backgroundFor(p));
            // The rule sits under every row except the last one in the card.
            boolean last = p == RowPosition.BOTTOM || p == RowPosition.SINGLE;
            b.rowRule.setVisibility(last ? View.GONE : View.VISIBLE);
        }

        /**
         * design.md §3.1 (DES-4-1): the (!) icon, red, textSize × 1.15 square, then a 4dp gap
         * (as an end inset, so it follows the layout direction). Centered on line 1 from API 29,
         * on the baseline below that.
         */
        @Nullable
        private ImageSpan overdueIconSpan() {
            Drawable d = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_overdue);
            if (d == null) return null;
            d = d.mutate();
            d.setTint(attrColor(b.rowMeta, androidx.appcompat.R.attr.colorError));
            int size = Math.round(b.rowMeta.getTextSize() * ICON_SCALE);
            int gap = itemView.getResources().getDimensionPixelSize(R.dimen.overdue_icon_gap);
            boolean rtl = b.rowMeta.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            Drawable inset = new InsetDrawable(d, rtl ? gap : 0, 0, rtl ? 0 : gap, 0);
            inset.setBounds(0, 0, size + gap, size);
            int align = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ? DynamicDrawableSpan.ALIGN_CENTER : DynamicDrawableSpan.ALIGN_BASELINE;
            return new ImageSpan(inset, align);
        }
    }

    static final class SectionHeaderHolder extends RecyclerView.ViewHolder {
        private final ItemSectionHeaderBinding b;

        SectionHeaderHolder(ItemSectionHeaderBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(HomeItem item) {
            DueBucket bucket = item.getBucket();
            if (bucket == null) return;
            Context ctx = itemView.getContext();
            String name = ctx.getString(sectionName(bucket));
            int count = item.getCount();
            b.sectionLabel.setText(name);
            b.sectionCount.setText(String.valueOf(count));
            itemView.setContentDescription(ctx.getResources().getQuantityString(
                    R.plurals.section_header_cd, count, name, count));
            ViewCompat.setAccessibilityHeading(itemView, true);
        }
    }

    static final class SummaryHolder extends RecyclerView.ViewHolder {
        private final ItemSummaryBinding b;

        SummaryHolder(ItemSummaryBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        /** §3.4: overdue in red bold, due today in pink-deep, the rest colorOnBackground. */
        void bind(HomeItem item) {
            HomeSummary s = item.getSummary();
            if (s == null) return;
            Context ctx = itemView.getContext();
            String sep = ctx.getString(R.string.summary_separator);
            SpannableStringBuilder sb = new SpannableStringBuilder();
            for (HomeSummary.Part p : s.parts()) {
                if (sb.length() > 0) sb.append(sep);
                switch (p.kind) {
                    case OVERDUE:
                        append(sb, plural(ctx, R.plurals.summary_overdue, p.count), new StyleSpan(Typeface.BOLD),
                                new ForegroundColorSpan(attrColor(b.summary, androidx.appcompat.R.attr.colorError)));
                        break;
                    case TODAY:
                        append(sb, plural(ctx, R.plurals.summary_due_today, p.count),
                                new ForegroundColorSpan(ContextCompat.getColor(ctx, R.color.mx_pink_deep)));
                        break;
                    case WEEK:
                        sb.append(plural(ctx, R.plurals.summary_due_week, p.count));
                        break;
                    default:
                        sb.append(plural(ctx, R.plurals.summary_nothing_this_week, p.count));
                        break;
                }
            }
            b.summary.setText(sb);
            // §10: a pause, not "middle dot", between the parts.
            b.summary.setContentDescription(sb.toString().replace(sep, ctx.getString(R.string.summary_separator_spoken)));
        }

        private static String plural(Context ctx, int id, int n) {
            return ctx.getResources().getQuantityString(id, n, n);
        }
    }

    static final class CaughtUpHolder extends RecyclerView.ViewHolder {
        private final ItemCaughtUpBinding b;

        CaughtUpHolder(ItemCaughtUpBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(HomeItem item) {
            int n = item.getCount();
            b.caughtUpBody.setText(itemView.getResources().getQuantityString(R.plurals.caught_up_body, n, n));
        }
    }

    static final class DoneHeaderHolder extends RecyclerView.ViewHolder {
        private final ItemDoneHeaderBinding b;

        DoneHeaderHolder(ItemDoneHeaderBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        /** {@code inPlace}: the same header changed (an expand or a new count), so animate the chevron. */
        void bind(HomeItem item, boolean inPlace) {
            int count = item.getDoneCount();
            boolean expanded = item.isExpanded();
            b.doneCount.setText(String.valueOf(count));
            itemView.setContentDescription(itemView.getContext().getString(
                    expanded ? R.string.done_header_cd_expanded : R.string.done_header_cd_collapsed, count));
            ViewCompat.setStateDescription(itemView, itemView.getContext().getString(
                    expanded ? R.string.state_expanded : R.string.state_collapsed));
            ViewCompat.setAccessibilityHeading(itemView, true);

            float target = expanded ? 180f : 0f;
            b.doneChevron.animate().cancel();
            if (inPlace && animatorsEnabled() && b.doneChevron.getRotation() != target) {
                b.doneChevron.animate().rotation(target).setDuration(CHEVRON_MS)
                        .setInterpolator(new android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f)) // FastOutSlowIn
                        .start();
            } else {
                b.doneChevron.setRotation(target);
            }
        }

        /** Reduced motion (design.md §9): with animations removed, jump to the end state. */
        private static boolean animatorsEnabled() {
            return Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled();
        }
    }

    static final class PlainHolder extends RecyclerView.ViewHolder {
        PlainHolder(View v) {
            super(v);
        }
    }
}
