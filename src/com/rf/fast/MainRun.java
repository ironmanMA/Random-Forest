package com.rf.fast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class MainRun {
  public static void main(String[] args){
		
		System.out.println("Random-Forest with Categorical support");
		
		String DataInfo,traindata,testdata,foraccuracy,withthreads;
		int numTrees,numThreads,numAttris,Ms;
		Scanner scan = new Scanner(System.in);
		Scanner scan2 = new Scanner(System.in);
		System.out.println("Enter TrainData path");
		traindata = scan.next();
		System.out.println("Enter TestData path");
		testdata=scan.next();
		System.out.println("Enter DataLayout path (use commas in between)");
		DataInfo=scan.next();
		System.out.println("Enter Number of Trees");
		numTrees=scan2.nextInt();		
		System.out.println("Enter Number of Attributes to be chosen ( sqrt(M) is set to default )");
		numAttris = scan2.nextInt();
		System.out.println("Enter Number of Threads");
		numThreads=scan2.nextInt();
		DescribeTrees DT = new DescribeTrees(traindata,DataInfo);
		ArrayList<ArrayList<String>> Train = DT.CreateInputCateg(traindata, DataInfo);
		ArrayList<ArrayList<String>> Test = DT.CreateInputCateg(testdata, DataInfo);
		ArrayList<Character> DataLayout = DT.CreateFinalLayout(DataInfo);
		
		/**
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
		
		
		int M=DataLayout.size()-1;
		if(numAttris<1)
			Ms = (int)Math.round(Math.log(M)/Math.log(2)+1);
		else
			Ms=numAttris;
		int C = Classes.size();
		RandomForest RFC = new RandomForest(DataLayout,numTrees,numThreads, M, Ms, C, Train, Test);
		System.out.println("Test data for Accuracy? [y/n]");
		foraccuracy = scan.next();
		System.out.println("Test data with Threads? [y/n]");
		withthreads = scan.next();
		
		if(foraccuracy.toLowerCase().toCharArray()[0]=='y')
			if(withthreads.toLowerCase().toCharArray()[0] == 'y')
				RFC.Start(true, true);
			else
				RFC.Start(true, false);
		else
			if(withthreads.toLowerCase().toCharArray()[0]=='y')
				RFC.Start(false, true);
			else
				RFC.Start(false, false);
	}
}
