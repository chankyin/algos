package ann

import kotlin.math.exp

fun sigmoid(x: Double) = 1 / (1 + exp(-x))

// x is the value returned from sigmoid(), and this function returns the derivative
fun sigmoidDev(x: Double) = x * (1 - x)
