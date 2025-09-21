---
title: Examples
description: Real-world FlowLang usage patterns and code examples
---

# FlowLang Examples

This collection of examples demonstrates the versatility and power of FlowLang for various programming tasks, from simple scripts to complex data processing and integration scenarios.

## Basic Examples

### Hello World

```flowlang
print("Hello, World!")
```

**Expected Output:**
```
Hello, World!
```

### Variable Operations

```flowlang
var name = "FlowLang"
var version = 1.0
var isAwesome = true

print("Language: " + name)
print("Version: " + version)
print("Is Awesome: " + isAwesome)
```

**Expected Output:**
```
Language: FlowLang
Version: 1.0
Is Awesome: true
```

### Simple Calculator

```flowlang
var a = 10
var b = 5

print("a + b = " + (a + b))
print("a - b = " + (a - b))
print("a * b = " + (a * b))
print("a / b = " + (a / b))
print("a % b = " + (a % b))
```

**Expected Output:**
```
a + b = 15
a - b = 5
a * b = 50
a / b = 2
a % b = 0
```

### Basic Control Flow

```flowlang
var x = 10
var y = 20
print("x + y = " + (x + y))

if (x > 5) {
    print("x is greater than 5")
} else {
    print("x is not greater than 5")
}

function greet(name) {
    return "Hello, " + name + "!"
}

print(greet("FlowLang"))
```

**Expected Output:**
```
x + y = 30
x is greater than 5
Hello, FlowLang!
```

### Natural Language Processing

```flowlang
print("Testing natural language processing...")

# This should be converted to: var total = price + tax
set total to price plus tax

# This should be converted to: if score > 50
if score is greater than 50 then
    print("Score is high!")
end if

# This should be converted to: var result = a * b
set result to a times b

print("Natural language processing test completed!")
```

**Expected Output:**
```
Testing natural language processing...
Score is high!
Natural language processing test completed!
```

**Note:** This example demonstrates FlowLang's natural language preprocessing capabilities. The natural language statements are automatically converted to standard FlowLang syntax before execution.

## Mathematical Examples

### Factorial Function

```flowlang
function factorial(n) {
    if (n <= 1) {
        return 1
    } else {
        return n * factorial(n - 1)
    }
}

print("Factorial of 5: " + factorial(5))
print("Factorial of 10: " + factorial(10))
```

### Fibonacci Sequence

```flowlang
function fibonacci(n) {
    if (n <= 1) {
        return n
    } else {
        return fibonacci(n - 1) + fibonacci(n - 2)
    }
}

print("Fibonacci sequence:")
var i = 0
while (i < 10) {
    print("F(" + i + ") = " + fibonacci(i))
    i = i + 1
}
```

### Prime Number Checker

```flowlang
function isPrime(n) {
    if (n < 2) {
        return false
    }
    
    var i = 2
    while (i * i <= n) {
        if (n % i == 0) {
            return false
        }
        i = i + 1
    }
    return true
}

print("Prime numbers from 1 to 20:")
var num = 1
while (num <= 20) {
    if (isPrime(num)) {
        print(num + " is prime")
    }
    num = num + 1
}
```

### Greatest Common Divisor

```flowlang
function gcd(a, b) {
    while (b != 0) {
        var temp = b
        b = a % b
        a = temp
    }
    return a
}

print("GCD of 48 and 18: " + gcd(48, 18))
print("GCD of 100 and 25: " + gcd(100, 25))
```

## String Processing

### String Concatenation

```flowlang
var firstName = "John"
var lastName = "Doe"
var fullName = firstName + " " + lastName

print("Full name: " + fullName)
print("Length: " + fullName.length)
```

### String Reversal

```flowlang
function reverseString(str) {
    var reversed = ""
    var i = str.length - 1
    
    while (i >= 0) {
        reversed = reversed + str[i]
        i = i - 1
    }
    
    return reversed
}

var original = "FlowLang"
var reversed = reverseString(original)
print("Original: " + original)
print("Reversed: " + reversed)
```

### Word Count

```flowlang
function countWords(text) {
    var words = 0
    var inWord = false
    var i = 0
    
    while (i < text.length) {
        var char = text[i]
        
        if (char != " " and char != "\t" and char != "\n") {
            if (not inWord) {
                words = words + 1
                inWord = true
            }
        } else {
            inWord = false
        }
        
        i = i + 1
    }
    
    return words
}

var text = "Hello world from FlowLang"
var wordCount = countWords(text)
print("Text: " + text)
print("Word count: " + wordCount)
```

## Control Flow Examples

### Number Guessing Game

```flowlang
var secretNumber = 42
var guess = 0
var attempts = 0
var maxAttempts = 5

print("Welcome to the Number Guessing Game!")
print("I'm thinking of a number between 1 and 100")

while (guess != secretNumber and attempts < maxAttempts) {
    # In a real application, this would get input from user
    guess = 42  # Simulated guess
    attempts = attempts + 1
    
    if (guess < secretNumber) {
        print("Too low! Try again.")
    } else if (guess > secretNumber) {
        print("Too high! Try again.")
    } else {
        print("Correct! You guessed it in " + attempts + " attempts!")
    }
}

if (attempts >= maxAttempts and guess != secretNumber) {
    print("Game over! The number was " + secretNumber)
}
```

### Grade Calculator

```flowlang
function calculateGrade(score) {
    if (score >= 90) {
        return "A"
    } else if (score >= 80) {
        return "B"
    } else if (score >= 70) {
        return "C"
    } else if (score >= 60) {
        return "D"
    } else {
        return "F"
    }
}

var scores = [85, 92, 78, 96, 88, 73, 91]
var i = 0

print("Student Grades:")
while (i < scores.length) {
    var grade = calculateGrade(scores[i])
    print("Score " + scores[i] + " = Grade " + grade)
    i = i + 1
}
```

### Temperature Converter

```flowlang
function celsiusToFahrenheit(celsius) {
    return (celsius * 9 / 5) + 32
}

function fahrenheitToCelsius(fahrenheit) {
    return (fahrenheit - 32) * 5 / 9
}

print("Temperature Converter")
print("25°C = " + celsiusToFahrenheit(25) + "°F")
print("77°F = " + fahrenheitToCelsius(77) + "°C")
```

## Function Examples

### Explicit Type Declarations

```flowlang
print("Testing FlowLang Advanced Features...")

# Explicit type declarations
var name: String = "FlowLang"
var version: Number = 2.0
var isAwesome: Boolean = true
var features: List = ["overloading", "types", "defaults"]

print("Name: " + name)
print("Version: " + version)
print("Awesome: " + isAwesome)
print("Features: " + features)

# Mix explicit and implicit types
var implicitString = "This is inferred as String"
var explicitString: String = "This is explicitly typed"

print("Implicit: " + implicitString)
print("Explicit: " + explicitString)
```

**Expected Output:**
```
Testing FlowLang Advanced Features...
Name: FlowLang
Version: 2.0
Awesome: true
Features: [overloading, types, defaults]
Implicit: This is inferred as String
Explicit: This is explicitly typed
```

### Function Overloading

```flowlang
# Define overloaded functions
function greet() {
    return "Hello!"
}

function greet(name: String) {
    return "Hello, " + name + "!"
}

function greet(name: String, age: Number) {
    return "Hello, " + name + "! You are " + age + " years old."
}

function greet(name: String, age: Number, city: String) {
    return "Hello, " + name + "! You are " + age + " years old and live in " + city + "."
}

# Test different overloads
print(greet())
print(greet("Alice"))
print(greet("Bob", 25))
print(greet("Charlie", 30, "New York"))
```

**Expected Output:**
```
Hello!
Hello, Alice!
Hello, Bob! You are 25 years old.
Hello, Charlie! You are 30 years old and live in New York.
```

### Default Parameter Values

```flowlang
# Function with default parameters
function createUser(name: String, age: Number = 18, isActive: Boolean = true, role: String = "user") {
    return "User: " + name + ", Age: " + age + ", Active: " + isActive + ", Role: " + role
}

# Test with different parameter combinations
print(createUser("Alice"))
print(createUser("Bob", 25))
print(createUser("Charlie", 30, true))
print(createUser("David", 35, false, "admin"))
```

**Expected Output:**
```
User: Alice, Age: 18, Active: true, Role: user
User: Bob, Age: 25, Active: true, Role: user
User: Charlie, Age: 30, Active: true, Role: user
User: David, Age: 35, Active: false, Role: admin
```

### Complex Function Overloading

```flowlang
# Mathematical operations with overloading
function calculate(a: Number) {
    return a * a  # Square
}

function calculate(a: Number, b: Number) {
    return a + b  # Addition
}

function calculate(a: Number, b: Number, operation: String = "add") {
    if (operation == "add") {
        return a + b
    } else if (operation == "multiply") {
        return a * b
    } else if (operation == "subtract") {
        return a - b
    } else if (operation == "divide") {
        return a / b
    } else {
        return 0
    }
}

# Test different mathematical operations
print("Square of 5: " + calculate(5))
print("5 + 3: " + calculate(5, 3))
print("5 + 3 (explicit): " + calculate(5, 3, "add"))
print("5 * 3: " + calculate(5, 3, "multiply"))
print("5 - 3: " + calculate(5, 3, "subtract"))
print("15 / 3: " + calculate(15, 3, "divide"))
```

**Expected Output:**
```
Square of 5: 25
5 + 3: 8
5 + 3 (explicit): 8
5 * 3: 15
5 - 3: 2
15 / 3: 5
```

### Advanced Default Values

```flowlang
# Function with complex default values
function formatMessage(greeting: String = "Hello", name: String = "World", punctuation: String = "!", emphasis: Boolean = false) {
    var message = greeting + ", " + name + punctuation
    if (emphasis) {
        message = message + " (EMPHASIS!)"
    }
    return message
}

# Test different combinations
print(formatMessage())
print(formatMessage("Hi"))
print(formatMessage("Hi", "Alice"))
print(formatMessage("Hi", "Alice", "?"))
print(formatMessage("Hi", "Alice", "?", true))
```

**Expected Output:**
```
Hello, World!
Hi, World!
Hi, Alice!
Hi, Alice?
Hi, Alice? (EMPHASIS!)
```

### Utility Functions with Overloading

```flowlang
# String manipulation functions
function repeat(text: String) {
    return text + text
}

function repeat(text: String, count: Number) {
    var result = ""
    var i = 0
    while (i < count) {
        result = result + text
        i = i + 1
    }
    return result
}

function repeat(text: String, count: Number, separator: String = "") {
    var result = ""
    var i = 0
    while (i < count) {
        if (i > 0) {
            result = result + separator
        }
        result = result + text
        i = i + 1
    }
    return result
}

# Test string repetition
print("Repeat 'Hi': " + repeat("Hi"))
print("Repeat 'Hi' 3 times: " + repeat("Hi", 3))
print("Repeat 'Hi' 3 times with separator: " + repeat("Hi", 3, ", "))
```

**Expected Output:**
```
Repeat 'Hi': HiHi
Repeat 'Hi' 3 times: HiHiHi
Repeat 'Hi' 3 times with separator: Hi, Hi, Hi
```

### Data Processing Functions

```flowlang
# Function to process data with different parameter combinations
function processData(data: String, count: Number = 1, verbose: Boolean = false, prefix: String = "Data") {
    var result = prefix + ": " + data
    if (verbose) {
        result = result + " (processed " + count + " times)"
    }
    return result
}

# Test data processing
print(processData("test"))
print(processData("test", 5))
print(processData("test", 5, true))
print(processData("test", 5, true, "Result"))
```

**Expected Output:**
```
Data: test
Data: test
Data: test (processed 5 times)
Result: test (processed 5 times)
```

### Math Library

```flowlang
function power(base, exponent) {
    var result = 1
    var i = 0
    
    while (i < exponent) {
        result = result * base
        i = i + 1
    }
    
    return result
}

function squareRoot(n) {
    if (n < 0) {
        return -1  # Error indicator
    }
    
    var guess = n / 2
    var i = 0
    
    while (i < 10) {  # Newton's method approximation
        guess = (guess + n / guess) / 2
        i = i + 1
    }
    
    return guess
}

print("2^3 = " + power(2, 3))
print("5^4 = " + power(5, 4))
print("√16 = " + squareRoot(16))
print("√25 = " + squareRoot(25))
```

### Array Operations

```flowlang
function findMax(numbers) {
    var max = numbers[0]
    var i = 1
    
    while (i < numbers.length) {
        if (numbers[i] > max) {
            max = numbers[i]
        }
        i = i + 1
    }
    
    return max
}

function findMin(numbers) {
    var min = numbers[0]
    var i = 1
    
    while (i < numbers.length) {
        if (numbers[i] < min) {
            min = numbers[i]
        }
        i = i + 1
    }
    
    return min
}

function calculateAverage(numbers) {
    var sum = 0
    var i = 0
    
    while (i < numbers.length) {
        sum = sum + numbers[i]
        i = i + 1
    }
    
    return sum / numbers.length
}

var numbers = [5, 2, 8, 1, 9, 3, 7, 4, 6]
print("Numbers: " + numbers)
print("Maximum: " + findMax(numbers))
print("Minimum: " + findMin(numbers))
print("Average: " + calculateAverage(numbers))
```

### Sorting Algorithm

```flowlang
function bubbleSort(arr) {
    var n = arr.length
    var i = 0
    
    while (i < n - 1) {
        var j = 0
        while (j < n - i - 1) {
            if (arr[j] > arr[j + 1]) {
                # Swap elements
                var temp = arr[j]
                arr[j] = arr[j + 1]
                arr[j + 1] = temp
            }
            j = j + 1
        }
        i = i + 1
    }
    
    return arr
}

var unsorted = [64, 34, 25, 12, 22, 11, 90]
print("Unsorted: " + unsorted)
var sorted = bubbleSort(unsorted)
print("Sorted: " + sorted)
```

## Event System Examples

### User Authentication System

```flowlang
# Event handlers
on userLogin {
    print("User " + username + " logged in successfully")
    print("Login time: " + timestamp)
    print("IP address: " + ipAddress)
    
    # Log the event
    logEvent("login", username)
}

on userLogout {
    print("User " + username + " logged out")
    print("Session duration: " + sessionDuration + " minutes")
    
    # Clean up user session
    cleanupSession(username)
}

on loginFailed {
    print("Login failed for user: " + username)
    print("Reason: " + reason)
    print("Attempt count: " + attemptCount)
    
    # Security logging
    logSecurityEvent("failed_login", username)
}

# Helper functions
function logEvent(eventType, username) {
    print("Event logged: " + eventType + " for " + username)
}

function cleanupSession(username) {
    print("Session cleaned up for " + username)
}

function logSecurityEvent(eventType, username) {
    print("Security event: " + eventType + " for " + username)
}
```

### Data Processing Pipeline

```flowlang
# Event handlers for data processing
on dataReceived {
    print("Data received from " + source)
    print("Data size: " + dataSize + " bytes")
    
    # Process the data
    var processedData = processData(rawData)
    
    # Trigger next event
    trigger dataProcessed(processedData, source)
}

on dataProcessed {
    print("Data processed successfully")
    print("Processed data: " + processedData)
    
    # Store the result
    storeData(processedData, source)
    
    # Notify completion
    trigger dataStored(processedData, source)
}

on dataStored {
    print("Data stored successfully")
    print("Storage location: " + storageLocation)
    
    # Send notification
    sendNotification("Data processing completed for " + source)
}

# Helper functions
function processData(rawData) {
    print("Processing data...")
    # Simulate data processing
    return "processed_" + rawData
}

function storeData(data, source) {
    print("Storing data for " + source)
    # Simulate data storage
    return "storage_location_" + source
}

function sendNotification(message) {
    print("Notification sent: " + message)
}
```

## Natural Language Examples

### Business Logic in Natural Language

```flowlang
# Natural language business rules
set customerDiscount to 0
if customerType equals "premium" then
    set customerDiscount to 0.15
else if customerType equals "gold" then
    set customerDiscount to 0.10
else if customerType equals "silver" then
    set customerDiscount to 0.05
end if

set subtotal to itemPrice times quantity
set discountAmount to subtotal times customerDiscount
set taxAmount to subtotal minus discountAmount times 0.08
set totalAmount to subtotal minus discountAmount plus taxAmount

print("Subtotal: " + subtotal)
print("Discount: " + discountAmount)
print("Tax: " + taxAmount)
print("Total: " + totalAmount)
```

### Inventory Management

```flowlang
# Natural language inventory rules
set reorderLevel to 50
set currentStock to 75
set orderQuantity to 100

if currentStock is less than reorderLevel then
    print("Low stock alert!")
    set orderQuantity to 200
    set priority to "high"
else if currentStock is less than reorderLevel times 1.5 then
    print("Stock getting low")
    set priority to "medium"
else
    print("Stock level is good")
    set priority to "low"
end if

if priority equals "high" then
    print("Urgent reorder needed: " + orderQuantity + " units")
else if priority equals "medium" then
    print("Schedule reorder: " + orderQuantity + " units")
end if
```

### Financial Calculations

```flowlang
# Natural language financial calculations
set principal to 10000
set annualRate to 0.05
set years to 5
set compoundFrequency to 12

set monthlyRate to annualRate divided by compoundFrequency
set totalPeriods to years times compoundFrequency
set compoundInterest to principal times (1 plus monthlyRate) to the power of totalPeriods
set simpleInterest to principal times annualRate times years

print("Principal: " + principal)
print("Annual Rate: " + annualRate)
print("Years: " + years)
print("Compound Interest: " + compoundInterest)
print("Simple Interest: " + simpleInterest)
print("Difference: " + (compoundInterest - simpleInterest))
```

## Class Examples

### Enhanced Class System with Inheritance

```flowlang
print("Testing FlowLang Enhanced Features...")

# Define a base class
class Animal {
    var name: text
    var species: text
    
    function makeSound() {
        return "Some generic sound"
    }
}

# Define a derived class
class Dog extends Animal {
    var breed: text
    var isGoodBoy: boolean = true
    
    function makeSound() {
        return "Woof!"
    }
    
    function fetch() {
        return name + " is fetching the ball!"
    }
}

# Define another derived class
class Cat extends Animal {
    var lives: number = 9
    var isIndoor: boolean = true
    
    function makeSound() {
        return "Meow!"
    }
    
    function purr() {
        return name + " is purring contentedly"
    }
}

# Test class information
print("Available classes: " + listClasses())
print("Animal class info: " + getClassInfo("Animal"))
print("Dog class info: " + getClassInfo("Dog"))
print("Cat class info: " + getClassInfo("Cat"))

# Create instances
var myDog = new Dog()
var myCat = new Cat()

# Set properties
myDog.name = "Buddy"
myDog.species = "Canine"
myDog.breed = "Golden Retriever"

myCat.name = "Whiskers"
myCat.species = "Feline"
myCat.lives = 8

# Call methods
print("Dog sound: " + myDog.makeSound())
print("Cat sound: " + myCat.makeSound())
print("Dog fetch: " + myDog.fetch())
print("Cat purr: " + myCat.purr())
```

**Expected Output:**
```
Testing FlowLang Enhanced Features...
Available classes: [Animal, Dog, Cat]
Animal class info: {name: Animal, properties: [name, species], methods: [makeSound]}
Dog class info: {name: Dog, extends: Animal, properties: [breed, isGoodBoy], methods: [makeSound, fetch]}
Cat class info: {name: Cat, extends: Animal, properties: [lives, isIndoor], methods: [makeSound, purr]}
Dog sound: Woof!
Cat sound: Meow!
Dog fetch: Buddy is fetching the ball!
Cat purr: Whiskers is purring contentedly
```

### Complex Class Hierarchy

```flowlang
# Define a vehicle hierarchy
class Vehicle {
    var maxSpeed: number
    var fuelType: text
    var isRunning: boolean = false
    
    function start() {
        isRunning = true
        return "Vehicle started"
    }
    
    function stop() {
        isRunning = false
        return "Vehicle stopped"
    }
}

class Car extends Vehicle {
    var doors: number
    var isElectric: boolean = false
    
    function honk() {
        return "Beep beep!"
    }
}

class ElectricCar extends Car {
    var batteryCapacity: number
    var chargingTime: number
    
    function charge() {
        return "Charging the electric car..."
    }
    
    function honk() {
        return "Electric beep!"
    }
}

# Create and test instances
var myCar = new Car()
myCar.maxSpeed = 120
myCar.fuelType = "Gasoline"
myCar.doors = 4

var myElectricCar = new ElectricCar()
myElectricCar.maxSpeed = 150
myElectricCar.fuelType = "Electric"
myElectricCar.doors = 2
myElectricCar.batteryCapacity = 100
myElectricCar.chargingTime = 8

print("Car honk: " + myCar.honk())
print("Electric car honk: " + myElectricCar.honk())
print("Electric car charge: " + myElectricCar.charge())
```

**Expected Output:**
```
Car honk: Beep beep!
Electric car honk: Electric beep!
Electric car charge: Charging the electric car...
```

### Object-Oriented Game Entities

```flowlang
# Base entity class
class Entity {
    var name: String
    var x: Number = 0
    var y: Number = 0
    var z: Number = 0
    var health: Number = 100
    
    function init(n) {
        this.name = n
    }
    
    function move(newX, newY, newZ) {
        this.x = newX
        this.y = newY
        this.z = newZ
    }
    
    function takeDamage(amount) {
        this.health = this.health - amount
        if (this.health <= 0) {
            print(this.name + " has died!")
        }
    }
}

# Player class
class Player extends Entity {
    var playerId: Number
    var level: Number = 1
    var experience: Number = 0
    
    function init(n, id) {
        super.init(n)
        this.playerId = id
    }
    
    function levelUp() {
        this.level = this.level + 1
        print(this.name + " leveled up to level " + this.level + "!")
    }
    
    function gainExperience(amount) {
        this.experience = this.experience + amount
        if (this.experience >= this.level * 100) {
            this.levelUp()
        }
    }
}

# Enemy class
class Enemy extends Entity {
    var enemyType: String
    var attackPower: Number = 10
    
    function init(n, type) {
        super.init(n)
        this.enemyType = type
    }
    
    function attack(target) {
        print(this.name + " attacks " + target.name + " for " + this.attackPower + " damage!")
        target.takeDamage(this.attackPower)
    }
}

# Create game entities
var player = new Player()
player.init("Alice", 1001)
player.move(10, 20, 30)

var enemy = new Enemy()
enemy.init("Goblin", "monster")
enemy.move(15, 20, 30)

# Game interactions
enemy.attack(player)
player.gainExperience(50)
```

**Expected Output:**
```
Goblin attacks Alice for 10 damage!
Alice gained 50 experience points
```

### Complete Game Event System

```flowlang
# Define game events
event playerJoin(playerName: text, playerId: number) "Player joins the game"
event playerMove(playerName: text, x: number, y: number, z: number) "Player moves to new position"
event playerAttack(playerName: text, target: text, damage: number) "Player attacks another player"

# Event handlers
on playerJoin {
    print("Welcome " + playerName + "! (ID: " + playerId + ")")
}

on playerMove {
    print(playerName + " moved to (" + x + ", " + y + ", " + z + ")")
}

on playerAttack {
    print(playerName + " attacked " + target + " for " + damage + " damage!")
}

# Trigger events
trigger playerJoin("Alice", 1001)
trigger playerMove("Alice", 10.5, 20.0, 30.5)
trigger playerAttack("Alice", "Bob", 25)
```

## Game Development Examples

### Simple RPG Character

```flowlang
# Character attributes
var characterName = "Hero"
var level = 1
var health = 100
var maxHealth = 100
var attack = 10
var defense = 5
var experience = 0
var experienceToNext = 100

# Character functions
function levelUp() {
    level = level + 1
    maxHealth = maxHealth + 20
    health = maxHealth
    attack = attack + 5
    defense = defense + 3
    experienceToNext = experienceToNext * 1.5
    
    print(characterName + " leveled up to level " + level + "!")
    print("Health: " + health + "/" + maxHealth)
    print("Attack: " + attack)
    print("Defense: " + defense)
}

function gainExperience(amount) {
    experience = experience + amount
    print("Gained " + amount + " experience points")
    
    if (experience >= experienceToNext) {
        levelUp()
        experience = experience - experienceToNext
    }
}

function takeDamage(damage) {
    var actualDamage = damage - defense
    if (actualDamage < 0) {
        actualDamage = 0
    }
    
    health = health - actualDamage
    print(characterName + " took " + actualDamage + " damage")
    
    if (health <= 0) {
        print(characterName + " has been defeated!")
    }
}

function heal(amount) {
    health = health + amount
    if (health > maxHealth) {
        health = maxHealth
    }
    print(characterName + " healed for " + amount + " health")
}

# Game events
on enemyDefeated {
    var expGained = enemyLevel * 10
    gainExperience(expGained)
    print("Defeated enemy of level " + enemyLevel)
}

on itemUsed {
    if (itemType == "healing") {
        heal(itemValue)
    } else if (itemType == "attack_boost") {
        attack = attack + itemValue
        print("Attack increased by " + itemValue)
    }
}

# Test the character system
print("Character: " + characterName)
print("Level: " + level)
print("Health: " + health + "/" + maxHealth)

# Simulate some gameplay
gainExperience(50)
takeDamage(15)
heal(10)
gainExperience(75)  # Should trigger level up
```

### Simple Combat System

```flowlang
function calculateDamage(attacker, defender) {
    var baseDamage = attacker.attack
    var defense = defender.defense
    var damage = baseDamage - defense
    
    if (damage < 1) {
        damage = 1
    }
    
    # Add some randomness
    var randomFactor = 0.8 + (Math.random() * 0.4)  # 0.8 to 1.2
    damage = damage * randomFactor
    
    return damage
}

function combatRound(attacker, defender) {
    print(attacker.name + " attacks " + defender.name)
    
    var damage = calculateDamage(attacker, defender)
    defender.health = defender.health - damage
    
    print(defender.name + " takes " + damage + " damage")
    print(defender.name + " health: " + defender.health)
    
    if (defender.health <= 0) {
        print(defender.name + " has been defeated!")
        return true
    }
    
    return false
}

# Create combatants
var player = {
    name: "Hero",
    health: 100,
    attack: 15,
    defense: 5
}

var enemy = {
    name: "Goblin",
    health: 50,
    attack: 8,
    defense: 2
}

# Combat loop
print("Combat begins!")
var round = 1

while (player.health > 0 and enemy.health > 0) {
    print("\n--- Round " + round + " ---")
    
    # Player attacks first
    var playerWins = combatRound(player, enemy)
    if (playerWins) {
        break
    }
    
    # Enemy attacks
    var enemyWins = combatRound(enemy, player)
    if (enemyWins) {
        break
    }
    
    round = round + 1
}

if (player.health > 0) {
    print("Victory! " + player.name + " wins!")
} else {
    print("Defeat! " + enemy.name + " wins!")
}
```

## Data Processing Examples

### CSV Parser

```flowlang
function parseCSVLine(line) {
    var fields = []
    var currentField = ""
    var inQuotes = false
    var i = 0
    
    while (i < line.length) {
        var char = line[i]
        
        if (char == '"') {
            inQuotes = not inQuotes
        } else if (char == ',' and not inQuotes) {
            fields = fields + [currentField]
            currentField = ""
        } else {
            currentField = currentField + char
        }
        
        i = i + 1
    }
    
    fields = fields + [currentField]
    return fields
}

function parseCSV(csvData) {
    var lines = csvData.split('\n')
    var rows = []
    var i = 0
    
    while (i < lines.length) {
        if (lines[i].length > 0) {
            var fields = parseCSVLine(lines[i])
            rows = rows + [fields]
        }
        i = i + 1
    }
    
    return rows
}

# Example CSV data
var csvData = "Name,Age,City\nJohn,25,New York\nJane,30,Los Angeles\nBob,35,Chicago"
var parsedData = parseCSV(csvData)

print("Parsed CSV data:")
var i = 0
while (i < parsedData.length) {
    var row = parsedData[i]
    print("Row " + i + ": " + row)
    i = i + 1
}
```

### Data Aggregation

```flowlang
function aggregateData(data, groupBy, aggregateField) {
    var groups = {}
    var i = 0
    
    # Group the data
    while (i < data.length) {
        var row = data[i]
        var groupKey = row[groupBy]
        var value = row[aggregateField]
        
        if (groups[groupKey] == null) {
            groups[groupKey] = []
        }
        
        groups[groupKey] = groups[groupKey] + [value]
        i = i + 1
    }
    
    # Calculate aggregates
    var results = {}
    var groupKeys = Object.keys(groups)
    var j = 0
    
    while (j < groupKeys.length) {
        var key = groupKeys[j]
        var values = groups[key]
        var sum = 0
        var count = values.length
        var k = 0
        
        while (k < values.length) {
            sum = sum + values[k]
            k = k + 1
        }
        
        results[key] = {
            sum: sum,
            average: sum / count,
            count: count
        }
        
        j = j + 1
    }
    
    return results
}

# Sample data
var salesData = [
    ["Product A", "Q1", 100],
    ["Product B", "Q1", 150],
    ["Product A", "Q2", 120],
    ["Product B", "Q2", 180],
    ["Product A", "Q3", 110],
    ["Product B", "Q3", 160]
]

var aggregated = aggregateData(salesData, 0, 2)  # Group by product, aggregate sales

print("Sales by Product:")
var products = Object.keys(aggregated)
var i = 0
while (i < products.length) {
    var product = products[i]
    var stats = aggregated[product]
    print(product + ": Total=" + stats.sum + ", Average=" + stats.average + ", Count=" + stats.count)
    i = i + 1
}
```

## Error Handling Examples

### Basic Error Handling

```flowlang
# FlowLang Error Demonstration Script
# This script demonstrates various error scenarios and their enhanced error reporting

# 1. Valid code - should work without errors
var x = 5
var y = 10
var sum = x + y
print("Sum: " + sum)

# 2. Function definition with error handling
function divide(a, b) {
    if (b == 0) {
        print("Error: Division by zero!")
        return 0
    }
    return a / b
}

var result = divide(10, 2)
print("Division result: " + result)

# 3. Class definition
class Calculator {
    var value = 0
    
    function add(n) {
        this.value = this.value + n
        return this.value
    }
    
    function getValue() {
        return this.value
    }
}

var calc = new Calculator()
calc.add(5)
calc.add(3)
print("Calculator value: " + calc.getValue())

# 4. Error handling example
function safeDivide(a, b) {
    if (b == 0) {
        print("Warning: Attempted division by zero")
        return null
    }
    return a / b
}

var safeResult = safeDivide(10, 0)
if (safeResult != null) {
    print("Safe division result: " + safeResult)
} else {
    print("Division was not performed due to zero divisor")
}
```

**Expected Output:**
```
Sum: 15
Division result: 5
Calculator value: 8
Warning: Attempted division by zero
Division was not performed due to zero divisor
```

### Debug Default Parameters

```flowlang
# Debug default parameters
function test(a, b = 10) {
    return a + b
}

print("Test with default: " + test(5))
print("Test without default: " + test(5, 3))
```

**Expected Output:**
```
Test with default: 15
Test without default: 8
```

### Simple Type Testing

```flowlang
# Simple type test
var name: String = "Hello"
var age: Number = 25
var flag: Boolean = true
print("Name: " + name)
print("Age: " + age)
print("Flag: " + flag)
```

**Expected Output:**
```
Name: Hello
Age: 25
Flag: true
```

## Integration Examples

### REST API Client

```kotlin
// Kotlin code to register API functions
val engine = FlowLangEngine.getInstance()

engine.registerFunction(FlowLangFunction("httpGet", { args ->
    val url = args[0] as String
    // Simulate HTTP GET request
    "Response from $url"
}, arrayOf(
    FlowLangParameter("url", "text")
)))

engine.registerFunction(FlowLangFunction("httpPost", { args ->
    val url = args[0] as String
    val data = args[1] as String
    // Simulate HTTP POST request
    "POST to $url with data: $data"
}, arrayOf(
    FlowLangParameter("url", "text"),
    FlowLangParameter("data", "text")
)))
```

```flowlang
# FlowLang script using API functions
function fetchUserData(userId) {
    var url = "https://api.example.com/users/" + userId
    var response = httpGet(url)
    return response
}

function updateUser(userId, userData) {
    var url = "https://api.example.com/users/" + userId
    var response = httpPost(url, userData)
    return response
}

# Use the API
var userData = fetchUserData("123")
print("User data: " + userData)

var updateResult = updateUser("123", "{\"name\": \"John Doe\"}")
print("Update result: " + updateResult)
```

### Database Operations

```kotlin
// Kotlin code to register database functions
val engine = FlowLangEngine.getInstance()

engine.registerFunction(FlowLangFunction("dbQuery", { args ->
    val sql = args[0] as String
    // Simulate database query
    listOf(
        mapOf("id" to 1, "name" to "John", "email" to "john@example.com"),
        mapOf("id" to 2, "name" to "Jane", "email" to "jane@example.com")
    )
}, arrayOf(
    FlowLangParameter("sql", "text")
)))

engine.registerFunction(FlowLangFunction("dbExecute", { args ->
    val sql = args[0] as String
    // Simulate database execution
    "Query executed: $sql"
}, arrayOf(
    FlowLangParameter("sql", "text")
)))
```

```flowlang
# FlowLang script using database functions
function getAllUsers() {
    var sql = "SELECT * FROM users"
    var results = dbQuery(sql)
    return results
}

function createUser(name, email) {
    var sql = "INSERT INTO users (name, email) VALUES ('" + name + "', '" + email + "')"
    var result = dbExecute(sql)
    return result
}

function updateUserEmail(userId, newEmail) {
    var sql = "UPDATE users SET email = '" + newEmail + "' WHERE id = " + userId
    var result = dbExecute(sql)
    return result
}

# Use the database functions
var users = getAllUsers()
print("Found " + users.length + " users")

var createResult = createUser("Bob Smith", "bob@example.com")
print("Create result: " + createResult)

var updateResult = updateUserEmail(1, "john.doe@example.com")
print("Update result: " + updateResult)
```

### File System Operations

```kotlin
// Kotlin code to register file system functions
val engine = FlowLangEngine.getInstance()

engine.registerFunction(FlowLangFunction("readFile", { args ->
    val filename = args[0] as String
    // Simulate file reading
    "Contents of $filename"
}, arrayOf(
    FlowLangParameter("filename", "text")
)))

engine.registerFunction(FlowLangFunction("writeFile", { args ->
    val filename = args[0] as String
    val content = args[1] as String
    // Simulate file writing
    "Written $content to $filename"
}, arrayOf(
    FlowLangParameter("filename", "text"),
    FlowLangParameter("content", "text")
)))
```

```flowlang
# FlowLang script using file system functions
function processLogFile(filename) {
    var content = readFile(filename)
    var lines = content.split('\n')
    var errorCount = 0
    var i = 0
    
    while (i < lines.length) {
        if (lines[i].contains("ERROR")) {
            errorCount = errorCount + 1
        }
        i = i + 1
    }
    
    return errorCount
}

function generateReport(data, outputFile) {
    var report = "Report Generated at " + System.currentTimeMillis() + "\n"
    report = report + "Data: " + data + "\n"
    report = report + "End of Report"
    
    var result = writeFile(outputFile, report)
    return result
}

# Use the file system functions
var errorCount = processLogFile("app.log")
print("Found " + errorCount + " errors in log file")

var reportResult = generateReport("Sample data", "report.txt")
print("Report generation: " + reportResult)
```

These examples demonstrate the versatility and power of FlowLang for various programming tasks, from simple scripts to complex data processing and integration scenarios.
