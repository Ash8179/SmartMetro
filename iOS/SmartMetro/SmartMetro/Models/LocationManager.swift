//
//  LocationManager.swift
//  SmartMetro
//
//  Created by 张文瑜 on 16/3/25.
//

import CoreLocation

class LocationManager: NSObject, ObservableObject {
    private let manager = CLLocationManager()
    @Published var location: CLLocationCoordinate2D?

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.requestWhenInUseAuthorization()

        #if targetEnvironment(simulator)
        // Use mock location when running on Simulator
        let randomLatitude = Double.random(in: 31.200...31.260)
        let randomLongitude = Double.random(in: 121.420...121.500)
        self.location = CLLocationCoordinate2D(latitude: randomLatitude, longitude: randomLongitude)
        print("Simulator: Mock Location Set: \(randomLatitude), \(randomLongitude)")
        #else
        // Use real GPS location on device
        manager.startUpdatingLocation()
        print("Device: Real Location updates started.")
        #endif
    }
}

extension LocationManager: CLLocationManagerDelegate {
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        #if !targetEnvironment(simulator) // Skip updating if mock location is used
        guard let newLocation = locations.last else { return }
        DispatchQueue.main.async {
            self.location = newLocation.coordinate
            print("Device Updated Location: \(self.location!.latitude), \(self.location!.longitude)")
        }
        #endif
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        switch manager.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            print("Location authorized, starting updates...")
            #if !targetEnvironment(simulator)
            manager.startUpdatingLocation()
            #endif
        case .denied, .restricted:
            print("Location access denied.")
            location = nil
        case .notDetermined:
            print("Location permission not determined, requesting...")
            manager.requestWhenInUseAuthorization()
        @unknown default:
            print("Unknown authorization status.")
        }
    }
}
