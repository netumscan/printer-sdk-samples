// SPDX-License-Identifier: MIT
// Copyright (c) 2026 NETUM
import SwiftUI

@main
struct PrinterQuickStartApp: App {
    @StateObject private var model = QuickStartModel()
    @Environment(\.scenePhase) private var scenePhase
    var body: some Scene {
        WindowGroup {
            QuickStartView(model: model).onChange(of: scenePhase) { phase in
                if phase == .background { model.background() }
                else if phase == .active { model.foregroundEntry() }
            }
        }
    }
}

struct QuickStartView: View {
    @ObservedObject var model: QuickStartModel
    var body: some View {
        NavigationView {
            Form {
                Section("1. BLE 扫描 → 选择 → 连接") {
                    Button(model.scanStopping ? "正在停止…" : model.scanning ? "停止扫描" : "扫描 BLE（10 秒）") {
                        model.scanning ? model.stopScan() : model.scan()
                    }.disabled(!model.canScan).accessibilityIdentifier("quickStart.scan")
                    Text(model.scanMessage).font(.footnote).accessibilityIdentifier("quickStart.scanStatus")
                    VStack(alignment: .leading, spacing: 12) {
                        ForEach(model.deviceRows) { row in
                            Button {
                                model.selectDevice(row.isSelected ? nil : row.id)
                            } label: {
                                HStack {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(row.title).foregroundStyle(.primary)
                                        Text(row.signal).font(.caption).foregroundStyle(.secondary)
                                    }
                                    Spacer()
                                    if row.isSelected { Image(systemName: "checkmark.circle.fill").accessibilityHidden(true) }
                                }.frame(maxWidth: .infinity, alignment: .leading).contentShape(Rectangle())
                            }.buttonStyle(.plain).disabled(!model.canSelectDevice)
                                .accessibilityIdentifier("quickStart.device.\(row.id.uuidString)")
                                .accessibilityValue(row.isSelected ? "已选择" : "未选择")
                        }
                    }
                    if model.scanning {
                        Text("候选会陆续出现。可先选择，结束或停止扫描后再手动连接。").font(.footnote)
                    }
                    Button("连接所选设备", action: model.connect)
                        .disabled(!model.canConnect || model.selected == nil).accessibilityIdentifier("quickStart.connect")
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
        }.navigationViewStyle(.stack)
    }
}
