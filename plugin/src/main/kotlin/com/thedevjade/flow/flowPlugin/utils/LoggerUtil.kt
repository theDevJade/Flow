package com.thedevjade.flow.flowPlugin.utils

import com.thedevjade.flow.flowPlugin.Flow
import java.util.logging.Logger

fun logger(): Logger {
    return Flow.instance.logger
}

fun info(t: Any) {
    logger().info(t.toString())
}

fun warning(t: Any) {
    logger().warning(t.toString())
}

fun error(t: Any) {
    logger().severe(t.toString())
}

