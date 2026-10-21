// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewParent;
import android.widget.Button;

/** 只保存页面折叠状态；展开和恢复不触发连接、初始化或打印。 */
final class DemoSections {
    private final Activity activity;
    private final int[] panels = {R.id.device_advanced, R.id.print_advanced, R.id.examples_panel, R.id.diagnostics_panel};
    private final int[] toggles = {R.id.device_advanced_toggle, R.id.print_advanced_toggle, R.id.examples_toggle, R.id.diagnostics_toggle};
    private final int[] expandLabels = {R.string.device_advanced_expand, R.string.print_advanced_expand,
        R.string.examples_expand, R.string.diagnostics_expand};
    private final int[] collapseLabels = {R.string.device_advanced_collapse, R.string.print_advanced_collapse,
        R.string.examples_collapse, R.string.diagnostics_collapse};
    private final boolean[] expanded = new boolean[panels.length];

    DemoSections(Activity activity, Bundle saved, Runnable changed) {
        this.activity = activity;
        for (int i = 0; i < panels.length; i++) {
            final int index = i;
            expanded[i] = saved != null && saved.getBoolean(key(i));
            activity.findViewById(toggles[i]).setOnClickListener(view -> {
                if (expanded[index]) clearHiddenFocus(panels[index]);
                expanded[index] = !expanded[index]; render(); changed.run();
            });
        }
        render();
    }
    boolean expanded(int panel) {
        for (int i = 0; i < panels.length; i++) if (panels[i] == panel) return expanded[i];
        return false;
    }
    void render() {
        for (int i = 0; i < panels.length; i++) {
            activity.findViewById(panels[i]).setVisibility(expanded[i] ? View.VISIBLE : View.GONE);
            ((Button)activity.findViewById(toggles[i])).setText(expanded[i] ? collapseLabels[i] : expandLabels[i]);
        }
    }
    void reveal(View target) {
        for (View current = target; current != null;) {
            for (int i = 0; i < panels.length; i++) if (current.getId() == panels[i]) expanded[i] = true;
            ViewParent parent = current.getParent(); current = parent instanceof View ? (View)parent : null;
        }
        render();
    }
    void save(Bundle saved) {
        for (int i = 0; i < panels.length; i++) saved.putBoolean(key(i), expanded[i]);
    }
    private void clearHiddenFocus(int panel) {
        View focused = activity.getCurrentFocus();
        for (View current = focused; current != null;) {
            if (current.getId() == panel) {
                android.view.inputmethod.InputMethodManager keyboard =
                    (android.view.inputmethod.InputMethodManager)activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
                if (keyboard != null) keyboard.hideSoftInputFromWindow(focused.getWindowToken(), 0);
                focused.clearFocus(); return;
            }
            ViewParent parent = current.getParent(); current = parent instanceof View ? (View)parent : null;
        }
    }
    private String key(int index) { return "section_" + activity.getResources().getResourceEntryName(panels[index]); }
}
