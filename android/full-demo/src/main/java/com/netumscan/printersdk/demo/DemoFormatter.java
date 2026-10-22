// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.*;

/** Presentation only; neither the Model nor its tests use these strings as request state. */
final class DemoFormatter {
    private DemoFormatter() { }
    static String status(DemoUiState state) {
        if (state.inputError != null) return state.inputError;
        if (state.cancelling) return "取消中；已发送数据无法撤回";
        if (state.preparing) return "正在后台准备图片；可取消未接纳的图片";
        if (state.discovering) return "正在搜索设备…";
        switch (state.phase) {
        case CONNECTING: return "正在连接…";
        case READY: return "已连接";
        case WORKING: return "处理中：" + state.activeKind;
        case DISCONNECTING: return "正在断开…";
        case CLOSED: return "已关闭";
        default: return "未连接";
        }
    }
    static String identity(DemoResult<SdkInfo> result) {
        if (result == null) return "SDK 信息未知（等待 Manager.sdkInfo）";
        SdkInfo info = result.raw.value;
        return info == null ? result(result) : "SDK " + info.distributionVersion + " / native " + info.nativeVersion + "；硬件状态未知";
    }
    static String result(DemoResult<?> result) {
        if (result == null) return "尚无请求结果";
        ManagerResult<?> raw = result.raw;
        String text = result.kind + " · " + raw.outcome + " · code=" + raw.code + " · " + raw.completion + "\n" + raw.message;
        if (raw.completion == ManagerResult.Completion.DATA_SENT) text += "\nDATA_SENT 仅代表数据已发送，不代表打印完成";
        if (raw.outputMayHaveOccurred) text += "\n可能已产生输出；取消或断开不能撤回，不会自动重打";
        if (raw.value instanceof ConnectionInfo) {
            text += "\n采用的打印配置：" + profileDetails((ConnectionInfo)raw.value);
        } else if (raw.value instanceof StatusResult) {
            StatusResult status = (StatusResult)raw.value;
            text += "\n缺纸=" + condition(status.paperOut) + "，开盖=" + condition(status.coverOpen) +
                "，纸将尽=" + condition(status.paperNearEnd) + "，切刀错误=" + condition(status.cutterError);
        } else if (raw.value instanceof PrinterCapabilities) {
            PrinterCapabilities capabilities = (PrinterCapabilities)raw.value;
            text += "\n图像=" + capabilities.imagePrinting + "，纸张=" + capabilities.paperTypeSelection +
                "，定位=" + capabilities.labelPositioning + "，状态=" + capabilities.statusQuery +
                "，Raw=" + capabilities.rawCommands + "；实际出纸仍需验证";
        } else if (raw.value instanceof java.util.List<?>) {
            text += "\n候选设备：" + ((java.util.List<?>)raw.value).size() + " 台；选择后可明确使用地址，不会自动连接";
        }
        return text;
    }
    /** 主页面只压缩成功结果；失败仍展示完整错误码和诊断消息。 */
    static String summary(DemoResult<?> result) {
        if (result == null || result.raw.outcome != ManagerResult.Outcome.SUCCESS) return result(result);
        String text = "SUCCESS · " + result.raw.completion;
        if (result.raw.completion == ManagerResult.Completion.DATA_SENT) text += "（不代表打印完成）";
        if (result.raw.outputMayHaveOccurred) text += "\n可能已产生输出";
        return text;
    }
    /** Only adopted configuration values; no address or Bluetooth name enters the journal. */
    static String profileDetails(ConnectionInfo info) {
        return "model=" + (info.model == null ? "UNKNOWN" : info.model);
    }
    private static String condition(Boolean value) { return value == null ? "未知" : value ? "是" : "否"; }
    static String connection(DemoUiState state) {
        String text = state.disconnectedResult == null ? "" : result(state.disconnectedResult);
        if (state.cleanupResult != null) text += (text.isEmpty() ? "" : "\n") + result(state.cleanupResult);
        return text;
    }
}
