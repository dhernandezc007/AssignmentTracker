package com.example.assignmenttracker.ui.assignments;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assignmenttracker.R;
import com.example.assignmenttracker.data.entity.Assignment;
import com.google.android.material.color.MaterialColors;

/**
 * Swipe either way to delete a row (design.md §4). Only rows swipe. While swiping, the gap
 * behind the row is filled with {@code colorError} and shows the white trash icon and "Delete"
 * 20dp in from the side being revealed, clipped to the row's card corners. The row slides;
 * nothing fades.
 */
public final class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    private final AssignmentListAdapter adapter;
    private final AssignmentListAdapter.Listener listener;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint label = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Drawable icon;
    private final String deleteText;
    private final float radius;
    private final int inset;
    private final int gap;
    private final Path clip = new Path();
    private final RectF bounds = new RectF();

    public SwipeToDeleteCallback(@NonNull Context context, @NonNull AssignmentListAdapter adapter,
                                 @NonNull AssignmentListAdapter.Listener listener, @NonNull View themed) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        this.listener = listener;
        int white = ContextCompat.getColor(context, R.color.mx_white);
        fill.setColor(MaterialColors.getColor(themed, androidx.appcompat.R.attr.colorError));
        label.setColor(white);
        label.setTextSize(context.getResources().getDimension(R.dimen.swipe_label_size));
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        Drawable d = ContextCompat.getDrawable(context, R.drawable.ic_delete);
        if (d == null) throw new IllegalStateException("ic_delete missing");
        icon = d.mutate();
        icon.setTint(white);
        deleteText = context.getString(R.string.action_delete);
        radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12,
                context.getResources().getDisplayMetrics());
        inset = context.getResources().getDimensionPixelSize(R.dimen.swipe_content_inset);
        gap = context.getResources().getDimensionPixelSize(R.dimen.swipe_icon_gap);
    }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
        return vh.getItemViewType() == HomeItem.TYPE_ROW ? super.getSwipeDirs(rv, vh) : 0;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder from,
                          @NonNull RecyclerView.ViewHolder to) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
        Assignment a = adapter.assignmentAt(vh.getBindingAdapterPosition());
        if (a != null) listener.onDelete(a);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View row = vh.itemView;
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX != 0f) {
            RowPosition p = adapter.rowPositionAt(vh.getBindingAdapterPosition());
            boolean roundTop = p == RowPosition.TOP || p == RowPosition.SINGLE;
            boolean roundBottom = p == RowPosition.BOTTOM || p == RowPosition.SINGLE;
            float t = roundTop ? radius : 0f;
            float b = roundBottom ? radius : 0f;
            bounds.set(row.getLeft(), row.getTop(), row.getRight(), row.getBottom());
            clip.reset();
            clip.addRoundRect(bounds, new float[]{t, t, t, t, b, b, b, b}, Path.Direction.CW);

            int save = c.save();
            c.clipPath(clip);
            c.drawRect(bounds, fill);

            int iw = icon.getIntrinsicWidth();
            int ih = icon.getIntrinsicHeight();
            int iconTop = (int) (bounds.centerY() - ih / 2f);
            float textW = label.measureText(deleteText);
            float baseline = bounds.centerY() - (label.descent() + label.ascent()) / 2f;
            if (dX > 0) { // revealing the left side
                int left = row.getLeft() + inset;
                icon.setBounds(left, iconTop, left + iw, iconTop + ih);
                icon.draw(c);
                c.drawText(deleteText, left + iw + gap, baseline, label);
            } else { // revealing the right side
                int right = row.getRight() - inset;
                icon.setBounds(right - iw, iconTop, right, iconTop + ih);
                icon.draw(c);
                c.drawText(deleteText, right - iw - gap - textW, baseline, label);
            }
            c.restoreToCount(save);
        }
        super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive);
    }
}
