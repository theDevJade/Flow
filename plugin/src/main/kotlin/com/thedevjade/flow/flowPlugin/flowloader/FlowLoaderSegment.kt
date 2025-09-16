package com.thedevjade.flow.flowPlugin.flowloader

abstract class FlowLoaderSegment {

    abstract val name: String
    abstract fun load()

    abstract fun unload()
}