//
//  SplashPage.swift
//  SmartMetro
//
//  Created by 张文瑜 on 24/4/25.
//

import SwiftUI
import SwiftfulLoadingIndicators

struct SplashPage: View {
    @State private var showMainView = false
    @StateObject private var locationManager = LocationManager()

    var body: some View {
        ZStack {
            Color(.systemBackground).ignoresSafeArea()

            if showMainView {
                ContentView()
                    .environmentObject(locationManager) // 传递到主界面
            } else {
                VStack(spacing: 24) {
                    Spacer()

                    LoadingIndicator(animation: .circleTrim, color: .black, size: .medium)

                    Spacer()
                }
                .onAppear {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                        withAnimation {
                            showMainView = true
                        }
                    }
                }
                .transition(.opacity)
            }
        }
    }
}

struct SplashPage_Previews: PreviewProvider {
    static var previews: some View {
        SplashPage()
    }
}
