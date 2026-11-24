/* Copyright (C) 2011-2012 Ruhan Ikeda
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 */

/* 
 * Jan-Feb 2016
 * 
 * Rewriting of this rubette includes:
 * 
 * 1. Tension values are set in HarmonicAnalysisModel rubette (HAM); not in this rubette anymore. 
 * Tension values are passed through denotators from HAM. (DONE)
 *  
 * 2. Implementation of Viterbi algorithm for path calculation (DONE)
 * 
 * 
 * July-Aug 2018
 * 
 * Continuation of this work
 * 3rd Goal: Evaluation of harmonic weights (DONE)
 * 
 * 
 * June 2020
 * 
 * Addition of metric proximity
 * 
 * 2021: Addition of window paths (being able to see
 * all chord evaluations in the window=cd+fd+1)
 * 
 */

package org.rubato.rubettes.score;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.rubato.base.AbstractRubette;
import org.rubato.base.Repository;
import org.rubato.base.Rubette;
import org.rubato.composer.RunInfo;
import org.rubato.composer.icons.Icons;
import org.rubato.logeo.FormFactory;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.FactorDenotator;
import org.rubato.math.yoneda.Form;
import org.rubato.math.yoneda.LimitDenotator;
import org.rubato.math.yoneda.LimitForm;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.math.yoneda.PowerForm;
import org.rubato.math.yoneda.SimpleDenotator;
import org.rubato.math.yoneda.SimpleForm;
import org.rubato.rubettes.util.Chain;
import org.rubato.rubettes.util.Chord;
import org.rubato.rubettes.util.GMINote;
import org.rubato.rubettes.util.LocalMeter;
import org.rubato.rubettes.util.Weight;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;

import static org.rubato.logeo.DenoFactory.makeDenotator;
import static org.rubato.xml.XMLConstants.FALSE_VALUE;
import static org.rubato.xml.XMLConstants.TRUE_VALUE;
import static org.rubato.xml.XMLConstants.VALUE_ATTR;

import com.sun.glass.events.KeyEvent;


public final class HarmonicPathRubette extends AbstractRubette

	implements ListSelectionListener, ActionListener {
	
	private static final ImageIcon icon = Icons.loadIcon(HarmonicPathRubette.class, "harmonicpath.png");
	static Repository rep = Repository.systemRepository();
	
	// Output Denotator (output #1)	
	public Form noteForm = rep.getForm("Note");
	public Form chordForm = FormFactory.makePowerForm("Chord", noteForm);
	private static Form scoreForm	= rep.getForm("Score");
	Form x = rep.registerBuiltin(chordForm);
	public PowerForm chordSequenceForm = FormFactory.makePowerForm("ChordSequence", chordForm);
    Form y2 = rep.registerBuiltin(chordSequenceForm);
	private SimpleForm riemannValueForm = FormFactory.makeRModuleForm("RiemannValue");
	private SimpleForm riemannRowIndexForm = FormFactory.makeZModuleForm("RiemannRowIndex");
	private SimpleForm riemannColIndexForm = FormFactory.makeZModuleForm("RiemannColIndex");	
	private List<Form> riemannValueList = new LinkedList<Form>();
	Boolean y123 = riemannValueList.add(riemannRowIndexForm);
    Boolean y125 = riemannValueList.add(riemannColIndexForm);
    Boolean y127 = riemannValueList.add(riemannValueForm);
    private LimitForm riemannValuePairForm = FormFactory.makeLimitForm("RiemannValuePair", riemannValueList);
	private PowerForm riemannMatrixForm = FormFactory.makePowerForm("RiemannMatrix", riemannValuePairForm);  	

	// Riemann Matrix Sequence
	private static Form methodForm = FormFactory.makeZModuleForm("Method");
	private List<Form> riemannMatrixList = new LinkedList<Form>();
	Boolean x123 = riemannMatrixList.add(chordForm);
    Boolean x125 = riemannMatrixList.add(riemannMatrixForm);	
	private LimitForm riemannMatrixWithChordForm = FormFactory.makeLimitForm("RiemannMatrixWithChord", riemannMatrixList);    
	private PowerForm riemannMatrixSequenceForm = FormFactory.makePowerForm("RiemannMatrixSequence", riemannMatrixWithChordForm);   
	private List<Form> riemannMatrixListWithChord = new LinkedList<Form>();
	Boolean x121 = riemannMatrixListWithChord.add(methodForm);
	Boolean x127 = riemannMatrixListWithChord.add(riemannMatrixSequenceForm);
	private LimitForm riemannMatrixSequenceWithMethodForm = FormFactory.makeLimitForm("RiemannMatrixSeqWithMethod", riemannMatrixListWithChord);  
	
	// Forms and denotators for HarmonicAnalysisModelRubette:

	// tonality takes values in Z_12
	static SimpleForm tonalityForm = FormFactory.makeZModuleForm("Tonality");
	static PowerForm tonalitiesForm = FormFactory.makePowerForm("Tonalities", tonalityForm);
	Form y = rep.registerBuiltin(tonalitiesForm);

	// modes takes values in Z_7
	// 0 is major (Ionian), 5 is minor (Aeolian), ...
	private SimpleForm modeForm = FormFactory.makeZModuleForm("Mode");
	PowerForm modesForm = FormFactory.makePowerForm("Modes", modeForm);
	Form z = rep.registerBuiltin(modesForm);

	// function takes values in Z_7
	// 0 is tonic, 1 is subtonic, 2 is median, 3 is subdominant, 4 is dominant,
	private SimpleForm functionForm = FormFactory.makeZModuleForm("Function");
	PowerForm functionsForm = FormFactory.makePowerForm("Functions", functionForm);
	Form w = rep.registerBuiltin(functionsForm);

	// Weight-of-thirds table values
	// Note that size of the table can be retrieved from other denotators
	// (tonality, mode, function)
	private SimpleForm sizeForm = FormFactory.makeZModuleForm("Size");  // to store row and column sizes of a table
	
	List<Form> weightList = new LinkedList<Form>();
	private SimpleForm weightForm = FormFactory.makeRModuleForm("Weight");
	private SimpleForm rowForm = FormFactory.makeZModuleForm("Row");
	private SimpleForm colForm = FormFactory.makeZModuleForm("Column");
	Boolean w1 = weightList.add(rowForm);
	Boolean w2 = weightList.add(colForm);
	Boolean w3 = weightList.add(weightForm);
	LimitForm weightOfThirdForm = FormFactory.makeLimitForm("WeightOfThird", weightList);
	PowerForm weightOfThirdsForm = FormFactory.makePowerForm("WeightOfThirds", weightOfThirdForm);
	Form u = rep.registerBuiltin(weightOfThirdsForm);
	
	// to add tension tables to the output denotator
	
	// tension form
	List<Form> tensionTableEntry = new LinkedList<Form>();	
    private SimpleForm tensVal = FormFactory.makeRModuleForm("TensionVal");
    Form ff = rep.registerBuiltin(tensVal);
	Boolean t1 = tensionTableEntry.add(rowForm);
	Boolean t2 = tensionTableEntry.add(colForm);
	Boolean t3 = tensionTableEntry.add(tensVal);
	LimitForm tensionTableCell = FormFactory.makeLimitForm("Tension", tensionTableEntry);
	PowerForm tensionTable = FormFactory.makePowerForm("Tensions", tensionTableCell);
	Form tt = rep.registerBuiltin(tensionTable);	

	List<Form> tableSize = new LinkedList<Form>();
	Boolean b1 = tableSize.add(sizeForm);
	Boolean b2 = tableSize.add(sizeForm);
	Boolean b3 = tableSize.add(sizeForm);
	Boolean b4 = tableSize.add(tensionTable);
	LimitForm tensionSizeAndTable = FormFactory.makeLimitForm("TableSizeAndTensionsTable", tableSize);
		
	// tonality & function inclusion table
	List<Form> inclusionTableEntry = new LinkedList<Form>();	
	private SimpleForm inclusionVal = FormFactory.makeZModuleForm("Inclusion");
	Form inc = rep.registerBuiltin(inclusionVal);
	Boolean b12 = inclusionTableEntry.add(rowForm);
	Boolean b13 = inclusionTableEntry.add(colForm);
	Boolean b14 = inclusionTableEntry.add(inclusionVal);
	LimitForm inclusionTableCell = FormFactory.makeLimitForm("InclusionCell", inclusionTableEntry);	
	PowerForm inclusionTableForm = FormFactory.makePowerForm("InclusionTableForm", inclusionTableCell);
	Form InclusionTable = rep.registerBuiltin(inclusionTableForm);
	
	List<Form> incTableSize = new LinkedList<Form>();
	Boolean b21 = incTableSize.add(sizeForm);
	Boolean b22 = incTableSize.add(sizeForm);
	Boolean b23 = incTableSize.add(sizeForm);
	Boolean b24 = incTableSize.add(InclusionTable);
	LimitForm incSizeAndTable = FormFactory.makeLimitForm("IncTableSizeAndTable", incTableSize);
	
	List<Form> analysisList = new LinkedList<Form>();
	Boolean x1 = analysisList.add(tonalitiesForm);
	Boolean x2 = analysisList.add(modesForm);
	Boolean x3 = analysisList.add(functionsForm);
	Boolean x4 = analysisList.add(weightOfThirdsForm);
	Boolean t5 = analysisList.add(tensionSizeAndTable); 			// tonal tensions
	Boolean t6 = analysisList.add(tensionSizeAndTable); 			// mode tensions
	Boolean t7 = analysisList.add(tensionSizeAndTable); 			// function tensions
	Boolean t8 = analysisList.add(incSizeAndTable);
	
	LimitForm analysisSpecForm = FormFactory.makeLimitForm("AnalysisSpec", analysisList);
	Form zz = rep.registerBuiltin(analysisSpecForm);
	
	Form durationForm = rep.getForm("Duration");
	private SimpleForm onsetForm = FormFactory.makeRModuleForm("Onset");	  
    private List<Form> chordAnalysis = new LinkedList<Form>();
    Boolean x225 = chordAnalysis.add(chordForm);
    Boolean x221 = chordAnalysis.add(onsetForm);
    Boolean x223 = chordAnalysis.add(durationForm);
    Boolean x227 = chordAnalysis.add(rowForm); // Riemann Matrix row
    Boolean x229 = chordAnalysis.add(colForm); // Riemann Matrix column
    LimitForm chordAnalysisForm = FormFactory.makeLimitForm("ChordAnalysis", chordAnalysis);
    Form x15 = rep.registerBuiltin(chordAnalysisForm);    
    PowerForm chordAnalysisSequenceForm = FormFactory.makePowerForm("ChordAnalysisSequence", chordAnalysisForm);
    Form x16 = rep.registerBuiltin(chordAnalysisSequenceForm);
    
    // Weight denotator	 
	Form weightValForm = FormFactory.makeRModuleForm("Value");
	Form pitchForm    = rep.getForm("Pitch");
	Form loudnessForm = rep.getForm("Loudness");
	Form voiceForm    = rep.getForm("Voice");	
	List<Form> WeightPair	= new LinkedList<Form>();	
	Boolean w10 = WeightPair.add(onsetForm);
	Boolean w12 = WeightPair.add(pitchForm);
	Boolean w14 = WeightPair.add(loudnessForm);
	Boolean w4 = WeightPair.add(durationForm);
	Boolean w5 = WeightPair.add(voiceForm);
	Boolean w0 = WeightPair.add(weightValForm);
	LimitForm NoteAndWeightForm = FormFactory.makeLimitForm("NoteAndWeight", WeightPair);
	Form w6 = rep.registerBuiltin(NoteAndWeightForm);
	PowerForm WeightSeqForm = FormFactory.makePowerForm("WeightSeq", NoteAndWeightForm);
	Form w7 = rep.registerBuiltin(WeightSeqForm);	
	
    protected Denotator HarmonicWeightSequence = null;
	private JPanel   properties = null;
	
	private String bpc[] = {"Viterbi", "Best Local Germs" };
	private JComboBox<String> BPCSelector = null;
	private int VITERBI = 0;
	private int BEST_LOCAL_GERMS = 1;
	private int pathMethod = VITERBI;	
	private int chordMethod = 0;	
	private int chain3rd[][];
	private int CHAIN_OF_THIRDS = 1;
	private int DIRECT = 2;
			
	// The following values will be coming via the denotator
	// preferences for CD and FD
	private JPanel HEPanel = null;
	private JLabel CDlabel, FDlabel;
	private int CD = 1;
	private int FD = 2;
	private JTextField causalDepthField, finalDepthField;		
	private int causalDepth=CD, finalDepth=FD;
	
	//// preferences for global and local thresholds 
	private JTextField globalThresholdField, localThresholdField;
	private int globalThreshold=8, localThreshold=8;
	private JTextField MetricPropertiesField, QuantizationField;			
	private JTextField distanceFactorField;	
	private JTextField GPWField, GOField, GMField;
	private double distanceFactor=0.5;
	private int gridPenWidth=1;
	private int weightCircleDiameter=12;
	private double gridOrigin=0.0, gridMesh=1.0;
	private JPanel DPanel, G1Panel, G2Panel, G3Panel;
	private JLabel dfLabel, GPWLabel, GOLabel, GMLabel;
	private JLabel MetricPropertieslabel, Quantizationlabel;
	
 	private ArrayList<AbstractRiemannMatrix> matrices = null;
 	private ArrayList<GMINote> notes = null;
	private ArrayList<RMPoint> bestPath = null;
	protected Denotator HarmonicAnalysisSequence = null;
	private JPanel weightPanel = null;
	private JPanel MetricPanel = null;
	
	// View dialog for analysis results table
	private JPanel view = null;
	private JHarmonicAnalysisDisplay pathView = null;
	private JHarmonicWeight weightView = null;
	private JTabbedPane tPane = null;
	
	// these will be filled in from the AnalysisSpecDenotator
	private int[] tons = null;
	private int[] modes = null;
	private int[] funs = null;
	private double[][] tonTension = null;
	private double[][] modeTension = null;
	private double[][] funTension = null;
	private boolean[][] inclusion = null;
	private int rowSize;
	private int colSize;	
	double[] chordWeights;
	private ArrayList<Weight> noteWeights = null; // harmonic weights of notes	
    private Viterbi vit;
    private BestLocalGerms blg;
	private ArrayList<ArrayList<RMPoint> > windowPaths = null;		
	private LinkedList<Chain> chainRepository = null;
	private double[] weights;   // weight-of-thirds table 
	
	private String methods[] = {"Local-Mazzola", "Global"};
	private String selectedMethod = methods[0];
	private JComboBox<String> calculationMethodSelector;	
	private int 			minLocalMeterLength = 4;  
    private double 			quantization = 0.25;
    private boolean         useMeters = false;
    private JCheckBox       useMetersBox;
    private boolean         calculateWeights = false;
    private JCheckBox       calcWeightsBox;
	private boolean 		tableOn = false;	   
	
	// for debugging
	private boolean DEBUG = true;

	// constructor
	public HarmonicPathRubette() {  		
		chainRepository = new LinkedList<Chain>();
		Chain.allThirdChains(chainRepository);
		// 1st input is riemann matrices,
		// 2nd input is harmonic analysis model denotator
		// 3rd input is from MidiFileIn (score denotator)
		setInCount(3);		
		// first output is harmonic path
		// 2nd output is harmonic weights
		setOutCount(2);		
	}
		
	public JComponent getProperties() {
		
		if (properties == null) {			
			properties = new JPanel();			
			JTabbedPane ptPane = new JTabbedPane();
			
			// Best Path Calculation Panel
            JPanel panel1 = new JPanel();
            panel1.setLayout(new BoxLayout(panel1, BoxLayout.Y_AXIS));
           
			JPanel BPCPanel = new JPanel();
			BPCPanel.setLayout(new BoxLayout(BPCPanel, BoxLayout.X_AXIS));
			JLabel label = new JLabel("Best Path Calculation Method: ");
			BPCPanel.add(label);					
			BPCSelector = new JComboBox<String>();
			BPCSelector.setEditable(false);
			BPCSelector.addActionListener(this);
			for (int i = 0; i < bpc.length; i++) {
				BPCSelector.addItem(bpc[i]);
			}
			BPCPanel.add(BPCSelector);	
			BPCSelector.setSelectedIndex(pathMethod);
			panel1.add(BPCPanel);
			
			// Harmonic Extend
			HEPanel = new JPanel();
			HEPanel.setLayout(new BoxLayout(HEPanel, BoxLayout.Y_AXIS));	
			HEPanel.setForeground(new Color(250,250,250));
			JPanel CDproperties = new JPanel();		
			CDproperties.setLayout(new BoxLayout(CDproperties, BoxLayout.X_AXIS));
			CDlabel = new JLabel("Causal Depth: ");
			if (pathMethod == BEST_LOCAL_GERMS) 
				CDlabel.setForeground(new Color(20,20,20));
			else
				CDlabel.setForeground(new Color(200,200,200));
			CDproperties.add(CDlabel);			
			causalDepthField = new JTextField("");
			
			if (pathMethod == BEST_LOCAL_GERMS) {
				causalDepthField.setText(String.valueOf(causalDepth));
				causalDepthField.setEditable(true);
				causalDepthField.setOpaque(false);
			} else {
				causalDepthField.setEditable(false);
				causalDepthField.setOpaque(true);	
			}					
			CDproperties.add(causalDepthField);				
			HEPanel.add(CDproperties);	
						
			JPanel FDproperties = new JPanel();
			FDproperties.setLayout(new BoxLayout(FDproperties, BoxLayout.X_AXIS));			
			FDlabel = new JLabel("Final Depth: ");
			if (pathMethod == BEST_LOCAL_GERMS) 
				FDlabel.setForeground(new Color(20,20,20));
			else
				FDlabel.setForeground(new Color(200,200,200));
			FDproperties.add(FDlabel);			
			finalDepthField = new JTextField("");
			
			if (pathMethod == BEST_LOCAL_GERMS) {
				finalDepthField.setText(String.valueOf(finalDepth));
				finalDepthField.setEditable(true);
				finalDepthField.setOpaque(false);
			} else {
				finalDepthField.setEditable(false);
				finalDepthField.setOpaque(true);
			}
			finalDepthField.setOpaque(true);
			FDproperties.add(finalDepthField);			
			HEPanel.add(FDproperties);			
            HEPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Harmonic Extend"));
            panel1.add(HEPanel);
            
            panel1.add(new JSeparator(SwingConstants.HORIZONTAL));
            panel1.add(new JSeparator(SwingConstants.HORIZONTAL));
            	    	
			// Riemann Level
            JPanel RLPanel = new JPanel();
			RLPanel.setLayout(new BoxLayout(RLPanel, BoxLayout.Y_AXIS));
			JPanel LTproperties = new JPanel();
			LTproperties.setLayout(new BoxLayout(LTproperties, BoxLayout.X_AXIS));			
			JLabel LTlabel = new JLabel("Local Threshold: ");
			LTproperties.add(LTlabel);	    	    	
			localThresholdField = new JTextField("");
			localThresholdField.setText(String.valueOf(localThreshold));
			localThresholdField.setEditable(true);
			localThresholdField.setOpaque(true);
	    	LTproperties.add(localThresholdField);	    	
	    	RLPanel.add(LTproperties);
			
	    	JPanel GTproperties = new JPanel();
			GTproperties.setLayout(new BoxLayout(GTproperties, BoxLayout.X_AXIS));			
			JLabel GTlabel = new JLabel("Global Threshold: ");
			GTproperties.add(GTlabel);    		    	
			globalThresholdField = new JTextField("");
			globalThresholdField.setText(String.valueOf(globalThreshold));
			globalThresholdField.setEditable(true);
			globalThresholdField.setOpaque(true);
			GTproperties.add(globalThresholdField);			
			RLPanel.add(GTproperties);					
	        RLPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Riemann Level"));
	        panel1.add(RLPanel);
	        
	        panel1.add(new JSeparator(SwingConstants.HORIZONTAL));
	        panel1.add(new JSeparator(SwingConstants.HORIZONTAL));
	        
	        // Add metrix proximity parameters:
	        MetricPanel = new JPanel();
	        MetricPanel.setLayout(new BoxLayout(MetricPanel, BoxLayout.Y_AXIS));
	        
            useMetersBox = new JCheckBox("Use Local Meters"); 
            useMetersBox.setToolTipText("Harmonic Analysis with metric proximity");
            useMetersBox.setSelected(useMeters);
            useMetersBox.addActionListener(this);
            MetricPanel.add(useMetersBox);

			JPanel MetricProperties = new JPanel();
			MetricProperties.setLayout(new BoxLayout(MetricProperties, BoxLayout.X_AXIS));			
			MetricPropertieslabel = new JLabel("Minimum Local Meter Length: ");
			MetricProperties.add(MetricPropertieslabel);	    	    	
			MetricPropertiesField = new JTextField("");
			MetricPropertiesField.setText(String.valueOf(minLocalMeterLength));
			MetricPropertiesField.setEditable(true);
			MetricPropertiesField.setOpaque(true);
			MetricProperties.add(MetricPropertiesField);	    	
	    	MetricPanel.add(MetricProperties);
	    	
			JPanel QuantizeProperties = new JPanel();
			QuantizeProperties.setLayout(new BoxLayout(QuantizeProperties, BoxLayout.X_AXIS));			
			Quantizationlabel = new JLabel("Quantization: ");
			QuantizeProperties.add(Quantizationlabel);	    	    	
			QuantizationField = new JTextField("");
			QuantizationField.setText(String.valueOf(quantization));
			QuantizationField.setEditable(true);
			QuantizationField.setOpaque(true);
			QuantizeProperties.add(QuantizationField);	    	
	    	MetricPanel.add(QuantizeProperties);
	    	MetricPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Metrix Proximity Parameters"));

	    	panel1.add(MetricPanel);
	    				
			ptPane.addTab("Harmonic Path", icon, panel1, "");
	        ptPane.setMnemonicAt(0, KeyEvent.VK_1);
	        	        						
	        // Weight Calculation Method: Local-Mazzola or Global	        	        
	        JPanel panel2 = new JPanel();
	        panel2.setLayout(new BoxLayout(panel2, BoxLayout.Y_AXIS));
	        
	        panel2.add(new JSeparator(SwingConstants.HORIZONTAL));
	        panel2.add(new JSeparator(SwingConstants.HORIZONTAL));
	        
			JPanel calcPane = new JPanel();
			calcPane.setLayout(new BoxLayout(calcPane, BoxLayout.X_AXIS));

            calcWeightsBox = new JCheckBox("Calculate Harmonic Weights of Notes"); 
            calcWeightsBox.setHorizontalTextPosition(JCheckBox.RIGHT);
            calcWeightsBox.setToolTipText("Calculate Harmonic Weights");
            calcWeightsBox.setSelected(calculateWeights);
            calcPane.add(calcWeightsBox);
            
            panel2.add(calcPane);

	        panel2.add(new JSeparator(SwingConstants.HORIZONTAL));
	        panel2.add(new JSeparator(SwingConstants.HORIZONTAL));
	        
			JPanel methodPane = new JPanel();
			methodPane.setLayout(new BoxLayout(methodPane, BoxLayout.X_AXIS));
			
			JLabel psLabel = new JLabel("Calculation Method: ");
			methodPane.add(psLabel);
			calculationMethodSelector = new JComboBox<String>();
			calculationMethodSelector.setEditable(false);
			calculationMethodSelector.addActionListener(this);
			for (int i = 0; i < methods.length; i++) {
				calculationMethodSelector.addItem(methods[i]);
			}
			calculationMethodSelector.setSelectedItem(selectedMethod);
			methodPane.add(calculationMethodSelector);
			panel2.add(methodPane);
			
	        panel2.add(new JSeparator(SwingConstants.HORIZONTAL));
	        panel2.add(new JSeparator(SwingConstants.HORIZONTAL));
	        
	        DPanel = new JPanel();
	     	DPanel.setLayout(new BoxLayout(DPanel, BoxLayout.X_AXIS));
	     	dfLabel = new JLabel("Distance Factor: ");
	     	dfLabel.setForeground(new Color(20,20,20));
			DPanel.add(dfLabel);
			distanceFactorField = new JTextField("0.5");
			distanceFactorField.setEditable(true);
			distanceFactorField.setText(String.valueOf(distanceFactor));
			distanceFactorField.setOpaque(true);			
			DPanel.add(distanceFactorField);					
			panel2.add(DPanel);
						
			// grid drawing paramaters
	     	G1Panel = new JPanel();
	     	G1Panel.setLayout(new BoxLayout(G1Panel, BoxLayout.X_AXIS));
	     	GPWLabel = new JLabel("Grid Pen Width: ");
	     	G1Panel.add(GPWLabel);
	     	GPWLabel.setForeground(new Color(20,20,20));
			GPWField = new JTextField("1");
			GPWField.setText(String.valueOf(gridPenWidth));
			GPWField.setEditable(true);
			GPWField.setOpaque(true);			
			G1Panel.add(GPWField);					
			panel2.add(G1Panel);

	     	G2Panel = new JPanel();
	     	G2Panel.setLayout(new BoxLayout(G2Panel, BoxLayout.X_AXIS));
	     	GOLabel = new JLabel("Grid Origin: ");
	     	G2Panel.add(GOLabel);
	     	GOLabel.setForeground(new Color(20,20,20));
			G2Panel.add(GOLabel);
			GOField = new JTextField("0.0");
			GOField.setText(String.valueOf(gridOrigin));
			GOField.setEditable(true);
			GOField.setOpaque(true);			
			G2Panel.add(GOField);					
			panel2.add(G2Panel);

	     	G3Panel = new JPanel();
	     	G3Panel.setLayout(new BoxLayout(G3Panel, BoxLayout.X_AXIS));
	     	GMLabel = new JLabel("Grid Mesh: ");
	     	G3Panel.add(GMLabel);
	     	GMLabel.setForeground(new Color(20,20,20));
			G3Panel.add(GMLabel);
			GMField = new JTextField("1.0");
			GMField.setText(String.valueOf(gridMesh));
			GMField.setEditable(true);
			GMField.setOpaque(true);			
			G3Panel.add(GMField);					
			panel2.add(G3Panel);
							
	        ptPane.addTab("Harmonic Weights", icon, panel2,"");
	        ptPane.setMnemonicAt(1, KeyEvent.VK_2);	  
	        properties.add(ptPane);
		}
		return properties;
	}
	
	
				
	public boolean applyProperties() {		
		
		int i = BPCSelector.getSelectedIndex();
		if (i == VITERBI) {
			pathMethod = VITERBI;
		} else if (i == BEST_LOCAL_GERMS) {
			pathMethod = BEST_LOCAL_GERMS;
			causalDepth = Integer.parseInt(causalDepthField.getText());
			finalDepth = Integer.parseInt(finalDepthField.getText());
		}		
		localThreshold = Integer.parseInt(localThresholdField.getText());
		globalThreshold = Integer.parseInt(globalThresholdField.getText());		
		
		// for metrix proximity
		setUseMeters(useMetersBox.isSelected());
		
		if (useMeters) {
			minLocalMeterLength = Integer.parseInt(MetricPropertiesField.getText());
			quantization = Double.parseDouble(QuantizationField.getText());		
		}
		
		int calculationMethodIndex = calculationMethodSelector.getSelectedIndex();
		if (calculationMethodIndex == 0)
			selectedMethod = "Local-Mazzola";
		else if (calculationMethodIndex == 1)
			selectedMethod = "Global";				
		distanceFactor = Double.parseDouble(distanceFactorField.getText());		
		gridPenWidth = Integer.parseInt(GPWField.getText());	
		gridOrigin = Double.parseDouble(GOField.getText());
		gridMesh = Double.parseDouble(GMField.getText());
	    useMeters = useMetersBox.isSelected();
	    calculateWeights = calcWeightsBox.isSelected();	    
	    return true;
	}	
	
	private void setUseMeters(boolean b) {
	    	useMeters = b;
	}

	
	
	public void run(RunInfo runInfo) {
 						
		readInputDenotators();
    	applyThreshold(matrices);
    	    	
    	if (useMeters) {  		
			// using metrix proximity, therefore, evaluate local meters    		
			if (notes == null) {
				addError("3rd Input denotator is not of form \"Score\".");
				return;
			} 
		    ArrayList<LocalMeter> meters = new ArrayList<LocalMeter>(notes.size()*notes.size());			
		    ArrayList<GMINote> qNotes = quantize(notes, this.quantization); 
	        findLocalMeters(qNotes, meters);
	        
	        // this is the final path where all local-meter paths will be written onto
	        ArrayList<RMPoint> finalPath = new ArrayList<RMPoint>(matrices.size());
	            	   	        
	    	for (int localM = 0; localM < meters.size(); localM++) {   	
	    		
	    		ArrayList <AbstractRiemannMatrix> meterMatrices = new ArrayList <AbstractRiemannMatrix>(matrices.size());
	    		filterMatrices(meters.get(localM), matrices, meterMatrices);
	    		
	    		bestPath = new ArrayList<RMPoint>(meterMatrices.size());
	    		windowPaths = new ArrayList<ArrayList<RMPoint> >(matrices.size());

	    		if (pathMethod == VITERBI) {        		
	        		Viterbi vit = new Viterbi(meterMatrices, bestPath, tons, modes, funs, tonTension, modeTension, funTension);
	        		vit.find();    	        		
	        	} else if (pathMethod == BEST_LOCAL_GERMS) {
	        		BestLocalGerms blg = new BestLocalGerms(meterMatrices, 
			        								bestPath, windowPaths,
			        								rowSize, colSize,
			        								tons, modes, funs, 	        			
			        								tonTension, modeTension, funTension, 
			        								inclusion, causalDepth, finalDepth);
	        		blg.find();	        		
	        	}	      
	        	// in order to pass the result to lilypond
	        	// accumulate path for all local meters and display all of them once.
	        	// this also avoids multiple (and different) function assignments to a chord.
	        	
	        	// write bestPath to finalPath
	        	inject(bestPath, finalPath);	        	
	    	}	
	    		
	    	Collections.sort(finalPath);   
    		if (pathView != null) {
    			if (!tableOn) {
    				pathView.displayTable(matrices, tons, modes, funs);	
    				tableOn = true;
    			} 
    			pathView.displayBestPath(finalPath, matrices, tons, modes, funs);	
    		}		    			    		
        	HarmonicAnalysisSequence = makeHarmAnalysisSequence(finalPath);   
    		setOutput(0, HarmonicAnalysisSequence);		    			    		

    	} else {  
			    // temporal, i.e. traditional way of doing harmonic analysis
    			// if windowPaths work, bestPath may be removed completely.
			    bestPath = new ArrayList<RMPoint>(matrices.size());
	    		windowPaths = new ArrayList<ArrayList<RMPoint> >(matrices.size());
				
		        if (pathMethod == VITERBI) {		        		
	        			vit = new Viterbi(matrices, bestPath, tons, modes, funs, tonTension, modeTension, funTension);
	        			vit.find();  
		        } else if (pathMethod == BEST_LOCAL_GERMS) {		        
		        		blg = new BestLocalGerms(matrices, 
		        						bestPath, windowPaths,
				        				rowSize, colSize,
				        				tons, modes, funs, 
				        				tonTension, modeTension, funTension, 
				        				inclusion, causalDepth, finalDepth);		        		
		        		blg.find();		
		        		this.windowPaths = blg.windowPaths;
		        }			        		        		       	   		        
                if (pathView != null) {
                	    if (DEBUG) System.out.println("windowPaths size = " + this.windowPaths.size());
                	    pathView.windowPaths = this.windowPaths;
                        pathView.display(bestPath, tons, modes, funs);
                }                
		        HarmonicAnalysisSequence = makeHarmAnalysisSequence(bestPath);
		        setOutput(0, HarmonicAnalysisSequence);
		        
		        if (calculateWeights) {		  
		        	calcWeights();
	                Denotator weightSequence = makeWeightSequence(noteWeights);
	                setOutput(1, weightSequence);
		        }	
		  }      		
	}	
	
 	
	// written for temporal proximity
	// should be generalized...
	private void calcWeights() {
		// find weights of chords on the optimum path	
    	chordWeights = new double[matrices.size()];
    	
    	findChordWeights(chordWeights);
    	// Find weight of each note in each chord. To do this:
        // STEP 1: find the new Riemann matrix for the chord (without this note)
        // STEP 2: find the new optimum path
        // STEP 3: find the difference of between two paths' weight	
        if (selectedMethod == "Global") {
            noteWeights = findNoteWeightsGlobal(pathMethod, bestPath, chordWeights, causalDepth, finalDepth);
        } else {
            // Local-Mazzola method
            noteWeights = findNoteWeightsLocal(bestPath, chordWeights, causalDepth, finalDepth);    
        }
        if (noteWeights != null) {
     		notes = toNotes(matrices);
            Collections.sort(notes);

            if (weightView != null) {
                    weightPanel.removeAll();             
                    weightView = new JHarmonicWeight(notes, noteWeights, 
                    								weightCircleDiameter, gridPenWidth, gridOrigin, gridMesh,
                    								"Harmonic Weight of Notes");     
                    weightPanel.add(weightView);
            }
        }	
        normalize(noteWeights);	
        // display weights as a list
        // displayWeightList();	
	}

	// for metric proximity only, inject found path points
	// into the finalPath
	private void inject(ArrayList<RMPoint> meterPath, 
									ArrayList<RMPoint> finalPath) {				
		for (int i=0; i<meterPath.size(); i++) {
			RMPoint p = meterPath.get(i);	
			// add this point to the adequate place in finalPath
			finalPath.add(p);
			// System.out.println("Onset = " + p.chord.onset+" "+p.toString());
		}
	}
	
	// distance from matrix[r1][c1] to next matrix[r2][c2]
	// remember that only r1,c1,r2,c2 are required to compute this distance
	private double distance(int r1, int c1, int r2, int c2) {
		
		double tonalityDistance = 0;
		int tDiff = 0;
		double modeDistance = 0;
		double functionDistance = 0;	
		
		// Tonal distance
		tDiff = posMod(c2 - c1, tons.length);
		boolean found = false; 	
		
		for (int r=0; r<tonTension.length && !found; r++) {	
			int diff = 0;
			for (int c=0; c<tonTension[0].length && !found; c++) { 	
				if (tDiff == diff) {
					tonalityDistance = tonTension[r][c]*tonTension[r][c];	    						
					// interpret a negative value as an imaginary value
					// i.e. -0.2 is 0.2i
					if (tonTension[r][c] < 0)
						tonalityDistance = -tonalityDistance;
					found = true;
				} else {
					if (r == 0) 
						diff = posMod(diff+7, tons.length);
					else if (r == 1)
						diff = posMod(diff+5, tons.length);
				}
			}
		}		
		// Mode distance
		int mode1 = (r1 / funs.length);
		int mode2 = (r2 / funs.length);	
		modeDistance = modeTension[mode1][mode2]*modeTension[mode1][mode2];
		if (modeTension[mode1][mode2] < 0)
			modeDistance = -modeDistance;
		// Function distance
		int function1 = r1 % funs.length;
		int function2 = r2 % funs.length;
		functionDistance = funTension[function1][function2]*funTension[function1][function2];					    				    			
		if (funTension[function1][function2] < 0)
			functionDistance = -functionDistance;		
		double distanceSum = tonalityDistance + modeDistance + functionDistance;		
		double dWeight = Math.exp(-Math.abs(distanceSum)); // as formalized in our AHAN paper
		return dWeight;  
	}
	
	// return positive modulus
	private int posMod(int i, int n) {return ((i%n < 0) ? (i%n)+n : i%n);}
	
	// extract matrices that belong to the localMeter.
	private void filterMatrices(LocalMeter localMeter,
								ArrayList<AbstractRiemannMatrix> matrices, 
								ArrayList <AbstractRiemannMatrix> meterMatrices) {
		
		double period = localMeter.onsets.get(1) - localMeter.onsets.get(0);
		
		double startOnset = localMeter.onsets.get(0);
		int size = localMeter.onsets.size();
		int startMatrix = -1;
		int i, m;
		
		for (m=0; m<matrices.size(); m++) {
			if (matrices.get(m).chord.onset == startOnset) {
				startMatrix = m;
				break;
			}
		}		
		if (startMatrix == -1) {
			addError("Local Meter not found!");
			return;
		} else {
			meterMatrices.add(matrices.get(startMatrix));
			int curMatIdx = startMatrix;
			int curIdx = 1;
			while (curIdx < size) {
				for (i=curMatIdx+1; i<matrices.size(); i++) {
					if (matrices.get(i).chord.onset == startOnset + period*curIdx) {
						meterMatrices.add(matrices.get(i));
						break;
					}
				}
				curIdx++;
				curMatIdx = i;
			}
		}
	}
	
	// return notes as an array
	private ArrayList<GMINote> toNotes(ArrayList<AbstractRiemannMatrix> matrices2) {
		
		int numNotes = AbstractRiemannMatrix.countNotes(matrices2);
		
		ArrayList<GMINote> notes = new ArrayList<GMINote>(numNotes);
		for (int i=0; i<matrices2.size(); i++) {
			Chord chord = matrices2.get(i).chord;
			for (int n=0; n<chord.notes.size(); n++) {
				notes.add(chord.notes.get(n));
			}
		}
		return notes;
	}


	// from a path (of RMPoints) create a chordAnalysisSequence denotator
	private Denotator makeHarmAnalysisSequence(ArrayList<RMPoint> path) {
		
		LinkedList<Denotator> harmSeq = new LinkedList<Denotator>();
		
		for (RMPoint p : path) {
			if (DEBUG) System.out.println(p.toString());
			Denotator d = makeRMPoint(p.chord, p.row, p.col);
			harmSeq.add(d);
		}
		Denotator c = makeDenotator(chordAnalysisSequenceForm, harmSeq);
		return c;
	}

	// Make a denotator from a RMPoint
	public Denotator makeRMPoint(Chord chord, int row, int col) {	
		
		LinkedList<Denotator> factors = new LinkedList<Denotator>();

		Denotator d0 = chord.makeChord(); 
		factors.add(d0);
		Denotator dOnset = makeDenotator(onsetForm, chord.onset);
		factors.add(dOnset);
		Denotator dDur = makeDenotator(durationForm, chord.duration);
		factors.add(dDur);
		Denotator dKey = makeDenotator(rowForm, row); 
		factors.add(dKey);
		Denotator dFunc = makeDenotator(colForm, col);
        factors.add(dFunc);
        Denotator d = makeDenotator(chordAnalysisForm, factors);
        return d;
	}
	
	
	/*
	 * Filtering on matrices with respect to local and global threshold percentages 
	 */
	private void applyThreshold(ArrayList<AbstractRiemannMatrix> matrices) {
			
		double globalMaxRiemannVal = calculateGlobalMaxRiemannVal(matrices);
		filter(matrices, globalThreshold, globalMaxRiemannVal);
		filter(matrices, localThreshold);
	}

	
	/*
	 * calculate the maximum Riemann matrix cell value among all matrices
	 */
	public static double calculateGlobalMaxRiemannVal(ArrayList<AbstractRiemannMatrix> matrices) {

		double globalMax = matrices.get(0).getValue(0, 0);
		double matrixMax = globalMax;
		
		for (AbstractRiemannMatrix rm : matrices) {
			matrixMax = calculateLocalMaxRiemannVal(rm);
			if (matrixMax > globalMax) {
				globalMax = matrixMax;
			}
		}
		return globalMax;
	}

	
	/*
	 * calculate the maximum Riemann matrix cell value among all matrices
	 */
	public static double calculateLocalMaxRiemannVal(AbstractRiemannMatrix matrix) {
		double max = matrix.getValue(0, 0);
		
		for (int r=0; r<matrix.rowCount; r++) {
			for (int c=0; c<matrix.colCount; c++) {
				if (matrix.getValue(r, c) > max) {
					max = matrix.getValue(r, c);
				}
			}
		}
		return max;
	}

	// 'filter' an array of matrices wrt the given thresholdValue 
	public void filter (ArrayList<AbstractRiemannMatrix> matrices, 
								int percentage, 
								double max) {
		if (percentage != 0) {
			for (AbstractRiemannMatrix matrix : matrices) {
				matrix.filter((double) ((max*percentage)/100.0));
			}
		}
	}
	
		
	// 'filter' an array of matrices wrt matrix's local minimums 
	public void filter (ArrayList<AbstractRiemannMatrix> matrices, 
							int percentage) {
		
		if (percentage != 0) {
			for (AbstractRiemannMatrix matrix : matrices) {
				double min = calculateLocalMaxRiemannVal(matrix);
				matrix.filter((min*percentage)/100.0);
			}
		}
	}
	
	
	public JComponent getView() {	        
		
		if (view == null) {			
			view = new JPanel();
			tPane = new JTabbedPane();
				        
	        weightPanel = new JPanel();
	        weightPanel.setLayout(new BoxLayout(weightPanel, BoxLayout.Y_AXIS));
        	weightView = new JHarmonicWeight(notes, noteWeights, weightCircleDiameter, gridPenWidth, gridOrigin, gridMesh,
        			"Harmonic Weight of Notes");           	
        	weightPanel.add(weightView);
        	       	
           	tPane.addTab("Harmonic Weights", icon, weightPanel, "");
        	tPane.setMnemonicAt(0, KeyEvent.VK_1);  
        	
        	// Weight View, only if weights to show        	
	        if (notes != null && noteWeights != null) {
				weightPanel.removeAll();
               	weightView = new JHarmonicWeight(notes, noteWeights, weightCircleDiameter, gridPenWidth, gridOrigin, gridMesh,
               			"Harmonic Weight of Notes");  
               	weightPanel.add(weightView);
 	        }  	
	        
			// Path View
            JPanel pathPanel = new JPanel();
            pathPanel.setLayout(new BoxLayout(pathPanel, BoxLayout.Y_AXIS));		
            pathView = new JHarmonicAnalysisDisplay(bestPath, windowPaths);
            pathPanel.add(pathView);
            
			if (bestPath != null) {		
				if (DEBUG) System.out.println("windowPaths size = " + this.windowPaths.size());
				pathView.display(bestPath, tons, modes, funs);	
			}						
			tPane.addTab("Harmonic Path", icon, pathPanel, "");
	        tPane.setMnemonicAt(1, KeyEvent.VK_2);
	        view.add(tPane);
		}
		return view;
	}
	
				
	private void readInputDenotators() {
		
		// input denotator #1
		Denotator riemannMatrixSeq = getInput(0); 		
		if (riemannMatrixSeq == null) {
			addError("Input denotator is null.");
			HarmonicAnalysisSequence = null;
			return;
		} else if (!riemannMatrixSeq.hasForm(riemannMatrixSequenceWithMethodForm)) {
			addError("Input denotator is not of form \"RiemannMatrixSequenceWithMethod\".");
			HarmonicAnalysisSequence = null;
			return;
		}				
		// input denotator #2
		Denotator analysisSpec = getInput(1);
		// check 2nd input
		if (analysisSpec == null) {
	        addError("Input denotator #2 (analysis specification) is null.");
	        HarmonicWeightSequence = null;
	        return;
	    } else if (!analysisSpec.hasForm(analysisSpecForm)) {        	
	    	addError("Input denotator #2 is not of form \"AnalysisSpec\".");
	        HarmonicWeightSequence = null;
	        return;
	    } else {       	
	    	// prepare matrix specification from analysisSpec
	    	List<Denotator> specList = ((FactorDenotator)analysisSpec).getFactors();
	    	
	    	// tonalities
	    	PowerDenotator p = (PowerDenotator) specList.get(0);
	    	List<Denotator> tonsList = ((FactorDenotator)p).getFactors();
	    	this.tons = new int[tonsList.size()];        	
	    	int i=0;
	    	for (Denotator t : tonsList) 
	    		this.tons[i++] = ((SimpleDenotator) t).getInteger();        	     	
	    	// modes
	    	p = (PowerDenotator) specList.get(1);
	    	List<Denotator> modesList = ((FactorDenotator) p).getFactors();
	    	this.modes = new int[modesList.size()];      	
	    	i=0;
	    	for (Denotator m : modesList) {
	    		this.modes[i++] = ((SimpleDenotator) m).getInteger();
	    	}      	
	    	// functions
	    	p = (PowerDenotator) specList.get(2);
	    	List<Denotator> funsList = ((FactorDenotator) p).getFactors();
	    	this.funs = new int[funsList.size()];
	    	i=0;
	    	for (Denotator f : funsList) {
	    		this.funs[i++] = ((SimpleDenotator) f).getInteger();
	    	}       	        	
	       	// weight-of-thirds
        	p = (PowerDenotator) specList.get(3);
        	List<Denotator> weightsList = ((FactorDenotator)p).getFactors();
        	this.weights = new double[weightsList.size()];
        	
        	for (Denotator w : weightsList) {   		
        		LimitDenotator l = (LimitDenotator) w;  		
        		int thisRow = ((SimpleDenotator)l.getFactor(0)).getInteger();
        		int thisCol = ((SimpleDenotator)l.getFactor(1)).getInteger();	
        		double thisWeight = ((SimpleDenotator)l.getFactor(2)).getReal();
        		this.weights[thisRow*tons.length+thisCol] = thisWeight;
        	}	

	    	// unpack tonality tension denotator
	    	LimitDenotator ll = (LimitDenotator) specList.get(4);
	    	rowSize = ((SimpleDenotator) ll.getFactor(1)).getInteger();
	    	colSize = ((SimpleDenotator) ll.getFactor(2)).getInteger();
	    	if (DEBUG) System.out.println("rowSize = "+rowSize+" and colSize=" + colSize);
	    	PowerDenotator tonTensionList = (PowerDenotator) ll.getFactor(3);
	 
	    	tonTension = new double[rowSize][colSize];
	    	
	    	for (Denotator t : tonTensionList) {
	    		LimitDenotator l = (LimitDenotator) t;  		       		
	    		int thisRow = ((SimpleDenotator)l.getFactor(0)).getInteger();
	    		int thisCol = ((SimpleDenotator)l.getFactor(1)).getInteger();	
	    		double thisTension = ((SimpleDenotator)l.getFactor(2)).getReal();
	    		tonTension[thisRow][thisCol] = thisTension;
	    		if (DEBUG) System.out.println("tonTension[" +thisRow+ "," + thisCol+ "]=" + thisTension);
	    	}
	 
	       	// unpack mode tension denotator
	    	LimitDenotator l2 = (LimitDenotator) specList.get(5);
	    	rowSize = ((SimpleDenotator) l2.getFactor(1)).getInteger();
	    	colSize = ((SimpleDenotator) l2.getFactor(2)).getInteger();
	       	if (DEBUG) System.out.println("rowSize = "+rowSize+" and colSize=" + colSize);
	    	PowerDenotator modTensionList = (PowerDenotator) l2.getFactor(3);       	       	
	    	modeTension = new double[rowSize][colSize];
	    	
	    	for (Denotator t : modTensionList) {
	    		LimitDenotator l = (LimitDenotator) t;  		
	    		int thisRow = ((SimpleDenotator)l.getFactor(0)).getInteger();
	    		int thisCol = ((SimpleDenotator)l.getFactor(1)).getInteger();	
	    		double thisTension = ((SimpleDenotator)l.getFactor(2)).getReal();
	    		modeTension[thisRow][thisCol] = thisTension;
	    		if (DEBUG) System.out.println("modeTension["+thisRow+","+thisCol+"]="+modeTension[thisRow][thisCol]);
	    	}
	    	        	
	    	// function tension denotator
	    	LimitDenotator l3 = (LimitDenotator) specList.get(6);
	    	rowSize = ((SimpleDenotator) l3.getFactor(1)).getInteger();
	    	colSize = ((SimpleDenotator) l3.getFactor(2)).getInteger();  
	    	if (DEBUG) System.out.println("rowSize = "+rowSize+" and colSize=" + colSize);
	       	PowerDenotator funTensionList = (PowerDenotator) l3.getFactor(3);       	           
	    	funTension = new double[rowSize][colSize];
	    	
	    	for (Denotator t : funTensionList) {
	    		LimitDenotator l = (LimitDenotator) t;  		
	    		int thisRow = ((SimpleDenotator)l.getFactor(0)).getInteger();
	    		int thisCol = ((SimpleDenotator)l.getFactor(1)).getInteger();	
	       		double thisTension = ((SimpleDenotator)l.getFactor(2)).getReal();
	    		funTension[thisRow][thisCol] = thisTension;
	    		if (DEBUG) System.out.println("funTension["+thisRow+","+thisCol+"]="+funTension[thisRow][thisCol]);
	    	}
	    	
	    	// unpack inclusion table
	    	LimitDenotator l4 = (LimitDenotator) specList.get(7);
	    	rowSize = ((SimpleDenotator) l4.getFactor(1)).getInteger();
	    	colSize = ((SimpleDenotator) l4.getFactor(2)).getInteger();  
	    	if (DEBUG) System.out.println("rowSize = "+rowSize+" and colSize=" + colSize);
	      	PowerDenotator inclusionTable = (PowerDenotator) l4.getFactor(3);       	           
	      	inclusion = new boolean[rowSize][colSize];
	    	
	    	for (Denotator inc : inclusionTable) {   		
	    		LimitDenotator l = (LimitDenotator) inc;  		
	    		int thisRow = ((SimpleDenotator)l.getFactor(0)).getInteger();
	    		int thisCol = ((SimpleDenotator)l.getFactor(1)).getInteger();
	    		int val = ((SimpleDenotator)l.getFactor(2)).getInteger();
	    		if (val == 1)
	    			this.inclusion[thisRow][thisCol] = true;
	    		else
	    			this.inclusion[thisRow][thisCol] = false;	
	    	}	
	    	
	    	// Now unpack Riemann Matrices
	    	LimitDenotator chordAndRiemannSeq = (LimitDenotator) riemannMatrixSeq;
	    	chordMethod = ((SimpleDenotator)chordAndRiemannSeq.getFactor(0)).getInteger();
	    	if (DEBUG) 
	    		System.out.println("chordMethod = " + chordMethod);
	    	
	    	List<Denotator> methodAndRiemannMatrixList = ((FactorDenotator)riemannMatrixSeq).getFactors();	    			
	    	Denotator rMatrixList = (Denotator) methodAndRiemannMatrixList.get(1);
	    	List<Denotator> riemannMatrixList = ((FactorDenotator)rMatrixList).getFactors();
			
	    	matrices = new ArrayList<AbstractRiemannMatrix>(riemannMatrixList.size());
			
			for (Denotator d: riemannMatrixList) {
		    	List<Denotator> chordAndMatrix = ((FactorDenotator)d).getFactors();
		    	Denotator c = (Denotator) chordAndMatrix.get(0);
		    	if (DEBUG) 
		    		c.display();
		    	Denotator m = (Denotator) chordAndMatrix.get(1);
		    	AbstractRiemannMatrix mat = new AbstractRiemannMatrix(c, m, modes.length*funs.length, tons.length);
				matrices.add(mat);
			}	   
			
			if (useMeters) {
				Denotator score = getInput(2); 	
				if (score == null) {
		            addError("Input denotator is null.");
		            notes = null;
		        } else if (!score.hasForm(scoreForm)) {
			        addError("Input denotator is not of form \"Score\".");
			        notes = null;
			    } else {
			       // return a sorted list of notes
			       notes = GMINote.scoreToGMINotes(score);  
			    }
			}
	    }
	}

	
	
	// quantization of note onsets
	// for quantization of 0.25, round onset values
	// to closest multiple of 0.25
	private ArrayList<GMINote> quantize(ArrayList<GMINote> noteSeq, double qVal) {
		
		ArrayList<GMINote> qNotes = new ArrayList<GMINote>(notes.size());
		
		for (int i=0; i<noteSeq.size(); i++) {	
			GMINote note = noteSeq.get(i);		
			int res = (int) Math.round((note.getOnset() / qVal));
			double qOnset = res*qVal;
			GMINote qnote = new GMINote(qOnset, note.getPitch(), note.getPitchInt(), 
							note.getLoudness(), note.getDuration(), note.getVoice());
			qNotes.add(qnote);			
		}				
		return qNotes;
	}
	
	
	// quantize onset values of matrices
	private void quantizeMatrices(ArrayList<AbstractRiemannMatrix> matrices, double qVal) {
		
		for (int i=0; i<matrices.size(); i++) {	
			double onset = matrices.get(i).chord.onset;
			int res = (int) Math.round((onset / qVal));
			matrices.get(i).chord.onset = res*qVal;		
		}				
	}

	
	
	// The following is copied verbatim from JMeterDisplay
	public void findLocalMeters(ArrayList<GMINote> notes, ArrayList<LocalMeter> localMeterSeq ) {
		
		ArrayList<Double> onsetSeq = new ArrayList<Double>(notes.size());
		
		// get the onsets from notes
		for (GMINote n: notes)
			onsetSeq.add(n.onset);
		
		// for every onset, find localmeters that begin with this onset
		// and add them to localMeterSeq
		for (int i=0; i<onsetSeq.size(); i++) {
			double curr_onset = onsetSeq.get(i);
			
			for (int j=i+1; j<onsetSeq.size(); j++) {
				Double period = onsetSeq.get(j)-curr_onset;
				if (period > 0.0) {
					LocalMeter curr_localmeter = new LocalMeter();
					curr_localmeter.add(curr_onset);
					int length = 0;
					curr_localmeter.add(onsetSeq.get(j));
					length++;
					
					// now this is a possible local meter, with 'period'
					// see if it has length of at least minLocalMeterLength
					Double member_candidate = onsetSeq.get(j)+period*length;
					
					while (onsetSeq.contains(member_candidate)) {
						curr_localmeter.add(member_candidate);
						length++;
						member_candidate = onsetSeq.get(j)+period*length;
					}
					
					// add this localmeter to LocalMeterSeq if length >= minLocalMeterLength
					// and is a maximal local meter
					if (length >= this.minLocalMeterLength) {
						// do not add, is not maximal
						boolean isMaximal = true;
						
						// check if this localMeter is a subset of
						// an existing localMeter
						for (LocalMeter lm: localMeterSeq) {
							if (lm.subsetOf(curr_localmeter))
								isMaximal = false;
						}
						if (isMaximal) {
							localMeterSeq.add(curr_localmeter);
						}
					}
				}	
			}	
		}		
		// sort local meters in ascending order (shortest local meters first)
		Collections.sort(localMeterSeq);
		// quantization
		quantizeMatrices(matrices, this.quantization);	
	}
		
	private int WEIGHT_LIST_SIZE = 600;
	private int WEIGHT_LIST_LOCATION = 300;
		
	// display weights in a separate window as a list
	public void displayWeightList() {
		
        JWeightList wList = new JWeightList(notes, noteWeights);
        if (notes != null) {
            wList.view("Harmonic Weights");
        }       
        wList.pack();
        wList.setMinimumSize(new Dimension(WEIGHT_LIST_SIZE, WEIGHT_LIST_SIZE));
        wList.setPreferredSize(new Dimension(WEIGHT_LIST_SIZE, WEIGHT_LIST_SIZE));
        wList.setMinimumSize(new Dimension(WEIGHT_LIST_SIZE, WEIGHT_LIST_SIZE));
        wList.setVisible(true);	
        wList.setLocation(WEIGHT_LIST_LOCATION,WEIGHT_LIST_LOCATION);	  
	}
	
	public boolean hasProperties() {
		return true;
	}
	
	public Rubette duplicate() {
        HarmonicPathRubette rubette = new HarmonicPathRubette();
        return rubette;
	}
	
    public void revertProperties() { }
	
	public boolean hasView() {
        return true;
    }		
		
	public String getGroup() {
        return "Score";
    }
	
    public String getName() {
        return "HarmonicPath";
    }
    
    
    public String getShortDescription() {
        return "Calculates a best harmonic path between chords";
    }
        
    public ImageIcon getIcon() {
        return icon;
    }
    
    public String getLongDescription() {
        return "The HarmonicWeight Rubette gets in a denotator of riemann matrix sequence form"+
               " and evaluates a best possible harmonic path. "; /* +
        		"1st input is from HarmonicWeights Rubette. " + 
        		"2nd input is from HarmonicModel Rubette. " + 
        		"3rd input is from MidiFileIn Rubette. "; */
    }

    private final static String CALCULATION_METHOD = "Path-Calculation-Method";
    private final static String CAUSAL_DEPTH = "Causal-Depth";
    private final static String FINAL_DEPTH = "Final-Depth";
    private final static String LOCAL_THRESHOLD = "Local-Threshold";
    private final static String GLOBAL_THRESHOLD = "Global-Threshold";
    
    private final static String USE_LOCAL_METERS = "Use-Local-Meters";
    private final static String MIN_LOCAL_METER_LENGTH = "Min-Local-Meter-Length";
    private final static String QUANTIZATION = "Quantization";
    
    private final static String DISTANCE_FACTOR = "Distance-Factor";
    private final static String GRID_PEN = "Grid-Pen-Width";
    private final static String GRID_ORIGIN = "Grid-Origin";
    private final static String GRID_MESH = "Grid-Mesh";
       
    public void toXML(XMLWriter writer) {
    	
    	if (pathMethod == VITERBI) {
    		writer.empty(CALCULATION_METHOD, VALUE_ATTR, "Viterbi");
    	} else if (pathMethod == BEST_LOCAL_GERMS) {
    		writer.empty(CALCULATION_METHOD, VALUE_ATTR, "Best-Local-Germs");
    		writer.empty(CAUSAL_DEPTH, VALUE_ATTR, causalDepth);
    		writer.empty(FINAL_DEPTH, VALUE_ATTR, finalDepth);
    	}
    	
    	writer.empty(LOCAL_THRESHOLD, VALUE_ATTR, localThreshold);
    	writer.empty(GLOBAL_THRESHOLD, VALUE_ATTR, globalThreshold );
    	
    	// metric proximity parameters
    	writer.empty(USE_LOCAL_METERS, VALUE_ATTR, useMeters?TRUE_VALUE:FALSE_VALUE);
    	writer.empty(MIN_LOCAL_METER_LENGTH, VALUE_ATTR, minLocalMeterLength);
    	writer.empty(QUANTIZATION, VALUE_ATTR, quantization);
   	
    	
    	// Harmonic Weight Paramaters
    	writer.empty(DISTANCE_FACTOR, VALUE_ATTR, distanceFactor);
    	writer.empty(GRID_PEN, VALUE_ATTR, gridPenWidth);		
    	writer.empty(GRID_ORIGIN, VALUE_ATTR, gridOrigin);
    	writer.empty(GRID_MESH, VALUE_ATTR, gridMesh);
    }
    
    public Rubette fromXML(XMLReader reader, Element element) {
   	
    	int method=0;
    	
    	Element child = XMLReader.getChild(element, CALCULATION_METHOD);
    	String val = child.getAttribute(VALUE_ATTR);
    	if (val.equals("Viterbi")) {
    		method=VITERBI;
    	} else if (val.equals("Best-Local-Germs")) {
    		method=BEST_LOCAL_GERMS;
    	}   	
    	if (method == BEST_LOCAL_GERMS) {
    		child = XMLReader.getChild(element, CAUSAL_DEPTH);
    		val = child.getAttribute(VALUE_ATTR);
    		causalDepth = Integer.parseInt(val);
    		
       		child = XMLReader.getChild(element, FINAL_DEPTH);
    		val = child.getAttribute(VALUE_ATTR);
    		finalDepth = Integer.parseInt(val);
    	}   	
    	// use meter params
    	boolean useM = false;
    	child = XMLReader.getChild(element, USE_LOCAL_METERS);
    	val = child.getAttribute(VALUE_ATTR);
    	if (val.equals(TRUE_VALUE)) {
    		useM = true;
    	} 
    	child = XMLReader.getChild(element, MIN_LOCAL_METER_LENGTH);
    	val = child.getAttribute(VALUE_ATTR);
    	minLocalMeterLength = Integer.parseInt(val);

    	child = XMLReader.getChild(element, QUANTIZATION);
    	val = child.getAttribute(VALUE_ATTR);
    	quantization = Double.parseDouble(val);
   	   	    	
    	// harmonic weight params
    	child = XMLReader.getChild(element, LOCAL_THRESHOLD);
    	val = child.getAttribute(VALUE_ATTR);
    	localThreshold = Integer.parseInt(val);
    	
       	child = XMLReader.getChild(element, GLOBAL_THRESHOLD);
    	val = child.getAttribute(VALUE_ATTR);
    	globalThreshold = Integer.parseInt(val);
    	
    	// Harmonic Weight Paramaters
    	child = XMLReader.getChild(element, DISTANCE_FACTOR);
    	val = child.getAttribute(VALUE_ATTR);
    	distanceFactor = Double.parseDouble(val);
    	
    	child = XMLReader.getChild(element, GRID_PEN);
    	val = child.getAttribute(VALUE_ATTR);
    	gridPenWidth = Integer.parseInt(val);

    	child = XMLReader.getChild(element, GRID_ORIGIN);
    	val = child.getAttribute(VALUE_ATTR);
    	gridOrigin = Double.parseDouble(val);
    	
    	child = XMLReader.getChild(element, GRID_MESH);
    	val = child.getAttribute(VALUE_ATTR);
    	gridMesh = Double.parseDouble(val);
    	
    	HarmonicPathRubette rubette = new HarmonicPathRubette();
    	rubette.pathMethod = method;
    	if (method == BEST_LOCAL_GERMS) {
    		rubette.causalDepth = causalDepth;
    		rubette.finalDepth = finalDepth;
    	}
    	rubette.localThreshold = localThreshold;
    	rubette.globalThreshold = globalThreshold;
    	
    	// use meter related params
    	rubette.useMeters = useM;
    	rubette.minLocalMeterLength = minLocalMeterLength;
    	rubette.quantization = quantization;
    	
    	// harmonic weight related params
    	rubette.distanceFactor = distanceFactor;
    	rubette.gridPenWidth = gridPenWidth;
    	rubette.gridMesh = gridMesh;
    	rubette.gridOrigin = gridOrigin;   	    	
    	return rubette;
    }
	
	public void valueChanged(ListSelectionEvent e) {}
		
	private Color FADED_COLOR = new Color(200,200,200);
	private Color ORIGINAL_COLOR = new Color(20,20,20);
			
	public void actionPerformed(ActionEvent e) {
				
		Object src = e.getSource();
		
		if (src == this.BPCSelector) {
			int i = BPCSelector.getSelectedIndex();
			if (i == VITERBI) {				
				// Change color of CD and FD fields
				// and also set them to uneditable	
				if (HEPanel != null) {
					TitledBorder tBorder = new TitledBorder(BorderFactory.createLineBorder(FADED_COLOR), "Harmonic Extend");
					tBorder.setTitleColor(FADED_COLOR);
					HEPanel.setBorder(tBorder);
					CDlabel.setForeground(FADED_COLOR);
					causalDepthField.setText("");
					causalDepthField.setEditable(false);
					FDlabel.setForeground(FADED_COLOR);
					finalDepthField.setText("");
					finalDepthField.setEditable(false);
				}
			} else if (i == BEST_LOCAL_GERMS) {		
				if (HEPanel != null) {
					HEPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Harmonic Extend"));
					CDlabel.setForeground(ORIGINAL_COLOR);
					causalDepthField.setEditable(true);
					causalDepthField.setText(Integer.toString(CD));
					FDlabel.setForeground(ORIGINAL_COLOR);
					finalDepthField.setEditable(true);
					finalDepthField.setText(Integer.toString(FD));	
				}
			}	
		} else if (src == useMetersBox) {
			boolean val = useMetersBox.isSelected();
			if (val == false) {
				if (MetricPanel != null) {
					TitledBorder tBorder = new TitledBorder(BorderFactory.createLineBorder(FADED_COLOR), "Metrix Proximity Parameters");
					tBorder.setTitleColor(FADED_COLOR);
					MetricPanel.setBorder(tBorder);
					MetricPropertieslabel.setForeground(FADED_COLOR);
					MetricPropertiesField.setText("");
					MetricPropertiesField.setEditable(false);
					Quantizationlabel.setForeground(FADED_COLOR);
					QuantizationField.setText("");
					QuantizationField.setEditable(false);
				}				
			} else {
				if (MetricPanel != null) {
					MetricPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Metrix Proximity Parameters"));
					MetricPropertieslabel.setForeground(ORIGINAL_COLOR);
					MetricPropertiesField.setEditable(true);
					MetricPropertiesField.setText(Integer.toString(minLocalMeterLength));
					Quantizationlabel.setForeground(ORIGINAL_COLOR);
					QuantizationField.setEditable(true);
					QuantizationField.setText(Double.toString(quantization));
				}				
			}
		}
	}	
	
	
	// Using the path, find weight of the chord with index 'index'
	// within the path. 
	private double findChordWeight(int index, ArrayList<RMPoint> path) {

		double weight = 0.0;
		
		// not possible but...
		if (index >= path.size()) {
			addError("error in index " + index +" ; path size=" + path.size());
			return 0.0;
		}
		
		for (int i=0; i<index; i++) {
			RMPoint cur_pt = path.get(i);
			RMPoint next_pt = path.get(i+1);
			weight += cur_pt.value;
			weight += distance (cur_pt.row, cur_pt.col,  next_pt.row,  next_pt.col);
		}	
		// add last point's weight
		weight += path.get(index).value;	
		return weight;
	}	

	
	// Find weight of the chords in the path
	public void findChordWeights(double[] chordWeights) {
						
		chordWeights[0] = bestPath.get(0).value;
		
		for (int i=1; i<bestPath.size(); i++) {
			RMPoint prev_pt = bestPath.get(i-1);
			RMPoint cur_pt = bestPath.get(i);
			chordWeights[i] = chordWeights[i-1]+ 
							distance(prev_pt.row, prev_pt.col,  cur_pt.row,  cur_pt.col) + 
							cur_pt.value;	
			if (DEBUG) System.out.println("chordweight["+i+"] = "+chordWeights[i]);
		}				
	}
	
		
	// uses Global method:
	// remove note, find the new path, find the difference between two paths
	private ArrayList<Weight> findNoteWeightsGlobal(int pathMethod, ArrayList<RMPoint> path, 
													double[] chordWeights, 
													int causalD, int finalD) {		
		int effectiveCDepth = 0;
		int effectiveFDepth = 0;
		
		int numNotes = AbstractRiemannMatrix.countNotes(matrices);
		ArrayList<Weight> noteWeights = new ArrayList<Weight>(numNotes);
		if (DEBUG) System.out.println("NoteWeights Global:");
		
		for (int ch=0; ch<matrices.size(); ch++) {
			
			if (DEBUG) System.out.println("ch = " + ch);
			Chord chord = matrices.get(ch).chord;
			
			for (int n=0; n<chord.notes.size(); n++) {
				if (DEBUG) System.out.println("n = " + n);
				Chord newChord = new Chord(chord.duration, chord.onset, chord.notes);	
				newChord.removeNote(n);
				
				// window of new matrices that include the new chord
				ArrayList<AbstractRiemannMatrix> newMatrices = null;
			
				if (pathMethod == BEST_LOCAL_GERMS) {
					effectiveCDepth = (ch >= causalD) ? causalD : ch;
					effectiveFDepth = (ch+finalD < matrices.size()) ? finalDepth : matrices.size()-1-ch;			
					newMatrices = new ArrayList<AbstractRiemannMatrix>(effectiveCDepth + effectiveFDepth + 1);
					
					for (int i=ch-effectiveCDepth; i<ch; i++)
						newMatrices.add(matrices.get(i));
					
				}  else if (pathMethod == VITERBI) {
					// copy matrices to a new array (newMatrices) up to index ch
					newMatrices = new ArrayList<AbstractRiemannMatrix>(ch+1);
					for (int i=0; i<ch; i++) 						
						newMatrices.add(matrices.get(i));					
				}				
				// Prepare and add the new matrix
				AbstractRiemannMatrix newMatrix = new AbstractRiemannMatrix(null, tons.length, modes.length, funs.length);
				newMatrix.setChord(newChord);
				
				// if newChord has no note at all, do not compute newMatrix
				if (newChord.notes.size() != 0) {	
					// find the chain-of-thirds of the new chord
					if (chordMethod == CHAIN_OF_THIRDS) {
						chain3rd = newChord.findChainsOfThirds(chainRepository);						
						// compute new matrix data with respect to the chain-of-thirds
						newMatrix.compute3rdChain(chain3rd, weights, tons.length, modes.length, funs.length);
					} else if (chordMethod == DIRECT) {
						newMatrix.computeDirect(weights, tons.length, modes.length, funs.length);							
					} else {
						addError("Chord method is undefined?");
					}						
					// add the new matrix
					newMatrices.add(newMatrix);
					// newMatrix.print();
				}				
				// if best-local-germs method, then add also finalD many matrices
				if (pathMethod == BEST_LOCAL_GERMS) {
					if (effectiveFDepth >= 1) {
						for (int i=0; i < effectiveFDepth; i++) {
							newMatrices.add(matrices.get(ch+1+i));
						}
					}
				}
				
				// REMARK: size of the newPath can be one matrix shorter, if the chord has only one note!
				ArrayList<RMPoint> newPath = new ArrayList<RMPoint>(newMatrices.size());
				
				double newChordWeight = 0.0;
				
				if (pathMethod == BEST_LOCAL_GERMS) {
					BestLocalGerms blg = new BestLocalGerms(newMatrices, 
							newPath, windowPaths,
							rowSize, colSize,
							tons, modes, funs, 	        			
							tonTension, modeTension, funTension, 
							inclusion, causalDepth, finalDepth);					
					blg.find();	
					
					newChordWeight = findChordWeight(effectiveCDepth, newPath);
					if (ch-effectiveCDepth-1 >= 0) {
						newChordWeight += chordWeights[ch-effectiveCDepth-1];
						RMPoint pt = path.get(ch-effectiveCDepth-1);
						RMPoint ptNewChord = newPath.get(0);
						newChordWeight += distance (pt.row, pt.col,  ptNewChord.row,  ptNewChord.col);
					}					
				} else if (pathMethod == VITERBI) {
					Viterbi vit = new Viterbi(newMatrices, newPath,
												tons, modes, funs, 
												tonTension, modeTension, funTension);
					vit.find();	
					if (newMatrices.size() >= 1)
						newChordWeight = findChordWeight(newMatrices.size()-1, newPath);						
				} 
				// Weight of note x is 1 / (d + (1-d)e^(w(chord_i-{x}) - w(chord_i))  (from ToM pp:822)
				double value = 1.0 /(distanceFactor + (1.0 - distanceFactor)*Math.exp(newChordWeight - chordWeights[ch]));	
				GMINote note = chord.notes.get(n);
				Weight weight = new Weight(value, note);				
				noteWeights.add(weight);
			}
		}
		return noteWeights;
	}
	
	// uses the Local-Mazzola method for weight calculation
	private ArrayList<Weight> findNoteWeightsLocal(ArrayList<RMPoint> path, 
													double[] chordWeights, 
													int causalD, int finalD) {
		
		int numNotes = AbstractRiemannMatrix.countNotes(matrices);
		ArrayList<Weight> noteWeights = new ArrayList<Weight>(numNotes);
		
		if (DEBUG) System.out.println("NoteWeights Local:");
		
		for (int ch=0; ch<matrices.size(); ch++) {
			
			if (DEBUG) System.out.println("ch = " + ch);
			Chord chord = matrices.get(ch).chord;
			
			for (int n=0; n<chord.notes.size(); n++) {
				if (DEBUG) System.out.println("n = " + n);
				Chord newChord = new Chord(chord.duration, chord.onset, chord.notes);	
				newChord.removeNote(n);
				
				// Prepare and add the new matrix
				AbstractRiemannMatrix newMatrix = new AbstractRiemannMatrix(null, tons.length, modes.length, funs.length);
				newMatrix.setChord(newChord);
				
				double newChordWeight;
				// if newChord has no note at all, do not compute newMatrix
				if (newChord.notes.size() != 0) {	
					// find the chain-of-thirds of the new chord
					if (chordMethod == CHAIN_OF_THIRDS) {
						chain3rd = newChord.findChainsOfThirds(chainRepository);						
						// compute new matrix data with respect to the chain-of-thirds
						newMatrix.compute3rdChain(chain3rd, weights, tons.length, modes.length, funs.length);
					} else if (chordMethod == DIRECT) {
						newMatrix.computeDirect(weights, tons.length, modes.length, funs.length);							
					} else {
						addError("Chord method is undefined?");
					}	
					RMPoint last_pt = path.get(ch);
					newChordWeight = chordWeights[ch] - last_pt.value + newMatrix.getValue(last_pt.row, last_pt.col);
				} else {
					newChordWeight = chordWeights[ch-1]; 
				}
				// Weight of note x is 1 / (d + (1-d)e^(w(chord_i-{x}) - w(chord_i))  (from ToM pp:822)
				double value = 1.0 /(distanceFactor + (1.0 - distanceFactor)*Math.exp(newChordWeight - chordWeights[ch]));	
				GMINote note = chord.notes.get(n);
				Weight weight = new Weight(value, note);				
				noteWeights.add(weight);
			}
		}
		return noteWeights;
	}
	
	
	// map weight values to the range [0.0, 1.0]
	public void normalize(ArrayList<Weight> weights) {
		// find max weight and min weight
		double max = max(weights);
		double min = min(weights);
		
		for (int i=0; i<weights.size(); i++) {
			weights.get(i).value = (weights.get(i).value - min)/(max - min);
		}
	}
	
	private double max(ArrayList<Weight> weights) {
		double max = weights.get(0).value;		
		for (int i=1; i<weights.size(); i++) {
			if (weights.get(i).value > max)
				max = weights.get(i).value;
		}
		return max;
	}
	
	private double min(ArrayList<Weight> weights) {
		double min = weights.get(0).value;		
		for (int i=1; i<weights.size(); i++) {
			if (weights.get(i).value < min)
				min = weights.get(i).value;
		}
		return min;
	}

	// the 2nd output of the rubette
	private Denotator makeWeightSequence(ArrayList<Weight> weights) {
		
		LinkedList<Denotator> allhWeights = new LinkedList<Denotator>();
		
		for (Weight w : weights) {
			Denotator value = makeDenotator(weightValForm, w.value);
			Denotator onset = makeDenotator(onsetForm, w.note.onset);	
			Denotator pitch = makeDenotator(pitchForm, w.note.pitch);
			Denotator loudness = makeDenotator(loudnessForm, w.note.loudness);
			Denotator duration = makeDenotator(durationForm, w.note.duration);
			Denotator voice = makeDenotator(voiceForm, w.note.voice);
			LinkedList<Denotator> factors = new LinkedList<Denotator>();
			factors.add(onset);
			factors.add(pitch);
			factors.add(loudness);
			factors.add(duration);
			factors.add(voice);
			factors.add(value);
			Denotator harmonicWeight = makeDenotator(NoteAndWeightForm, factors);	
			allhWeights.add(harmonicWeight);
		}
		Denotator harmonicWeightSeq = makeDenotator(WeightSeqForm, allhWeights);
		return harmonicWeightSeq;
	}
}