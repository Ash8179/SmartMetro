//
//  MetroAPIService.swift
//  SmartMetro
//
//  Created by 张文瑜 on 15/4/25.
//

import Foundation

// MARK: - 拥挤度相关模型
struct CrowdingInfo: Decodable {
    let line_number: Int
    let data: [String: [CarriageCrowding]]
}

struct CarriageCrowding: Decodable, Identifiable {
    var id: Int { line_carriage }
    let crowd_level: Int
    let line_carriage: Int
    let person_num: Int
    let timestamp: String
}

// MARK: - 到站时间相关模型
struct TrainArrivalResponse: Decodable {
    let station_name: String
    let lines: [String: TrainLineArrival]
}

struct TrainLineArrival: Decodable {
    let up_direction: [TrainArrival]
    let down_direction: [TrainArrival]
}

struct TrainArrival: Decodable, Identifiable {
    var id: String { train_number }
    let train_number: String
    let direction: String
    let expected_arrival_time: String
    let path_id: Int
    let line_id: Int
}

// MARK: - API 服务
class MetroAPIService {
    static let shared = MetroAPIService()
    private init() {}

    // 获取车厢拥挤度数据
    func fetchCrowding(for lineNumber: Int) async throws -> [String: [CarriageCrowding]] {
        guard let url = URL(string: "http://localhost:5004/api/crowding/batch?line_number=\(lineNumber)") else {
            throw URLError(.badURL)
        }

        let (data, _) = try await URLSession.shared.data(from: url)

        if let jsonString = String(data: data, encoding: .utf8) {
            print("拥挤度原始JSON:\n\(jsonString)")
        }

        let decoded = try JSONDecoder().decode(CrowdingInfo.self, from: data)
        return decoded.data
    }

    // 获取到站时间数据
    func fetchNextTrains(for stationName: String) async throws -> TrainArrivalResponse {
        guard let urlEncoded = stationName.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "http://localhost:5005/api/next_trains?station_name=\(urlEncoded)") else {
            throw URLError(.badURL)
        }

        let (data, _) = try await URLSession.shared.data(from: url)

        if let jsonString = String(data: data, encoding: .utf8) {
            print("到站时间原始JSON:\n\(jsonString)")
        }

        return try JSONDecoder().decode(TrainArrivalResponse.self, from: data)
    }
}
