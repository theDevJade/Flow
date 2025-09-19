package com.thedevjade.flow.flowPlugin.flowloader

import com.thedevjade.flow.flowPlugin.utils.info
import org.reflections.Reflections

object SegmentLoader {

    private var loaded = false
    private var segments = emptyList<FlowLoaderSegment>()

    private fun init() {

        val reflections = Reflections("com.thedevjade.flow.flowPlugin.flowloader.segments")

        val classes = reflections.getSubTypesOf(FlowLoaderSegment::class.java)

        segments = classes.mapNotNull { clazz ->
            try {
                clazz.getDeclaredConstructor().newInstance()
            } catch (e: Exception) {
                info("Failed to load segment class ${clazz.name}: ${e.message}")
                null
            }
        }

        loaded = true
    }

    fun load() {
        if (!loaded) init()
        segments.forEach {
            info("ENABLING SEGMENT ${it.name}")
            it.load()
            info("ENABLED SEGMENT ${it.name}")
        }
    }

    fun unload() {
        segments.forEach {
            info("DISABLING SEGMENT ${it.name}")
            it.unload()
            info("DISABLED SEGMENT ${it.name}")
        }
    }
}
