package com.thedevjade.flow.flowPlugin.flowloader

import com.thedevjade.flow.flowPlugin.utils.ReflectionUtils
import com.thedevjade.flow.flowPlugin.utils.info
import kotlin.reflect.typeOf

object SegmentLoader {

    private var loaded = false
    private var segments = emptyList<FlowLoaderSegment>()

    private fun init() {
        segments = ReflectionUtils.getClassesInPackage("com.thedevjade.flow.flowPlugin.flowloader.segments").filter {
            it.genericSuperclass == typeOf<SegmentLoader>()
        }.map {
            it.getConstructor().newInstance() as FlowLoaderSegment
        }
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