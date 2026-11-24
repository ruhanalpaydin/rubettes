/* Copyright (C) 2018 Ruhan Ikeda
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
*
*
* This is part of View of HarmonicPath rubette
* and one of the two outputs of the HarmonicPath rubette
* (the other output is displayed through JHarmonicAnalysisDisplay)
* 
* Harmonic Weights are displayed through this class.
*
*/



package org.rubato.rubettes.score;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import org.rubato.rubettes.util.GMINote;
import org.rubato.rubettes.util.Weight;


public class JHarmonicWeightDisplay extends JPanel 
									implements Scrollable {
	
	private static final long serialVersionUID = 1L;
	
	private ArrayList<GMINote> notes = null;
	private ArrayList<Weight> weights = null;
	private int width = 0;  
	private int weightPenWidth = 5;
	private double gridMesh = 1.0;
	private double gridOrigin = 0.0;
	private int gridPenWidth = 1;
	private final int X_MARGIN = 5;
	private final int Y_MARGIN = 5;	
	private int    stretch = DEFAULT_STRETCH;
	private static final int DEFAULT_STRETCH = 8;
    private static final int BAR_HEIGHT      = 4;
    private static final int VIEW_HEIGHT     = 120*BAR_HEIGHT;
    private static final Dimension MINIMUM_SIZE  = new Dimension(VIEW_HEIGHT, VIEW_HEIGHT);
    private Dimension currentSize = MINIMUM_SIZE;
    
    
    private static final int colorCount = 16;
        
    private static final float values[] = {
            0.05f, 0.01f, 0.15f, 0.2f, 0.25f, 0.3f, 0.35f, 0.4f,
            0.45f, 0.5f, 0.55f, 0.6f, 0.65f, 0.7f, 0.75f, 0.8f
    };    
    private static Color gridColor = Color.pink;                   
    private static final Color[] voiceWeightColors;        
    static {
        voiceWeightColors = new Color[colorCount];
        for (int j = 0; j < colorCount; j++) {
            	voiceWeightColors[j] = Color.getHSBColor(0.0f, 0.0f, values[15-j]);
        }
    }
    private boolean DEBUG = false;
   
	// constructor
	public JHarmonicWeightDisplay(ArrayList<GMINote> notes, 
							ArrayList<Weight> weights,
							int weightPenWidth, 
							int gridPenWidth, 
							double gridOrigin, 
							double gridMesh) {							
		this.notes = notes;
		this.weights = weights;		
		this.weightPenWidth = weightPenWidth;
		this.gridMesh = gridMesh;
		this.gridPenWidth = gridPenWidth;
		this.gridOrigin = gridOrigin;
		
		setOpaque(true);
        setBackground(Color.WHITE);
        
        if (notes == null)
        	width = this.stretch*10;
        else {
	        double totalDuration = notes.get(notes.size()-1).onset + notes.get(notes.size()-1).duration;
	        width = (int)(totalDuration*this.stretch);
	        while (width < VIEW_HEIGHT) { 
	        	stretch *= 2;
	        	totalDuration = notes.get(notes.size()-1).onset + notes.get(notes.size()-1).duration;
	            width = (int)(totalDuration*this.stretch);           
	        }   
        }
        currentSize = new Dimension(width, VIEW_HEIGHT);
        setSize(currentSize);
        setPreferredSize(currentSize);
        setMaximumSize(currentSize);
        setMinimumSize(currentSize);
	}
	
	protected void paintComponent(Graphics g) {
		if (notes != null) {
	        super.paintComponent(g);
	        Graphics2D g2d = (Graphics2D)g;
	        g.setColor(Color.WHITE);
	        g.fillRect(0, 0, getWidth(), getHeight());
	        drawGrids(g2d);
	        drawNotes(g2d);
		}
    }
	
	private void updateSize() {

		double totalDuration = notes.get(notes.size()-1).onset + notes.get(notes.size()-1).duration;
        width = (int)(totalDuration*this.stretch);
        currentSize = new Dimension(width, getHeight());
        setSize(currentSize);
        setPreferredSize(currentSize);
        setMaximumSize(currentSize);
        setMinimumSize(currentSize);
        repaint();
	}
	
	public void setScore(ArrayList<GMINote> notes, 
							ArrayList<Weight> weights,
							int weightPenWidth,
							int gridPenWidth, 
							double gridOrigin, 
							double gridMesh) {		
			this.notes = notes;
			this.weights = weights;
			this.weightPenWidth = weightPenWidth;
			this.gridMesh = gridMesh;
			this.gridPenWidth = gridPenWidth;
			this.gridOrigin = gridOrigin;			
			updateSize();
	}
	    
	private void drawGrids(Graphics2D g) {
		int height = this.getHeight();
		int width = this.getWidth();

		int leftMargin = X_MARGIN;
 		int rightMargin = X_MARGIN;
 		int topMargin = Y_MARGIN;
 		int bottomMargin = Y_MARGIN;

        g.setColor(gridColor);
        double maxOnset = notes.get(notes.size()-1).onset;
        
        int numGrids = (int) ((maxOnset-gridOrigin)/gridMesh);
        
        for (int gnum=0; gnum<numGrids+1; gnum++) {
        	int x = (int) (((((double)gnum*gridMesh)+gridOrigin) / maxOnset) * (double) (width-leftMargin-rightMargin));
	        for (int pen=0; pen< gridPenWidth; pen++) {
				g.drawLine(x+leftMargin+pen, bottomMargin, 
							x+leftMargin+pen, height-topMargin);
			}
        }
	}
    
    private void drawNotes(Graphics2D g) {
    	
        if (notes == null) {
            return;
        }             
        if (DEBUG) System.out.println("Num notes = "+notes.size());
        if (DEBUG) System.out.println("Num weights = "+weights.size());
        int width = this.getWidth();		
		int leftMargin = X_MARGIN;
 		int rightMargin = X_MARGIN;
 		int topMargin = Y_MARGIN;
 		int bottomMargin = Y_MARGIN;

 		// find maximum weight and minimum weight
		double maxWeight = weights.get(0).value;
    	double minWeight = weights.get(0).value;
        for (Weight w : weights) {
        	if (w.value > maxWeight)
        		maxWeight = w.value;
        	if (w.value < minWeight)
         		minWeight = w.value;
        }	
                
        // scale weights to [0.0 - 1.0] interval
        // minWeight will be 0.0
        // maxWeight will be 1.0
        for (Weight w : weights) {
        	w.value = (w.value-minWeight)/(maxWeight-minWeight);
        }

        double maxOnset = notes.get(notes.size()-1).onset;
        
		 // draw notes	
		for (int index=0; index<weights.size(); index++ ) {
			GMINote note = weights.get(index).note;			
			int x = (int) ((note.onset / maxOnset) * (double) (width-leftMargin-rightMargin));
			int y = VIEW_HEIGHT-note.pitch_int*BAR_HEIGHT-BAR_HEIGHT-topMargin;
	        setVoiceWeightColor(g, weights.get(index).value);
	        g.fill(new Ellipse2D.Float(x, y, weightPenWidth, weightPenWidth));
		}
    }
    
    private void setVoiceWeightColor(Graphics2D g, double weight) {
    	// scale weights to [0, colorCount-1] interval
        int wColor = (int) (weight*(colorCount-1));
        g.setColor(voiceWeightColors[wColor]);
    }
            
	public Dimension getPreferredSize() {
        return currentSize;
    }
	
	public Dimension getPreferredScrollableViewportSize() {
		 return currentSize;
	}
			
	public void zoomIn() {	        
		this.stretch = Math.min(256, this.stretch*2);
        updateSize();	        
	}
        
	public void zoomOut() {	        
		this.stretch = Math.max(2, this.stretch/2);
        updateSize();
	}

	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 50;
	}

	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 50;
	}

	public boolean getScrollableTracksViewportWidth() {
		return false;
	}

	public boolean getScrollableTracksViewportHeight() {
		return false;
	}
}
