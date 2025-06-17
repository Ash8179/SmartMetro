//
//  SmartMetroApp.swift
//  SmartMetro
//
//  Created by 张文瑜 on 25/3/25.
//

import SwiftUI
import UIKit
import font

@main
struct SmartMetroApp: App {
    init() {
        FontLoader.loadRetroGamingFont()
    }
    var body: some Scene {
        WindowGroup {
            SplashPage()
            //ContentView()
        }
    }
}
