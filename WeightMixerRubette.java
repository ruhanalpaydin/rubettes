/* Copyright (C) 2019 Ruhan Ikeda
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
 * WeightMixer Rubette extracts weight values from
 * weight sequence denotators at the inputs. 
 * The number of inputs can be set by the user.
 * Weight mixing is done according to a formula 
 * input bu the user. 
 * 
 * This formula should be set in the Properties
 * prior to running the network.
 * 
 * There is a single output: A weight sequence.
 * 
 */


package org.rubato.rubettes.score;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import static org.rubato.composer.Utilities.getJDialog;
import static org.rubato.logeo.DenoFactory.makeDenotator;

import org.rubato.base.AbstractRubette;
import org.rubato.base.Repository;
import org.rubato.base.Rubette;
import org.rubato.composer.RunInfo;
import org.rubato.composer.components.JConnectorSliders;
import org.rubato.composer.dialogs.scheme.Messages;
import org.rubato.composer.icons.Icons;
import org.rubato.logeo.FormFactory;
import org.rubato.math.arith.Rational;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.FactorDenotator;
import org.rubato.math.yoneda.Form;
import org.rubato.math.yoneda.LimitForm;
import org.rubato.math.yoneda.PowerForm;
import org.rubato.rubettes.builtin.ArithCompiler;
import org.rubato.rubettes.builtin.ArithVM;
import org.rubato.rubettes.util.Weight;
import org.rubato.rubettes.util.GMINote;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;

import com.sun.glass.events.KeyEvent;



public class WeightMixerRubette extends AbstractRubette
    							implements ChangeListener {
	
	Repository rep = Repository.systemRepository();	 
	Form weightValForm = FormFactory.makeRModuleForm("Value");
	Form onsetForm		= rep.getForm("Onset");
	Form pitchForm    = rep.getForm("Pitch");
	Form loudnessForm = rep.getForm("Loudness");
	Form durationForm = rep.getForm("Duration");
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
			
    private JPanel            properties = null;
    private JConnectorSliders inSlider = null;
    private JPanel            inPanel = null;
    private double[]          values = null;   
    private JTextArea         exprTextArea = null;
    private JLabel            infoLabel = null;
    private String            expressionString = ""; 
    private Denotator[]       weightSeqs;
    protected ArrayList<ArrayList<Weight>> weightSequences = null;
    protected Denotator 	  outputSeq = null;    
	private JPanel 			  view = null;
    private ArithCompiler     compiler = null;
    private ArithVM           vm = null;
    private JCheckBox showInpListBox, showOutListBox;
    private boolean           showIList = false;
    private boolean           showOList = false;
    
    ArrayList<Weight> mixedWeightList = null;
	private JPanel weightPanel = null;
	private JHarmonicWeight weightView = null;

    
    private static final ImageIcon icon = Icons.loadIcon(WeightMixerRubette.class, "mixing.png");

    
    public WeightMixerRubette() {
        setInCount(1);
        setOutCount(1);
    }
    
           
    public void run(RunInfo runInfo) {
        
    	// Read weights in the input. For example:
      	// weightSeqs[0] are metric weights
      	// weightSeqs[1] are harmonic weights   	
    	   	
    	weightSequences = new ArrayList<>(getInCount());
    	values = new double[getInCount()]; 

    	for (int i = 0; i < getInCount(); i++) {
    		System.out.println("i = " + i);
            weightSeqs[i] = getInput(i);
            if (weightSeqs[i] == null) {
                values[i] = 0.0;
            } else if (!weightSeqs[i].hasForm(WeightSeqForm)){
            	addError("Input denotator is not of form \"WeightSeq\".");
                outputSeq = null;    
                return;
            } else {
            	// read the WeightSeq denotator into WeightList
            	List<Denotator> w = ((FactorDenotator)weightSeqs[i]).getFactors();
            	
            	ArrayList<Weight> wList = new ArrayList<Weight>(w.size());
            	for (Denotator wDen : weightSeqs[i]) {
            		Weight weight = new Weight(wDen);
            		wList.add(weight);
            	}
            	weightSequences.add(wList);
            }
        }
    	
    	
    	// Check weight sequences to see
    	// that they contain the same sequence of notes
    	boolean noProblem = checkAlign(weightSequences);
    	
    	if (!noProblem) {
    		addError("Input Weights do not align! ");
    		outputSeq = null;
    		return;
    	} else {
        	// pass individual Weight values to be used in the expression
        	int seqSize = weightSequences.get(0).size();
        	int numSeqs = weightSequences.size();
        	
           	mixedWeightList = new ArrayList<Weight>(seqSize);

           	for (int w=0; w<seqSize; w++) {
        		for (int s=0; s<numSeqs; s++) {
        			values[s] = weightSequences.get(s).get(w).value;
        		}   
        		// if there is an expression already, then mix weights
        		vm.eval(values);
            	if (vm.hasError()) {
            		addError(vm.getError());
            		return;
            	} else {
            		double mixedWeight = vm.getRealResult();
            		Weight mw = new Weight(mixedWeight, weightSequences.get(0).get(w).note);
            		mw.note.onset = weightSequences.get(0).get(w).note.onset;
             		mixedWeightList.add(mw);
            	}
        	}
           	
	        if (mixedWeightList != null) {
	        	if (view != null) {
	        		weightPanel.removeAll();
	        		ArrayList<GMINote> notes = extractNotes(mixedWeightList); 
	        		weightView = new JHarmonicWeight(notes, mixedWeightList, 12, 1, 0.0, 0.5,
	        											"Mixed Weight of Notes");           	
	        		weightPanel.add(weightView);
	        	}
	        }

        	// Prepare the output denotator
        	// Should be a WeightSeq denotator
        	Denotator output = toDenotator(mixedWeightList);
        	setOutput(0, output);     
        	
        	// Display input and output weights as a list, if 
        	// selected by the user
        	if (showIList) {
        		for (int s=0; s<getInCount(); s++) {
        			JWeightList list = new JWeightList(weightSequences.get(s));
            		list.view("Input Weight " + String.valueOf(s));
               		list.pack();
            	    list.setMinimumSize(new Dimension(600, 600));
            	    list.setPreferredSize(new Dimension(600, 600));
            	    list.setMinimumSize(new Dimension(600, 600));
            	    list.setVisible(true);	
            	    list.setLocation(400+s*20, 400+s*20);	  
        		}
        	}
        	
        	if (showOList) {
        		JWeightList list = new JWeightList(mixedWeightList);
        		list.view("Mixed Weights");
        		list.pack();
        	    list.setMinimumSize(new Dimension(600, 600));
        	    list.setPreferredSize(new Dimension(600, 600));
        	    list.setMinimumSize(new Dimension(600, 600));
        	    list.setVisible(true);	
        	    list.setLocation(300,300);	  
        	}
         }       
     }

    
	private Denotator toDenotator(ArrayList<Weight> wList) {
		LinkedList<Denotator> weightSeq = new LinkedList<Denotator>();
		for (Weight w : wList) {	
			Denotator weightAndNote = w.makeWeight();
			weightSeq.add(weightAndNote);			
		}
		Denotator r = makeDenotator(WeightSeqForm, weightSeq);
		return r;
	}
	


     /* Check if weight sequences belong to exactly the same sequence of notes.
      * If not, do the mixing adequately.
      * 
      * For example: If Weight Sequence 1 = ((n1, w1), (n2, w2), (n3, w3)) and 
      * Weight Sequence 2 = ((n1, w4) (n3, w5)) then 
      * Output Sequence = ((n1, f(w1,w4)), (n2, f(w2, {})), (n3, f(w3,w5)))
      * OR
      * Why does n2's weight do not exist in Weight Sequence 2?
      * The latter output is sounder. Give an error about n2's absence in Weight Sequence 2.
      */
	 private boolean checkAlign(ArrayList<ArrayList<Weight>> weightSq) {
		
		int seq = maxIndex(weightSq);		  // index of the longest sequence
		
		for (int n=0; n<weightSq.get(seq).size(); n++) {
			Weight weight = weightSq.get(seq).get(n);
			
			// search for the note in all other weight sequences

			for (int s=0; s<weightSq.size(); s++) {
				if (s!=seq) {
					boolean found = false;
					int size = weightSq.get(s).size();
					for (int w=0; w<size && !found; w++) {
						if (sameNote(weight.note, weightSq.get(s).get(w).note)) {
							found = true;
						}
					}
					if (!found) return false;
				}
			}
		}			
		return true;
	 }

	 
	 
	 private boolean sameNote(GMINote note, GMINote note2) {
		if(note.onset == note2.onset && note.pitch_int == note2.pitch_int && note.voice == note2.voice) {
			return true;
		} else {
			return false;
		}
	}


	// return the index of the longest weight sequence
	 private int maxIndex(ArrayList<ArrayList<Weight>> weightSq) {

		 int maxIndex = 0;
		 int max = weightSq.get(maxIndex).size();
		 
		for (int s=1; s<weightSq.size(); s++) {
			if (weightSq.get(s).size() > max) {
				maxIndex = s;
				max = weightSq.get(maxIndex).size();
			}
		}			
		return maxIndex;
	 }

	 
	public boolean hasView() {
        return true;
    }		

	
	/*
	 * (non-Javadoc)
	 * @see org.rubato.base.AbstractRubette#getView()
	 */
	public JComponent getView() {	        
		
		if (view == null) {
			view = new JPanel();
			weightPanel = new JPanel();
	        weightPanel.setLayout(new BoxLayout(weightPanel, BoxLayout.Y_AXIS));
	        weightPanel.setMinimumSize(new Dimension(500,750));
	        weightPanel.setBackground(Color.WHITE);
	        
	        if (mixedWeightList != null) {
	        	view.removeAll();
	        	
		        ArrayList<GMINote> notes = extractNotes(mixedWeightList); 
	        	weightView = new JHarmonicWeight(notes, mixedWeightList, 12, 1, 0.0, 0.5, "Mixed Weights of Notes");           	
	        	weightPanel.add(weightView);
	        }
	        view.add(weightPanel);
		}
		return view;
	}
	
 
	
	 // extract notes from weights
	 // need this to call JHarmonicWeightDisplay()
   	 private ArrayList<GMINote> extractNotes(ArrayList<Weight> wList) {
		
   		ArrayList<GMINote> notes = new ArrayList<GMINote>(wList.size());
   		
   		for (Weight w : wList) {
   			GMINote note = new GMINote(w.note.getOnset(), w.note.getPitch(), w.note.getPitchInt(),
   								w.note.getLoudness(),w.note.getDuration(), w.note.getVoice());
   			notes.add(note);
    	}
		return notes;
	}

  	 
	public String getGroup() {
        return "Score";
     }

    
     public String getName() {
        return "WeightMixer";
     }

    
     public Rubette newInstance() {
        return new WeightMixerRubette();
     }
    
    
     public Rubette duplicate() {
    	WeightMixerRubette newRubette = new WeightMixerRubette();
        newRubette.setInCount(getInCount());
        newRubette.weightSeqs = new Denotator[getInCount()];

        return newRubette;
     }

    
     public boolean hasProperties() {
        return true;
     }

    
     public JComponent getProperties() {
        if (properties == null) {
            properties = new JPanel();            
            properties.setLayout(new BorderLayout());
            
            inSlider = new JConnectorSliders(true, false);
            inSlider.setInLimits(2, 12);
            inSlider.setInValue(getInCount());
            inSlider.addChangeListener(this);
            properties.add(inSlider, BorderLayout.NORTH);
            
            inPanel = new JPanel();
            fillInPanel();
            properties.add(inPanel, BorderLayout.CENTER);
            
            // Add weight mixing formula
            // Code is taken from RealArithRubette.java
            Box box = new Box(BoxLayout.Y_AXIS);

            JLabel label = new JLabel("Weight Mixing Formula (#0 is the 1st input, #1 is the 2nd,...):");   
            box.add(label);
            exprTextArea = new JTextArea(5, 20);
            exprTextArea.setText(expressionString);
            JScrollPane scrollPane = new JScrollPane(exprTextArea);
 	        box.add(scrollPane, BorderLayout.NORTH);
 
            infoLabel = new JLabel(""); //$NON-NLS-1$
            infoLabel.setForeground(Color.RED);
            infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            box.add(infoLabel, BorderLayout.CENTER);
            
            showInpListBox = new JCheckBox("Show Input weights as a list"); 
            showInpListBox.setToolTipText("Check this to see input weights as a text list");
            showInpListBox.setSelected(showIList);
            box.add(showInpListBox);
            
            showOutListBox = new JCheckBox("Show Mixed weights as a list"); 
            showOutListBox.setToolTipText("Check this to see output weights as a text list");
            showOutListBox.setSelected(showOList);
            box.add(showOutListBox);

            properties.add(box, BorderLayout.SOUTH);
             
        }
        return properties;
    }


     private boolean compile(String expr, int nrArgs) {
        if (compiler == null) {
            compiler = new ArithCompiler();
        }
        vm = null;
        compiler.setExpression(expr, true);
        if (compiler.parse(nrArgs)) {
            vm = compiler.getVM();
            return true;
        }
        return false;
    }

  
    public boolean applyProperties() {        
        int inCount = inSlider.getInValue();
        setInCount(inCount);
        weightSeqs = new Denotator[inCount];
        
        // Should get the formula here
        int newInCount = inSlider.getInValue();
        String expr = exprTextArea.getText().trim();
        if (expr.length() == 0) {
        	// infoLabel.setText(Messages.getString("RealArithRubette.noexpression")); //$NON-NLS-1$
            return false;
        }

        setListWeightBox(showInpListBox, showInpListBox.isSelected());
        setListWeightBox(showOutListBox, showOutListBox.isSelected());

        boolean ok = compile(expr, newInCount);
        if (!ok) {
            infoLabel.setText(compiler.getError());
            return false;
        }
        else {
            setInCount(newInCount);
            expressionString = expr;
            values = new double[getInCount()];
            return true;
        }
        
     }

    
    
    
    
    
    private void setListWeightBox(JCheckBox box, boolean b) {
    	if (box == showInpListBox) 
    		showIList = b;
    	else
    		showOList = b;
    }

    
    public void revertProperties() {
        inSlider.setInValue(getInCount());
        fillInPanel();
        getJDialog(properties).pack();
        
    }

    
    public String getShortDescription() {
        return "Mixes weights in the inputs";
    }

    
    public ImageIcon getIcon() {
        return icon;
    }
    

    public String getLongDescription() {
        return "The WeightMixer Rubette mixes the weights according to "+
               "the given formula. Inputs are #0, #1,... in the formula";
    }

    
    public Denotator getValue(int i) {
        return weightSeqs[i];
    }
    
    
    public void stateChanged(ChangeEvent e) {
        fillInPanel();
        getJDialog(properties).pack();
    }
    
    
    private void fillInPanel() {
        inPanel.removeAll();
     }
    
    
    private final static String INPUTS    	= "Inputs";
    private final static String FORMULA 	= "Formula";
    private final static String EXPR     	= "expr";
    private final static String COUNT_ATTR 	= "count";

    
    public void toXML(XMLWriter writer) {
        writer.openBlock(INPUTS, COUNT_ATTR, getInCount());        
        // String s = exprTextArea.getText().trim();
        // writer.empty(FORMULA, EXPR, s);
        writer.closeBlock();
    }
    
    
    public Rubette fromXML(XMLReader reader, Element element) {
        Element child = XMLReader.getChild(element, INPUTS);
        int inCount = XMLReader.getIntAttribute(child, COUNT_ATTR, 2, 12, 2);
        /*
        child = XMLReader.getChild(child, FORMULA);
        String formula = child.getAttribute(EXPR);
        */
 
        WeightMixerRubette rubette = new WeightMixerRubette();
        rubette.setInCount(inCount);       
        rubette.weightSeqs = new Denotator[inCount];
        // rubette.expressionString = formula;
         
        return rubette;
    }
 }
