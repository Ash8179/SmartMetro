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
        VStack(spacing: 10) {
            textField("出发站", text: $fromStation)
            textField("目的地", text: $toStation)
            searchButton
        }
        .padding()
        .background(RoundedRectangle(cornerRadius: 12).fill(Color(.systemGray6)))
    }
    
    private func textField(_ placeholder: String, text: Binding<String>) -> some View {
        TextField(placeholder, text: text)
            .textFieldStyle(RoundedBorderTextFieldStyle())
            .padding(.horizontal)
            .frame(height: 44)
            .background(Color(.white))
            .cornerRadius(8)
    }
    
    private var searchButton: some View {
        Button(action: {
            Task { await fetchRoute() }
        }) {
            HStack {
                Image(systemName: "magnifyingglass.circle.fill")
                Text("查询路线")
                    .bold()
            }
            .foregroundColor(.white)
            .padding()
            .frame(maxWidth: .infinity)
            .background(fromStation.isEmpty || toStation.isEmpty ? Color.gray : Color.blue)
            .cornerRadius(10)
        }
        .disabled(fromStation.isEmpty || toStation.isEmpty)
        .padding(.top, 10)
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
        VStack(spacing: 10) {
            routeSummary
            Divider()
            ForEach(data.path) { step in
                RouteStepView(step: step)
                    .padding(.horizontal)
            }
        }
        .padding()
    }
    
    private var routeSummary: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("出发: \(data.from_station.cn)")
                Text("到达: \(data.to_station.cn)")
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 4) {
                Text("总时间: \(data.total_time)min")
                Text("换乘: \(data.transfer_count)次")
            }
        }
        .padding()
        .background(RoundedRectangle(cornerRadius: 12).fill(Color(.systemGray6)))
    }
}

// MARK: - 路线步骤
struct RouteStepView: View {
    let step: RouteStep
    
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if step.transfer {
                TransferStepView(step: step)
            } else {
                SegmentStepView(step: step)
            }
            cumulativeTimeView
        }
        .padding()
        .background(RoundedRectangle(cornerRadius: 10).fill(Color(.systemGray6)))
    }
    
    private var cumulativeTimeView: some View {
        HStack {
            Spacer()
            Text("累计 \(step.cumulative_time) 分钟")
                .font(.caption2)
                .foregroundColor(.gray)
        }
    }
}

// MARK: - 换乘信息
struct TransferStepView: View {
    let step: RouteStep
    
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Image(systemName: "arrow.triangle.2.circlepath")
                    .foregroundColor(.red)
                Text(step.message ?? "换乘")
                    .bold()
                    .foregroundColor(.orange)
            }
            
            Text("🔄 \(step.from_line ?? 0)号线 → \(step.to_line ?? 0)号线")
                .font(.caption)
                .bold()
            
            Text("⏳ 换乘时间: \(step.transfer_time ?? 0) 分钟")
                .font(.caption)
                .bold()
        }
    }
}

// MARK: - 乘坐地铁信息
struct SegmentStepView: View {
    let step: RouteStep
    
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Image(systemName: "tram.fill")
                    .foregroundColor(.blue)
                Text("\(step.from_station?.cn ?? "") → \(step.to_station?.cn ?? "")")
                    .bold()
            }
            
            Text("🚆 线路: \(step.line_id ?? 0)号线")
                .font(.caption)
                .bold()
            
            Text("⏳ 乘车时间: \(step.segment_time ?? 0) 分钟")
                .font(.caption)
                .bold()
        }
    }
}
