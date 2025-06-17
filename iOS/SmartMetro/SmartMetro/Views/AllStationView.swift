//
//  AllStationView.swift
//  SmartMetro
//
//  Created by 张文瑜 on 1/4/25.
//

import SwiftUI

struct StationQuery: View {
    @State private var expandedLine: Int?
    @StateObject private var metroStationService = MetroStationService()
    @State private var searchText: String = ""

    var body: some View {
        NavigationStack {
            VStack {
                searchBar
                
                ScrollView {
                    VStack(spacing: 10) {
                        ForEach(filteredLines, id: \.lineNumber) { line in
                            lineCard(line)
                        }
                    }
                    .padding()
                }
            }
            .navigationTitle("路线查询")
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                metroStationService.fetchStations()
            }
        }
    }
    
    /// 搜索栏
    private var searchBar: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.gray)
            TextField("搜索线路", text: $searchText)
                .textFieldStyle(.roundedBorder)
        }
        .padding(.horizontal)
    }
    
    /// 线路卡片
    private func lineCard(_ line: LineInfo) -> some View {
        let isExpanded = expandedLine == line.lineNumber
        let config = lineConfig[line.lineNumber] ?? (Color.gray, Color(.systemGray6), "\(line.lineNumber)号线")
        
        return VStack {
            // 标题栏
            Text(config.name)
                .font(.headline)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding()
                .background(config.color)
                .cornerRadius(10)
                .onTapGesture {
                    withAnimation(.easeInOut(duration: 0.3)) { // 平滑动画
                        expandedLine = isExpanded ? nil : line.lineNumber
                    }
                }
            
            // 站点列表（展开时显示）
            if isExpanded {
                VStack(spacing: 5) {
                    ForEach(line.allStations, id: \.self) { station in
                        Text(station)
                            .font(.body)
                            .padding(.vertical, 8)
                            .frame(maxWidth: .infinity)
                            .background(Color(.systemGray6))
                            .cornerRadius(8)
                    }
                }
                .padding(.top, 5)
                .padding(.horizontal)
                .transition(.opacity.combined(with: .scale(scale: 0.9, anchor: .top)))
            }
        }
    }

    /// 过滤搜索结果
    private var filteredLines: [LineInfo] {
        if searchText.isEmpty {
            return metroStationService.allLines
        } else {
            return metroStationService.allLines.filter {
                lineConfig[$0.lineNumber]?.name.contains(searchText) == true
            }
        }
    }

    // 线路配置
    private let lineConfig: [Int: (color: Color, bgColor: Color, name: String)] = [
        1: (Color(hex: "e3002b"), Color(hex: "fdeae9"), "1号线"),
        2: (Color(hex: "8cc220"), Color(hex: "EBF7EC"), "2号线"),
        3: (Color(hex: "fcd600"), Color(hex: "fffee5"), "3号线"),
        4: (Color(hex: "461d84"), Color(hex: "f1ebf4"), "4号线"),
        5: (Color(hex: "944d9a"), Color(hex: "e8d2f0"), "5号线"),
        6: (Color(hex: "d40068"), Color(hex: "ffcae4"), "6号线"),
        7: (Color(hex: "ed6f00"), Color(hex: "ffcc99"), "7号线"),
        8: (Color(hex: "0094d8"), Color(hex: "60b7d4"), "8号线"),
        9: (Color(hex: "87caed"), Color(hex: "85C6DA"), "9号线"),
        10: (Color(hex: "c6afd4"), Color(hex: "e0c5f0"), "10号线"),
        11: (Color(hex: "871c2b"), Color(hex: "BB8866"), "11号线"),
        12: (Color(hex: "007a60"), Color(hex: "99CBC1"), "12号线"),
        13: (Color(hex: "e999c0"), Color(hex: "f4b8d2"), "13号线"),
        14: (Color(hex: "616020"), Color(hex: "9a982f"), "14号线"),
        15: (Color(hex: "c8b38e"), Color(hex: "f9e7c8"), "15号线"),
        16: (Color(hex: "98d1c0"), Color(hex: "C6E8DF"), "16号线"),
        17: (Color(hex: "bb796f"), Color(hex: "ebd6d3"), "17号线"),
        18: (Color(hex: "C09453"), Color(hex: "C09453"), "18号线"),
        41: (Color(hex: "b5b6b6"), Color(hex: "f2f7f7"), "浦江线"),
        51: (Color(hex: "cccccc"), Color(hex: "dddddd"), "机场联络线")
    ]
}
