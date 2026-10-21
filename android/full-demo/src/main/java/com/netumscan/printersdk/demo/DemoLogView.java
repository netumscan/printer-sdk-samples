// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.app.Activity;
import android.text.Layout;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.TimeZone;

/** 日志页的纯显示逻辑：格式切换、滚动与位置保留；不发起 SDK 请求或文件操作。 */
final class DemoLogView implements AutoCloseable {
    private final Activity activity;
    private final Collection<String> lines;
    private final ScrollView scroll;
    private final TextView text;
    private final CheckBox details;
    private final View latest;
    private final ViewTreeObserver.OnScrollChangedListener scrollListener;
    private boolean started, degraded, following = true, rendering, needsPosition = true;
    private long renderGeneration, interactionGeneration;
    private final ArrayList<String> renderedEntries = new ArrayList<>();
    private int[] renderedStarts = new int[0];
    private String displayed = "";
    private Anchor pendingAnchor;
    private ViewTreeObserver positionObserver;
    private ViewTreeObserver.OnPreDrawListener positionListener;

    private static final class Anchor {
        final String entry;
        final int offset;
        Anchor(String entry, int offset) { this.entry = entry; this.offset = offset; }
    }

    // 只观察触摸且始终返回 false；点击、滚动和无障碍行为仍交给 ScrollView。
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    DemoLogView(Activity activity, Collection<String> lines) {
        this.activity = activity; this.lines = lines;
        scroll = activity.findViewById(R.id.log_scroll);
        text = activity.findViewById(R.id.log);
        details = activity.findViewById(R.id.log_details);
        latest = activity.findViewById(R.id.log_latest);
        details.setOnCheckedChangeListener((button, checked) -> { displayed = ""; render(started, degraded); });
        scroll.setSmoothScrollingEnabled(false);
        scroll.setOnTouchListener((view, event) -> {
            // 仅记录用户手势，不接管系统滚动、文本选择或无障碍交互。
            ++interactionGeneration; pendingAnchor = null;
            return false;
        });
        scrollListener = () -> {
            if (!rendering) following = atBottom();
            updateLatest();
        };
        scroll.getViewTreeObserver().addOnScrollChangedListener(scrollListener);
        latest.setOnClickListener(view -> {
            ++interactionGeneration; pendingAnchor = null; following = true;
            cancelPositioning(); needsPosition = false;
            scrollTo(true, 0); updateLatest();
        });
    }

    /** 按“原记录 + 行内偏移”保留回看位置，详情切换或新日志到达时不强行跳到末尾。 */
    void render(boolean active, boolean journalDegraded) {
        started = active; degraded = journalDegraded;
        ArrayList<String> entries = new ArrayList<>(lines), display = new ArrayList<>();
        TimeZone zone = TimeZone.getDefault();
        for (String entry : entries) display.add(details.isChecked() ? entry : LogDisplay.compact(entry, zone));
        String next = TextUtils.join("\n", display);
        boolean changed = !next.equals(displayed) || !entries.equals(renderedEntries);
        updateTimeZone(); updateLatest();
        if (!changed && !needsPosition) return;
        boolean follow = following;
        Anchor anchor = follow ? null : captureAnchor();
        int previousY = scroll.getScrollY();
        long generation = ++renderGeneration, interaction = interactionGeneration;
        cancelPositioning(); pendingAnchor = anchor; needsPosition = true;
        if (changed) {
            renderedEntries.clear(); renderedEntries.addAll(entries);
            renderedStarts = new int[display.size()];
            int start = 0;
            for (int index = 0; index < display.size(); index++) {
                renderedStarts[index] = start; start += display.get(index).length() + 1;
            }
            if (!next.equals(text.getText().toString())) text.setText(next);
            displayed = next;
        }
        // 隐藏页面先保留记录锚点，重新显示并完成文本测量后再恢复滚动位置。
        if (!started || !text.isShown()) return;
        positionObserver = text.getViewTreeObserver();
        positionListener = () -> {
            cancelPositioning();
            if (!activity.isDestroyed() && started && generation == renderGeneration) {
                pendingAnchor = null; needsPosition = false;
                if (interaction == interactionGeneration) {
                    scrollTo(follow, anchor == null ? previousY : anchorY(anchor));
                    following = follow || atBottom();
                }
                updateLatest();
            }
            return true;
        };
        positionObserver.addOnPreDrawListener(positionListener);
    }

    void onShow() { needsPosition = true; }
    void onHide() {
        if (!following) pendingAnchor = captureAnchor();
        cancelPositioning(); needsPosition = true;
    }
    void onStop(boolean visible) {
        if (visible && !following) pendingAnchor = captureAnchor();
        cancelPositioning(); needsPosition = true; started = false;
    }
    void clear() {
        ++interactionGeneration; pendingAnchor = null; following = true;
        cancelPositioning(); needsPosition = true; displayed = "";
    }

    private void cancelPositioning() {
        if (positionObserver != null && positionObserver.isAlive() && positionListener != null)
            positionObserver.removeOnPreDrawListener(positionListener);
        positionObserver = null; positionListener = null;
    }
    private Anchor captureAnchor() {
        if (pendingAnchor != null) return pendingAnchor;
        Layout layout = text.getLayout();
        if (layout == null || renderedEntries.isEmpty()) return null;
        int top = Math.max(0, scroll.getScrollY() - text.getTop() - text.getTotalPaddingTop());
        int line = layout.getLineForVertical(top), offset = layout.getLineStart(line);
        int index = Arrays.binarySearch(renderedStarts, offset);
        if (index < 0) index = -index - 2;
        index = Math.max(0, Math.min(index, renderedEntries.size() - 1));
        int recordLine = layout.getLineForOffset(renderedStarts[index]);
        return new Anchor(renderedEntries.get(index), Math.max(0, top - layout.getLineTop(recordLine)));
    }
    private int anchorY(Anchor anchor) {
        Layout layout = text.getLayout();
        int index = renderedEntries.indexOf(anchor.entry);
        // 锚点记录已被有界列表淘汰时，回到仍保留的最早记录。
        if (layout == null || index < 0) return 0;
        int top = layout.getLineTop(layout.getLineForOffset(renderedStarts[index]));
        int end = index + 1 < renderedStarts.length ?
            layout.getLineTop(layout.getLineForOffset(renderedStarts[index + 1])) : layout.getHeight();
        return text.getTop() + text.getTotalPaddingTop() + top + Math.min(anchor.offset, Math.max(0, end - top - 1));
    }
    private void scrollTo(boolean tail, int y) {
        rendering = true;
        try {
            int bottom = scroll.getChildCount() == 0 ? 0 : scroll.getChildAt(0).getBottom() + scroll.getPaddingBottom();
            // fullScroll 会改变焦点，可能把可选中的日志文本拉回开头，因此只设置滚动位置。
            scroll.scrollTo(0, tail ? Math.max(0, bottom - scroll.getHeight()) : y);
        } finally { rendering = false; }
    }
    private boolean atBottom() {
        return scroll.getChildCount() == 0 || scroll.getChildAt(0).getHeight() <=
            scroll.getHeight() + scroll.getScrollY() + Math.round(24 * activity.getResources().getDisplayMetrics().density);
    }
    private void updateLatest() { latest.setVisibility(!following && !lines.isEmpty() ? View.VISIBLE : View.GONE); }
    private void updateTimeZone() {
        TimeZone zone = TimeZone.getDefault();
        int offset = zone.getOffset(System.currentTimeMillis()) / 60000;
        String formattedOffset = String.format(Locale.ROOT, "%s%02d:%02d", offset < 0 ? "-" : "+",
            Math.abs(offset) / 60, Math.abs(offset) % 60);
        String label = activity.getString(R.string.log_time_zone, zone.getID() + "（UTC" + formattedOffset + "）");
        if (degraded) label += "\n日志磁盘不可用；当前使用有界内存记录";
        ((TextView)activity.findViewById(R.id.log_time_zone)).setText(label);
    }

    // 这里只解绑触摸监听，不识别或消费点击；保留 ScrollView 原生无障碍行为。
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    @Override public void close() {
        started = false; cancelPositioning();
        details.setOnCheckedChangeListener(null); scroll.setOnTouchListener(null); latest.setOnClickListener(null);
        ViewTreeObserver observer = scroll.getViewTreeObserver();
        if (observer.isAlive()) observer.removeOnScrollChangedListener(scrollListener);
    }
}
