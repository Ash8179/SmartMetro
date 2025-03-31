import SwiftUI

// MARK: - 流式布局组件
struct FlexibleView<Data: Collection, Content: View>: View where Data.Element: Hashable {
    let data: Data
    let spacing: CGFloat
    let alignment: HorizontalAlignment
    let content: (Data.Element) -> Content
    @State private var availableWidth: CGFloat = 0
    
    var body: some View {
        ZStack(alignment: Alignment(horizontal: alignment, vertical: .center)) {
            Color.clear
                .frame(height: 1)
                .readSize { size in
                    availableWidth = size.width
                }
            
            _FlexibleView(
                availableWidth: availableWidth,
                data: data,
                spacing: spacing,
                alignment: alignment,
                content: content
            )
        }
    }
}

private struct _FlexibleView<Data: Collection, Content: View>: View where Data.Element: Hashable {
    let availableWidth: CGFloat
    let data: Data
    let spacing: CGFloat
    let alignment: HorizontalAlignment
    let content: (Data.Element) -> Content
    @State var elementsSize: [Data.Element: CGSize] = [:]
    
    var body : some View {
        VStack(alignment: alignment, spacing: spacing) {
            ForEach(computeRows(), id: \.self) { rowElements in
                HStack(spacing: spacing) {
                    ForEach(rowElements, id: \.self) { element in
                        content(element)
                            .fixedSize()
                            .readSize { size in
                                elementsSize[element] = size
                            }
                    }
                }
            }
        }
    }
    
    func computeRows() -> [[Data.Element]] {
        var rows: [[Data.Element]] = [[]]
        var currentRow = 0
        var remainingWidth = availableWidth
        
        for element in data {
            let elementSize = elementsSize[element, default: CGSize(width: availableWidth, height: 1)]
            
            if remainingWidth - (elementSize.width + spacing) >= 0 {
                rows[currentRow].append(element)
            } else {
                currentRow += 1
                rows.append([element])
                remainingWidth = availableWidth
            }
            
            remainingWidth -= (elementSize.width + spacing)
        }
        
        return rows
    }
}

// MARK: - 视图尺寸读取扩展
extension View {
    func readSize(onChange: @escaping (CGSize) -> Void) -> some View {
        background(
            GeometryReader { geometry in
                Color.clear
                    .preference(key: SizePreferenceKey.self, value: geometry.size)
            }
        )
        .onPreferenceChange(SizePreferenceKey.self, perform: onChange)
    }
}

private struct SizePreferenceKey: PreferenceKey {
    static var defaultValue: CGSize = .zero
    static func reduce(value: inout CGSize, nextValue: () -> CGSize) {
        value = nextValue()
    }
}

// MARK: - 颜色扩展
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var rgbValue: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&rgbValue)
        
        let r = Double((rgbValue & 0xFF0000) >> 16) / 255.0
        let g = Double((rgbValue & 0x00FF00) >> 8) / 255.0
        let b = Double(rgbValue & 0x0000FF) / 255.0
        
        self.init(red: r, green: g, blue: b)
    }
}

// MARK: - 站点行视图
struct StationRow: View {
    let station: MetroStation
    @State private var showLineStations = false
    
    // 线路配置（保持不变）
    private let lineConfig: [Int: (color: Color, bgColor: Color, name: String)] = [
        1: (Color(hex: "e3002b"), Color(hex: "fdeae9"), "1"),
        2: (Color(hex: "8cc220"), Color(hex: "EBF7EC"), "2"),
        3: (Color(hex: "fcd600"), Color(hex: "fffee5"), "3"),
        4: (Color(hex: "461d84"), Color(hex: "f1ebf4"), "4"),
        5: (Color(hex: "944d9a"), Color(hex: "e8d2f0"), "5"),
        6: (Color(hex: "d40068"), Color(hex: "ffcae4"), "6"),
        7: (Color(hex: "ed6f00"), Color(hex: "ffcc99"), "7"),
        8: (Color(hex: "0094d8"), Color(hex: "60b7d4"), "8"),
        9: (Color(hex: "87caed"), Color(hex: "85C6DA"), "9"),
        10: (Color(hex: "c6afd4"), Color(hex: "e0c5f0"), "10"),
        11: (Color(hex: "871c2b"), Color(hex: "BB8866"), "11"),
        12: (Color(hex: "007a60"), Color(hex: "99CBC1"), "12"),
        13: (Color(hex: "e999c0"), Color(hex: "f4b8d2"), "13"),
        14: (Color(hex: "616020"), Color(hex: "9a982f"), "14"),
        15: (Color(hex: "c8b38e"), Color(hex: "f9e7c8"), "15"),
        16: (Color(hex: "98d1c0"), Color(hex: "C6E8DF"), "16"),
        17: (Color(hex: "bb796f"), Color(hex: "ebd6d3"), "17"),
        18: (Color(hex: "C09453"), Color(hex: "C09453"), "18"),
        41: (Color(hex: "b5b6b6"), Color(hex: "f2f7f7"), "浦江线"),
        51: (Color(hex: "cccccc"), Color(hex: "dddddd"), "机场联络线")
    ]
    
    private var stationLines: [Int] {
        station.associatedLines
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 6) {
                    Text(station.nameCN)
                        .font(.system(size: 20, weight: .semibold))
                    Text(station.nameEN)
                        .font(.system(size: 14))
                        .foregroundColor(.gray)
                }
                
                Spacer()

                VStack(alignment: .trailing) {
                    HStack(spacing: 6) {
                        ForEach(stationLines, id: \.self) { lineNumber in
                            if let config = lineConfig[lineNumber] {
                                Text(config.name)
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(.white)
                                    .frame(width: 32, height: 32)
                                    .background(
                                        RoundedRectangle(cornerRadius: 6)
                                            .fill(config.color)
                                    )
                            }
                        }
                    }
                    
                    Text("\(station.distanceM)m") 
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(.secondary)
                        .padding(.top, 1) // 添加间距为1
                }
            }
            
            // 线路站点信息
            if showLineStations, let firstLine = stationLines.first {
                VStack(alignment: .leading, spacing: 8) {
                    Text("\(lineConfig[firstLine]?.name ?? "")号线全程站点")
                        .font(.subheadline)
                        .foregroundColor(.gray)
                    
                    FlexibleView(
                        data: station.lineInfo.allStations,
                        spacing: 8,
                        alignment: .leading
                    ) { stationName in
                        let bgColor = lineConfig[firstLine]?.bgColor ?? Color.gray
                        return Text(stationName)
                            .font(.caption)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(
                                RoundedRectangle(cornerRadius: 4)
                                    .fill(bgColor.opacity(0.3))
                            )
                    }
                }
                .transition(.opacity)
            }
        }
        .padding(.vertical, 12)
        .onTapGesture {
            withAnimation(.spring()) {
                showLineStations.toggle()
            }
        }
    }
}

// MARK: - test
struct StationRow_Previews: PreviewProvider {
    static var transferStation: MetroStation = MetroStation(
        id: 1062,
        nameCN: "四平路",
        nameEN: "Siping Road",
        travelGroup: "189",
        distanceM: 500,
        lineInfo: LineInfo(
            lineNumber: 8,
            allStations: ["市光路", "嫩江路", "翔殷路", "黄兴公园", "延吉中路", "黄兴路", "江浦路", "鞍山新村", "四平路", "曲阳路", "虹口足球场"]
        ),
        associatedLines: [8, 10]
    )
    
    static var singleLineStation: MetroStation = MetroStation(
        id: 1063,
        nameCN: "同济大学",
        nameEN: "Tongji University",
        travelGroup: "244",
        distanceM: 300,
        lineInfo: LineInfo(
            lineNumber: 10,
            allStations: ["虹桥火车站", "虹桥2号航站楼", "虹桥1号航站楼", "上海动物园", "龙溪路", "水城路", "伊犁路", "宋园路", "虹桥路"]
        ),
        associatedLines: [10]
    )
    
    static var previews: some View {
        Group {
            StationRow(station: transferStation)
                .previewDisplayName("换乘站")
            
            StationRow(station: singleLineStation)
                .previewDisplayName("单线路站")
            
        }
        .previewLayout(.sizeThatFits)
        .padding()
    }
}
