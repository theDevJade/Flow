package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang

import java.nio.file.*
import kotlinx.coroutines.*

class FlowLangWatcher {
    private var watchService: WatchService? = null
    private var watchJob: Job? = null

    fun watchDirectory() {
        try {
            watchService = FileSystems.getDefault().newWatchService()
            val path = Paths.get(FlowLang.configuration?.flowLangPath ?: "./flowlang")

            path.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
            )

            watchJob = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    val key = watchService?.take()
                    if (key != null) {
                        for (event in key.pollEvents()) {
                            val kind = event.kind()
                            val filename = event.context() as Path

                            when (kind) {
                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_MODIFY -> {
                                    if (filename.toString().endsWith(".flowlang")) {
                                        GlobalHooks.loggingHook.info("File changed: $filename")
                                        // Handle file change here
                                    }
                                }
                                StandardWatchEventKinds.ENTRY_DELETE -> {
                                    if (filename.toString().endsWith(".flowlang")) {
                                        GlobalHooks.loggingHook.info("File deleted: $filename")
                                        // Handle file deletion here
                                    }
                                }
                            }
                        }
                        key.reset()
                    }
                }
            }
        } catch (e: Exception) {
            GlobalHooks.loggingHook.error("Error setting up file watcher: ${e.message}")
        }
    }

    fun stopWatching() {
        watchJob?.cancel()
        watchService?.close()
    }
}
