//
//  MetroDetailView.swift
//  SmartMetro
//
//  Created by 张文瑜 on 25/3/25.
//

import SwiftUI
import SwiftfulLoadingIndicators

// MARK: - 错误视图组件
struct ErrorView: View {
    let message: String
    var retryAction: (() -> Void)? = nil
    
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 48))
                .foregroundColor(.orange)
            
            Text(message)
                .foregroundColor(.primary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            if let retryAction = retryAction {
                Button(action: retryAction) {
                    Text("重试")
                        .fontWeight(.bold)
                        .frame(maxWidth: 120)
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 5)
    }
}

// MARK: - 拥挤度徽章组件
struct CrowdLevelBadge: View {
    let level: Int
    var isLarge: Bool = false

    private var icon: String {
        switch level {
        case 0: return "person.fill"
        case 1: return "person.2.fill"
        case 2: return "person.3.fill"
        default: return "questionmark"
        }
    }

    private var color: Color {
        switch level {
        case 0: return .green
        case 1: return .orange
        case 2: return .red
        default: return .gray
        }
    }

    var body: some View {
        Image(systemName: icon)
            .font(.system(size: isLarge ? 16 : 12, weight: .bold))
            .foregroundColor(.white)
            .padding(isLarge ? 8 : 6)
            .background(
                Circle()
                    .fill(color)
                    .shadow(color: color.opacity(0.3), radius: 2, x: 0, y: 1)
            )
    }
}

struct StationDetailView: View {
    let station: MetroStation
    @State private var selectedLine: Int?
    @State private var upCrowdingData: [CarriageCrowding] = []
    @State private var downCrowdingData: [CarriageCrowding] = []
    @State private var trainArrivals: [TrainArrival] = []
    @State private var isLoading = false
    @State private var showLineSelection = false
    @State private var errorMessage: String?
    @State private var congestionData: CongestionResponse? = nil
    
    
    var body: some View {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    headerView
                    
                    if let errorMessage = errorMessage {
                        ErrorView(message: errorMessage) {
                            Task { await loadData(for: selectedLine!) }
                        }
                    } else if let line = selectedLine {
                        lineDetailView(for: line)
                    } else {
                        emptyStateView
                    }
                }
                .padding()
            }
            .navigationTitle(station.nameCN)
            .navigationBarTitleDisplayMode(.inline)
            .task {
                // 默认选择第一条线路
                if selectedLine == nil, let firstLine = station.associatedLines.first {
                    selectedLine = firstLine
                    await loadData(for: firstLine)
                }
            }
        }
 
    
    // MARK: - 子视图
        private var headerView: some View {
            VStack(alignment: .leading, spacing: 8) {
                Text(station.nameCN)
                    .font(.largeTitle)
                    .bold()
                
                // 线路选择器
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(station.associatedLines, id: \.self) { line in
                            Button(action: { selectLine(line) }) {
                                Text("线路 \(line)")
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 8)
                                    .background(selectedLine == line ? Color.blue : Color.gray.opacity(0.2))
                                    .foregroundColor(selectedLine == line ? .white : .primary)
                                    .cornerRadius(8)
                            }
                        }
                    }
                }
            }
        }
    
    private var emptyStateView: some View {
        VStack {
            Text("请选择要查看的线路")
                .font(.headline)
                .foregroundColor(.secondary)
            Button("选择线路") {
                showLineSelection = true
            }
            .buttonStyle(.bordered)
        }
        .frame(maxWidth: .infinity, minHeight: 200)
    }
    
    private func lineDetailView(for line: Int) -> some View {
        Group {
            if isLoading {
                LoadingIndicator(animation: .text)
                    .frame(maxWidth: .infinity, minHeight: 200)
            } else {
                VStack(spacing: 20) {
                    // 车厢拥挤度可视化
                    if !upCrowdingData.isEmpty || !downCrowdingData.isEmpty {
                        VStack(spacing: 16) {
                            if !upCrowdingData.isEmpty {
                                TrainVisualizationView(
                                    crowdingData: upCrowdingData,
                                    direction: "上行",
                                    lineColor: .blue
                                )
                            }
                            
                            if !downCrowdingData.isEmpty {
                                TrainVisualizationView(
                                    crowdingData: downCrowdingData,
                                    direction: "下行",
                                    lineColor: .red
                                )
                            }
                        }
                    }

                    // 到站时间卡片
                    if !trainArrivals.isEmpty {
                        ArrivalCard(arrivals: trainArrivals)
                    }

                    // 安检口站点拥挤信息
                    if let congestion = congestionData {
                        StationCongestionView(congestion: congestion)
                    }
                }
                .padding(.horizontal)
            }
        }
    }
    
    // MARK: - 方法
    private func selectLine(_ line: Int) {
        selectedLine = line
        Task { await loadData(for: line) }
    }
    
    private func loadData(for line: Int) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            // 并行获取
            async let crowdingTask = MetroAPIService.shared.fetchCrowding(for: line)
            async let arrivalsTask = MetroAPIService.shared.fetchNextTrains(for: station.nameCN)

            let (crowdingDict, arrivalsResponse) = try await (crowdingTask, arrivalsTask)

            // 上行 = path_0
            let upCrowding = crowdingDict["path_0"] ?? []
            // 下行 = path_1
            let downCrowding = crowdingDict["path_1"] ?? []

            print("上行车厢共 \(upCrowding.count) 条数据")
            print("下行车厢共 \(downCrowding.count) 条数据")

            let lineKey = "Line_\(line)"
            guard let lineData = arrivalsResponse.lines[lineKey] else {
                errorMessage = "未找到线路 \(line) 的列车信息"
                trainArrivals = []
                upCrowdingData = upCrowding
                downCrowdingData = downCrowding
                return
            }

            print("上行列车数量: \(lineData.up_direction.count)")
            print("下行列车数量: \(lineData.down_direction.count)")

            trainArrivals = lineData.up_direction.map { arrival in
                print("上行列车: \(arrival.train_number) - 到达时间: \(arrival.expected_arrival_time)")
                return arrival
            } + lineData.down_direction.map { arrival in
                print("下行列车: \(arrival.train_number) - 到达时间: \(arrival.expected_arrival_time)")
                return arrival
            }

            print("合并后的列车总数: \(trainArrivals.count)")

            // 将数据分别赋值
            upCrowdingData = upCrowding
            downCrowdingData = downCrowding

        } catch {
            errorMessage = "加载失败: \(error.localizedDescription)"
            trainArrivals = []
            upCrowdingData = []
            downCrowdingData = []
            print("加载数据错误: \(error)")
        }
    }
    
    
}

// MARK: - 卡片组件
struct CrowdingCard: View {
    let crowdingData: [CarriageCrowding]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(crowdingData) { item in
                HStack {
                    Text("车厢 \(item.line_carriage)")
                        .font(.subheadline)
                    Spacer()
                    Text("\(item.person_num) 人")
                        .foregroundColor(.primary)
                    Text("拥挤度: \(item.crowd_level)")
                        .foregroundColor(item.crowd_level == 0 ? .green : (item.crowd_level == 1 ? .orange : .red))
                }
                .padding(8)
                .background(Color.gray.opacity(0.1))
                .cornerRadius(8)
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(radius: 3)
    }
}

struct ArrivalCard: View {
    let arrivals: [TrainArrival]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "calendar.badge.clock")
                    .foregroundColor(.blue)
                
                Text("列车到达时间")
                    .font(.headline).bold()

                Text("(共\(arrivals.count)班)")
                    .font(.caption)
                    .foregroundColor(.gray)
                
                Spacer()
            }

            Divider()

            let upTrain = arrivals.first { $0.direction.lowercased() == "up" }
            let downTrain = arrivals.first { $0.direction.lowercased() == "down" }

            if upTrain == nil && downTrain == nil {
                Text("暂无列车信息")
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity)
            } else {
                HStack(spacing: 12) {
                    if let upTrain = upTrain {
                        arrivalHighlightCard(arrival: upTrain, directionLabel: "上行")
                    }
                    if let downTrain = downTrain {
                        arrivalHighlightCard(arrival: downTrain, directionLabel: "下行")
                    }
                }
                .padding(.vertical, 4)

                let remainingArrivals = arrivals.filter { $0.id != upTrain?.id && $0.id != downTrain?.id }

                if !remainingArrivals.isEmpty {
                    Divider().padding(.vertical, 4)

                    ForEach(remainingArrivals.prefix(4)) { arrival in
                        VStack(alignment: .leading) {
                            HStack {
                                Text(formatTime(arrival.expected_arrival_time))
                                    .font(.subheadline)
                                    .monospacedDigit()

                                Spacer()

                                Text("列车 \(arrival.train_number)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)

                                directionBadge(for: arrival.direction.lowercased() == "up" ? "上行" : "下行")
                            }

                            if arrival.id != remainingArrivals.last?.id {
                                Divider()
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(.systemBackground))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(Color.gray.opacity(0.15), lineWidth: 1)  // 边框
                )
                .shadow(color: Color.black.opacity(0.08), radius: 6, x: 0, y: 3)  // 阴影
        )
    }

    private func arrivalHighlightCard(arrival: TrainArrival, directionLabel: String) -> some View {
        VStack(alignment: .center, spacing: 4) {
            Text(formatTime(arrival.expected_arrival_time))
                .font(.system(size: 28, weight: .bold, design: .rounded))
                .foregroundColor(.primary)

            Text("列车 \(arrival.train_number)")
                .font(.caption)
                .foregroundColor(.secondary)

            directionBadge(for: directionLabel)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.systemBackground))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.gray.opacity(0.15), lineWidth: 1)  // 边框
                )
                .shadow(color: Color.black.opacity(0.08), radius: 6, x: 0, y: 3)  // 阴影
        )
    }

    private func directionBadge(for label: String) -> some View {
        let color: Color = label == "上行" ? .green : .blue
        return Text(label)
            .font(.footnote)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(color.opacity(0.2))
            .foregroundColor(color)
            .clipShape(Capsule())
    }

    private func formatTime(_ timeString: String) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")  // 避免地区时区问题
        formatter.timeZone = TimeZone(secondsFromGMT: 0)      // 可根据后端设定调整
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"

        if let date = formatter.date(from: timeString) {
            formatter.dateFormat = "HH:mm"
            return formatter.string(from: date)
        }
        return timeString
    }
}

struct CongestionCard: View {
    let nameCN: String
    let checkpoints: [Checkpoint]

    private var updatedTime: String {
        checkpoints.max(by: { $0.createdAt < $1.createdAt })?.createdAt ?? "N/A"
    }

    private func formatTime(_ timeString: String) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX") // 避免不同地区日期解析错乱

        // 判断是否含有 Z
        if timeString.contains("Z") {
            formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ"
        } else {
            formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        }

        guard let date = formatter.date(from: timeString) else { return timeString }

        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return formatter.string(from: date)
    }

    private func crowdingLevel(for num: Int) -> Int {
        switch num {
        case 0...3: return 0
        case 4...8: return 1
        default:     return 2
        }
    }

    var body: some View {
        VStack(alignment: .leading) {
            headerSection()

            VStack {
                Spacer()
                checkpointsGrid()
                Spacer()
                updateTimeSection()
            }
        }
        .frame(height: 160) // 保证整体高度，比例才对
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(.systemBackground))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(Color.gray.opacity(0.15), lineWidth: 1)
                )
                .shadow(color: Color.black.opacity(0.12), radius: 8, x: 0, y: 4)
        )
    }

    private func headerSection() -> some View {
        HStack {
            Image(systemName: "shield.lefthalf.filled")
                .foregroundColor(.blue)
            Text("安检口状态监控")
                .font(.headline).bold()
            Spacer()
        }
    }

    private func checkpointsGrid() -> some View {
        let maxCount = min(checkpoints.count, 5)
        let spacing: CGFloat = {
            switch maxCount {
            case 5: return 1      // 五个时非常紧凑
            case 4: return 4      // 四个时略宽
            default: return 8     // 其它正常间距
            }
        }()

        return HStack(spacing: spacing) {
            ForEach(checkpoints.prefix(5).enumerated().map { $0 }, id: \.element.id) { _, cp in
                Group {
                    if maxCount == 5 {
                        CompactCheckpointBadge(
                            id: String(cp.checkpointID),
                            crowdLevel: crowdingLevel(for: cp.personNum)
                        )
                    } else {
                        CheckpointBadge(
                            id: String(cp.checkpointID),
                            crowdLevel: crowdingLevel(for: cp.personNum)
                        )
                    }
                }
                .frame(width: maxCount == 5 ? 54 : 64)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, maxCount == 5 ? 0 : 8) // 保持垂直收紧逻辑
    }

    private func updateTimeSection() -> some View {
        HStack {
            Spacer()
            HStack(spacing: 4) {
                Image(systemName: "clock")
                    .font(.system(size: 10))
                    .foregroundColor(.secondary)
                Text(formatTime(updatedTime))
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
    }
}


struct CheckpointBadge: View {
    let id: String
    let crowdLevel: Int
    private let minWidth: CGFloat = 60
    private let maxWidth: CGFloat = 72
    
    private var statusColor: Color {
        switch crowdLevel {
        case 0: return .green
        case 1: return .orange
        default: return .red
        }
    }
    
    var body: some View {
        VStack(spacing: 8) {
            // 编号标识 - 优化尺寸比例
            Text(id)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(.white)
                .frame(width: 22, height: 22)
                .background(Circle().fill(Color.blue))
                .shadow(color: .black.opacity(0.1), radius: 2, x: 0, y: 2)
            
            // 拥挤度指示器 - 增强视觉层次
            HStack(spacing: 5) {
                ForEach(0..<3) { index in
                    Circle()
                        .fill(index <= crowdLevel ? statusColor : Color.gray.opacity(0.15))
                        .frame(width: index <= crowdLevel ? 12 : 10,
                               height: index <= crowdLevel ? 12 : 10)
                        .scaleEffect(index <= crowdLevel ? 1.0 : 0.9)
                }
            }
            .padding(.horizontal, 6)
            .frame(height: 16)
        }
        .padding(10)
        .frame(minWidth: minWidth, maxWidth: maxWidth)
        .background(
            RoundedRectangle(cornerRadius: 10)
                .fill(Color(.systemBackground))
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(Color.gray.opacity(0.18), lineWidth: 0.5)
                )
                .shadow(color: .black.opacity(0.08), radius: 3, x: 0, y: 1)
        )
    }
}

struct CompactCheckpointBadge: View {
    let id: String
    let crowdLevel: Int
    
    private var statusColor: Color {
        switch crowdLevel {
        case 0: return .green
        case 1: return .orange
        default: return .red
        }
    }
    
    var body: some View {
        VStack(spacing: 6) {
            // 编号标识
            Text(id)
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(.white)
                .frame(width: 18, height: 18)
                .background(Circle().fill(Color.blue))
                .shadow(color: .black.opacity(0.1), radius: 1, x: 0, y: 1)
            
            // 拥挤度指示器
            HStack(spacing: 3) {
                ForEach(0..<3) { index in
                    Circle()
                        .fill(index <= crowdLevel ? statusColor : Color.gray.opacity(0.15))
                        .frame(width: index <= crowdLevel ? 10 : 8,
                               height: index <= crowdLevel ? 10 : 8)
                }
            }
            .padding(.horizontal, 2)
            .frame(height: 14)
        }
        // 仅修改此处宽度（从54降为48）
        .padding(.vertical, 8)
        .padding(.horizontal, 4)
        .frame(width: 48)
        .background(
            RoundedRectangle(cornerRadius: 10)
                .fill(Color(.systemBackground))
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(Color.gray.opacity(0.18), lineWidth: 0.5)
                )
                .shadow(color: .black.opacity(0.08), radius: 3, x: 0, y: 1)
        )
    }
}


// 拥挤度指示器组件
struct CrowdLevelIndicator: View {
    let level: Int
    
    private var statusText: String {
        switch level {
        case 0: return "畅通"
        case 1: return "一般"
        case 2: return "拥挤"
        default: return "拥挤"
        }
    }
    
    private var statusIcon: String {
        switch level {
        case 0: return "checkmark.circle.fill"
        case 1: return "clock.fill"
        case 2: return "exclamationmark.circle.fill"
        default: return "exclamationmark.circle.fill"
        }
    }
    
    private var statusColor: Color {
        switch level {
        case 0: return Color(.systemGreen)
        case 1: return Color(.systemOrange)
        default: return Color(.systemRed)
        }
    }
    
    var body: some View {
        HStack(spacing: 4) {
            // 状态图标
            Image(systemName: statusIcon)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(statusColor)
            
            // 状态文本
            Text(statusText)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(statusColor)
            
            // 小圆点指示器
            HStack(spacing: 3) {
                ForEach(0..<3) { index in
                    Circle()
                        .fill(index <= level ? statusColor : Color.gray.opacity(0.2))
                        .frame(width: 5, height: 5)
                }
            }
            .padding(.leading, 2)
        }
    }
}

struct StationCongestionView: View {
    let congestion: CongestionResponse

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("站点：\(congestion.nameCN)")
                .font(.title2)
                .bold()
            
            Text("Travel Group：\(congestion.travelGroup)")
                .font(.subheadline)
                .foregroundColor(.gray)

            Divider()

            ForEach(congestion.checkpoints) { checkpoint in
                VStack(alignment: .leading, spacing: 6) {
                    Text("安检口ID: \(checkpoint.checkpointID)")
                        .font(.headline)
                    
                    Text("关联ID: \(checkpoint.id.map { String($0) }.joined(separator: ", "))")
                        .font(.subheadline)
                        .foregroundColor(.secondary)

                    HStack {
                        Label("\(checkpoint.personNum) 人", systemImage: "person.3.fill")
                            .foregroundColor(.blue)
                        Spacer()
                        Text(formatDate(checkpoint.createdAt))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                .padding()
                .background(Color(.secondarySystemBackground))
                .cornerRadius(12)
                .shadow(color: Color.black.opacity(0.05), radius: 4, x: 0, y: 2)
            }
        }
        .padding()
    }

    private func formatDate(_ isoString: String) -> String {
        let formatter = ISO8601DateFormatter()
        if let date = formatter.date(from: isoString) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateStyle = .short
            displayFormatter.timeStyle = .short
            return displayFormatter.string(from: date)
        }
        return isoString
    }
}

struct TrainVisualizationView: View {
    let crowdingData: [CarriageCrowding]
    let direction: String
    let lineColor: Color
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Circle()
                    .fill(lineColor)
                    .frame(width: 12, height: 12)
                
                Text(direction)
                    .font(.subheadline)
                    .bold()
            }
            
            // 列车图形
            HStack(spacing: 2) {
                ForEach(crowdingData) { carriage in
                    VStack(spacing: 4) {
                        // 车厢编号
                        Text("\(carriage.line_carriage)")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                        
                        // 车厢图形
                        RoundedRectangle(cornerRadius: 4)
                            .stroke(lineColor, lineWidth: 1.5)
                            .background(
                                RoundedRectangle(cornerRadius: 4)
                                    .fill(Color(.systemBackground))
                            )
                            .frame(width: 40, height: 30)
                            .overlay {
                                CrowdLevelBadge(level: carriage.crowd_level)
                                    .scaleEffect(0.8)
                            }
                        
                        // 人数
                        Text("\(carriage.person_num)人")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding(.vertical, 8)
            .padding(.horizontal, 4)
            .background(Color(.secondarySystemBackground))
            .cornerRadius(8)
        }
    }
}

extension MetroAPIService {
    func fetchCongestionDetails(stationName: String) async throws -> CongestionResponse {
        // Safely encode station name
        guard let encodedName = stationName.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) else {
            print("Failed to encode station name: \(stationName)")
            throw URLError(.badURL)
        }
        
        // Build URL
        let urlString = "\(EnvironmentSwitch.baseURL)/smartmetro/congestion_details?name_cn=\(encodedName)"
        guard let url = URL(string: urlString) else {
            print("Invalid URL: \(urlString)")
            throw URLError(.badURL)
        }
        
        print("Fetching congestion details from URL: \(url.absoluteString)")
        
        // Perform network request
        let (data, response) = try await URLSession.shared.data(from: url)

        // Optional: check server response
        if let httpResponse = response as? HTTPURLResponse, !(200...299).contains(httpResponse.statusCode) {
            print("Server responded with status: \(httpResponse.statusCode)")
            throw URLError(.badServerResponse)
        }

        // Decode JSON
        return try JSONDecoder().decode(CongestionResponse.self, from: data)
    }
}

