package com.thedevjade.flow.flowPlugin

import com.thedevjade.flow.common.config.ConfigurationLoader
import com.thedevjade.flow.flowPlugin.commands.FlowCommand
import com.thedevjade.flow.flowPlugin.commands.FlowLangCommand
import com.thedevjade.flow.flowPlugin.flowloader.SegmentLoader
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class Flow : JavaPlugin() {

    companion object {
        lateinit var instance: Flow
    }

    override fun onEnable() {
        instance = this
        ConfigurationLoader.configFile = File(dataFolder, "config.toml")
        ConfigurationLoader.load()
        SegmentLoader.load()
        registerCommands()
    }

    override fun onDisable() {
        ConfigurationLoader.save()
        SegmentLoader.unload()
    }

    override fun reloadConfig() {
        ConfigurationLoader.load()
    }

    fun registerCommands() {
        getCommand("flow")?.apply {
            val flowCmd = FlowCommand()
            setExecutor(flowCmd)
            tabCompleter = flowCmd
        }

        getCommand("flowlang")?.apply {
            val flowLangCmd = FlowLangCommand()
            setExecutor(flowLangCmd)
            tabCompleter = flowLangCmd
        }
    }
}
