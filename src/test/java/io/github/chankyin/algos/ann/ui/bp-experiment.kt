package io.github.chankyin.algos.ann.ui

import io.github.chankyin.algos.ann.BackPropagatingNetwork
import io.github.chankyin.algos.ann.NetworkLayer
import io.github.chankyin.algos.ann.ShuffleMethod
import io.github.chankyin.algos.ann.purelin
import io.github.chankyin.algos.makeLinePlot
import io.github.chankyin.algos.math.c
import org.jfree.chart.ChartPanel
import org.jfree.ui.RefineryUtilities
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.lang.Math.floor
import javax.swing.*
import javax.swing.event.ChangeEvent
import kotlin.math.*

/// user interface stuff

class MSlider(name: String, val start: Double, end: Double, val step: Double = 1.0, initial: Double = start) :
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

object Inputs {
	val trials = MSlider("Trials", 1.0, 10.0, 1.0, 3.0)
	val iterations = MSlider("Iterations", 10.0, 5000.0, 1.0, 100.0)
	val layerCount = MSlider("Layer count", 1.0, 20.0)
	val layers = mutableListOf<InputLayer>()
	val outputLearnRate = MSlider("Output learn rate", 0.0, 1.0, 0.01, 0.3)
	val shuffle = JComboBox(arrayOf("Iterations * Datasets", "Iterations * Shuffle(Datasets)", "Datasets * Iterations", "Shuffle(Iterations * Datasets)"))

	val layerController = LayerController()
}

val chartPanel = JPanel()
val progressIndicator = JProgressBar(0, 1000)
val frame = JFrame("experiment :: bc")

class LayerController : JPanel() {
	init {
		layout = BoxLayout(this, BoxLayout.Y_AXIS)
		add(Inputs.layerCount)
		val layer0 = InputLayer(1)
		add(layer0)
		Inputs.layers.add(layer0)
		Inputs.layerCount.addChangeListener {
			if(Inputs.layerCount.intValue == Inputs.layers.size) return@addChangeListener
			if(Inputs.layerCount.value < Inputs.layers.size) {
				// reduce layers
				for(i in Inputs.layerCount.intValue until Inputs.layers.size) {
					remove(Inputs.layers.removeAt(Inputs.layerCount.intValue))
				}
			} else {
				// increase layers
				for(i in Inputs.layers.size until Inputs.layerCount.intValue) {
					val comp = InputLayer(Inputs.layers.size + 1)
					Inputs.layers.add(comp)
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

private fun pair(c1: Component, c2: Component): JPanel {
	val ret = JPanel()
	ret.add(c1)
	ret.add(c2)
	return ret
}

fun main(args: Array<String>) {
	frame.contentPane = JPanel()
	frame.contentPane.layout = BoxLayout(frame.contentPane, BoxLayout.Y_AXIS)

	frame.contentPane.add(Inputs.trials)
	frame.contentPane.add(Inputs.iterations)
	frame.contentPane.add(Inputs.layerController)
	frame.contentPane.add(Inputs.outputLearnRate)
	frame.contentPane.add(pair(JLabel("Shuffle"), Inputs.shuffle))

	val button = JButton("Recalculate")
	val recalc: (ActionEvent?)->Unit = {
		train(
				trials = Inputs.trials.intValue,
				iterations = Inputs.iterations.intValue,
				learnRates = Inputs.layers.map {l -> l.learnRate.value / 100.0}.toDoubleArray()
						+ doubleArrayOf(Inputs.outputLearnRate.value / 100.0),
				layers = Inputs.layers.map {l -> l.nodes.intValue}.toTypedArray(),
				shuffle = ShuffleMethod.values()[Inputs.shuffle.selectedIndex]
		)
	}
	button.addActionListener(recalc)
	frame.contentPane.add(button)

	chartPanel.layout = BoxLayout(chartPanel, BoxLayout.Y_AXIS)
	frame.contentPane.add(chartPanel)

	frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
	recalc(null)
	fixFramePos(frame)
	frame.isVisible = true
}

fun fixFramePos(frame: JFrame){
	if((frame.extendedState and JFrame.MAXIMIZED_BOTH) == 0){
		frame.pack()
		RefineryUtilities.centerFrameOnScreen(frame)
	}
	frame.requestFocus()
}

/// training logic
var locked = false

private fun train(trials: Int, iterations: Int, learnRates: DoubleArray, layers: Array<Int>, shuffle: ShuffleMethod) {
	if(locked) {
		JOptionPane.showMessageDialog(frame, "Already recalculating", "Warning", JOptionPane.WARNING_MESSAGE)
		return
	}
	locked = true

	progressIndicator.isStringPainted = true
	progressIndicator.maximumSize = Dimension(300, 20)
	progressIndicator.value = 0
	chartPanel.add(progressIndicator, 0)
	frame.validate()

	val worker = object : SwingWorker<Map<String, DoubleArray>, Int>() {
		override fun doInBackground(): Map<String, DoubleArray> {
			val plotData = mutableMapOf<String, DoubleArray>()
			var lastProgress = 0
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

				plotData["#$trial Max"] = conv.convMax
				plotData["#$trial Average"] = conv.convAvg
			}
			return plotData
		}

		override fun process(chunks: MutableList<Int>) {
			progressIndicator.value = chunks.last()
		}

		override fun done() {
			locked = false
			val plotData = get()
			val plot = makeLinePlot(plotData, training.title, "Iterations", "Convergence")
			chartPanel.removeAll()
			chartPanel.add(ChartPanel(plot))
			chartPanel.invalidate()

			fixFramePos(frame)
			frame.validate()
		}
	}

	worker.execute()
}

class Training(
		val inputSize: Int,
		val dataSets: List<BackPropagatingNetwork.DataSet>,
		val title: String
)

val training = Training(
		inputSize = 1,
		dataSets = List(180) {
			val x = it - 90.0
			BackPropagatingNetwork.DataSet(c(x), c(sin(x), cos(x), tan(x)))
		},
		title = "sin/cos/tan"
)
