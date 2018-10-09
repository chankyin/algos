package io.github.chankyin.algos

import java.util.*
import java.util.concurrent.ThreadLocalRandom

fun <T> shuffleArray(array: Array<T>, random: Random = ThreadLocalRandom.current()){
	for(i in array.lastIndex downTo 0){
		val j = random.nextInt(i+1)
		val tmp = array[j]
		array[j] = array[i]
		array[i] = tmp
	}
}
