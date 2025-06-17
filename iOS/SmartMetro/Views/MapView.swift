//
//  MapView.swift
//  SmartMetro
//
//  Created by 张文瑜 on 4/6/25.
//

import SwiftUI
import MapKit

// MARK: - Color extension for Hex
extension UIColor {
    convenience init(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")

        var rgb: UInt64 = 0
        Scanner(string: hexSanitized).scanHexInt64(&rgb)

        let r = CGFloat((rgb & 0xFF0000) >> 16) / 255
        let g = CGFloat((rgb & 0x00FF00) >> 8) / 255
        let b = CGFloat(rgb & 0x0000FF) / 255

        self.init(red: r, green: g, blue: b, alpha: 1.0)
    }
}

// MARK: - Line Config
private let lineConfig: [Int: (color: UIColor, bgColor: UIColor, name: String)] = [
    1: (.init(hex: "e3002b"), .init(hex: "fdeae9"), "1"),
    2: (.init(hex: "8cc220"), .init(hex: "EBF7EC"), "2"),
    3: (.init(hex: "fcd600"), .init(hex: "fffee5"), "3"),
    4: (.init(hex: "461d84"), .init(hex: "f1ebf4"), "4"),
    5: (.init(hex: "944d9a"), .init(hex: "e8d2f0"), "5"),
    6: (.init(hex: "d40068"), .init(hex: "ffcae4"), "6"),
    7: (.init(hex: "ed6f00"), .init(hex: "ffcc99"), "7"),
    8: (.init(hex: "0094d8"), .init(hex: "60b7d4"), "8"),
    9: (.init(hex: "87caed"), .init(hex: "85C6DA"), "9"),
    10: (.init(hex: "c6afd4"), .init(hex: "e0c5f0"), "10"),
    11: (.init(hex: "871c2b"), .init(hex: "BB8866"), "11"),
    12: (.init(hex: "007a60"), .init(hex: "99CBC1"), "12"),
    13: (.init(hex: "e999c0"), .init(hex: "f4b8d2"), "13"),
    14: (.init(hex: "616020"), .init(hex: "9a982f"), "14"),
    15: (.init(hex: "c8b38e"), .init(hex: "f9e7c8"), "15"),
    16: (.init(hex: "98d1c0"), .init(hex: "C6E8DF"), "16"),
    17: (.init(hex: "bb796f"), .init(hex: "ebd6d3"), "17"),
    18: (.init(hex: "C09453"), .init(hex: "C09453"), "18"),
    41: (.init(hex: "b5b6b6"), .init(hex: "f2f7f7"), "浦江线"),
    51: (.init(hex: "cccccc"), .init(hex: "dddddd"), "机场联络线")
]

// MARK: - Custom Annotation
class StationAnnotation: NSObject, MKAnnotation {
    let coordinate: CLLocationCoordinate2D
    let title: String?
    let lineString: String

    init(coordinate: CLLocationCoordinate2D, title: String, lineString: String) {
        self.coordinate = coordinate
        self.title = title
        self.lineString = lineString
    }
}

// MARK: - MapView
struct MapView: UIViewRepresentable {
    @ObservedObject var locationManager: LocationManager
    var mapDetails: [MapDetail]
    var onStationSelected: (String) -> Void

    class Coordinator: NSObject, MKMapViewDelegate {
        var parent: MapView

        init(parent: MapView) {
            self.parent = parent
        }

        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            guard let station = annotation as? StationAnnotation else { return nil }

            let identifier = "station"
            var view = mapView.dequeueReusableAnnotationView(withIdentifier: identifier)

            if view == nil {
                view = MKAnnotationView(annotation: annotation, reuseIdentifier: identifier)
                view?.canShowCallout = false
                view?.isUserInteractionEnabled = true
            } else {
                view?.annotation = annotation
            }

            view?.subviews.forEach { $0.removeFromSuperview() }

            // Visual content
            let dot = UIView(frame: CGRect(x: 0, y: 0, width: 6, height: 6))
            dot.backgroundColor = .black
            dot.layer.cornerRadius = 3

            let nameLabel = UILabel()
            nameLabel.text = station.title
            nameLabel.font = .systemFont(ofSize: 10)
            nameLabel.textColor = .black
            nameLabel.textAlignment = .center

            let lineStack = UIStackView()
            lineStack.axis = .horizontal
            lineStack.spacing = 2
            lineStack.alignment = .center

            let lineNumbers = station.lineString
                .split(separator: ",")
                .compactMap { Int($0.trimmingCharacters(in: .whitespaces)) }

            for line in lineNumbers {
                let config = lineConfig[line] ?? (.black, .lightGray, "?")

                let label = UILabel()
                label.text = config.name
                label.font = .boldSystemFont(ofSize: 7)
                label.textColor = .white
                label.textAlignment = .center
                label.backgroundColor = config.color.withAlphaComponent(0.7)
                label.layer.cornerRadius = 6
                label.clipsToBounds = true

                let size: CGFloat = (line == 41 || line == 51) ? 18 : 12
                label.translatesAutoresizingMaskIntoConstraints = false
                NSLayoutConstraint.activate([
                    label.widthAnchor.constraint(equalToConstant: size),
                    label.heightAnchor.constraint(equalToConstant: size)
                ])

                lineStack.addArrangedSubview(label)
            }

            let fullStack = UIStackView(arrangedSubviews: [dot, nameLabel, lineStack])
            fullStack.axis = .vertical
            fullStack.alignment = .center
            fullStack.spacing = 2

            view?.addSubview(fullStack)
            fullStack.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                fullStack.centerXAnchor.constraint(equalTo: view!.centerXAnchor),
                fullStack.centerYAnchor.constraint(equalTo: view!.centerYAnchor, constant: -15)
            ])

            view?.frame = CGRect(x: 0, y: 0, width: 80, height: 60)

            // 添加点击手势
            let tap = UITapGestureRecognizer(target: self, action: #selector(annotationTapped(_:)))
            view?.addGestureRecognizer(tap)

            return view
        }

        @objc func annotationTapped(_ sender: UITapGestureRecognizer) {
            guard let view = sender.view as? MKAnnotationView,
                  let station = view.annotation as? StationAnnotation else { return }
            parent.onStationSelected(station.title ?? "")
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    func makeUIView(context: Context) -> MKMapView {
        let mapView = MKMapView()
        mapView.delegate = context.coordinator
        mapView.mapType = .mutedStandard
        mapView.showsUserLocation = true
        mapView.isZoomEnabled = true
        return mapView
    }

    func updateUIView(_ mapView: MKMapView, context: Context) {
        if let location = locationManager.location, !regionSet {
            let region = MKCoordinateRegion(center: location,
                                            span: MKCoordinateSpan(latitudeDelta: 0.1, longitudeDelta: 0.1))
            mapView.setRegion(region, animated: true)
            DispatchQueue.main.async {
                self.regionSet = true
            }
        }

        mapView.removeAnnotations(mapView.annotations)

        var seenNames = Set<String>()

        for detail in mapDetails {
            guard !seenNames.contains(detail.nameCN) else { continue }
            seenNames.insert(detail.nameCN)

            let coord = CLLocationCoordinate2D(latitude: detail.gaoLat, longitude: detail.gaoLng)
            let annotation = StationAnnotation(coordinate: coord, title: detail.nameCN, lineString: detail.line)
            mapView.addAnnotation(annotation)
        }
    }

    @State private var regionSet = false
}

struct BottomCardView: View {
    let station: MetroStation?
    @Namespace private var animationNamespace
    @State private var isExpanded = false
    @State private var showDetails = false
    @State private var selectedLine: Int? = nil
    @State private var errorMessage: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let station = station {
                headerView(for: station)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 16)

                if isExpanded {
                    ZStack {
                        if showDetails {
                            StationDetailsView(nameCN: station.nameCN, isShowing: $showDetails)
                                .matchedGeometryEffect(id: "ExpandedView", in: animationNamespace)
                                .transition(.opacity.combined(with: .move(edge: .bottom)))
                        } else {
                            expandedContentView(for: station)
                                .matchedGeometryEffect(id: "ExpandedView", in: animationNamespace)
                                .transition(.opacity.combined(with: .move(edge: .bottom)))
                        }
                    }
                }
            } else {
                Text("SmartMetro Map")
                    .font(.headline)
                    .padding()
                    .frame(maxWidth: .infinity)
            }
        }
        .background(Color(.systemBackground).opacity(station == nil ? 0.8 : 1))
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .shadow(radius: 8)
        .padding(.horizontal)
        .padding(.bottom)
        .animation(.spring(), value: isExpanded)
    }

    private func headerView(for station: MetroStation) -> some View {
        Button(action: { withAnimation { isExpanded.toggle() } }) {
            HStack(alignment: .center, spacing: 18) {
                VStack(alignment: .leading, spacing: 5) {
                    Text(station.nameCN)
                        .font(.system(size: 21, weight: .bold))
                    Text(station.nameEN)
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 9) {
                    HStack(spacing: 8) {
                        ForEach(station.associatedLines, id: \.self) { line in
                            if let config = lineConfig[line] {
                                Button(action: {
                                    selectedLine = (selectedLine == line) ? nil : line  // 切换选中状态
                                }) {
                                    Text(config.name)
                                        .font(.system(size: 14, weight: .bold))
                                        .foregroundColor(.white)
                                        .frame(width: 28, height: 28)
                                        .background(
                                            Color(config.color)
                                                .opacity(selectedLine == line ? 1.0 : 0.5)
                                        )
                                        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 8)
                                                .stroke(selectedLine == line ? Color.white : .clear, lineWidth: 2)
                                        )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    Text("\(station.distanceM)m")
                        .font(.system(size: 11.5, weight: .medium))
                        .foregroundColor(.secondary)
                }
            }
            .padding(.vertical, 10)
        }
        .buttonStyle(.plain)
    }

    private func expandedContentView(for station: MetroStation) -> some View {
        VStack(spacing: 16) {
            Button("更多信息") {
                withAnimation {
                    showDetails = true
                }
            }
            .font(.caption)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(Color(.systemGray5))
            .clipShape(RoundedRectangle(cornerRadius: 6))

            Text("请选择要查看的线路")
                .foregroundColor(.secondary)
                .frame(maxWidth: .infinity, minHeight: 100)
        }
        .padding(.bottom, 16)
    }
}

// MARK: - Preview Wrapper / Entry Point
struct MapScreen: View {
    @StateObject private var locationManager = LocationManager()
    @State private var mapDetails: [MapDetail] = []
    @State private var allStations: [MetroStation] = [] // 假设你有加载这个数据
    @State private var selectedStation: MetroStation? = nil

    var body: some View {
        ZStack {
            MapView(locationManager: locationManager, mapDetails: mapDetails) { selectedName in
                if let station = allStations.first(where: { $0.nameCN == selectedName }) {
                    selectedStation = station
                }
            }
            .edgesIgnoringSafeArea(.all)
            .onAppear {
                MapDetailService.shared.fetchMapDetails { result in
                    switch result {
                    case .success(let details):
                        DispatchQueue.main.async {
                            self.mapDetails = details
                        }
                    case .failure(let error):
                        print("加载站点数据失败: \(error.localizedDescription)")
                    }
                }
            }

            VStack {
                Spacer()
                BottomCardView(station: selectedStation)
            }
        }
    }
}
