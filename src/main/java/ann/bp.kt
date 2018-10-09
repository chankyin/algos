package ann

import math.*
import kotlin.math.abs
import kotlin.math.max

// generic variable names:
// layer number = i
// node number within layer = j

data class NetworkLayer(
		val size: Int,
		val activate: (Double) -> Double = ::sigmoid,
		val deactivate: (Double) -> Double = ::sigmoidDev
)

class BackPropagatingNetwork(
		val inputSize: Int,
		val layers: Array<NetworkLayer>
) {
	val thresholds: Array<Vector> = Array(layers.size) {
		Vector(layers[it].size) { _ -> Math.random() - 0.5 }
	}

	val weights: Array<Matrix> = Array(layers.size) { i ->
		Matrix(if (i == 0) inputSize else layers[i - 1].size, layers[i].size) { _, _ -> Math.random() - 0.5 }
	}

	private fun activate(i: Int, v: Vector) = v.map(layers[i].activate)
	private fun deactivate(i: Int, v: Vector) = v.map(layers[i].deactivate)

	fun calculate(input: Vector): Array<Vector> {
		var last = input
		val outputs = Array<Vector?>(layers.size) { null }
		for (i in 0 until layers.size) {
			last = activate(i, fromRow(last.matrixRow() * weights[i]) + thresholds[i])
			outputs[i] = last
		}
		return outputs.requireNoNulls()
	}

	data class DataSet(
			val input: Vector,
			val output: Vector
	)

	fun trainSimple(dataSets: Collection<DataSet>, iterations: Int, learnRates: Array<Double>, errorCap: Double = 0.1): TrainDetails {
		assert(dataSets.all { it.input.dimension == inputSize })
		assert(dataSets.all { it.output.dimension == layers.last().size })
		assert(learnRates.size == layers.size)

		val convAvg = DoubleArray(iterations)
		val convMax = DoubleArray(iterations)

		for (iteration in 0 until iterations) {
			for (dataSet in dataSets) {
				// front calculate
				val outputs = calculate(dataSet.input)

				// back propagate

				// formula for ascent dE/db:
				// dE / db_last = -(dataSet.output - b_last)
				// dE / db_(i-1) = dE / db_i * db_i / db_(i-1)
				var ascent: Vector? = null // ascent is the dE/db
				for (i in outputs.lastIndex downTo 0) {
					if (ascent == null) {
						ascent = -(dataSet.output - outputs[i]) // first ascent
					} else {
						assert(ascent.dimension == layers[i + 1].size)
						ascent = fromCol(weights[i] * toCol(ascent * deactivate(i, outputs[i])))
					}
					assert(ascent.dimension == layers[i].size)
					val deactivated = deactivate(i, outputs[i])
					val prev = if (i == 0) dataSet.input else outputs[i - 1]

					// change of weight: dE/dw = dE/db * db/dw
					weights[i] -= toCol(ascent * deactivated) * toRow(prev) * learnRates[i]
					// change of threshold: dE/dt = dE/db * db/dt
					thresholds[i] -= ascent * deactivated * learnRates[i]
				}
			}

			val errorMatrix = Matrix(dataSets.size, layers.last().size) { _, _ -> 0.0 }
			for ((i, dataSet) in dataSets.withIndex()) {
				val diff = calculate(dataSet.input).last() - dataSet.output
				errorMatrix.setRow(i, diff.map(::abs))
			}

			convAvg[iteration] = errorMatrix.values.sum() / errorMatrix.m / errorMatrix.n
			convMax[iteration] = errorMatrix.values.reduce { a, b -> max(a, b) }
		}

		return TrainDetails(convAvg, convMax)
	}

	data class TrainDetails(
			val convAvg: DoubleArray,
			val convMax: DoubleArray
	)
}
