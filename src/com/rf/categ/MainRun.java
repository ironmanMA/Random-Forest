package com.rf.real.categ;

import java.util.ArrayList;
import java.util.HashMap;

public class MainRunCateg {
	public static void main(String[] args){
		System.out.println("Random-Forest with Categorical support");
		System.out.println("Now Running");
		/*
		 * data has to be separated by either ',' or ' ' only...
		 */
		int categ=0;
		String traindata,testdata;
		if(categ>0){
			traindata="/home/mohammad/Desktop/Material/KDDTrainSmall.txt";
			testdata="/home/mohammad/Desktop/Material/KDDTestSmall.txt";
		}else if(categ<0){
			traindata="/home/mohammad/Desktop/Material/Data.txt";
			testdata="/home/mohammad/Desktop/Material/Test.txt";
		}else{
			traindata="/home/mohammad/Desktop/Material/KDDTrain+.txt";
			testdata="/home/mohammad/Desktop/Material/KDDTest+.txt";
		}
		
		DescribeTreesCateg DT = new DescribeTreesCateg(traindata);
		ArrayList<ArrayList<String>> Train = DT.CreateInputCateg(traindata);
		ArrayList<ArrayList<String>> Test = DT.CreateInputCateg(testdata);
		/*
		 * For class-labels 
		 */
		HashMap<String, Integer> Classes = new HashMap<String, Integer>();
		for(ArrayList<String> dp : Train){
			String clas = dp.get(dp.size()-1);
			if(Classes.containsKey(clas))
				Classes.put(clas, Classes.get(clas)+1);
			else
				Classes.put(clas, 1);				
		}
		
		int numTrees=10;
		int M=Train.get(0).size()-1;
		int Ms = (int)Math.round(Math.log(M)/Math.log(2)+1);
		int C = Classes.size();
		RandomForestCateg RFC = new RandomForestCateg(numTrees, M, Ms, C, Train, Test);
		RFC.Start();
		
	}
}
