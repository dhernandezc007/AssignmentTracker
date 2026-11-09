package com.example.assignmenttracker.ui.assignments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assignmenttracker.R;
import com.example.assignmenttracker.data.entity.Assignment;
import com.example.assignmenttracker.databinding.ItemAssignmentBinding;
import com.example.assignmenttracker.databinding.ItemEmptyBinding;

/** One flat list with heterogeneous items (design.md §2). Slices 1–2: EMPTY and ROW. */
public class AssignmentListAdapter extends ListAdapter<HomeItem, RecyclerView.ViewHolder> {

    /** Tap on a row: open it in Add/Edit. */
    public interface OnRowClick {
        void onRowClick(@NonNull Assignment assignment);
    }

    private static final DiffUtil.ItemCallback<HomeItem> DIFF = new DiffUtil.ItemCallback<HomeItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull HomeItem a, @NonNull HomeItem b) {
            return a.sameItemAs(b);
        }

        @Override
        public boolean areContentsTheSame(@NonNull HomeItem a, @NonNull HomeItem b) {
            return a.sameContentAs(b);
        }
    };

    private final OnRowClick onRowClick;

    public AssignmentListAdapter(@NonNull OnRowClick onRowClick) {
        super(DIFF);
        this.onRowClick = onRowClick;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == HomeItem.TYPE_EMPTY) {
            return new EmptyHolder(ItemEmptyBinding.inflate(inflater, parent, false));
        }
        RowHolder h = new RowHolder(ItemAssignmentBinding.inflate(inflater, parent, false));
        h.itemView.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Assignment a = getItem(pos).getAssignment();
            if (a != null) onRowClick.onRowClick(a);
        });
        // TalkBack: "Double-tap to edit" (design.md §10) instead of "to activate".
        ViewCompat.replaceAccessibilityAction(h.itemView, AccessibilityActionCompat.ACTION_CLICK,
                parent.getContext().getString(R.string.action_edit), (view, args) -> view.performClick());
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof RowHolder) {
            ((RowHolder) holder).bind(getItem(position));
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
        private final ItemAssignmentBinding b;

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
            b.rowTitle.setText(a.title);
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

    static final class EmptyHolder extends RecyclerView.ViewHolder {
        EmptyHolder(ItemEmptyBinding b) {
            super(b.getRoot());
        }
    }
}
