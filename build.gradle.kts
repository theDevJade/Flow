plugins {
    kotlin("jvm") version "2.2.20" apply false
}


tasks.register("testBuild") {
    group = "verification"
    description = "Build all backend modules without running tests"
    dependsOn(":common:build", ":flow:build", ":webserver:build", ":plugin:build")
}


tasks.register("runWebSocketServer") {
    group = "application"
    description = "Run the WebSocket server from root"
    dependsOn(":flow:runWebSocketServer")
}


tasks.register<Exec>("flutterAnalyze") {
    group = "verification"
    description = "Run Flutter analyze on the frontend"
    workingDir = file("flow_frontend")
    commandLine("flutter", "analyze")
}


tasks.register("fullTestBuild") {
    group = "verification"
    description = "Run complete test build - backend build + Flutter analyze"
    dependsOn("testBuild", "flutterAnalyze")
}
