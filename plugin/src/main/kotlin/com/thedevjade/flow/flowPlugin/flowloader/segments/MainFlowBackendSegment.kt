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
            File(Flow.instance.dataFolder, "flow.db").path

        if (running.compareAndSet(false, true)) {
            workerThread = Thread {
                try {
                    WebSocketCaller.run()
                } catch (e: InterruptedException) {
                    FlowLogger.debug(name, "Backend thread interrupted during shutdown")
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
        running.set(false)
        workerThread?.interrupt()

        // Give the thread a moment to respond to interrupt
        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
            // Ignore
        }


        FlowWebserver.killAll()

        // Wait for the thread to finish with a longer timeout
        workerThread?.join(10000)
        workerThread = null
    }
}
