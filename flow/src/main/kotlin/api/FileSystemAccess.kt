package flow.api

import com.thedevjade.flow.common.models.FileTreeNode
import java.nio.file.Path

interface FileSystemAccess {

    fun setRootDirectory(path: Path): Boolean
    fun addAllowedDirectory(path: Path): Boolean
    fun removeAllowedDirectory(path: Path): Boolean
    fun isPathAllowed(path: Path): Boolean
    fun getRootDirectory(): Path?
    fun getAllowedDirectories(): Set<Path>


    suspend fun getFileTree(rootPath: Path? = null): FileTreeNode?
    suspend fun readFile(filePath: Path): String?
    suspend fun writeFile(filePath: Path, content: String): Boolean
    suspend fun createFile(filePath: Path): Boolean
    suspend fun createDirectory(dirPath: Path): Boolean
    suspend fun deleteFile(filePath: Path): Boolean
    suspend fun deleteDirectory(dirPath: Path, recursive: Boolean = false): Boolean
    suspend fun fileExists(filePath: Path): Boolean
    suspend fun isDirectory(path: Path): Boolean
    suspend fun getFileSize(filePath: Path): Long?
    suspend fun getLastModified(path: Path): Long?


    suspend fun listDirectory(dirPath: Path): List<FileTreeNode>
    suspend fun getFileInfo(path: Path): FileTreeNode?
}
