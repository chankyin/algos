package io.github.chankyin.algos

import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import org.jfree.ui.ApplicationFrame
import org.jfree.ui.RefineryUtilities

fun showLinePlot(title: String, xName: String, yName: String, dataSets: Map<String, DoubleArray>): ApplicationFrame {
	val frame = ApplicationFrame(title)
	val chart = makeLinePlot(dataSets, title, xName, yName)
	frame.contentPane = ChartPanel(chart)

	frame.pack()
	RefineryUtilities.centerFrameOnScreen(frame)
	return frame
}

fun makeLinePlot(dataSets: Map<String, DoubleArray>, title: String, xName: String, yName: String): JFreeChart? {
	val set = XYSeriesCollection()
	for((label, data) in dataSets) {
		val series = XYSeries(label)
		for(x in 0 until data.size) {
			series.add(x, data[x])
		}
		set.addSeries(series)
	}
	return ChartFactory.createXYLineChart(
			title,
			xName,
			yName,
			set,
			PlotOrientation.VERTICAL,
			true,
			true,
			false
	)
}
