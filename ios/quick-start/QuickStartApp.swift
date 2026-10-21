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
                    Section("1. BLE 扫描 → 选择 → 连接") {
                        Button(model.scanning ? "停止扫描" : "扫描 BLE（10 秒）") {
                            model.scanning ? model.stopScan() : model.scan()
                        }.disabled(!model.canConnect && !model.scanning)
                        Picker("设备", selection: $model.selected) {
                            Text("请选择").tag(Optional<UUID>.none)
                            ForEach(model.devices, id: \.identifier) { device in
                                Text(QuickStartModel.deviceTitle(device)).tag(Optional(device.identifier))
                            }
                        }.disabled(!model.canConnect)
                        Button("连接所选设备", action: model.connect)
                            .disabled(!model.canConnect || model.selected == nil)
                        Button("断开并清理", action: model.disconnect)
                            .disabled(model.phase == .offline || model.phase == .disconnecting || model.phase == .closed)
                        Text("选择 LT-M90_xxxx 设备后连接，SDK 使用原始名称推荐 BLE 和打印配置；未知设备请使用 Full Demo。名称匹配不代表硬件认证。")
                            .font(.footnote)
                    }
                    Section("2. 打印固定测试图") {
                        Button("打印一份测试图", action: model.printSample).disabled(!model.canPrint)
                        Button("取消当前请求", action: model.cancel).disabled(!model.canCancel)
                        Text("棋盘 PNG 直接传给 SDK，页面 48 × 8 mm。SDK 后台解码，固定一份且不附加定位命令。RequestID 表示请求已登记，接纳失败也通过最终回调报告。")
                            .font(.footnote)
                    }
                    Section("3. 观察回调") {
                        Text(model.status).accessibilityIdentifier("quickStart.status")
                        Text(model.resultDescription).textSelection(.enabled)
                        if let result = model.cleanupResult { Text("断开清理\n" + QuickStartModel.describe(result)) }
                        if let result = model.disconnectionResult { Text("意外断连\n" + QuickStartModel.describe(result)) }
                        if let result = model.scanResult { Text("BLE 扫描\n" + QuickStartModel.describe(result)) }
                        Text("DATA_SENT 只表示传输完成，请检查实际出纸。取消不能撤回数据。后台会断开；不会自动重连或重放。")
                            .font(.footnote)
                    }
                }.navigationTitle("Printer Quick Start")
            }.navigationViewStyle(.stack).onChange(of: scenePhase) { phase in
                if phase == .background { model.disconnect() }
            }
        }
    }
}
