//
//  RouteResponse.swift
//  SmartMetro
//
//  Created by 张文瑜 on 31/3/25.
//

import Foundation

struct RouteResponse: Decodable {
    let success: Bool
    let data: RouteData?
}

struct RouteData: Decodable, Equatable {
    let path: [RouteStep]
    let total_time: Int
    let from_station: Station
    let to_station: Station
    let transfer_count: Int
}

struct RouteStep: Decodable, Identifiable, Equatable {
    var id = UUID()
    let cumulative_time: Int
    let transfer: Bool
    
    // Segment properties
    let from_station: Station?
    let line_id: Int?
    let segment_time: Int?
    let to_station: Station?
    
    // Transfer properties
    let from_line: Int?
    let message: String?
    let to_line: Int?
    let transfer_time: Int?
    
    private enum CodingKeys: String, CodingKey {
        case cumulative_time, transfer, from_station, line_id, segment_time, to_station = "to_station"
        case from_line, message, to_line, transfer_time
    }
}

struct Station: Decodable, Equatable {
    let cn: String
    let en: String
}
