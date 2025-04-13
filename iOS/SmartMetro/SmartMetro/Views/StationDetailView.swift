import SwiftUI

struct StationDetailView: View {
    let station: MetroStation
    @State private var crowdingData: [CrowdData] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var retryCount = 0
    private let maxRetryCount = 3
    
    var body: some View {
        ScrollView {
            contentView
                .padding()
        }
        .navigationTitle(station.nameCN)
        .task { await fetchCrowdingData() }
        .refreshable { await fetchCrowdingData() }
    }
    
    @ViewBuilder
    private var contentView: some View {
        if isLoading {
            ProgressView("正在加载拥挤度信息...")
                .frame(maxWidth: .infinity)
        } else if let errorMessage = errorMessage {
            ErrorView(message: errorMessage, retryAction: { await fetchCrowdingData() })
        } else {
            linesView
        }
    }
    
    private var linesView: some View {
        ForEach(station.associatedLines, id: \.self) { lineNumber in
            LineView(lineNumber: lineNumber, data: crowdingData.filter { $0.lineNumber == lineNumber })
                .padding(.top, 12)
        }
    }
    
    private func fetchCrowdingData() async {
        guard retryCount < maxRetryCount else { return }
        
        isLoading = true
        errorMessage = nil
        
        do {
            var allData: [CrowdData] = []
            try await withThrowingTaskGroup(of: CrowdData?.self) { group in
                for lineNumber in station.associatedLines {
                    for carriage in 1...8 {
                        group.addTask {
                            try await fetchCarriageData(lineNumber: lineNumber, carriage: carriage)
                        }
                    }
                }
                
                for try await data in group {
                    if let data = data {
                        allData.append(data)
                    }
                }
            }
            
            crowdingData = allData.sorted { $0.lineNumber < $1.lineNumber || $0.lineCarriage < $1.lineCarriage }
            retryCount = 0
        } catch {
            retryCount += 1
            errorMessage = retryCount < maxRetryCount ?
                "加载失败，正在尝试重新加载 (\(retryCount)/\(maxRetryCount))" :
                "加载失败: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    private func fetchCarriageData(lineNumber: Int, carriage: Int) async throws -> CrowdData? {
        let url = URL(string: "http://127.0.0.1:5004/api/crowding?line_id=1&line_number=\(lineNumber)&line_carriage=\(carriage)")!
        let (data, _) = try await URLSession.shared.data(from: url)
        let response = try JSONDecoder.crowdAPI.decode(CrowdResponse.self, from: data)
        return response.data
    }
}

// 提取的子视图
struct LineView: View {
    let lineNumber: Int
    let data: [CrowdData]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("线路 \(lineNumber)")
                .font(.headline)
            
            ForEach(data) { data in
                LineDataRow(data: data)
            }
        }
    }
}

struct LineDataRow: View {
    let data: CrowdData
    
    var body: some View {
        HStack {
            Text("车厢 \(data.lineCarriage)")
                .font(.system(size: 16, weight: .medium))
            
            Spacer()
            
            HStack(spacing: 8) {
                Text("人数: \(data.personNum)")
                    .font(.system(size: 14))
                
                CrowdLevelBadge(level: data.crowdLevel)
            }
        }
        .padding(.vertical, 6)
        .padding(.horizontal, 12)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

struct CrowdLevelBadge: View {
    let level: Int
    
    private var text: String {
        switch level {
        case 0: return "舒适"
        case 1: return "正常"
        case 2: return "拥挤"
        default: return "未知"
        }
    }
    
    private var color: Color {
        switch level {
        case 0: return .green
        case 1: return .yellow
        case 2: return .red
        default: return .gray
        }
    }
    
    var body: some View {
        Text(text)
            .font(.system(size: 12, weight: .bold))
            .foregroundColor(.white)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(color)
            .clipShape(RoundedRectangle(cornerRadius: 4))
    }
}

struct ErrorView: View {
    let message: String
    let retryAction: () async -> Void
    
    var body: some View {
        VStack {
            Text(message)
                .foregroundColor(.red)
                .multilineTextAlignment(.center)
            
            Button("重试") {
                Task { await retryAction() }
            }
            .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity)
    }
}



// MARK: - 预览部分
struct StationDetailView_Previews: PreviewProvider {

    // 模拟换乘站
    static var transferStation: MetroStation {
        MetroStation(
            id: 1062,
            nameCN: "四平路站",
            nameEN: "Siping Road",
            travelGroup: "189",
            distanceM: 500,
            lineInfo: LineInfo(
                lineNumber: 8,
                allStations: ["市光路", "嫩江路", "翔殷路", "黄兴公园", "延吉中路", "黄兴路", "江浦路", "鞍山新村", "四平路", "曲阳路", "虹口足球场"]
            ),
            associatedLines: [8, 10]
        )
    }

    // 模拟单线路站
    static var singleLineStation: MetroStation {
        MetroStation(
            id: 1063,
            nameCN: "同济大学站",
            nameEN: "Tongji University",
            travelGroup: "244",
            distanceM: 300,
            lineInfo: LineInfo(
                lineNumber: 10,
                allStations: ["虹桥火车站", "虹桥2号航站楼", "虹桥1号航站楼", "上海动物园", "龙溪路", "水城路", "伊犁路", "宋园路", "虹桥路"]
            ),
            associatedLines: [10]
        )
    }

    // 创建模拟 CrowdData
    private static func createMockCrowdData(
        crowdLevel: Int,
        lineCarriage: Int,
        lineID: Int,
        lineNumber: Int,
        personNum: Int
    ) -> CrowdData {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "EEE, dd MMM yyyy HH:mm:ss zzz"

        let timestamp = formatter.string(from: Date())

        let jsonString = """
        {
            "status": "success",
            "data": {
                "crowd_level": \(crowdLevel),
                "line_carriage": \(lineCarriage),
                "line_id": \(lineID),
                "line_number": \(lineNumber),
                "person_num": \(personNum),
                "timestamp": "\(timestamp)"
            }
        }
        """
        let jsonData = jsonString.data(using: .utf8)!
        return try! JSONDecoder.crowdAPI.decode(CrowdResponse.self, from: jsonData).data!
    }

    // 模拟拥挤度数据
    static var previewCrowdingData: [CrowdData] {
        [
            createMockCrowdData(crowdLevel: 0, lineCarriage: 1, lineID: 1, lineNumber: 8, personNum: 30),
            createMockCrowdData(crowdLevel: 1, lineCarriage: 2, lineID: 1, lineNumber: 8, personNum: 65),
            createMockCrowdData(crowdLevel: 2, lineCarriage: 3, lineID: 1, lineNumber: 8, personNum: 90),
            createMockCrowdData(crowdLevel: 1, lineCarriage: 1, lineID: 1, lineNumber: 10, personNum: 55)
        ]
    }

    static var previews: some View {
        Group {
            // 换乘站预览
            NavigationStack {
                StationDetailView(station: transferStation)
            }
            .previewDisplayName("换乘站")

            // 单线路站预览
            NavigationStack {
                StationDetailView(station: singleLineStation)
            }
            .previewDisplayName("单线路站")

            // 加载状态预览
            NavigationStack {
                StationLoadingMockView()
            }
            .previewDisplayName("加载中")

            // 错误状态预览
            NavigationStack {
                StationErrorMockView()
            }
            .previewDisplayName("错误状态")

            // 模拟数据预览
            NavigationStack {
                StationSimulatedDataView(crowdingData: previewCrowdingData)
            }
            .previewDisplayName("模拟数据")
        }
        .previewLayout(.sizeThatFits)
    }
}

// MARK: - 自定义Mock视图

struct StationLoadingMockView: View {
    var body: some View {
        VStack {
            Spacer()
            ProgressView("正在加载拥挤度信息...")
            Spacer()
        }
        .padding()
    }
}

struct StationErrorMockView: View {
    var body: some View {
        VStack {
            Spacer()
            Text("数据加载失败")
                .foregroundColor(.red)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .padding()
    }
}

struct StationSimulatedDataView: View {
    let crowdingData: [CrowdData]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("线路 8")
                    .font(.headline)

                ForEach(crowdingData.filter { $0.lineNumber == 8 }) { data in
                    HStack {
                        Text("车厢 \(data.lineCarriage)")
                        Spacer()
                        Text("人数: \(data.personNum)")
                            .font(.system(size: 14))
                    }
                    .padding(.vertical, 6)
                    .padding(.horizontal, 12)
                    .background(Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                }
            }
            .padding()
        }
    }
}




