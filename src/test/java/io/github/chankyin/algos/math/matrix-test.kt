package io.github.chankyin.algos.math

import io.github.chankyin.algos.assertFinite6
import io.github.chankyin.algos.genFinite
import io.github.chankyin.algos.genRange
import io.kotlintest.matchers.exactly
import io.kotlintest.matchers.plusOrMinus
import io.kotlintest.properties.assertAll
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class MatrixTest : StringSpec({
	"equals" {
		Matrix(doubleArrayOf(1.0, 2.0, 3.0, 4.0), 2, 2) shouldTend Matrix(doubleArrayOf(1.0, 2.0, 3.0, 4.0), 2, 2)
	}

	"identity" {
		assertAll(genRange(1..3)) {n ->
			val identity = Matrix.identity(n)
			identity.m shouldBe n
			identity.n shouldBe n
			for(i in 0 until n) {
				for(j in 0 until n) {
					identity[i, j] shouldBe exactly(if(i == j) 1.0 else 0.0)
				}
			}
		}
	}

	"add" {
		assertFinite6 {a: Double, b: Double, c: Double, d: Double, e: Double, f: Double ->
			Matrix.make(arrayOf(
					doubleArrayOf(1.0, 2.0, 5.0),
					doubleArrayOf(3.0, 4.0, 6.0))) + Matrix.make(arrayOf(
					doubleArrayOf(a, b, c),
					doubleArrayOf(d, e, f))) shouldTend Matrix.make(arrayOf(
					doubleArrayOf(1.0 + a, 2.0 + b, 5.0 + c),
					doubleArrayOf(3.0 + d, 4.0 + e, 6.0 + f)))
		}
	}

	"subtract" {
		assertFinite6 {a: Double, b: Double, c: Double, d: Double, e: Double, f: Double ->
			Matrix.make(arrayOf(
					doubleArrayOf(1.0, 2.0, 5.0),
					doubleArrayOf(3.0, 4.0, 6.0))) - Matrix.make(arrayOf(
					doubleArrayOf(a, b, c),
					doubleArrayOf(d, e, f))) shouldTend Matrix.make(arrayOf(
					doubleArrayOf(1.0 - a, 2.0 - b, 5.0 - c),
					doubleArrayOf(3.0 - d, 4.0 - e, 6.0 - f)))
		}
	}

	"multiply size" {
		assertAll(genRange(1..10), genRange(1..10), genRange(1..10)) {m1: Int, m2: Int, m3: Int ->
			val m = Matrix(DoubleArray(m1 * m2), m1, m2) * Matrix(DoubleArray(m2 * m3), m2, m3)
			m.m shouldBe m1
			m.n shouldBe m3
		}
	}

	"multiply identity" {
		assertFinite6 {a: Double, b: Double, c: Double, d: Double, e: Double, f: Double ->
			val m = Matrix.make(arrayOf(doubleArrayOf(a, b, c), doubleArrayOf(d, e, f)))
			m * Matrix.identity(3) shouldTend m
		}
	}
	"identity multiply" {
		assertFinite6 {a: Double, b: Double, c: Double, d: Double, e: Double, f: Double ->
			val m = Matrix.make(arrayOf(doubleArrayOf(a, b, c), doubleArrayOf(d, e, f)))
			Matrix.identity(2) * m shouldTend m
		}
	}

	"transpose" {
		assertFinite6 {a: Double, b: Double, c: Double, d: Double, e: Double, f: Double ->
			val m = Matrix.make(arrayOf(doubleArrayOf(a, b, c), doubleArrayOf(d, e, f)))
			m.transpose() shouldTend Matrix.make(arrayOf(doubleArrayOf(a, d), doubleArrayOf(b, e), doubleArrayOf(c, f)))
		}
	}
})

class MatrixInverseTest : StringSpec({
	"invert identity" {
		assertAll(genRange(1..10)) {
			val identity = Matrix.identity(it)
			identity.inverse() shouldTend identity
		}
	}
	"invert random" {
		val d = genFinite.random().iterator()
		assertAll(genRange(1..5)) {n: Int ->
			val identity = Matrix.identity(n)
			for(iteration in 1..100) {
				val matrix = Matrix(n, n) {_, _ -> d.next()}
				if(matrix.det() != 0.0) {
					val inverse = matrix.inverse()
					matrix * inverse shouldTend identity
					inverse * matrix shouldTend identity
				}
			}
		}
	}
})

infix fun (Matrix).shouldTend(that: Matrix) {
	this.m shouldBe that.m
	this.n shouldBe that.n

	for(i in 0 until m) {
		for(j in 0 until n) {
			this[i, j] shouldBe (that[i, j] plusOrMinus 1.0e-10)
		}
	}
}
