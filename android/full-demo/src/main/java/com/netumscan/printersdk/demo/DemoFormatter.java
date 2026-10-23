// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.*;

/** Presentation only; neither the Model nor its tests use these strings as request state. */
final class DemoFormatter {
    private DemoFormatter() { }
    static String liveStatus(DemoUiState state) {
        if (!state.connected) return "未连接 · 盖纸状态未知";
        StatusResult sample = state.liveStatus;
        return (sample == null || sample.coverOpen == null ? "上盖状态未知" : sample.coverOpen ? "上盖已打开" : "上盖已关闭") +
            " · " + (sample == null || sample.paperOut == null ? "纸张状态未知" : sample.paperOut ? "打印机缺纸" : "纸张在位");
    }
    static String sdkVersion(DemoResult<SdkInfo> result) {
        SdkInfo info = result == null || result.raw.outcome != ManagerResult.Outcome.SUCCESS ? null : result.raw.value;
        return info == null ? "SDK 版本未知" : "SDK " + info.distributionVersion;
    }
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
        if (raw.outcome == ManagerResult.Outcome.SUCCESS) {
            String description = successDescription(result.kind);
            if (description != null) text += "\n" + description;
        }
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
                "，定位=" + capabilities.labelPositioning + "，主动查询=" + capabilities.statusQuery + "，主动上报=" + capabilities.statusReporting +
                "，Raw=" + capabilities.rawCommands;
        } else if (raw.value instanceof java.util.List<?>) {
            text += "\n候选设备：" + ((java.util.List<?>)raw.value).size() + " 台；选择后可明确使用地址，不会自动连接";
        }
        return text;
    }
    /** 主页面只压缩成功结果；失败仍展示完整错误码和诊断消息。 */
    static String summary(DemoResult<?> result) {
        if (result == null || result.raw.outcome != ManagerResult.Outcome.SUCCESS) return result(result);
        String text = "SUCCESS · " + result.raw.completion;
        String description = successDescription(result.kind);
        if (description != null) text += "\n" + description;
        if (result.raw.completion == ManagerResult.Completion.DATA_SENT) text += "（不代表打印完成）";
        if (result.raw.outputMayHaveOccurred) text += "\n可能已产生输出";
        return text;
    }
    private static String successDescription(DemoResult.Kind kind) {
        switch (kind) {
        case STATUS: return "已收到并验证本次状态回复；状态显示由监听更新";
        case CONNECT: return "已连接；设备信息见本次连接快照";
        case PROFILE: return "已采用打印机型号；设备信息保留本次连接快照";
        case CAPABILITIES: return "已读取 SDK 能力";
        case IDENTITY: return "已读取 SDK 信息";
        case SCAN: return "BLE 搜索已结束；选择设备后手动连接";
        case BONDED: return "已读取配对设备；选择设备后手动连接";
        case DISCONNECTED: case DISCONNECT: return "连接已断开";
        case CLOSE: return "已关闭";
        default: return null;
        }
    }
    /** Only adopted configuration values; no address or Bluetooth name enters the journal. */
    static String profileDetails(ConnectionInfo info) {
        return "model=" + (info.model == null ? "UNKNOWN" : info.model);
    }
    private static String condition(Boolean value) { return value == null ? "未知" : value ? "是" : "否"; }
    private static String known(Object value) { return value == null ? "未知" : value.toString(); }
    static String printerInfo(DemoUiState state) {
        if (!state.connected) return "设备信息未知；连接后自动读取一次";
        PrinterInfo info = state.connectionInfo == null ? null : state.connectionInfo.printerInfo;
        if (info == null) return "本次连接未获取设备信息";
        return "连接信息快照 · 部分字段含义待确认\n本次读回不改变打印配置，不表示打印完成\n" +
            "制造商：" + known(info.manufacturer) + "\n设备名称：" + known(info.deviceName) +
            "\nUID（非序列号）：" + known(info.uidHex) + "\n协议报告版本号（含义待确认）：" + known(info.reportedVersion) +
            "\n最大打印宽度：" + known(info.maxPrintWidthDots) + " dots；DPI：" + known(info.reportedDpi) +
            "\n电量：" + known(info.batteryPercent) + "；外接电源：" + condition(info.externalPower) +
            "\n打印头温度：" + known(info.printheadTemperatureCelsius) + " ℃；异常：" + condition(info.hasError) +
            "\n纸型代码：" + known(info.paperTypeCode) + "；速度级别：" + known(info.speedLevel) +
            "；浓度级别：" + known(info.densityLevel) + "\nRTC：" + condition(info.supportsRtc) +
            "；NFC：" + condition(info.supportsNfc) + "；压缩：" + condition(info.supportsCompression);
    }
    static String connection(DemoUiState state) {
        String text = state.disconnectedResult == null ? "" : result(state.disconnectedResult);
        if (state.cleanupResult != null) text += (text.isEmpty() ? "" : "\n") + result(state.cleanupResult);
        return text;
    }
}
