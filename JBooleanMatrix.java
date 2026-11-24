/* Copyright (C) 2018 Ruhan Ikeda
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

/* This is the boolean version of JRiemannMatrixDisplay.
 * Is written to hold the locus inclusion table in
 * HarmonicAnalysisModel rubette        --- July 2018 */

package org.rubato.rubettes.score;

import java.awt.Color;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;


public class JBooleanMatrix extends JPanel {
	
	private static final long serialVersionUID = 1L;
	public BooleanTableModel model;
	public JTable table;
	
	protected int SCALE;
	protected int FUNCTIONS;
	protected int MODES;
	
	private String[] colNames = null;
	private String[] rowNames = null;
	private Object[][] data = null;
	
	boolean DEBUG = false;
	
	
	/* 
	 * Table model for Boolean Matrix
	 */
	class BooleanTableModel extends AbstractTableModel implements TableModel, TableModelListener {		

		private static final long serialVersionUID = 1L;	
		
		public boolean isCellEditable(int row, int col) {
			 if (col == 0)	return false;
			 else return true;
	    }	    
		
	    public int getColumnCount() {
			return colNames.length;
		}	  
	    
	    public int getRowCount() {
			return data.length;
	    }	    
	    
	    public Object[][] getData() {
	    	return data;
	    }	
	    
	    public Class<?> getColumnClass(int col) {
	       return Boolean.class;  
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
		
		public void setRowNames() {
			for (int i=0; i<getRowCount(); i++)
				data[i][0] = rowNames[i];
		}
		
		
		public void refresh(){
			for (int i=0; i<this.getRowCount(); i++) {
				for (int j=0; j<this.getColumnCount(); j++) {
					fireTableCellUpdated(i,j);
				}
			}
		}
		
		public void reset() {
			for (int r=0; r < model.getRowCount(); r++) {
	            for (int c=0; c < model.getColumnCount()-1; c++) { 
	            	setValueAt((Boolean) true, r, c+1);
	            }
			}
		    refresh();	
		}
		
		
		public void tableChanged(TableModelEvent e) {
			// TODO Auto-generated method stub
			
		}		
	}
	
	
	
	// constructor
	public JBooleanMatrix(int[] tonalities, int[] modes, int[] functions) {
		
		colNames = new String[tonalities.length+1]; 
		ToString(tonalities, colNames);
		
		rowNames = new String[modes.length*functions.length];
		product(modes, functions, rowNames);
		
		SCALE = tonalities.length;
		MODES = modes.length;
		FUNCTIONS = functions.length;
		
		data = new Object[rowNames.length][colNames.length];
		
		model = new BooleanTableModel();
		model.setRowNames();
		
		table = new JTable(model);   
		model.addTableModelListener(model);
		
		JTableHeader header = table.getTableHeader();
		header.setBackground(Color.LIGHT_GRAY);	
		table.setGridColor(Color.DARK_GRAY);
	
		// Add col names
		for (int c=0; c<colNames.length; c++) {
			table.getColumnModel().getColumn(c).setHeaderValue(colNames[c]);
		}
				
		// Columns widths
		for (int c=0; c<table.getColumnCount(); c++) {
			table.getColumnModel().getColumn(c).setPreferredWidth(6);
		}
		
		this.setTableCellAlignment(JLabel.LEFT);
		setVisible(true);		
	}
	
	
	// Write column names
	private void ToString(int[] tonalities, String[] cols) {
		cols[0] = "";
		for (int i=1; i<=tonalities.length; i++)
			cols[i] = Integer.toString(tonalities[i-1]);		
		return;
	}


	
	// Print the table data
	public void printToBuffer(BufferedWriter out){
		for (int r=0; r < model.getRowCount(); r++) {
            for (int c=1; c < model.getColumnCount(); c++) {   	
            	Object o = model.getValueAt(r, c);
            	try {
					out.write(o.toString()+" ");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            try {
				out.write("\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
		
	
	
	// read from buffer into table
	public void readBuffer(BufferedReader in) {
		for (int r=0; r < model.getRowCount(); r++) {           	
            try {
				String line = in.readLine();
				// Now put blank-seperated strings in this line into
				// the current row
				String[] splits = line.split(" ");
									
				// first col is rowname
				for (int c=1; c < model.getColumnCount(); c++) {  
					model.setValueAt(splits[c-1], r, c);
				}
										
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}    
		model.refresh();
	}
		
	
	
	/*
	 * product of two string arrays (i.e. product of regular languages)
	 * ["cadde","fikir"]*["tepe","bostan"] = ["caddetepe","caddebostan","fikirtepe","fikirbostan"]
	 */
	private void product(int[] arr1, int[] arr2, String[] res) {	
		for (int i=0; i<arr1.length; i++)
			for (int j=0; j<arr2.length; j++)
				res[i*arr2.length+j] = Integer.toString(arr1[i])+"("+Integer.toString(arr2[j])+")";
	}



	class CustomRenderer extends DefaultTableCellRenderer 
	{
		private static final long serialVersionUID = 1L;
	
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	    {
	        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	        c.setBackground(Color.LIGHT_GRAY);
	        this.setHorizontalAlignment(JLabel.CENTER);
	        return c;
	    }
	}
	
		
	private void setTableCellAlignment(int alignment) {
	    
		DefaultTableCellRenderer rowNameRenderer, colRenderer;
		TableColumn rowNameColumn, otherColumn;
			
		// renderer for first column (row names)
		rowNameColumn = table.getColumnModel().getColumn(0);
		rowNameRenderer = new CustomRenderer();
		rowNameColumn.setCellRenderer(rowNameRenderer);
		
		// renderer for other columns 
		colRenderer = new DefaultTableCellRenderer();
		colRenderer.setHorizontalAlignment(alignment);
		
	    for (int i=1; i<table.getColumnCount(); i++){
	    	 otherColumn = table.getColumnModel().getColumn(i);
	    	 otherColumn.setCellRenderer(colRenderer);
	    }
	    // repaint to show table cell changes
	    updateUI();
	}
}


