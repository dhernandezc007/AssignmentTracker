package com.example.assignmenttracker.ui.assignments;

import android.animation.ValueAnimator;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assignmenttracker.R;
import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.databinding.ItemAssignmentBinding;
import com.example.assignmenttracker.databinding.ItemDoneHeaderBinding;
import com.example.assignmenttracker.databinding.ItemEmptyBinding;
import com.google.android.material.color.MaterialColors;

import java.util.List;

/** One flat list with heterogeneous items (design.md §2). Slice 3: EMPTY, ROW and DONE_HEADER. */
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
        if (viewType == HomeItem.TYPE_EMPTY) {
            return new EmptyHolder(ItemEmptyBinding.inflate(inflater, parent, false));
        }
        if (viewType == HomeItem.TYPE_DONE_HEADER) {
            DoneHeaderHolder h = new DoneHeaderHolder(ItemDoneHeaderBinding.inflate(inflater, parent, false));
            h.itemView.setOnClickListener(v -> listener.onToggleDoneSection());
            return h;
        }
        RowHolder h = new RowHolder(ItemAssignmentBinding.inflate(inflater, parent, false));
        h.itemView.setOnClickListener(v -> {
            Assignment a = assignmentAt(h.getBindingAdapterPosition());
            if (a != null) listener.onRowClick(a);
        });
        // A click (not a checked-change listener) so binding a row never fires it.
        h.b.rowCheck.setOnClickListener(v -> {
            Assignment a = assignmentAt(h.getBindingAdapterPosition());
            if (a != null) listener.onToggleDone(a, h.b.rowCheck.isChecked());
        });
        // TalkBack: "Double-tap to edit" (design.md §10) instead of "to activate".
        ViewCompat.replaceAccessibilityAction(h.itemView, AccessibilityActionCompat.ACTION_CLICK,
                parent.getContext().getString(R.string.action_edit), (view, args) -> view.performClick());
        // TalkBack users can't swipe: the same delete as a custom action (design.md §4).
        ViewCompat.addAccessibilityAction(h.itemView, parent.getContext().getString(R.string.action_delete),
                (view, args) -> {
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

    static final class RowHolder extends RecyclerView.ViewHolder {
        final ItemAssignmentBinding b;

        RowHolder(ItemAssignmentBinding b) {
            super(b.getRoot());
            this.b = b;
            // DES-1-4: clip the ripple to the bg_row_* corners. The XML attribute only
            // applies on API 31+, the setter works from API 21.
            b.getRoot().setClipToOutline(true);
        }

        void bind(HomeItem item) {
            Assignment a = item.getAssignment();
            if (a == null) return;
            boolean done = a.isDone();
            b.rowTitle.setText(a.title);
            // Done: muted title, no strikethrough; the green checked box marks the state (§3.1).
            b.rowTitle.setTextColor(MaterialColors.getColor(b.rowTitle,
                    done ? com.google.android.material.R.attr.colorOnSurfaceVariant
                            : com.google.android.material.R.attr.colorOnSurface));
            b.rowCheck.setChecked(done);
            b.rowCheck.setContentDescription(itemView.getContext().getString(R.string.checkbox_cd, a.title));

            String meta = RowMeta.text(a, itemView.getContext().getString(R.string.meta_separator));
            b.rowMeta.setText(meta);
            b.rowMeta.setVisibility(meta == null ? View.GONE : View.VISIBLE);
            b.rowBody.setMinimumHeight(itemView.getResources().getDimensionPixelSize(
                    meta == null ? R.dimen.row_min_one_line : R.dimen.row_min_two_line));

            RowPosition p = item.getPosition();
            b.getRoot().setBackgroundResource(backgroundFor(p));
            // The rule sits under every row except the last one in the card.
            boolean last = p == RowPosition.BOTTOM || p == RowPosition.SINGLE;
            b.rowRule.setVisibility(last ? View.GONE : View.VISIBLE);
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

    static final class EmptyHolder extends RecyclerView.ViewHolder {
        EmptyHolder(ItemEmptyBinding b) {
            super(b.getRoot());
        }
    }
}
