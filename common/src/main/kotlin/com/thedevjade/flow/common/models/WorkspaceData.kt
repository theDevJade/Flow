package com.thedevjade.flow.common.models

import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceData(
    val id: String,
    val name: String,
    var openPages: List<String>,
    var activePage: String?,
    val lastModified: String
)
