package com.thedevjade.flow.flowPlugin

import com.thedevjade.flow.api.FlowConfig
import com.thedevjade.flow.api.FlowCore
import com.thedevjade.flow.common.config.ConfigurationLoader
import com.thedevjade.flow.extension.LoadedExtensionInfo
import com.thedevjade.flow.extension.api.ExtensionMetadata
import com.thedevjade.flow.flowPlugin.builtinextensions.SimpleBuiltInMinecraftExtension
import com.thedevjade.flow.flowPlugin.commands.FlowCommand
import com.thedevjade.flow.flowPlugin.commands.SimpleFlowLangCommand
import com.thedevjade.flow.flowPlugin.flowloader.SegmentLoader
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.FlowLang
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.FlowLangConfiguration
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.net.URLClassLoader

class Flow : JavaPlugin() {

    companion object {
        lateinit var instance: Flow
    }

    private lateinit var builtInExtension: SimpleBuiltInMinecraftExtension

    override fun onEnable() {
        instance = this
        ConfigurationLoader.configFile = File(dataFolder, "config.toml")
        ConfigurationLoader.load()
        SegmentLoader.load()


        initializeFlowCore()


        initializeFlowLang()


        initializeBuiltInExtension()

        registerCommands()
    }

    override fun onDisable() {

        if (::builtInExtension.isInitialized) {
            builtInExtension.disable()
            builtInExtension.destroy()
        }


        runBlocking {
            FlowCore.getInstance().shutdown()
        }


        FlowLang.stop()

        ConfigurationLoader.save()
        SegmentLoader.unload()
    }

    override fun reloadConfig() {
        ConfigurationLoader.load()
    }

    private fun initializeFlowCore() {
        val flowConfig = FlowConfig(
            dataDirectory = File(dataFolder, "data").absolutePath,
            maxUsers = 1000,
            maxGraphsPerUser = 100,
            webSocketPort = 9090,
            enableMetrics = true,
            enableLogging = true,
            logLevel = FlowConfig.LogLevel.INFO,
            startTime = System.currentTimeMillis()
        )

        FlowCore.initialize(flowConfig)
        logger.info("FlowCore initialized with data directory: ${flowConfig.dataDirectory}")
    }

    private fun initializeFlowLang() {
        val flowLangConfig = FlowLangConfiguration(
            flowLangPath = File(dataFolder, "flowlang").absolutePath,
            flowLangDirectoryWatch = true
        )

        FlowLang.start(flowLangConfig)
        logger.info("FlowLang initialized with path: ${flowLangConfig.flowLangPath}")
    }

    private fun initializeBuiltInExtension() {
        builtInExtension = SimpleBuiltInMinecraftExtension()


        val mockContext = object : com.thedevjade.flow.extension.api.ExtensionContext {
            override val flowCore: FlowCore = FlowCore.getInstance()
            override val config: com.thedevjade.flow.extension.api.ExtensionConfig =
                object : com.thedevjade.flow.extension.api.ExtensionConfig {
                    override fun getString(key: String, defaultValue: String): String = defaultValue
                    override fun getInt(key: String, defaultValue: Int): Int = defaultValue
                    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = defaultValue
                    override fun getDouble(key: String, defaultValue: Double): Double = defaultValue
                    override fun getList(key: String, defaultValue: List<String>): List<String> = defaultValue
                    override fun set(key: String, value: Any) {}
                }
            override val logger: com.thedevjade.flow.extension.api.ExtensionLogger =
                object : com.thedevjade.flow.extension.api.ExtensionLogger {
                    override fun debug(message: String, vararg args: Any?) = this@Flow.logger.info("[DEBUG] $message")
                    override fun info(message: String, vararg args: Any?) = this@Flow.logger.info(message)
                    override fun warn(message: String, vararg args: Any?) = this@Flow.logger.warning(message)
                    override fun error(message: String, vararg args: Any?) = this@Flow.logger.severe(message)
                    override fun error(message: String, throwable: Throwable, vararg args: Any?) =
                        this@Flow.logger.log(java.util.logging.Level.SEVERE, message, throwable)
                }
            override val scheduler: com.thedevjade.flow.extension.api.ExtensionScheduler =
                object : com.thedevjade.flow.extension.api.ExtensionScheduler {
                    override fun scheduleSync(task: () -> Unit) {
                        server.scheduler.runTask(this@Flow, task)
                    }

                    override fun scheduleAsync(task: suspend () -> Unit): kotlinx.coroutines.Job {
                        return kotlinx.coroutines.GlobalScope.launch {
                            try {
                                task()
                            } catch (e: Exception) {
                                this@Flow.logger.severe("Error in async task: ${e.message}")
                            }
                        }
                    }

                    override fun scheduleDelayed(delayMs: Long, task: () -> Unit) {
                        server.scheduler.runTaskLater(this@Flow, task, delayMs / 50)
                    }

                    override fun scheduleRepeating(intervalMs: Long, task: () -> Unit): kotlinx.coroutines.Job {
                        server.scheduler.runTaskTimer(this@Flow, task, 0, intervalMs / 50)
                        return kotlinx.coroutines.GlobalScope.launch {

                            kotlinx.coroutines.delay(intervalMs)
                        }
                    }
                }
            override val eventBus: com.thedevjade.flow.extension.api.ExtensionEventBus =
                object : com.thedevjade.flow.extension.api.ExtensionEventBus {
                    override fun <T : Any> subscribe(eventType: Class<T>, handler: (T) -> Unit) {}
                    override fun <T : Any> unsubscribe(eventType: Class<T>, handler: (T) -> Unit) {}
                    override fun <T : Any> publish(event: T) {}
                }
            override val dependencyInjector: com.thedevjade.flow.extension.api.DependencyInjector =
                object : com.thedevjade.flow.extension.api.DependencyInjector {
                    override fun <T : Any> getInstance(type: Class<T>): T? = null
                    override fun <T : Any> getInstance(type: Class<T>, name: String): T? = null
                    override fun <T : Any> registerInstance(type: Class<T>, instance: T) {}
                    override fun <T : Any> registerInstance(type: Class<T>, name: String, instance: T) {}
                }
        }

        builtInExtension.initialize(mockContext)
        builtInExtension.enable()


        runBlocking {
            val extensionManager = FlowCore.getInstance().extensions


            extensionManager.registerExtensionWithRegistry(builtInExtension)


            val extensionInfo = LoadedExtensionInfo(
                extension = builtInExtension,
                metadata = ExtensionMetadata(
                    name = "BuiltInMinecraft",
                    version = "1.0.0",
                    description = "Built-in Minecraft integration for Flow",
                    author = "Flow Team",
                    mainClass = "com.thedevjade.flow.flowPlugin.builtinextensions.SimpleBuiltInMinecraftExtension",
                    dependencies = emptyList(),
                    jarPath = "built-in",
                    loadTime = System.currentTimeMillis(),
                    lastModified = System.currentTimeMillis()
                ),
                classLoader = this::class.java.classLoader as URLClassLoader?
            )


            try {
                val loadedExtensionsField = extensionManager.javaClass.getDeclaredField("loadedExtensions")
                loadedExtensionsField.isAccessible = true
                val loadedExtensions =
                    loadedExtensionsField.get(extensionManager) as java.util.concurrent.ConcurrentHashMap<String, LoadedExtensionInfo>
                loadedExtensions["BuiltInMinecraft"] = extensionInfo
                logger.info("Built-in Minecraft extension registered with FlowCore")
            } catch (e: Exception) {
                logger.warning("Failed to register built-in extension with FlowCore: ${e.message}")
            }
        }

        logger.info("Built-in Minecraft extension initialized and enabled")
    }

    fun registerCommands() {
        getCommand("flow")?.apply {
            val flowCmd = FlowCommand()
            setExecutor(flowCmd)
            tabCompleter = flowCmd
        }

        getCommand("flowlang")?.apply {
            val flowLangCmd = SimpleFlowLangCommand()
            setExecutor(flowLangCmd)
            tabCompleter = flowLangCmd
        }
    }
}
