/*
 * Copyright (C) 2021 Ruhan Ikeda
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

/* Write the harmonic analysis output to a lilypond file */

/* 
 * TODO list: (by Feb 28, 2021)
 * 
 * (1) Fix {T,D,S,t,d,s}. Needs to be abstract, taken as a parameter.
 * (2) revert properties
 * (3) tied stuff and more testing on musical examples
 * (4) accidentals with respect to key sign (#'s and flats).
 * 
 */


package org.rubato.rubettes.score;

import static org.rubato.xml.XMLConstants.VALUE_ATTR;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

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
import org.rubato.math.yoneda.PowerForm;
import org.rubato.math.yoneda.SimpleDenotator;
import org.rubato.math.yoneda.SimpleForm;
import org.rubato.rubettes.util.Chord;
import org.rubato.rubettes.util.JScoreList;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;



public class LilypondFileOutRubette extends AbstractRubette {
	
	static Repository rep = Repository.systemRepository();
	
	// Input Denotator (output #1)	
	private Form noteForm = rep.getForm("Note");
	private Form chordForm = FormFactory.makePowerForm("Chord", noteForm);
	private SimpleForm rowForm = FormFactory.makeZModuleForm("Row");
	private SimpleForm colForm = FormFactory.makeZModuleForm("Column");
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

	    
	private static final ImageIcon icon = Icons.loadIcon(LilypondFileOutRubette.class, "lilypondFileOut.png");
	
	protected JScoreList analysisList = null;
    private JTextField   fileNameField;
    private File         lilyFile         = null;
    private double 		 lilyTempo = 0.0;
    private String		 lilyKey = null;
    private String		 lilyMeter = null;
    private File         selectedFile     = null;
    private File         currentDirectory = new File(".");
    private JFileChooser fileChooser      = null;
    private JTextField	 tempoField, keyField, meterField;
        
    protected JPanel properties = null;
    private ArrayList<RMPoint> path = null;
       
	private String[] tonalities = {"C", "Cis", "D", "Dis", "E", "F", "Fis", "G", "Gis", "A", "Ais", "B"};
	private String[] modsFuns = {"T", "S", "D", "t", "s", "d"};

	
	// constructor
    public LilypondFileOutRubette() {
        setInCount(1);
        setOutCount(0);
    }

  
	public void run(RunInfo runInfo) {
		Denotator analysis = getInput(0);
		
		if (analysis == null) {
            addError("Input denotator is null.");	            
        }
        else if (!analysis.hasForm(chordAnalysisSequenceForm)) {
            addError("Input denotator is not of form \"chordAnalysisSequence\".");
            analysis = null;
        } else {
        	path = analysis2path(analysis);	 	
        	write2file(path, lilyFile);       	
        }		
	}

		
	// writes the path to the lilypond file
	private void write2file(ArrayList<RMPoint> path, File file) {
		
		Chord chord;
		int size;
		int prevTonality = -1;
		double thisDuration, durationSoFarInMeasure=0;
		int measureNum = 1;
        		
        try {       	
        	PrintWriter out = new PrintWriter(file.getName());
        	
        	// calculate total duration of a measure
        	// based on lilyTempo and lilyMeter
        	// in order to put barlines accordingly.
        	double measureDur = measureDurationInSeconds(lilyTempo, lilyMeter);
        	System.out.println("measureDur = " + measureDur);
        	
        	out.println("%\\version \"2.20.0\"\n");
        	out.println("% " + file.getName() + "\n%\n");
        	out.print("% Created by LilypondFileOut Rubette of RUBATO on ");        	
        	out.println((new Date()).toString()+"\n");
        	
        	out.println("\\header {");
        	out.println("\t title = \"Harmonic Analysis Results\" ");
        	out.println("\t subtitle = \"Generated by LilypondOutFile Rubette on RUBATO\" ");
        	out.println("}\n");
        	
        	out.println("global = {");
        	out.println("\t\\key " + lilyKey);
        	out.println("\t\\time "+ lilyMeter);
        	out.println("}\n");
       	        	       	
        	out.println("allChords = {");
        	out.println("\t\\global");
        	
        	double prevOnset = path.get(0).chord.onset;
        	double prevDuration = path.get(0).chord.duration;
        	out.print(" % m. "+ measureNum + "\n");
        	        	
        	// first chord
    		out.print("<");
    		chord = path.get(0).chord;
    		size = chord.notes.size();
    		for (int i=0; i<size-1; i++) {
    			out.print(keynum2lilypitch(chord.notes.get(i).pitch_int) + " ");
    		}
    		out.print(keynum2lilypitch(chord.notes.get(size-1).pitch_int));
    		out.print(">");
    		out.print(duration2lilydur(chord.duration) + " ");
    		durationSoFarInMeasure += chord.duration;
      	       	        	
        	for (int p=1; p<path.size(); p++) {
        		// if the chord's onset is more than
        		// the previous chord's onset + duration
        		// then the difference is a rest.
    			if (path.get(p).chord.onset > (prevOnset + prevDuration)) {
	        		thisDuration = path.get(p).chord.onset - (prevOnset + prevDuration);
	        		out.print("r"+duration2lilydur(thisDuration)+" ");  
	        		prevOnset = prevOnset + prevDuration;
	        		prevDuration = thisDuration;
	        		p--;
    			} else {
	        		out.print("<");
	        		chord = path.get(p).chord;
	        		size = chord.notes.size();
	        		for (int i=0; i<size-1; i++) {
	        			out.print(keynum2lilypitch(chord.notes.get(i).pitch_int) + " ");
	        		}
	        		out.print(keynum2lilypitch(chord.notes.get(size-1).pitch_int));
	        		out.print(">");
	        		thisDuration = chord.duration;
	        		out.print(duration2lilydur(thisDuration) + " ");
	        		prevOnset = chord.onset;
	        		prevDuration = chord.duration;
    			}
    			durationSoFarInMeasure += thisDuration;
    			
    			if (durationSoFarInMeasure == measureDur) {
    				out.print("\n" + "| ");
    				out.print(" % m. "+ ++measureNum+ "\n");
    				durationSoFarInMeasure = 0.0;
    			}
        	}
        	
        	// all notes have been written, maybe a rest should follow
        	if (durationSoFarInMeasure < measureDur) {
        		out.println("r"+duration2lilydur(measureDur-durationSoFarInMeasure)+" "); 
        		out.print("|| ");
        	}        	
        	out.println("}\n");
        	
        	// analysis results as lyrics
		    out.println("analysis = \\lyricmode {");
		    out.print("\t");
        	for (RMPoint p : path) {
        		// out.print("\\markup \\with-color #darkblue ");
        		if (p.col != prevTonality) {       			
        			out.print(tonalities[p.col]+":");	
        			prevTonality = p.col;
        		} 
        		out.print("("+modsFuns[p.row]+")");
        		out.print(" ");
        	}
		    out.println("\n}\n");
		    
		    out.println("\\score {");
		    out.println("\t<<");
		    out.println("\t\t\\new Voice = \"AllTheChords\" {");
		    out.println("\t\t\t\\allChords");
		    out.println("\t\t}");
		    out.println("\t\t\\new Lyrics \\lyricsto \"AllTheChords\" {");
		    out.println("\t\t\t\\analysis");
		    out.println("\t\t}");
		    out.println("\t>>");
		    out.println("}");
		    
        	out.flush();
            out.close();
        } catch (IOException e) {
            addError("File %%1 could not be written to.", file.getName());
        }
	}

	
	/* 
	 * Read chords and analysis results 
	 * and write this to the file entered by the user.
	 */	
	private ArrayList<RMPoint> analysis2path(Denotator analysis) {
		
	   	List<Denotator> analysisResult = ((FactorDenotator)analysis).getFactors();
	   	
        path = new ArrayList<RMPoint>(analysisResult.size());

        for (Denotator d : analysisResult) {
        	 LimitDenotator chordAnalysis = (LimitDenotator) d;
        	 Denotator ch = chordAnalysis.getFactor(0);
        	 Chord chord = new Chord(ch);
         	 chord.onset = ((SimpleDenotator)chordAnalysis.getFactor(1)).getReal();
        	 chord.duration = ((SimpleDenotator)chordAnalysis.getFactor(2)).getReal();
        	 int row = ((SimpleDenotator)chordAnalysis.getFactor(3)).getInteger();
        	 int col = ((SimpleDenotator)chordAnalysis.getFactor(4)).getInteger();     	 
        	 RMPoint point = new RMPoint(0, chord,  row,  col, 0);
             path.add(point);	
         }
         return path;
 	}
	
	
	
	// calculate total seconds in a duration
	// lilyTempo is seconds per quarter note
	// lilyMeter is a string, "2/4", "3/8", etc.
	
	private double measureDurationInSeconds(double lilyTempo, String lilyMeter) {

		// get the string upto "/" into numerStr;
		// get the string after "/" into denomStr;
		String[] arrOfStr = lilyMeter.split("/", 2); 
		
		int numerator = Integer.parseInt(arrOfStr[0]);
		int denominator = Integer.parseInt(arrOfStr[1]);
		
		// total measure time is numerator*denominator-in-seconds.
		// denominator-in-seconds is (4/denominator) * lilyTempo.
				
		return ((4.0/denominator) * lilyTempo * numerator);		
	}
	
	
	
	// a whole note, or a dotted note or a tied note (with ~)
	private String duration2lilydur(double dur) {
		
		double howManyQuarters = dur / lilyTempo;
		String dots="";
		
		if (isWhole(howManyQuarters)) {			
			if (isPowerOfTwo ((int) howManyQuarters)) {
				int numQuarters = (int) howManyQuarters;
				if (numQuarters == 1) {
					return "4";					
				} else if (numQuarters == 2) {
					return "2";	
				} else if (numQuarters == 4) {
					return "1";
				} else if (numQuarters == 8) {
					return "c\\breve";
				} else if (numQuarters == 16) {
					return "c\\longa";
				} else {
					return "unidentified duration";
				}
			} else {
				// dotted or tied...
				// dotted if 11,111,110,
				// tied if 101,1001, 1010, etc..
				int numQuarters = (int) howManyQuarters;
				if (isDotted(numQuarters)) {
					
					// find longest duration note
					int log2 = (int) Math.floor(Math.log(numQuarters) / Math.log(2));
					int longestDur = (int) Math.pow(2, log2);
					
					int curDur = longestDur;
					while (curDur < dur) {
						curDur += longestDur/2;
						dots += ".";
						longestDur = longestDur/2;
					}
					return (String.valueOf(4/log2) + dots);
				} else {
					return "is tied";				
				}
			}
		} else if (isWhole(1.0/howManyQuarters)) {
			if (isPowerOfTwo ((int) (1.0/howManyQuarters))) {
				// 8, 16, 32, 64, ...
				int num = 4 * (int) (1.0/howManyQuarters);
				return String.valueOf(num);				
			} else {
				int numQuarters = (int) howManyQuarters;
				if (isDotted(numQuarters)) {
					// find longest duration note
					int log2 = (int) Math.floor(Math.log(numQuarters) / Math.log(2));
					int longestDur = (int) Math.pow(2, log2);
					int curDur = longestDur;
					while (curDur < dur) {
						curDur += longestDur/2;
						dots += ".";
						longestDur = longestDur/2;
					}
					return (String.valueOf(4/log2) + dots);
				} else {
					return "is tied";		
				}
			}
		} else {
			double numQuarters = howManyQuarters;
			if (isDotted(numQuarters)) {
				System.out.print("Duration = "+dur);
				// find longest duration note
				int log2 = (int) Math.floor(Math.log(numQuarters) / Math.log(2));
				double longestDur = Math.pow(2, log2);
				double curDur = longestDur;
				while (curDur < dur) {
					curDur += longestDur/2;
					dots += ".";
					longestDur = longestDur/2;
				}
				System.out.println(" durString = "+ String.valueOf((int) (4/Math.pow(2, log2))) + dots);
				return (String.valueOf(((int) (4/Math.pow(2, log2))) + dots));
			} else {
				System.out.print("Duration = "+dur);
				int log2 = (int) Math.floor(Math.log(numQuarters) / Math.log(2));
				int toAdd = log2;
				double longestDur = Math.pow(2, log2);
				double curDur = longestDur;
				while (curDur < dur) {
					curDur += longestDur/2;
					if (curDur > dur) {
						curDur -= longestDur/2;
					} 
					longestDur = longestDur/2;
					toAdd = toAdd-1;
				}		
				System.out.println(" durString = "+ String.valueOf((int) (4/Math.pow(2, log2))) + "~ " +
						String.valueOf((int) (4/Math.pow(2, toAdd))));
				return (String.valueOf((int) (4/Math.pow(2, log2))) + "~ " +
						String.valueOf((int) (4/Math.pow(2, toAdd))));
			}
		}	
	}


	
	private boolean isWhole(double number) {
		return (Math.floor(number) == Math.ceil(number));
	}

	private boolean isPowerOfTwo (int number)  { 
	    /* First x in the below expression is for the case when x is 0 */
	    return (number > 0 && ((number & (number - 1)) == 0));
	} 
	
	// 110,11,111,...
	private boolean isDotted(double number) {
		if (number > 1) {
			if (number % 2 == 0)
				return isDotted(number/2);
			else
				return (isPowerOfTwo(((int)number)+1));
		} else {
			double num = number;
			while (!isWhole(num))
				num *= 2;
			return isDotted(num);
		}	
	}


		
	/*
	 * pitch class of a pitch as letters
	 * 
	 * MIDI keynum 60 is middle C = c' in lilypond absolute mode.
	 */
	 private String keynum2lilypitch (int pitch_int) {
		 
		assert(pitch_int >= 0 && pitch_int <= 127);
		
		int val = pitch_int % 12;
		int octave = pitch_int / 12;
		
		String letter = "";
		switch (val) {
			case 0: letter += "c"; break;
			case 1: letter += "cis"; break;
			case 2: letter += "d"; break;
			case 3: letter += "dis"; break;
			case 4: letter += "e"; break;
			case 5: letter += "f"; break;
			case 6: letter += "fis"; break;
			case 7: letter += "g"; break;
			case 8: letter += "gis"; break;
			case 9: letter += "a"; break;
			case 10: letter += "ais"; break;
			case 11: letter += "b"; break;
		}
		
		// handle octave information
		switch (octave) {
			case 1: letter += ",,,"; break;
			case 2: letter += ",,"; break;
			case 3: letter += ","; break;
			case 4: letter += ""; break;
			case 5: letter += "'"; break;
			case 6: letter += "''"; break;
			case 7: letter += "'''"; break;
			case 8: letter += "''''"; break;
			case 9: letter += "'''''"; break;
		}		
		return letter;
	 }
		
	 
	public boolean hasProperties() {
        return true;
    }


	public JComponent getProperties() {
	    if (properties == null) {
	        properties = new JPanel();            
	        properties.setLayout(new BorderLayout());
	        
	        // filename
	        Box fileBox = new Box(BoxLayout.X_AXIS);
	        fileBox.setBorder(new TitledBorder(BorderFactory.createEmptyBorder(), "Lilypond output file"));
	        
	        fileNameField = new JTextField(getLilypondFileName());
	        fileNameField.setEditable(false);
	        fileNameField.setOpaque(true);
	        fileNameField.setBackground(Color.WHITE);
	        fileBox.add(fileNameField);
	
	        fileBox.add(Box.createHorizontalStrut(5));
	        
	        JButton browseButton = new JButton("Browse...");
	        browseButton.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                browseFile(properties);
	            } 
	        });
	        fileBox.add(browseButton);
	        
	        properties.add(fileBox, BorderLayout.NORTH);
	        selectedFile = new File(getLilypondFileName());
	        
	        // lilypond settings
	        JPanel lilyPanel = new JPanel();
	        lilyPanel.setLayout(new BoxLayout(lilyPanel, BoxLayout.Y_AXIS));	
	        lilyPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Lilypond score settings"));
	        	        
	        // tempo
	        JPanel tempoPanel = new JPanel();
	        tempoPanel.setLayout(new BoxLayout(tempoPanel, BoxLayout.X_AXIS));		        
			JLabel tempoLabel = new JLabel("Time for quarter note (in seconds):");
			tempoPanel.add(tempoLabel);    		    	
			tempoField = new JTextField("");
			tempoField.setText("");
			tempoField.setEditable(true);
			tempoField.setOpaque(true);
			tempoPanel.add(tempoField);					
			lilyPanel.add(tempoPanel);
			
			// meter
	        JPanel meterPanel = new JPanel();
	        meterPanel.setLayout(new BoxLayout(meterPanel, BoxLayout.X_AXIS));		        
			JLabel meterLabel = new JLabel("Meter (2/4, 3/4, 3/8, 7/4, etc.):");
			meterPanel.add(meterLabel);    		    	
			meterField = new JTextField("");
			meterField.setText("");
			meterField.setEditable(true);
			meterField.setOpaque(true);
			meterPanel.add(meterField);					
			lilyPanel.add(meterPanel);
			
			// key
	        JPanel keyPanel = new JPanel();
	        keyPanel.setLayout(new BoxLayout(keyPanel, BoxLayout.X_AXIS));		        
			JLabel keyLabel = new JLabel("Key (c \\major, aes \\minor):");
			keyPanel.add(keyLabel);    		    	
			keyField = new JTextField("");
			keyField.setText("");
			keyField.setEditable(true);
			keyField.setOpaque(true);
			keyPanel.add(keyField);					
			lilyPanel.add(keyPanel);

	        properties.add(lilyPanel, BorderLayout.SOUTH);
	    }
	    return properties;
	}
	
	    
	private String getLilypondFileName() {
	    String fileName = "";
	    if (lilyFile != null) {
	        try {
	            fileName = lilyFile.getCanonicalPath();
	        }
	        catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	    return fileName;
	}
	    
	    	
	protected void browseFile(JPanel props) {
	    if (fileChooser == null) {
	        createFileChooser();
	    }
	    fileChooser.setCurrentDirectory(currentDirectory);
	    int res = fileChooser.showSaveDialog(props);
	    if (res == JFileChooser.APPROVE_OPTION) {
	        selectedFile = fileChooser.getSelectedFile();
	        currentDirectory = fileChooser.getCurrentDirectory();
	        try {
	            fileNameField.setText(selectedFile.getCanonicalPath());
	        }
	        catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	}
	
	
	private void createFileChooser() {
	    fileChooser = new JFileChooser();
	    fileChooser.setFileFilter(new FileFilter() {
	        public boolean accept(File f) {
	            return f.isDirectory() || f.getName().endsWith(".ly");
	        }
	        public String getDescription() {
	            return "Lilypond File";
	        }
	    });        
	}
	
	
	public boolean applyProperties() {
	    lilyFile = selectedFile;	    
	    
	    String val = tempoField.getText();
	    if (!val.isEmpty())
	    	lilyTempo = Double.parseDouble(val);
	    
	    val = meterField.getText();
	    if (!val.isEmpty())
	    	lilyMeter = val;
	    
	    val = keyField.getText();	
	    if (!val.isEmpty())
	    	lilyKey = val;
	    
	    return true;
	}
	
	
	
	public void revertProperties() {
	    String fileName = " ";
	    try {
	        fileName = lilyFile.getCanonicalPath();
	    }
	    catch (IOException e) {
	    }
	    fileNameField.setText(fileName);
	    
	    // also other fields here...
	}

	
	
	private void setLilypondFile(String fileName) {
	       lilyFile = new File(fileName);
	}

	
	
	// Fields for XML input/output
    private final static String LILY_TEMPO = "Tempo";
    private final static String LILY_METER= "Meter";
    private final static String LILY_KEY = "Key";

		  	
	public Rubette fromXML(XMLReader reader, Element element) {
		
       Element child;
       String val;
       String fileName = null;
       
       child = XMLReader.getChild(element, "File"); 
       if (child != null) { 
    	   val = child.getAttribute(VALUE_ATTR);
    	   fileName = XMLReader.getStringAttribute(child, "name");
       }
            
       child = XMLReader.getChild(element, LILY_TEMPO);
       if (child != null) {      
	       val = child.getAttribute(VALUE_ATTR);
	       lilyTempo = Double.parseDouble(val);
       }
    	
       child = XMLReader.getChild(element, LILY_METER);
       if (child != null) {  
	       val = child.getAttribute(VALUE_ATTR);
	       lilyMeter = val;
       }
    	
       child = XMLReader.getChild(element, LILY_KEY);
       if (child != null) { 
	       val = child.getAttribute(VALUE_ATTR);
	       lilyKey = val;
       }
        
       LilypondFileOutRubette newRubette = new LilypondFileOutRubette();
       if (fileName != null) {
    	   newRubette.setLilypondFile(reader.toAbsolutePath(fileName));  
       }
       newRubette.lilyTempo = lilyTempo;
       newRubette.lilyMeter = lilyMeter;
       newRubette.lilyKey = lilyKey;

       return newRubette;
 	}
	
	
	public void toXML(XMLWriter writer) {
		writer.empty("File", "name", writer.toRelativePath(getLilypondFileName()));
    	writer.empty(LILY_TEMPO, VALUE_ATTR, lilyTempo);
    	writer.empty(LILY_METER, VALUE_ATTR, lilyMeter);
    	writer.empty(LILY_KEY, VALUE_ATTR, lilyKey);
 	}

	
	public Rubette duplicate() {
		LilypondFileOutRubette newRubette = new LilypondFileOutRubette();
        return newRubette;
	} 
		
	public void init() { }
	
    public String getGroup() {
        return "Score";
    }

    public String getName() {
        return "LilypondFileOut";
    }
    
    public String getShortDescription() {
        return "translate to Lilypond file";
    }
    
    public ImageIcon getIcon() {
        return icon;
    }
   
    public String getLongDescription() {
        return "The LilypondFileOut Rubette translates harmonic analysis results"
        		+ " of the HarmonicPath rubette"
        		+ " and writes them to a Lilypond file";
    }
}
