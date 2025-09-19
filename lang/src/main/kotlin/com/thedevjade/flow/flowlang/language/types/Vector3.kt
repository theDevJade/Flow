package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.types

data class Vector3(
    var x: Double,
    var y: Double,
    var z: Double
) {
    override fun toString(): String = "Vector3($x, $y, $z)"

    operator fun plus(other: Vector3): Vector3 = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3): Vector3 = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Double): Vector3 = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Double): Vector3 = Vector3(x / scalar, y / scalar, z / scalar)
}
