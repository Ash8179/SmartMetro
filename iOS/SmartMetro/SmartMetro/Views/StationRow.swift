import SwiftUI

// MARK: - 流式布局组件
struct FlexibleView<Data: Collection, Content: View>: View where Data.Element: Hashable {
    let data: Data
    let spacing: CGFloat
    let alignment: HorizontalAlignment
    let content: (Data.Element) -> Content
    @State private var availableWidth: CGFloat = 0
    @State private var showingDetail = false
    
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
    @State private var isExpanded = false
    @State private var selectedLine: Int? = nil
    @State private var upCrowdingData: [CarriageCrowding] = []
    @State private var downCrowdingData: [CarriageCrowding] = []
    @State private var trainArrivals: [TrainArrival] = []
    @State private var isLoading = false
    @State private var errorMessage: String?
    @Namespace private var animationNamespace
    
    // 线路颜色配置
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
    
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            headerView
                .padding(.horizontal, 20)
                .padding(.vertical, 16)

            if isExpanded {
                expandedContentView
                    .matchedGeometryEffect(id: "ExpandedView", in: animationNamespace)
                    .transition(.opacity.combined(with: .move(edge: .bottom)))
            }
        }
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .shadow(color: Color.black.opacity(0.1), radius: 8, x: 0, y: 4)
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }

    // MARK: - Header
    private var headerView: some View {
        Button(action: toggleExpansion) {
            HStack(alignment: .center, spacing: 18) {
                VStack(alignment: .leading, spacing: 5) {
                    Text(station.nameCN)
                        .font(.system(size: 21, weight: .bold))
                        .foregroundColor(.primary)
                        .lineLimit(1)

                    Text(station.nameEN)
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                VStack(alignment: .trailing, spacing: 9) {
                    HStack(spacing: 8) {  // 徽章间距 +1
                        ForEach(station.associatedLines, id: \.self) { line in
                            if let config = lineConfig[line] {
                                Text(config.name)
                                    .font(.system(size: 14, weight: .bold))  // 字体+1
                                    .foregroundColor(.white)
                                    .frame(width: 28, height: 28)            // 徽章尺寸+4
                                    .background(config.color)
                                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))  // 圆角同步加大
                            }
                        }
                    }

                    Text("\(station.distanceM)m")
                        .font(.system(size: 11.5, weight: .medium))
                        .foregroundColor(.secondary)
                }
            }
            .padding(.vertical, 10)
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle())
    }
    
    private var expandedContentView: some View {
        VStack(spacing: 16) {
            // 线路选择器
            lineSelectorView
            
            // 选中的线路详情
            if let line = selectedLine {
                if let errorMessage = errorMessage {
                    ErrorView(message: errorMessage) {
                        loadData(for: line)
                    }
                    .padding(.horizontal)
                } else {
                    lineDetailView(for: line)
                        .padding(.horizontal, 12)
                }
            } else {
                Text("请选择要查看的线路")
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, minHeight: 100)
            }
        }
        .padding(.bottom, 16)
    }
    
    private var lineSelectorView: some View {
        HStack(spacing: 12) {
            ForEach(station.associatedLines, id: \.self) { line in
                Button(action: {
                    selectedLine = line
                    loadData(for: line)
                }) {
                    if let config = lineConfig[line] {
                        Text(config.name)
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(selectedLine == line ? .white : config.color)
                            .frame(width: 48, height: 36) // fixed size for all buttons
                            .background(selectedLine == line ? config.color : Color(.systemGray5))
                            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    }
                }
                .buttonStyle(PlainButtonStyle())
            }
        }
        .frame(maxWidth: .infinity)  // Stretch container to center
        .padding(.horizontal, 20)
    }
    
    private func lineDetailView(for line: Int) -> some View {
        VStack(spacing: 16) {
            if isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, minHeight: 150)
            } else {
                if !upCrowdingData.isEmpty {
                    crowdingCard(for: "上行", data: upCrowdingData)
                }
                if !downCrowdingData.isEmpty {
                    crowdingCard(for: "下行", data: downCrowdingData)
                }
                if !trainArrivals.isEmpty {
                    arrivalCard
                }
            }
        }
    }
    
    private func crowdingCard(for path: String, data: [CarriageCrowding]) -> some View {
        let primaryColor = lineConfig[selectedLine ?? 1]?.color ?? .blue
        _ = lineConfig[selectedLine ?? 1]?.bgColor ?? Color(.systemGray6)

        return VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("最近\(path)列车")
                    .font(.headline)
                    .foregroundColor(.primary)

                Spacer()

                if let averageLevel = averageCrowdingLevel(for: path) {
                    CrowdLevelBadge(level: averageLevel)
                }
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    Image(systemName: "train.side.rear.car")
                        .font(.system(size: 24))
                        .foregroundColor(primaryColor)

                    ForEach(1...8, id: \.self) { index in
                        let carriage = data.first(where: { $0.line_carriage == index })

                        VStack(spacing: 6) {
                            Text("\(index)")
                                .font(.caption)
                                .foregroundColor(.secondary)

                            ZStack {
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(Color(.systemBackground))
                                    .shadow(color: .black.opacity(0.05), radius: 1, x: 0, y: 1)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(primaryColor, lineWidth: 1.2)
                                    )
                                    .frame(width: 48, height: 36)

                                if let carriage = carriage {
                                    CrowdLevelBadge(level: carriage.crowd_level)
                                        .scaleEffect(0.8)
                                } else {
                                    Image(systemName: "questionmark.circle")
                                            .font(.system(size: 14))
                                            .foregroundColor(.gray)
                                }
                            }

                            if let carriage = carriage {
                                HStack(spacing: 2) {
                                    Image(systemName: "person.fill")
                                        .font(.caption2)
                                    Text("\(carriage.person_num)")
                                        .font(.caption2)
                                        .foregroundColor(.secondary)
                                }
                            } else {
                                Text("--")
                                    .font(.caption2)
                                    .foregroundColor(.gray)
                            }
                        }
                    }

                    Image(systemName: "train.side.front.car")
                        .font(.system(size: 24))
                        .foregroundColor(primaryColor)
                }
                .padding(.vertical, 8)
            }
        }
        .padding(16)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
    
    private var arrivalCard: some View {
        ArrivalCard(arrivals: trainArrivals)
    }
    
    // MARK: - 组件
    private func lineBadge(line: Int, config: (color: Color, bgColor: Color, name: String), isSelected: Bool) -> some View {
        Text(config.name)
            .font(.system(size: 15, weight: .bold))
            .foregroundColor(isSelected ? .white : config.color)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(
                RoundedRectangle(cornerRadius: 6)
                    .fill(isSelected ? config.color : config.bgColor)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 6)
                    .stroke(config.color, lineWidth: 1)
            )
    }
    
    // MARK: - 操作方法
    private func toggleExpansion() {
        withAnimation(.spring()) {
            isExpanded.toggle()
            if isExpanded && selectedLine == nil {
                selectedLine = station.associatedLines.first
            } else if !isExpanded {
                selectedLine = nil
                upCrowdingData = []
                downCrowdingData = []
                trainArrivals = []
                errorMessage = nil
            }
        }
    }
    
    private func selectLine(_ line: Int) {
        withAnimation {
            selectedLine = line
            loadData(for: line)
        }
    }
    
    private func loadData(for line: Int) {
        isLoading = true
        upCrowdingData = []
        downCrowdingData = []
        trainArrivals = []
        errorMessage = nil
        
        Task {
            do {
                async let crowdingTask = MetroAPIService.shared.fetchCrowding(for: line)
                async let arrivalsTask = MetroAPIService.shared.fetchNextTrains(for: station.nameCN)
                
                let (crowdingDict, arrivalsResponse) = await (try crowdingTask, try arrivalsTask)
                
                // 提取上下行拥挤度数据
                let upData = crowdingDict["path_0"] ?? []
                let downData = crowdingDict["path_1"] ?? []
                
                // 获取到站时间数据 - 合并上下行
                let lineKey = "Line_\(line)"
                guard let lineArrivals = arrivalsResponse.lines[lineKey] else {
                    throw NSError(domain: "", code: 0, userInfo: [NSLocalizedDescriptionKey: "未找到线路\(line)的列车信息"])
                }
                
                // 合并上下行列车数据
                let allArrivals = lineArrivals.up_direction + lineArrivals.down_direction
                
                withAnimation {
                    self.upCrowdingData = upData
                    self.downCrowdingData = downData
                    self.trainArrivals = allArrivals
                    self.isLoading = false
                }
                
                // 调试打印
                print("拥挤度数据 - 上行: \(upData.count)条, 下行: \(downData.count)条")
                print("到站时间数据 - 上行: \(lineArrivals.up_direction.count)班, 下行: \(lineArrivals.down_direction.count)班")
                print("合并后列车总数: \(allArrivals.count)班")
                
            } catch {
                withAnimation {
                    self.isLoading = false
                    self.errorMessage = "数据加载失败: \(error.localizedDescription)"
                }
                print("数据加载错误: \(error)")
            }
        }
    }
    
    // MARK: - 计算属性
    private func averageCrowdingLevel(for path: String) -> Int? {
        var data: [CarriageCrowding] = []
        
        // 根据 path 选择对应的数据
        if path == "up" {
            data = upCrowdingData
        } else if path == "down" {
            data = downCrowdingData
        }
        
        // 计算平均值
        guard !data.isEmpty else { return nil }
        let total = data.reduce(0) { $0 + $1.crowd_level }
        return total / data.count
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
