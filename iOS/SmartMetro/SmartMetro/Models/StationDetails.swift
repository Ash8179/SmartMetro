//
//  StationDetails.swift
//  SmartMetro
//
//  Created by 张文瑜 on 18/4/25.
//

import Foundation

@MainActor
class StationDetailsViewModel: ObservableObject {
    @Published var stationDetails: StationDetails?
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var hasAttemptedLoad = false
    
    func fetchStationDetails(nameCN: String) async {
        print("⏳ [ViewModel] Starting fetch for: \(nameCN)")
        hasAttemptedLoad = true
        isLoading = true
        
        // 确保在主线程更新状态
        DispatchQueue.main.async {
            self.isLoading = true
        }
        
        defer {
            DispatchQueue.main.async {
                self.isLoading = false
            }
        }
        
        guard let encodedName = nameCN.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) else {
            print("❌ [ViewModel] Failed to encode name")
            DispatchQueue.main.async {
                self.errorMessage = "Invalid station name"
            }
            return
        }
        
        let urlString = "http://127.0.0.1:5008/station_details?name_cn=\(encodedName)"
        print("🔗 [ViewModel] Request URL: \(urlString)")
        
        guard let url = URL(string: urlString) else {
            print("❌ [ViewModel] Invalid URL")
            DispatchQueue.main.async {
                self.errorMessage = "Invalid URL"
            }
            return
        }
        
        do {
            print("🌐 [ViewModel] Starting network request...")
            let (data, response) = try await URLSession.shared.data(from: url)
            
            if let httpResponse = response as? HTTPURLResponse {
                print("📡 [ViewModel] HTTP Status: \(httpResponse.statusCode)")
                guard (200...299).contains(httpResponse.statusCode) else {
                    throw URLError(.badServerResponse)
                }
            }
            
            print("📦 [ViewModel] Received data: \(String(data: data, encoding: .utf8)?.prefix(100) ?? "nil")...")
            
            // 直接解码为 StationDetails
            let decoded = try JSONDecoder().decode(StationDetails.self, from: data)
            
            DispatchQueue.main.async {
                self.stationDetails = decoded
                print("✅ [ViewModel] Data loaded successfully")
            }
        } catch {
            print("❌ [ViewModel] Error: \(error)")
            DispatchQueue.main.async {
                self.errorMessage = error.localizedDescription
            }
        }
    }
}

struct StationDetailsResponse: Codable {
    let station: StationDetails

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        self.station = try container.decode(StationDetails.self)
    }
}

struct StationDetails: Codable {
    let nameCN: String
    let nameEN: String
    let statID: String
    let elevators: [Elevator]
    let entrances: [Entrance]
    let toilets: [Toilet]

    enum CodingKeys: String, CodingKey {
        case nameCN = "name_cn"
        case nameEN = "name_en"
        case statID = "stat_id"
        case elevators, entrances, toilets
    }
}

/// 只包含部分字段的结构体，通常来自简化 JSON 数据
struct PartialStationDetails: Codable {
    let nameCN: String

    enum CodingKeys: String, CodingKey {
        case nameCN = "name_cn"
    }

    /// 将 PartialStationDetails 转换为完整的 StationDetails
    func toStationDetails() -> StationDetails {
        StationDetails(
            nameCN: self.nameCN,
            nameEN: "",
            statID: "",
            elevators: [],
            entrances: [],
            toilets: []
        )
    }
}


struct Elevator: Codable {
    let description: String
    let icon1: String
    let icon2: String
    let idAlias: String?
    let line: Int
    let nameCN: String
    let nameEN: String
    let statID: String

    enum CodingKeys: String, CodingKey {
        case description, icon1, icon2
        case idAlias = "id_alias"
        case line
        case nameCN = "name_cn"
        case nameEN = "name_en"
        case statID = "stat_id"
    }
}

struct Entrance: Codable {
    let description: String
    let entranceID: String
    let icon1: String
    let icon2: String
    let idAlias: String?    // 改成可选
    let memo: String
    let nameCN: String
    let nameEN: String
    let statID: String
    let status: Int

    enum CodingKeys: String, CodingKey {
        case description
        case entranceID = "entrance_id"
        case icon1, icon2
        case idAlias = "id_alias"
        case memo
        case nameCN = "name_cn"
        case nameEN = "name_en"
        case statID = "stat_id"
        case status
    }
}

struct Toilet: Codable {
    let description: String
    let descriptionEN: String?
    let icon1: String
    let icon2: String
    let line: Int
    let nameCN: String
    let nameEN: String
    let planCloseDate: String?
    let planOpenDate: String?
    let statID: String
    let status: Int?
    let toiletInside: Int

    enum CodingKeys: String, CodingKey {
        case description
        case descriptionEN = "description_en"
        case icon1, icon2, line
        case nameCN = "name_cn"
        case nameEN = "name_en"
        case planCloseDate = "plan_close_date"
        case planOpenDate = "plan_open_date"
        case statID = "stat_id"
        case status
        case toiletInside = "toilet_inside"
    }
}

