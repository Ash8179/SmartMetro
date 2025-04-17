import CoreLocation

class LocationManager: NSObject, ObservableObject {
    private let manager = CLLocationManager()
    @Published var location: CLLocationCoordinate2D?

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.requestWhenInUseAuthorization()
        manager.startUpdatingLocation()
        
        // 随机生成一个上海市中心附近的坐标
        let randomLatitude = Double.random(in: 31.200...31.260)
        let randomLongitude = Double.random(in: 121.420...121.500)
        self.location = CLLocationCoordinate2D(latitude: randomLatitude, longitude: randomLongitude)

        print("📍 Mock Location Set: \(randomLatitude), \(randomLongitude)")
    }
}

// MARK: - CLLocationManagerDelegate 扩展
extension LocationManager: CLLocationManagerDelegate {
    /// 位置更新回调
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let newLocation = locations.last else { return }
        DispatchQueue.main.async {
            self.location = newLocation.coordinate
            print("Updated Location: \(self.location!.latitude), \(self.location!.longitude)")
        }
    }
    
    /// 监听用户权限更改
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        switch manager.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            print("Location authorized, starting updates...")
            manager.startUpdatingLocation()
        case .denied, .restricted:
            print("Location access denied.")
            location = nil
        case .notDetermined:
            print("Location permission not determined, requesting...")
            manager.requestWhenInUseAuthorization()
        @unknown default:
            print("Unknown authorization status")
        }
    }
}
