import SwiftUI

@main
struct PrinterDemoApp: App {
    @StateObject private var model = DemoModel()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            DemoView(model: model)
                .onChange(of: scenePhase) { value in
                    if value == .background { model.background() }
                    else if value == .active { model.foreground() }
                }
        }
    }
}
