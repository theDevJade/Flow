package com.thedevjade.flow.common.models

import kotlinx.serialization.Serializable

@Serializable
data class PageData(
    val id: String,
    val type: String, // "graph", "file", "terminal"
    val title: String,
    val contentId: String, // graphId or filePath
    val lastModified: String
)
