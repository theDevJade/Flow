package com.thedevjade.flow.extension.loader

import com.thedevjade.flow.api.FlowCore
import com.thedevjade.flow.extension.ExtensionManager


object ExtensionLoader {
    private var extensionManager: ExtensionManager? = null


    suspend fun initialize(flowCore: FlowCore) {
        extensionManager = ExtensionManager(flowCore)
        extensionManager?.initialize()
    }


    fun getExtensionManager(): ExtensionManager? = extensionManager


    fun dispose() {
        extensionManager?.dispose()
        extensionManager = null
    }
}