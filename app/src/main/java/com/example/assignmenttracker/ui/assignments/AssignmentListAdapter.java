package com.example.assignmenttracker.ui.assignments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assignmenttracker.R;
import com.example.assignmenttracker.databinding.ItemAssignmentBinding;
import com.example.assignmenttracker.databinding.ItemEmptyBinding;

/** One flat list with heterogeneous items (design.md §2). Slice 1: EMPTY and ROW. */
public class AssignmentListAdapter extends ListAdapter<HomeItem, RecyclerView.ViewHolder> {

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

    public AssignmentListAdapter() {
        super(DIFF);
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
        return new RowHolder(ItemAssignmentBinding.inflate(inflater, parent, false));
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
        }

        void bind(HomeItem item) {
            //noinspection ConstantConditions (rows always carry an assignment)
            b.rowTitle.setText(item.getAssignment().title);
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
