package flow.api.implementation

import flow.api.FileSystemAccess
import com.thedevjade.flow.common.models.FileTreeNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.*

class FileSystemAccessImpl : FileSystemAccess {
    private var rootDirectory: Path? = null
    private val allowedDirectories = mutableSetOf<Path>()

    companion object {
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024
        private const val MAX_DEPTH = 10
    }

    override fun setRootDirectory(path: Path): Boolean {
        return try {
            val absolutePath = path.toAbsolutePath().normalize()
            if (Files.exists(absolutePath) && Files.isDirectory(absolutePath)) {
                rootDirectory = absolutePath

                allowedDirectories.clear()
                allowedDirectories.add(absolutePath)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("FileSystemAccessImpl: Error setting root directory: ${e.message}")
            false
        }
    }

    override fun addAllowedDirectory(path: Path): Boolean {
        return try {
            val absolutePath = path.toAbsolutePath().normalize()
            if (Files.exists(absolutePath) && Files.isDirectory(absolutePath)) {

                if (rootDirectory != null && !absolutePath.startsWith(rootDirectory!!)) {
                    println("FileSystemAccessImpl: Directory $path is outside root directory")
                    return false
                }
                allowedDirectories.add(absolutePath)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("FileSystemAccessImpl: Error adding allowed directory: ${e.message}")
            false
        }
    }

    override fun removeAllowedDirectory(path: Path): Boolean {
        val absolutePath = path.toAbsolutePath().normalize()
        return allowedDirectories.remove(absolutePath)
    }

    override fun isPathAllowed(path: Path): Boolean {
        return try {
            val absolutePath = path.toAbsolutePath().normalize()


            if (rootDirectory != null) {
                val isAllowed = absolutePath.startsWith(rootDirectory!!)
                if (isAllowed) {
                    return true
                } else {
                    println("FileSystemAccessImpl: Access denied to file: $absolutePath")
                    return false
                }
            }


            val isAllowedFallback = allowedDirectories.any { allowedDir ->
                absolutePath.startsWith(allowedDir)
            }
            if (!isAllowedFallback) {
                println("FileSystemAccessImpl: Access denied to file: $absolutePath")
            }
            isAllowedFallback
        } catch (e: Exception) {
            println("FileSystemAccessImpl: Error checking path allowed: ${e.message}")
            false
        }
    }

    override fun getRootDirectory(): Path? = rootDirectory

    override fun getAllowedDirectories(): Set<Path> = allowedDirectories.toSet()

    override suspend fun getFileTree(rootPath: Path?): FileTreeNode? {
        return withContext(Dispatchers.IO) {
            try {
                val startPath = rootPath ?: rootDirectory ?: return@withContext null

                if (!isPathAllowed(startPath)) {
                    println("FileSystemAccessImpl: Access denied to path: $startPath")
                    return@withContext null
                }

                buildFileTree(startPath, 0)
            } catch (e: Exception) {
                println("FileSystemAccessImpl: Error building file tree: ${e.message}")
                null
            }
        }
    }

    private fun buildFileTree(path: Path, depth: Int): FileTreeNode? {
        if (depth > MAX_DEPTH) return null

        return try {
            val fileName = path.fileName?.toString() ?: path.toString()
            val isDirectory = Files.isDirectory(path)
            val size = if (isDirectory) null else Files.size(path)
            val lastModified = Files.getLastModifiedTime(path).toMillis()

            val children = if (isDirectory && depth < MAX_DEPTH) {
                try {
                    Files.list(path).use { stream ->
                        val childNodes = mutableListOf<FileTreeNode>()
                        stream.filter { childPath: Path ->
                            isPathAllowed(childPath) && !childPath.fileName.toString().startsWith(".")
                        }.forEach { childPath: Path ->
                            buildFileTree(childPath, depth + 1)?.let { node ->
                                childNodes.add(node)
                            }
                        }
                        childNodes.sortedWith { a: FileTreeNode, b: FileTreeNode ->

                            when {
                                a.type == "directory" && b.type != "directory" -> -1
                                a.type != "directory" && b.type == "directory" -> 1
                                else -> a.name.compareTo(b.name, ignoreCase = true)
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("FileSystemAccessImpl: Error listing directory $path: ${e.message}")
                    emptyList<FileTreeNode>()
                }
            } else {
                emptyList<FileTreeNode>()
            }

            FileTreeNode(
                name = fileName,
                type = if (isDirectory) "directory" else "file",
                path = path.toString(),
                children = children,
                size = size,
                lastModified = lastModified
            )
        } catch (e: Exception) {
            println("FileSystemAccessImpl: Error processing path $path: ${e.message}")
            null
        }
    }

    override suspend fun readFile(filePath: Path): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (!isPathAllowed(filePath)) {
                    println("FileSystemAccessImpl: Access denied to read file: $filePath")
                    return@withContext null
                }

                if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                    return@withContext null
                }

                val fileSize = Files.size(filePath)
                if (fileSize > MAX_FILE_SIZE) {
                    println("FileSystemAccessImpl: File too large to read: $filePath (${fileSize} bytes)")
                    return@withContext null
                }

                Files.readString(filePath)
            } catch (e: Exception) {
                println("FileSystemAccessImpl: Error reading file $filePath: ${e.message}")
                null
            }
        }
    }

    override suspend fun writeFile(filePath: Path, content: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!isPathAllowed(filePath)) {
                    println("FileSystemAccessImpl: Access denied to write file: $filePath")
                    return@withContext false
                }

                if (content.length > MAX_FILE_SIZE) {
                    println("FileSystemAccessImpl: Content too large to write: ${content.length} bytes")
                    return@withContext false
                }


                filePath.parent?.let { parentDir ->
                    if (!Files.exists(parentDir)) {
                        Files.createDirectories(parentDir)
                    }
                }

                Files.writeString(filePath, content)
                true
            } catch (e: Exception) {
                println("FileSystemAccessImpl: Error writing file $filePath: ${e.message}")
                false
            }
        }
    }

    override suspend fun createFile(filePath: Path): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!isPathAllowed(filePath)) {
                    println("FileSystemAccessImpl: Access denied to create file: $filePath")
                    return@withContext false
                }

                if (Files.exists(filePath)) {
                    return@withContext false
                }


                filePath.parent?.let { parentDir ->
                    if (!Files.exists(parentDir)) {
                        Files.createDirectories(parentDir)
                    }
                }

                Files.createFile(filePath)
                true
            } catch (e: Exception) {
                println("FileSystemAccessImpl: Error creating file $filePath: ${e.message}")
                false
            }
        }
    }

    override suspend fun createDirectory(dirPath: Path): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!isPathAllowed(dirPath)) {
                    println("FileSystemAccessImpl: Access denied to create directory: $dirPath")
                    return@withContext false
                }

                if (Files.exists(dirPath)) {
                    return@withContext false
                }

                Files.createDirectories(dirPath)
                true
            } catch (e: Exception) {
                println("FileSystemAccessImpl: Error creating directory $dirPath: ${e.message}")
                false
            }
        }
    }

    override suspend fun deleteFile(filePath: Path): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!isPathAllowed(filePath)) {
                    println("FileSystemAccessImpl: Access denied to delete file: $filePath")
                    return@withContext false
                }

                if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                    return@withContext false
                }

                Files.delete(filePath)
                true
            } catch (e: Exception) {
                println("FileSystemAccessImpl: Error deleting file $filePath: ${e.message}")
                false
            }
        }
    }

    override suspend fun deleteDirectory(dirPath: Path, recursive: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!isPathAllowed(dirPath)) {
                    println("FileSystemAccessImpl: Access denied to delete directory: $dirPath")
                    return@withContext false
                }

                if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                    return@withContext false
                }

                if (recursive) {
                    Files.walkFileTree(dirPath, object : SimpleFileVisitor<Path>() {
                        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                            Files.delete(file)
                            return FileVisitResult.CONTINUE
                        }

                        override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                            Files.delete(dir)
                            return FileVisitResult.CONTINUE
                        }
                    })
                } else {
                    Files.delete(dirPath)
                }
                true
            } catch (e: Exception) {
                println("FileSystemAccessImpl: Error deleting directory $dirPath: ${e.message}")
                false
            }
        }
    }

    override suspend fun fileExists(filePath: Path): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                isPathAllowed(filePath) && Files.exists(filePath)
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun isDirectory(path: Path): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                isPathAllowed(path) && Files.exists(path) && Files.isDirectory(path)
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun getFileSize(filePath: Path): Long? {
        return withContext(Dispatchers.IO) {
            try {
                if (isPathAllowed(filePath) && Files.exists(filePath) && !Files.isDirectory(filePath)) {
                    Files.size(filePath)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun getLastModified(path: Path): Long? {
        return withContext(Dispatchers.IO) {
            try {
                if (isPathAllowed(path) && Files.exists(path)) {
                    Files.getLastModifiedTime(path).toMillis()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun listDirectory(dirPath: Path): List<FileTreeNode> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isPathAllowed(dirPath) || !Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                    return@withContext emptyList()
                }

                Files.list(dirPath).use { stream ->
                    val childNodes = mutableListOf<FileTreeNode>()
                    stream.filter { childPath: Path ->
                        isPathAllowed(childPath) && !childPath.fileName.toString().startsWith(".")
                    }.forEach { childPath: Path ->
                        runBlocking {
                            getFileInfo(childPath)?.let { node ->
                                childNodes.add(node)
                            }
                        }
                    }
                    childNodes.sortedWith { a: FileTreeNode, b: FileTreeNode ->
                        when {
                            a.type == "directory" && b.type != "directory" -> -1
                            a.type != "directory" && b.type == "directory" -> 1
                            else -> a.name.compareTo(b.name, ignoreCase = true)
                        }
                    }
                }
            } catch (e: Exception) {
                println("FileSystemAccessImpl: Error listing directory $dirPath: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun getFileInfo(path: Path): FileTreeNode? {
        return withContext(Dispatchers.IO) {
            try {
                if (!isPathAllowed(path) || !Files.exists(path)) {
                    return@withContext null
                }

                val fileName = path.fileName?.toString() ?: path.toString()
                val isDirectory = Files.isDirectory(path)
                val size = if (isDirectory) null else Files.size(path)
                val lastModified = Files.getLastModifiedTime(path).toMillis()

                FileTreeNode(
                    name = fileName,
                    type = if (isDirectory) "directory" else "file",
                    path = path.toString(),
                    children = emptyList(),
                    size = size,
                    lastModified = lastModified
                )
            } catch (e: Exception) {
                println("FileSystemAccessImpl: Error getting file info for $path: ${e.message}")
                null
            }
        }
    }
}
