//
//  StationInfo.swift
//  SmartMetro
//
//  Created by 张文瑜 on 17/4/25.
//

import Foundation

// MARK: - First API Response Model (fltime/all)
struct FirstLastTimeResponse: Codable {
    let status: String
    let data: [FirstLastTimeData]
}

struct FirstLastTimeData: Codable, Identifiable {
    var id: String { "\(line_id)-\(path_id)-\(station_id)" }
    
    var description: String?
    let first_time: String
    let from_stat_cn: String
    let last_time: String
    let line_id: Int
    let path_id: Int
    let station_id: Int
    let to_stat_cn: String
    let to_station_id: Int
    
    var direction: String {
        if let description = description {
            return description
        }
        return path_id == 0 ? "Upbound" : "Downbound"
    }
}

// MARK: - Second API Response Model (station/order)
struct StationOrderResponse: Codable {
    let status: String
    let data: [StationOrder]
    let line: Int

    struct StationOrder: Codable, Identifiable {
        var id = UUID()
        let station_order: Int
        let stations: [StationOrderData]
    }

    struct StationOrderData: Codable, Identifiable, Equatable {
        var id: String { "\(path_id)-\(station_id)" }
        let name_cn: String
        let path_id: Int
        let station_id: Int

        var direction: String {
            path_id == 0 ? "Upbound" : "Downbound"
        }
    }
}

// MARK: - Custom Decoder for station_order JSON format
extension StationOrderResponse {
    private enum CodingKeys: String, CodingKey {
        case status, data, line
    }

    struct DynamicKey: CodingKey {
        var stringValue: String
        init?(stringValue: String) { self.stringValue = stringValue }
        var intValue: Int? { Int(stringValue) }
        init?(intValue: Int) { self.stringValue = "\(intValue)" }
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        status = try container.decode(String.self, forKey: .status)
        line = try container.decode(Int.self, forKey: .line)

        let rawData = try container.nestedContainer(keyedBy: DynamicKey.self, forKey: .data)
        var tempData: [StationOrder] = []

        for key in rawData.allKeys {
            if let order = Int(key.stringValue) {
                let stations = try rawData.decode([StationOrderData].self, forKey: key)
                tempData.append(StationOrder(station_order: order, stations: stations))
            }
        }

        data = tempData.sorted { $0.station_order < $1.station_order }
    }
}

// MARK: - Combined Model for View Usage
struct StationInfo {
    let firstLastTimes: [FirstLastTimeData]
    let stationOrders: [StationOrderResponse.StationOrder]
    
    init(firstLastResponse: FirstLastTimeResponse, orderResponse: StationOrderResponse) {
        self.firstLastTimes = firstLastResponse.data
        self.stationOrders = orderResponse.data
    }
    
    func getFirstLastTime(for stationId: Int, lineId: Int) -> FirstLastTimeData? {
        return firstLastTimes.first { $0.station_id == stationId && $0.line_id == lineId }
    }
    
    func getStationOrder(for stationOrder: Int) -> StationOrderResponse.StationOrder? {
        return stationOrders.first { $0.station_order == stationOrder }
    }
}

