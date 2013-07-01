package com.rf.real;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class DescribeTrees {
//method to take the txt fle as input and pass those values to random forests
  BufferedReader BR = null;
	String path;
	public DescribeTrees(String path){
		this.path=path;
	}
	public ArrayList<int[]> CreateInput(String path){

		ArrayList<int[]> DataInput = new ArrayList<int[]>();
		
		try {
		 
		String sCurrentLine;
		BR = new BufferedReader(new FileReader(path));

		while ((sCurrentLine = BR.readLine()) != null) {
			ArrayList<Integer> Sp=new ArrayList<Integer>();int i;
			if(sCurrentLine!=null){
				sCurrentLine=" "+sCurrentLine+" ";
				for(i=0;i<sCurrentLine.length();i++){
					if(Character.isWhitespace(sCurrentLine.charAt(i)))
						Sp.add(i);
				}int[] DataPoint = new int[Sp.size()-1];
				for(i=0;i<Sp.size()-1;i++){
					DataPoint[i]=Integer.parseInt(sCurrentLine.substring(Sp.get(i)+1, Sp.get(i+1)));
				}DataInput.add(DataPoint);//for(int t=0;t<DataInput.get(0).length;t++){System.out.print(DataInput.get(0)[t]+",");}System.out.println("");
			}
		}

	} catch (IOException e) {
		e.printStackTrace();
	} finally {
		try {
			if (BR != null)BR.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	return DataInput;
}
}
