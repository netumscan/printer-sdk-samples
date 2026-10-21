// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/** 仅渲染应用提交历史；不调度请求，也不推断 SDK 队列或硬件状态。 */
final class DemoPrintRequestsView {
    private final Activity activity;
    private final DemoModel model;
    private final LinearLayout rows;
    private List<DemoUiState.PrintRequest> rendered;
    private boolean cancellable;

    DemoPrintRequestsView(Activity activity, DemoModel model) {
        this.activity = activity; this.model = model;
        rows = activity.findViewById(R.id.print_requests);
    }

    void render(DemoUiState state) {
        boolean enabled = state.foreground && state.phase != DemoUiState.Phase.DISCONNECTING && state.phase != DemoUiState.Phase.CLOSED;
        if (rendered != null && rendered.equals(state.printRequests) && enabled == cancellable) return;
        rendered = state.printRequests; cancellable = enabled;
        activity.findViewById(R.id.print_requests_section).setVisibility(rendered.isEmpty() ? View.GONE : View.VISIBLE);
        rows.removeAllViews();
        for (DemoUiState.PrintRequest request : rendered) {
            LinearLayout row = new LinearLayout(activity); row.setOrientation(LinearLayout.VERTICAL);
            TextView heading = new TextView(activity);
            heading.setText(activity.getString(R.string.print_request_heading, request.sequence, request.requestId, request.description));
            row.addView(heading);
            TextView result = new TextView(activity); result.setTextIsSelectable(true);
            result.setText(request.result != null ? DemoFormatter.summary(request.result) : activity.getString(
                request.cancelling ? R.string.print_request_cancelling : R.string.print_request_waiting));
            row.addView(result);
            if (request.result == null) {
                Button cancel = new Button(activity);
                cancel.setText(activity.getString(R.string.cancel_print_request, request.sequence));
                cancel.setTag(request.requestId); cancel.setEnabled(enabled && !request.cancelling);
                cancel.setOnClickListener(view -> model.cancel(request.requestId));
                row.addView(cancel);
            }
            rows.addView(row);
        }
    }
}
