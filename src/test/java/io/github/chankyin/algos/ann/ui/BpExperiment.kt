@file:JvmName("BpExperiment")

package io.github.chankyin.algos.ann.ui

import io.github.chankyin.algos.ann.BackPropagatingNetwork
import io.github.chankyin.algos.ann.NetworkLayer
import io.github.chankyin.algos.ann.ShuffleMethod
import io.github.chankyin.algos.ann.purelin
import io.github.chankyin.algos.makeLinePlot
import io.github.chankyin.algos.math.c
import org.jfree.chart.ChartPanel
import org.jfree.ui.RefineryUtilities
import java.awt.FlowLayout
import java.lang.Math.floor
import javax.swing.*
import javax.swing.event.ChangeEvent
import kotlin.math.*

/// user interface stuff

class MSlider
@JvmOverloads constructor(name: String, val start: Double, end: Double, val step: Double = 1.0, initial: Double = start) :
		JPanel(FlowLayout()) {
	val steps = floor((end - start) / step).toInt()
	val slider = JSlider(0, steps, 0)
	val valueLabel: JLabel = JLabel(start.toString())

	var value: Double
		get() {
			return start + step * slider.value
		}
		set(value) {
			slider.value = round((value - start) / step).toInt()
			showValue(value)
		}

	init {
		add(JLabel(name))
		add(slider)
		add(valueLabel)

		value = initial

		slider.addChangeListener {showValue(value)}
	}

	private fun showValue(value: Double) {
		valueLabel.text = (round(value * 1000) / 1000.0).toString()
	}

	val intValue get() = value.toInt()

	fun addChangeListener(l: (ChangeEvent)->Unit) = slider.addChangeListener(l)
}

val frame = JFrame("experiment :: bc")
val itf = BpExperimentInterface()

class LayerController : JPanel() {
	val layerCount = MSlider("Number of layers", 1.0, 30.0)
	val layers = mutableListOf<InputLayer>()

	init {
		layout = BoxLayout(this, BoxLayout.Y_AXIS)
		add(layerCount)
		val layer0 = InputLayer(1)
		add(layer0)
		layers.add(layer0)
		layerCount.addChangeListener {
			if(layerCount.intValue == layers.size) return@addChangeListener
			if(layerCount.value < layers.size) {
				// reduce layers
				for(i in layerCount.intValue until layers.size) {
					remove(layers.removeAt(layerCount.intValue))
				}
			} else {
				// increase layers
				for(i in layers.size until layerCount.intValue) {
					val comp = InputLayer(layers.size + 1)
					layers.add(comp)
					add(comp)
				}
			}
			fixFramePos(frame)
		}
	}
}

class InputLayer(i: Int) : JPanel() {
	val learnRate = MSlider("Learn rate", 0.0, 1.0, 0.01, 0.3)
	val nodes = MSlider("Nodes in layer", 1.0, 20.0, 1.0, 5.0)

	init {
		layout = FlowLayout()
		add(JLabel("Layer #$i"))
		add(learnRate)
		add(nodes)
	}
}

fun recalculate() = train(
		trials = itf.trials.intValue,
		iterations = itf.iterations.intValue,
		learnRates = itf.layers.map {l -> l.learnRate.value / 100.0}.toDoubleArray()
				+ doubleArrayOf(itf.outputLearnRate.value / 100.0),
		layers = itf.layers.map {l -> l.nodes.intValue}.toTypedArray(),
		shuffle = ShuffleMethod.values()[itf.shuffle.selectedIndex]
)


fun main(args: Array<String>) {
	frame.contentPane = itf.mainPanel
	frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
	recalculate()
	fixFramePos(frame)
	frame.isVisible = true
}

fun fixFramePos(frame: JFrame) {
	if((frame.extendedState and JFrame.MAXIMIZED_BOTH) == 0) {
		frame.pack()
		RefineryUtilities.centerFrameOnScreen(frame)
	}
	frame.requestFocus()
}

/// training logic
var locked = false
var firstTime = true

private fun train(trials: Int, iterations: Int, learnRates: DoubleArray, layers: Array<Int>, shuffle: ShuffleMethod) {
	if(locked) {
		JOptionPane.showMessageDialog(frame, "Already recalculating", "Warning", JOptionPane.WARNING_MESSAGE)
		return
	}
	locked = true

	itf.progressIndicator.value = 0

	val worker = object : SwingWorker<NetworkResult, Int>() {
		override fun doInBackground(): NetworkResult {
			val plotData = mutableMapOf<String, DoubleArray>()
			var lastProgress = 0

			val tests = StringBuilder()

			for(trial in 1..trials) {
				val nl = Array(layers.size + 1) {
					if(it == layers.size) NetworkLayer(3, purelin)
					else NetworkLayer(layers[it])
				}
				val network = BackPropagatingNetwork(training.inputSize, nl)
				val conv = network.trainSimple(
						dataSets = training.dataSets,
						iterations = iterations,
						learnRates = learnRates,
						shuffle = shuffle,
						publishProgress = {
							val newProgress = ((trial - 1 + it) / trials * 1000).roundToInt()
							if(newProgress != lastProgress) {
								lastProgress = newProgress
								publish(newProgress)
							}
						}
				)
				plotData["Trial #$trial: max"] = conv.convMax
				plotData["Trial #$trial: average"] = conv.convAvg

				tests.appendln("Trial #$trial")
				for(set in training.dataSets) {
					tests.append(set.input.toString(3)).append(" = ").append(set.output.toString(3)).append(" -> ")
							.appendln(network.calculate(set.input).last().toString(3))
				}
				tests.appendln()
			}

			return NetworkResult(plotData, tests.toString())
		}

		override fun process(chunks: MutableList<Int>) {
			itf.progressIndicator.value = chunks.last()
			itf.progressIndicator.invalidate()
			frame.validate()
		}

		override fun done() {
			locked = false
			val plotData = get()

			val plot = makeLinePlot(plotData.plot, training.title, "Iterations", "Convergence")
			itf.chartPanel.removeAll()
			itf.chartPanel.add(ChartPanel(plot))
			itf.chartPanel.validate()

			itf.testResults.text = plotData.tests

			if(firstTime) fixFramePos(frame)
			firstTime = false
		}
	}

	worker.execute()
}

class NetworkResult(
		val plot: Map<String, DoubleArray>,
		val tests: String
)

class Training(
		val inputSize: Int,
		val dataSets: List<BackPropagatingNetwork.DataSet>,
		val title: String
)

val training = Training(
		inputSize = 2,
		dataSets = listOf(
				BackPropagatingNetwork.DataSet(c(0.0, 0.0), c(0.0, 0.0, 0.0)),
				BackPropagatingNetwork.DataSet(c(0.0, 1.0), c(0.0, 1.0, 1.0)),
				BackPropagatingNetwork.DataSet(c(1.0, 0.0), c(0.0, 1.0, 1.0)),
				BackPropagatingNetwork.DataSet(c(1.0, 1.0), c(1.0, 1.0, 0.0))
		),
		title = "and/or/xor"
)
//val training = Training(
//		inputSize = 1,
//		dataSets = List(37) {
//			val x = (it / 36.0 - 0.5) * PI
//			val set = BackPropagatingNetwork.DataSet(c(x / PI * 180.0), c(sin(x), cos(x), tan(x)))
//			return@List set
//		},
//		title = "sin/cos/tan"
//)
