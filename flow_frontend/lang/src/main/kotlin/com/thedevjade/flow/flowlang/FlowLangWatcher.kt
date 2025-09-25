package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang

import kotlinx.coroutines.*
import java.nio.file.*

class FlowLangWatcher(private val watchPath: String? = null) {
    private var watchService: WatchService? = null
    private var watchJob: Job? = null

    fun watchDirectory() {
        try {
            watchService = FileSystems.getDefault().newWatchService()
            val path = Paths.get(watchPath ?: FlowLang.configuration?.flowLangPath ?: "./flowlang")

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

                                    }
                                }

                                StandardWatchEventKinds.ENTRY_DELETE -> {
                                    if (filename.toString().endsWith(".flowlang")) {
                                        GlobalHooks.loggingHook.info("File deleted: $filename")

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
