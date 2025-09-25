package com.thedevjade.io.flowlang

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.FlowLang
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangEngine
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.*
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.types.Vector3
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class FlowLangTests {
    private lateinit var engine: FlowLangEngine

    @BeforeEach
    fun setup() {

        val field = FlowLangEngine::class.java.getDeclaredField("instance")
        field.isAccessible = true
        field.set(null, null)

        engine = FlowLangEngine.getInstance()
        FlowLang.start()
    }

    @Test
    fun testLiteralExpressions() {
        assertEquals(42.0, engine.execute("42"))
        assertEquals("hello", engine.execute("\"hello\""))
        assertEquals(true, engine.execute("true"))
        assertEquals(false, engine.execute("false"))
        assertNull(engine.execute("null"))
    }

    @Test
    fun testArithmeticOperations() {
        assertEquals(3.0, engine.execute("1 + 2"))
        assertEquals(-1.0, engine.execute("1 - 2"))
        assertEquals(6.0, engine.execute("2 * 3"))
        assertEquals(2.0, engine.execute("6 / 3"))
        assertEquals(1.0, engine.execute("4 % 3"))

        assertEquals(14.0, engine.execute("2 + 3 * 4"))
        assertEquals(20.0, engine.execute("(2 + 3) * 4"))

        assertEquals(-5.0, engine.execute("-5"))
    }

    @Test
    fun testStringOperations() {
        assertEquals("hello world", engine.execute("\"hello\" + \" world\""))
        assertEquals("hello42", engine.execute("\"hello\" + 42"))
    }

    @Test
    fun testComparisonOperations() {
        assertEquals(true, engine.execute("1 < 2"))
        assertEquals(false, engine.execute("1 > 2"))
        assertEquals(true, engine.execute("1 <= 1"))
        assertEquals(true, engine.execute("1 >= 1"))
        assertEquals(true, engine.execute("1 == 1"))
        assertEquals(false, engine.execute("1 != 1"))

        assertEquals(true, engine.execute("\"a\" < \"b\""))
        assertEquals(true, engine.execute("\"a\" == \"a\""))
    }

    @Test
    fun testLogicalOperations() {
        assertEquals(true, engine.execute("true and true"))
        assertEquals(false, engine.execute("true and false"))
        assertEquals(true, engine.execute("true or false"))
        assertEquals(false, engine.execute("false or false"))
        assertEquals(false, engine.execute("not true"))
        assertEquals(true, engine.execute("not false"))

        assertEquals(false, engine.execute("false and nonexistent"))
    }

    @Test
    fun testVariables() {
        val ctx = FlowLangContext()

        engine.execute("var x = 42", ctx)
        assertEquals(42.0, engine.execute("x", ctx))

        engine.execute("x = 10", ctx)
        assertEquals(10.0, engine.execute("x", ctx))

        val child = ctx.createChildContext()
        engine.execute("var y = 20", child)
        assertEquals(20.0, engine.execute("y", child))
        assertEquals(10.0, engine.execute("x", child))

        assertThrows(Exception::class.java) { engine.execute("z", ctx) }
        assertThrows(Exception::class.java) { engine.execute("y", ctx) }
    }

    @Test
    fun testIfStatement() {
        val ctx = FlowLangContext()

        engine.execute(
            """
            var x = 10
            if (x > 5) {
              x = 20
            } else {
              x = 0
            }
        """.trimIndent(), ctx
        )
        assertEquals(20.0, engine.execute("x", ctx))

        engine.execute(
            """
            x = 3
            if (x > 5) {
              x = 20
            } else {
              x = 0
            }
        """.trimIndent(), ctx
        )
        assertEquals(0.0, engine.execute("x", ctx))
    }

    @Test
    fun testWhileLoop() {
        val ctx = FlowLangContext()

        engine.execute(
            """
            var x = 0
            var i = 0
            while (i < 5) {
              x = x + i
              i = i + 1
            }
        """.trimIndent(), ctx
        )
        assertEquals(10.0, engine.execute("x", ctx))
    }

    @Test
    fun testForLoop() {
        val ctx = FlowLangContext()

        engine.execute(
            """
            var sum = 0
            for (var i = 0; i < 5; i = i + 1) {
              sum = sum + i
            }
        """.trimIndent(), ctx
        )
        assertEquals(10.0, engine.execute("sum", ctx))
    }

    @Test
    fun testFunctions() {
        val ctx = FlowLangContext()

        engine.execute(
            """
            function add(a, b) { return a + b }
            var result = add(2, 3)
        """.trimIndent(), ctx
        )
        assertEquals(5.0, engine.execute("result", ctx))

        engine.execute(
            """
            function factorial(n) {
              if (n <= 1) return 1
              return n * factorial(n - 1)
            }
            var fact5 = factorial(5)
        """.trimIndent(), ctx
        )
        assertEquals(120.0, engine.execute("fact5", ctx))
    }

    @Test
    fun testEvents() {
        engine.registerEvent(
            FlowLangEvent(
                "testEvent", arrayOf(
                    FlowLangParameter("value", "number")
                )
            )
        )

        val output = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(output))

        try {
            engine.execute(
                """
                on testEvent {
                    print("Event value: " + value)
                }
            """.trimIndent()
            )
            engine.triggerEvent("testEvent", 42.0)
        } finally {
            System.setOut(originalOut)
        }

        val outText = output.toString()
        assertTrue(outText.contains("Event value: 42"))
    }

    @Test
    fun testGameIntegration() {

        engine.registerType(FlowLangType("Player", Player::class.java))
        engine.registerType(FlowLangType("Vector", Vector3::class.java))


        engine.registerFunction(
            FlowLangFunction(
                "spawn", { args ->
                    val et = args[0]?.toString()
                    val pos = args[1] as Vector3
                    println("Spawned $et at $pos")
                    null
                }, arrayOf(
                    FlowLangParameter("entityType", "text"),
                    FlowLangParameter("position", "Vector")
                )
            )
        )


        engine.registerEvent(
            FlowLangEvent(
                "playerJoin", arrayOf(
                    FlowLangParameter("player", "Player")
                )
            )
        )
        engine.registerEvent(
            FlowLangEvent(
                "playerDamage", arrayOf(
                    FlowLangParameter("player", "Player"),
                    FlowLangParameter("amount", "number"),
                    FlowLangParameter("damageType", "text")
                )
            )
        )

        val ctx = FlowLangContext()
        val player = Player("TestPlayer", 100.0, Vector3(0.0, 0.0, 0.0))
        ctx.setVariable("player", player)

        assertEquals("TestPlayer", engine.execute("player.name", ctx))
        assertEquals(100.0, engine.execute("player.health", ctx))

        engine.execute(""" spawn("enemy", player.position) """, ctx)

        engine.execute(
            """
            var damageHandled = false
            on playerDamage {
              damageHandled = true
              player.health = player.health - amount
            }
        """.trimIndent(), ctx
        )

        engine.triggerEvent("playerDamage", player, 25.0, "fire")

        assertEquals(true, engine.execute("damageHandled", ctx))
        assertEquals(75.0, engine.execute("player.health", ctx))
    }
}


data class Player(
    val name: String,
    var health: Double,
    val position: Vector3
) {
    override fun toString(): String = "Player($name, Health=$health)"
}
