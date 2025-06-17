//
//  CheckpointResponse.swift
//  SmartMetro
//
//  Created by 张文瑜 on 19/4/25.
//

import Foundation

struct CongestionResponse: Codable {
    let statID: Int
    let nameCN: String
    let travelGroup: String
    let checkpoints: [Checkpoint]

    enum CodingKeys: String, CodingKey {
        case statID = "stat_id"
        case nameCN = "name_cn"
        case travelGroup = "travel_group"
        case checkpoints
    }
}

struct Checkpoint: Codable, Identifiable {
    let checkpointID: Int
    let id: [Int]
    let personNum: Int
    let createdAt: String

    enum CodingKeys: String, CodingKey {
        case checkpointID = "checkpoint_id"
        case id
        case personNum = "person_num"
        case createdAt = "created_at"
    }
}
