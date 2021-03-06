package io.github.chankyin.algos.math

import kotlin.math.*

internal typealias number = Double
internal typealias numberArray = DoubleArray

internal val toNumber: (Number)->Double = {it.toDouble()}

//fun c(vararg values: Number): Vector {
//	val dims = numberArray(values.size)
//	for (i in 0 until values.size) {
//		dims[i] = toNumber(values[i])
//	}
//	return Vector(dims)
//}

fun c(vararg values: number) = Vector(values)

class Vector(val values: numberArray) {
	constructor(dimension: Int, f: (i: Int)->number) : this(numberArray(dimension, f))

	val dimension: Int get() = values.size
	val modulusSquared: number get() = values.sum()
	val modulus: number get() = sqrt(modulusSquared)

	operator fun get(i: Int) = values[i]
	operator fun set(i: Int, value: number) = values.set(i, value)

	operator fun plus(value: number) = Vector(dimension) {values[it] + value}
	operator fun minus(value: number) = Vector(dimension) {values[it] - value}
	operator fun times(value: number) = Vector(dimension) {values[it] * value}
	operator fun div(value: number) = Vector(dimension) {values[it] / value}

	operator fun unaryMinus() = Vector(dimension) {-values[it]}

	operator fun plus(that: Vector) = biMap(that, number::plus)
	operator fun minus(that: Vector) = biMap(that, number::minus)
	operator fun times(that: Vector) = biMap(that, number::times)

	infix fun dot(that: Vector) = (this * that).values.sum()

	override fun equals(other: Any?) = other is Vector && values.contentEquals(other.values)
	override fun hashCode(): Int = values.hashCode()

	override fun toString() = toString(null)
	fun toString(sigFig: Int? = null): String {
		return values.map {
			if(sigFig == null || it == 0.0) return@map it.toString()
			val unit = floor(log10(abs(it))) // 0.10 to 0.99 = -1
			val factor = 10.0.pow(-unit + sigFig - 1) // 0.10 * 10^+1 = 1.0, * 10^(sigFig-1)
			if((factor*it).isNaN()){
				println()
			}
			(it * factor).roundToLong() / factor
		}.joinToString(prefix = "(", postfix = ")")
	}

	fun matrixRow() = Matrix(values, 1, dimension)
	fun matrixCol() = Matrix(values, dimension, 1)
	fun matrixDiagonal() = Matrix(dimension, dimension) {i, j -> if(i == j) values[i] else 0.0}

	fun map(f: (number)->number) = Vector(values.size) {f(values[it])}

	private inline fun biMap(that: Vector, mapper: (number, number)->number): Vector {
		if(this.dimension != that.dimension) {
			throw IllegalArgumentException("Dimensions are incompatible: ${this.dimension} != ${that.dimension}")
		}

		return Vector(numberArray(dimension) {mapper(this.values[it], that.values[it])})
	}

	private inline fun biMapAssign(that: Vector, mapper: (number, number)->number) {
		if(this.dimension != that.dimension) throw IllegalArgumentException("Dimensions are incompatible")
		for(i in 0 until values.size) {
			values[i] = mapper(values[i], that.values[i])
		}
	}
}
