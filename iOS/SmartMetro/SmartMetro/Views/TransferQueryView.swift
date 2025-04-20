//
//  TransferQueryView.swift
//  SmartMetro
//
//  Created by 张文瑜 on 31/3/25.
//

import SwiftUI

struct TransferQueryView: View {
    @State private var fromStation = ""
    @State private var toStation = ""
    @State private var routeData: RouteData?
    @State private var isLoading = false
    @State private var errorMessage: String?
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 10) {
                inputSection
                
                if isLoading {
                    ProgressView("正在查询...")
                        .padding()
                } else if let error = errorMessage {
                    errorView(message: error)
                } else if let data = routeData {
                    ScrollView {
                        LazyVStack(spacing: 10) {
                            RouteDetailsView(data: data)
                        }
                        .padding()
                    }
                } else {
                    Spacer()
                }
            }
            .padding()
            .navigationTitle("换乘查询")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭") { dismiss() }
                }
            }
        }
    }
    
    // MARK: - 输入部分
    private var inputSection: some View {
        VStack(spacing: 12) {
            inputField(title: "出发站", text: $fromStation)
            inputField(title: "目的地", text: $toStation)
            searchButton
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.systemBackground))
                .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.gray.opacity(0.2), lineWidth: 0.5)
                )
        )
    }

    // MARK: - 更统一的输入框样式
    private func inputField(title: String, text: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.caption)
                .foregroundColor(.gray)
            TextField("", text: text)
                .padding(12)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color.white)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(Color.gray.opacity(0.2), lineWidth: 1)
                        )
                )
        }
    }
    
    private var searchButton: some View {
        Button(action: {
            Task { await fetchRoute() }
        }) {
            HStack {
                Image(systemName: "magnifyingglass")
                Text("查询路线")
                    .fontWeight(.semibold)
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(fromStation.isEmpty || toStation.isEmpty ? Color.gray : Color.blue)
            .foregroundColor(.white)
            .cornerRadius(12)
            .animation(.easeInOut(duration: 0.2), value: fromStation.isEmpty || toStation.isEmpty)
        }
        .disabled(fromStation.isEmpty || toStation.isEmpty)
    }
    
    // MARK: - 错误视图
    private func errorView(message: String) -> some View {
        Label(message, systemImage: "exclamationmark.triangle.fill")
            .foregroundColor(.red)
            .padding()
            .background(RoundedRectangle(cornerRadius: 8).fill(Color(.systemGray6)))
    }
    
    // MARK: - 网络请求
    private func fetchRoute() async {
        isLoading = true
        defer { isLoading = false }
        errorMessage = nil
        routeData = nil
        
        guard !fromStation.isEmpty, !toStation.isEmpty else {
            errorMessage = "请输入出发站和目的地"
            return
        }
        
        guard let fromEncoded = fromStation.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let toEncoded = toStation.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "http://127.0.0.1:5001/Dijkstra?from=\(fromEncoded)&to=\(toEncoded)") else {
            errorMessage = "无效的站点名称"
            return
        }
        
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let response = try JSONDecoder().decode(RouteResponse.self, from: data)
            
            if response.success, let data = response.data {
                routeData = data
            } else {
                errorMessage = "未找到路线"
            }
        } catch {
            errorMessage = "解析路线失败: \(error.localizedDescription)"
        }
    }
}

// MARK: - 路线详情
struct RouteDetailsView: View {
    let data: RouteData

    var body: some View {
        VStack(spacing: 12) {
            routeSummary
                .padding(.horizontal)

            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 12) {
                    ForEach(data.path) { step in
                        RouteStepView(step: step)
                            .cardStyle()
                    }
                }
                .padding(.horizontal)
            }
        }
        .padding(.vertical)
    }

    // MARK: 摘要视图
    private var routeSummary: some View {
        let capsuleWidth: CGFloat = 100  // 可根据文字内容调整

        return VStack(spacing: 12) {
            // —————— 第一行：出发 走路小人 到达 ——————
            HStack {
                InfoCapsule(text: data.from_station.cn,
                            systemImage: "mappin.and.ellipse",
                            color: .blue)
                    .frame(width: capsuleWidth, alignment: .leading)

                Spacer()

                Capsule()
                    .fill(Color(.systemGray5))
                    .frame(width: 32, height: 32)
                    .overlay(
                        Image(systemName: "figure.walk")
                            .foregroundColor(.orange)
                    )

                Spacer()

                InfoCapsule(text: data.to_station.cn,
                            systemImage: "mappin",
                            color: .red)
                    .frame(width: capsuleWidth, alignment: .trailing)
            }
            .frame(height: 32)  // 保持高度统一

            // —————— 第二行：总时间    换乘次数 ——————
            HStack(spacing: 8) {
                InfoCapsule(text: "\(data.total_time) min",
                            systemImage: "clock.fill",
                            color: .green)
                    .frame(width: capsuleWidth, alignment: .leading)

                Spacer()

                InfoCapsule(text: "换乘：\(data.transfer_count) 次",
                            systemImage: "arrow.triangle.swap",
                            color: .purple)
                    .frame(width: capsuleWidth, alignment: .trailing)
            }
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.systemBackground))
                .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.gray.opacity(0.2), lineWidth: 0.5)
                )
        )
    }
}

// MARK: —————— Capsule 组件 ——————
private struct InfoCapsule: View {
    let text: String
    let systemImage: String
    let color: Color

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: systemImage)
            Text(text)
                .font(.caption2).bold()
        }
        .padding(.vertical, 6)
        .padding(.horizontal, 10)
        .background(color.opacity(0.1))
        .foregroundColor(color)
        .clipShape(Capsule())
    }
}

// MARK: —————— 统一卡片样式 Modifier ——————
private extension View {
    func cardStyle() -> some View {
        self
            .padding()
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color(.systemBackground))
                    .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color.gray.opacity(0.2), lineWidth: 0.5)
                    )
            )
    }
}

// MARK: - 路线步骤
struct RouteStepView: View {
    let step: RouteStep

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if step.transfer {
                TransferStepView(step: step)
            } else {
                SegmentStepView(step: step)
            }
        }
        .cardStyle()  // 复用之前定义的白底+边框+阴影
    }
}

// MARK: - 乘坐地铁信息
struct SegmentStepView: View {
    let step: RouteStep

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            // 第一行：线路标题
            Text("Line \(step.line_id ?? 0)")
                .font(.headline)

            // 第二行：起讫站点 Capsule + 箭头 Capsule
            HStack {
                InfoCapsule(
                    text: step.from_station?.cn ?? "",
                    systemImage: "mappin.and.ellipse",
                    color: .blue
                )
                Spacer()
                // 箭头 Capsule
                Capsule()
                    .fill(Color(.systemGray5))
                    .frame(width: 32, height: 32)
                    .overlay(
                        Image(systemName: "arrow.right")
                            .foregroundColor(.secondary)
                    )
                Spacer()
                InfoCapsule(
                    text: step.to_station?.cn ?? "",
                    systemImage: "mappin",
                    color: .red
                )
            }

            // 第三行：时间信息
            HStack {
                Text("\(step.segment_time ?? 0) 分钟")
                    .font(.subheadline)
                Spacer()
                Text("累计 \(step.cumulative_time) 分钟")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
    }
}

// MARK: - 换乘信息
struct TransferStepView: View {
    let step: RouteStep

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            // 第一行：Transfer 标题
            Text("Line \(step.from_line ?? 0) → Line \(step.to_line ?? 0)")
                .font(.headline)

            // 第二行省略 Capsule，直接展示一次性换乘时间
            HStack {
                Text("\(step.transfer_time ?? 0) 分钟")
                    .font(.subheadline)
                Spacer()
                Text("累计 \(step.cumulative_time) 分钟")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
    }
}

