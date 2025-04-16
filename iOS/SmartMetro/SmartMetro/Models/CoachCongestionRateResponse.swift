//
//  CoachCongestionRateResponse.swift
//  SmartMetro
//
//  Created by 张文瑜 on 13/4/25.
//
import Foundation

// MARK: - CrowdResponse
struct CrowdResponse: Codable {
    let status: String
    let data: CrowdData?
}

// MARK: - CrowdData
struct CrowdData: Codable, Identifiable {
    let id: String
    let crowdLevel: Int
    let lineCarriage: Int
    let lineID: Int
    let lineNumber: Int
    let personNum: Int
    let timestamp: Date

    enum CodingKeys: String, CodingKey {
        case crowdLevel = "crowd_level"
        case lineCarriage = "line_carriage"
        case lineID = "line_id"
        case lineNumber = "line_number"
        case personNum = "person_num"
        case timestamp
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        crowdLevel = try container.decode(Int.self, forKey: .crowdLevel)
        lineCarriage = try container.decode(Int.self, forKey: .lineCarriage)
        lineID = try container.decode(Int.self, forKey: .lineID)
        lineNumber = try container.decode(Int.self, forKey: .lineNumber)
        personNum = try container.decode(Int.self, forKey: .personNum)
        timestamp = try container.decode(Date.self, forKey: .timestamp)
        id = "\(lineNumber)-\(lineCarriage)"
    }
}

extension JSONDecoder {
    static var crowdAPI: JSONDecoder {
        let decoder = JSONDecoder()
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "EEE, dd MMM yyyy HH:mm:ss zzz"
        decoder.dateDecodingStrategy = .formatted(formatter)
        return decoder
    }
}
