import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        InitKoin_iosKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
