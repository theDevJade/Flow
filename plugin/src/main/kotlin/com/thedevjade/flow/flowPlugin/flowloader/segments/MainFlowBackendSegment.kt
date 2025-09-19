package com.thedevjade.flow.flowPlugin.flowloader.segments

import com.thedevjade.flow.common.config.FlowConfiguration
import com.thedevjade.flow.common.models.FlowLogger
import com.thedevjade.flow.flowPlugin.Flow
import com.thedevjade.flow.flowPlugin.flowloader.FlowLoaderSegment
import com.thedevjade.flow.flowPlugin.utils.Logger
import com.thedevjade.flow.webserver.FlowWebserver
import com.thedevjade.flow.webserver.WebSocketCaller
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class MainFlowBackendSegment : FlowLoaderSegment() {

    override val name: String = "MainFlowBackend"

    private var workerThread: Thread? = null
    private val running = AtomicBoolean(false)

    override fun load() {
        FlowLogger.handler = Logger()
        FlowConfiguration.databaseConfig.databasePath =
            File(Flow.instance.dataFolder, "data.sqlite").path

        if (running.compareAndSet(false, true)) {
            workerThread = Thread {
                try {
                    WebSocketCaller.run()
                } catch (e: InterruptedException) {
                } catch (e: Exception) {
                    FlowLogger.error(name, "Backend crashed", e)
                } finally {
                    running.set(false)
                }
            }.apply {
                name = "MainFlowBackendThread"
                isDaemon = true
                start()
            }
        }
    }

    override fun unload() {
        FlowWebserver.killAll()
        running.set(false)
        workerThread?.interrupt()
        workerThread?.join(5000)
        workerThread = null
    }
}
