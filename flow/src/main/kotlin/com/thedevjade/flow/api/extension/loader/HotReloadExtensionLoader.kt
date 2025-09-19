package com.thedevjade.flow.extension.loader

import com.thedevjade.flow.api.FlowCore
import com.thedevjade.flow.extension.api.*
import kotlinx.coroutines.*
import java.io.File
import java.net.URLClassLoader
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong


class HotReloadExtensionLoader(
    private val flowCore: FlowCore,
    private val extensionsDirectory: File = File("extensions"),
    private val watchInterval: Long = 1000L
) {
    private val loadedExtensions = ConcurrentHashMap<String, LoadedExtension>()
    private val extensionClassLoaders = ConcurrentHashMap<String, URLClassLoader>()
    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private val watchKeys = ConcurrentHashMap<WatchKey, Path>()
    private val executionIdGenerator = AtomicLong(0)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    fun startWatching() {

        if (!extensionsDirectory.exists()) {
            extensionsDirectory.mkdirs()
        }


        val watchKey = extensionsDirectory.toPath().register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE
        )
        watchKeys[watchKey] = extensionsDirectory.toPath()


        scope.launch {
            watchLoop()
        }


        scope.launch {
            loadExistingExtensions()
        }
    }


    fun stopWatching() {
        scope.cancel()
        watchService.close()
    }


    suspend fun loadExtension(jarFile: File): ExtensionLoadResult {
        return try {
            val metadata = createMetadata(jarFile)
            val classLoader = createClassLoader(jarFile)
            val extensionClass = loadExtensionClass(classLoader, metadata.mainClass)
            val extension = instantiateExtension(extensionClass)


            val context = createExtensionContext(metadata.name)
            extension.initialize(context)
            extension.enable()

            val loadedExtension = LoadedExtension(
                extension = extension,
                metadata = metadata,
                classLoader = classLoader,
                jarFile = jarFile
            )

            loadedExtensions[metadata.name] = loadedExtension
            extensionClassLoaders[metadata.name] = classLoader

            ExtensionLoadResult.Success(extension, metadata)
        } catch (e: Exception) {
            ExtensionLoadResult.Failure("Failed to load extension: ${e.message ?: "Unknown error"}")
        }
    }


    suspend fun reloadExtension(extensionName: String): ExtensionReloadResult {
        val loadedExtension = loadedExtensions[extensionName] ?: return ExtensionReloadResult.Failure("Extension not found: $extensionName")

        return try {
            val jarFile = loadedExtension.jarFile
            val currentModified = jarFile.lastModified()


            loadedExtension.extension.disable()
            loadedExtension.extension.destroy()
            loadedExtension.classLoader.close()


            val result = loadExtension(jarFile)
            when (result) {
                is ExtensionLoadResult.Success -> ExtensionReloadResult.Success(result.extension, result.metadata)
                is ExtensionLoadResult.Failure -> ExtensionReloadResult.Failure(result.error)
            }
        } catch (e: Exception) {
            ExtensionReloadResult.Failure("Failed to reload extension: ${e.message}")
        }
    }


    suspend fun unloadExtension(extensionName: String): Boolean {
        val loadedExtension = loadedExtensions.remove(extensionName) ?: return false

        return try {
            loadedExtension.extension.disable()
            loadedExtension.extension.destroy()
            loadedExtension.classLoader.close()
            extensionClassLoaders.remove(extensionName)
            true
        } catch (e: Exception) {
            false
        }
    }


    fun getLoadedExtensions(): Map<String, LoadedExtension> = loadedExtensions.toMap()


    fun getLoadedExtension(name: String): LoadedExtension? = loadedExtensions[name]


    fun isExtensionLoaded(name: String): Boolean = loadedExtensions.containsKey(name)

    private suspend fun loadExistingExtensions() {
        if (!extensionsDirectory.exists()) return

        extensionsDirectory.listFiles { _, name -> name.endsWith(".jar") }?.forEach { jarFile ->
            try {
                loadExtension(jarFile)
            } catch (e: Exception) {
                println("Failed to load existing extension ${jarFile.name}: ${e.message}")
            }
        }
    }

    private suspend fun watchLoop() {
        while (scope.isActive) {
            try {
                val watchKey = watchService.take()
                processWatchEvents(watchKey)
                watchKey.reset()
            } catch (e: Exception) {
                if (scope.isActive) {
                    println("Watch service error: ${e.message}")
                }
            }
        }
    }

    private suspend fun processWatchEvents(watchKey: WatchKey) {
        for (event in watchKey.pollEvents()) {
            val kind = event.kind()
            val context = event.context() as? Path ?: continue

            if (context.toString().endsWith(".jar")) {
                val jarFile = extensionsDirectory.toPath().resolve(context).toFile()

                when (kind) {
                    StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY -> {

                        if (isExtensionLoaded(jarFile.nameWithoutExtension)) {
                            reloadExtension(jarFile.nameWithoutExtension)
                        } else {
                            loadExtension(jarFile)
                        }
                    }
                    StandardWatchEventKinds.ENTRY_DELETE -> {

                        unloadExtension(jarFile.nameWithoutExtension)
                    }
                }
            }
        }
    }

    private fun createMetadata(jarFile: File): ExtensionMetadata {
        val classLoader = createClassLoader(jarFile)
        val manifest = classLoader.getResourceAsStream("META-INF/MANIFEST.MF")


        val name = extractExtensionName(jarFile) ?: jarFile.nameWithoutExtension
        val version = "1.0.0"
        val description = "Extension loaded from ${jarFile.name}"
        val author = "Unknown"
        val mainClass = findMainClass(classLoader) ?: "Extension"

        return ExtensionMetadata(
            name = name,
            version = version,
            description = description,
            author = author,
            mainClass = mainClass,
            dependencies = emptyList(),
            jarPath = jarFile.absolutePath,
            loadTime = System.currentTimeMillis(),
            lastModified = jarFile.lastModified()
        )
    }

    private fun createClassLoader(jarFile: File): URLClassLoader {
        val urls = arrayOf(jarFile.toURI().toURL())
        return URLClassLoader(urls, this::class.java.classLoader)
    }

    private fun loadExtensionClass(classLoader: URLClassLoader, mainClass: String): Class<*> {
        return classLoader.loadClass(mainClass)
    }

    private fun instantiateExtension(extensionClass: Class<*>): FlowExtension {
        val constructor = extensionClass.getDeclaredConstructor()
        constructor.isAccessible = true
        return constructor.newInstance() as FlowExtension
    }

    private fun createExtensionContext(extensionName: String): ExtensionContext {
        return object : ExtensionContext {
            override val flowCore: FlowCore = this@HotReloadExtensionLoader.flowCore
            override val config: ExtensionConfig = createExtensionConfig(extensionName)
            override val logger: ExtensionLogger = createExtensionLogger(extensionName)
            override val scheduler: ExtensionScheduler = createExtensionScheduler()
            override val eventBus: ExtensionEventBus = createExtensionEventBus()
            override val dependencyInjector: DependencyInjector = createDependencyInjector()
        }
    }

    private fun createExtensionConfig(extensionName: String): ExtensionConfig {
        return object : ExtensionConfig {
            private val config = ConcurrentHashMap<String, Any>()

            override fun getString(key: String, defaultValue: String): String {
                return config[key] as? String ?: defaultValue
            }

            override fun getInt(key: String, defaultValue: Int): Int {
                return config[key] as? Int ?: defaultValue
            }

            override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
                return config[key] as? Boolean ?: defaultValue
            }

            override fun getDouble(key: String, defaultValue: Double): Double {
                return config[key] as? Double ?: defaultValue
            }

            override fun getList(key: String, defaultValue: List<String>): List<String> {
                return config[key] as? List<String> ?: defaultValue
            }

            override fun set(key: String, value: Any) {
                config[key] = value
            }
        }
    }

    private fun createExtensionLogger(extensionName: String): ExtensionLogger {
        return object : ExtensionLogger {
            override fun debug(message: String, vararg args: Any?) {
                println("[DEBUG] [$extensionName] ${message.format(*args)}")
            }

            override fun info(message: String, vararg args: Any?) {
                println("[INFO] [$extensionName] ${message.format(*args)}")
            }

            override fun warn(message: String, vararg args: Any?) {
                println("[WARN] [$extensionName] ${message.format(*args)}")
            }

            override fun error(message: String, vararg args: Any?) {
                println("[ERROR] [$extensionName] ${message.format(*args)}")
            }

            override fun error(message: String, throwable: Throwable, vararg args: Any?) {
                println("[ERROR] [$extensionName] ${message.format(*args)}")
                throwable.printStackTrace()
            }
        }
    }

    private fun createExtensionScheduler(): ExtensionScheduler {
        return object : ExtensionScheduler {
            override fun scheduleSync(task: () -> Unit) {
                scope.launch { task() }
            }

            override fun scheduleAsync(task: suspend () -> Unit): Job {
                return scope.launch { task() }
            }

            override fun scheduleDelayed(delayMs: Long, task: () -> Unit) {
                scope.launch {
                    delay(delayMs)
                    task()
                }
            }

            override fun scheduleRepeating(intervalMs: Long, task: () -> Unit): Job {
                return scope.launch {
                    while (isActive) {
                        task()
                        delay(intervalMs)
                    }
                }
            }
        }
    }

    private fun createExtensionEventBus(): ExtensionEventBus {
        return object : ExtensionEventBus {
            private val handlers = ConcurrentHashMap<Class<*>, MutableList<(Any) -> Unit>>()

            override fun <T : Any> subscribe(eventType: Class<T>, handler: (T) -> Unit) {
                handlers.getOrPut(eventType) { mutableListOf() }.add(handler as (Any) -> Unit)
            }

            override fun <T : Any> unsubscribe(eventType: Class<T>, handler: (T) -> Unit) {
                handlers[eventType]?.remove(handler as (Any) -> Unit)
            }

            override fun <T : Any> publish(event: T) {
                handlers[event::class.java]?.forEach { it(event) }
            }
        }
    }

    private fun createDependencyInjector(): DependencyInjector {
        return object : DependencyInjector {
            private val instances = ConcurrentHashMap<String, Any>()

            override fun <T : Any> getInstance(type: Class<T>): T? {
                return instances[type.name] as? T
            }

            override fun <T : Any> getInstance(type: Class<T>, name: String): T? {
                return instances["${type.name}:$name"] as? T
            }

            override fun <T : Any> registerInstance(type: Class<T>, instance: T) {
                instances[type.name] = instance
            }

            override fun <T : Any> registerInstance(type: Class<T>, name: String, instance: T) {
                instances["${type.name}:$name"] = instance
            }
        }
    }

    private fun extractExtensionName(jarFile: File): String? {
        return jarFile.nameWithoutExtension
    }

    private fun findMainClass(classLoader: URLClassLoader): String? {

        return null
    }


    fun dispose() {
        loadedExtensions.values.forEach { extension ->
            try {
                extension.extension.disable()
                extension.extension.destroy()
                extension.classLoader.close()
            } catch (e: Exception) {

            }
        }

        loadedExtensions.clear()
        extensionClassLoaders.clear()
        watchKeys.clear()
        scope.cancel()
    }
}


data class LoadedExtension(
    val extension: FlowExtension,
    val metadata: ExtensionMetadata,
    val classLoader: URLClassLoader,
    val jarFile: File
)