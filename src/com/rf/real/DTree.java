package com.rf.real;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Creates a decision tree based on the specifications of random forest trees
 *
 */
public class DTree {


	/** Instead of checking each index we'll skip every INDEX_SKIP indices unless there's less than MIN_SIZE_TO_CHECK_EACH*/
	private static final int INDEX_SKIP=3;
	/** If there's less than MIN_SIZE_TO_CHECK_EACH points, we'll check each one */
	private static final int MIN_SIZE_TO_CHECK_EACH=10;
	/** If the number of data points is less than MIN_NODE_SIZE, we won't continue splitting, we'll take the majority vote */
	private static final int MIN_NODE_SIZE=5;
	/** the number of data records */
	private int N;
	/** the number of samples left out of the boostrap of all N to test error rate 
	 * @see <a href="http://www.stat.berkeley.edu/~breiman/RandomForests/cc_home.htm#ooberr">OOB error estimate</a>
	*/
	private int testN;
	/** Of the testN, the number that were correctly identified */
	private int correct;
	/** an estimate of the importance of each attribute in the data record
	 * @see <a href="http://www.stat.berkeley.edu/~breiman/RandomForests/cc_home.htm#varimp">Variable Importance</a>
	 */
	private int[] importances;
	/** This is the root of the Decision Tree */
	private TreeNode root;
	/** This is a pointer to the Random Forest this decision tree belongs to */
	private RandomForest forest;
	/** This keeps track of all the predictions done by this tree */
	public ArrayList<Integer> predictions;

	/**
	 * This constructs a decision tree from a data matrix.
	 * It first creates a bootstrap sample, the train data matrix, as well as the left out records, 
	 * the test data matrix. Then it creates the tree, then calculates the variable importances (not essential)
	 * and then removes the links to the actual data (to save memory)
	 * 
	 * @param data		The data matrix as a List of int arrays - each array is one record, each index in the array is one attribute, and the last index is the class
	 * 					(ie [ x1, x2, . . ., xM, Y ]).
	 * @param forest	The random forest this decision tree belongs to
	 */
	public DTree(ArrayList<int[]> data,RandomForest forest,int num){
		this.forest=forest;
		N=data.size();
		importances=new int[RandomForest.M];
		predictions = new ArrayList<Integer>();
	
		//System.out.println("Make a Dtree num : "+num+" with N:"+N+" M:"+RandomForest.M+" Ms:"+RandomForest.Ms);
		
		ArrayList<int[]> train=new ArrayList<int[]>(N); //data becomes the "bootstrap" - that's all it knows
		ArrayList<int[]> test=new ArrayList<int[]>();
		//System.out.println("Creating tree No."+num);
		BootStrapSample(data,train,test,num);//populates train and test using data
		testN=test.size();
		correct=0;	
		
		root=CreateTree(train,num);//creating tree using training data
		//CalcTreeVariableImportanceAndError(test,num);
		FlushData(root);
	}
	/**
	 * Responsible for gauging the error rate of this tree and 
	 * calculating the importance values of each attribute
	 * 
	 * @param test	The left out data matrix
	 */
	private void CalcTreeVariableImportanceAndError(ArrayList<int[]> test,int nv) {
		correct=CalcTreeErrorRate(test,nv);
		CalculateClasses(test, nv);
		
		for (int m=0;m<RandomForest.M;m++){
			ArrayList<int[]> data=RandomlyPermuteAttribute(CloneData(test),m);
			int correctAfterPermute=0;
			for (int[] arr:data){
				int prediction=Evaluate(arr);
				if (prediction == GetClass(arr))
					correctAfterPermute++;
			}
			importances[m]+=(correct-correctAfterPermute);
		}		
//		System.out.println("Importances of tree ");
//		for(int m=0;m<importances.length;m++){
//			System.out.println(" "+importances[m]);
//		}
	}

	/**
	 * Calculates the tree error rate,
	 * displays the error rate to console,
	 * and updates the total forest error rate
	 * 
	 * @param test	the test data matrix
	 * @return	the number correct
	 */
	public int CalcTreeErrorRate(ArrayList<int[]> test,int nu){		
		int correct=0;		
		for (int[] record:test){
			int Class=Evaluate(record);
			forest.UpdateOOBEstimate(record,Class);
			int k = record[record.length-1];
//			if (Evaluate(record) == GetClass(record))
				if (Evaluate(record) == k)
				correct++;
		}
		
		double err=1-correct/((double)test.size());
//		System.out.print("\n");
		System.out.println("Number of correct  = "+correct+", out of :"+test.size());
		System.out.println("Test-Data error rate of tree "+nu+"  is: "+(err*100)+" %");
		
		return correct;
	}
	/**
	 * This method will get the classes and will return the updates
	 * 
	 */
	public ArrayList<Integer> CalculateClasses(ArrayList<int[]> test,int nu){
		ArrayList<Integer> corest = new ArrayList<Integer>();int k=0;int korect = 0;
		for(int[] record : test){
			int kls = Evaluate(record);
			corest.add(kls);
			int k1 = record[record.length-1];
			if (kls==k1)
				korect++;
		}
		predictions= corest;
		return corest;
		
	}
	/**
	 * This will classify a new data record by using tree
	 * recursion and testing the relevant variable at each node.
	 * 
	 * This is probably the most-used function in all of <b>GemIdent</b>.
	 * It would make sense to inline this in assembly for optimal performance.
	 * 
	 * @param record 	the data record to be classified
	 * @return			the class the data record was classified into
	 */
	public int Evaluate(int[] record){//need to write this 
		TreeNode evalNode=root;
		
		while (true){
			if (evalNode.isLeaf)
				return evalNode.Class;
			if (record[evalNode.splitAttributeM] <= evalNode.splitValue)
				evalNode=evalNode.left;
			else
				evalNode=evalNode.right;			
		}
	}
	/**
	 * Takes a list of data records, and switches the mth attribute across data records.
	 * This is important in order to test the importance of the attribute. If the attribute 
	 * is randomly permuted and the result of the classification is the same, the attribute is
	 * not important to the classification and vice versa.
	 * 
	 * @see <a href="http://www.stat.berkeley.edu/~breiman/RandomForests/cc_home.htm#varimp">Variable Importance</a>
	 * @param test		The data matrix to be permuted
	 * @param m			The attribute index to be permuted
	 * @return			The data matrix with the mth column randomly permuted
	 */
	private ArrayList<int[]> RandomlyPermuteAttribute(ArrayList<int[]> test,int m){
		int num=test.size()*2;
		for (int i=0;i<num;i++){
			int a=(int)Math.floor(Math.random()*test.size());
			int b=(int)Math.floor(Math.random()*test.size());
			int[] arrA=test.get(a);
			int[] arrB=test.get(b);
			int temp=arrA[m];
			arrA[m]=arrB[m];
			arrB[m]=temp;
		}
		return test;
	}
	/**
	 * Creates a copy of the data matrix
	 * @param data		the data matrix to be copied
	 * @return			the cloned data matrix
	 */
	private ArrayList<int[]> CloneData(ArrayList<int[]> data){
		ArrayList<int[]> clone=new ArrayList<int[]>(data.size());
		int M=data.get(0).length;
		for (int i=0;i<data.size();i++){
			int[] arr=data.get(i);
			int[] arrClone=new int[M];
			for (int j=0;j<M;j++){
				arrClone[j]=arr[j];
			}
			clone.add(arrClone);
		}
		return clone;
	}
	/**
	 * This creates the decision tree according to the specifications of random forest trees. 
	 * 
	 * @see <a href="http://www.stat.berkeley.edu/~breiman/RandomForests/cc_home.htm#overview">Overview of random forest decision trees</a>
	 * @param train		the training data matrix (a bootstrap sample of the original data)
	 * @return			the TreeNode object that stores information about the parent node of the created tree
	 */
	private TreeNode CreateTree(ArrayList<int[]> train, int ntree){
		TreeNode root=new TreeNode();
		root.data=train;
		//System.out.println("creating ");
		RecursiveSplit(root,ntree);
		return root;
	}
	/**
	 * 
	 * @author kapelner
	 *
	 */
	private class TreeNode implements Cloneable{
		public boolean isLeaf;
		public TreeNode left;
		public TreeNode right;
		public int splitAttributeM;
		public Integer Class;
		public List<int[]> data;
		public int splitValue;
		public int generation;
		
		public TreeNode(){
			splitAttributeM=-99;
			splitValue=-99;
			generation=1;
		}
		public TreeNode clone(){ //"data" element always null in clone
			TreeNode copy=new TreeNode();
			copy.isLeaf=isLeaf;
			if (left != null) //otherwise null
				copy.left=left.clone();
			if (right != null) //otherwise null
				copy.right=right.clone();
			copy.splitAttributeM=splitAttributeM;
			copy.Class=Class;
			copy.splitValue=splitValue;
			return copy;
		}
	}
	private class DoubleWrap{
		public double d;
		public DoubleWrap(double d){
			this.d=d;
		}		
	}
	/**
	 * This is the crucial function in tree creation. 
	 * 
	 * <ul>
	 * <li>Step A
	 * Check if this node is a leaf, if so, it will mark isLeaf true
	 * and mark Class with the leaf's class. The function will not
	 * recurse past this point.
	 * </li>
	 * <li>Step B
	 * Create a left and right node and keep their references in
	 * this node's left and right fields. For debugging purposes,
	 * the generation number is also recorded. The {@link RandomForest#Ms Ms} attributes are
	 * now chosen by the {@link #GetVarsToInclude() GetVarsToInclude} function
	 * </li>
	 * <li>Step C
	 * For all Ms variables, first {@link #SortAtAttribute(List,int) sort} the data records by that attribute 
	 * , then look through the values from lowest to 
	 * highest. If value i is not equal to value i+1, record i in the list of "indicesToCheck."
	 * This speeds up the splitting. If the number of indices in indicesToCheck >  MIN_SIZE_TO_CHECK_EACH
	 * then we will only {@link #CheckPosition(int, int, int, ImageAnalysisStatistics.DTree.DoubleWrap, ImageAnalysisStatistics.DTree.TreeNode) check} the
	 * entropy at every {@link #INDEX_SKIP INDEX_SKIP} index otherwise, we {@link #CheckPosition(int, int, int, ImageAnalysisStatistics.DTree.DoubleWrap, ImageAnalysisStatistics.DTree.TreeNode) check}
	 * the entropy for all. The "E" variable records the entropy and we are trying to find the minimum in which to split on
	 * </li>
	 * <li>Step D
	 * The newly generated left and right nodes are now checked:
	 * If the node has only one record, we mark it as a leaf and set its class equal to
	 * the class of the record. If it has less than {@link #MIN_NODE_SIZE MIN_NODE_SIZE}
	 * records, then we mark it as a leaf and set its class equal to the {@link #GetMajorityClass(List) majority class}.
	 * If it has more, then we do a manual check on its data records and if all have the same class, then it
	 * is marked as a leaf. If not, then we run {@link #RecursiveSplit(ImageAnalysisStatistics.DTree.TreeNode) RecursiveSplit} on
	 * that node
	 * </li>
	 * </ul>
	 * 
	 * @param parent	The node of the parent
	 */
	private void RecursiveSplit(TreeNode parent, int Ntreenum){
		
		//System.out.println("Recursivly spilitting tree : "+Ntreenum);
		if (!parent.isLeaf){
			
			//-------------------------------Step A
			Integer Class=CheckIfLeaf(parent.data);
			if (Class != null){
				parent.isLeaf=true;
				parent.Class=Class;
				//System.out.println("leaf for this tree: "+Ntreenum);
//				System.out.print("parent leaf! Class:"+parent.Class+"  ");
//				PrintOutClasses(parent.data);
				return;
			}
			
			//-------------------------------Step B
			int Nsub=parent.data.size();
//			PrintOutClasses(parent.data);			
			
			parent.left=new TreeNode();
			parent.left.generation=parent.generation+1;
			parent.right=new TreeNode();
			parent.right.generation=parent.generation+1;
			//System.out.println("Creating left and right nodes for this tree: "+Ntreenum);
			ArrayList<Integer> vars=GetVarsToInclude();//randomly selects Ms.Nos of attributes from M
			
			DoubleWrap lowestE=new DoubleWrap(Double.MAX_VALUE);

			//-------------------------------Step C
			for (int m:vars){
				
				SortAtAttribute(parent.data,m);//sorts on a particular column in the row
				
				ArrayList<Integer> indicesToCheck=new ArrayList<Integer>();
				for (int n=1;n<Nsub;n++){
					int classA=GetClass(parent.data.get(n-1));
					int classB=GetClass(parent.data.get(n));
					if (classA != classB)
						indicesToCheck.add(n);
				}
				
				if (indicesToCheck.size() == 0){//if all the Y-values are same, then get the class directly
					parent.isLeaf=true;
					parent.Class=GetClass(parent.data.get(0));
					continue;
				}
//				System.out.print("indices to check for tree : "+Ntreenum);
//				for (int n:indicesToCheck)
//					System.out.print(" "+n);
//				System.out.print("\n ");
				if (indicesToCheck.size() > MIN_SIZE_TO_CHECK_EACH){
					for (int i=0;i<indicesToCheck.size();i+=INDEX_SKIP){
	//					for (int n=1;n<Nsub;n++)
						//System.out.println("Checking positions for index : "+i+" and tree :"+Ntreenum);
						CheckPosition(m,indicesToCheck.get(i),Nsub,lowestE,parent,Ntreenum);
						if (lowestE.d == 0)//lowestE now has the minimum conditional entropy so IG is max there
							break;
					}
				}
				else {
					for (int n:indicesToCheck){
						CheckPosition(m,n,Nsub,lowestE,parent,Ntreenum);
						if (lowestE.d == 0)
							break;
					}
				}
//				BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
//				System.out.println("************************* lowest e:"+lowestE.d);
//				try {reader.readLine();} catch (IOException e){}
				if (lowestE.d == 0)
					break;
			}
//			System.out.print("\n");
//			System.out.print("split attrubute num:"+parent.splitAttributeM+" at val:"+parent.splitValue+" n:"+parent.data.size()+" ");
//			PrintOutClasses(parent.data);
			//			System.out.println("\nmadeSplit . . .");
//			PrintOutNode(parent," ");
//			PrintOutNode(parent.left,"    ");
//			PrintOutNode(parent.right,"    ");

			//-------------------------------Step D
				//------------Left Child
			if (parent.left.data.size() == 1){
				parent.left.isLeaf=true;
				parent.left.Class=GetClass(parent.left.data.get(0));							
			}
			else if (parent.left.data.size() < MIN_NODE_SIZE){
				parent.left.isLeaf=true;
				parent.left.Class=GetMajorityClass(parent.left.data);	
			}
			else {
				Class=CheckIfLeaf(parent.left.data);
				if (Class == null){
					parent.left.isLeaf=false;
					parent.left.Class=null;
//					System.out.println("make branch left: m:"+m);
				}
				else {
					parent.left.isLeaf=true;
					parent.left.Class=Class;
				}
			}
				//------------Right Child
			if (parent.right.data.size() == 1){
				parent.right.isLeaf=true;
				parent.right.Class=GetClass(parent.right.data.get(0));								
			}
			else if (parent.right.data.size() < MIN_NODE_SIZE){
				parent.right.isLeaf=true;
				parent.right.Class=GetMajorityClass(parent.right.data);	
			}
			else {
				Class=CheckIfLeaf(parent.right.data);
				if (Class == null){
//					System.out.println("make branch right: m:"+m);
					parent.right.isLeaf=false;
					parent.right.Class=null;
				}
				else {
					parent.right.isLeaf=true;
					parent.right.Class=Class;
				}
			}
			
			if (!parent.left.isLeaf)
				RecursiveSplit(parent.left,Ntreenum);
//			else {				
//				System.out.print("left leaf! Class:"+parent.left.Class+"  ");
//				PrintOutClasses(parent.left.data);
//			}
			if (!parent.right.isLeaf)
				RecursiveSplit(parent.right,Ntreenum);
//			else {				
//				System.out.print("leaf right! Class:"+parent.right.Class+"  ");
//				PrintOutClasses(parent.right.data);
//			}
		}
	}
	/**
	 * Given a data matrix, return the most popular Y value (the class)
	 * @param data	The data matrix
	 * @return		The most popular class
	 */
	private int GetMajorityClass(List<int[]> data){
		int[] counts=new int[RandomForest.C];
		for (int[] record:data){
			int Class=record[record.length-1];//GetClass(record);
			counts[Class-1]++;
		}
		int index=-99;
		int max=Integer.MIN_VALUE;
		for (int i=0;i<counts.length;i++){
			if (counts[i] > max){
				max=counts[i];
				index=i+1;
			}				
		}
		return index;
	}

	/**
	 * Checks the {@link #CalcEntropy(double[]) entropy} of an index in a data matrix at a particular attribute (m)
	 * and returns the entropy. If the entropy is lower than the minimum to date (lowestE), it is set to the minimum.
	 * 
	 * The total entropy is calculated by getting the sub-entropy for below the split point and after the split point.
	 * The sub-entropy is calculated by first getting the {@link #GetClassProbs(List) proportion} of each of the classes
	 * in this sub-data matrix. Then the entropy is {@link #CalcEntropy(double[]) calculated}. The lower sub-entropy
	 * and upper sub-entropy are then weight averaged to obtain the total entropy. 
	 * 
	 * @param m				the attribute to split on
	 * @param n				the index to check
	 * @param Nsub			the number of records in the data matrix
	 * @param lowestE		the minimum entropy to date
	 * @param parent		the parent node
	 * @return				the entropy of this split
	 */
	private double CheckPosition(int m,int n,int Nsub,DoubleWrap lowestE,TreeNode parent, int nTre){
		//                       var,	index,	train.size,	lowest number,	for a tree
		//System.out.println("Checking position of the index attribute of tree :"+nTre);
		if (n < 1) //exit conditions
			return 0;
		if (n > Nsub)
			return 0;
		
		List<int[]> lower=GetLower(parent.data,n);
		List<int[]> upper=GetUpper(parent.data,n);
		if (lower == null)
			System.out.println("lower list null");	
		if (upper == null)
			System.out.println("upper list null");
		double[] pl=GetClassProbs(lower);
		double[] pu=GetClassProbs(upper);
		double eL=CalcEntropy(pl);
		double eU=CalcEntropy(pu);
	
		double e=(eL*lower.size()+eU*upper.size())/((double)Nsub);
//		System.out.println("g:"+parent.generation+" N:"+parent.data.size()+" M:"+RandomForest.M+" Ms:"+RandomForest.Ms+" n:"+n+" m:"+m+" val:"+parent.data.get(n)[m]+"                                                           e:"+e);
//		out.write(m+","+n+","+parent.data.get(n)[m]+","+e+"\n");
		if (e < lowestE.d){			
			lowestE.d=e;
//			System.out.print("-");
			parent.splitAttributeM=m;
			parent.splitValue=parent.data.get(n)[m];
			parent.left.data=lower;	
			parent.right.data=upper;
		}
		return e;//entropy
	}
	/**
	 * Given a data record, return the Y value - take the last index
	 * 
	 * @param record		the data record
	 * @return				its y value (class)
	 */
	public static int GetClass(int[] record){
		return record[RandomForest.M];
	}
	/**
	 * Given a data matrix, check if all the y values are the same. If so,
	 * return that y value, null if not
	 * 
	 * @param data		the data matrix
	 * @return			the common class (null if not common)
	 */
	private Integer CheckIfLeaf(List<int[]> data){
//		System.out.println("checkIfLeaf");
		boolean isLeaf=true;
		int ClassA=GetClass(data.get(0));
		for (int i=1;i<data.size();i++){			
			int[] recordB=data.get(i);
			if (ClassA != GetClass(recordB)){
				isLeaf=false;
				break;
			}
		}
		if (isLeaf)
			return GetClass(data.get(0));
		else
			return null;
	}
	/**
	 * Split a data matrix and return the upper portion
	 * 
	 * @param data		the data matrix to be split
	 * @param nSplit	return all data records above this index in a sub-data matrix
	 * @return			the upper sub-data matrix
	 */
	private List<int[]> GetUpper(List<int[]> data,int nSplit){
		int N=data.size();
		List<int[]> upper=new ArrayList<int[]>(N-nSplit);
		for (int n=nSplit;n<N;n++)
			upper.add(data.get(n));
		return upper;
	}
	/**
	 * Split a data matrix and return the lower portion
	 * 
	 * @param data		the data matrix to be split
	 * @param nSplit	return all data records below this index in a sub-data matrix
	 * @return			the lower sub-data matrix
	 */
	private List<int[]> GetLower(List<int[]> data,int nSplit){
		List<int[]> lower=new ArrayList<int[]>(nSplit);
		for (int n=0;n<nSplit;n++)
			lower.add(data.get(n));
		return lower;
	}
	/**
	 * This class compares two data records by numerically comparing a specified attribute
	 * 
	 * @author kapelner
	 *
	 */
	private class AttributeComparator implements Comparator{		
		/** the specified attribute */
		private int m;
		/**
		 * Create a new comparator
		 * @param m			the attribute in which to compare on
		 */
		public AttributeComparator(int m){
			this.m=m;
		}
		/**
		 * Compare the two data records. They must be of type int[].
		 * 
		 * @param o1		data record A
		 * @param o2		data record B
		 * @return			-1 if A[m] < B[m], 1 if A[m] > B[m], 0 if equal
		 */
		public int compare(Object o1, Object o2){
			int a=((int[])o1)[m];
			int b=((int[])o2)[m];
			if (a < b)
				return -1;
			if (a > b)
				return 1;
			else
				return 0;
		}		
	}
	/**
	 * Sorts a data matrix by an attribute from lowest record to highest record
	 * 
	 * @param data			the data matrix to be sorted
	 * @param m				the attribute to sort on
	 */
	@SuppressWarnings("unchecked")
	private void SortAtAttribute(List<int[]> data,int m){
		Collections.sort(data,new AttributeComparator(m));
	}
	/**
	 * Given a data matrix, return a probabilty mass function representing 
	 * the frequencies of a class in the matrix (the y values)
	 * 
	 * @param records		the data matrix to be examined
	 * @return				the probability mass function
	 */
	private double[] GetClassProbs(List<int[]> records){
		
		double N=records.size();
		
		int[] counts=new int[RandomForest.C];
//		System.out.println("counts:");
//		for (int i:counts)
//			System.out.println(i+" ");
		
		for (int[] record:records)
			counts[GetClass(record)-1]++;

		double[] ps=new double[RandomForest.C];
		for (int c=0;c<RandomForest.C;c++)
			ps[c]=counts[c]/N;
//		System.out.print("probs:");
//		for (double p:ps)
//			System.out.print(" "+p);
//		System.out.print("\n");
		return ps;
	}
	/** ln(2) */
	private static final double logoftwo=Math.log(2);
	/**
	 * Given a probability mass function indicating the frequencies of 
	 * class representation, calculate an "entropy" value using the method
	 * in Tan Steinbach Kumar's "Data Mining" textbook
	 * 
	 * @param ps			the probability mass function
	 * @return				the entropy value calculated
	 */
	private double CalcEntropy(double[] ps){
		double e=0;		
		for (double p:ps){
			if (p != 0) //otherwise it will divide by zero - see TSK p159
				e+=p*Math.log(p)/logoftwo;
		}
		return -e; //according to TSK p158
	}
	/**
	 * Of the M attributes, select {@link RandomForest#Ms Ms} at random.
	 * 
	 * @return		The list of the Ms attributes' indices
	 */
	private ArrayList<Integer> GetVarsToInclude() {
		boolean[] whichVarsToInclude=new boolean[RandomForest.M];

		for (int i=0;i<RandomForest.M;i++)
			whichVarsToInclude[i]=false;
		
		while (true){
			int a=(int)Math.floor(Math.random()*RandomForest.M);
			whichVarsToInclude[a]=true;
			int N=0;
			for (int i=0;i<RandomForest.M;i++)
				if (whichVarsToInclude[i])
					N++;
			if (N == RandomForest.Ms)
				break;
		}
		
		ArrayList<Integer> shortRecord=new ArrayList<Integer>(RandomForest.Ms);
		
		for (int i=0;i<RandomForest.M;i++)
			if (whichVarsToInclude[i])
				shortRecord.add(i);
		return shortRecord;
	}

	/**
	 * Create a boostrap sample of a data matrix
	 * 
	 * @param data		the data matrix to be sampled
	 * @param train		the bootstrap sample
	 * @param test		the records that are absent in the bootstrap sample
	 */
	private void BootStrapSample(ArrayList<int[]> data,ArrayList<int[]> train,ArrayList<int[]> test,int numb){
		ArrayList<Integer> indices=new ArrayList<Integer>(N);
		for (int n=0;n<N;n++)
			indices.add((int)Math.floor(Math.random()*N));
		ArrayList<Boolean> in=new ArrayList<Boolean>(N);
		for (int n=0;n<N;n++)
			in.add(false); //have to initialize it first
		for (int num:indices){
			train.add((data.get(num)).clone());
			in.set(num,true);
		}//System.out.println("created training-data for tree : "+numb);
		for (int i=0;i<N;i++)
			if (!in.get(i))
				test.add((data.get(i)).clone());//System.out.println("created testing-data for tree : "+numb);//everywhere its set to false we get those to test data
		
//		System.out.println("bootstrap N:"+N+" size of bootstrap:"+bootstrap.size());
	}
	/**
	 * Recursively deletes all data records from the tree. This is run after the tree
	 * has been computed and can stand alone to classify incoming data.
	 * 
	 * @param node		initially, the root node of the tree
	 */
	private void FlushData(TreeNode node){
		node.data=null;
		if (node.left != null)
			FlushData(node.left);
		if (node.right != null)
			FlushData(node.right);
	}
	
//	// possible to clone trees
//	private DTree(){}
//	public DTree clone(){
//		DTree copy=new DTree();
//		copy.root=root.clone();
//		return copy;
//	}

	/**
	 * Get the number of data records in the test data matrix that were classified correctly
	 */
	public int getNumCorrect(){
		return correct;
	}
	/**
	 * Get the number of data records left out of the bootstrap sample
	 */
	public int getTotalNumInTestSet(){
		return testN;
	}
	/**
	 * Get the importance level of attribute m for this tree
	 */
	public int getImportanceLevel(int m){
		return importances[m];
	}
//	private void PrintOutNode(TreeNode parent,String init){
//		try {
//			System.out.println(init+"node: left"+parent.left.toString());
//		} catch (Exception e){
//			System.out.println(init+"node: left null");
//		}
//		try {
//			System.out.println(init+" right:"+parent.right.toString());
//		} catch (Exception e){
//			System.out.println(init+"node: right null");
//		}
//		try {
//			System.out.println(init+" isleaf:"+parent.isLeaf);
//		} catch (Exception e){}
//		try {
//			System.out.println(init+" splitAtrr:"+parent.splitAttributeM);
//		} catch (Exception e){}
//		try {
//			System.out.println(init+" splitval:"+parent.splitValue);
//		} catch (Exception e){}
//		try {
//			System.out.println(init+" class:"+parent.Class);
//		} catch (Exception e){}
//		try {
//			System.out.println(init+" data size:"+parent.data.size());
//			PrintOutClasses(parent.data);
//		} catch (Exception e){
//			System.out.println(init+" data: null");
//		}		
//	}
//	private void PrintOutClasses(List<int[]> data){
//		try {
//			System.out.print(" (n="+data.size()+") ");
//			for (int[] record:data)
//				System.out.print(GetClass(record));
//			System.out.print("\n");		
//		}
//		catch (Exception e){
//			System.out.println("PrintOutClasses: data null");	
//		}
//	}
//	public static void PrintBoolArray(boolean[] b) {
//		System.out.print("vars to include: ");
//		for (int i=0;i<b.length;i++)
//			if (b[i])
//				System.out.print(i+" ");
//		System.out.print("\n\n");		
//	}
//
//	public static void PrintIntArray(List<int[]> lower) {
//		System.out.println("tree");
//		for (int i=0;i<lower.size();i++){
//			int[] record=lower.get(i);
//			for (int j=0;j<record.length;j++){
//				System.out.print(record[j]+" ");
//			}
//			System.out.print("\n");
//		}
//		System.out.print("\n");
//		System.out.print("\n");
//	}
}
