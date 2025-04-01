//
//  AllStationResponse.swift
//  SmartMetro
//
//  Created by 张文瑜 on 1/4/25.
//
import Foundation

struct MetroStationAPIResponseItem: Codable {
    let allStations: String  // camelCase in Swift
    let line: Int
    
    enum CodingKeys: String, CodingKey {
        case allStations = "all_stations"  // maps JSON to Swift
        case line
    }
}

class MetroStationService: ObservableObject {
    @Published var allLines: [LineInfo] = []
    
    func fetchStations() {
        guard let url = URL(string: "http://127.0.0.1:5003/allstations") else { return }
        
        URLSession.shared.dataTask(with: url) { data, response, error in
            if let data = data {
                do {
                    let decodedResponse = try JSONDecoder().decode([MetroStationAPIResponseItem].self, from: data)
                    DispatchQueue.main.async {
                        self.allLines = decodedResponse.map { item in
                            let stationNames = item.allStations
                                .replacingOccurrences(of: "\"", with: "")
                                .components(separatedBy: ", ")
                            
                            return LineInfo(lineNumber: item.line, allStations: stationNames)
                        }
                    }
                } catch {
                    print("Failed to decode JSON: \(error)")
                }
            }
        }.resume()
    }
}

