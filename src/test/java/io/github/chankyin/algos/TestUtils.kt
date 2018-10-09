package io.github.chankyin.algos

import io.kotlintest.properties.Gen
import io.kotlintest.properties.PropertyContext
import io.kotlintest.properties.assertAll
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

fun genRange(range: IntRange) = object : Gen<Int> {
	override fun constants(): Iterable<Int> = range

	override fun random() = object : Sequence<Int> {
		override fun iterator(): Iterator<Int> = range.iterator()
	}
}

val genFinite = Gen.double().filterNot {it.isNaN() || it.isInfinite()}
fun assertFinite6(fn: PropertyContext.(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double)->Unit) {
	assertAll(genFinite, genFinite, genFinite, genFinite, genFinite, genFinite, fn)
}
