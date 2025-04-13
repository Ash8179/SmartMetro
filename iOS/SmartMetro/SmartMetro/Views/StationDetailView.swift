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



