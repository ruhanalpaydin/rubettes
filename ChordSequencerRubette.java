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
 * Feb 2019: Extract register info as well.
 *
 *
 * Feb 2021: Problems with long pieces. Eine Kleine Nacht Music I.
 */

package org.rubato.rubettes.score;

import static org.rubato.logeo.DenoFactory.makeDenotator;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.rubato.base.*;
import org.rubato.logeo.FormFactory;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.FactorDenotator;
import org.rubato.math.yoneda.Form;
import org.rubato.math.yoneda.LimitDenotator;
import org.rubato.math.yoneda.PowerForm;
import org.rubato.rubettes.util.Chord;
import org.rubato.rubettes.util.GMINote;
import org.rubato.rubettes.util.JChordDisplay;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.rubato.composer.RunInfo;
import org.rubato.composer.icons.Icons;
import org.w3c.dom.Element;

import static org.rubato.xml.XMLConstants.FALSE_VALUE;
import static org.rubato.xml.XMLConstants.TRUE_VALUE;
import static org.rubato.xml.XMLConstants.VALUE_ATTR;


/* 
 * This rubette takes a score denotator
 * and outputs chord sequence denotator
 */

public class ChordSequencerRubette extends AbstractRubette {	
	
	public static Repository rep 	= Repository.systemRepository();
	public static Form noteForm		= rep.getForm("Note");
	private static Form scoreForm	= rep.getForm("Score");
	
	// register chord form
	public static Form chordForm = FormFactory.makePowerForm("Chord", noteForm);
	Form x = rep.registerBuiltin(chordForm);
	
	// register chordsequence form
	public static PowerForm chordSequenceForm = FormFactory.makePowerForm("ChordSequence", chordForm);
    Form y = rep.registerBuiltin(chordSequenceForm);
    	
	private static final ImageIcon icon = Icons.loadIcon(ChordSequencerRubette.class, "chordsequencer.png");
	    
	
    /* GUI components */
	private JChordDisplay chordDisplay = null;
	
	/* Checkbox in the properties window */
    private JPanel           properties = null;
    private boolean          useDuration = false;
    private JCheckBox        useDurationBox;
    private JTextField		 quantizationField;
   
	protected ArrayList<GMINote> notes = null;
	protected ArrayList<Chord> chords = null;
	protected Denotator chordSequence = null;

	protected boolean DEBUG = false;
	private double quantizationVal = 0.03125;
	
	// 2.0 -> quarter
	// 1.0 -> eighth
	// 0.5 --> sisteenth
	// 0.25 --> thirthsecond
	// 0.125 ---> 64th
	// 0.0625 ---> 128th

	
	
	/*
	 * Rubette's interface
	 */
	public ChordSequencerRubette() {
        setInCount(1);
        setOutCount(1);        	
	}
	

	
	/*
	 * (non-Javadoc)
	 * @see org.rubato.base.AbstractRubette#run(org.rubato.composer.RunInfo)
	 */
	public void run(RunInfo runInfo) {	
		
		Denotator score = getInput(0);
		
		if (score == null) {
            addError("Input denotator is null.");
            chordSequence = null;
        }
        else if (!score.hasForm(scoreForm)) {
            addError("Input denotator is not of form \"Score\".");
            chordSequence = null;
        } else {
        	// return a sorted list of notes
        	notes = notesFromScore(score);    
        	/*
			for (int i=0; i<notes.size(); i++) {
				System.out.println("ons="+notes.get(i).onset+ " pitch="+GMINote.toPitchLetter(notes.get(i).pitch_int)+
						" dur= "+notes.get(i).duration+ " voice="+notes.get(i).voice);
			}
			*/
            try {
            	// convert notes to a chordsequence denotator
				chordSequence = chordSeqFromScore(notes);
				// display the chords in the chordSequence denotator
				setAndViewChordSeq(chordSequence);

            } catch (RubatoException e) {
				e.printStackTrace();
			}
        }
		if (chords!= null)
			setOutput(0, toChordSeqFromChords(this.chords));
    }
	
	
	// extract notes 
	private ArrayList<GMINote> notesFromScore(Denotator score) {
        int maxVoice = 0;
        List<Denotator> scoreNotes = ((FactorDenotator)score).getFactors();
        notes = new ArrayList<GMINote>(scoreNotes.size());
        double duration = 0.0;
        double begin = Integer.MAX_VALUE;
        
        for (Denotator d : scoreNotes) {
            LimitDenotator n = (LimitDenotator) d;
            GMINote note = new GMINote(n);
            if (note.onset + note.duration > duration) {
                duration = note.onset + note.duration;
            }
            if (note.onset < begin) {
                begin = note.onset;
            }
            maxVoice = Math.max(note.voice, maxVoice);
            notes.add(note);
        }	        
        // shift all notes, so that the first note starts at 0
        for (GMINote n : notes) {
            n.onset -= begin;
        }
        Collections.sort(notes);
        return notes;
    }

	
	
	
	
	private  Denotator toChordSeqFromChords(ArrayList<Chord> chordList) {
		LinkedList<Denotator> chordSeq = new LinkedList<Denotator>();
		for (Chord c : chordList) {
			Denotator d = c.toChord();
			chordSeq.add(d);
		}
		Denotator s = makeDenotator(chordSequenceForm, chordSeq);
		return s;
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
            properties.setLayout(new BorderLayout());
            useDurationBox = new JCheckBox("Use Duration"); 
            useDurationBox.setToolTipText("Uncheck this if chord starts with notes having the same onset");
            useDurationBox.setSelected(useDuration);
            properties.add(useDurationBox, BorderLayout.NORTH);
            
            properties.add(new JSeparator(SwingConstants.HORIZONTAL), BorderLayout.CENTER);
 
            
            // Also put a qantizationVal TextField here
            // so that the user enters a quantization value
            // to be used when Use Duration is set.    
            JPanel quantizeProperties = new JPanel();
			quantizeProperties.setLayout(new BoxLayout(quantizeProperties, BoxLayout.X_AXIS));			
			JLabel quantizationlabel = new JLabel("Quantization: ");
			quantizeProperties.add(quantizationlabel);	    	    	
			quantizationField = new JTextField("");
			quantizationField.setText(String.valueOf(quantizationVal));
			quantizationField.setEditable(true);
			quantizationField.setOpaque(true);
			quantizeProperties.add(quantizationField);	    	

			properties.add(quantizeProperties, BorderLayout.SOUTH);
                    
        }
        return properties;
    }
	
	   
    public boolean applyProperties() {
    	
        setUseDuration(useDurationBox.isSelected());
        quantizationVal = Double.parseDouble(quantizationField.getText());		
        
        return true;
    }

    
 	public JComponent getView() {
		        
		if (chordDisplay == null) {
            chordDisplay = new JChordDisplay(chords);   									               
            if (chords != null) {
                chordDisplay.setAndViewChordSeq(chords);
            }
		}	
        return chordDisplay;
    }


	/*
	 * Given a chord sequence denotator, convert it to a list of chords
	 * and then hand it to the GUI for display
	 */
	private void setAndViewChordSeq(Denotator chordSeq) {	
		
		this.chords = Chord.toChordsFromDenotator(chordSeq);
		if (chordDisplay != null) {
            this.chordDisplay.setAndViewChordSeq(this.chords);
        }
	}


	public void init() { }
	
	
	// quantization of note onsets
	// for quantization of 0.25, round onset values
	// to closest multiple of 0.25
	private ArrayList<GMINote> quantize_onsets(ArrayList<GMINote> noteSeq, double qVal) {
		
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
	
	
	// sometimes in the midi file there are two notes
	// which has the same pitch and onset and same channel 
	// but different durations
	// which is an impossibility in actual music. 
	// In such a case, remove the smaller duration note.
	//
	// E.g.this is the case in Sonata05-2.mid in the trill
	// at the end of measure 6.
	// Feb, 2021
	
	private  ArrayList<GMINote> check_MIDI(ArrayList<GMINote> notes) {
		
		ArrayList<GMINote> fixedNotes = new ArrayList<GMINote>(notes.size());
		
		GMINote prevNote = notes.get(0);
		fixedNotes.add(prevNote);
				
		for (int i=1; i<notes.size(); i++) {	
			GMINote note = notes.get(i);	
			if ((note.onset == prevNote.onset) && (note.pitch_int == prevNote.pitch_int) && (note.voice == prevNote.voice)) {
				// take the longer duration note
				if (note.duration >= prevNote.duration) {
					if (DEBUG) System.out.println("same note! Removing " + prevNote.onset + ", " + prevNote.pitch_int);
					fixedNotes.remove(prevNote);
					fixedNotes.add(note);
					prevNote = note;
				} else {
					fixedNotes.add(prevNote);
				}				
			} else {
				fixedNotes.add(note);
				prevNote = note;
			}					
		}	
		return fixedNotes;
	}

	
	// quantization of note onsets
	// for quantization of 0.25, round onset values
	// to closest multiple of 0.25
	private ArrayList<GMINote> quantize_durations(ArrayList<GMINote> noteSeq, double qVal) {
		
		ArrayList<GMINote> qNotes = new ArrayList<GMINote>(notes.size());
		
		for (int i=0; i<noteSeq.size(); i++) {	
			GMINote note = noteSeq.get(i);		
			int res = (int) Math.round((note.getDuration() / qVal));
			double qDur = res*qVal;
			GMINote qnote = new GMINote(note.getOnset(), note.getPitch(), note.getPitchInt(), 
							note.getLoudness(), qDur, note.getVoice());
			qNotes.add(qnote);			
		}				
			
		/*
		for (int i=0; i<qNotes.size(); i++) {
			System.out.println("ons="+qNotes.get(i).onset+ " pitch="+GMINote.toPitchLetter(qNotes.get(i).pitch_int)+
					" dur= "+qNotes.get(i).duration+ " voice="+qNotes.get(i).voice);
		}	
		*/	
		return qNotes;
	}

	
		
	/* 
	 * Build a chordsequence denotator from array of notes
	 * in two different ways, taking into account use-duration.
	 */
	private Denotator chordSeqFromScore(ArrayList<GMINote> notes) 
										throws RubatoException {
		
		notes = check_MIDI(notes);
				
		if (useDuration) {				
			//divide all notes so that chord onsets will align with note onsets		
			/*
			System.out.println("Before quantization of notes:\n");
			for (int i=0; i<notes.size(); i++) {
				System.out.println("ons="+notes.get(i).onset+ " pitch="+GMINote.toPitchLetter(notes.get(i).pitch_int)+
						" dur= "+notes.get(i).duration+ " voice="+notes.get(i).voice);
			}
			 */
			
			
			ArrayList<GMINote> qNotes = quantize_onsets(notes, quantizationVal);
			qNotes = quantize_durations(qNotes, quantizationVal);
			
			/*
			System.out.println("After quantization of notes:\n");

			for (int i=0; i<notes.size(); i++) {
				System.out.println("ons="+qNotes.get(i).onset+ " pitch="+GMINote.toPitchLetter(qNotes.get(i).pitch_int)+
						" dur= "+qNotes.get(i).duration+ " voice="+qNotes.get(i).voice);
			}
			*/
			
			ArrayList<GMINote> divided_notes = divide(qNotes);
			// now find the chords
			return (conquer(divided_notes));
			
		} else {
			GMINote current_note;
			
			// do not divide notes 
			// a chord is a sequence of notes with the same onset
			LinkedList<Denotator> chordSeq = new LinkedList<Denotator>();
			
			boolean[] used_notes = new boolean[notes.size()];
			
			for (int i=0; i<notes.size(); i++) 
				used_notes[i] = false;
			
			for (int i=0; i<notes.size(); i++) {
				if (used_notes[i] == false) {
					// first note of the chord
					current_note = notes.get(i);
					used_notes[i] = true;
					LinkedList<Denotator> noteList = new LinkedList<Denotator>();
					noteList.add(current_note.makeNote());
					
					// find all notes that has the same onset
					for (int j=i+1; j<notes.size(); j++) {
						if (used_notes[j] == false) {
							if(notes.get(j).onset == current_note.onset) {
								noteList.add(notes.get(j).makeNote());
								used_notes[j] = true;
							}
						}
					}					
					Denotator chord = makeDenotator(chordForm,  noteList);
		        	chordSeq.add(chord);
				}
			}				
			Denotator d = makeDenotator(chordSequenceForm, chordSeq);
			return d;
		}
	}
    

	
	/*
	 * Divide all notes so that chord onsets align with note onsets;
	 * re-sort notes after this division process
	 */
	ArrayList<GMINote> divide(ArrayList<GMINote> notes) 
										throws RubatoException {
		
		
		ArrayList<GMINote> divided_notes = new ArrayList<GMINote>();
			
		while (notes.size() > 0) {
			
			GMINote m = notes.get(0);
			if(DEBUG) {
				System.out.println("\nConsidering window for note: "+m.getOnset());
			}
			
        	// m is the note under inspection, all chords that m is part of will be found 
			LinkedList<GMINote> overlaps = new LinkedList<GMINote>();
			LinkedList<GMINote> toRemove = new LinkedList<GMINote>();
			LinkedList<GMINote> toAdd = new LinkedList<GMINote>();
			
			// build a window of overlapping notes with m (including m)
        	for (GMINote n : notes) {
        		if ((n.onset >= m.onset) && n.onset < (m.onset + m.duration)) 
        			overlaps.add(n);
        	}
        	
        	// remove overlaps from notes
        	for (GMINote n : overlaps)   
        		notes.remove(n);
        	
        	// handle overlaps
        	if (overlaps.size() == 1) {
        		// if there is no overlap, then there is nothing to divide
            	divided_notes.add(m);
        	} else {
        		
        		// there are overlaps so there may be notes to divide
        		double cur_onset = m.onset;
        		
    			while (overlaps.size() > 1) {
    				if (DEBUG) {
    					System.out.println("cur_onset="+cur_onset+" Overlaps:\n");
    					for (GMINote n : overlaps) 
    						System.out.println(n.toString());
    					System.out.println("--------- end ----------");
    				}
    				
    				// reference note is a segment as [onset,duration] whose projection will make a chord
    				GMINote r = getReference(overlaps);
    				
        			for (GMINote n : overlaps) {
        				if (n.onset == r.onset) {
        					if (n.duration == r.duration) {
        						toRemove.add(n);
        						divided_notes.add(n);
        					} else {
        						GMINote n1 = new GMINote(cur_onset, n.pitch, n.pitch_int, n.loudness, 
        											r.duration, n.voice);
        						GMINote n2 = new GMINote(cur_onset+r.duration, n.pitch, n.pitch_int, n.loudness,
        								            n.duration-r.duration, n.voice);
        						toRemove.add(n);
        						divided_notes.add(n1);
        						if (DEBUG) {
        							System.out.println("Dividing into following notes:");
        							System.out.println(n1.toString());
        						}
        						
        						toAdd.add(n2);
        						if (DEBUG) {
        							System.out.println(n2.toString());
        							System.out.println("---");
        						}
        					}
        				} 
        			}
        			// do the removes and adds on overlaps
        			if (DEBUG) {
        				System.out.println("Removing " + toRemove.size());
        				System.out.println("Adding " + toRemove.size());
        			}
        			for (GMINote n : toRemove) overlaps.remove(n);
        			for (GMINote n : toAdd) overlaps.add(n);
        			
        			// print overlaps
        			if (DEBUG){ 
        				for (GMINote n : overlaps) System.out.println(n.toString());
							System.out.println("--------- end ----------");
        			}
        			toRemove.clear();
        			toAdd.clear();
        			Collections.sort(overlaps);
        			cur_onset += r.duration;
        			if (DEBUG) {
        				System.out.println("Current onset=" + cur_onset + " m.onset=" + m.onset + " m.duration=" + m.duration);
        			}
        			// if all remaining notes in the overlaps protruding from the current the window
        			// then add these notes to 'notes' and re-sort
        			if (cur_onset >= (m.onset + m.duration)) {
        				if (DEBUG) {
        					System.out.println("Transferring following overlaps to next window:");
        					for (GMINote n : overlaps)
        						System.out.println(n.toString());
        					System.out.println("--------- end ----------");
        				}
        	    		
        				for (GMINote n : overlaps) 
        					notes.add(n);
        				Collections.sort(notes);
        				overlaps.clear();
        			}
    			} // while -- while there are overlaps
        	} // else if there are overlaps
        	notes.remove(m);
		} // while -- there are notes
		
		Collections.sort(divided_notes);
		if (DEBUG) {
			System.out.println("Divided notes: ");
			for (GMINote d : divided_notes)
				System.out.println("onset="+ d.onset + " d= "+ d.duration + " ;pitch=" + d.pitch_int);
			System.out.println("--------- end ----------");
		}
		return divided_notes;
	}
	
	
	
	/*
	 * Create a note that is :
	 * Note with the smallest onset
	 * Note that, this note will not be further segmented
	 */
	GMINote getReference(LinkedList<GMINote> overlapNotes) {
		
		// m is the notes with smallest onset
        GMINote m = Collections.min(overlapNotes);
               
        // smallest duration among overlapping same onsets */
		for (int i=0; i<overlapNotes.size();i++) {
			if (overlapNotes.get(i) != m) {
				if ((overlapNotes.get(i).onset == m.onset) && (overlapNotes.get(i).duration < m.duration)) {
					m = overlapNotes.get(i);
					if (DEBUG) System.out.println("m with dur="+ m.duration);
				}
			}
		}
       
        GMINote n = new GMINote(m.onset, m.pitch, m.pitch_int, m.loudness, m.duration, m.voice);
                
        for (GMINote o : overlapNotes) {
            if (o != n) {
                if (o.onset > n.onset && (o.onset < (n.onset + n.duration))) {
                        n.duration = o.onset - n.onset;
                }
            }
        }
        
        if (DEBUG) System.out.println("Reference note="+n.toString());
        return n;
	
		
		/*
		double minDur = overlapNotes.get(0).duration;
		int minIndex = 0;
		
		for (int i=1; i<overlapNotes.size();i++) {
			if (overlapNotes.get(i).duration < minDur)
				minDur = overlapNotes.get(i).duration;
				minIndex = i;
		}
				
		GMINote n = new GMINote(overlapNotes.get(minIndex).onset, overlapNotes.get(minIndex).pitch, 
						overlapNotes.get(minIndex).pitch_int, overlapNotes.get(minIndex).loudness, 
						overlapNotes.get(minIndex).duration, overlapNotes.get(minIndex).voice);
		
		// n is the note with smallest duration	
		if (DEBUG) System.out.println("Reference note="+n.toString());
		return n;
		*/
	}
	
	
	
	/*
	 * Build chords out of a list from a sequence of notes
	 * that do not overlap partially (that is to say, any two
	 * notes in the list overlap completely (same onset and same duration)
	 * or do not overlap at all.
	 */
	Denotator conquer(ArrayList<GMINote> notes) {
		
    	// This is the list of chords that make a chord sequence
    	LinkedList<Denotator> chordSeq = new LinkedList<Denotator>();
    	
    	double current_onset;
    	int i=0, j=0;
    	if(DEBUG) {
    		System.out.println("Conquering notes:");
    	}
		for (i=0; i<notes.size(); i=j) {
			// This is the list of notes that make a chord
			LinkedList<Denotator> noteList = new LinkedList<Denotator>();
			
			if (DEBUG) System.out.println("Building chord on" + notes.get(i).toString());
			noteList.add(notes.get(i).makeNote());
			current_onset = notes.get(i).onset;
			for (j=i+1; j<notes.size() && notes.get(j).onset == current_onset; j++) {
				noteList.add(notes.get(j).makeNote());
				if (DEBUG) System.out.println(" ----- Adding :"+notes.get(j).toString());
			}
			Denotator chord = makeDenotator(chordForm,  noteList);
        	chordSeq.add(chord);
		}
		Denotator d = makeDenotator(chordSequenceForm, chordSeq);
		return (d);
	}	
	
    
    
    public String getGroup() {
        return "Score";
    }

    public String getName() {
        return "ChordSequencer";
    }
    
    public String getShortDescription() {
        return "Outputs a sequence of chords";
    }
    
    public ImageIcon getIcon() {
        return icon;
    }

    public String getLongDescription() {
        return "The ChordSequencer Rubette gets in a score form"+
               " and outputs a chord sequence denotator";
    }
    
    public Rubette duplicate() {
        ChordSequencerRubette newRubette = new ChordSequencerRubette();
        return newRubette;
    }
 
    public void toXML(XMLWriter writer) {
            writer.empty(USEDURATION, VALUE_ATTR, useDuration?TRUE_VALUE:FALSE_VALUE);
            writer.empty(QUANTIZATION, VALUE_ATTR, quantizationVal);
    }
 
    public Rubette fromXML(XMLReader reader, Element element) {

    	boolean r = false;
    	Element child = XMLReader.getChild(element, USEDURATION);
    	String val = child.getAttribute(VALUE_ATTR);
    	if (val.equals(TRUE_VALUE)) {
    		r = true;
    	}
    	child = XMLReader.getChild(element, QUANTIZATION);
    	val = child.getAttribute(VALUE_ATTR);
    	quantizationVal = Double.parseDouble(val);
    	
    	ChordSequencerRubette rubette = new ChordSequencerRubette();
    	rubette.setUseDuration(r);
    	rubette.setQuantization(quantizationVal);
    	return rubette;
    }
    

    private void setUseDuration(boolean b) {
    	useDuration = b;
    }
    
    private void setQuantization(double q) {
    	quantizationVal = q;
    }


    private final static String USEDURATION = "UseDuration";
    private final static String QUANTIZATION = "Quantization";
    
}
