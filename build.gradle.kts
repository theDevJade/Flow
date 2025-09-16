plugins {
    kotlin("jvm") version "2.2.20" apply false
}

// Task to build all modules (backend test build)
tasks.register("testBuild") {
    group = "verification"
    description = "Build all backend modules without running tests"
    dependsOn(":common:build", ":flow:build", ":webserver:build", ":plugin:build")
}

// Task to run WebSocket server (delegates to webserver module)
tasks.register("runWebSocketServer") {
    group = "application"
    description = "Run the WebSocket server from root"
    dependsOn(":flow:runWebSocketServer")
}

// Task to analyze Flutter code
tasks.register<Exec>("flutterAnalyze") {
    group = "verification"
    description = "Run Flutter analyze on the frontend"
    workingDir = file("flow_frontend")
    commandLine("flutter", "analyze")
}

// Task to run both backend test build and Flutter analyze
tasks.register("fullTestBuild") {
    group = "verification"
    description = "Run complete test build - backend build + Flutter analyze"
    dependsOn("testBuild", "flutterAnalyze")
}
