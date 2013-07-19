package com.rf.fast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class DescribeTrees {
  //method to take the txt fle as input and pass those values to random forests
		BufferedReader BR = null;
		String path;
		String layout;
		
		public DescribeTrees(String path, String layout){
			this.path=path;
			this.layout = layout;
		}
		public ArrayList<ArrayList<String>> CreateInputCateg(String path, String layout){

			ArrayList<ArrayList<String>> DataInput = new ArrayList<ArrayList<String>>();
			
			try {
			 
			String sCurrentLine;
			BR = new BufferedReader(new FileReader(path));
			
			while ((sCurrentLine = BR.readLine()) != null) {
				ArrayList<Integer> Sp=new ArrayList<Integer>();int i;
				if(sCurrentLine!=null){
					if(sCurrentLine.indexOf(",")>=0){
						//has comma
						
						sCurrentLine=","+sCurrentLine+",";
						char[] c =sCurrentLine.toCharArray();
						for(i=0;i<sCurrentLine.length();i++){
							if(c[i]==',')
								Sp.add(i);
						}ArrayList<String> DataPoint=new ArrayList<String>();
						for(i=0;i<Sp.size()-1;i++){
							DataPoint.add(sCurrentLine.substring(Sp.get(i)+1, Sp.get(i+1)).trim());
						}DataInput.add(DataPoint);//System.out.println(DataPoint);
					}
					else if(sCurrentLine.indexOf(" ")>=0){
						//has spaces
						sCurrentLine=" "+sCurrentLine+" ";
						for(i=0;i<sCurrentLine.length();i++){
							if(Character.isWhitespace(sCurrentLine.charAt(i)))
								Sp.add(i);
						}ArrayList<String> DataPoint=new ArrayList<String>();
						for(i=0;i<Sp.size()-1;i++){
							DataPoint.add(sCurrentLine.substring(Sp.get(i), Sp.get(i+1)).trim());
						}DataInput.add(DataPoint);//System.out.println(DataPoint);
					}
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
			
			/**
			 * checking with the layout parameters
			 * 
			 */
			char[] datalayout = CreateLayout(layout);
			if(datalayout.length!=DataInput.get(0).size()){
				System.out.print("Data Layout is incorrect. "+datalayout.length+" "+DataInput.get(0).size());
				return null;
			}else{
				ArrayList<Character> FinalPin = new ArrayList<Character>();
				for(char c:datalayout)
					FinalPin.add(c);
				for(int i=0;i<FinalPin.size();i++){
					if(FinalPin.get(i)=='I'){
						FinalPin.remove(i);
						for(ArrayList<String>DP:DataInput)
							DP.remove(i);
						i=i-1;
					}
				}
				for(int i=0;i<FinalPin.size();i++){
					if(FinalPin.get(i)=='L'){
						for(ArrayList<String> DP:DataInput){
							String swap = DP.get(i);
							DP.set(i, DP.get(DP.size()-1));
							DP.set(DP.size()-1, swap);
						}
					}break;
				}
			}
			
		return DataInput;
	}
		
		/**
		 * Breaks the run length code for data layout
		  	 * N-Nominal/Number/Real
			 * C-Categorical/Alphabetical/Numerical
			 * I-Ignore Attribute
			 * L-Label - last of the fields
		 *
		 * @param dataInfo
		 * @return
		 */
		public char[] CreateLayout(String dataIn){
			char[] lay =dataIn.trim().toCharArray();
			ArrayList<Character> layo = new ArrayList<Character>();
			ArrayList<Character> LaySet = new ArrayList<Character>();
			LaySet.add('N');LaySet.add('C');LaySet.add('I');LaySet.add('L');
			ArrayList<Integer> number = new ArrayList<Integer>();
			for(char ch:lay){
				if(ch!=',')
					layo.add(ch);
			}
			lay = new char[layo.size()];
			for(int i=0;i<layo.size();i++)
				lay[i] = layo.get(i);
			layo.clear();
			for(int i=0;i<lay.length;i++){
				if(LaySet.contains(lay[i])){
					if(convertonumb(number)<=0)
					layo.add(lay[i]);
					else{
						for(int j=0;j<convertonumb(number);j++){
							layo.add(lay[i]);
						}
					}
					number.clear();
				}
				else
					number.add(Character.getNumericValue(lay[i]));					
			}
			lay = new char[layo.size()];
			for(int i=0;i<layo.size();i++)
				lay[i] = layo.get(i);
			return lay;
		}
		
		/**
		 * converts arraylist to numbers
		 * 
		 * @param number
		 * @return
		 */
		private int convertonumb(ArrayList<Integer> number) {
			// TODO Auto-generated method stub
			int numb=0;
			if(number!=null){
				for(int ij=0;ij<number.size();ij++){
					numb=numb*10+number.get(ij);
				}
			}
			return numb;
		}
		
		/**
		 * Creates final list that Forest will use as reference...
		 * @param dataIn
		 * @return
		 */
		public ArrayList<Character> CreateFinalLayout(String dataIn) {
			// "N 3 C 2 N C 4 N C 8 N 2 C 19 N L I";
						char[] lay =dataIn.toCharArray();
						ArrayList<Character> layo = new ArrayList<Character>();
						ArrayList<Character> LaySet = new ArrayList<Character>();
						LaySet.add('N');LaySet.add('C');LaySet.add('I');LaySet.add('L');
						ArrayList<Integer> number = new ArrayList<Integer>();
						for(char ch:lay){
							if(ch!=',')
								layo.add(ch);
						}
						lay = new char[layo.size()];
						for(int i=0;i<layo.size();i++)
							lay[i] = layo.get(i);
						layo.clear();
						for(int i=0;i<lay.length;i++){
							if(LaySet.contains(lay[i])){
								if(convertonumb(number)<=0)
								layo.add(lay[i]);
								else{
									for(int j=0;j<convertonumb(number);j++){
										layo.add(lay[i]);
									}
								}
								number.clear();
							}
							else
								number.add(Character.getNumericValue(lay[i]));					
						}
						for(int i=0;i<layo.size();i++)
							if(layo.get(i)=='I'||layo.get(i)=='L'){
								layo.remove(i);
								i=i-1;
							}
						layo.add('L');
						System.out.println("Final Data Layout Parameters "+layo);
						return layo;
		}
}
