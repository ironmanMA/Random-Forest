package com.rf.real.categ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class RandomForestCateg {
	
	/** the number of threads to use when generating the forest */
	private static final int NUM_THREADS=Runtime.getRuntime().availableProcessors();
	//private static final int NUM_THREADS=1;
	/** the number of categorical responses of the data (the classes, the "Y" values) - set this before beginning the forest creation */
	public static int C;
	/** the number of attributes in the data - set this before beginning the forest creation */
	public static int M;
	/** Of the M total attributes, the random forest computation requires a subset of them
	 * to be used and picked via random selection. "Ms" is the number of attributes in this
	 * subset. The formula used to generate Ms was recommended on Breiman's website.
	 */
	public static int Ms;//recommended by Breiman: =(int)Math.round(Math.log(M)/Math.log(2)+1);
	/** the collection of the forest's decision trees */
	private ArrayList<DTreeCateg> trees;
	/** the collection of the forest's decision trees */
	private ArrayList<DTreeCateg2> trees2;
	/** the starting time when timing random forest creation */
	private long time_o;
	/** the number of trees in this random tree */
	private int numTrees;
	/** For progress bar display for the creation of this random forest, this is the amount to update by when one tree is completed */
	private double update;
	/** For progress bar display for the creation of this random forest, this records the total progress */
	private double progress;
	/** this is an array whose indices represent the forest-wide importance for that given attribute */
	private int[] importances;
	/** This maps from a data record to an array that records the classifications by the trees where it was a "left out" record (the indices are the class and the values are the counts) */
	private HashMap<int[],int[]> estimateOOB;
	/** the total forest-wide error */
	private double error;
	/** the thread pool that controls the generation of the decision trees */
	private ExecutorService treePool;
	/** the original training data matrix that will be used to generate the random forest classifier */
	private ArrayList<ArrayList<String>> data;
	/** the data on which produced random forest will be tested*/
	private ArrayList<ArrayList<String>> testdata;
	/** This holds all of the predictions of trees in a Forest */
	private ArrayList<ArrayList<String>> Prediction;
	/**
	 * This hold the genres of attributes in the forest
	 * 
	 * 1 if categ
	 * 0 if real
	 */
	public ArrayList<Integer> TrainAttributes;
	public ArrayList<Integer> TestAttributes;
	
	/**
	 * Initializes a Breiman random forest creation
	 * 
	 * @param numTrees			the number of trees in the forest
	 * @param data				the training data used to generate the forest
	 * @param buildProgress		records the progress of the random forest creation
	 */
	public RandomForestCateg(int numTrees,int M,int Ms,int C, ArrayList<ArrayList<String>> train,ArrayList<ArrayList<String>> test) {
		// TODO Auto-generated constructor stub
		StartTimer();
		this.numTrees=numTrees;
		this.data=train;
		this.testdata=test;
		this.M=M;
		this.Ms=Ms;
		this.C=C;
		this.TrainAttributes=GetAttributes(train);
		this.TestAttributes=GetAttributes(test);
		trees = new ArrayList<DTreeCateg>(numTrees);
		trees2 = new ArrayList<DTreeCateg2>(numTrees);
		update=100/((double)numTrees);
		progress=0;
		System.out.println("creating "+numTrees+" trees in a random Forest. . . ");
		System.out.println("total data size is "+train.size());
		System.out.println("number of attributes "+M);
		System.out.println("number of selected attributes "+Ms);
		
		estimateOOB=new HashMap<int[],int[]>(data.size());
		Prediction = new ArrayList<ArrayList<String>>();
	}
	/**
	 * Begins the random forest creation
	 */
	public void Start() {
		// TODO Auto-generated method stub
		System.out.println("Number of threads started : "+NUM_THREADS);
		System.out.println("Starting trees");
		treePool=Executors.newFixedThreadPool(NUM_THREADS);
		for (int t=0;t<numTrees;t++){
			treePool.execute(new CreateTree(data,this,t+1));
		}treePool.shutdown();
		try {	         
			treePool.awaitTermination(Long.MAX_VALUE,TimeUnit.SECONDS); //effectively infinity
	    } catch (InterruptedException ignored){
	    	System.out.println("interrupted exception in Random Forests");
	    }
		if(data.get(0).size()>testdata.get(0).size()){
			//TestForestNoLabel2(trees2, data, testdata);
			TestForestNoLabel(trees,data,testdata);
		}
		else if(data.get(0).size()==testdata.get(0).size()){
			TestForest2(trees2, data, testdata);
			//TestForest(trees,data,testdata);
		}
			
		else
			System.out.println("Cannot test this data");
		
		System.out.print("Done in "+TimeElapsed(time_o));
	}
	/**
	 * Predicting unlabeled data
	 * 
	 * @param trees2
	 * @param data2
	 * @param testdata2
	 */
	private void TestForestNoLabel(ArrayList<DTreeCateg> trees2,ArrayList<ArrayList<String>> data2,ArrayList<ArrayList<String>> testdata2) {
		// TODO Auto-generated method stub
		ArrayList<String> TestResult = new ArrayList<String>();
		System.out.println("Predicting Labels now");
		for(ArrayList<String> DP:testdata2){
			ArrayList<String> Predict = new ArrayList<String>();
			for(DTreeCateg DT:trees2){
				Predict.add(DT.Evaluate(DP, testdata2));
			}
			TestResult.add(ModeofList(Predict));
		}
	}
	/**
	 * Testing the forest using the test-data 
	 * 
	 * @param DTreeCateg
	 * @param TrainData
	 * @param TestData
	 * 
	 */
	public void TestForest(ArrayList<DTreeCateg> trees,ArrayList<ArrayList<String>> train,ArrayList<ArrayList<String>> test){
		int correctness=0;ArrayList<String> ActualValues = new ArrayList<String>();
		
		for(ArrayList<String> s:test){
			ActualValues.add(s.get(s.size()-1));
		}int treee=1;
		System.out.println("Testing forest now ");
		
		for(DTreeCateg DTC : trees){
			DTC.CalculateClasses(train, test, treee);treee++;
			if(DTC.predictions!=null)
			Prediction.add(DTC.predictions);
		}
		for(int i = 0;i<test.size();i++){
			ArrayList<String> Val = new ArrayList<String>();
			for(int j=0;j<trees.size();j++){
				Val.add(Prediction.get(j).get(i));
			}
			String pred = ModeofList(Val);
			if(pred.equalsIgnoreCase(ActualValues.get(i))){
				correctness = correctness +1;
			}
		}
		System.out.println("The Result of Predictions :-");
		System.out.println("Total Cases : "+test.size());
		System.out.println("Total CorrectPredicitions  : "+correctness);
		System.out.println("Forest Accuracy :"+(correctness*100/test.size())+"%");				
	}
	/**
	 * Predicting unlabeled data
	 * 
	 * @param trees22
	 * @param data2
	 * @param testdata2
	 */
	private void TestForestNoLabel2(ArrayList<DTreeCateg2> trees22,ArrayList<ArrayList<String>> data2,ArrayList<ArrayList<String>> testdata2) {
		// TODO Auto-generated method stub
		ArrayList<String> TestResult = new ArrayList<String>();
		System.out.println("Predicting Labels now");
		for(ArrayList<String> DP:testdata2){
			ArrayList<String> Predict = new ArrayList<String>();
			for(DTreeCateg2 DT:trees22){
				Predict.add(DT.Evaluate(DP, testdata2));
			}
			TestResult.add(ModeofList(Predict));
		}
	}
	/**
	 * Testing the forest using the test-data 
	 * 
	 * @param DTreeCateg2
	 * @param TrainData
	 * @param TestData
	 * 
	 */
	public void TestForest2(ArrayList<DTreeCateg2> trees,ArrayList<ArrayList<String>> train,ArrayList<ArrayList<String>> test){
		int correctness=0;ArrayList<String> ActualValues = new ArrayList<String>();
		
		for(ArrayList<String> s:test){
			ActualValues.add(s.get(s.size()-1));
		}int treee=1;
		System.out.println("Testing forest now ");
		
		for(DTreeCateg2 DTC : trees){
			DTC.CalculateClasses(train, test, treee);treee++;
			if(DTC.predictions!=null)
			Prediction.add(DTC.predictions);
		}
		for(int i = 0;i<test.size();i++){
			ArrayList<String> Val = new ArrayList<String>();
			for(int j=0;j<trees.size();j++){
				Val.add(Prediction.get(j).get(i));
			}
			String pred = ModeofList(Val);
			if(pred.equalsIgnoreCase(ActualValues.get(i))){
				correctness = correctness +1;
			}
		}
		System.out.println("The Result of Predictions :-");
		System.out.println("Total Cases : "+test.size());
		System.out.println("Total CorrectPredicitions  : "+correctness);
		System.out.println("Forest Accuracy :"+(correctness*100/test.size())+"%");				
	}
	/**
	 * To find the final prediction of the trees
	 * 
	 * @param predictions
	 * @return the mode of the list
	 */
	public String ModeofList(ArrayList<String> predictions) {
		// TODO Auto-generated method stub
		String MaxValue = null; int MaxCount = 0;
		for(int i=0;i<predictions.size();i++){
			int count=0;
			for(int j=0;j<predictions.size();j++){
				if(predictions.get(j).trim().equalsIgnoreCase(predictions.get(i).trim()))
					count++;
				if(count>MaxCount){
					MaxValue=predictions.get(i);
					MaxCount=count;
				}
			}
		}return MaxValue;
	}
	/**
	 * This class houses the machinery to generate one decision tree in a thread pool environment.
	 *
	 */
	private class CreateTree implements Runnable{
		/** the training data to generate the decision tree (same for all trees) */
		private ArrayList<ArrayList<String>> data;
		/** the current forest */
		private RandomForestCateg forest;
		/** the Tree number */
		private int treenum;
		/**
		 * A default constructor
		 */
		public CreateTree(ArrayList<ArrayList<String>> data,RandomForestCateg forest,int num){
			this.data=data;
			this.forest=forest;
			this.treenum=num;
		}
		/**
		 * Creates the decision tree
		 */
		public void run() {
			//trees.add(new DTreeCateg(data,forest,treenum));
			trees2.add(new DTreeCateg2(data, forest, treenum));
			progress+=update;
		}
	}
	
	/** Start the timer when beginning forest creation */
	private void StartTimer(){
		time_o=System.currentTimeMillis();
	}
	/**
	 * Given a certain time that's elapsed, return a string
	 * representation of that time in hr,min,s
	 * 
	 * @param timeinms	the beginning time in milliseconds
	 * @return			the hr,min,s formatted string representation of the time
	 */
	private static String TimeElapsed(long timeinms){
		double s=(double)(System.currentTimeMillis()-timeinms)/1000;
		int h=(int)Math.floor(s/((double)3600));
		s-=(h*3600);
		int m=(int)Math.floor(s/((double)60));
		s-=(m*60);
		return ""+h+"hr "+m+"m "+s+"sec";
	}
	/**
	 * Checks if attribute is categorical or not
	 * 
	 * @param s
	 * @return boolean true if it has an alphabet
	 */
	private boolean isAlphaNumeric(String s){
		char c[]=s.toCharArray();boolean hasalpha=false;
		for(int j=0;j<c.length;j++){
			hasalpha = Character.isLetter(c[j]);
			if(hasalpha)break;
		}return hasalpha;
	}
	/**
	 * Of the attributes selected this function will record the genre of attributes  
	 */
	private ArrayList<Integer> GetAttributes(List<ArrayList<String>> data){
		ArrayList<Integer> Attributes = new ArrayList<Integer>();int iter = 0;
		ArrayList<String> DataPoint = data.get(iter);
		if(DataPoint.contains("n/a") || DataPoint.contains("N/A")){
			iter = iter +1;
			DataPoint = data.get(iter);
		}
		for(int i =0;i<DataPoint.size();i++){
			if(isAlphaNumeric(DataPoint.get(i)))
				Attributes.add(1);
			else
				Attributes.add(0);
		}
		return Attributes;
	}
}
