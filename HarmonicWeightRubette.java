/* Copyright (C) 2011 Ruhan Ikeda
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
 * Updates: July 2017 (with respect to RUBATO-Harmorubette v2.7)
 * 
 * 1) direct, chain of thirds
 * 2) Riemann logic (weight of thirds) table will be in the input denotator
 * 
 * Continue this work by June 21, 2018
 * 
 */

package org.rubato.rubettes.score;

import static org.rubato.logeo.DenoFactory.makeDenotator;
import static org.rubato.xml.XMLConstants.VALUE_ATTR;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;

/*
* JAbstractRiemannMatrix handles GUI of the Riemann Matrix
* AbstractRiemannMatrix holds matrix data and does computations on the data of the table:
* 
* .-----------------------.
* | HarmonicWeightModel   | 
* |-----------------------|
* | JAbstractRiemannMatrix| 
* |-----------------------|
* | AbstractRiemannMatrix | 
* '-----------------------'
* 
*/


public final class HarmonicWeightRubette extends AbstractRubette implements
	ChangeListener, ListSelectionListener, ActionListener {

	
	private static final ImageIcon icon = Icons.loadIcon(HarmonicWeightRubette.class, "harmonicweight.png");
	
	private static Repository rep = Repository.systemRepository();	
	private static Form noteForm = rep.getForm("Note");
	public static Form chordForm = FormFactory.makePowerForm("Chord", noteForm);
	private static Form methodForm = FormFactory.makeZModuleForm("Method");
	Form x = rep.registerBuiltin(chordForm);
	
	// register chordsequence form
	public static PowerForm chordSequenceForm = FormFactory.makePowerForm("ChordSequence", chordForm);
    Form y2 = rep.registerBuiltin(chordSequenceForm);  	

 	private SimpleForm riemannRowIndexForm = FormFactory.makeZModuleForm("RiemannRowIndex");
	private SimpleForm riemannColIndexForm = FormFactory.makeZModuleForm("RiemannColIndex");	
	private SimpleForm riemannValueForm = FormFactory.makeRModuleForm("RiemannValue");
	private List<Form> riemannValueList = new LinkedList<Form>();
	Boolean y123 = riemannValueList.add(riemannRowIndexForm);
    Boolean y125 = riemannValueList.add(riemannColIndexForm);
    Boolean y127 = riemannValueList.add(riemannValueForm);
    private LimitForm riemannValuePairForm = FormFactory.makeLimitForm("RiemannValuePair", riemannValueList);
	 	
	// Riemann Matrix
    // Boolean x121 = riemannMatrixList.add(methodForm);
	private PowerForm riemannMatrixForm = FormFactory.makePowerForm("RiemannMatrix", riemannValuePairForm);
    
    // Riemann Matrix Sequence
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
	Boolean t5 = analysisList.add(tensionSizeAndTable); 			//tonal tensions
	Boolean t6 = analysisList.add(tensionSizeAndTable); 			//mode tensions
	Boolean t7 = analysisList.add(tensionSizeAndTable); 			//function tensions
	Boolean t8 = analysisList.add(incSizeAndTable);
	
	LimitForm analysisSpecForm = FormFactory.makeLimitForm("AnalysisSpec", analysisList);
	Form zz = rep.registerBuiltin(analysisSpecForm);
	

	
	protected Denotator HarmonicWeightSequence = null;
	public ArrayList<Chord> chords = null;

	private String methods[] = {"Chain of Thirds", "Direct"};
	private String selectedMethod = methods[0];
	private JComboBox<String> calculationMethodSelector;
	
	private int CHAIN_OF_THIRDS = 1;
	private int DIRECT = 2;
	
	private JPanel properties = null;
	
	// these will be filled in from the AnalysisSpecDenotator
	private int[] tons;
	private int[] modes;
	private int[] funs;
	private double[] weights;   // weight-of-thirds table 
		
	/// View dialog
	private JPanel view = null;

	private DefaultListModel<String> listModel = null;
	private DefaultListModel<String> listModel2 = null;
	private JList<String> chordList, chord;
	private JSplitPane sp = null;
	protected int selectedChords[] = null;
	private int chain3rd[][];
	private AbstractRiemannMatrix rMatrix;
	private JAbstractRiemannMatrixDisplay rMatrixDisplay;
	private JPanel riemannPanel = null;
	protected Denotator matrixSequence = null;
	private LinkedList<Chain> chainRepository = null;
	
	
	final boolean DEBUG = false;

	
	// constructor
	public HarmonicWeightRubette() {		
		setInCount(2);
        setOutCount(1);        
 	}
	
	
	public boolean hasView() {
        return true;
    }

		
	public boolean hasProperties() {
		return true;
	}
	
	
	public JComponent getProperties() {
		
		if (properties == null) {
			properties = new JPanel();
			properties.setLayout(new BoxLayout(properties, BoxLayout.Y_AXIS));

			JPanel psPane = new JPanel();
			psPane.setLayout(new BoxLayout(psPane, BoxLayout.X_AXIS));
			JLabel psLabel = new JLabel("Calculation Method: ");
			psPane.add(psLabel);
			calculationMethodSelector = new JComboBox<String>();
			calculationMethodSelector.setEditable(false);
			calculationMethodSelector.addActionListener(this);
			for (int i = 0; i < methods.length; i++) {
				calculationMethodSelector.addItem(methods[i]);
			}
			calculationMethodSelector.setSelectedItem(selectedMethod);
			psPane.add(calculationMethodSelector);
			properties.add(psPane);			
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
		
		int calculationMethodIndex = calculationMethodSelector.getSelectedIndex();
		if (calculationMethodIndex == 0)
			selectedMethod = "Chain of Thirds";
		else if (calculationMethodIndex == 1)
			selectedMethod = "Direct";		
		return true;
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see org.rubato.base.AbstractRubette#getView()
	 */
	public JComponent getView() {	        
		if (view == null) {
			view = new JPanel();
			view.setLayout(new BorderLayout());
			
			listModel = new DefaultListModel<String>();
			chordList = new JList<String>(listModel);
			chordList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			chordList.addListSelectionListener(this);
			
			listModel2 = new DefaultListModel<String>();
			chord = new JList<String>(listModel2);
			
			JScrollPane pane = new JScrollPane(chordList);
			pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);			
			pane.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Chords"));
			view.add(pane, BorderLayout.WEST);
			
			JScrollPane pane2 = new JScrollPane(chord);
			pane2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			pane2.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Minimal 3rd chains"));
			view.add(pane2, BorderLayout.EAST);
						
			sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pane, pane2);
			view.add(sp,BorderLayout.NORTH);
			
            if (chords != null) {
            	setAndViewChords(chords);
            }
    
            // Now the Riemann Matrix part
            riemannPanel = new JPanel();          
            riemannPanel.setLayout(new BoxLayout(riemannPanel, BoxLayout.X_AXIS));
            if (chords != null) {
            	if (DEBUG) 
            		System.out.println("In View ---> Size of modes = "+modes.length+" and Size of funcs = "+ funs.length);
	            rMatrixDisplay = new JAbstractRiemannMatrixDisplay(this.tons, this.modes, this.funs, false);
	            JScrollPane riemannMatrixPane = new JScrollPane(rMatrixDisplay.table);
	            riemannMatrixPane.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Riemann Matrix"));
	            rMatrixDisplay.table.setPreferredScrollableViewportSize(rMatrixDisplay.table.getPreferredSize());
	            riemannPanel.add(riemannMatrixPane);  
            }                                      
            view.add(riemannPanel, BorderLayout.CENTER);
		}	
		return view;
	}
	
	
	public void init() { }
	
	
	
	public Rubette duplicate() {
		return null;
	}

	
	
	public void run(RunInfo runInfo) {
		
		Denotator chordSeq = getInput(0);
		// check 1st input
		if (chordSeq == null) {
            addError("Input denotator #1 (chord sequnece) is null.");
            HarmonicWeightSequence = null;
            return;
        } else if (!chordSeq.hasForm(chordSequenceForm)) {
            addError("Input denotator #1 is not of form \"ChordSequence\".");
            HarmonicWeightSequence = null;
            return;
        }
				
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
        	List<Denotator> modesList = ((FactorDenotator)p).getFactors();
        	this.modes = new int[modesList.size()];      	
        	i=0;
        	for (Denotator m : modesList) {
        		this.modes[i++] = ((SimpleDenotator) m).getInteger();
        	}      	
        	// functions
        	p = (PowerDenotator) specList.get(2);
        	List<Denotator> funsList = ((FactorDenotator)p).getFactors();
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
        }
				
    	// extract chords from the denotator          
    	chords = Chord.toChordsFromDenotator(chordSeq);
    	if (selectedMethod.equals("Chain of Thirds")) {
    		matrixSequence = computeRiemannMatrixSequence(chords, true);
    	} else {
    		matrixSequence = computeRiemannMatrixSequence(chords, false);
    	}
       	// the code above will switch to different branches
    	// based on Riemann Matrix Computation method selections 
    	// by the user in the properties window of this Rubette

    	
    	setAndViewChords(chords);  
    	
    	
    	// remove the previous Riemann Panel
    	// and prepare a new Riemann Panel
    	
    	if (view != null) {
    		view.remove(riemannPanel);
    	}
    	
        riemannPanel = new JPanel();          
        riemannPanel.setLayout(new BoxLayout(riemannPanel, BoxLayout.X_AXIS));
        if (DEBUG) 
        	System.out.println("In Run ---> Size of modes = "+modes.length+" and Size of funcs = "+ funs.length);
        rMatrixDisplay = new JAbstractRiemannMatrixDisplay(this.tons, this.modes, this.funs, false);
        JScrollPane riemannMatrixPane = new JScrollPane(rMatrixDisplay.table);
        riemannMatrixPane.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Riemann Matrix"));
        rMatrixDisplay.table.setPreferredScrollableViewportSize(rMatrixDisplay.table.getPreferredSize());
        riemannPanel.add(riemannMatrixPane);  
        
        // add the newly prepared Riemann Panel
        if (view != null) {
        	view.add(riemannPanel, BorderLayout.CENTER);
        }
                                    
        setOutput(0, matrixSequence);
	}

	/*
	 * Create the sequence of Riemann matrices from chords
	 * and put them into a denotator
	 */
	private Denotator computeRiemannMatrixSequence(ArrayList<Chord> chords, boolean ChainOfThirds) {
		
		LinkedList<Denotator> matrixSeq = new LinkedList<Denotator>();
		Denotator method;

		
		rMatrix = new AbstractRiemannMatrix(null, tons.length, modes.length, funs.length);
				
		if (ChainOfThirds){
			// Build the chain repository
			// chord will be checked against the 3rd chains in this repository
			chainRepository = new LinkedList<Chain>();
			Chain.allThirdChains(chainRepository);
			if (DEBUG) 
				System.out.println("------ chain repository is prepared ------");
			
			method = makeDenotator(methodForm, CHAIN_OF_THIRDS);
		} else {
			method = makeDenotator(methodForm, DIRECT);
		}
		
		for (Chord c : chords) {
			LinkedList<Denotator> matrixValSeq = new LinkedList<Denotator>();
			
			if (ChainOfThirds) {
				chain3rd = c.findChainsOfThirds(chainRepository);
				rMatrix.compute3rdChain(chain3rd, weights, tons.length, modes.length, funs.length);
			} else {
				// direct 
				rMatrix.computeDirect(weights, tons.length, modes.length, funs.length);				
			}
						
			// denotator for the chord
			Denotator chord = c.toChord();

			// denotator for its riemann matrix
			for (int row=0; row<rMatrix.rowCount; row++) {
				for (int col=0; col<rMatrix.colCount; col++) { 
					double val = new Double(rMatrix.data[row][col].toString());
					// System.out.println(row + " " + col + " "+ val);
					Denotator matrixValue = makeRiemannValuePair(row, col, val);
					// System.out.println("------riemannMatrixValuePair-------------");
					// matrixValue.display();
					matrixValSeq.add(matrixValue);
				}
			}
			Denotator riemannMatrix = makeDenotator(riemannMatrixForm, matrixValSeq);
			// System.out.println("------riemannMatrix-------------");
			// riemannMatrix.display();
			
			// link these two denotators
			LinkedList<Denotator> factors = new LinkedList<Denotator>();
			factors.add(chord);
			factors.add(riemannMatrix);
			Denotator riemannMatrixWithChord = makeDenotator(riemannMatrixWithChordForm, factors);
			// add them the denotator to the sequence
			// System.out.println("------riemannMatrixWithChord-------------");
			// riemannMatrix.display();			
			matrixSeq.add(riemannMatrixWithChord);
		}
		Denotator riemannMatrixSequence = makeDenotator(riemannMatrixSequenceForm, matrixSeq);
		LinkedList<Denotator> SeqAndMethod = new LinkedList<Denotator>();
		SeqAndMethod.add(method);
		SeqAndMethod.add(riemannMatrixSequence);
		Denotator riemannMatrixSeqAndChord = makeDenotator(riemannMatrixSequenceWithMethodForm, SeqAndMethod);

		return riemannMatrixSeqAndChord;
	}

	
	
	/*
	 * Make a denotator of riemannValueForm
	 */
	public Denotator makeRiemannValuePair(int row, int col, double val) {
        LinkedList<Denotator> factors = new LinkedList<Denotator>();
        Denotator rid = makeDenotator(riemannRowIndexForm, row);
        Denotator rcd = makeDenotator(riemannColIndexForm, col);
        Denotator rvd = makeDenotator(riemannValueForm, val);
        factors.add(rid);
        factors.add(rcd);
        factors.add(rvd);        
        Denotator d = makeDenotator(riemannValuePairForm, factors);
        return d; 
    }
    
	

	/*
	 * list chords in the combo box as Chord(1), Chord(2), ...
	 */
	private void setAndViewChords(ArrayList<Chord> chords) {
		
		if (sp != null) {
			listModel.removeAllElements();	
			int cnum=0;
			for (Chord c : chords) {
				String str = "";
				for (GMINote n : c.notes) {
					str = str + GMINote.toPitchLetter(n.pitch_int) + " ";
				}
				listModel.add(cnum, str);
				cnum++;
			}
		}		
	}


	
	public void valueChanged(ListSelectionEvent e) {	
		listModel2.removeAllElements();
		
		selectedChords = ((JList<?>)e.getSource()).getSelectedIndices();		
		if (selectedChords.length == 1) {
			Chord chord = chords.get(selectedChords[0]);			
			viewMinimalChains(chord);
			if (selectedMethod.equals("Chain of Thirds")) {
				rMatrix.compute3rdChain(chain3rd, weights, tons.length, modes.length, funs.length);
			} else {
				rMatrix.computeDirect(weights, tons.length, modes.length, funs.length);
			}
			rMatrixDisplay.display(rMatrix);
		} 
	}
	
	
		
	/*
	 *  display minimal 3rd chains on the top right window
	 */
	private void viewMinimalChains(Chord chord) {
		
		if (selectedMethod.equals("Chain of Thirds")) {
			chain3rd = chord.findChainsOfThirds(chainRepository);
			// list all chains
			for (int i=0; chain3rd[i][0] != Chord.END_OF_CHAINS; i++) {
				String str = "";
				for (int j=0; j<chain3rd[i].length && chain3rd[i][j] != Chord.CHAIN_END; j++) {
					str = str + Integer.toString(chain3rd[i][j]) + " ";
				}
				listModel2.add(0, str);
			}
		} else {
			String str = "Not Applicable";
			listModel2.add(0, str);
		}
	}
	
    
    public String getGroup() {
        return "Score";
    }

    public String getName() {
        return "HarmonicWeight";
    }
    
    public String getShortDescription() {
        return "Finds Harmonic Weight of Chords";
    }
    
    public ImageIcon getIcon() {
        return icon;
    }

    public String getLongDescription() {
        return "The HarmonicWeight Rubette gets in a denotator of chordsequence form"+
               " and outputs a sequence of denotators of chordweight form";
    }

    private final static String METHOD = "method";
    
    
    @Override  
	public void toXML(XMLWriter writer) {
		writer.empty(METHOD, VALUE_ATTR, selectedMethod);
	}


	public Rubette fromXML(XMLReader reader, Element element) {
		
	 	Element child = XMLReader.getChild(element, METHOD);
	 	String val = child.getAttribute(VALUE_ATTR);
	 	
	 	HarmonicWeightRubette rubette = new HarmonicWeightRubette();	
	 	rubette.selectedMethod = val;
	 	return rubette;	 		 	
	}

       
    
	@Override
	public void stateChanged(ChangeEvent arg0) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub		
	}
}