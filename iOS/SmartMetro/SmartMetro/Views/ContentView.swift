//
//  ContentView.swift
//  SmartMetro
//
//  Created by 张文瑜 on 16/3/25.
//

import SwiftUI
import CoreLocation
import SwiftfulLoadingIndicators
import FluidGradient

struct ContentView: View {
    @EnvironmentObject var locationManager: LocationManager
    @State private var stations: [MetroStation] = []
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var showingTransferQuery = false
    @State private var showingTravelInfo = false
    @State private var showingTestStation = false

    init(forPreview: Bool = false) {
        if forPreview {
            self._stations = State(initialValue: [
                MetroStation(id: 1, nameCN: "人民广场", nameEN: "People's Square", travelGroup: "1", distanceM: 150, lineInfo: LineInfo(lineNumber: 1, allStations: []), associatedLines: [1, 2, 8]),
                MetroStation(id: 2, nameCN: "南京西路", nameEN: "West Nanjing Road", travelGroup: "2", distanceM: 300, lineInfo: LineInfo(lineNumber: 2, allStations: []), associatedLines: [2])
            ])
            self._isLoading = State(initialValue: false)
            self._errorMessage = State(initialValue: nil)
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
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
        }
        .sheet(isPresented: $showingTransferQuery) {
            TransferQueryView()
        }
        .sheet(isPresented: $showingTravelInfo) {
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
        .padding(.vertical, 14)    // Slightly taller vertical padding
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
            .cornerRadius(16)                                   // Softer corners
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
            .frame(maxWidth: .infinity, minHeight: 52)          // Minimum height increased
            .background(Color(.systemGray6))
            .cornerRadius(16)                                   // Softer corners
            .shadow(color: .gray.opacity(0.1), radius: 4, x: 0, y: 3)
        }
    }

    @ViewBuilder
    private var mainContentView: some View {
        if isLoading {
            VStack(spacing: 12) {
                LoadingIndicator(animation: .text)
            }
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
        GeometryReader { geo in
            VStack {
                Spacer(minLength: geo.size.height * 0.1)

                VStack(alignment: .leading, spacing: 8) {
                    Text("Error")
                        .font(.largeTitle)
                        .bold()

                    VStack(spacing: 6) {
                        Label {
                            Text("Fail to load")
                                .font(.title3)
                                .bold()
                        } icon: {
                            Image(systemName: "wifi.exclamationmark")
                                .font(.title2)
                        }
                        .frame(maxWidth: .infinity)
                        .multilineTextAlignment(.center)

                        Text(message)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)

                        Button {
                            Task {
                                await loadData()
                            }
                        } label: {
                            Text("Retry")
                                .font(.footnote)
                                .foregroundColor(.blue)
                        }
                        .padding(.top, 2)
                    }
                    .frame(maxWidth: .infinity)
                }
                .frame(width: geo.size.width * 0.8)

                HStack {
                    Text("Alternatively...")
                        .font(.headline)
                        .foregroundColor(.secondary)
                    Spacer()
                }
                .frame(width: geo.size.width * 0.8)
                .padding(.top, 10)

                PassengerRunnerGameView()
                    .frame(height: 300)
                    .padding(.top, -40)

                Spacer()
            }
            .frame(width: geo.size.width, height: geo.size.height)
        }
        .background(Color(.systemBackground))
    }

    private var emptyView: some View {
        GeometryReader { geo in
            VStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Error")
                        .font(.largeTitle)
                        .bold()

                    VStack(spacing: 6) {
                        Label {
                            Text("No Metro Station Found")
                                .font(.title3)
                                .bold()
                        } icon: {
                            Image(systemName: "tram.fill")
                                .font(.title2)
                        }
                        .frame(maxWidth: .infinity)
                        .multilineTextAlignment(.center)

                        Text("No metro stations within 5 kilometers.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                }
                .frame(width: geo.size.width * 0.8)
                .padding(.bottom, 0)

                PassengerRunnerGameView()
                    .frame(height: 300)
                    .frame(width: geo.size.width * 0.8)
            }
            .frame(width: geo.size.width, height: geo.size.height)
            .position(x: geo.size.width / 2, y: geo.size.height / 2)
        }
        .background(Color(.systemBackground))
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
            guard let encodedLatitude = String(latitude).addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
                  let encodedLongitude = String(longitude).addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
                  let url = URL(string: "\(EnvironmentSwitch.baseURL)/smartmetro/nearest_stations?lat=\(encodedLatitude)&lng=\(encodedLongitude)") else {
                throw URLError(.badURL)
            }
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
    
    static var previews: some View {
        Group {
            ContentView(forPreview: true)
                .environmentObject(LocationManager())
                .previewDisplayName("正常状态（带测试数据）")
                .task {
                    // 模拟加载成功
                }
                .onAppear {
                    // 手动注入一些站点数据
                }

            ContentView()
                .environmentObject(LocationManager())
                .previewDisplayName("加载中")
                .onAppear {
                    // 模拟加载中状态
                }

            ContentView()
                .environmentObject(LocationManager())
                .previewDisplayName("错误状态")
                .onAppear {
                    // 模拟错误状态
                }
        }
    }
}
