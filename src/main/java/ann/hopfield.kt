package ann

import math.SymmetricMatrix

class HopfieldNetwork(size: Int) {
	val weights = SymmetricMatrix(size) {_, _ -> Math.random()}
}
