import SwiftUI
import CoreLocation

struct ContentView: View {
    @StateObject private var locationManager = LocationManager()
    @State private var stations: [MetroStation] = []
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var showingTransferQuery = false
    @State private var showingTravelInfo = false
    @State private var showingTestStation = false // 新增测试状态

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
        .sheet(isPresented: $showingTravelInfo) { // Sheet for TravelInfo
            StationInfoView()
        }
    }

    // MARK: - Button Section
    private var buttonSection: some View {
        HStack(spacing: 24) { // Increased horizontal space between the buttons for better balance
            transferQueryButton
            travelInfoButton
        }
        .padding(.horizontal, 28)  // Extra side margins for breathing room
        .padding(.vertical, 14)    // Slightly taller vertical padding for elegance
    }

    // MARK: - Transfer Query Button
    private var transferQueryButton: some View {
        Button(action: {
            showingTransferQuery = true
        }) {
            Label {
                Text("换乘查询")
                    .font(.system(size: 16, weight: .semibold))  // Text size adjusted
            } icon: {
                Image(systemName: "arrow.triangle.swap")
                    .font(.system(size: 22))                    // Icon size enlarged
            }
            .foregroundColor(.blue)
            .frame(maxWidth: .infinity, minHeight: 52)          // Minimum height increased for visual balance
            .background(Color(.systemGray6))
            .cornerRadius(16)                                   // Softer corners for modern look
            .shadow(color: .gray.opacity(0.1), radius: 4, x: 0, y: 3)
        }
    }

    // MARK: - Travel Info Button
    private var travelInfoButton: some View {
        Button(action: {
            showingTravelInfo = true
        }) {
            Label {
                Text("线路查询")
                    .font(.system(size: 16, weight: .semibold))  // Text size adjusted
            } icon: {
                Image(systemName: "map")
                    .font(.system(size: 22))                    // Icon size enlarged
            }
            .foregroundColor(.blue)
            .frame(maxWidth: .infinity, minHeight: 52)          // Minimum height increased for visual balance
            .background(Color(.systemGray6))
            .cornerRadius(16)                                   // Softer corners for modern look
            .shadow(color: .gray.opacity(0.1), radius: 4, x: 0, y: 3)
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
    
    static let mockLineInfo = LineInfo(
        lineNumber: 10,
        allStations: ["虹桥火车站", "虹桥2号航站楼", "虹桥1号航站楼", "上海动物园"]
    )
    
    static let transferLineInfo = LineInfo(
        lineNumber: 8,
        allStations: ["市光路", "嫩江路", "翔殷路", "黄兴公园"]
    )

    static var previews: some View {
        Group {
            PreviewWrapper(stations: [
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
            ])
            .previewDisplayName("换乘站点")
            
            PreviewWrapper(stations: [
                MetroStation(
                    id: 3,
                    nameCN: "南京东路",
                    nameEN: "East Nanjing Road",
                    travelGroup: "10",
                    distanceM: 800,
                    lineInfo: mockLineInfo,
                    associatedLines: [10]
                )
            ])
            .previewDisplayName("单线路站点")
            
            PreviewWrapper(isLoading: true)
                .previewDisplayName("加载中")
            
            PreviewWrapper(errorMessage: "定位服务不可用")
                .previewDisplayName("错误状态")
            
            PreviewWrapper(stations: [])
                .previewDisplayName("空数据")
        }
        .previewLayout(.sizeThatFits)
    }

    struct PreviewWrapper: View {
        @State private var stations: [MetroStation]
        @State private var isLoading: Bool
        @State private var errorMessage: String?
        @State private var showingTransferQuery = false
        @State private var showingLineQuery = false
        @State private var showingTravelInfo = false

        init(stations: [MetroStation] = [], isLoading: Bool = false, errorMessage: String? = nil) {
            _stations = State(initialValue: stations)
            _isLoading = State(initialValue: isLoading)
            _errorMessage = State(initialValue: errorMessage)
        }

        var body: some View {
            NavigationStack {
                VStack(spacing: 6) {
                    Divider()
                    HStack(spacing: 15) {
                        Button(action: { showingTransferQuery = true }) {
                            VStack(spacing: 4) {
                                Text("🚆")
                                Text("换乘查询")
                                    .font(.system(size: 12))
                            }
                            .foregroundColor(.blue)
                            .padding(10)
                            .frame(maxWidth: .infinity)
                            .background(Color(.systemGray6))
                            .cornerRadius(10)
                        }
                        Button(action: { showingLineQuery = true }) {
                            VStack(spacing: 4) {
                                Text("🗺️")
                                Text("线路查询")
                                    .font(.system(size: 12))
                            }
                            .foregroundColor(.blue)
                            .padding(10)
                            .frame(maxWidth: .infinity)
                            .background(Color(.systemGray6))
                            .cornerRadius(10)
                        }
                    }
                    .padding(.vertical, 10)
                    Divider()
                    if isLoading {
                        ProgressView("Locating...")
                    } else if let errorMessage {
                        ContentUnavailableView(
                            label: { Label("Fail to load", systemImage: "wifi.exclamationmark") },
                            description: { Text(errorMessage) }
                        )
                    } else if stations.isEmpty {
                        ContentUnavailableView(
                            label: { Label("No Metro Station Found", systemImage: "tram.fill") },
                            description: { Text("No metro stations within 5 kilometers.") }
                        )
                    } else {
                        List(stations.prefix(5)) { station in
                            StationRow(station: station)
                        }
                        .listStyle(.plain)
                    }
                }
                .navigationTitle("上海地铁")
            }
        }
    }
}
