package com.thedevjade.flow.flowPlugin

import com.thedevjade.flow.flowPlugin.flowloader.SegmentLoader
import config.ConfigurationLoader
import org.bukkit.plugin.java.JavaPlugin

class Flow : JavaPlugin() {

    companion object {
        public lateinit var instance: Flow
    }

    override fun onEnable() {
        instance = this
        ConfigurationLoader.load()
        SegmentLoader.load()
    }

    override fun onDisable() {
        ConfigurationLoader.save()
        SegmentLoader.unload()
    }

    override fun reloadConfig() {
        ConfigurationLoader.load()
    }
}
