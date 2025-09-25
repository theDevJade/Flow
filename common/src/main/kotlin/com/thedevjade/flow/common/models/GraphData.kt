package com.thedevjade.flow.common.models

import kotlinx.serialization.Serializable

@Serializable
data class GraphData(
    val id: String,
    var nodes: List<NodeData>,
    var connections: List<ConnectionData>,
    val lastModified: Long
)
