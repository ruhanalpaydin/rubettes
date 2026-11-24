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


package org.rubato.rubettes.score;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.rubato.rubettes.util.GMINote;
import org.rubato.rubettes.util.Weight;


public class JHarmonicWeightList extends JFrame {
	
	private static final long serialVersionUID = 1L;
	public ArrayList<GMINote> notes;
	public ArrayList<Weight> weights;
	public JTextArea weightList;
	
	
	public JHarmonicWeightList (ArrayList<GMINote> noteSeq, ArrayList<Weight> weightSeq) {
		
		this.notes = noteSeq;
		this.weights = weightSeq;
				
		this.setTitle("Harmonic Weights of Individual Notes");
		this.setLayout(new BorderLayout());
		
		JPanel panel = new JPanel();
		panel.setBackground(Color.WHITE);
		panel.setLayout(new BorderLayout());
		this.weightList = new JTextArea(16,16);
		weightList.setBackground(Color.WHITE);
		weightList.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(weightList);
		 
		panel.add(scrollPane);
		this.add(panel);
	}
	
	
	public void view() {

		weightList.setText("Onset"+"\t"+ "Pitch"+ "\t" + "Weight"+"\n");
		weightList.append("========================\n");
		
		for (int index=0; index<weights.size(); index++ ) {
			GMINote note = weights.get(index).note;
			
			// Note note = weights.get(index).chord.notes.get(noteIndx);
					
			String str = String.format("%.4f", note.onset);
			weightList.append(str);
			weightList.append("\t");

			str = String.format("%d", note.pitch_int);
			weightList.append(str);
			weightList.append("\t");
			
			str = String.format("%.4f", weights.get(index).value);
			weightList.append(str);
			weightList.append("\n");
		}		
	}
}
