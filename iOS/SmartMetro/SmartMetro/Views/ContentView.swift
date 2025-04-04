import SwiftUI
import CoreLocation

struct ContentView: View {
    @StateObject private var locationManager = LocationManager()
    @State private var stations: [MetroStation]
    @State private var isLoading: Bool
    @State private var errorMessage: String?
    @State private var showingTransferQuery = false
    @State private var showingLineQuery = false // 新增状态变量

    init(stations: [MetroStation] = [], isLoading: Bool = false, errorMessage: String? = nil) {
        _stations = State(initialValue: stations)
        _isLoading = State(initialValue: isLoading)
        _errorMessage = State(initialValue: errorMessage)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 6) {
                Divider()
                buttonSection
                Divider()
                mainContentView
            }
            .navigationTitle("上海地铁")
            .task {
                if stations.isEmpty && errorMessage == nil {
                    await loadData()
                }
            }
        }
        .sheet(isPresented: $showingTransferQuery) {
            TransferQueryView()
        }
        .sheet(isPresented: $showingLineQuery) { // 新增线路查询视图
            StationQuery()
        }
    }

    private var buttonSection: some View {
        HStack(spacing: 20) {
            transferQueryButton
            lineQueryButton // 添加新按钮
        }
        .padding(.vertical, 10)
    }

    private var transferQueryButton: some View {
        Button(action: {
            showingTransferQuery = true
        }) {
            HStack(spacing: 4) {
                Text("换乘查询")
                Text("🚆")
            }
            .font(.headline)
            .foregroundColor(.blue)
            .padding(.horizontal, 20)
            .padding(.vertical, 15)
            .background(Color(.systemGray6))
            .cornerRadius(10)
        }
    }

    private var lineQueryButton: some View { // 线路查询按钮
        Button(action: {
            showingLineQuery = true
        }) {
            HStack(spacing: 4) {
                Text("线路查询")
                Text("🗺️")
            }
            .font(.headline)
            .foregroundColor(.blue)
            .padding(.horizontal, 20)
            .padding(.vertical, 15)
            .background(Color(.systemGray6))
            .cornerRadius(10)
        }
    }

    @ViewBuilder
    private var mainContentView: some View {
        if isLoading {
            ProgressView("Locating...")
        } else if let error = errorMessage {
            errorView(message: error)
        } else if stations.isEmpty {
            emptyView
        } else {
            stationList
        }
    }

    private var stationList: some View {
        List(stations.prefix(5)) { station in
            StationRow(station: station)
        }
        .listStyle(.plain)
        .refreshable {
            await loadData()
        }
    }

    private func errorView(message: String) -> some View {
        ContentUnavailableView(
            label: { Label("Fail to load", systemImage: "wifi.exclamationmark") },
            description: { Text(message) },
            actions: {
                Button("Retry") {
                    Task {
                        await loadData()
                    }
                }
            }
        )
    }

    private var emptyView: some View {
        ContentUnavailableView(
            label: { Label("No Metro Station Found", systemImage: "tram.fill") },
            description: { Text("No metro stations within 5 kilometers.") }
        )
    }

    private func loadData() async {
        guard let location = locationManager.location else {
            errorMessage = "Location service required."
            return
        }

        isLoading = true
        defer { isLoading = false }

        let latitude = location.latitude
        let longitude = location.longitude

        do {
            let url = URL(string: "http://127.0.0.1:5002/nearest_stations?lat=\(latitude)&lng=\(longitude)")!
            let (data, _) = try await URLSession.shared.data(from: url)

            let decoder = JSONDecoder()
            let response = try decoder.decode(StationResponse.self, from: data)
            stations = response.nearestStations
            errorMessage = nil
        } catch DecodingError.dataCorrupted(_) {
            errorMessage = "Data Missing."
        } catch DecodingError.keyNotFound(_,_) {
            errorMessage = "Location outside Shanghai area."
        } catch DecodingError.typeMismatch(_, _) {
            errorMessage = "Wrong Data Type."
        } catch {
            errorMessage = "Unknown Error: \(error.localizedDescription)"
        }
    }
}



// MARK: - 预览提供器
struct ContentView_Previews: PreviewProvider {
    // 模拟线路信息
    static let mockLineInfo = LineInfo(
        lineNumber: 10,
        allStations: ["虹桥火车站", "虹桥2号航站楼", "虹桥1号航站楼", "上海动物园"]
    )
    
    // 模拟换乘站点的线路信息
    static let transferLineInfo = LineInfo(
        lineNumber: 8,
        allStations: ["市光路", "嫩江路", "翔殷路", "黄兴公园"]
    )
    
    static var previews: some View {
        Group {
            // 正常数据预览（包含换乘站点）
            ContentView(
                stations: [
                    MetroStation(
                        id: 1,
                        nameCN: "同济大学",
                        nameEN: "Tongji University",
                        travelGroup: "244",
                        distanceM: 250,
                        lineInfo: mockLineInfo,
                        associatedLines: [11]
                    ),
                    MetroStation(
                        id: 2,
                        nameCN: "四平路",
                        nameEN: "Siping Road",
                        travelGroup: "189",
                        distanceM: 560,
                        lineInfo: transferLineInfo,
                        associatedLines: [8, 10]
                    )
                ]
            )
            .previewDisplayName("换乘站点")
            
            // 单线路站点预览
            ContentView(
                stations: [
                    MetroStation(
                        id: 3,
                        nameCN: "南京东路",
                        nameEN: "East Nanjing Road",
                        travelGroup: "10",
                        distanceM: 800,
                        lineInfo: mockLineInfo,
                        associatedLines: [10]
                    )
                ]
            )
            .previewDisplayName("单线路站点")
            
            // 加载中状态
            ContentView(
                stations: [],
                isLoading: true
            )
            .previewDisplayName("加载中")
            
            // 错误状态
            ContentView(
                stations: [],
                errorMessage: "定位服务不可用"
            )
            .previewDisplayName("错误状态")
            
            // 空数据状态
            ContentView(stations: [])
                .previewDisplayName("空数据")
        }
        .previewLayout(.sizeThatFits)
    }
}
