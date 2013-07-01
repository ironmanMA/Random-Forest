package com.rf.real;

import java.util.ArrayList;

public class MainRun {
	@SuppressWarnings("static-access")
	public static void main(String args[]){

		String traindata="/home/mohammad/Desktop/Material/Data.txt";
		String testdata="/home/mohammad/Desktop/Material/Test.txt";
		int numTrees=100;
		
		DescribeTrees DT = new DescribeTrees(traindata);
		ArrayList<int[]> Input=DT.CreateInput(traindata);int categ=0;
		
		DescribeTrees DTT = new DescribeTrees(traindata);
		ArrayList<int[]> Test=DTT.CreateInput(testdata);
		
		for(int k=0;k<Input.size();k++){if(Input.get(k)[Input.get(k).length-1]<categ)continue;else{categ=Input.get(k)[Input.get(k).length-1];}}
		
		RandomForest RaF =new RandomForest(numTrees, Input, Test);
		RaF.C=categ;
		RaF.M=Input.get(0).length-1;
		RaF.Ms=(int)Math.round(Math.log(RaF.M)/Math.log(2)+1);
		RaF.Start();

	}
}
