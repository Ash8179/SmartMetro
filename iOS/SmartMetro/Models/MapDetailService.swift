//
//  MapDetailService.swift
//  SmartMetro
//
//  Created by 张文瑜 on 5/6/25.
//

import Foundation

// MARK: - 模型结构体
struct MapDetail: Codable {
    let statID: String
    let nameCN: String
    let line: String
    let longitude: Double
    let latitude: Double
    let gaoLng: Double
    let gaoLat: Double

    enum CodingKeys: String, CodingKey {
        case statID = "stat_id"
        case nameCN = "name_cn"
        case line
        case longitude
        case latitude
        case gaoLng = "gao_lng"
        case gaoLat = "gao_lat"
    }
}

// MARK: - 服务类
class MapDetailService {
    static let shared = MapDetailService()
    private init() {}

    private let urlString = "http://127.0.0.1:5001/smartmetro/map_details"

    func fetchMapDetails(completion: @escaping (Result<[MapDetail], Error>) -> Void) {
        guard let url = URL(string: urlString) else {
            completion(.failure(NSError(domain: "Invalid URL", code: 0)))
            return
        }

        let task = URLSession.shared.dataTask(with: url) { data, response, error in
            // 错误处理
            if let error = error {
                completion(.failure(error))
                return
            }

            // 检查响应数据
            guard let data = data else {
                completion(.failure(NSError(domain: "No data received", code: 0)))
                return
            }

            do {
                // 解码 JSON 为结构体数组
                let decoder = JSONDecoder()
                let mapDetails = try decoder.decode([MapDetail].self, from: data)
                completion(.success(mapDetails))
            } catch {
                completion(.failure(error))
            }
        }

        task.resume()
    }
}
