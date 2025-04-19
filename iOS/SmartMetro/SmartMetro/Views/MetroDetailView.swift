import SwiftUI

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
        case 0: return "person"
        case 1: return "person.2"
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
                ProgressView()
                    .frame(maxWidth: .infinity, minHeight: 200)
            } else {
                VStack(spacing: 20) {
                    // 车拥挤度可视化
                    if !upCrowdingData.isEmpty || !downCrowdingData.isEmpty {
                        VStack(spacing: 16) {
                            // 上行列车
                            if !upCrowdingData.isEmpty {
                                TrainVisualizationView(
                                    crowdingData: upCrowdingData,
                                    direction: "上行",
                                    lineColor: .blue
                                )
                            }
                            
                            // 下行列车
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
                Text("列车到达时间")
                    .font(.headline)

                Text("(共\(arrivals.count)班)")
                    .font(.caption)
                    .foregroundColor(.gray)
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
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
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
        .shadow(color: Color.black.opacity(0.05), radius: 4, x: 0, y: 2)
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
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        if let date = formatter.date(from: timeString) {
            formatter.dateFormat = "HH:mm"
            return formatter.string(from: date)
        }
        return timeString
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

// MARK: - 预览

#Preview {
    NavigationStack {
        StationDetailView(station: MetroStation(
            id: 1,
            nameCN: "人民广场",
            nameEN: "People's Square",
            travelGroup: "市中心",
            distanceM: 350,
            lineInfo: LineInfo(lineNumber: 2, allStations: ["静安寺", "南京西路", "人民广场", "南京东路"]),
            associatedLines: [1, 2, 8]
        ))
    }
}
