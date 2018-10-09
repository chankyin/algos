package ann

import math.Matrix
import math.Vector
import kotlin.math.sqrt

class Sofm(
		val inputSize: Int,
		val outputSize: Int,
		val learnRate: Double
) {
	val weights = Matrix(inputSize, outputSize) { _, _ -> Math.random() }

	fun apply(input: Vector) {
		assert(input.dimension == inputSize)
		val dist = Vector(outputSize) { j ->
			var out = 0.0
			for (i in 0 until inputSize) {
				out += (input[i] - weights[i, j])
			}
			sqrt(out)
		}
	}
}
