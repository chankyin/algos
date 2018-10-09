package io.github.chankyin.algos.ann

import io.github.chankyin.algos.math.SymmetricMatrix

class HopfieldNetwork(size: Int) {
	val weights = SymmetricMatrix(size) {_, _ -> Math.random()}
}
