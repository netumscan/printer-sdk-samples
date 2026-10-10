// SPDX-License-Identifier: MIT
// Copyright (c) 2026 NETUM
import SwiftUI

@main
struct PrinterQuickStartApp: App {
    @StateObject private var model = QuickStartModel()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            NavigationView {
                Form {
                    Section("1. 扫描并选择设备") {
                        Button(model.scanning ? "停止扫描" : "扫描 BLE（10 秒）") {
                            model.scanning ? model.stopScan() : model.scan()
                        }
                        .disabled(model.connected || model.busy)
                        Picker("设备", selection: $model.selected) {
                            Text("请选择").tag(Optional<UUID>.none)
                            ForEach(model.devices, id: \.identifier) { device in
                                Text(device.name ?? "未命名 BLE 设备").tag(Optional(device.identifier))
                            }
                        }
                        .disabled(model.connected || model.busy || model.scanning)
                        Text("广播设备不代表已经认证的打印机。请手动选择目标设备。")
                            .font(.footnote)
                    }
                    Section("2. 确认配置并连接") {
                        Text("此最小示例使用 M90 V1.2 BLE 流控和 CPCL。请确认硬件、固件、DPI 与宽度后连接；预设不代表硬件认证。")
                            .font(.footnote)
                        HStack {
                            Text("DPI")
                            TextField("DPI", text: $model.dpi).keyboardType(.numberPad)
                                .multilineTextAlignment(.trailing).accessibilityLabel("DPI")
                        }.disabled(model.connected || model.busy)
                        HStack {
                            Text("设备最大宽度（dots）")
                            TextField("宽度", text: $model.widthDots).keyboardType(.numberPad)
                                .multilineTextAlignment(.trailing).accessibilityLabel("设备最大宽度（dots）")
                        }.disabled(model.connected || model.busy)
                        Button(model.connected ? "断开" : "连接所选设备") {
                            model.connected ? model.disconnect() : model.connect()
                        }
                        .disabled(model.busy || model.scanning || (!model.connected && model.selected == nil))
                    }
                    Section("3. 提交并观察结果") {
                        Button("打印一张测试图") { model.printSample() }
                            .disabled(!model.connected || model.busy || model.printing)
                        Button("取消当前任务") { model.cancel() }
                            .disabled(!model.printing)
                        Button("刷新本地连接状态") { model.refreshConnectionState() }
                            .disabled(!model.connected || model.busy)
                        Text(model.status).accessibilityIdentifier("quickStart.status")
                        Text("DATA_SENT 只表示传输完成。取消不能撤回已发送的数据；断开或回到前台不会自动续打、重连或重放。")
                            .font(.footnote)
                    }
                }
                .navigationTitle("Printer Quick Start")
            }
            .navigationViewStyle(.stack)
            .onChange(of: scenePhase) { phase in
                if phase == .background { model.background() }
            }
        }
    }
}
