package io.github.chankyin.algos.ann.ui;

import javax.swing.*;
import java.util.List;

public class BpExperimentInterface{
	private JPanel mainPanel;
	private MSlider trials;
	private MSlider iterations;
	private LayerController layerController;
	private MSlider outputLearnRate;
	private JComboBox shuffle;
	private JButton recalc;
	private JPanel inputPanel;
	private JPanel testPanel;
	private JEditorPane testResults;
	private JProgressBar progressIndicator;
	private JPanel chartPanel;
	private JPanel chartParentPanel;

	public BpExperimentInterface(){
		recalc.addActionListener(e -> BpExperiment.recalculate());
	}

	private void createUIComponents(){
		trials = new MSlider("Trials", 1.0, 10.0, 1.0, 1.0);
		iterations = new MSlider("Iterations", 10.0, 5000.0, 1.0, 50.0);
		outputLearnRate = new MSlider("Output learn rate", 0.0, 1.0, 0.01, 0.3);
		progressIndicator = new JProgressBar(0, 1000);
		progressIndicator.setMaximum(1000);
		progressIndicator.setValue(0);
		progressIndicator.setStringPainted(true);
	}

	public JPanel getMainPanel(){
		return mainPanel;
	}

	public MSlider getTrials(){
		return trials;
	}

	public MSlider getIterations(){
		return iterations;
	}

	public LayerController getLayerController(){
		return layerController;
	}

	public List<InputLayer> getLayers(){
		return layerController.getLayers();
	}

	public MSlider getOutputLearnRate(){
		return outputLearnRate;
	}

	public JComboBox getShuffle(){
		return shuffle;
	}

	public JButton getRecalc(){
		return recalc;
	}

	public JPanel getInputPanel(){
		return inputPanel;
	}

	public JPanel getTestPanel(){
		return testPanel;
	}

	public JEditorPane getTestResults(){
		return testResults;
	}

	public JProgressBar getProgressIndicator(){
		return progressIndicator;
	}

	public JPanel getChartPanel(){
		return chartPanel;
	}

	public JPanel getChartParentPanel(){
		return chartParentPanel;
	}
}
