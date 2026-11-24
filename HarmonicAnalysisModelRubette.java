/* Copyright (C) 2012-13 Ruhan Ikeda
 *
 * Bug fixes and other improvements: Jan 2016
 * (3 more improvements to do, are on the notebook (1 Subat 2016 page)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 */

/* 
 * Fixes: 2018-2019:
 * 
 * GUI is updated. 
 * Properties can not be loaded/saved. FIX this.
 * 
 */

package org.rubato.rubettes.score;

import static org.rubato.logeo.DenoFactory.makeDenotator;
import static org.rubato.xml.XMLConstants.VALUE_ATTR;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.LinkedList;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.rubato.base.AbstractRubette;
import org.rubato.base.Repository;
import org.rubato.base.Rubette;
import org.rubato.composer.RunInfo;
import org.rubato.composer.icons.Icons;
import org.rubato.logeo.FormFactory;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.Form;
import org.rubato.math.yoneda.LimitForm;
import org.rubato.math.yoneda.PowerForm;
import org.rubato.math.yoneda.SimpleForm;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;

import com.sun.glass.events.KeyEvent;

/* 
 * Takes in scale, tonality, function, mode specifications from the user
 * Outputs this model as a denotator. 
 * 
 * JAbstractRiemannMatrix handles GUI of the weights-of-thirds table 
 * AbstractRiemannMatrix holds matrix data and does computations on the data of the table:
 * 
 * .-----------------------.
 * | HarmonicAnalysisModel | 
 * |-----------------------|
 * | JAbstractRiemannMatrix| 
 * |-----------------------|
 * | AbstractRiemannMatrix | 
 * '-----------------------'
 * 
 * 
 * REMARK: If pitch systems with microtonal sounds are also considered, 
 * then MidiFileInRubette should also take this rubette as input.
 *
 */



public class HarmonicAnalysisModelRubette extends AbstractRubette
		implements ActionListener, ChangeListener {

	private static final ImageIcon icon = Icons.loadIcon(
			HarmonicAnalysisModelRubette.class, "harmonicanalysismodel.png");

	private String pitchSystem[] = { "12-tone equal tempered", };
	private int equalTempered[] = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
	
	private String modes[] = { "Major,Minor", "All 7 Greek Modes" };
	private int majorMinor[] = { 0, 5 };
	private int greekModes[] = { 0, 1, 2, 3, 4, 5, 6 };
	
	private String functions[] = { "T,S,D", "All 7 Degrees" };
	private int riemannFunctions[] = { 0, 5, 7 };
	private int diatonicFunctions[] = { 0, 1, 2, 3, 4, 5, 6 };
	
	/* The most recent selection by the user, else the defaults */
	private int[] selPitchSystem = equalTempered;
	private int[] selModes = majorMinor;
	private int[] selFunctions = riemannFunctions;
	private int p=0,m=0,f=0;

	// View dialog
	private JPanel view = null;
	private JTabbedPane tPane = null;
	JComponent panel1, panel2, panel3;
	private JAbstractRiemannMatrixDisplay rMatrixDisplay = null;
	private JBooleanMatrix locusDisplay = null;

	protected Denotator HarmonicAnalysisModel = null;

	private JPanel properties = null;

	public final double[][] FixedWeightOfThirds = {
			// "Major, minor" x "Tonic, Subdominant, Dominant"
				  	  { 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 
						1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 
						0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 
						1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 
						1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0,
						0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1 }, 

			// "Major, minor" x "All diatonic functions(T,ST,M,S,D,SM,LT)"
				  	{ 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, // T
					1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, // ST
					0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, // M
					1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, // S
					0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, // D
					1, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, // SM
					0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 1, // LT

					// Minor
					1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, // T
					1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0, // ST
					0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1, // M
					1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, // S
					0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, // D
					1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, // SM
					0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0 },// LT

			// "All 7 Greek modes" x "Tonic, Dominant, Subdominant"
			{ 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0,
					0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0,
					0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 1, 1, 0,
					0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0,
					0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1,
					0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0,
					1, 0, 0, 1, 0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0,
					0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1,
					0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 1, 0,
					0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0,
					1, 1, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0,
					0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0,
					0, 0, 1, 1, 0, 0, 0, 1, 0, 0 },

			// "All 7 Greek modes" x "All diatonic functions(T,ST,M,S,D,SM,LT)"
			{ 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0,
					0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0,
					0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 1, 1, 0,
					0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0,
					0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1,
					0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0,
					1, 0, 0, 1, 0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0,
					0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1,
					0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 1, 0,
					0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0,
					1, 1, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0,
					0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0,
					0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0,
					0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1,
					0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0,
					1, 0, 0, 1, 0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0,
					0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1,
					0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 1, 0,
					0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0,
					1, 1, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0,
					0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0,
					0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0,
					0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 0, 1,
					0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0,
					1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1,
					0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1,
					0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 1, 0,
					0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0,
					1, 1, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0,
					0, 1, 0, 0, 0, 1 } };

	private JComboBox<String> pitchSystemSelector, modeSystemSelector, functionSystemSelector;

	// operations that can be applied to a sequence of chords
	private JComboBox<String> opSelect;
	private JComboBox<String> RopSelect;
	private JComboBox<String> LopSelect;
	private final String[] opNames = { "<Select>", "Apply", "Load", "Save"};
	private static final int SELECT = 0;
	private static final int APPLY = 1;
	private static final int LOAD = 2;
	private static final int SAVE = 3;

	
	// Tonality change table's data is fixed regardless of
	// HarmonicAnalysisModelRubette settings.
	private JAbstractMatrixDisplay tonalityDisplay;

	int[] tonColNames = { 0, 1, 2, 3, 4, 5, 6 };
	String[] tonRowNames = { "fifth", "fourth" };
	Double[][] tonData = { 	{ 0.0, 2.0, 4.0, 6.0, 8.0, 10.0, 12.0 },
							{ 0.0, 2.0, 4.0, 6.0, 8.0, 10.0, 12.0 } };

	private JAbstractMatrixDisplay modeDisplay;

	Double[][][] modeData = {
				{ { 0.0, 2.0 }, 
				  { 1.5, 0.0 } },
			
				{   { 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0 },
					{ 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0 },
					{ 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0 },
					{ 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0 },
					{ 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0 },
					{ 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0 },
					{ 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0 } } };

	private JAbstractMatrixDisplay functionDisplay;
	Double[][][] funData = {
			{ { 0.0, 1.0, 0.5 }, 
			  { -0.2, 1.0, 0.8 }, 
			  { -0.5, 0.2, 1.0 } },
			
			{   { 0.0, 1.5, 1.5, 1.5, 0.5, 1.5, 0.5 },
				{ -0.2, 1.0, 1.0, 1.0, 0.8, 1.0, 0.8 },
				{ -0.2, 1.0, 1.0, 1.0, 0.8, 1.0, 0.8 },
				{ -0.2, 1.0, 1.0, 1.0, 0.8, 1.0, 0.8 },
				{ -0.5, 0.2, 0.2, 0.2, 1.0, 1.0, 0.2 },
				{ -0.2, 0.0, 0.0, 0.0, 0.8, 0.8, 0.0 },
				{ -0.5, 0.2, 0.2, 0.2, 0.0, 0.2, 1.0 }, } };

	private String[] modeNames = null;
	private String[] funNames = null;
	
	// private boolean[][] tonsFunsInclusion;
	
	
	// July 24, 2013
	// Forms and denotators for HarmonicAnalysisModelRubette:

	private static Repository rep = Repository.systemRepository();
	// tonality takes values in Z_12
	static SimpleForm tonalityForm = FormFactory.makeZModuleForm("Tonality");
	static PowerForm tonalitiesForm = FormFactory.makePowerForm("Tonalities",
			tonalityForm);
	Form y = rep.registerBuiltin(tonalitiesForm);

	// modes takes values in Z_7
	// 0 is major (Ionian), 5 is minor (Aeolian), ...
	static SimpleForm modeForm = FormFactory.makeZModuleForm("Mode");
	PowerForm modesForm = FormFactory.makePowerForm("Modes", modeForm);
	Form z = rep.registerBuiltin(modesForm);

	// function takes values in Z_7
	// 0 is tonic, 1 is subtonic, 2 is median, 3 is subdominant, 4 is dominant,
	// ...
	static SimpleForm functionForm = FormFactory.makeZModuleForm("Function");
	PowerForm functionsForm = FormFactory.makePowerForm("Functions",
			functionForm);
	Form w = rep.registerBuiltin(functionsForm);

	// Weight-of-thirds table values
	// Note that size of the table can be retrieved from other denotators
	// (tonality, mode, function)
	static SimpleForm sizeForm = FormFactory.makeZModuleForm("Size");  // to store row and column sizes of a table
	
	List<Form> weightList = new LinkedList<Form>();
	static SimpleForm weightForm = FormFactory.makeRModuleForm("Weight");
	static SimpleForm rowForm = FormFactory.makeZModuleForm("Row");
	static SimpleForm colForm = FormFactory.makeZModuleForm("Column");
	Boolean w1 = weightList.add(rowForm);
	Boolean w2 = weightList.add(colForm);
	Boolean w3 = weightList.add(weightForm);
	LimitForm weightOfThirdForm = FormFactory.makeLimitForm("WeightOfThird", weightList);
	PowerForm weightOfThirdsForm = FormFactory.makePowerForm("WeightOfThirds", weightOfThirdForm);
	Form u = rep.registerBuiltin(weightOfThirdsForm);
	
	// to add tension tables to the output denotator
	
	// tension form
	List<Form> tensionTableEntry = new LinkedList<Form>();	
    static SimpleForm tensVal = FormFactory.makeRModuleForm("TensionVal");
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
	static SimpleForm inclusionVal = FormFactory.makeZModuleForm("Inclusion");
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


	
	// constructor
	public HarmonicAnalysisModelRubette() {
		setInCount(0);
		setOutCount(1);
	}

	
	
	// has a properties dialog
	public boolean hasProperties() {
		return true;
	}

	
	
	/*
	 * Should have both getProperties() and applyProperties()(non-Javadoc)
	 * 
	 * @see org.rubato.base.SimpleAbstractRubette#getProperties() Ref: Page 275
	 * on Milmeister's book
	 */
	public JComponent getProperties() {
		if (properties == null) {

			properties = new JPanel();
			properties.setLayout(new BoxLayout(properties, BoxLayout.Y_AXIS));

			JPanel psPane = new JPanel();
			psPane.setLayout(new BoxLayout(psPane, BoxLayout.X_AXIS));
			JLabel psLabel = new JLabel("Pitch System: ");
			psPane.add(psLabel);
			pitchSystemSelector = new JComboBox<String>();
			pitchSystemSelector.setEditable(false);
			pitchSystemSelector.addActionListener(this);
			for (int i = 0; i < pitchSystem.length; i++) {
				pitchSystemSelector.addItem(pitchSystem[i]);
			}
			psPane.add(pitchSystemSelector);
			pitchSystemSelector.setSelectedIndex(p);
			properties.add(psPane);

			JPanel msPane = new JPanel();
			msPane.setLayout(new BoxLayout(msPane, BoxLayout.X_AXIS));
			JLabel msLabel = new JLabel("Modes:         ");
			msPane.add(msLabel);
			modeSystemSelector = new JComboBox<String>();
			modeSystemSelector.setEditable(false);
			modeSystemSelector.addActionListener(this);
			for (int i = 0; i < modes.length; i++) {
				modeSystemSelector.addItem(modes[i]);
			}
			msPane.add(modeSystemSelector);
			modeSystemSelector.setSelectedIndex(m);
			properties.add(msPane);

			JPanel fsPane = new JPanel();
			fsPane.setLayout(new BoxLayout(fsPane, BoxLayout.X_AXIS));
			JLabel fsLabel = new JLabel("Functions:   ");
			fsPane.add(fsLabel);
			functionSystemSelector = new JComboBox<String>();
			functionSystemSelector.setEditable(false);
			functionSystemSelector.addActionListener(this);
			for (int i = 0; i < functions.length; i++) {
				functionSystemSelector.addItem(functions[i]);
			}
			fsPane.add(functionSystemSelector);
			functionSystemSelector.setSelectedIndex(f);
			properties.add(fsPane);
		}
		return properties;
	}

	
	
	/*
	 * Whatever is selected through the Selection Boxes takes its effect here
	 * (non-Javadoc)
	 * 
	 * @see org.rubato.base.SimpleAbstractRubette#applyProperties()
	 */
	public boolean applyProperties() {
		p = pitchSystemSelector.getSelectedIndex();
		if (p == 0)
			selPitchSystem = equalTempered;

		m = modeSystemSelector.getSelectedIndex();
		if (m == 0)
			selModes = majorMinor;
		else if (m == 1)
			selModes = greekModes;

		f = functionSystemSelector.getSelectedIndex();
		if (f == 0)
			selFunctions = riemannFunctions;
		else if (f == 1)
			selFunctions = diatonicFunctions;

		// If view is open, needs to be redrawn as well.
		if (view != null) {
			
			tPane.removeAll();
			
			rMatrixDisplay = new JAbstractRiemannMatrixDisplay(selPitchSystem,
					selModes, selFunctions, true);
			setMatrixValues(selPitchSystem, selModes, selFunctions);
			
			panel1 = makeTablePanel(rMatrixDisplay);
			tPane.addTab("Riemann Logic", icon, panel1,	"");

			panel1.repaint();
	        panel1.updateUI();

	        tonalityDisplay = new JAbstractMatrixDisplay(tonColNames, tonRowNames, tonData);
	        
	        if (selModes == majorMinor) {
				modeNames = new String[majorMinor.length];
				for (int r = 0; r < majorMinor.length; r++)
					modeNames[r] = Integer.toString(majorMinor[r]);
				modeDisplay = new JAbstractMatrixDisplay(majorMinor, modeNames,
						modeData[0]);
			} else {
				modeNames = new String[greekModes.length];
				for (int r = 0; r < greekModes.length; r++)
					modeNames[r] = Integer.toString(greekModes[r]);
				modeDisplay = new JAbstractMatrixDisplay(greekModes, modeNames,
						modeData[1]);
			}
	        
			if (selFunctions == riemannFunctions) {
				funNames = new String[riemannFunctions.length];
				for (int r = 0; r < riemannFunctions.length; r++)
					funNames[r] = Integer.toString(riemannFunctions[r]);
				functionDisplay = new JAbstractMatrixDisplay(riemannFunctions,
						funNames, funData[0]);
			} else {
				funNames = new String[diatonicFunctions.length];
				for (int r = 0; r < diatonicFunctions.length; r++)
					funNames[r] = Integer.toString(diatonicFunctions[r]);
				functionDisplay = new JAbstractMatrixDisplay(diatonicFunctions,
						funNames, funData[1]);
			}

			panel2 = makeTableSeqPanel(tonalityDisplay, modeDisplay, functionDisplay);
	   		opSelect = new JComboBox<String>();
    		opSelect.setEditable(false);
    		opSelect.addActionListener(this);
    		for (int j=0; j<opNames.length; j++) {
    			opSelect.addItem(opNames[j]);
    		}
    		opSelect.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Operation"));
    		panel2.add(opSelect);			
	        tPane.addTab("Harmonic Tensor", icon, panel2, "");	        
	        panel2.repaint();
	        panel2.updateUI();
	        	        	        
	        locusDisplay = new JBooleanMatrix(selPitchSystem, selModes, selFunctions);
	        locusDisplay.model.reset();
	        panel3 = makeTablePanel(locusDisplay);
			// APPLY/LOAD/SAVE
    		LopSelect = new JComboBox<String>();
    		LopSelect.setEditable(false);
    		LopSelect.addActionListener(this);
    		for (int j=0; j<opNames.length; j++) {
    			LopSelect.addItem(opNames[j]);
    		}
    		LopSelect.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Operation"));
    		panel3.add(LopSelect);						
	        tPane.addTab("Locus Inclusion", icon, panel3, "");

	        panel3.repaint();
	        panel3.updateUI();

	        //The following line enables to use scrolling tabs.
	        tPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		}
		return true;
	}

	
	public boolean hasView() {
		return true;
	}

	
	public void init() { }
	
	
	public JComponent getView() {

		if (view == null) {

			view = new JPanel();
            view.setLayout(new BorderLayout());           
			tPane = new JTabbedPane();						
			// Riemann Logic Pane
			rMatrixDisplay = new JAbstractRiemannMatrixDisplay(selPitchSystem,
					selModes, selFunctions, true);
			setMatrixValues(selPitchSystem, selModes, selFunctions);
			panel1 = makeTablePanel(rMatrixDisplay);
			// APPLY/LOAD/SAVE
    		RopSelect = new JComboBox<String>();
    		RopSelect.setEditable(false);
    		RopSelect.addActionListener(this);
    		for (int j=0; j<opNames.length; j++) {
    			RopSelect.addItem(opNames[j]);
    		}
    		RopSelect.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Operation"));
    		panel1.add(RopSelect);
						
	        tPane.addTab("Riemann Logic", icon, panel1, "");
	        tPane.setMnemonicAt(0, KeyEvent.VK_1);
	        
	        
	        // Harmonic Tensor Pane
			// Tonality tension values
	        tonalityDisplay = new JAbstractMatrixDisplay(tonColNames, tonRowNames, tonData);
	        
	        if (selModes == majorMinor) {
				modeNames = new String[majorMinor.length];
				for (int r = 0; r < majorMinor.length; r++)
					modeNames[r] = Integer.toString(majorMinor[r]);
				modeDisplay = new JAbstractMatrixDisplay(majorMinor, modeNames,
						modeData[0]);
			} else {
				modeNames = new String[greekModes.length];
				for (int r = 0; r < greekModes.length; r++)
					modeNames[r] = Integer.toString(greekModes[r]);
				modeDisplay = new JAbstractMatrixDisplay(greekModes, modeNames,
						modeData[1]);
			}
	        
			if (selFunctions == riemannFunctions) {
				funNames = new String[riemannFunctions.length];
				for (int r = 0; r < riemannFunctions.length; r++)
					funNames[r] = Integer.toString(riemannFunctions[r]);
				functionDisplay = new JAbstractMatrixDisplay(riemannFunctions,
						funNames, funData[0]);
			} else {
				funNames = new String[diatonicFunctions.length];
				for (int r = 0; r < diatonicFunctions.length; r++)
					funNames[r] = Integer.toString(diatonicFunctions[r]);
				functionDisplay = new JAbstractMatrixDisplay(diatonicFunctions,
						funNames, funData[1]);
			}

			JComponent panel2 = makeTableSeqPanel(tonalityDisplay, modeDisplay, functionDisplay);
			
    		opSelect = new JComboBox<String>();
    		opSelect.setEditable(false);
    		opSelect.addActionListener(this);
    		for (int j=0; j<opNames.length; j++) {
    			opSelect.addItem(opNames[j]);
    		}
    		opSelect.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Operation"));
    		panel2.add(opSelect);			
	        tPane.addTab("Harmonic Tensor", icon, panel2,"");
	        tPane.setMnemonicAt(1, KeyEvent.VK_2);	        
	        
	        locusDisplay = new JBooleanMatrix(selPitchSystem, selModes, selFunctions);
	        locusDisplay.model.reset();
	        panel3 = makeTablePanel(locusDisplay);
			// APPLY/LOAD/SAVE
    		LopSelect = new JComboBox<String>();
    		LopSelect.setEditable(false);
    		LopSelect.addActionListener(this);
    		for (int j=0; j<opNames.length; j++) {
    			LopSelect.addItem(opNames[j]);
    		}
    		LopSelect.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Operation"));
    		panel3.add(LopSelect);						
	        tPane.addTab("Locus Inclusion", icon, panel3,"");
	        tPane.setMnemonicAt(2, KeyEvent.VK_3);
	        
	        //The following line enables to use scrolling tabs.
	        tPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);	        
	        view.add(tPane);
		}		
		return view;
	}
	
	

	// Make a table panel for double (or integer) matrix
	protected JComponent makeTablePanel(JAbstractRiemannMatrixDisplay matrix) {
        JPanel panel = new JPanel(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JScrollPane riemannMatrixPane = new JScrollPane(matrix.table);   
        panel.setPreferredSize(new Dimension(600,140));
        panel.add(riemannMatrixPane);
        return panel;
    }
	
	
	// Make a table panel for boolen matrix
	protected JComponent makeTablePanel(JBooleanMatrix matrix) {
        JPanel panel = new JPanel(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JScrollPane riemannMatrixPane = new JScrollPane(matrix.table);   
        panel.setPreferredSize(new Dimension(600,140));
        panel.add(riemannMatrixPane);
        return panel;
    }
	
	
	
	protected JComponent makeTableSeqPanel(JAbstractMatrixDisplay matrix1,
										JAbstractMatrixDisplay matrix2,
										JAbstractMatrixDisplay matrix3) {
		
        JPanel panel = new JPanel(false);  
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        JScrollPane tonalityPane = new JScrollPane(matrix1.table);
        matrix1.table.setPreferredScrollableViewportSize(matrix1.table
                        .getPreferredSize());    
        panel.add(tonalityPane);
        
        JScrollPane modePane = new JScrollPane(matrix2.table);
        matrix2.table.setPreferredScrollableViewportSize(matrix2.table
                .getPreferredSize());  
        panel.add(modePane);
        
        JScrollPane funcPane = new JScrollPane(matrix3.table);
        matrix3.table.setPreferredScrollableViewportSize(matrix3.table
                .getPreferredSize());  
        panel.add(funcPane);
        
        return panel;
    }
    

	
	/*
	 * set the data of the weight-of-thirds table
	 * 
	 * this is not good! makes things spagettish
	 */
	private void setMatrixValues(int[] tons, int[] modes, int[] funs) {
		if (tons == equalTempered) {
			if (modes == majorMinor) {
				if (funs == riemannFunctions)
					rMatrixDisplay.model.reset(FixedWeightOfThirds[0]);
				else if (funs == diatonicFunctions)
					rMatrixDisplay.model.reset(FixedWeightOfThirds[1]);
			} else if (modes == greekModes) {
				if (funs == riemannFunctions)
					rMatrixDisplay.model.reset(FixedWeightOfThirds[2]);
				else if (funs == diatonicFunctions)
					rMatrixDisplay.model.reset(FixedWeightOfThirds[3]);
			}
		}
	}
	
	
	public String getGroup() {
		return "Score";
	}

	public String getName() {
		return "HarmonicAnalysisModel";
	}

	public String getShortDescription() {
		return "Harmonic Analysis Model";
	}

	public ImageIcon getIcon() {
		return icon;
	}

	public String getLongDescription() {
		return "The Harmonic Analysis Model is to specify tonalities, "
				+ "modes and functions for analysis, i.e. the harmonic analysis space";
	}
	
	/*
	private String pitchSystem[] = { "12-tone equal tempered", };
	private int equalTempered[] = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
	
	private String modes[] = { "Major,Minor", "All 7 Greek Modes" };
	private int majorMinor[] = { 0, 5 };
	private int greekModes[] = { 0, 1, 2, 3, 4, 5, 6 };
	
	private String functions[] = { "T,S,D", "All 7 Degrees" };
	private int riemannFunctions[] = { 0, 5, 7 };
	private int diatonicFunctions[] = { 0, 1, 2, 3, 4, 5, 6 };
	
	private int[] selPitchSystem = equalTempered;
	private int[] selModes = majorMinor;
	private int[] selFunctions = riemannFunctions;
	int p,m,f;
	*/

	private final static String PITCH_SYSTEM = "pitch-system";
	private final static String MODE = "mode";
	private final static String FUNCTION = "function";
		
	public void toXML(XMLWriter writer) {
         writer.empty(PITCH_SYSTEM, VALUE_ATTR, pitchSystem[p]);
         writer.empty(MODE, VALUE_ATTR, modes[m]);
		 writer.empty(FUNCTION, VALUE_ATTR, functions[f]);         
 	}

	public Rubette fromXML(XMLReader reader, Element element) {
		
	 	Element child = XMLReader.getChild(element, PITCH_SYSTEM);
	 	String val = child.getAttribute(VALUE_ATTR);
	 	if (val.equals("12-tone equal tempered")) {
	 		p = 0;
	 	}

	 	child = XMLReader.getChild(element, MODE);
	 	val = child.getAttribute(VALUE_ATTR);
	 	if (val.equals("Major,Minor")) {
	 		m = 0;
	 	} else if (val.equals("All 7 Greek Modes")) {
	 		m = 1;
	 	}
	 	
	 	child = XMLReader.getChild(element, FUNCTION);
	 	val = child.getAttribute(VALUE_ATTR);
	 	if (val.equals("T,S,D")) {
	 		f = 0;
	 	} else if (val.equals("All 7 Degrees")) {
	 		f = 1;
	 	}

	 	HarmonicAnalysisModelRubette rubette = new HarmonicAnalysisModelRubette();	 
	 	rubette.p = p;
	 	rubette.m = m;
	 	rubette.f = f;
	 	return rubette;	 		 	
	}
	
	
	
	/* 
	 * Prepares the output denotator
	 */
	public void run(RunInfo runInfo) {

		HarmonicAnalysisModel = setAnalysisModelDenotator(selPitchSystem, selModes, selFunctions);
		setOutput(0, HarmonicAnalysisModel);
	}

	
	
	/* 
	 * Form the denotator output of the rubette
	 */
	private Denotator setAnalysisModelDenotator(int[] tons, 
												int[] modes,
												int[] funs) {

		// tonalities
		LinkedList<Denotator> tonalityList = new LinkedList<Denotator>();
		for (int n = 0; n < tons.length; n++) {
			Denotator t = makeTonality(tons[n]);
			tonalityList.add(t);
		}
		Denotator t = makeDenotator(tonalitiesForm, tonalityList);

		// modes
		LinkedList<Denotator> modeList = new LinkedList<Denotator>();
		for (int n = 0; n < modes.length; n++) {
			modeList.add(makeMode(modes[n]));
		}
		Denotator m = makeDenotator(modesForm, modeList);

		// functions
		LinkedList<Denotator> functionList = new LinkedList<Denotator>();
		for (int n = 0; n < funs.length; n++) {
			functionList.add(makeFunction(funs[n]));
		}
		Denotator f = makeDenotator(functionsForm, functionList);

		// add the weight of thirds table
		LinkedList<Denotator> weightList = new LinkedList<Denotator>();

		int rowCount = modes.length*funs.length;
		int colCount = tons.length;
		Double v;

		for (int r = 0; r < rowCount; r++) {
			for (int c = 0; c < colCount; c++) {
				// System.out.print("r = "+r+" c = "+c);
 				if (view == null) {
 					int cm = comb(tons, modes, funs);
					v = FixedWeightOfThirds[cm][r*colCount+c];
 				} else {
					v = rMatrixDisplay.model.getDoubleValueAt(r,c+1);
 				}
				// System.out.println(" val = "+v);
				LinkedList<Denotator> wList = new LinkedList<Denotator>();
				Denotator ro = makeRow(r);
				wList.add(ro);
				Denotator co = makeColumn(c);
				wList.add(co);
				Denotator d = makeWeight(v);
				wList.add(d);
				Denotator w = makeDenotator(weightOfThirdForm, wList);
				weightList.add(w);
			}
		}
		Denotator w = makeDenotator(weightOfThirdsForm, weightList);

		
		// tonalities tension
		LinkedList<Denotator> TonalitiesSizeList = new LinkedList<Denotator>();
		// add row size and column size
		Denotator dimNum = makeSize(2);
		TonalitiesSizeList.add(dimNum);
		Denotator rowSize = makeSize(tonRowNames.length);
		TonalitiesSizeList.add(rowSize);
		Denotator colSize = makeSize(tonColNames.length);
		TonalitiesSizeList.add(colSize);
		
		LinkedList<Denotator> TonalitiesTensionList = new LinkedList<Denotator>();
		for (int r=0; r < tonRowNames.length; r++) {
			for (int c=0; c < tonColNames.length; c++) {
				double val = tonData[r][c];
				LinkedList<Denotator> tList = new LinkedList<Denotator>();
				Denotator ro = makeRow(r);
				tList.add(ro);
				Denotator co = makeColumn(c);
				tList.add(co);
				Denotator d = makeTension(val);
				tList.add(d);
				Denotator tf = makeDenotator(tensionTableCell, tList);
				TonalitiesTensionList.add(tf);
			}
		}	
		Denotator t2 = makeDenotator(tensionTable, TonalitiesTensionList);	
		TonalitiesSizeList.add(t2);
		Denotator tff = makeDenotator(tensionSizeAndTable, TonalitiesSizeList);

		
		// modes tension
		int mIndex = (selModes == majorMinor ? 0 : 1);		
		LinkedList<Denotator> ModesSizeList = new LinkedList<Denotator>();
		// add row size and column size
		dimNum = makeSize(2);
		ModesSizeList.add(dimNum);
		rowSize = makeSize(modes.length);
		ModesSizeList.add(rowSize);
		colSize = makeSize(modes.length);
		ModesSizeList.add(colSize);
		
		LinkedList<Denotator> ModesTensionList = new LinkedList<Denotator>();
		for (int r=0; r < modes.length; r++) {
			for (int c=0; c < modes.length; c++) {
				double val = modeData[mIndex][r][c];
				LinkedList<Denotator> mList = new LinkedList<Denotator>();
				Denotator ro = makeRow(r);
				mList.add(ro);
				Denotator co = makeColumn(c);
				mList.add(co);
				Denotator d = makeTension(val);
				mList.add(d);
				Denotator mf = makeDenotator(tensionTableCell, mList);
				ModesTensionList.add(mf);
			}
		}
		Denotator t3 = makeDenotator(tensionTable, ModesTensionList);	
		ModesSizeList.add(t3);
		Denotator mff = makeDenotator(tensionSizeAndTable, ModesSizeList);		
		
		
		// function tension
		int fIndex = (selFunctions == riemannFunctions ? 0 : 1);
		LinkedList<Denotator> FunSizeList = new LinkedList<Denotator>();
		// add row size and column size
		dimNum = makeSize(2);
		FunSizeList.add(dimNum);
		rowSize = makeSize(funs.length);
		FunSizeList.add(rowSize);
		colSize = makeSize(funs.length);
		FunSizeList.add(colSize);
		
		LinkedList<Denotator> FunctionsTensionList = new LinkedList<Denotator>();
		for (int r=0; r < funs.length; r++) {
			for (int c=0; c < funs.length; c++) {
				double val = funData[fIndex][r][c];
				LinkedList<Denotator> fList = new LinkedList<Denotator>();
				Denotator ro = makeRow(r);
				fList.add(ro);
				Denotator co = makeColumn(c);
				fList.add(co);
				Denotator d = makeTension(val);
				fList.add(d);
				Denotator ff = makeDenotator(tensionTableCell, fList);
				FunctionsTensionList.add(ff);
			}
		}
		Denotator t4 = makeDenotator(tensionTable, FunctionsTensionList);	
		FunSizeList.add(t4);		
		Denotator fff = makeDenotator(tensionSizeAndTable, FunSizeList);	
		
		
		// Inclusion table
		LinkedList<Denotator> IncSizeList = new LinkedList<Denotator>();
		// add row size and column size
		dimNum = makeSize(2);
		IncSizeList.add(dimNum);
		rowSize = makeSize(rowCount);
		IncSizeList.add(rowSize);
		colSize = makeSize(colCount);
		IncSizeList.add(colSize);

				
		LinkedList<Denotator> incList = new LinkedList<Denotator>();
		boolean cell;
		for (int r = 0; r < rowCount; r++) {
			for (int c = 1; c < colCount+1; c++) {
				if (view == null) {
					cell = true;
 				} else {
					String str = String.valueOf(locusDisplay.model.getValueAt(r,c));
					cell = Boolean.parseBoolean(str);
 				}
				// System.out.println("cell = "+cell);
				LinkedList<Denotator> iList = new LinkedList<Denotator>();
				Denotator ro = makeRow(r);
				iList.add(ro);
				Denotator co = makeColumn(c-1);
				iList.add(co);
				Denotator d = makeInclusion(cell);
				iList.add(d);
				Denotator i = makeDenotator(inclusionTableCell, iList);
				incList.add(i);
			}
		}
		Denotator inc = makeDenotator(inclusionTableForm, incList);
		IncSizeList.add(inc);		
		Denotator incl = makeDenotator(incSizeAndTable, IncSizeList);	
		
						
		LinkedList<Denotator> analysisList = new LinkedList<Denotator>();
		analysisList.add(t);
		analysisList.add(m);
		analysisList.add(f);
		analysisList.add(w);
		analysisList.add(tff);
		analysisList.add(mff);
		analysisList.add(fff);
		analysisList.add(incl);
		Denotator aSpec = makeDenotator(analysisSpecForm, analysisList);
		return aSpec;
	}

	
	private int comb(int[] tons, int[] modes, int[] funs) {
        if (tons == equalTempered)
                if (modes == majorMinor) {
                        if (funs == riemannFunctions)
                                return (0);
                        else if (funs == diatonicFunctions)
                                return (1);
                } else if (modes == greekModes) {
                        if (funs == riemannFunctions)
                                return (2);
                        else if (funs == diatonicFunctions)
                                return (3);
                }
        return -1;
	}
	
	
	static Denotator makeTonality(int t) {
		Denotator d = makeDenotator(tonalityForm, t);
		return d;
	}

	static Denotator makeMode(int t) {
		Denotator d = makeDenotator(modeForm, t);
		return d;
	}

	static Denotator makeFunction(int t) {
		Denotator d = makeDenotator(functionForm, t);
		return d;
	}

	static Denotator makeWeight(double val) {
		Denotator d = makeDenotator(weightForm, val);
		return d;
	}

	static Denotator makeInclusion(boolean val) {
		
		Denotator d;
		
		if (val == true)
			d = makeDenotator(inclusionVal, 1);
		else
			d = makeDenotator(inclusionVal, 0);
		return d;
	}
	
	static Denotator makeRow(int t) {
		Denotator d = makeDenotator(rowForm, t);
		return d;
	}

	static Denotator makeColumn(int t) {
		Denotator d = makeDenotator(colForm, t);
		return d;
	}

	static Denotator makeSize(int t) {
		Denotator d = makeDenotator(sizeForm, t);
		return d;
	}
	
	
	static Denotator makeTension(double t) {
		Denotator d = makeDenotator(tensVal, t);
		return d;
	}
   
		

	public void stateChanged(ChangeEvent arg0) {
		// TODO Auto-generated method stub
	}

	
	
	// LOAD/SAVE/APPLY buttons of harmonic distance parameters
	public void actionPerformed(ActionEvent e) {

		Object src = e.getSource();

		if (src == opSelect) {
			int opCode = ((JComboBox<?>) src).getSelectedIndex();
			switch (opCode) {
			case APPLY:
				// write table values to 
				// tonData[], funData[], modeData[]
				tonalityDisplay.printToArray(tonData);	
				modeDisplay.printToArray(modeData[m]);
				functionDisplay.printToArray(funData[f]);
				opSelect.setSelectedIndex(SELECT);
				break;
			case LOAD:
				// Popup a file load dialog
				JFileChooser fcl = new JFileChooser();
				int returnVal2 = fcl.showOpenDialog(this.view);
				if (returnVal2 == JFileChooser.APPROVE_OPTION) {
					// load the selected XML file to table
					File file = fcl.getSelectedFile();
					BufferedReader in = null;
					try {
						in = new BufferedReader(new FileReader(file.getName()));
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					}
					// rMatrixDisplay.readBuffer(in);
					tonalityDisplay.readBuffer(in);
					modeDisplay.readBuffer(in);
					functionDisplay.readBuffer(in);
				}
				opSelect.setSelectedIndex(SELECT);
				break;
			case SAVE:
				// Popup a file save dialog
				// Save the chord sequence in the specified txt file
				JFileChooser fc = new JFileChooser();
				int returnVal = fc.showSaveDialog(this.view);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					try {
						// now save the table in XML format
						FileWriter fstream = new FileWriter(file.getName());
						BufferedWriter out = new BufferedWriter(fstream);
					
						// rMatrixDisplay.printToBuffer(out);
						tonalityDisplay.printToBuffer(out);
						modeDisplay.printToBuffer(out);
						functionDisplay.printToBuffer(out);
						out.flush();
						out.close();

					} catch (IOException ioExc) {
						System.out.println("File" + file.getName()
								+ "could not be written to.");
					}
				}
				opSelect.setSelectedIndex(SELECT);
				break;
			default:
				break;
			}
		} else if (src == RopSelect) {
			int opCode = ((JComboBox<?>) src).getSelectedIndex();
			switch (opCode) {
			case APPLY:
				break;
			case LOAD:
				// Popup a file load dialog
				JFileChooser fcl = new JFileChooser();
				int returnVal2 = fcl.showOpenDialog(this.view);
				if (returnVal2 == JFileChooser.APPROVE_OPTION) {
					// load the selected XML file to table
					File file = fcl.getSelectedFile();
					BufferedReader in = null;
					try {
						in = new BufferedReader(new FileReader(file.getName()));
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					}
					rMatrixDisplay.readBuffer(in);
				}
				opSelect.setSelectedIndex(SELECT);
				break;
			case SAVE:
				// Popup a file save dialog
				// Save the chord sequence in the specified txt file
				JFileChooser fc = new JFileChooser();
				int returnVal = fc.showSaveDialog(this.view);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					try {
						// now save the table in XML format
						FileWriter fstream = new FileWriter(file.getName());
						BufferedWriter out = new BufferedWriter(fstream);
					
						rMatrixDisplay.printToBuffer(out);
						
						out.flush();
						out.close();

					} catch (IOException ioExc) {
						System.out.println("File" + file.getName()
								+ "could not be written to.");
					}
				}
				opSelect.setSelectedIndex(SELECT);
				break;
			default:
				break;
			} 
		} else if (src == LopSelect) {
				// locus inclusion
				int opCode = ((JComboBox<?>) src).getSelectedIndex();
				switch (opCode) {
				case APPLY:
					break;
				case LOAD:
					// Popup a file load dialog
					JFileChooser fcl = new JFileChooser();
					int returnVal2 = fcl.showOpenDialog(this.view);
					if (returnVal2 == JFileChooser.APPROVE_OPTION) {
						// load the selected XML file to table
						File file = fcl.getSelectedFile();
						BufferedReader in = null;
						try {
							in = new BufferedReader(new FileReader(file.getName()));
						} catch (FileNotFoundException e1) {
							e1.printStackTrace();
						}
						locusDisplay.readBuffer(in);
					}
					opSelect.updateUI();
					break;
				case SAVE:
					// Popup a file save dialog
					// Save the chord sequence in the specified txt file
					JFileChooser fc = new JFileChooser();
					int returnVal = fc.showSaveDialog(this.view);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						try {
							// now save the table in XML format
							FileWriter fstream = new FileWriter(file.getName());
							BufferedWriter out = new BufferedWriter(fstream);						
							locusDisplay.printToBuffer(out);							
							out.flush();
							out.close();

						} catch (IOException ioExc) {
							System.out.println("File" + file.getName()
									+ "could not be written to.");
						}
					}
					opSelect.setSelectedIndex(SELECT);
					break;
				default:
					break;
				}
			}
		}



	@Override
	public Rubette duplicate() {
		// TODO Auto-generated method stub
		return null;
	}	
}

