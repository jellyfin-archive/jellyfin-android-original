package com.mb.android.media;

public interface SwipeDragHelperAdapter {

    void onItemMove(int fromPosition, int toPosition);

    void onItemDismiss(int position);
}
