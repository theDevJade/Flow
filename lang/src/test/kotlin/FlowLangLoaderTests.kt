package com.thedevjade.io.flowlang

import com.thedevjade.io.flowlang.language.FlowLangEngine
import com.thedevjade.io.flowlang.language.memory.FlowLangFunction
import com.thedevjade.io.flowlang.language.memory.FlowLangParameter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File

class FlowLangLoaderTests {
    
    @Test
    fun testLoaderTests() {
        FlowLang.stop()
        FlowLang.start()
        val path = File(FlowLang.configuration?.flowLangPath ?: "./flowlang").absolutePath
        assertNotNull(path)
        assertTrue(path.isNotEmpty())
    }

    @Test
    fun testFlowLangLoaderTest() {
        val engine = FlowLangEngine.getInstance()
        engine.registerFunction(FlowLangFunction("log", { args ->
            val content = args[0]
            println(content)
            null
        }, arrayOf(
            FlowLangParameter("content", "text")
        )))
        
        FlowLang.stop()
        val configuration = FlowLangConfiguration()
        
        // Find project root directory
        var directory = File(".").absolutePath
        while (directory != "/" && directory.isNotEmpty()) {
            if (File(directory).listFiles { _, name -> name.endsWith(".gradle.kts") }?.isNotEmpty() == true) {
                break
            }
            directory = File(directory).parent ?: break
        }
        
        configuration.flowLangPath = "$directory/testFlowLang"
        FlowLang.start(configuration)
        
        val path = File(FlowLang.configuration?.flowLangPath ?: "./flowlang").absolutePath
        assertNotNull(path)
        assertTrue(path.isNotEmpty())
    }
}
