// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/** Responsive presentation only; no parameter or session changes. */
final class CanvasLayout {
    private CanvasLayout() { }

    static void apply(View root, int contentWidthDp, float fontScale, boolean receipt) {
        boolean paired = contentWidthDp >= 320 && fontScale <= 1.2f;
        int gap = Math.round(12 * root.getResources().getDisplayMetrics().density);
        row(root, R.id.canvas_size_row, R.id.canvas_width_field, R.id.canvas_height_field, paired, gap);
        // 份数留在主流程；横向起点归打印高级设置，不跨容器强制配成两列。
        field(root.findViewById(R.id.canvas_copies_field), false, 0);
        field(root.findViewById(R.id.canvas_origin_field), false, 0);
        root.findViewById(R.id.canvas_origin_field).setVisibility(View.GONE);
        root.findViewById(R.id.origin_help).setVisibility(View.GONE);
        View copies = root.findViewById(R.id.copies);
        boolean advanced = root.findViewById(R.id.print_advanced).getVisibility() == View.VISIBLE;
        int nextField = advanced ? receipt ? R.id.trailing_blank : R.id.origin_mode : R.id.print;
        if (copies.getNextFocusForwardId() != nextField) copies.setNextFocusForwardId(nextField);
    }

    private static void row(View root, int rowId, int firstId, int secondId, boolean paired, int gap) {
        LinearLayout row = root.findViewById(rowId);
        int orientation = paired ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL;
        if (row.getOrientation() != orientation) row.setOrientation(orientation);
        field(root.findViewById(firstId), paired, paired ? gap : 0);
        field(root.findViewById(secondId), paired, 0);
    }

    private static void field(View view, boolean paired, int endMargin) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)view.getLayoutParams();
        int width = paired ? 0 : ViewGroup.LayoutParams.MATCH_PARENT;
        float weight = paired ? 1 : 0;
        if (params.width == width && params.height == ViewGroup.LayoutParams.WRAP_CONTENT &&
                params.weight == weight && params.getMarginEnd() == endMargin) return;
        params.width = width;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.weight = weight;
        params.setMarginEnd(endMargin);
        view.setLayoutParams(params);
    }
}
