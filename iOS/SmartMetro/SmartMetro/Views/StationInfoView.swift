//
//  StationInfoView.swift
//  SmartMetro
//
//  Created by 张文瑜 on 17/4/25.
//

import SwiftUI

struct StationInfoView: View {
    @State private var firstLastTimes: [FirstLastTimeData] = []
    @State private var stationOrders: [Int: StationOrderResponse] = [:]  // 使用 line ID 作为 key
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var selectedTab = 0
    @State private var selectedLine = 1
    
    var body: some View {
        NavigationStack {
            VStack {
                Picker("View Mode", selection: $selectedTab) {
                    Text("首末班车查询").tag(0)
                    Text("运行路线查询").tag(1)
                }
                .pickerStyle(SegmentedPickerStyle())
                .padding()
                
                if selectedTab == 1 {
                    Picker("Select Line", selection: $selectedLine) {
                        ForEach(1..<19, id: \.self) { line in
                            Text("Line \(line)").tag(line)
                        }
                        Text("浦江线").tag(41)
                        Text("机场联络线").tag(51)
                    }
                    .pickerStyle(MenuPickerStyle())
                    .padding(.horizontal)
                }

                Group {
                    if isLoading {
                        ProgressView()
                            .scaleEffect(1.5)
                            .padding()
                    } else if let error = errorMessage {
                        Text("Error: \(error)")
                            .foregroundColor(.red)
                    } else {
                        switch selectedTab {
                        case 0:
                            FirstLastTrainView(times: firstLastTimes)
                        case 1:
                            if let orderResponse = stationOrders[selectedLine] {
                                let flattenedOrders = orderResponse.data.flatMap { $0.stations }
                                StationOrderView(
                                    orders: flattenedOrders,
                                    lineID: orderResponse.line
                                )
                            } else {
                                Text("No station data available for Line \(selectedLine)")
                                    .foregroundColor(.secondary)
                            }
                        default:
                            EmptyView()
                        }
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .navigationTitle(selectedTab == 0 ? "首末班车查询" : "运行路线查询")
            .task {
                await loadData()
            }
        }
    }
    
    private func loadData() async {
        isLoading = true
        errorMessage = nil
        
        do {
            let firstLastResponse = try await loadFirstLastTimes()
            self.firstLastTimes = firstLastResponse.data
            
            let stationOrdersData = try await loadAllStationOrders()
            self.stationOrders = stationOrdersData
        } catch {
            errorMessage = error.localizedDescription
        }
        
        isLoading = false
    }

    private func loadFirstLastTimes() async throws -> FirstLastTimeResponse {
        guard let url = URL(string: "\(EnvironmentSwitch.baseURL)/smartmetro/fltime/all") else {
            throw URLError(.badURL)
        }
        let (data, _) = try await URLSession.shared.data(from: url)
            print("Response Data: \(String(data: data, encoding: .utf8) ?? "No Data")")
        return try JSONDecoder().decode(FirstLastTimeResponse.self, from: data)
    }
    private func loadAllStationOrders() async throws -> [Int: StationOrderResponse] {
        var combinedData: [Int: StationOrderResponse] = [:]
        let allLines = Array(1...18) + [41, 51]
        
        for line in allLines {
            guard let url = URL(string: "\(EnvironmentSwitch.baseURL)/smartmetro/station/order?line=\(line)") else {
                continue
            }
            do {
                let (data, _) = try await URLSession.shared.data(from: url)
                let response = try JSONDecoder().decode(StationOrderResponse.self, from: data)
                combinedData[line] = response
            } catch {
                print("Failed to load Line \(line): \(error.localizedDescription)")
            }
        }
        
        return combinedData
    }
}

// MARK: - Subviews - FirstLastTrainCard
struct FirstLastTrainCard: View {
    let config: (color: Color, bgColor: Color, name: String)
    let times: [FirstLastTimeData]

    var body: some View {
        VStack(spacing: 22) {
            // MARK: - Header
            HStack {
                Spacer()
                HStack(spacing: 8) {
                    Image(systemName: "tram.fill")
                        .foregroundColor(config.color)
                        .font(.system(size: 18))

                    Text("线路 \(config.name)")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.primary)
                }
                Spacer()
            }
            .padding(14)
            .background(
                config.color.opacity(0.12) // 使用主题色带透明度
            )
            .cornerRadius(14)
            .shadow(color: config.color.opacity(0.08), radius: 2, x: 0, y: 1)

            // MARK: - Train Time Sections
            ForEach(displayedTimes().indices, id: \.self) { index in
                let time = displayedTimes()[index]

                VStack(spacing: 16) {
                    // For Line 4: Show direction label
                    if config.name == "4" {
                        let description = index == 0 ? "内圈运行" : "外圈运行"
                        Text(description)
                            .font(.caption)
                            .foregroundColor(config.color)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 5)
                            .background(config.color.opacity(0.1))
                            .cornerRadius(20)
                    }

                    // From / To Stations
                    HStack(spacing: 14) {
                        stationCard(title: "始发站", name: time.from_stat_cn)
                        stationCard(title: "终点站", name: time.to_stat_cn)
                    }

                    Divider()
                        .frame(height: 1)
                        .background(Color.gray.opacity(0.1))
                        .padding(.horizontal, 30)

                    // First & Last Time Tags
                    HStack(spacing: 14) {
                        smallTimeTag(title: "首", time: time.first_time)
                        smallTimeTag(title: "末", time: time.last_time)
                    }
                }
                .padding(.horizontal, 6)
            }
        }
        .padding(20)
        .background(
            LinearGradient(
                gradient: Gradient(colors: [
                    Color.white,
                    config.bgColor.opacity(0.25) // 更明显的层次
                ]),
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .cornerRadius(18)
        .shadow(color: config.color.opacity(0.3), radius: 6, x: 0, y: 3)
    }

    private func displayedTimes() -> [FirstLastTimeData] {
        config.name == "4" ? Array(times.prefix(2)) : times
    }

    private func stationCard(title: String, name: String) -> some View {
        VStack(spacing: 4) {
            Text(title)
                .font(.caption)
                .foregroundColor(.gray)

            Text(name)
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(config.color)
        }
        .frame(maxWidth: .infinity)
        .padding(12)
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: config.color.opacity(0.08), radius: 1, x: 0, y: 1)
    }

    private func smallTimeTag(title: String, time: String) -> some View {
        HStack(spacing: 6) {
            Image(systemName: title == "首" ? "sunrise.fill" : "moon.fill")
                .font(.caption2)
                .foregroundColor(config.color)

            Text("\(title)：\(time)")
                .font(.caption)
                .foregroundColor(.primary)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color.white)
        .cornerRadius(10)
        .shadow(color: config.color.opacity(0.08), radius: 1, x: 0, y: 1)
    }
}

struct FirstLastTrainView: View {
    let times: [FirstLastTimeData]

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                ForEach(groupedByLine(), id: \.key) { lineID, group in
                    if let config = MetroLineConfig.getConfig(for: lineID) {
                        FirstLastTrainCard(config: config, times: group)
                    }
                }
            }
            .padding()
        }
    }

    private func groupedByLine() -> [(key: Int, value: [FirstLastTimeData])] {
        let grouped = Dictionary(grouping: times, by: { $0.line_id })
        return grouped.sorted { $0.key < $1.key }
    }
}

struct StationOrderView: View {
    let orders: [StationOrderResponse.StationOrderData]
    let lineID: Int
    
    var body: some View {
        let grouped = Dictionary(grouping: orders) { $0.path_id }
        let sortedKeys = grouped.keys.sorted()

        ScrollView {
            Group {
                if sortedKeys.count == 1, let pathId = sortedKeys.first, let stations = grouped[pathId] {
                    if let config = MetroLineConfig.getConfig(for: lineID) {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Line \(lineID)")
                                .font(.title2.bold())  // Increased size
                                .foregroundColor(config.color)
                                .padding(.bottom, 6)  // Adjusted padding for balance

                            ForEach(stations.indices, id: \.self) { i in
                                StationLineRow(
                                    station: stations[i],
                                    isFirst: i == 0,
                                    isLast: i == stations.count - 1,
                                    lineColor: config.color
                                )
                            }
                        }
                        .padding()
                        .background(
                            RoundedRectangle(cornerRadius: 16)
                                .fill(Color(.systemBackground))
                                .shadow(color: .black.opacity(0.1), radius: 6, x: 0, y: 3)
                        )
                        .padding(.horizontal)
                        .frame(maxWidth: 600)
                    }
                } else if !sortedKeys.isEmpty {
                    if let config = MetroLineConfig.getConfig(for: lineID) {
                        HStack(alignment: .top, spacing: 24) {
                            ForEach(sortedKeys, id: \.self) { pathId in
                                if let stations = grouped[pathId] {
                                    VStack(alignment: .leading, spacing: 12) {
                                        HStack(alignment: .firstTextBaseline, spacing: 8) {
                                            Text("Line \(lineID)")
                                                .font(.title2.bold())  // Increased size
                                                .foregroundColor(config.color)
                                                .padding(.bottom, 4)

                                            Text("Path \(pathId)")
                                                .font(.subheadline)
                                                .foregroundColor(.secondary)
                                        }
                                        .padding(.bottom, 8)

                                        ForEach(stations.indices, id: \.self) { i in
                                            StationLineRow(
                                                station: stations[i],
                                                isFirst: i == 0,
                                                isLast: i == stations.count - 1,
                                                lineColor: config.color
                                            )
                                        }
                                    }
                                    .padding()
                                    .background(
                                        RoundedRectangle(cornerRadius: 16)
                                            .fill(Color(.systemBackground))
                                            .shadow(color: .black.opacity(0.1), radius: 6, x: 0, y: 3)
                                    )
                                    .frame(maxWidth: 300)
                                }
                            }
                        }
                        .padding(.horizontal)
                    }
                } else {
                    Text("No data available")
                        .font(.headline)
                        .foregroundColor(.gray)
                        .padding()
                }
            }
            .padding(.vertical)
        }
    }
}

struct StationLineRow: View {
    let station: StationOrderResponse.StationOrderData
    let isFirst: Bool
    let isLast: Bool
    let lineColor: Color

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(spacing: 0) {
                if !isFirst {
                    RoundedRectangle(cornerRadius: 1)
                        .fill(lineColor)
                        .frame(width: 2, height: 10)
                } else {
                    Spacer().frame(width: 2, height: 10)
                }

                Circle()
                    .fill(lineColor)
                    .frame(width: 10, height: 10)

                if !isLast {
                    RoundedRectangle(cornerRadius: 1)
                        .fill(lineColor)
                        .frame(width: 2, height: 10)
                } else {
                    Spacer().frame(width: 2, height: 10)
                }
            }
            .frame(minHeight: 44)  // 保证对齐高度

            VStack(alignment: .leading, spacing: 4) {
                Text(station.name_cn)
                    .font(.body)
                    .foregroundColor(.primary)
            }

            Spacer()
        }
        .padding(8)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(.systemBackground))
                .shadow(color: .black.opacity(0.05), radius: 1, x: 0, y: 1)
        )
    }
}

// Unified color configuration
struct MetroLineConfig {
    static func getConfig(for lineID: Int) -> (color: Color, bgColor: Color, name: String)? {
        switch lineID {
        case 1: return (Color(hex: "e3002b"), Color(hex: "fdeae9"), "1")
        case 2: return (Color(hex: "8cc220"), Color(hex: "EBF7EC"), "2")
        case 3: return (Color(hex: "fcd600"), Color(hex: "fffee5"), "3")
        case 4: return (Color(hex: "461d84"), Color(hex: "f1ebf4"), "4")
        case 5: return (Color(hex: "944d9a"), Color(hex: "e8d2f0"), "5")
        case 6: return (Color(hex: "d40068"), Color(hex: "ffcae4"), "6")
        case 7: return (Color(hex: "ed6f00"), Color(hex: "ffcc99"), "7")
        case 8: return (Color(hex: "0094d8"), Color(hex: "60b7d4"), "8")
        case 9: return (Color(hex: "87caed"), Color(hex: "85C6DA"), "9")
        case 10: return (Color(hex: "c6afd4"), Color(hex: "e0c5f0"), "10")
        case 11: return (Color(hex: "871c2b"), Color(hex: "BB8866"), "11")
        case 12: return (Color(hex: "007a60"), Color(hex: "99CBC1"), "12")
        case 13: return (Color(hex: "e999c0"), Color(hex: "f4b8d2"), "13")
        case 14: return (Color(hex: "616020"), Color(hex: "9a982f"), "14")
        case 15: return (Color(hex: "c8b38e"), Color(hex: "f9e7c8"), "15")
        case 16: return (Color(hex: "98d1c0"), Color(hex: "C6E8DF"), "16")
        case 17: return (Color(hex: "bb796f"), Color(hex: "ebd6d3"), "17")
        case 18: return (Color(hex: "C09453"), Color(hex: "C09453"), "18")
        case 41: return (Color(hex: "b5b6b6"), Color(hex: "f2f7f7"), "浦江线")
        case 51: return (Color(hex: "cccccc"), Color(hex: "dddddd"), "机场联络线")
        default: return nil
        }
    }
}

// MARK: - Preview
struct StationInfoView_Previews: PreviewProvider {
    static var previews: some View {
        // Create complete sample data that matches your API structure
        let sampleFirstLastTimes = [
            FirstLastTimeData(
                description: nil,
                first_time: "5:30:00",
                from_stat_cn: "莘庄",
                last_time: "22:32:00",
                line_id: 1,
                path_id: 1,
                station_id: 111,
                to_stat_cn: "富锦路",
                to_station_id: 138
            ),
            FirstLastTimeData(
                description: "外圈运行",
                first_time: "5:25:00",
                from_stat_cn: "宜山路",
                last_time: "22:35:00",
                line_id: 4,
                path_id: 1,
                station_id: 402,
                to_stat_cn: "宜山路",
                to_station_id: 402
            )
        ]
        
        // Create sample station orders that match your StationOrderResponse structure
        let sampleLine1Order = StationOrderResponse(
            status: "success",
            data: [
                StationOrderResponse.StationOrder(
                    station_order: 1,
                    stations: [
                        StationOrderResponse.StationOrderData(
                            name_cn: "莘庄",
                            path_id: 0,
                            station_id: 101
                        ),
                        StationOrderResponse.StationOrderData(
                            name_cn: "外环路",
                            path_id: 0,
                            station_id: 102
                        )
                    ]
                ),
                StationOrderResponse.StationOrder(
                    station_order: 2,
                    stations: [
                        StationOrderResponse.StationOrderData(
                            name_cn: "莲花路",
                            path_id: 0,
                            station_id: 103
                        ),
                        StationOrderResponse.StationOrderData(
                            name_cn: "锦江乐园",
                            path_id: 0,
                            station_id: 104
                        )
                    ]
                )
            ],
            line: 1
        )
        
        let sampleLine2Order = StationOrderResponse(
            status: "success",
            data: [
                StationOrderResponse.StationOrder(
                    station_order: 1,
                    stations: [
                        StationOrderResponse.StationOrderData(
                            name_cn: "徐泾东",
                            path_id: 0,
                            station_id: 201
                        ),
                        StationOrderResponse.StationOrderData(
                            name_cn: "虹桥火车站",
                            path_id: 0,
                            station_id: 202
                        )
                    ]
                )
            ],
            line: 2
        )
        
        // Create a preview instance
        StationInfoView.forPreview(
            firstLastTimes: sampleFirstLastTimes,
            stationOrders: [1: sampleLine1Order, 2: sampleLine2Order]
        )
    }
}

extension StationInfoView {
    // Special initializer just for previews
    static func forPreview(
        firstLastTimes: [FirstLastTimeData],
        stationOrders: [Int: StationOrderResponse]
    ) -> StationInfoView {
        var view = StationInfoView()
        view._firstLastTimes = State(initialValue: firstLastTimes)
        view._stationOrders = State(initialValue: stationOrders)
        view._isLoading = State(initialValue: false)
        return view
    }
}

