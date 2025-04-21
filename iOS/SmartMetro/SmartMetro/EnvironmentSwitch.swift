//
//  EnvironmentSwitch.swift
//  SmartMetro
//
//  Created by 张文瑜 on 20/4/25.
//

import Foundation

enum AppEnvironment {
    case simulator
    case device
    case production

    var baseURL: String {
        switch self {
        case .simulator:
            return "http://127.0.0.1:5001"
        case .device:
            return "http://172.20.10.3:5001"
        case .production:
            return "https://your-production-server.com"
        }
    }
}

struct EnvironmentSwitch {
    static var current: AppEnvironment {
        #if targetEnvironment(simulator)
        return .simulator
        #else
        return .device
        #endif
    }

    static var baseURL: String {
        return current.baseURL
    }
}
