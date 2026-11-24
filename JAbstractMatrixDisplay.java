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

package org.rubato.rubettes.score;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;


public class JAbstractMatrixDisplay extends JPanel 
	implements ActionListener{
	
	private static final long serialVersionUID = 1L;
	public AbstractTableModel model;
	public JTable table;
	
	
	private String[] colNames = null;
	private String[] rowNames = null;
	private Object[][] data = null;
	
	boolean DEBUG=false;
	
	
	/*
	 * Table model for Matrix
	 */
	class AbstractTableModel extends DefaultTableModel {
		
		private static final long serialVersionUID = 1L;
	
		public boolean isCellEditable(int row, int col) {
			 if (col == 0)
				 return false;
			 else
				 return true;
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
	    
	    // gets data part only - not row names
		public Object getValueAt(int row, int col) {
			return data[row][col];
		}
		
		public int getIntValueAt(int row, int col) {
				Object val =  data[row][col];
				String s = val.toString();
				return Integer.parseInt(s);
		}
		
		
		public double getDoubleValueAt(int row, int col) {
				Object val =  data[row][col];
				String s = val.toString();
				return Double.parseDouble(s);
		}
		
		
		public boolean getBooleanValueAt(int row, int col) {
						Object val =  data[row][col];
						String s = val.toString();
						return Boolean.parseBoolean(s);
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
		
	}
	
	
	
	// constructor
	public JAbstractMatrixDisplay(int[] colNaming, String[] rowNaming, Object[][] dat) {
		
		colNames = new String[colNaming.length+1]; 
		ToString(colNaming, colNames, "");
		rowNames = new String[rowNaming.length];
		System.arraycopy(rowNaming, 0, rowNames, 0, rowNaming.length);
		this.data = new Object[rowNames.length][colNames.length];
		for (int i = 0; i < dat.length; i++) {
			// double to Object
	        System.arraycopy(dat[i], 0, data[i], 1, dat[i].length);
	    }
		
		
		model = new AbstractTableModel();
		model.setRowNames();
		
		table = new JTable(model);        	
		
		JTableHeader header = table.getTableHeader();
		header.setBackground(Color.LIGHT_GRAY);	
		table.setGridColor(Color.DARK_GRAY);
	
		// Add col names
		for (int c=0; c<colNames.length; c++) {
			table.getColumnModel().getColumn(c).setHeaderValue(colNames[c]);
		}
				
		// Columns widths
		for (int c=0; c<table.getColumnCount(); c++) {
			table.getColumnModel().getColumn(c).setPreferredWidth(10);
		}
		
		this.setTableCellAlignment(JLabel.LEFT);
		setVisible(true);		
	}
	
	
		
	
	// Write row/col names
	private void ToString(int[] data, String[] names, String first) {
		names[0] = first;
		for (int i=1; i<=data.length; i++)
			names[i] = Integer.toString(data[i-1]);		
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
				e.printStackTrace();
			}
		}
	}
		
	
	// Print the table data
	public void printToArray(Double[][] array){
		
		System.out.println("row number = "+ model.getRowCount() );
		System.out.println("col number = "+ model.getColumnCount() );
		for (int r=0; r < model.getRowCount(); r++) {
			System.out.println("row = "+ r);
			for (int c=1; c < model.getColumnCount()-1; c++) {   	
				System.out.println("col = "+ c);
				String str = model.getValueAt(r, c).toString();
				array[r][c-1] = (Double) Double.parseDouble(str);
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
	
	
	
	public void reset(int[] val) {
		for (int r=0; r < model.getRowCount(); r++) {
            for (int c=0; c < model.getColumnCount()-1; c++) {   	
            	model.setValueAt(val[r*(model.getColumnCount()-1)+c], r, c+1);
            }
		}
		model.refresh();
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
	
	
	
	/*
	 * display the given rMatrix 
	 */
	public void display(AbstractRiemannMatrix rMatrix) {
		DecimalFormat df = new DecimalFormat("#.##");
		for (int r=0; r<rMatrix.rowCount; r++)
			for (int c=0; c<rMatrix.colCount; c++)
				model.setValueAt(df.format(rMatrix.data[r][c]), r, c+1);
		this.model.refresh();
	}
	
	
	public void print() {
		System.out.println("-------- MATRIX --------");
		for (int row=0; row<rowNames.length; row++) {
			for (int col=0; col<colNames.length; col++) {
				System.out.print(data[row][col]+" ");
			}
			System.out.println();
		}
		System.out.println("-------- END --------");
	}
	
		
	@Override
	public void actionPerformed(ActionEvent arg0) {
	}
}
