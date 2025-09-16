package com.thedevjade.flow.common.models

import kotlinx.serialization.Serializable

@Serializable
data class FileTreeNode(
    val name: String,
    val type: String, // "file" or "directory"
    val path: String,
    val children: List<FileTreeNode> = emptyList(),
    val size: Long? = null,
    val lastModified: Long? = null
)
