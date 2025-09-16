package com.thedevjade.flow.flowPlugin.utils

import java.io.File
import java.util.jar.JarFile

object ReflectionUtils {
    fun getClassesInPackage(pkg: String): List<Class<*>> {
        val classLoader = Thread.currentThread().contextClassLoader
        val path = pkg.replace('.', '/')
        val resources = classLoader.getResources(path)
        val classes = mutableListOf<Class<*>>()

        while (resources.hasMoreElements()) {
            val url = resources.nextElement()
            when (url.protocol) {
                "file" -> {
                    val dir = File(url.toURI())
                    dir.walkTopDown()
                        .filter { it.isFile && it.name.endsWith(".class") }
                        .forEach {
                            val className = pkg + "." + it.name.removeSuffix(".class")
                            classes += Class.forName(className)
                        }
                }
                "jar" -> {
                    val connection = url.openConnection() as java.net.JarURLConnection
                    val jarFile = connection.jarFile
                    classes += scanJar(jarFile, pkg)
                }
            }
        }

        return classes
    }

    private fun scanJar(jar: JarFile, pkg: String): List<Class<*>> {
        val path = pkg.replace('.', '/')
        return jar.entries()
            .asSequence()
            .filter { it.name.startsWith(path) && it.name.endsWith(".class") && !it.isDirectory }
            .map {
                val className = it.name.removeSuffix(".class").replace('/', '.')
                Class.forName(className)
            }
            .toList()
    }
}