// swift-tools-version: 6.1
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "font",
    platforms: [
        .iOS(.v13),
        .macOS(.v10_15)
    ],
    products: [
        .library(
            name: "font",
            targets: ["font"]),
    ],
    dependencies: [],
    targets: [
        .target(
            name: "font",
            dependencies: [],
            resources: [
                .process("Fonts")
            ]
        ),
        .testTarget(
            name: "fontTests",
            dependencies: ["font"]),
    ]
)


