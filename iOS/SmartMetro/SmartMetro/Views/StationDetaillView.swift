//
//  StationDetailView.swift
//  SmartMetro
//
//  Created by 张文瑜 on 18/4/25.
//

import SwiftUI
import SwiftfulLoadingIndicators

struct StationDetailsView: View {
    let nameCN: String
    @Binding var isShowing: Bool

    @StateObject private var viewModel = StationDetailsViewModel()
    
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

    var groupedElevators: [Int: [Elevator]] {
        guard let station = viewModel.stationDetails else { return [:] }
        return Dictionary(grouping: station.elevators, by: { $0.line })
    }

    var uniqueToilets: [Toilet] {
        guard let station = viewModel.stationDetails else { return [] }
        // 用 description 去重
        var seen = Set<String>()
        return station.toilets.filter { seen.insert($0.description).inserted }
    }

    var sortedEntrances: [Entrance] {
        guard let station = viewModel.stationDetails else { return [] }
        return station.entrances.sorted {
            $0.entranceID.localizedStandardCompare($1.entranceID) == .orderedAscending
        }
    }

    var body: some View {
        VStack {
            HStack {
                Spacer()
                Button(action: {
                    withAnimation {
                        isShowing = false
                    }
                }) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.title2)
                        .foregroundColor(.secondary)
                }
                .padding(.trailing, 12)
                .padding(.top, 8)
            }

            ScrollView {
                if viewModel.isLoading {
                    LoadingIndicator(animation: .text)
                        .padding()
                } else if let _ = viewModel.stationDetails {
                    VStack(alignment: .leading, spacing: 16) {

                        ModernCard(title: "出入口信息") {
                            if sortedEntrances.isEmpty {
                                NoDataView(text: "未找到任何出入口，请查看站内地图")
                            } else if sortedEntrances.count > 10 {
                                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                                    ForEach(sortedEntrances, id: \.entranceID) { entrance in
                                        let cleanID = entrance.entranceID.replacingOccurrences(of: "号口", with: "")
                                        
                                        ZStack(alignment: .topLeading) {
                                            Color(.systemGray6)
                                                .clipShape(RoundedRectangle(cornerRadius: 16))
                                            
                                            VStack(alignment: .leading, spacing: 0) {
                                                Text("\(cleanID)号口")
                                                    .font(.headline)
                                                    .padding(.top, 12)
                                                    .padding(.bottom, 8)
                                                
                                                Text(entrance.description.isEmpty ? "暂无描述" : entrance.description)
                                                    .font(.caption)
                                                    .foregroundColor(.secondary)
                                                    .lineLimit(2)
                                                    .fixedSize(horizontal: false, vertical: true)
                                                    .padding(.bottom, 12)
                                            }
                                            .padding(.horizontal, 12)
                                            .frame(maxWidth: .infinity, alignment: .topLeading)
                                        }
                                        .frame(maxWidth: .infinity, minHeight: 100)
                                    }
                                }
                                .padding(.top, 8)
                            } else {
                                // 一列列表布局
                                VStack(spacing: 12) {
                                    ForEach(sortedEntrances, id: \.entranceID) { entrance in
                                        let cleanID = entrance.entranceID.replacingOccurrences(of: "号口", with: "")
                                        HStack(alignment: .firstTextBaseline, spacing: 12) { // 修改对齐方式
                                            Text("\(cleanID)号口")
                                                .font(.headline)
                                                .alignmentGuide(.firstTextBaseline) { d in
                                                    d[.firstTextBaseline] // 对齐到第一个文本基线
                                                }
                                                .frame(width: 80, alignment: .leading)

                                            Text(entrance.description.isEmpty ? "暂无描述" : entrance.description)
                                                .font(.caption)
                                                .foregroundColor(.secondary)
                                                .fixedSize(horizontal: false, vertical: true)
                                                .multilineTextAlignment(.leading) // 确保多行左对齐
                                                .frame(maxWidth: .infinity, alignment: .leading)
                                        }
                                        .frame(maxWidth: .infinity) // 确保填满宽度
                                        Divider()
                                    }
                                }
                                .padding(.top, 8)
                            }
                        }


                        ModernCard(title: "无障碍电梯") {
                            if groupedElevators.isEmpty {
                                NoDataView()
                            } else {
                                ForEach(groupedElevators.sorted(by: { $0.key < $1.key }), id: \.key) { line, elevators in
                                    // 获取线路配置，如果没有则使用默认值
                                    let config = lineConfig[line] ?? (color: Color.primary, bgColor: Color.clear, name: "\(line)")
                                    
                                    HStack(spacing: 8) {
                                        // Badge
                                        Text(config.name)
                                            .font(.system(size: 14, weight: .bold))
                                            .foregroundColor(.white)
                                            .frame(width: 28, height: 28)
                                            .background(config.color)
                                            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))

                                        // Line - 文本，颜色跟Badge一致
                                        Text("Line \(line)")
                                            .font(.headline)
                                            .foregroundColor(config.color)

                                        Spacer()
                                    }
                                    .padding(.bottom, -5)
                                    .frame(maxWidth: .infinity, alignment: .leading)

                                    let sortedElevators = elevators.sorted {
                                        let id0 = Int($0.description.split(separator: "#", maxSplits: 1).first ?? "") ?? 0
                                        let id1 = Int($1.description.split(separator: "#", maxSplits: 1).first ?? "") ?? 0
                                        return id0 < id1
                                    }

                                    VStack(spacing: 0) {
                                        ForEach(
                                            sortedElevators
                                                .map { elevator -> (String, String, String, String) in
                                                    let parts = elevator.description.split(separator: "#", maxSplits: 1).map(String.init)
                                                    let id = parts.first ?? ""
                                                    var rest = parts.count > 1 ? parts[1] : ""

                                                    var service = ""
                                                    if let start = rest.firstIndex(of: "（"),
                                                       let end = rest.firstIndex(of: "）"),
                                                       start < end {
                                                        service = String(rest[rest.index(after: start)..<end])
                                                        rest.removeSubrange(start...end)
                                                    }

                                                    let trimmed = rest.trimmingCharacters(in: .whitespaces)
                                                    let comps = trimmed.split(separator: " ", maxSplits: 1).map(String.init)
                                                    let type = comps.first ?? ""
                                                    var desc = comps.count > 1 ? comps[1] : ""
                                                    if let range = desc.range(of: "——") ?? desc.range(of: "—") {
                                                        desc = String(desc[range.upperBound...]).trimmingCharacters(in: .whitespacesAndNewlines)
                                                    }

                                                    return (id, type, desc, service)
                                                }
                                                .sorted { $0.0 < $1.0 },
                                            id: \.0
                                        ) { (id, type, desc, service) in
                                            VStack(alignment: .leading, spacing: 4) {
                                                Text("\(id)号梯")
                                                    .font(.subheadline.bold())
                                                    .foregroundColor(.primary)

                                                HStack {
                                                    Text(type)
                                                        .font(.caption)
                                                        .foregroundColor(.secondary)
                                                    Spacer()
                                                    if !service.isEmpty {
                                                        Text(service)
                                                            .font(.caption2)
                                                            .foregroundColor(.accentColor)
                                                            .padding(.horizontal, 6)
                                                            .padding(.vertical, 2)
                                                            .background(Color.accentColor.opacity(0.1))
                                                            .clipShape(Capsule())
                                                    }
                                                }

                                                Text(desc)
                                                    .font(.subheadline)
                                                    .foregroundColor(.primary)  // 改用主文本颜色
                                                    .fixedSize(horizontal: false, vertical: true)
                                                    .padding(.top, 6)
                                                    //.padding(.bottom, 2) //先不加了

                                                Divider()
                                                    .padding(.top, 8)
                                            }
                                            .padding(.vertical, 8)
                                        }
                                    }
                                }
                            }
                        }


                        ModernCard(title: "卫生间信息") {
                            if uniqueToilets.isEmpty {
                                NoDataView()
                            } else {
                                ToiletListView(toilets: uniqueToilets)
                            }
                        }
                    }
                    .padding(.vertical, 8)
                } else if viewModel.hasAttemptedLoad {
                    Text(viewModel.errorMessage ?? "加载失败")
                        .foregroundColor(.red)
                        .padding()
                }
            }
        }
        .navigationTitle("车站详情")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.fetchStationDetails(nameCN: nameCN)
        }
    }
    
    func parseElevatorDescription(_ raw: String) -> (String, String, String, String) {
        let parts = raw.split(separator: "#", maxSplits: 1).map(String.init)
        let id = parts.first ?? ""
        var rest = parts.count > 1 ? parts[1] : ""

        var service = ""
        if let start = rest.firstIndex(of: "（"),
           let end = rest.firstIndex(of: "）"),
           start < end {
            service = String(rest[rest.index(after: start)..<end])
            rest.removeSubrange(start...end)
        }

        let trimmed = rest.trimmingCharacters(in: .whitespaces)
        let comps = trimmed.split(separator: " ", maxSplits: 1).map(String.init)
        let type = comps.first ?? ""
        let desc = comps.count > 1 ? comps[1] : ""

        return (id, type, desc, service)
    }
    
    // MARK: - 厕所信息相关视图
    private struct ToiletListView: View {
        let toilets: [Toilet]

        /// 根据 line + description 去重
        private var filteredToilets: [Toilet] {
            var seen = Set<String>()
            return toilets.filter { toilet in
                let key = "\(toilet.line)-\(toilet.description)"
                return seen.insert(key).inserted
            }
        }

        var body: some View {
            Group {
                ForEach(filteredToilets, id: \.lineAndDescription) { toilet in
                    ToiletContainerView(toilet: toilet)
                }
            }
        }

    }

    private struct ToiletContainerView: View {
        let toilet: Toilet
        
        var body: some View {
            VStack(alignment: .leading, spacing: 8) {
                if let descriptionEN = toilet.descriptionEN {
                    ParsedToiletView(descriptionEN: descriptionEN, toilet: toilet)
                } else {
                    SimpleToiletView(toilet: toilet)
                }
            }
        }
    }

    // 只显示简单的 “Line X | 状态 | 描述”
    private struct SimpleToiletView: View {
        let toilet: Toilet

        private var statusText: String {
            let parts = toilet.description.components(separatedBy: " ")
            if parts.count >= 2, parts[0].contains("号线") {
                return parts[1]
            } else {
                return parts.first ?? ""
            }
        }

        private var chineseDesc: String {
            let parts = toilet.description.components(separatedBy: " ")
            if parts.count >= 2, parts[0].contains("号线") {
                return parts.dropFirst(2).joined(separator: " ")
            } else {
                return parts.dropFirst().joined(separator: " ")
            }
        }

        private var englishDesc: String {
            toilet.descriptionEN?
                .replacingOccurrences(of: "\r\n<br />", with: "")
                .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        }

        var body: some View {
            ToiletItemView(
                line: toilet.line,
                statusText: statusText,
                chineseDesc: chineseDesc,
                englishDesc: englishDesc
            )
        }
    }

    // 优先按行匹配英文，否则 fallback 为 default
    private struct ParsedToiletView: View {
        let descriptionEN: String
        let toilet: Toilet
        @State private var parsedItems: [ToiletDescItem] = []

        var statusText: String {
            let parts = toilet.description.components(separatedBy: " ")
            if parts.count >= 2, parts[0].contains("号线") {
                return parts[1]
            } else {
                return parts.first ?? ""
            }
        }

        var chineseDesc: String {
            let parts = toilet.description.components(separatedBy: " ")
            if parts.count >= 2, parts[0].contains("号线") {
                return parts.dropFirst(2).joined(separator: " ")
            } else {
                return parts.dropFirst().joined(separator: " ")
            }
        }

        var body: some View {
            Group {
                if let match = parsedItems.first(where: { $0.line == toilet.line }) {
                    ToiletItemView(
                        line: toilet.line,
                        statusText: statusText,
                        chineseDesc: chineseDesc,
                        englishDesc: match.desc
                    )
                } else {
                    SimpleToiletView(toilet: toilet)
                }
            }
            .onAppear {
                parsedItems = parseToiletDescription(descriptionEN)
            }
        }

        private func parseToiletDescription(_ input: String) -> [ToiletDescItem] {
            let clean = input
                .replacingOccurrences(of: "\r\n", with: ";")
                .replacingOccurrences(of: "\n", with: ";")
                .replacingOccurrences(of: "<br />", with: ";")
            let parts = clean
                .split(separator: ";")
                .map { String($0).trimmingCharacters(in: .whitespacesAndNewlines) }
                .filter { !$0.isEmpty }

            let pattern = #"(?i)Line\s*(\d+):?\s*(.+)"#
            let regex = try? NSRegularExpression(pattern: pattern)
            var results = [ToiletDescItem]()

            for part in parts {
                guard
                    let m = regex?.firstMatch(in: part, range: NSRange(part.startIndex..., in: part)),
                    let r1 = Range(m.range(at: 1), in: part),
                    let r2 = Range(m.range(at: 2), in: part),
                    let lineNum = Int(part[r1])
                else { continue }

                let desc = String(part[r2]).trimmingCharacters(in: .whitespacesAndNewlines)
                results.append(ToiletDescItem(line: lineNum, desc: desc))
            }
            return results
        }
    }


    // 新的 ItemView，使用 statusText
    private struct ToiletItemView: View {
        let line: Int
        let statusText: String
        let chineseDesc: String
        let englishDesc: String

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
            if let config = lineConfig[line] {
                VStack(alignment: .leading, spacing: 6) {
                    HStack(spacing: 8) {
                        Text(config.name)
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.white)
                            .frame(width: 28, height: 28)
                            .background(config.color)
                            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))

                        Text("Line \(line)")
                            .font(.headline)
                            .foregroundColor(config.color)

                        Spacer()

                        Text(statusText)
                            .font(.caption2)
                            .foregroundColor(.accentColor)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.accentColor.opacity(0.1))
                            .clipShape(Capsule())
                    }
                    .padding(.bottom, 6)

                    if !chineseDesc.isEmpty {
                        Text(chineseDesc)
                            .font(.subheadline)
                            .foregroundColor(.primary)
                    }

                    if !englishDesc.isEmpty && englishDesc != chineseDesc {
                        Text(englishDesc)
                            .font(.caption2)
                            .foregroundColor(.gray.opacity(0.8))
                    }

                    Divider()
                }
                .padding(.vertical, 4)
            } else {
                // fallback: line 未知时返回空视图
                EmptyView()
            }
        }
    }


    // MARK: - 数据模型
    private struct ToiletDescItem: Identifiable {
        let id = UUID()
        let line: Int
        let desc: String
    }

}

extension Toilet {
    var lineAndDescription: String {
        return "\(line)-\(description)"
    }
}

// MARK: - 卡片样式
struct ModernCard<Content: View>: View {
    let title: String
    let content: Content

    init(title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.headline)
                .padding(.horizontal)

            Divider()
                .background(Color.gray.opacity(0.4))

            content
                .padding(.horizontal)
        }
        .padding(.vertical, 12)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .shadow(color: Color.black.opacity(0.04), radius: 6, x: 0, y: 3)
        .padding(.horizontal)
    }
}

// MARK: - 无数据提示
struct NoDataView: View {
    var text: String = "暂无数据"
    var body: some View {
        Text(text)
            .font(.footnote)
            .foregroundColor(.secondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(Color(.systemGray6))
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .padding(.horizontal, 8)
    }
}

// MARK: - 模拟数据加载
struct MockDataProvider {
    static func getStationDetails(by nameCN: String) -> StationDetails? {
        if nameCN == "陆家嘴" {
            return StationDetails(
                nameCN: "陆家嘴",
                nameEN: "Lujiazui",
                statID: "247",
                elevators: [
                    Elevator(description: "1#无障碍电梯 地面-站厅——陆家嘴环路，近1号口（自助）", icon1: "", icon2: "", idAlias: nil, line: 2, nameCN: "", nameEN: "", statID: ""),
                    Elevator(description: "2#无障碍电梯 2号线站厅-换乘通道——（自助）", icon1: "", icon2: "", idAlias: nil, line: 2, nameCN: "", nameEN: "", statID: ""),
                    Elevator(description: "3#无障碍电梯 站厅-站台（自助）", icon1: "", icon2: "", idAlias: nil, line: 2, nameCN: "", nameEN: "", statID: ""),
                    Elevator(description: "1#无障碍电梯 站厅-站台——站厅中间，站台中部（自助）", icon1: "", icon2: "", idAlias: nil, line: 14, nameCN: "", nameEN: "", statID: ""),
                    Elevator(description: "2#无障碍电梯 站厅-国金商场平台层——近8号口（自助）", icon1: "", icon2: "", idAlias: nil, line: 14, nameCN: "", nameEN: "", statID: "")
                ],
                entrances: [
                    Entrance(description: "陆家嘴环路 世纪大道", entranceID: "1", icon1: "", icon2: "", idAlias: "1号口", memo: "R*", nameCN: "", nameEN: "", statID: "", status: 1),
                    Entrance(description: "花园石桥路", entranceID: "10", icon1: "", icon2: "", idAlias: "10号出入口", memo: "R*", nameCN: "", nameEN: "", statID: "", status: 1)
                ],
                toilets: [
                    Toilet(description: "费区内 往封浜方向车头", descriptionEN: "Line 14 Inside pay area, train head in the direction of Fengbang", icon1: "", icon2: "", line: 14, nameCN: "", nameEN: "", planCloseDate: nil, planOpenDate: nil, statID: "247", status: nil, toiletInside: 0)
                ]
            )
        }
        return nil
    }
}

struct StationDetailsView_Previews: PreviewProvider {
    struct PreviewWrapper: View {
        @State private var isShowing = true
        var body: some View {
            NavigationView {
                StationDetailsView(nameCN: "人民广场", isShowing: $isShowing)
            }
        }
    }

    static var previews: some View {
        PreviewWrapper()
    }
}
