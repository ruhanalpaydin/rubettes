/* Copyright (C) 2016 Ruhan Ikeda
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
* This rubette accepts a denotator of score and 
* outputs a sequence of <onset, weight>
*
*/

package org.rubato.rubettes.score;

import static org.rubato.logeo.DenoFactory.makeDenotator;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.rubato.base.*;
import org.rubato.logeo.FormFactory;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.Form;
import org.rubato.math.yoneda.LimitForm;
import org.rubato.math.yoneda.PowerForm;
import org.rubato.rubettes.util.GMINote;
import org.rubato.rubettes.util.JPartsDisplay;
import org.rubato.rubettes.util.LocalMeter;
import org.rubato.rubettes.util.Weight;
import org.rubato.composer.RunInfo;
import org.rubato.composer.icons.Icons;

/* Important notice:
 * Quantize notes in the beginning, and then do all computations on the quantized notes.
 */


public class MetroRubette extends SimpleAbstractRubette {
	
	public static Repository rep = Repository.systemRepository();
	
	// input denotator preparation
	Form noteForm				= rep.getForm("Note");
	Form chordForm				= FormFactory.makePowerForm("Chord", noteForm);
	PowerForm chordSequenceForm	= FormFactory.makePowerForm("ChordSequence", chordForm);
	Form scoreForm				= rep.getForm("Score");
	
	// Weight denotator preparation  	 
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
	
	// GUI related variables
	private static final ImageIcon icon = Icons.loadIcon(MetroRubette.class, "metrorubette.png");
	
	private final String minLocalMeterLengthKey	= "minLocalMeterLength";
    private int minLocalMeterLength;
	private final String metricalProfileKey	= "metricalProfile";
    private int metricalProfile;
    private final String quantizationKey = "quantization";
    private double quantization;

    protected ArrayList<GMINote> notes = null;
    protected ArrayList<GMINote> qNotes = null;
    protected ArrayList<Weight> weights = null;
    public ArrayList<LocalMeter> localMeterSeq = null;
    public JPartsDisplay partsDisplay = null;
    public Denotator metricWeights = null;    
    protected JPanel guiProperties = null;
    protected JTextField minLocalMeterField;
    protected JTextField metricProfileField;
    protected JPanel gproperties = null;
    
	protected boolean DEBUG = false;
	
	
	
	public MetroRubette() {
        setInCount(1);
        setOutCount(1);      
        putProperty(new IntegerProperty(this.metricalProfileKey, "Metrical Profile", 2, 2, 100));
        putProperty(new IntegerProperty(this.minLocalMeterLengthKey, "Minimum local meter length", 4, 2, 1000));
        putProperty(new DoubleProperty(this.quantizationKey, "Quantization", 0.25, 0.0001, 1.0));    
	}
	
	
	private void readProperties() {
		metricalProfile = ((IntegerProperty)this.getProperty(this.metricalProfileKey)).getInt();
    	minLocalMeterLength = ((IntegerProperty)this.getProperty(this.minLocalMeterLengthKey)).getInt();
    	quantization = ((DoubleProperty)this.getProperty(this.quantizationKey)).getDouble();
     }
	
	
	public void run(RunInfo runInfo) {
		
		Denotator seq	= getInput(0);
			
		if (seq == null) {
            addError("Input denotator is null.");
            metricWeights = null;
        } 
		// Metrorubette does analysis on chordSequenceForm
		else if (!seq.hasForm(scoreForm)) {
            addError("Input denotator is not of form Score\".");
            metricWeights = null;
        } 
		else if (seq.hasForm(scoreForm)) {
        	// do the analysis
        	readProperties();
        	notes = GMINote.scoreToGMINotes(seq);   
        	
    		qNotes = new ArrayList<GMINote>(notes.size());
        	qNotes = quantize(notes, quantization); 
        	
        	
        	setAndViewParts(qNotes);   
        	
        	// prepare output denotator
        	if (partsDisplay == null) {
        		// No part selection yet,
        		// Assume that user wants all parts
        		weights = new ArrayList<Weight>(qNotes.size());
        		computeWeights(notes, weights, quantization);       		
        	} else {
        		// Weights of selected parts only
        		weights = partsDisplay.getWeights();       		
            	// Set the output denotator
        	}     
        	normalize(weights);
        	metricWeights = toOnsetWeights(weights);
    		setOutput(0, metricWeights);
    		
    		displayList(qNotes, weights);    		
        } 	
	}
	

	// map weights values to the range [0.0, 1.0]
	private void normalize(ArrayList<Weight> weights) {
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

	
	
	// quantization of note onsets
	// for quantization of 0.25, round onset values
	// to closets multiple of 0.25
	private ArrayList<GMINote> quantize(ArrayList<GMINote> noteSeq, double qVal) {
		
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


	
	private void displayList(ArrayList<GMINote> notes, ArrayList<Weight> weights) {
       			
		// Display weights as a list
		JWeightList weightList = new JWeightList(notes, weights);
        weightList.view("Metric Weights");            
        weightList.pack();
        weightList.setPreferredSize(new Dimension(600, 600));
        weightList.setVisible(true);	
        weightList.setLocation(300,300);	
 	}
	
	
	
	// The following is copied almost verbatim (except scaling) from JMeterDisplay
	private void computeWeights(ArrayList<GMINote> notes, ArrayList<Weight> weights, double quantization) {

		// for every onset in the notes 
		// find the localmeter in the localMeterSeq that has this onset, 
		// increment onset's weight by the amount 
		
		ArrayList<LocalMeter> meters = new ArrayList<LocalMeter>(notes.size()*notes.size());
		findLocalMeters(notes, meters);	
							
		for (int idx=0; idx<notes.size(); idx++) {
			double weight = 0.0;
			for (LocalMeter lm : meters) {
				if (lm.onsets.contains(notes.get(idx).getOnset())) {
					int power=1;
					for (int i=0; i<metricalProfile; i++)
						power *= lm.length()-1;
					weight += power;
				}
			}			
			Weight w = new Weight(weight, notes.get(idx));
			weights.add(w);			
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
	}

	

	private void setAndViewParts(ArrayList<GMINote> notes) {
		if (partsDisplay != null) {
            partsDisplay.setAndViewParts(notes, this.metricalProfile, this.minLocalMeterLength, this.quantization);
        }
	}


	
	/*
	 * Create a denotator of form weightSequence
	 * from a list of weights
	 */
	
	public Denotator toOnsetWeights(ArrayList<Weight> weights) {
		
		List<Denotator> powSeq = new LinkedList<Denotator>();
		
		for (int i=0; i<weights.size(); i++) {	
			Denotator o = makeDenotator(onsetForm, weights.get(i).note.getOnset());
			Denotator p = makeDenotator(pitchForm, weights.get(i).note.getPitch());
			Denotator l = makeDenotator(loudnessForm, weights.get(i).note.getLoudness());
			Denotator d = makeDenotator(durationForm, weights.get(i).note.getDuration());
			Denotator v = makeDenotator(voiceForm, weights.get(i).note.getVoice());
			Denotator w = makeDenotator(weightValForm, weights.get(i).value);
			List<Denotator> powPair = new LinkedList<Denotator>();		
			powPair.add(o);
			powPair.add(p);
			powPair.add(l);
			powPair.add(d);
			powPair.add(v);
			powPair.add(w);
			Denotator pairD = makeDenotator(NoteAndWeightForm, powPair);
			powSeq.add(pairD);
		}		
		Denotator ws = makeDenotator(WeightSeqForm, powSeq);
		return ws;
	}
	
		
	public boolean hasView() {
        return true;
    }
	
	
	public JComponent getView() {
		if (partsDisplay == null) {
            partsDisplay = new JPartsDisplay(qNotes, this.metricalProfile, this.minLocalMeterLength, this.quantization);
            if (qNotes != null)
            	partsDisplay.setAndViewParts(qNotes, this.metricalProfile, this.minLocalMeterLength, this.quantization);
		}	
        return partsDisplay;
	}
	 
	
	public boolean hasProperties() {
		return true;
	}
	
	public String getGroup() {
        return "Score";
    }

    public String getName() {
        return "Metro";
    }
    
    public String getShortDescription() {
        return "Computes inner meters";
    }
    
    public ImageIcon getIcon() {
        return icon;
    }

    public String getLongDescription() {
        return "The MetroRubette gets in a score of parts"+
               " and outputs meter analysis on the selected part(s)";
    }
    
    public Rubette duplicate() {
        MetroRubette newRubette = new MetroRubette();
        return newRubette;
    }
}