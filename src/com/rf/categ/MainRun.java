package com.rf.real.categ;

import java.util.ArrayList;
import java.util.HashMap;

public class MainRun {
	public static void main(String[] args){
		String traindata="/home/mohammad/Desktop/Material/KDDTrainSmall.txt";//data has to be separated by either ',' or ' ' only...
		String testdata="/home/mohammad/Desktop/Material/KDDTestSmall.txt";
		
		DescribeTreesCateg DT = new DescribeTreesCateg(traindata);
		ArrayList<ArrayList<String>> Train = DT.CreateInputCateg(traindata);//System.out.println(Train.get(0).size());
		ArrayList<ArrayList<String>> Test = DT.CreateInputCateg(testdata);//System.out.println(Test.get(0).size());
		
		//for class-labels 
		HashMap<String, Integer> Classes = new HashMap<String, Integer>();
		for(ArrayList<String> dp : Train){
			String clas = dp.get(dp.size()-1);
			if(Classes.containsKey(clas))
				Classes.put(clas, Classes.get(clas)+1);
			else
				Classes.put(clas, 1);				
		}System.out.println(Classes.size());
		
		int numTrees=1;
		int M=Train.get(0).size()-1;
		int Ms = (int)Math.round(Math.log(M)/Math.log(2)+1);
		RandomForestCateg RFC = new RandomForestCateg(numTrees, M, Ms, Train, Test);
		RFC.C = Classes.size();
		RFC.Start();
		
		//System.out.println("Printing traindata "+Train);
		
	}
}
