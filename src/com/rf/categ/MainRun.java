package com.rf.real.categ;

import java.util.ArrayList;

public class MainRun {
  public static void main(String[] args){
		String traindata="/home/mohammad/Desktop/Material/KDDTrainSmall.txt";//data has to be separated by either ',' or ' ' only...
		String testdata="/home/mohammad/Desktop/Material/KDDTestSmall.txt";
		int numTrees=50;
		
		DescribeTreesCateg DT = new DescribeTreesCateg(traindata);
		ArrayList<ArrayList<String>> Train = DT.CreateInputCateg(traindata);
		ArrayList<ArrayList<String>> Test = DT.CreateInputCateg(testdata);
		int M=0;//set this
		int Ms=0;//set this
		
		RandomForestCateg RFC = new RandomForestCateg(numTrees, M, Ms, Train, Test);
		RFC.M=Train.get(0).size()-1;
		RFC.Ms = (int)Math.round(Math.log(RFC.M)/Math.log(2)+1);;
		RFC.Start();
		
		//System.out.println("Printing traindata "+Train);
		
	}
}
