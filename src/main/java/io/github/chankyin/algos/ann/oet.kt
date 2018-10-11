package io.github.chankyin.algos.ann

import io.github.chankyin.algos.math.Matrix
import kotlin.math.atanh
import kotlin.math.tanh

class OptimalEstimateTrainingNetwork(
		val inputSize: Int,
		val outputSize: Int
) {
	fun train(a: Matrix, cXp: Matrix) {
		assert(a.m == cXp.m)
		assert(a.n == inputSize);
		assert(cXp.n == outputSize)

		val wInit = a.pseudoInverse() * cXp.map(::atanh)
		val bXp = cXp.map(::atanh) * wInit.pseudoInverse()

		val v = a.pseudoInverse() * bXp.map(::atanh)
		val b = (a * v).map(::tanh)
		val w = b.pseudoInverse() * cXp.map(::atanh)
	}
}
