package com.rf.real.categ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class DTreeCateg {
	
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
	private RandomForestCateg forest;
	
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
	public DTreeCateg(ArrayList<ArrayList<String>> data,RandomForestCateg forest, int treenum) {
		// TODO Auto-generated constructor stub
		this.forest=forest;
		N=data.size();
		importances = new int[RandomForestCateg.M];
		
		ArrayList<ArrayList<String>> train = new ArrayList<ArrayList<String>>(N);
		ArrayList<ArrayList<String>> test = new ArrayList<ArrayList<String>>();
		System.out.println("Creating tree No."+treenum);
		
		BootStrapSample(data,train,test,treenum);
		testN=test.size();
		correct=0;
		
		root=CreateTree(train,treenum);
		FlushData(root, treenum);
		System.out.println("deleted data for tree:"+treenum);
	}
	
	/**
	 * Create a boostrap sample of a data matrix
	 * 
	 * @param data		the data matrix to be sampled
	 * @param train		the bootstrap sample
	 * @param test		the records that are absent in the bootstrap sample
	 */
	private void BootStrapSample(ArrayList<ArrayList<String>> data,ArrayList<ArrayList<String>> train,ArrayList<ArrayList<String>> test,int numb){
		ArrayList<Integer> indices=new ArrayList<Integer>(N);
		for (int n=0;n<N;n++)
			indices.add((int)Math.floor(Math.random()*N));
		ArrayList<Boolean> in=new ArrayList<Boolean>(N);
		for (int n=0;n<N;n++)
			in.add(false); //have to initialize it first
		for (int num:indices){
			train.add((ArrayList<String>) data.get(num).clone());
			in.set(num,true);
		}//System.out.println("created training-data for tree : "+numb);
		for (int i=0;i<N;i++)
			if (!in.get(i))
				test.add((ArrayList<String>) (data.get(i)).clone());//System.out.println("created testing-data for tree : "+numb);//everywhere its set to false we get those to test data
		
//		System.out.println("bootstrap N:"+N+" size of bootstrap:"+bootstrap.size());
	}
	
	/**
	 * This creates the decision tree according to the specifications of random forest trees. 
	 *
	 * @param train		the training data matrix (a bootstrap sample of the original data)
	 * @return			the TreeNode object that stores information about the parent node of the created tree
	 */
	private TreeNode CreateTree(ArrayList<ArrayList<String>> train, int ntree){
		TreeNode root=new TreeNode();
		root.label = "|ROOT|";
		root.data=train;
		//System.out.println("creating ");
		RecursiveSplit(root,ntree);
		return root;
	}
	/**
	 * 
	 * @author TreeNode
	 *
	 */
	private class TreeNode implements Cloneable{
		public boolean isLeaf;
		public ArrayList<TreeNode> ChildNode ;
		public TreeNode left;
		public TreeNode right;
		public int splitAttributeM;
		public boolean spiltonCateg = false;
		public String Class;
		public List<ArrayList<String>> data;
		public String splitValue;//check this if it return false on splitonCateg
		public String label;//Label of each node
		public int generation;
		
		public TreeNode(){
			splitAttributeM=-99;
			splitValue="-99";
			generation=1;
		}
		public TreeNode clone(){ //"data" element always null in clone
			TreeNode copy=new TreeNode();
			copy.isLeaf=isLeaf;
			for(TreeNode TN : ChildNode){
				if(TN != null){
					copy.ChildNode.add(TN.clone());
				}
			}
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
	private class DoubleWrap{//hold the entropy
		public double d;
		public DoubleWrap(double d){
			this.d=d;
		}		
	}
	/**
	 * Evaluates each record and traverses through the tree and returns the predictions
	 * 
	 * @param Each record the the data
	 * @return he predicted class 
	 * 
	 */
	public String Evaluate(ArrayList<String> record, ArrayList<ArrayList<String>> tester){
		TreeNode evalNode=root;
		
		while (true) {
			if(evalNode.isLeaf)
				return evalNode.Class;
			if(evalNode.spiltonCateg){
				// if its categorical
				String nodeLable = record.get(evalNode.splitAttributeM);
				boolean found = false;
				for(TreeNode child:evalNode.ChildNode){
					/*
					 * Check for child with label same the data point
					 */
					if(nodeLable.equalsIgnoreCase(child.label)){//what if the category is not present at all
						evalNode = child;
						found = true;
						break;
					}
						
				}
				//accomodate the missing values
				if(!found){
					// find if supervised or non supervised
					System.out.println("this lable not found :"+nodeLable);
					if(evalNode.data.get(0).size()>record.size()){
						/**
						 * this is labels do not exist ... 
						 * replicate this result RF.C times and then take this as each class and 
						 * 
						 */
					}else{
						/**
						 * Labels exist 
						 */
						nodeLable = changeNodeLabel(evalNode.splitAttributeM,record,evalNode.data);
						record.set(evalNode.splitAttributeM, nodeLable);
						Evaluate(record, tester);
					}
					
				}
			}else{
				//if its real-valued
				String ActualValue = record.get(evalNode.splitAttributeM);
				float Compare = Float.parseFloat(evalNode.splitValue);
				float Actual = Float.parseFloat(ActualValue);
				if(Actual <= Compare){
					/* 
					 * Left Child
					 * Check for the child label as Left
					 */
					for(TreeNode child:evalNode.ChildNode){
						if(child.label.equalsIgnoreCase("Left"))
							evalNode = child;
					}
				}else{
					/*
					 * Right Child
					 * Check for the child label as Right 
					 */
					for(TreeNode child:evalNode.ChildNode){
						if(child.label.equalsIgnoreCase("Right"))
							evalNode = child;
					}
				}
			}
			return null;
		}
	}
	private String changeNodeLabel(int splitAttributeM, ArrayList<String> record,List<ArrayList<String>> data) {
		// TODO Auto-generated method stub
		// get the list of all the attributes where class is that
		String label = record.get(record.size()-1);
		ArrayList<String> ToFind = new ArrayList<String>();
		for(ArrayList<String> DP : data){
			if(DP.get(DP.size()-1).equalsIgnoreCase(label)){
				ToFind.add(DP.get(splitAttributeM));
			}
		}
		String Fill =null;
		Fill = forest.ModeofList(ToFind);
		return Fill;
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
	 * Create a group of nodes and keep their references in node's fields 		//Create a left and right node and keep their references in
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
		
		if (!parent.isLeaf){
			//-------------------------------Step A
			String Class=CheckIfLeaf(parent.data);
			if (Class != null){
				parent.isLeaf=true;
				parent.Class=Class;
//				PrintOutClasses(parent.data);
				return;
			}
			//-------------------------------Step B
			int Nsub=parent.data.size();
//			PrintOutClasses(parent.data);			
			
			parent.ChildNode = new ArrayList<DTreeCateg.TreeNode>();
			for(TreeNode TN: parent.ChildNode){
				TN.generation = parent.generation + 1;
			}
			parent.left=new TreeNode();
			parent.left.generation=parent.generation+1;
			parent.right=new TreeNode();
			parent.right.generation=parent.generation+1;
			
			ArrayList<Integer> vars=GetVarsToInclude();//randomly selects Ms.Nos of attributes from M
			
			DoubleWrap lowestE=new DoubleWrap(Double.MAX_VALUE);
			
			//-------------------------------Step C
			 
			for (int m:vars){
				SortAtAttribute(parent.data,m);//sorts on a particular column in the row
				ArrayList<Integer> indicesToCheck=new ArrayList<Integer>();// which data points to be scrutinized 
				for (int n=1;n<Nsub;n++){
					String classA=GetClass(parent.data.get(n-1));
					String classB=GetClass(parent.data.get(n));
					if(!classA.equalsIgnoreCase(classB))
						indicesToCheck.add(n);
				}
				if (indicesToCheck.size() == 0){//if all the Y-values are same, then get the class directly
					parent.isLeaf=true;
					parent.Class=GetClass(parent.data.get(0));
					continue;
				}
				
				if (indicesToCheck.size() > MIN_SIZE_TO_CHECK_EACH){
					for (int i=0;i<indicesToCheck.size();i+=INDEX_SKIP){
						CheckPosition(m, indicesToCheck.get(i), Nsub, lowestE, parent, Ntreenum);
						if (lowestE.d == 0)//lowestE now has the minimum conditional entropy so IG is max there
							break;
					}
				}else{
					for (int n:indicesToCheck){
						CheckPosition(m, indicesToCheck.get(n), Nsub, lowestE, parent, Ntreenum);
						if (lowestE.d == 0)//lowestE now has the minimum conditional entropy so IG is max there
							break;
					}
				}
				if (lowestE.d == 0)
					break;
			}
			//-------------------------------Step D
				for(int i=0;i<parent.ChildNode.size();i++){
					if(parent.ChildNode.get(i).data.size()==1){
						parent.ChildNode.get(i).isLeaf=true;
						parent.ChildNode.get(i).Class=GetClass(parent.ChildNode.get(i).data.get(0));
					}
					else if(parent.ChildNode.get(i).data.size() < MIN_NODE_SIZE){
						parent.ChildNode.get(i).isLeaf=true;
						parent.ChildNode.get(i).Class=GetMajorityClass(parent.ChildNode.get(i).data);
					}
					else{
						Class = CheckIfLeaf(parent.ChildNode.get(i).data);
						if(Class == null){
							parent.ChildNode.get(i).isLeaf=false;
							parent.ChildNode.get(i).Class=null;
						}else{
							parent.ChildNode.get(i).isLeaf=true;
							parent.ChildNode.get(i).Class=Class;
						}
					}
					
					if(parent.ChildNode.get(i).isLeaf)
						RecursiveSplit(parent.ChildNode.get(i), Ntreenum);
				}
		}
		
	}
	/**
	 * Given a data matrix, return the most popular Y value (the class)
	 * @param data	The data matrix
	 * @return		The most popular class
	 */
	private String GetMajorityClass(List<ArrayList<String>> data){
		// find the max class for this data.
		HashMap<String, Integer> count = new HashMap<String, Integer>();
		for(ArrayList<String> s : data){
			String clas = GetClass(s);
			if(count.containsKey(clas))
				count.put(clas, count.get(clas)+1);
			else
				count.put(clas, 1);			
		}int k =0;String max = null;
		for(Entry<String, Integer> entry : count.entrySet()){
			if(entry.getValue()>k){
				k=entry.getValue();
				max=entry.getKey();
			}
		}return max;
	}
	/**
	 * Checks the {@link #CalcEntropy(double[]) entropy} of an index in a data matrix at a particular attribute (m)
	 * and returns the entropy. If the entropy is lower than the minimum to date (lowestE), it is set to the minimum.
	 * 
	 * If the attribute is real-valued then :  
	 *	 The total entropy is calculated by getting the sub-entropy for below the split point and after the split point.
	 * 	The sub-entropy is calculated by first getting the {@link #GetClassProbs(List) proportion} of each of the classes
	 *	 in this sub-data matrix. Then the entropy is {@link #CalcEntropy(double[]) calculated}. The lower sub-entropy
	 *	 and upper sub-entropy are then weight averaged to obtain the total entropy.
	 * 
	 *If the attribute is categorical-valued then :
	 *	The total entropy is calculated by entropy of the each of categorical-value in the attribute
	 *	The split is done by recursively calling split over each of the categorical value;
	 * 
	 * @param m				the attribute to split on
	 * @param n				the index to check
	 * @param Nsub			the number of records in the data matrix
	 * @param lowestE		the minimum entropy to date
	 * @param parent		the parent node
	 * @return				the entropy of this split
	 */
	private double CheckPosition(int m,int n,int Nsub,DoubleWrap lowestE,TreeNode parent, int nTre){
		ArrayList<String> check = parent.data.get(0);
		String real_categ = check.get(m);
		
		if (n < 1) //exit conditions
			return 0;
		if (n > Nsub)
			return 0;
		
		if(isAlphaNumeric(real_categ)){
			
			//this is a categorical thing
			ArrayList<ArrayList<String>> Collect = new ArrayList<ArrayList<String>>();
			for(int i=0;i<Nsub;i++){
				Collect.add(parent.data.get(i));
			}
			double pr[]=getClassProbs(Collect);
			double ent =CalEntropy(pr);
			if (ent < lowestE.d){
				lowestE.d=ent;
				parent.splitAttributeM=m;
				parent.spiltonCateg = true;
				parent.splitValue=parent.data.get(n).get(m);
				/**
				 * Adding Data to Child
				 * 
				 */
				parent.ChildNode.clear();//remove everything before you add
				
				//Finding number of unique categorical values
				ArrayList<String> count = new ArrayList<String>();
				for(int i=0;i<Nsub;i++){
					String check2 = parent.data.get(i).get(m);
					if(!count.contains(check2))
						count.add(check2);
				}

				//creating each child-node with its own data
				for(String node : count){
					ArrayList<ArrayList<String>> child_data = new ArrayList<ArrayList<String>>();
					for(int i=0;i<Nsub;i++){
						if(node.equalsIgnoreCase(parent.data.get(i).get(m)))
							child_data.add(parent.data.get(i));
					}//adding node and its contents
					TreeNode Child = new TreeNode();
					Child.data = child_data;
					Child.label = node;
					parent.ChildNode.add(Child);					
				}
			}
		}else{
			
			//this is a real valued thing
			ArrayList<ArrayList<String>> lower=GetLower(parent.data,n);
			ArrayList<ArrayList<String>> upper=GetUpper(parent.data,n);
			double pl[]=getClassProbs(lower);
			double pu[]=getClassProbs(upper);
			double eL=CalEntropy(pl);
			double eU=CalEntropy(pu);
		
			double e=(eL*lower.size()+eU*upper.size())/((double)Nsub);
			
			if (e < lowestE.d){
				lowestE.d=e;
				parent.splitAttributeM=m;
				parent.spiltonCateg=true;
				parent.splitValue = parent.data.get(n).get(m);
				
				/**
				 * Adding Data to Left/Right Child
				 * 
				 */
				parent.ChildNode.clear();//remove everything before you add
				
				//adding left child
				TreeNode Child_Left = new TreeNode();
				Child_Left.data=lower;
				Child_Left.label="Left";
				parent.ChildNode.add(Child_Left);
				
				//adding right child
				TreeNode Child_Right = new TreeNode();
				Child_Right.data=upper;
				Child_Left.label="Right";
				parent.ChildNode.add(Child_Right);				
			}
		}
		
		return nTre;
		
	}
	/**
	 * Split a data matrix and return the lower portion
	 * 
	 * @param data		the data matrix to be split
	 * @param nSplit	return all data records below this index in a sub-data matrix
	 * @return			the lower sub-data matrix
	 */
	private ArrayList<ArrayList<String>> GetLower(List<ArrayList<String>> data,int nSplit){
		ArrayList<ArrayList<String>> LS = new ArrayList<ArrayList<String>>();
		for(int n=0;n<nSplit;n++){
			LS.add(data.get(n));
		}return LS;
	}
	/**
	 * Split a data matrix and return the upper portion
	 * 
	 * @param data		the data matrix to be split
	 * @param nSplit	return all data records above this index in a sub-data matrix
	 * @return			the upper sub-data matrix
	 */
	private ArrayList<ArrayList<String>> GetUpper(List<ArrayList<String>> data,int nSplit){
		int N=data.size();
		ArrayList<ArrayList<String>> LS = new ArrayList<ArrayList<String>>();
		for(int n=nSplit;n<N;n++){
			LS.add(data.get(n));
		}return LS;
	}
	/**
	 * Given a data matrix, return a probabilty mass function representing 
	 * the frequencies of a class in the matrix (the y values)
	 * 
	 * @param records		the data matrix to be examined
	 * @return				the probability mass function
	 */
	private double[] getClassProbs(ArrayList<ArrayList<String>> record){
		double N=record.size();
		HashMap<String, Integer > counts = new HashMap<String, Integer>();
		for(ArrayList<String> s : record){
			String clas = GetClass(s);
			if(counts.containsKey(clas))
				counts.put(clas, counts.get(clas)+1);
			else
				counts.put(clas, 1);
		}
		double[] prbs=new double[RandomForestCateg.C];int c =0;
		for(Entry<String, Integer> entry : counts.entrySet()){
			prbs[c]=entry.getValue()/N;c++;
		}return prbs;
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
	private double CalEntropy(double[] ps){
		double e=0;		
		for (double p:ps){
			if (p != 0) //otherwise it will divide by zero - see TSK p159
				e+=p*Math.log(p)/logoftwo;
		}
		return -e; //according to TSK p158
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
	 * Sorts a data matrix by an attribute from lowest record to highest record
	 * 
	 * @param data			the data matrix to be sorted
	 * @param m				the attribute to sort on
	 */
	@SuppressWarnings("unchecked")
	private void SortAtAttribute(List<ArrayList<String>> data,int m){
		ArrayList<String> check = data.get(0);
		String k = check.get(m);
		if(isAlphaNumeric(k)){
			Collections.sort(data,new AttributeComparatorCateg(m));
		}else{
			Collections.sort(data,new AttributeComparatorReal(m));
		}
	}
	/**
	 * This class compares two data records by numerically/categorically comparing a specified attribute
	 * 
	 *
	 */
	private class AttributeComparatorCateg implements Comparator<ArrayList<String>>{		
		/** the specified attribute */
		private int m;
		/**
		 * Create a new comparator
		 * @param m			the attribute in which to compare on
		 */
		public AttributeComparatorCateg(int m){
			this.m=m;
		}
		/**
		 * Compare the two data records. They must be of type int[].
		 * 
		 * @param o1		data record A
		 * @param o2		data record B
		 * @return			-1 if A[m] < B[m], 1 if A[m] > B[m], 0 if equal
		 */
		@Override
		public int compare(ArrayList<String> arg1, ArrayList<String> arg2) {//compare strings
			// TODO Auto-generated method stub
			String a = arg1.get(m);
			String b = arg2.get(m);
			return a.compareToIgnoreCase(b);
		}		
	}/**
	 * This class compares two data records by numerically/categorically comparing a specified attribute
	 * 
	 *
	 */
	private class AttributeComparatorReal implements Comparator<ArrayList<String>>{		
		/** the specified attribute */
		private int m;
		/**
		 * Create a new comparator
		 * @param m			the attribute in which to compare on
		 */
		public AttributeComparatorReal(int m){
			this.m=m;
		}
		/**
		 * Compare the two data records. They must be of type int[].
		 * 
		 * @param arg1		data record A
		 * @param arg2		data record B
		 * @return			-1 if A[m] < B[m], 1 if A[m] > B[m], 0 if equal
		 */
		@Override
		public int compare(ArrayList<String> arg1, ArrayList<String> arg2) {//compare value of strings
			// TODO Auto-generated method stub
			String a = arg1.get(m);float a2 = Float.parseFloat(a);
			String b = arg2.get(m);float b2 = Float.parseFloat(b);
			if(a2<b2)
				return -1;
			else if(a2>b2)
				return 1;
			else
				return 0;
		}		
	}
	/**
	 * Of the M attributes, select {@link RandomForest#Ms Ms} at random.
	 * 
	 * @return		The list of the Ms attributes' indices
	 */
	private ArrayList<Integer> GetVarsToInclude() {
		boolean[] whichVarsToInclude=new boolean[RandomForestCateg.M];

		for (int i=0;i<RandomForestCateg.M;i++)
			whichVarsToInclude[i]=false;
		
		while (true){
			int a=(int)Math.floor(Math.random()*RandomForestCateg.M);
			whichVarsToInclude[a]=true;
			int N=0;
			for (int i=0;i<RandomForestCateg.M;i++)
				if (whichVarsToInclude[i])
					N++;
			if (N == RandomForestCateg.Ms)
				break;
		}
		
		ArrayList<Integer> shortRecord=new ArrayList<Integer>(RandomForestCateg.Ms);
		
		for (int i=0;i<RandomForestCateg.M;i++)
			if (whichVarsToInclude[i])
				shortRecord.add(i);
		return shortRecord;
	}
	/**
	 * Given a data record, return the Y value - take the last index
	 * 
	 * @param record		the data record
	 * @return				its y value (class)
	 */
	public static String GetClass(ArrayList<String> record){
		return record.get(RandomForestCateg.M);
	}
	/**
	 * Given a data matrix, check if all the y values are the same. If so,
	 * return that y value, null if not
	 * 
	 * @param data		the data matrix
	 * @return			the common class (null if not common)
	 */
	private String CheckIfLeaf(List<ArrayList<String>> data){
//		System.out.println("checkIfLeaf");
		boolean isLeaf=true;
		String ClassA=GetClass(data.get(0));
		for (int i=1;i<data.size();i++){			
			ArrayList<String> recordB=data.get(i);
			if (ClassA.equalsIgnoreCase(GetClass(recordB))){
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
	 * Recursively deletes all data records from the tree. This is run after the tree
	 * has been computed and can stand alone to classify incoming data.
	 * 
	 * @param node		initially, the root node of the tree
	 * @param treenum 
	 */
	private void FlushData(TreeNode node, int treenum){
		node.data=null;
		if(node.ChildNode!=null){
			for(TreeNode TN : node.ChildNode){
				if(TN != null)
					FlushData(TN,treenum);
			}
		}
		if (node.left != null)
			FlushData(node.left,treenum);
		if (node.right != null)
			FlushData(node.right,treenum);
	}

}
