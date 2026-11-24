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
 *
 * Displays the harmonic weights of notes,
 * which are computed by HarmonicPath Rubette.
 * 
 * Also, has a number of settings under the harmonic weights:
 * Grid origin, grid mesh, grid-pen size, 
 * zoom-in, zoom-out, APPLY buttons.
 *
 */

package org.rubato.rubettes.score;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.rubato.rubettes.util.GMINote;
import org.rubato.rubettes.util.Weight;


public class JHarmonicWeight extends JPanel
				implements ActionListener, ListSelectionListener, Scrollable{

	private static final long serialVersionUID = 1L;
	
	private ArrayList<GMINote> notes = null;
	private ArrayList<Weight> weights = null;
	private int weightPenWidth = 12;
	private double gridMesh = 1.0;
	private double gridOrigin = 0.0;
	private int gridPenWidth = 1;
    private static final int VIEW_HEIGHT     = 400;

    protected JList<String> noteSeqList;
	protected JList<String> meterSeqList;
	protected DefaultListModel<String> noteSeqListModel;
	protected DefaultListModel<String> meterSeqListModel;
	protected int selectedNotes[] = null;
	protected JTextField gridMeshField;
	protected JTextField gridOriginField;
	protected JTextField gridPenSizeField;
	protected JTextField weightPenSizeField;
	protected JTextField quantField;
	protected JButton AButton, zoomInButton, zoomOutButton;
	protected JHarmonicWeightDisplay pianoRollPanel;
	protected JScrollPane settingsScroll;
	protected JSplitPane spHor2;


	// constructor
	public JHarmonicWeight(ArrayList<GMINote> notes, 
							ArrayList<Weight> weights,
							int weightPenWidth, 
							int gridPenWidth, 
							double gridOrigin, 
							double gridMesh,
							String weightName) {							
		this.notes = notes;
		this.weights = weights;	
		this.weightPenWidth = weightPenWidth;
		this.gridMesh = gridMesh;
		this.gridPenWidth = gridPenWidth;
		this.gridOrigin = gridOrigin;
		
		////\\\\\ WEIGHTS PANEL at NORTH /////\\\\\
		this.setLayout(new BorderLayout());
		
		JPanel weightsPanel = new JPanel();
		weightsPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.BLACK), weightName));
		weightsPanel.setLayout(new BoxLayout(weightsPanel, BoxLayout.Y_AXIS));
		
		pianoRollPanel = new JHarmonicWeightDisplay(notes, weights, 
												weightPenWidth, gridPenWidth, 
 												gridOrigin, gridMesh);
				
		JScrollPane pianoRollScroll = new JScrollPane(pianoRollPanel);
		pianoRollScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		pianoRollScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		weightsPanel.add(pianoRollScroll);
		
		add(weightsPanel, BorderLayout.NORTH);
		
 		////\\\\\ SETTINGS PANEL at SOUTH /////\\\\\
 		JPanel settingsPanel = new JPanel();	
 		settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
 		settingsPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Settings"));
        
 		settingsScroll = new JScrollPane(settingsPanel);
        settingsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        settingsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        settingsScroll.setPreferredSize(new Dimension(VIEW_HEIGHT, 100));
        
  		
 		//// LEFT Settings
 		JPanel leftPanel = new JPanel();	
 		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
 		      
         // grid mesh
        JPanel gridMeshPanel = new JPanel();	
        gridMeshPanel.setLayout(new BorderLayout());
		JLabel gridMeshTextArea = new JLabel();
		gridMeshTextArea.setText("Grid Mesh:");
		gridMeshTextArea.setBackground(new Color(240,240,240));	
		gridMeshTextArea.setAlignmentX(RIGHT_ALIGNMENT);
		gridMeshPanel.add(gridMeshTextArea, BorderLayout.WEST);		
		gridMeshField = new JTextField(5);
		gridMeshField.setText(Double.toString(gridMesh));
		gridMeshField.setEditable(true);
		gridMeshField.setOpaque(true);
		gridMeshField.setBackground(Color.WHITE);
		gridMeshPanel.add(gridMeshField, BorderLayout.EAST);		 
		leftPanel.add(gridMeshPanel);
		 
		// grid origin
		JPanel gridOriginPanel = new JPanel();	
		gridOriginPanel.setLayout(new BorderLayout());
		JLabel gridOriginTextArea = new JLabel();
		gridOriginTextArea.setText("Grid Origin:");
		gridOriginTextArea.setBackground(new Color(240,240,240));
		gridOriginPanel.add(gridOriginTextArea, BorderLayout.WEST);		
		gridOriginField = new JTextField(5);
		gridOriginField.setText(Double.toString(gridOrigin));
		gridOriginField.setEditable(true);
		gridOriginField.setOpaque(true);
		gridOriginField.setBackground(Color.WHITE);
		gridOriginPanel.add(gridOriginField, BorderLayout.EAST);
		leftPanel.add(gridOriginPanel);
		 
		// grid pen size
		JPanel gridPenSizePanel = new JPanel();		 
		gridPenSizePanel.setLayout(new BorderLayout());
		JLabel gridPenSizeTextArea = new JLabel();
		gridPenSizeTextArea.setText("Grid Pen Width:");
		gridPenSizeTextArea.setBackground(new Color(240,240,240));
		gridPenSizePanel.add(gridPenSizeTextArea, BorderLayout.WEST);		
		gridPenSizeField = new JTextField(5);
		gridPenSizeField.setText(Integer.toString(gridPenWidth));
		gridPenSizeField.setEditable(true);
		gridPenSizeField.setOpaque(true);
		gridPenSizeField.setBackground(Color.WHITE);
		gridPenSizePanel.add(gridPenSizeField, BorderLayout.EAST);
		leftPanel.add(gridPenSizePanel);

		// weight pen size
		JPanel weightPenSizePanel = new JPanel();	
		weightPenSizePanel.setLayout(new BorderLayout());
		JLabel weightPenSizeTextArea = new JLabel();
		weightPenSizeTextArea.setText("Weight Circle Diameter:");
		weightPenSizeTextArea.setBackground(new Color(240,240,240));		
		weightPenSizePanel.add(weightPenSizeTextArea, BorderLayout.WEST);		
		weightPenSizeField = new JTextField(5);
		weightPenSizeField.setText(Integer.toString(weightPenWidth));
		weightPenSizeField.setEditable(true);
		weightPenSizeField.setOpaque(true);
		weightPenSizeField.setBackground(Color.WHITE);
		weightPenSizePanel.add(weightPenSizeField, BorderLayout.EAST);
		leftPanel.add(weightPenSizePanel);
					
		settingsPanel.add(leftPanel);	
		settingsPanel.add(new JSeparator(JSeparator.VERTICAL));
				
		////\\\\\ MIDDLE PANEL (ZOOM PANEL)
		JPanel zoomPanel = new JPanel();	
		zoomPanel.setLayout(new BoxLayout(zoomPanel, BoxLayout.X_AXIS));			
				
		// apply button		 
		ImageIcon applyIcon = new ImageIcon("/Users/umup/eclpse-workspace/rubato-src-20070720/java/src/org/rubato/rubettes/util/gear.png");
		AButton = new JButton(applyIcon);
		AButton.setText("APPLY");
		AButton.setForeground(Color.LIGHT_GRAY);
		AButton.setVerticalTextPosition(AbstractButton.BOTTOM);
        AButton.setHorizontalTextPosition(AbstractButton.CENTER);
		AButton.addActionListener(this);
		zoomPanel.add(AButton);
		
		ImageIcon zoomInIcon = new ImageIcon("/Users/umup/eclpse-workspace/rubato-src-20070720/java/src/org/rubato/rubettes/util/zoom-in.gif");
 		zoomInButton = new JButton(zoomInIcon);
 		zoomInButton.setText("Zoom  In");
 		zoomInButton.setForeground(Color.GRAY);
 		zoomInButton.setVerticalTextPosition(AbstractButton.BOTTOM);
        zoomInButton.setHorizontalTextPosition(AbstractButton.CENTER);
 		zoomInButton.addActionListener(this);
		zoomPanel.add(zoomInButton);
				
		ImageIcon zoomOutIcon = new ImageIcon("/Users/umup/eclpse-workspace/rubato-src-20070720/java/src/org/rubato/rubettes/util/zoom-out.gif");		
		zoomOutButton = new JButton(zoomOutIcon);
		zoomOutButton.setText("Zoom Out");
		zoomOutButton.setForeground(Color.GRAY);
 		zoomOutButton.setVerticalTextPosition(AbstractButton.BOTTOM);
        zoomOutButton.setHorizontalTextPosition(AbstractButton.CENTER);
		zoomOutButton.addActionListener(this);
		zoomPanel.add(zoomOutButton);
		
		settingsPanel.add(zoomPanel);
		settingsPanel.add(new JSeparator(JSeparator.VERTICAL));
				
		add(settingsPanel, BorderLayout.CENTER);	
	}

	public Dimension getPreferredScrollableViewportSize() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean getScrollableTracksViewportWidth() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean getScrollableTracksViewportHeight() {
		// TODO Auto-generated method stub
		return false;
	}

	public void valueChanged(ListSelectionEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		// When Apply button is pressed
		// Weight graph params are updated
		// Weight graph should be redrawn
		
		Object src = e.getSource();
		
		if (src == AButton) {
			// redraw the Weight Graph 
			this.gridMesh = Double.parseDouble(gridMeshField.getText());
			this.gridPenWidth = Integer.parseInt(gridPenSizeField.getText());
			this.gridOrigin = Double.parseDouble(gridOriginField.getText());
			this.weightPenWidth = Integer.parseInt(weightPenSizeField.getText());

	    	pianoRollPanel.setScore(notes, weights, 
	    								weightPenWidth, gridPenWidth,  
										gridOrigin, gridMesh);
		} else if (src == zoomInButton) {
			pianoRollPanel.zoomIn();
		} else if (src == zoomOutButton) {
			pianoRollPanel.zoomOut();
		} 
	}	
}
