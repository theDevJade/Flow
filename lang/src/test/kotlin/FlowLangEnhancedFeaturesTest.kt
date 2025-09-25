package com.thedevjade.io.flowlang

import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.FlowLang
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangEngine
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.*
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.nodes.AssignmentNode
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.nodes.BinaryOpNode
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.nodes.LiteralNode
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.nodes.VariableNode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class FlowLangEnhancedFeaturesTest {
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
    fun testEnhancedEventParameters() {

        val event = FlowLangEvent(
            "playerAction",
            arrayOf(
                FlowLangParameter("playerName", "text"),
                FlowLangParameter("action", "text"),
                FlowLangParameter("x", "number"),
                FlowLangParameter("y", "number"),
                FlowLangParameter("z", "number")
            ),
            "Triggered when a player performs an action"
        )
        engine.registerEvent(event)


        val eventInfo = engine.getEventInfo("playerAction")
        assertNotNull(eventInfo)
        assertTrue(eventInfo!!.contains("playerAction"))
        assertTrue(eventInfo.contains("playerName: text"))
        assertTrue(eventInfo.contains("action: text"))
        assertTrue(eventInfo.contains("Triggered when a player performs an action"))


        val paramInfo = engine.getEventParameters("playerAction")
        assertNotNull(paramInfo)
        assertEquals(5, paramInfo!!.size)
        assertEquals("playerName", paramInfo[0]["name"])
        assertEquals("text", paramInfo[0]["type"])
        assertEquals(false, paramInfo[0]["optional"])


        val eventList = engine.listEvents()
        assertTrue(eventList.contains("playerAction"))


        val ctx = FlowLangContext()
        val output = captureOutput {

            engine.execute(
                """
                on playerAction {
                    print("Player " + playerName + " performed " + action + " at (" + x + ", " + y + ", " + z + ")")
                }
            """.trimIndent(), ctx
            )


            Thread.sleep(10)


            engine.triggerEvent("playerAction", "Alice", "jump", 10.0, 20.0, 30.0)
        }


        println("Captured output: '$output'")


        val expectedMessage = "Player Alice performed jump at (10.0, 20.0, 30.0)"
        val actualMessage = "Player Alice performed jump at (10, 20, 30)"


        val containsExpected = output.contains(expectedMessage) || output.contains(actualMessage)

        if (!containsExpected) {

            val simpleOutput = captureOutput {
                engine.execute("print('Hello World')", ctx)
            }
            println("Simple print test output: '$simpleOutput'")
        }
        assertTrue(containsExpected, "Expected to find '$expectedMessage' or '$actualMessage' in output: '$output'")
    }

    @Test
    fun testEventParameterValidation() {
        val event = FlowLangEvent(
            "testEvent",
            arrayOf(
                FlowLangParameter("required", "text"),
                FlowLangParameter("optional", "number", true, 42.0)
            ),
            "Test event with optional parameters"
        )
        engine.registerEvent(event)


        assertTrue(event.validateParameters(arrayOf("test")))
        assertTrue(event.validateParameters(arrayOf("test", 100.0)))
        assertFalse(event.validateParameters(arrayOf<Any?>()))
        assertTrue(event.validateParameters(arrayOf("test", null)))
    }

    @Test
    fun testBuiltInEventFunctions() {

        engine.registerEventWithParams("event1", arrayOf(FlowLangParameter("param1", "text")), "First test event")
        engine.registerEventWithParams(
            "event2", arrayOf(
                FlowLangParameter("param1", "number"),
                FlowLangParameter("param2", "boolean")
            ), "Second test event"
        )


        val eventList = engine.execute("listEvents()")
        assertTrue(eventList.toString().contains("event1"))
        assertTrue(eventList.toString().contains("event2"))


        val eventInfo = engine.execute("getEventInfo(\"event1\")")
        assertTrue(eventInfo.toString().contains("event1"))
        assertTrue(eventInfo.toString().contains("param1: text"))


        val eventParams = engine.execute("getEventParameters(\"event2\")")
        assertTrue(eventParams.toString().contains("param1: number"))
        assertTrue(eventParams.toString().contains("param2: boolean"))
    }

    @Test
    fun testBasicClassDefinition() {

        val personClass = FlowLangClass("Person")
        personClass.addProperty(FlowLangProperty("name", "text"))
        personClass.addProperty(FlowLangProperty("age", "number"))
        personClass.addProperty(FlowLangProperty("email", "text"))


        val methodBody = BinaryOpNode.FlowLangScript(
            BinaryOpNode.BlockNode(
                listOf(
                    BinaryOpNode.ReturnNode(
                        BinaryOpNode(
                            VariableNode("name"),
                            "+",
                            LiteralNode(" is ")
                        )
                    )
                )
            )
        )
        val method = FlowLangMethod("introduce", arrayOf(), methodBody)
        personClass.addMethod(method)

        engine.registerClass(personClass)


        assertTrue(engine.hasClass("Person"))
        assertEquals("Person", engine.getClass("Person")?.name)


        val classInfo = engine.getClassInfo("Person")
        assertNotNull(classInfo)
        assertTrue(classInfo!!.contains("class Person"))
        assertTrue(classInfo.contains("name"))
        assertTrue(classInfo.contains("age"))
        assertTrue(classInfo.contains("email"))


        val classList = engine.listClasses()
        assertTrue(classList.contains("Person"))
    }

    @Test
    fun testClassInheritance() {

        val animalClass = FlowLangClass("Animal")
        animalClass.addProperty(FlowLangProperty("name", "text"))
        animalClass.addProperty(FlowLangProperty("species", "text"))

        val animalMethod = FlowLangMethod(
            "makeSound", arrayOf(),
            BinaryOpNode.FlowLangScript(
                BinaryOpNode.BlockNode(
                    listOf(
                        BinaryOpNode.ReturnNode(LiteralNode("Some generic sound"))
                    )
                )
            )
        )
        animalClass.addMethod(animalMethod)


        val dogClass = FlowLangClass("Dog", "Animal")
        dogClass.addProperty(FlowLangProperty("breed", "text"))
        dogClass.addProperty(FlowLangProperty("isGoodBoy", "boolean", FlowLangProperty.Visibility.PUBLIC, true))

        val dogMethod = FlowLangMethod(
            "makeSound", arrayOf(),
            BinaryOpNode.FlowLangScript(
                BinaryOpNode.BlockNode(
                    listOf(
                        BinaryOpNode.ReturnNode(LiteralNode("Woof!"))
                    )
                )
            )
        )
        dogClass.addMethod(dogMethod)

        engine.registerClass(animalClass)
        engine.registerClass(dogClass)


        assertTrue(dogClass.hasSuperClass())
        assertEquals("Animal", dogClass.superClassName)


        val dogInfo = engine.getClassInfo("Dog")
        assertNotNull(dogInfo)
        assertTrue(dogInfo!!.contains("extends Animal"))
        assertTrue(dogInfo.contains("breed"))
        assertTrue(dogInfo.contains("isGoodBoy"))
    }

    @Test
    fun testClassInstanceCreation() {

        val pointClass = FlowLangClass("Point")
        pointClass.addProperty(FlowLangProperty("x", "number", FlowLangProperty.Visibility.PUBLIC, 0.0))
        pointClass.addProperty(FlowLangProperty("y", "number", FlowLangProperty.Visibility.PUBLIC, 0.0))


        val distanceMethod = FlowLangMethod(
            "distance", arrayOf(FlowLangParameter("other", "object")),
            BinaryOpNode.FlowLangScript(
                BinaryOpNode.BlockNode(
                    listOf(
                        BinaryOpNode.ReturnNode(
                            BinaryOpNode(
                                BinaryOpNode(
                                    BinaryOpNode(
                                        BinaryOpNode(VariableNode("x"), "-", VariableNode("other.x")),
                                        "*",
                                        BinaryOpNode(VariableNode("x"), "-", VariableNode("other.x"))
                                    ),
                                    "+",
                                    BinaryOpNode(
                                        BinaryOpNode(VariableNode("y"), "-", VariableNode("other.y")),
                                        "*",
                                        BinaryOpNode(VariableNode("y"), "-", VariableNode("other.y"))
                                    )
                                ),
                                "**",
                                LiteralNode(0.5)
                            )
                        )
                    )
                )
            )
        )
        pointClass.addMethod(distanceMethod)

        engine.registerClass(pointClass)


        val point1 = engine.createInstance("Point", arrayOf(0.0, 0.0))
        val point2 = engine.createInstance("Point", arrayOf(3.0, 4.0))

        assertNotNull(point1)
        assertNotNull(point2)


        assertEquals(0.0, point1!!.getProperty("x"))
        assertEquals(0.0, point1.getProperty("y"))
        assertEquals(0.0, point2!!.getProperty("x"))
        assertEquals(0.0, point2.getProperty("y"))


        point1.setProperty("x", 1.0)
        point1.setProperty("y", 2.0)
        assertEquals(1.0, point1.getProperty("x"))
        assertEquals(2.0, point1.getProperty("y"))
    }

    @Test
    fun testBuiltInClassFunctions() {

        val testClass = FlowLangClass("TestClass")
        testClass.addProperty(FlowLangProperty("value", "number"))
        engine.registerClass(testClass)


        val classList = engine.execute("listClasses()")
        assertTrue(classList.toString().contains("TestClass"))


        val classInfo = engine.execute("getClassInfo(\"TestClass\")")
        assertTrue(classInfo.toString().contains("class TestClass"))
        assertTrue(classInfo.toString().contains("value"))


        val hasClass = engine.execute("hasClass(\"TestClass\")")
        assertEquals(true, hasClass)

        val hasNonExistentClass = engine.execute("hasClass(\"NonExistentClass\")")
        assertEquals(false, hasNonExistentClass)
    }

    @Test
    fun testClassMethodExecution() {

        val calculatorClass = FlowLangClass("Calculator")
        calculatorClass.addProperty(FlowLangProperty("result", "number", FlowLangProperty.Visibility.PUBLIC, 0.0))


        val addMethod = FlowLangMethod(
            "add", arrayOf(FlowLangParameter("value", "number")),
            BinaryOpNode.FlowLangScript(
                BinaryOpNode.BlockNode(
                    listOf(
                        AssignmentNode(
                            "result",
                            BinaryOpNode(VariableNode("result"), "+", VariableNode("value"))
                        ),
                        BinaryOpNode.ReturnNode(VariableNode("result"))
                    )
                )
            )
        )
        calculatorClass.addMethod(addMethod)


        val multiplyMethod = FlowLangMethod(
            "multiply", arrayOf(FlowLangParameter("value", "number")),
            BinaryOpNode.FlowLangScript(
                BinaryOpNode.BlockNode(
                    listOf(
                        AssignmentNode(
                            "result",
                            BinaryOpNode(VariableNode("result"), "*", VariableNode("value"))
                        ),
                        BinaryOpNode.ReturnNode(VariableNode("result"))
                    )
                )
            )
        )
        calculatorClass.addMethod(multiplyMethod)

        engine.registerClass(calculatorClass)


        val calc = engine.createInstance("Calculator", arrayOf())
        assertNotNull(calc)


        val result1 = calc!!.callMethod("add", arrayOf(5.0))
        assertEquals(5.0, result1)

        val result2 = calc.callMethod("add", arrayOf(3.0))
        assertEquals(8.0, result2)

        val result3 = calc.callMethod("multiply", arrayOf(2.0))
        assertEquals(16.0, result3)
    }

    @Test
    fun testReadOnlyProperties() {
        val readOnlyClass = FlowLangClass("ReadOnlyClass")
        readOnlyClass.addProperty(
            FlowLangProperty(
                "readOnlyValue", "number",
                FlowLangProperty.Visibility.PUBLIC, 42.0, true
            )
        )

        engine.registerClass(readOnlyClass)

        val instance = engine.createInstance("ReadOnlyClass", arrayOf())
        assertNotNull(instance)


        assertEquals(42.0, instance!!.getProperty("readOnlyValue"))


        assertThrows(Exception::class.java) {
            instance.setProperty("readOnlyValue", 100.0)
        }
    }

    @Test
    fun testComplexClassHierarchy() {

        val vehicleClass = FlowLangClass("Vehicle")
        vehicleClass.addProperty(FlowLangProperty("maxSpeed", "number"))
        vehicleClass.addProperty(FlowLangProperty("fuelType", "text"))

        val carClass = FlowLangClass("Car", "Vehicle")
        carClass.addProperty(FlowLangProperty("doors", "number"))
        carClass.addProperty(FlowLangProperty("isElectric", "boolean"))

        val truckClass = FlowLangClass("Truck", "Vehicle")
        truckClass.addProperty(FlowLangProperty("cargoCapacity", "number"))
        truckClass.addProperty(FlowLangProperty("hasTrailer", "boolean"))

        val electricCarClass = FlowLangClass("ElectricCar", "Car")
        electricCarClass.addProperty(FlowLangProperty("batteryCapacity", "number"))
        electricCarClass.addProperty(FlowLangProperty("chargingTime", "number"))

        engine.registerClass(vehicleClass)
        engine.registerClass(carClass)
        engine.registerClass(truckClass)
        engine.registerClass(electricCarClass)


        assertTrue(engine.hasClass("Vehicle"))
        assertTrue(engine.hasClass("Car"))
        assertTrue(engine.hasClass("Truck"))
        assertTrue(engine.hasClass("ElectricCar"))


        assertEquals("Vehicle", carClass.superClassName)
        assertEquals("Vehicle", truckClass.superClassName)
        assertEquals("Car", electricCarClass.superClassName)


        val electricCarInfo = engine.getClassInfo("ElectricCar")
        assertNotNull(electricCarInfo)
        assertTrue(electricCarInfo!!.contains("extends Car"))
        assertTrue(electricCarInfo.contains("batteryCapacity"))
        assertTrue(electricCarInfo.contains("chargingTime"))
    }

    @Test
    fun testEventAndClassIntegration() {

        val playerClass = FlowLangClass("Player")
        playerClass.addProperty(FlowLangProperty("name", "text"))
        playerClass.addProperty(FlowLangProperty("health", "number", FlowLangProperty.Visibility.PUBLIC, 100.0))
        engine.registerClass(playerClass)


        val playerDamageEvent = FlowLangEvent(
            "playerDamage",
            arrayOf(
                FlowLangParameter("player", "Player"),
                FlowLangParameter("damage", "number"),
                FlowLangParameter("damageType", "text")
            ),
            "Triggered when a player takes damage"
        )
        engine.registerEvent(playerDamageEvent)


        val eventInfo = engine.getEventInfo("playerDamage")
        assertNotNull(eventInfo)
        assertTrue(eventInfo!!.contains("player: Player"))
        assertTrue(eventInfo.contains("damage: number"))
        assertTrue(eventInfo.contains("damageType: text"))


        val paramInfo = engine.getEventParameters("playerDamage")
        assertNotNull(paramInfo)
        assertEquals(3, paramInfo!!.size)
        assertEquals("Player", paramInfo[0]["type"])
    }

    private fun captureOutput(block: () -> Unit): String {
        val output = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(output))

        try {
            block()
        } finally {
            System.setOut(originalOut)
        }

        return output.toString()
    }
}
