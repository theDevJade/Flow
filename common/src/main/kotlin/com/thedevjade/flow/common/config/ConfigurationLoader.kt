package com.thedevjade.flow.common.config

import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import java.io.File

object ConfigurationLoader {
    val toml = Toml()
    val tomlWriter = TomlWriter()

    lateinit var configFile: File


    fun load() {
        if (!configFile.exists()) {
            save()
        }
        FlowConfiguration.webserverConfig = toml.read(configFile).to(FlowConfiguration.WebserverConfig::class.java)
    }

    fun save() {
        if (!configFile.exists()) {
            configFile.parentFile.mkdirs()
            configFile.createNewFile()
        }
        val text = tomlWriter.write(FlowConfiguration.webserverConfig)
        configFile.writeText(text)
    }
}