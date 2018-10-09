package io.github.chankyin.algos.ann

import io.github.chankyin.algos.math.*
import kotlin.math.abs
import kotlin.math.max

// generic variable names:
// layer number = i
// node number within layer = j

data class NetworkLayer(
		val size: Int,
		val activator: Activator = sigmoid
)

enum class ShuffleMethod {
	IT_DS,
	INNER,
	DS_IT,
	OUTER
}

class BackPropagatingNetwork(
		val inputSize: Int,
		val layers: Array<NetworkLayer>
) {
	val thresholds: Array<Vector> = Array(layers.size) {
		Vector(layers[it].size) {_ -> Math.random() - 0.5}
	}

	val weights: Array<Matrix> = Array(layers.size) {i ->
		Matrix(if(i == 0) inputSize else layers[i - 1].size, layers[i].size) {_, _ -> Math.random() - 0.5}
	}

	private fun activate(i: Int, v: Vector) = v.map(layers[i].activator::act)
	private fun actDev(i: Int, v: Vector) = v.map(layers[i].activator::dev)

	fun calculate(input: Vector): Array<Vector> {
		if(input.dimension != inputSize) {
			throw IllegalArgumentException("Input vector is not of correct dimension: expected $inputSize, got ${input.dimension}")
		}

		var last = input
		val outputs = Array<Vector?>(layers.size) {null}
		for(i in 0 until layers.size) {
			last = activate(i, fromRow(toRow(last) * weights[i]) + thresholds[i])
			outputs[i] = last
		}
		return outputs.requireNoNulls()
	}

	data class DataSet(
			val input: Vector,
			val output: Vector
	)

	fun trainSimple(dataSets: List<DataSet>, iterations: Int, learnRates: DoubleArray, shuffle: ShuffleMethod = ShuffleMethod.IT_DS, errorCap: Double? = 0.1, publishProgress: ((Double)->Unit)? = null): TrainDetails {
		assert(dataSets.all {it.input.dimension == inputSize})
		assert(dataSets.all {it.output.dimension == layers.last().size})
		assert(learnRates.size == layers.size)

		val convAvg = DoubleArray(iterations * dataSets.size)
		val convMax = DoubleArray(iterations * dataSets.size)

		var count = 0
		val total = iterations * dataSets.size
		when(shuffle) {
			ShuffleMethod.IT_DS -> {
				outer@ for(iteration in 0 until iterations) {
					for(dataSet in dataSets) {
						val enough = trainDataSetOnce(dataSet, learnRates, dataSets, convAvg, count++, convMax, errorCap)
						publishProgress?.invoke((count).toDouble() / total)
						if(enough) break@outer
					}
				}
			}
			ShuffleMethod.INNER -> {
				val copy = MutableList(dataSets.size) {dataSets[it]}
				outer@ for(iteration in 0 until iterations) {
					copy.shuffle()
					for(dataSet in copy) {
						val enough = trainDataSetOnce(dataSet, learnRates, dataSets, convAvg, count++, convMax, errorCap)
						publishProgress?.invoke((count).toDouble() / total)
						if(enough) break@outer
					}
				}
			}
			ShuffleMethod.DS_IT -> {
				outer@ for(dataSet in dataSets) {
					for(iteration in 0 until iterations) {
						val enough = trainDataSetOnce(dataSet, learnRates, dataSets, convAvg, count++, convMax, errorCap)
						publishProgress?.invoke((count).toDouble() / total)
						if(enough) break@outer
					}
				}
			}
			ShuffleMethod.OUTER -> {
				TODO("Implement completely random iteration without O(mn) memory")
			}
		}

		return TrainDetails(convAvg, convMax)
	}

	private fun trainDataSetOnce(dataSet: DataSet, learnRates: DoubleArray, dataSets: List<DataSet>, convAvg: DoubleArray, iteration: Int, convMax: DoubleArray, errorCap: Double?): Boolean {
		// front calculate
		val outputs = calculate(dataSet.input)

		// back propagate

		// formula for ascent dE/db:
		// dE / db_last = -(dataSet.output - b_last)
		// dE / db_(i-1) = dE / db_i * db_i / db_(i-1) = dE / db_i * w_i * output_i
		var ascent: Vector? = null // ascent is the dE/db
		for(i in outputs.lastIndex downTo 0) {
			if(ascent == null) {
				ascent = -(dataSet.output - outputs[i]) // first ascent
			} else {
				assert(ascent.dimension == layers[i + 1].size)
				ascent = fromCol(weights[i + 1] * toCol(ascent * actDev(i + 1, outputs[i + 1])))
			}
			assert(ascent.dimension == layers[i].size)
			val deactivated = actDev(i, outputs[i])
			val prev = if(i == 0) dataSet.input else outputs[i - 1]

			// change of weight: dE/dw = dE/db * db/dw
			val dw = toCol(prev) * toRow(ascent * deactivated) * learnRates[i]
			weights[i] -= dw
			// change of threshold: dE/dt = dE/db * db/dt
			thresholds[i] -= ascent * deactivated * learnRates[i]
		}

		if(errorCap == null) return false

		val errorMatrix = Matrix(dataSets.size, layers.last().size) {_, _ -> 0.0}
		for((i, ds) in dataSets.withIndex()) {
			val diff = calculate(ds.input).last() - ds.output
			errorMatrix.setRow(i, diff.map(::abs))
		}

		convAvg[iteration] = errorMatrix.values.sum() / errorMatrix.m / errorMatrix.n
		convMax[iteration] = errorMatrix.values.reduce {a, b -> max(a, b)}

		return convMax[iteration] < errorCap
	}

	data class TrainDetails(
			val convAvg: DoubleArray,
			val convMax: DoubleArray
	)
}
