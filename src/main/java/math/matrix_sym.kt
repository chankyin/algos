package math

class SymmetricMatrix(n: Int, f: (i: Int, j: Int)->number) : Matrix(n, n, f) {
	override fun set(i: Int, j: Int, value: number) {
		super.set(i, j, value)
		if(j != i) super.set(j, i, value)
	}
}
