package com.thedevjade.flow.common.models

import kotlinx.serialization.Serializable

@Serializable
data class ConnectionData(
    val id: String,
    val fromNode: String,
    val fromOutput: String,
    val toNode: String,
    val toInput: String
)
