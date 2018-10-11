package io.github.chankyin.algos.math

import java.util.*
import kotlin.math.roundToInt

// values structure
// 0   m    .. jm       .. m(n-1)
// 1   m+1  .. jm+1     .. m(n-1)+1
// ..  ..   ..          .. ..
// i   m+i  .. jm+i     .. m(n-1)+i
// ..  ..   .. ..       .. ..
// m-1 2m-1 .. (j+1)m-1 .. mn-1
open class Matrix {
	val values: numberArray
	val m: Int
	val n: Int

	companion object {
		fun identity(n: Int) = Matrix(n, n) {i, j -> if(i == j) 1.0 else 0.0}

		fun make(rows: Array<numberArray>): Matrix {
			val m = rows.size
			val n = rows[0].size

			val array = numberArray(m * n)
			for(i in 0 until m) {
				assert(rows[i].size == n)
				for(j in 0 until n) {
					array[j * m + i] = rows[i][j]
				}
			}
			return Matrix(array, m, n)
		}
	}

	constructor(matrix: Matrix) {
		values = matrix.values.clone()
		m = matrix.m
		n = matrix.n
	}

	constructor(values: numberArray, m: Int, n: Int) {
		this.values = values
		this.m = m
		this.n = n
	}

	constructor(m: Int, n: Int, f: (i: Int, j: Int)->number) {
		this.m = m
		this.n = n
		values = numberArray(m * n)
		for(i in 0 until m) {
			for(j in 0 until n) {
				this[i, j] = f(i, j)
			}
		}
	}

	operator fun get(i: Int, j: Int) = values[j * m + i]
	operator fun get(@Suppress("UNUSED_PARAMETER") i: Unit?, j: Int) = this col j
	operator fun get(i: Int, @Suppress("UNUSED_PARAMETER") j: Unit?) = this row i
	operator fun get(ii: IntRange, jj: IntRange): Matrix {
		val m = (ii.last - ii.first) / ii.step + 1
		val n = (jj.last - jj.first) / jj.step + 1
		val matrix = Matrix(m, n) {_, _ -> 0.0}
		for((i1, i0) in ii.withIndex()) {
			for((j1, j0) in jj.withIndex()) {
				matrix[i1, j1] = this[i0, j0]
			}
		}
		return matrix
	}

	open operator fun set(i: Int, j: Int, value: number) = values.set(j * m + i, value)

	fun setRow(i: Int, row: Vector) {
		for(j in 0 until n) {
			this[i, j] = row[j]
		}
	}

	fun setCol(j: Int, col: Vector) {
		for(i in 0 until m) {
			this[i, j] = col[i]
		}
	}

	override fun equals(other: Any?) = other is Matrix && m == other.m && n == other.n && Arrays.equals(values, other.values)
	fun equals(that: Matrix, epsilon: Double): Boolean {
		if(m != that.m || n != that.n) return false
		for(i in 0 until m) {
			for(j in 0 until n) {
				if(this[i, j] - that[i, j] > epsilon || that[i, j] - this[i, j] > epsilon) {
					return false
				}
			}
		}
		return true
	}

	override fun hashCode() = ((7 * 31 + m) * 31 + n) * 31 + Arrays.hashCode(values)

	override fun toString(): String {
		val builder = StringBuilder("(\n")
		val strings = values.map {
			if(it.isNaN()) "NaN"
			else ((it * 1000).roundToInt() / 1000.0).toString()
		}
		val widths = IntArray(n) {j -> strings.slice(j * m until (j + 1) * m).asSequence().map {it.length + 2}.max()!!}
		for(i in 0 until m) {
			builder.append("  ")
			for(j in 0 until n) {
				builder.append(strings[j * m + i].plus(", ").padEnd(widths[j]))
			}
			builder.append('\n')
		}
		builder.append(")")
		return builder.toString()
	}

	operator fun plus(value: number) = Matrix(m, n) {i, j -> this[i, j] + value}
	operator fun minus(value: number) = Matrix(m, n) {i, j -> this[i, j] - value}
	operator fun times(value: number) = Matrix(m, n) {i, j -> this[i, j] * value}
	operator fun div(value: number) = Matrix(m, n) {i, j -> this[i, j] / value}

	operator fun plus(that: Matrix) = biMap(that) {a, b -> a + b}
	operator fun minus(that: Matrix) = biMap(that) {a, b -> a - b}

	infix fun row(i: Int) = Vector(numberArray(n) {j -> this[i, j]})
	infix fun col(j: Int) = Vector(values.sliceArray(m * j until m * (j + 1)))

	fun map(f: (Double)->Double) = Matrix(m, n) {i, j -> f(this[i, j])}

	fun transpose() = Matrix(n, m) {i, j -> this[j, i]}

	operator fun times(that: Matrix): Matrix {
		if(this.n != that.m) {
			throw IllegalArgumentException("Dimensions are incompatible: ${this.n} != ${that.m}")
		}

		return Matrix(this.m, that.n) {i, j -> (this row i) dot (that col j)}
	}

	fun trace(): number {
		var out = 0.0
		for(ij in 0 until n) {
			out += this[ij, ij]
		}
		return out
	}

	fun det(): number {
		if(m != n) throw IllegalArgumentException("Determinant is only valid on square matrices")
		if(m == 1) return values[0]
		var out = 0.0
		for(j in 0 until n) {
			out += (if(j % 2 == 0) 1 else -1) * this[0, j] * (this[1 until m, 0 until j] join this[1 until m, j + 1 until n]).det()
		}
		return out
	}


	private fun zeroesInRow(i: Int, minJ: Int = 0): Int {
		for(j in minJ until n) {
			if(this[i, j] != 0.0) return j
		}
		return n
	}

	fun toEchelon(): MutableList<MatrixRowOperation> {
		val out = mutableListOf<MatrixRowOperation>()

		// for each jth column to the right, eliminate all [j+1..m-1, j] using [j, j].
		for(j in 0 until n) {
			// if [j, j] = 0, check if we have non-zeroes below to swap with and process
			if(this[j, j] == 0.0) {
				var skip = true
				for(i in j + 1 until m) {
					if(this[i, j] != 0.0) {
						skip = false
						val op = MatrixRowOperations.swapRows(j, i)
						op(this)
						out.add(op)
						break
					}
				}
				// if all the values below are zeroes, there is no need (nor possible) to add.
				if(skip) continue
			}

			for(i in j + 1 until m) {
				if(this[i, j] == 0.0) continue // no need to add a row with factor = 0
				// want: this[i,j] + factor * this[j,j] == 0
				// factor = -this[i,j] / this[j,j]
				val factor = -this[i, j] / this[j, j]
				val op = MatrixRowOperations.addRow(j, factor, i)
				op(this)
				out.add(op)
			}
		}

		return out
	}

	fun toIdentity(): List<MatrixRowOperation> {
		val ret = toEchelon()

		// first, set the diagonal elements to 1
		for(i in 0 until m) {
			if(this[i, i] != 1.0) {
				val op = MatrixRowOperations.multiplyRow(i, 1.0 / this[i, i])
				op(this)
				ret.add(op)
			}
		}

		// then, for each row, set the upper triangle to 0 top-down
		for(j in 1 until n) {
			for(i in 0 until j) {
				if(this[i, j] != 0.0) {
					val op = MatrixRowOperations.addRow(j, -this[i, j], i)
					op(this)
					ret.add(op)
				}
			}
		}

		return ret
	}

	/**
	 * Implements a general Gauss-Jordan elimination matrix inverse algorithm on an n*n matrix
	 */
	fun inverse(): Matrix {
		if(m != n) throw IllegalArgumentException("Inverse is only valid on square matrices")

		val det = det()
		if(det == 0.0) throw IllegalArgumentException("The matrix is not invertible")

		val left = Matrix(this)
		val right = Matrix.identity(n)

		val ops = left.toIdentity()
		for(op in ops) {
			op(right)
		}

		return right
	}

	fun pseudoInverse() = when {
		m > n -> (transpose() * this).inverse() * transpose()
		m < n -> transpose() * (this * transpose()).inverse()
		else -> inverse()
	}


	infix fun above(that: Matrix): Matrix {
		if(this.n != that.n) throw IllegalArgumentException("Dimensions are incompatible")
		return Matrix(this.m + that.m, n) {i, j -> if(i < this.m) this[i, j] else that[i - this.m, j]}
	}

	infix fun join(that: Matrix): Matrix {
		if(this.m != that.m) throw IllegalArgumentException("Dimensions are incompatible")
		return Matrix(this.values + that.values, m, this.n + that.n)
	}

	private inline fun biMap(that: Matrix, mapper: (number, number)->number): Matrix {
		if(this.m != that.m || this.n != that.n) throw IllegalArgumentException("Dimensions are incompatible")
		val values = numberArray(values.size)
		for(i in 0 until values.size) {
			values[i] = mapper(this.values[i], that.values[i])
		}

		return Matrix(values, m, n)
	}
}

fun fromRow(matrix: Matrix): Vector {
	if(matrix.m != 1) throw IllegalArgumentException("Expected matrix.m = 1, got ${matrix.m}")
	return matrix row 0
}

fun fromCol(matrix: Matrix): Vector {
	if(matrix.n != 1) throw IllegalArgumentException("Expected matrix.n = 1, got ${matrix.n}")
	return matrix col 0
}

fun toRow(v: Vector) = v.matrixRow()
fun toCol(v: Vector) = v.matrixCol()
fun toDiagonal(v: Vector) = v.matrixDiagonal()

val (IntRange).size: Int get() = (this.last - this.first) / this.step + 1

typealias MatrixRowOperation = (matrix: Matrix)->Unit

object MatrixRowOperations {
	fun swapRows(i1: Int, i2: Int): MatrixRowOperation = {
		for(j in 0 until it.n) {
			val tmp = it[i1, j]
			it[i1, j] = it[i2, j]
			it[i2, j] = tmp
		}
	}

	fun addRow(from: Int, factor: number, to: Int): MatrixRowOperation = {
		for(j in 0 until it.n) {
			it[to, j] += it[from, j] * factor
		}
	}

	fun multiplyRow(i: Int, factor: number): MatrixRowOperation = {
		for(j in 0 until it.n) {
			it[i, j] *= factor
		}
	}
}
