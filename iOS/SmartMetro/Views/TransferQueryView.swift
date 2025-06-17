//
//  TransferQueryView.swift
//  SmartMetro
//
//  Created by 张文瑜 on 31/3/25.
//

import SwiftUI
import SwiftfulLoadingIndicators

struct TransferQueryView: View {
    @Namespace private var searchNamespace
    @State private var showSearchBar = true
    @State private var fromStation = ""
    @State private var toStation = ""
    @State private var routeData: RouteData?
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var reachedTopOnce = false
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if showSearchBar {
                    inputSection
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 10)
                        .transition(.move(edge: .top).combined(with: .opacity))
                        .padding(.bottom, 8) // 恢复原样
                        .animation(.spring(response: 0.3, dampingFraction: 0.5, blendDuration: 0.2), value: showSearchBar)
                }
                
                Group {
                    if isLoading {
                        LoadingIndicator(animation: .text).padding()
                    } else if let error = errorMessage {
                        errorView(message: error)
                    } else if let data = routeData {
                        resultsView(data: data)
                    } else {
                        Spacer()
                    }
                }
            }
            .animation(.easeInOut, value: showSearchBar)
            .navigationTitle("换乘查询")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭") { dismiss() }
                }
            }
            .onChange(of: routeData) {
                withAnimation {
                    showSearchBar = routeData == nil
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
    
    private func resultsView(data: RouteData) -> some View {
        ScrollView {
            GeometryReader { geo in
                Color.clear
                    .preference(key: ScrollOffsetPreferenceKey.self, value: geo.frame(in: .named("scroll")).minY)
            }
            .frame(height: 0)

            RouteDetailsView(data: data)
                .padding()
        }
        .coordinateSpace(name: "scroll")
        .onPreferenceChange(ScrollOffsetPreferenceKey.self) { value in
            // 第一次到达顶部，标记但不展开
            if value >= 0 && !reachedTopOnce {
                reachedTopOnce = true
                return
            }

            // 滚动离开顶部，重置标记
            if value < -10 {
                reachedTopOnce = false
            }

            // 只有“已经到达顶部”且“继续上拉超过阈值”才展开搜索栏
            withAnimation(.spring(response: 0.3, dampingFraction: 0.45)) {
                if reachedTopOnce && value > 30 {
                    showSearchBar = true
                } else if value < -40 {
                    showSearchBar = false
                }
            }
        }
        .refreshable {
            resetSearch()
        }
    }
    
    private func resetSearch() {
        withAnimation {
            routeData = nil
            fromStation = ""
            toStation = ""
            errorMessage = nil
        }
    }
    
    private var searchButton: some View {
        Button(action: {
            UIApplication.shared.endEditing()   // 收起键盘
            Task { await fetchRoute() }         // 执行查询
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
              let url = URL(string: EnvironmentSwitch.baseURL + "/smartmetro/dijkstra?from=\(fromEncoded)&to=\(toEncoded)")
        else {
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
        VStack(spacing: 8) {  // 调整整体间距更紧凑
            routeSummary
                .padding(.horizontal)

            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {  // 去掉多余的间距，靠 Divider 区隔
                    ForEach(data.path.indices, id: \.self) { index in
                        RouteStepView(step: data.path[index])
                            .padding(.vertical, 8)  // 单步上下留 8pt 空隙
                        
                        // 除最后一个外，添加 Divider
                        if index != data.path.indices.last {
                            Divider()
                        }
                    }
                }
                .padding(.horizontal)
            }
        }
        .padding(.vertical, 8)  // 更紧凑的外层 padding
    }

    // MARK: 摘要视图
    private var routeSummary: some View {
        VStack(spacing: 8) {
            // 第一行：出发站 -> 小人 -> 到达站
            HStack {
                InfoCapsule(text: data.from_station.cn,
                            systemImage: "mappin.and.ellipse",
                            color: .blue)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Capsule()
                    .fill(Color(.systemOrange).opacity(0.1))
                    .frame(width: 32, height: 32)
                    .overlay(
                        Image(systemName: "figure.walk")
                            .foregroundColor(.orange)
                    )

                InfoCapsule(text: data.to_station.cn,
                            systemImage: "mappin",
                            color: .red)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
            .frame(height: 32)

            // 第二行：总时间 + 换乘次数
            HStack {
                InfoCapsule(text: "\(data.total_time) min",
                            systemImage: "clock.fill",
                            color: .green)
                    .frame(maxWidth: .infinity, alignment: .leading)

                InfoCapsule(text: "换乘：\(data.transfer_count) 次",
                            systemImage: "arrow.triangle.swap",
                            color: .purple)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
        }
        .padding(.vertical, 8)
        .padding(.horizontal)
    }
}

// MARK: - 偏移偏好键
struct ScrollOffsetPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value += nextValue()
    }
}

// MARK: - Capsule 组件
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
        .frame(minWidth: 60, maxWidth: .infinity) // 弹性宽度
        .frame(height: 32) // 固定高度
        .background(color.opacity(0.1))
        .foregroundColor(color)
        .clipShape(Capsule())
        .overlay(
            Capsule()
                .stroke(color.opacity(0.3), lineWidth: 0.5)
        )
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
    private let lineConfig: [Int: (color: Color, bgColor: Color, name: String)] = [
        1: (Color(hex: "e3002b"), Color(hex: "fdeae9"), "1"),
        2: (Color(hex: "8cc220"), Color(hex: "EBF7EC"), "2"),
        3: (Color(hex: "fcd600"), Color(hex: "fffee5"), "3"),
        4: (Color(hex: "461d84"), Color(hex: "f1ebf4"), "4"),
        5: (Color(hex: "944d9a"), Color(hex: "e8d2f0"), "5"),
        6: (Color(hex: "d40068"), Color(hex: "ffcae4"), "6"),
        7: (Color(hex: "ed6f00"), Color(hex: "ffcc99"), "7"),
        8: (Color(hex: "0094d8"), Color(hex: "60b7d4"), "8"),
        9: (Color(hex: "87caed"), Color(hex: "85C6DA"), "9"),
        10: (Color(hex: "c6afd4"), Color(hex: "e0c5f0"), "10"),
        11: (Color(hex: "871c2b"), Color(hex: "BB8866"), "11"),
        12: (Color(hex: "007a60"), Color(hex: "99CBC1"), "12"),
        13: (Color(hex: "e999c0"), Color(hex: "f4b8d2"), "13"),
        14: (Color(hex: "616020"), Color(hex: "9a982f"), "14"),
        15: (Color(hex: "c8b38e"), Color(hex: "f9e7c8"), "15"),
        16: (Color(hex: "98d1c0"), Color(hex: "C6E8DF"), "16"),
        160: (Color(hex: "98d1c0"), Color(hex: "C6E8DF"), "16"),
        17: (Color(hex: "bb796f"), Color(hex: "ebd6d3"), "17"),
        18: (Color(hex: "C09453"), Color(hex: "C09453"), "18"),
        41: (Color(hex: "b5b6b6"), Color(hex: "f2f7f7"), "浦江线"),
        51: (Color(hex: "cccccc"), Color(hex: "dddddd"), "机场联络线")
    ]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if step.transfer {
                TransferStepView(step: step, lineConfig: lineConfig)
            } else {
                SegmentStepView(step: step, lineConfig: lineConfig)
            }
        }
        .padding()
    }
}

// MARK: - 乘坐地铁信息
struct SegmentStepView: View {
    let step: RouteStep
    let lineConfig: [Int: (color: Color, bgColor: Color, name: String)]
    
    var lineInfo: (color: Color, bgColor: Color, name: String) {
        lineConfig[step.line_id ?? 0] ?? (Color.primary, Color.secondary, "未知线路")
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // 第一行：线路标题
            Text(displayLineName(for: step.line_id))
                .font(.headline)
                .foregroundColor(lineInfo.color)
                .padding(.bottom, 4)
            
            // 第二行：起止站点 + 箭头
            HStack {
                Text(step.from_station?.cn ?? "")
                    .font(.callout).bold()
                    .frame(maxWidth: .infinity, alignment: .leading)
                
                Image(systemName: "arrow.right")
                    .foregroundColor(.secondary)
                
                Text(step.to_station?.cn ?? "")
                    .font(.callout).bold()  // 中文稍大些
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
            
            Divider()
            
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
        .padding(.vertical, 8)  // 更紧凑
        .padding(.horizontal, 12)
    }
    
    private func displayLineName(for id: Int?) -> String {
        if id == 160 {
            return "Line 16 大站车"
        }
        return "Line \(id ?? 0)"
    }
}

// MARK: - 换乘信息
struct TransferStepView: View {
    let step: RouteStep
    let lineConfig: [Int: (color: Color, bgColor: Color, name: String)]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // 第一行：换乘信息 居中
            HStack {
                Spacer()
                Text("换乘：")
                    .font(.headline)
                
                Text(displayLineName(for: step.from_line))
                    .font(.headline)
                    .foregroundColor(lineConfig[step.from_line ?? 0]?.color ?? .primary)

                Image(systemName: "arrow.right")
                    .foregroundColor(.secondary)
                
                Text(displayLineName(for: step.to_line))
                    .font(.headline)
                    .foregroundColor(lineConfig[step.to_line ?? 0]?.color ?? .primary)
                Spacer()
            }
            .padding(.bottom, 4)

            Divider()

            // 第二行时间信息
            HStack {
                Text("换乘时间：\(step.transfer_time ?? 0) 分钟")
                    .font(.subheadline)
                Spacer()
                Text("累计 \(step.cumulative_time) 分钟")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 8)    // 与SegmentStepView一致
        .padding(.horizontal, 12) // 与SegmentStepView一致
    }
    
    private func displayLineName(for id: Int?) -> String {
        if id == 160 {
            return "Line 16"
        }
        return "Line \(id ?? 0)"
    }
}

extension UIApplication {
    func endEditing() {
        sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }
}

#Preview {
    TransferQueryView()
}
