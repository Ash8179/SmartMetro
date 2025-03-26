import Foundation

struct StationResponse: Decodable {
    let userLocation: UserLocation
    let nearestStations: [MetroStation]
    
    enum CodingKeys: String, CodingKey {
        case userLocation = "user_location"
        case nearestStations = "nearest_stations"
    }
}

struct UserLocation: Decodable {
    let lat: Double
    let lng: Double
}
