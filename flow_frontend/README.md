# 🌊 Flow

[![Kotlin](https://img.shields.io/badge/kotlin-2.2-blue?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Flutter](https://img.shields.io/badge/flutter-app-blue?logo=flutter&logoColor=white)](https://flutter.dev/)
[![Build](https://img.shields.io/github/actions/workflow/status/thedevjade/Flow/automated-build.yml?label=build&logo=github)](https://github.com/thedevjade/Flow/actions)
[![Docs](https://img.shields.io/badge/docs-online-success?logo=readthedocs&logoColor=white)](https://thedevjade.github.io/Flow/)
[![License](https://img.shields.io/github/license/thedevjade/Flow?color=yellow)](LICENSE)

> **Flow** is a Kotlin-infused playspace combining **FlowLang**, a custom scripting language, with a **graph editor**
> and **terminal**—all optimized for performance and extensibility.

---

## 🖼 Preview

<p align="center">
  <img src="assets/banner.png" alt="Flow Banner" width="80%"/>
</p>

---

## ✨ Features

- ⚡ **FlowLang** – Lightweight scripting language designed specifically for Flow.
- 🖥 **Graph Editor** – Visual flowchart maker with **live execution** support.
- 🎯 **Extension System** – Powerful, reflection-based extension system with hot reloading.
- 🔥 **Hot Reloading** – Automatic extension reloading without restart.
- 📡 **WebSocket Integration** – Real-time communication and data handling.
- 🧩 **Developer API** – Extend and modify the language, graph editor, or terminal.
- 📱 **Custom Flutter App** – Cross-platform UI with performance in mind.

---

## 📸 Screenshots

<p align="center">
  <img src="assets/screenshot-graph.png" alt="Graph Editor" width="45%"/>
  <img src="assets/screenshot-terminal.png" alt="Flow Terminal" width="45%"/>
</p>

---

## 🎯 Extension System

Flow features a powerful, reflection-based extension system that makes it incredibly easy to create custom
functionality:

### Key Features

- 🔥 **Hot Reloading** - Automatic reloading when you modify JAR files
- 🎯 **Graph Nodes** - Create TRIGGER and ACTION nodes with simple annotations
- 🔧 **FlowLang Integration** - Add functions, events, and types to FlowLang
- ⌨️ **Terminal Commands** - Add CLI commands with simple annotations
- 🛡️ **Type Safety** - Full Kotlin type safety throughout
- 📦 **JAR Support** - Load extensions from JAR files

### Quick Example

```kotlin
@TriggerNode(name = "User Login", category = "Auth")
class UserLoginTrigger : SimpleTriggerNode() {
    override suspend fun execute(): TriggerResult {
        log("User logged in!")
        return TriggerResult.Success
    }
}

@ActionNode(name = "Send Email", category = "Communication")
class SendEmailAction : SimpleActionNode() {
    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        val recipient = inputs["recipient"] as? String ?: return ActionResult.Error("No recipient")
        // Send email logic...
        return ActionResult.Success(mapOf("messageId" to "123"))
    }
}
```

## 📖 Documentation

Full docs available here:  
👉 [**Flow Documentation**](https://thedevjade.github.io/Flow/)

---

## 🚧 Status

This project is **very WIP** (work-in-progress).  
If you encounter **bugs** or **unexpected behavior**:

- Open an [issue](https://github.com/thedevjade/Flow/issues)
- Or contact me directly on Discord: `aballofnewsies`

---

## ✅ TODO

- [X] Improve **graph editor UX** (zoom, pan, snapping).
- [X] Implement **extension system** with hot reloading and reflection-based discovery.
- [X] Implement **graph executor** to run flow charts with TRIGGER and ACTION nodes.
- [X] Implement **FlowLang integration** for extensions.
- [ ] Implement **debugging tools** for the scripting engine.
- [ ] Implement full **Minecraft** support with extensions and events
- [ ] Improve performance

---

## 📜 License

This project is licensed under the terms of the [GNU AFFERO GENERAL PUBLIC LICENSE](LICENSE).
