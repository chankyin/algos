package io.github.chankyin.algos.ann

import kotlin.math.exp

interface Activator {
	/**
	 * The activator function
	 */
	fun act(x: Double): Double

	/**
	 * The first derivative of the activator function
	 * @param x The value returned from the activator function
	 */
	fun dev(x: Double): Double
}

val sigmoid = object : Activator {
	override fun act(x: Double) = 1 / (1 + exp(-x))
	override fun dev(x: Double) = x * (1 - x)
}

val purelin = object : Activator {
	override fun act(x: Double) = x
	override fun dev(x: Double) = 1.0
}
