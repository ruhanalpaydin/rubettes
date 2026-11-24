/* Copyright (C) 2011-2013 Ruhan Ikeda
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
 * Displays the resultant harmonic path,
 * evaluated by HarmonicPath rubette.
 * 
 * 2021 Feb: 
 * =========
 * Adding the ability to show window paths (beneath the cursor), when a cell is clicked
 *
 */

package org.rubato.rubettes.score;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import java.awt.event.MouseEvent;
import javax.swing.event.ListSelectionEvent;



public class JHarmonicAnalysisDisplay extends JPanel {
		
	private static final long serialVersionUID = 1L;

	private ArrayList<RMPoint> bestPath = null;
	public ArrayList<ArrayList<RMPoint> > windowPaths = null;	
	private HarmonicAnalysisTableModel model;
	public JTable table;	
	private String[] colNames = {"F","C","G","D","A","E","B","F#","C#","G#","D#","A#"};
	private String[] tonalities = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
	private int colShift = 2; // The column names are: chord# | onset | F | C | G |.....
	private int[] tons = null;
	private int[] modes = null;
	private int[] funs = null;
   
	// table data
	private Object[][] data = null; 	
	
	// size of window is 100-chord long
	final int MIN_CHORD_NUM = 100;	
	private int selectedRow;
	private Popup popup;
	    
    // for debugging
	private final boolean DEBUG = false;
	
	// Table model for Harmonic Analysis
	class HarmonicAnalysisTableModel extends DefaultTableModel {
		
		private static final long serialVersionUID = 1L;	
		
		public boolean isCellEditable(int row, int col) {
	         return false;
	    }		
	    public int getColumnCount() {
	    	return data[0].length;
		}	    
	    public int getRowCount() {
			return data.length;
	    }	    
	    public Object[][] getData() {
	    	return data;
	    }	    
	    // gets data part only - not row names
		public Object getValueAt(int row, int col) {
			return data[row][col];
		}		
		// sets data part only -- not row names
		public void setValueAt(Object value, int row, int col) {
	        data[row][col] = value;
	        fireTableCellUpdated(row, col);
	    }		
		// sets row names (first column in the table)
		public void setRowNames() {
			for (int i=0; i<getRowCount(); i++)
				data[i][0] = i+1;
		}				
		public void clear() {
			for (int r=0; r<getRowCount(); r++) {				
				for (int c=0; c<getColumnCount(); c++) {					
					setValueAt("", r, c);
				}
			}	
		}
	}
	
	
	
	/* 
	 * Constructor:
	 *
	 * If numChords is less than 0, display an empty table of width=20 chords
	 * Otherwise display analysis results in the table
	 */
	
	public JHarmonicAnalysisDisplay(ArrayList<RMPoint> bestPath,
									ArrayList<ArrayList<RMPoint>> windowP) {
						
		this.bestPath = bestPath;
		this.windowPaths = windowP;
		
		setLayout(new BorderLayout());
		int colShift = 2; // chord # + onset
		
		// table is of size, at least MIN_CHORD_NUM
		if ((bestPath == null) || (bestPath.size() < MIN_CHORD_NUM))
			data = new Object[MIN_CHORD_NUM][colNames.length+colShift];
		else
			data = new Object[bestPath.size()][colNames.length+colShift];
		
		// create a model
		model = new HarmonicAnalysisTableModel();
		
		// set rownames
		model.setRowNames();
		
		// create the table wrt the model
		table = new JTable(model); 
		
		table.addMouseListener(new MouseAdapter() {
      
			public void mouseClicked(MouseEvent e) {
            	// System.out.println("SelectedRow = "+selectedRow);
            	if (selectedRow >= windowPaths.size()) return;
            	
                if (popup != null) {
                    popup.hide();
                }              
                String str = "<HTML>";
                for (int i=0; i<windowPaths.get(selectedRow).size(); i++) {
                	RMPoint point = windowPaths.get(selectedRow).get(i);
                	str += tonalities[point.col]+":";
        			int modeNum = point.row / funs.length;
        			int funNum = point.row % funs.length; 
        			str += Integer.toString(modes[modeNum])+ "(" + Integer.toString(funs[funNum])+ ")<BR>";
                }
                str += "</HTML>";
                JLabel text = new JLabel(str);
                popup = PopupFactory.getSharedInstance().getPopup(e.getComponent(), text, e.getXOnScreen(), e.getYOnScreen());
                popup.show();
            }
        });
		
		table.getSelectionModel().addListSelectionListener(new RowListener());
		
		// place the table into a pane
		JScrollPane analysisPane = new JScrollPane(table);
		analysisPane.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Harmonic Analysis Result"));       
        add(analysisPane, BorderLayout.CENTER);
		setTableHeader();
		table.setGridColor(Color.DARK_GRAY);
		setTableCellAlignment(JLabel.LEFT);
		
	    TableColumn tColumn = table.getColumnModel().getColumn(0);
	    tColumn.setCellRenderer(new ColumnColorRenderer(Color.lightGray, Color.BLACK));
	    tColumn = table.getColumnModel().getColumn(1);
	    tColumn.setCellRenderer(new ColumnColorRenderer(Color.lightGray, Color.BLACK));	 	    
	}
	
	
	private class RowListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
        	
            if (event.getValueIsAdjusting()) {
                return;
            }
            selectedRow = table.getSelectedRow();  
        }       
    }
	
	
	class ColumnColorRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;
		Color backgroundColor, foregroundColor;
		public ColumnColorRenderer(Color backgroundColor, Color foregroundColor) {
		      super();
		      this.backgroundColor = backgroundColor;
		      this.foregroundColor = foregroundColor;
		}
		public Component getTableCellRendererComponent(JTable table, Object value, 
							boolean isSelected, boolean hasFocus, 
							int row, int column) {
		      Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		      cell.setBackground(backgroundColor);
		      cell.setForeground(foregroundColor);
		      return cell;
		}
	 }

	
	// Set the table header as Chord1, Chord2, ...
	public void setTableHeader() {
		TableColumnModel colModel = table.getColumnModel();		
		colModel.getColumn(0).setHeaderValue("Chord #");
		colModel.getColumn(1).setHeaderValue("Onset");
		
		for (int c=colShift; c<model.getColumnCount(); c++) {
			colModel.getColumn(c).setHeaderValue(colNames[c-colShift]);
		}			
		table.getTableHeader().setBackground(Color.LIGHT_GRAY);	
	}
	
	
	// Resize the data of the table -- if necessary
	// We know that assert(bestPath.size() > model.getColumnCount()) = true		
	public void updateTable(ArrayList<RMPoint> bestPath) {
				
		if (DEBUG) 	System.out.println("Updating table size to "+bestPath.size()+" chords");		
		
		data = new Object[bestPath.size()][colNames.length+2];
		model.fireTableDataChanged();
		this.bestPath = bestPath;		
	}
	
	
	
	/*
	 * display the given rMatrix 
	 * we know that assert(bestPath != null) is true
	 */
	 
	public void display(ArrayList<RMPoint> bestPath, 
						int[] tons, int[] modes, int[] funs) {
		
		this.tons = tons;
		this.modes = modes;
		this.funs = funs;
		
		// enlarge table data if necessary
		if (bestPath.size() > model.getRowCount()) {
			updateTable(bestPath);
		}
		
		// clear the table
		model.clear();
		model.setRowNames();
		
		// add data to table
		int c = 0;
		String value = null;
		for (RMPoint p : bestPath) {
			if (DEBUG) System.out.println("point# "+c);			
			// write onset
			String strOnset = String.format("%.2f", p.chord.onset);
			model.setValueAt(strOnset, c, 1);
			// decompose p.row into (fun#,mode#) form
			int modeNum = p.row / funs.length;
			int funNum = p.row % funs.length; 
			value = Integer.toString(modes[modeNum])+ "(" + Integer.toString(funs[funNum])+ ")";
			model.setValueAt(value, c, this.col2fifths(p.col, colShift));
			c++;
		}		
	}
	
	

	// display the given rMatrix with onsets only
	public void displayTable(ArrayList<AbstractRiemannMatrix> matrices, int[] tons, int[] modes, int[] funs) {
		
		// clear the table
		model.clear();
		model.setRowNames();
		
		// add data to table
		int c = 0;
		for (AbstractRiemannMatrix m : matrices) {
			if (DEBUG) System.out.println("point# "+c);			
			String strOnset = String.format("%.2f", m.chord.onset);
			model.setValueAt(strOnset, c, 1);
			c++;
		}
	}

	
	
	public void displayBestPath(ArrayList<RMPoint> bestPath, ArrayList<AbstractRiemannMatrix> matrices, int[] tons, int[] modes, int[] funs) {

		int currRow = 0;
		int writeToRow = 0;
		String value = null;

		// add bestPath to table
		for (RMPoint p : bestPath) {
			// decompose p.row into (fun#,mode#) form
			int modeNum = p.row / funs.length;
			int funNum = p.row % funs.length; 
			value = Integer.toString(modes[modeNum])+ "(" + Integer.toString(funs[funNum])+ ")";

			boolean found = false;
			// find the row in the table
			for (int m=currRow; m<matrices.size() && !found; m++) {
				if (p.chord.onset == matrices.get(m).chord.onset) {
					writeToRow = m;
					currRow = m+1;
					found = true;
				}
			}
			model.setValueAt(value, writeToRow, this.col2fifths(p.col, colShift));			
		}
	}
	
	
	
	/*
	 * Harmonic Analysis Results matrix row names from descending order 
	 * to cycle of fifths order
	 */
	private int col2fifths (int col, int shift) {
				
		switch (col) {
			case 5: return 0+shift;
			case 0: return 1+shift;
			case 7: return 2+shift;
			case 2: return 3+shift;
			case 9: return 4+shift;
			case 4: return 5+shift;
			case 11: return 6+shift;
			case 6: return 7+shift;
			case 1: return 8+shift;
			case 8: return 9+shift;
			case 3: return 10+shift;
			case 10: return 11+shift;
			default: return -1;	
		}
 	}	

	
	
	class CustomRenderer extends DefaultTableCellRenderer 
	{
		private static final long serialVersionUID = 1L;

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	    {
	        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	        c.setBackground(Color.LIGHT_GRAY);
	        this.setHorizontalAlignment(JLabel.LEFT);
	        return c;
	    }
	}
	
		
	private void setTableCellAlignment(int alignment) {
	    
		DefaultTableCellRenderer rowNameRenderer, onsetRenderer, colRenderer;
		TableColumn rowNameColumn, onsetColumn, otherColumn;
			
		// renderer for first column (row names)
		rowNameColumn = table.getColumnModel().getColumn(0);
		rowNameRenderer = new CustomRenderer();
		rowNameColumn.setCellRenderer(rowNameRenderer);
		
		onsetColumn = table.getColumnModel().getColumn(1);
		onsetRenderer = new CustomRenderer();
		onsetColumn.setCellRenderer(onsetRenderer);
		
		// renderer for other columns 
		colRenderer = new DefaultTableCellRenderer();
		colRenderer.setHorizontalAlignment(alignment);
		
	    for (int i=colShift; i<table.getColumnCount(); i++) {
	    	 otherColumn = table.getColumnModel().getColumn(i);
	    	 otherColumn.setCellRenderer(colRenderer);
	    }
	}	
}