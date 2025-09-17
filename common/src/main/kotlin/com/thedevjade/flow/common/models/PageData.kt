package com.thedevjade.flow.common.models

import kotlinx.serialization.Serializable

@Serializable
data class PageData(
    val id: String,
    val type: String,
    val title: String,
    val contentId: String,
    val lastModified: String
)
