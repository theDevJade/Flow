package com.thedevjade.flow.common.models

import kotlinx.serialization.Serializable

@Serializable
data class NodeData(
    val id: String,
    val type: String,
    val position: Position,
    val data: Map<String, String>
)

@Serializable
data class Position(
    val x: Double,
    val y: Double
)
