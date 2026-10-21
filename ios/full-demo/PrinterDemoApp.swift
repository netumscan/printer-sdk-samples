import SwiftUI

@main
struct PrinterDemoApp: App {
    @StateObject private var model = DemoModel(configurationStore: DemoConfigurationStore(), journal: DemoLogJournal())
    @Environment(\.scenePhase) private var scenePhase
    var body: some Scene {
        WindowGroup {
            DemoView(model: model).onAppear { model.loadInfo() }.onChange(of: scenePhase) { phase in
                if phase == .background { model.background() }
                else if phase == .active { model.foregroundEntry() }
            }
        }
    }
}
