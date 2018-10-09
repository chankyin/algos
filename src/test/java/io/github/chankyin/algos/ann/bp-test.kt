package io.github.chankyin.algos.ann

import io.github.chankyin.algos.math.c
import io.kotlintest.specs.StringSpec

class BpNetworkTest : StringSpec({
	"logic" {
		val network = BackPropagatingNetwork(2, arrayOf(NetworkLayer(5), NetworkLayer(3, purelin)))
		network.trainSimple(listOf(
				BackPropagatingNetwork.DataSet(c(0.0, 0.0), c(0.0, 0.0, 0.0)),
				BackPropagatingNetwork.DataSet(c(0.0, 1.0), c(0.0, 1.0, 1.0)),
				BackPropagatingNetwork.DataSet(c(1.0, 0.0), c(0.0, 1.0, 1.0)),
				BackPropagatingNetwork.DataSet(c(1.0, 1.0), c(1.0, 1.0, 0.0))
		), 1000, doubleArrayOf(0.3, 0.3))

		println("BpNetwork logic test:")
		println("(0, 0) -> " + network.calculate(c(0.0, 0.0)).last())
		println("(0, 1) -> " + network.calculate(c(0.0, 1.0)).last())
		println("(1, 0) -> " + network.calculate(c(1.0, 0.0)).last())
		println("(1, 1) -> " + network.calculate(c(1.0, 1.0)).last())
	}
})
