//
//  MetroStation.swift
//  SmartMetro
//
//  Created by 张文瑜 on 16/3/25.
//

struct MetroStation: Decodable, Identifiable {
    let id: Int
    let nameCN: String
    let nameEN: String
    let travelGroup: String
    let distanceM: Int
    let lineInfo: LineInfo
    let associatedLines: [Int]
    
    enum CodingKeys: String, CodingKey {
        case id = "stat_id"
        case nameCN = "name_cn"
        case nameEN = "name_en"
        case travelGroup = "travel_group"
        case distanceM = "distance_m"
        case lineInfo = "line_info"
        case associatedLines = "associated_lines"
    }
}

struct LineInfo: Decodable {
    let lineNumber: Int
    let allStations: [String]
    
    enum CodingKeys: String, CodingKey {
        case lineNumber = "line"
        case allStations = "all_stations"
    }
}
