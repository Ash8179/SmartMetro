//
//  FontLoader.swift
//  font
//
//  Created by 张文瑜 on 11/6/25.
//

import CoreText
import Foundation

public enum FontLoader {
    public static func loadRetroGamingFont() {
        print("📦 尝试加载 retrogaming.ttf 字体...")

        let contents = try? FileManager.default.contentsOfDirectory(at: Bundle.module.bundleURL, includingPropertiesForKeys: nil)
        print("📦 Swift Package Bundle.module 包含文件：")
        contents?.forEach { print(" - \($0.lastPathComponent)") }

        guard let url = Bundle.module.url(forResource: "retrogaming", withExtension: "ttf") else {
            print("⚠️ 未找到字体 retrogaming.ttf")
            return
        }

        var error: Unmanaged<CFError>?
        CTFontManagerRegisterFontsForURL(url as CFURL, .process, &error)

        if let error = error?.takeUnretainedValue() {
            print("❌ 字体注册失败: \(error.localizedDescription)")
        } else {
            print("✅ retrogaming 字体注册成功")
        }
    }
}

