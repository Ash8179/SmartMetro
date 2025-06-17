//
//  PassengerRunnerGameView.swift
//  SmartMetro
//
//  Created by 张文瑜 on 27/4/25.
//

import SwiftUI
import font

struct PassengerRunnerGameView: View {
    // MARK: - State
    @State private var passengerY: CGFloat = 0
    @State private var velocity: CGFloat = 0
    @State private var obstacles: [Obstacle] = []
    @State private var isGameOver = false
    @State private var distance: Int = 0
    @State private var currentFrame: Int = 1
    @State private var frameTick: Int = 0
    @State private var gameAreaSize: CGSize = .zero
    @State private var nextObstacleTick: Int = 0
    @State private var hasStarted = false
    @State private var nextObstacleSpawnTick: Int = 100
    @State private var highScore: Int = 0

    // MARK: - Constants
    private let gravity: CGFloat = 1.4
    private let jumpStrength: CGFloat = -20
    private let groundHeight: CGFloat = 40
    private let passengerSize = CGSize(width: 40, height: 40)

    private let baseObstacleSpeed: CGFloat = 5.5
    private let baseSpawnMinInterval: Int = 100
    private let baseSpawnMaxInterval: Int = 160

    private let obstacleScales: [String: CGFloat] = [
        "Stop Sign": 0.15,
        "Traffic Cone": 0.11,
        "Trash Bin": 0.10
    ]

    private let obstacleBaseSizes: [String: CGSize] = [
        "Stop Sign": CGSize(width: 177 * 0.18, height: 374 * 0.18),
        "Traffic Cone": CGSize(width: 157 * 0.11, height: 284 * 0.11),
        "Trash Bin": CGSize(width: 275 * 0.10, height: 379 * 0.10)
    ]

    var body: some View {
        GeometryReader { outerGeo in
            VStack {
                GeometryReader { geo in
                    ZStack {
                        RoundedRectangle(cornerRadius: 20)
                            .fill(Color.white)
                            .shadow(color: Color.black.opacity(0.1), radius: 8, x: 0, y: 4)
                        
                        gameElements(geo: geo)
                            .clipShape(RoundedRectangle(cornerRadius: 20))
                    }
                    .onAppear { gameAreaSize = geo.size }
                }
                .aspectRatio(1.6, contentMode: .fit)
                .frame(width: outerGeo.size.width * 0.8)
                .padding(.top, 50)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        }
        .background(Color.clear)
        .onTapGesture {
            if !hasStarted {
                hasStarted = true
                impactFeedback(.light)
            } else if !isGameOver && passengerY == 0 {
                velocity = jumpStrength
                impactFeedback(.light)
            }
        }
        .onAppear { startGame() }
        .onReceive(Timer.publish(every: 1/60, on: .main, in: .common).autoconnect()) { _ in
            guard hasStarted, !isGameOver else { return }
            frameTick += 1
            updatePassenger()
            updateObstacles()
            checkCollision()

            if frameTick % 6 == 0 {
                distance += 1
                currentFrame = currentFrame % 8 + 1
            }

            if frameTick >= nextObstacleSpawnTick {
                spawnObstacle()
                scheduleNextObstacle()
            }
        }
    }

    // MARK: - View Components
    @ViewBuilder
    private func gameElements(geo: GeometryProxy) -> some View {
        ZStack(alignment: .topLeading) {
            groundLine(geo: geo)
            obstacleElements(geo: geo)
            playerCharacter(geo: geo)
            distanceLabel
            gameOverOverlay
            startOverlay
        }
    }

    private var distanceLabel: some View {
        HStack {
            Text("\(distance)m")
                .font(.custom("Retro Gaming", size: 20))
                .foregroundColor(.black)
            Spacer()
        // MARK: - Best Score
            /*
            Text("Best: \(highScore)m")
                .font(.custom("Retro Gaming", size: 18))
                .foregroundColor(.black)
             */
        }
        .padding(.horizontal, 16)
        .padding(.top, 16)
    }

    private func groundLine(geo: GeometryProxy) -> some View {
        ZStack(alignment: .top) {
            Rectangle()
                .fill(Color(red: 0.3, green: 0.3, blue: 0.3))
                .frame(height: groundHeight)
                .position(x: geo.size.width/2, y: geo.size.height - groundHeight/2)

            HStack(spacing: 20) {
                ForEach(0..<10) { _ in
                    Rectangle()
                        .fill(Color.yellow)
                        .frame(width: 30, height: 4)
                }
            }
            .position(x: geo.size.width/2, y: geo.size.height - groundHeight/2)
            .offset(y: 8)
        }
    }

    private func playerCharacter(geo: GeometryProxy) -> some View {
        Image("runner\(currentFrame)")
            .resizable()
            .interpolation(.none)
            .frame(width: passengerSize.width, height: passengerSize.height)
            .position(
                x: geo.size.width * 0.2,
                y: geo.size.height - groundHeight - passengerSize.height/2 + passengerY
            )
    }

    private func obstacleElements(geo: GeometryProxy) -> some View {
        ForEach(obstacles) { obs in
            Image(obs.type)
                .resizable()
                .interpolation(.none)
                .frame(width: obs.width, height: obs.height)
                .position(
                    x: obs.x,
                    y: geo.size.height - groundHeight - obs.height/2
                )
        }
    }

    private var gameOverOverlay: some View {
        Group {
            if isGameOver {
                VStack(spacing: 12) {
                    Text("GAME OVER")
                        .font(.custom("Retro Gaming", size: 24))
                        .foregroundColor(.black)

                    Text("SCORE: \(distance)m")
                        .font(.custom("Retro Gaming", size: 16))
                        .foregroundColor(.black)

                    Button(action: restartGame) {
                        Text("RESTART")
                            .font(.custom("Retro Gaming", size: 18))
                            .padding(.vertical, 8)
                            .padding(.horizontal, 20)
                            .background(Color.white)
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(Color.black, lineWidth: 2)
                            )
                    }
                    .buttonStyle(PlainButtonStyle())
                }
                .padding(24)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color.white)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(Color.black, lineWidth: 2)
                        )
                        .shadow(color: .black.opacity(0.2), radius: 8, x: 0, y: 4)
                )
                .transition(.scale.combined(with: .opacity))
                .position(x: gameAreaSize.width/2, y: gameAreaSize.height/2)
            }
        }
    }

    private var startOverlay: some View {
        Group {
            if !hasStarted {
                VStack(spacing: 12) {
                    Text("TAP TO START")
                        .font(.custom("Retro Gaming", size: 24))
                        .foregroundColor(.black)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 12)
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color.white.opacity(0.9))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(Color.black.opacity(0.9), lineWidth: 2)
                                )
                        )
                        .shadow(color: .black.opacity(0.2), radius: 8, x: 0, y: 4)
                }
                .position(x: gameAreaSize.width/2, y: gameAreaSize.height/2)
            }
        }
    }

    // MARK: - Game Logic
    private func startGame() {
        passengerY = 0
        velocity = 0
        obstacles = []
        isGameOver = false
        distance = 0
        currentFrame = 1
        frameTick = 0
        hasStarted = false
        nextObstacleSpawnTick = baseSpawnMinInterval
    }

    private func restartGame() {
        impactFeedback(.medium)
        startGame()
    }

    private func updatePassenger() {
        velocity += gravity
        passengerY += velocity
        if passengerY > 0 {
            passengerY = 0
            velocity = 0
        }

        if distance > highScore {
            highScore = distance
        }
    }

    private func updateObstacles() {
        let currentSpeed = baseObstacleSpeed + CGFloat(distance) * 0.01
        obstacles = obstacles.map { var o = $0; o.x -= currentSpeed; return o }
        obstacles.removeAll { $0.x < -$0.width }
    }

    private func spawnObstacle() {
        let types = ["Stop Sign", "Traffic Cone", "Trash Bin"]
        guard let selectedType = types.randomElement(),
              let baseSize = obstacleBaseSizes[selectedType] else { return }

        let newObstacle = Obstacle(
            x: gameAreaSize.width + baseSize.width/2,
            width: baseSize.width,
            height: baseSize.height,
            type: selectedType
        )
        obstacles.append(newObstacle)
    }

    private func scheduleNextObstacle() {
        let difficultyFactor = min(CGFloat(distance) / 500, 2.0) // 难度增长更缓慢
        let minInterval = max(Int(CGFloat(baseSpawnMinInterval) / (1 + difficultyFactor)), 50)
        let maxInterval = max(Int(CGFloat(baseSpawnMaxInterval) / (1 + difficultyFactor)), 100)

        nextObstacleSpawnTick = frameTick + Int.random(in: minInterval...maxInterval)
    }

    private func checkCollision() {
        guard gameAreaSize != .zero else { return }

        let passengerRect = CGRect(
            x: gameAreaSize.width * 0.2 - passengerSize.width/2,
            y: gameAreaSize.height - groundHeight - passengerSize.height + passengerY,
            width: passengerSize.width,
            height: passengerSize.height
        )

        for obs in obstacles {
            let obstacleRect = CGRect(
                x: obs.x - obs.width/2,
                y: gameAreaSize.height - groundHeight - obs.height,
                width: obs.width,
                height: obs.height
            )

            if passengerRect.insetBy(dx: 6, dy: 6)
                .intersects(obstacleRect.insetBy(dx: 6, dy: 6)) {
                isGameOver = true
                impactFeedback(.heavy)
                break
            }
        }
    }

    private func impactFeedback(_ style: UIImpactFeedbackGenerator.FeedbackStyle) {
        let generator = UIImpactFeedbackGenerator(style: style)
        generator.impactOccurred()
    }
}

struct Obstacle: Identifiable {
    let id = UUID()
    var x: CGFloat
    var width: CGFloat
    var height: CGFloat
    var type: String
}

struct PassengerRunnerGameView_Previews: PreviewProvider {
    static var previews: some View {
        PassengerRunnerGameView()
            .previewLayout(.sizeThatFits)
            .padding()
    }
}
