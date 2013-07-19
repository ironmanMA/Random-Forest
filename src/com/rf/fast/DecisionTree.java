package com.rf.fast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;

public class DecisionTree {
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
	
	/** This keeps track of all the predictions done by this tree */
	public ArrayList<String> predictions;
	
	/** This is the root of the Decision Tree */
	private TreeNode root;
	
	/** This is a pointer to the Random Forest this decision tree belongs to */
	private RandomForest forest;
	
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
	public DecisionTree(ArrayList<ArrayList<String>> data,RandomForest forest, int treenum) {
		// TODO Auto-generated constructor stub
		this.forest=forest;
		N=data.size();
		importances = new int[RandomForest.M];
		
		ArrayList<ArrayList<String>> train = new ArrayList<ArrayList<String>>(N);
		ArrayList<ArrayList<String>> test = new ArrayList<ArrayList<String>>();
		
		BootStrapSample(data,train,test,treenum);
		testN=test.size();
		correct=0;
		
		root=CreateTree(train,treenum);
		FlushData(root, treenum);
	}
	
	/**
	 * Create a boostrap sample of a data matrix
	 * 
	 * @param data		the data matrix to be sampled
	 * @param train		the bootstrap sample
	 * @param test		the records that are absent in the bootstrap sample
	 */
	@SuppressWarnings("unchecked")
	private void BootStrapSample(ArrayList<ArrayList<String>> data,ArrayList<ArrayList<String>> train,ArrayList<ArrayList<String>> test,int numb){
		ArrayList<Integer> indices=new ArrayList<Integer>();
		for (int n=0;n<N;n++)
			indices.add((int)Math.floor(Math.random()*N));
		ArrayList<Boolean> in=new ArrayList<Boolean>();
		for (int n=0;n<N;n++)
			in.add(false); //have to initialize it first
		for (int num:indices){
			ArrayList<String> k = data.get(num);
			train.add((ArrayList<String>) k.clone());
			in.set(num,true);
		}//System.out.println("created training-data for tree : "+numb);
		for (int i=0;i<N;i++)
			if (!in.get(i))
				test.add(data.get(i));//System.out.println("created testing-data for tree : "+numb);//everywhere its set to false we get those to test data
		
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
	public class TreeNode implements Cloneable{
		public boolean isLeaf;
		public ArrayList<TreeNode> ChildNode ;
		public HashMap<String, String> Missingdata;
//		public TreeNode left;
//		public TreeNode right;
		public int splitAttributeM;//which attribute its split on...
		public boolean spiltonCateg ;
		public String Class;
		public ArrayList<ArrayList<String>> data;
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
//			if (left != null) //otherwise null
//				copy.left=left.clone();
//			if (right != null) //otherwise null
//				copy.right=right.clone();
			copy.splitAttributeM=splitAttributeM;
			copy.Class=Class;
			copy.splitValue=splitValue;
			copy.spiltonCateg = spiltonCateg;
			copy.label=label;
			return copy;
		}
	}
	/**
	 * hold the entropy
	 * @author Mohammad
	 *
	 */
	private class DoubleWrap{
		public double d;
		public DoubleWrap(double d){
			this.d=d;
		}		
	}
	/**
	 * This method will get the classes and will return the updates
	 * 
	 */
	public ArrayList<String> CalculateClasses(ArrayList<ArrayList<String>> traindata,ArrayList<ArrayList<String>> testdata, int treenumber){
		ArrayList<String> predicts = new ArrayList<String>();
		for(ArrayList<String> record:testdata){
			String Clas = Evaluate(record, traindata);
			if(Clas==null){
				predicts.add("n/a");
			}
			else
			predicts.add(Clas);
		}
		predictions = predicts;
		return predicts;
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
				else{
					if(evalNode.spiltonCateg){
					// if its categorical
						String recordCategory = record.get(evalNode.splitAttributeM);
						boolean found=false;String Res = evalNode.Missingdata.get(GetClass(record));
						
						for(TreeNode child:evalNode.ChildNode){
							
							// Check for child with label same the data point
							if(recordCategory.equalsIgnoreCase(child.label)){//what if the category is not present at all
								evalNode = child;
								found = true;
								break;
							}
						}if(!found){
							for(TreeNode child:evalNode.ChildNode){
								if(Res!=null){
									if(Res.trim().equalsIgnoreCase(child.label)){
										evalNode = child;
										break;
									}
								}else{
									return "n/a";
								}
							}
						}
					}else{
						//if its real-valued
						double Compare = Double.parseDouble(evalNode.splitValue);
						double Actual = Double.parseDouble(record.get(evalNode.splitAttributeM));
						if(Actual <= Compare){
							if(evalNode.ChildNode.get(0).label.equalsIgnoreCase("Left"))
								evalNode=evalNode.ChildNode.get(0);
							else
								evalNode=evalNode.ChildNode.get(1);
//							evalNode=evalNode.left;
//							System.out.println("going in child :"+evalNode.label);
						}else{
							if(evalNode.ChildNode.get(0).label.equalsIgnoreCase("Right"))
								evalNode=evalNode.ChildNode.get(0);
							else
								evalNode=evalNode.ChildNode.get(1);
//							evalNode=evalNode.right;
//							System.out.println("going in child :"+evalNode.label);
						}
					}
				}
		}
	}
	/**
	 * 
	 * @param data
	 * @param splitAttributeM
	 * @param classofRecord
	 * @return
	 */
	private  TreeNode getChildtoTraverse(ArrayList<TreeNode> Chil,int splitAttributeM, String classofRecord) {
		// TODO Auto-generated method stub
		int max=0;TreeNode res=new TreeNode();
		for(int i=0;i<Chil.size();i++){
			if(Chil.get(i)!=null && Chil.get(i).data.size()>0){
				int k=0;
				for(ArrayList<String> SSS:Chil.get(i).data){
					if(GetClass(SSS).equalsIgnoreCase(classofRecord))
						k++;
				}if(k>max){
					max=k;
					res = Chil.get(i);
				}
			}
		}return res;
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
				return;
			}
			
			
			//-------------------------------Step B
			int Nsub=parent.data.size();			
			
			parent.ChildNode = new ArrayList<DecisionTree.TreeNode>();
			for(TreeNode TN: parent.ChildNode){
				TN = new TreeNode();
				TN.generation = parent.generation+1;
			}
			
			ArrayList<Integer> vars=GetVarsToInclude();//randomly selects Ms.Nos of attributes from M
			DoubleWrap lowestE=new DoubleWrap(Double.MAX_VALUE);
			
			
			//-------------------------------Step C
			for (int m:vars){//m is from 0-M
				ArrayList<Integer> DataPointToCheck=new ArrayList<Integer>();// which data points to be scrutinized 
				
				SortAtAttribute(parent.data,m);//sorts on a particular column in the row
				for (int n=1;n<Nsub;n++){
					String classA=GetClass(parent.data.get(n-1));
					String classB=GetClass(parent.data.get(n));
					if(!classA.equalsIgnoreCase(classB))
						DataPointToCheck.add(n);
				}
				
				if (DataPointToCheck.size() == 0){//if all the Y-values are same, then get the class directly
					parent.isLeaf=true;
					parent.Class=GetClass(parent.data.get(0));
					continue;
				}
				
				if (DataPointToCheck.size() > MIN_SIZE_TO_CHECK_EACH){
					for (int i=0;i<DataPointToCheck.size();i+=INDEX_SKIP){
						CheckPosition(m, DataPointToCheck.get(i), Nsub, lowestE, parent, Ntreenum);
						if (lowestE.d == 0)//lowestE now has the minimum conditional entropy so IG is max there
							break;
					}
				}else{
					for (int k:DataPointToCheck){
						CheckPosition(m,k, Nsub, lowestE, parent, Ntreenum);
						if (lowestE.d == 0)//lowestE now has the minimum conditional entropy so IG is max there
							break;
					}
				}
				if (lowestE.d == 0)
					break;
			}
			//System.out.println("Adding "+parent.ChildNode.size()+" children at level: "+parent.generation);
			//-------------------------------Step D
			for(TreeNode Child:parent.ChildNode){
				if(Child.data.size()==1){
					Child.isLeaf=true;
					Child.Class=GetClass(Child.data.get(0));
				}else if(Child.data.size()<MIN_NODE_SIZE){
					Child.isLeaf=true;
					Child.Class=GetMajorityClass(Child.data);
				}else{
					Class=CheckIfLeaf(Child.data);
					if(Class==null){
						Child.isLeaf=false;
						Child.Class=null;
					}else{
						Child.isLeaf=true;
						Child.Class=Class;
					}
				}
				if(!Child.isLeaf){
					RecursiveSplit(Child, Ntreenum);
				}
			}
		}
	}
	
	/**
	 * Sorts a data matrix by an attribute from lowest record to highest record
	 * 
	 * @param data			the data matrix to be sorted
	 * @param m				the attribute to sort on
	 */
	private void SortAtAttribute(ArrayList<ArrayList<String>> data, int m) {
		if(forest.DataAttributes.get(m) == 'C')
			System.out.print("");//Collections.sort(data,new AttributeComparatorCateg(m));
		else
			Collections.sort(data,new AttributeComparatorReal(m));
		
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
	}
	
	/**
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
			double a2 = Double.parseDouble(arg1.get(m));
			double b2 = Double.parseDouble(arg2.get(m));
			if(a2<b2)
				return -1;
			else if(a2>b2)
				return 1;
			else
				return 0;
		}		
	}

	/**
	 * Given a data matrix, return the most popular Y value (the class)
	 * @param data	The data matrix
	 * @return		The most popular class
	 */
	private String GetMajorityClass(ArrayList<ArrayList<String>> data){
		// find the max class for this data.
		ArrayList<String> ToFind = new ArrayList<String>();
		for(ArrayList<String> s:data){
			ToFind.add(s.get(s.size()-1));
		}
		String MaxValue = null; int MaxCount = 0;
		for(String s1:ToFind){
			int count =0;
			for(String s2:ToFind){
				if(s2.equalsIgnoreCase(s1))
					count++;
			}
			if(count > MaxCount){
				MaxValue = s1;
				MaxCount = count;
			}
		}return MaxValue;
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
	
		double entropy =0;
		
		if (n < 1) //exit conditions
			return 0;
		if (n > Nsub)
			return 0;
		
		if(forest.DataAttributes.get(m)=='C'){
			
			//this is a categorical thing
			// find out the distinct values in that attribute...from parent.data
			ArrayList<String> uni_categ = new ArrayList<String>(); //unique categories
			ArrayList<String> uni_classes = new ArrayList<String>(); //unique classes
			HashMap<String, String> ChildMissingMap = new HashMap<String, String>();// Class Vs Node-label
			HashMap<String, Integer> ChilFreq = new HashMap<String, Integer>();//Node-Label Vs frequency
			
			for(ArrayList<String> s:parent.data){
				if(!uni_categ.contains(s.get(m).trim())){
					uni_categ.add(s.get(m).trim());
					ChilFreq.put(s.get(m), 0);
				}
					
				if(!uni_classes.contains(GetClass(s)))
					uni_classes.add(GetClass(s));
			}
			
			//data pertaining to each of the value
			HashMap<String, ArrayList<ArrayList<String>>> ChildDataMap = new HashMap<String, ArrayList<ArrayList<String>>>();
			for(String s:uni_categ){
				ArrayList<ArrayList<String>> child_data = new ArrayList<ArrayList<String>>();
				for(ArrayList<String> S:parent.data){
					if(s.trim().equalsIgnoreCase(S.get(m).trim()))
						child_data.add(S);
				}
				ChildDataMap.put(s, child_data);
			}
			
			//can merge the above two
			//Adding missing-data-suits
			for(String S1:uni_classes){
				int max=0;String Resul = null;
				for(ArrayList<String> S2:parent.data){
					if(GetClass(S2).equalsIgnoreCase(S1)){
						if(ChilFreq.containsKey(S2.get(m)))
							ChilFreq.put(S2.get(m), ChilFreq.get(S2.get(m))+1);
					}
					if(ChilFreq.get(S2.get(m))>max){
						max=ChilFreq.get(S2.get(m));
						Resul = S2.get(m);
					}
				}
				ChildMissingMap.put(S1, Resul);//System.out.println("Mapping Class: "+S1+" to attribute: "+Resul);
			}
			//calculating entropy
			for(Entry<String,ArrayList<ArrayList<String>>> entry:ChildDataMap.entrySet()){
				entropy+=CalEntropy(getClassProbs(entry.getValue()))*entry.getValue().size();
			}
			entropy = entropy/((double)Nsub);
			//if its the least...
			if (entropy < lowestE.d){
				lowestE.d=entropy;
				parent.splitAttributeM=m;
				parent.spiltonCateg = true;
				parent.splitValue=parent.data.get(n).get(m);
				parent.Missingdata=ChildMissingMap;
				/**
				 * Adding Data to Child
				 * 
				 */
				ArrayList<TreeNode> Children = new ArrayList<TreeNode>();
				for(Entry<String,ArrayList<ArrayList<String>>> entry:ChildDataMap.entrySet()){
					TreeNode Child = new TreeNode();
					Child.data=entry.getValue();
					Child.label=entry.getKey();
					Children.add(Child);
				}
				parent.ChildNode=Children;
			}
		}else{
			
			//this is a real valued thing
			
			HashMap<String, ArrayList<ArrayList<String>>> UpLo = GetUpperLower(parent.data, n, m);
			
			ArrayList<ArrayList<String>> lower = UpLo.get("lower"); 
			ArrayList<ArrayList<String>> upper = UpLo.get("upper"); 
			
			ArrayList<Double> pl=getClassProbs(lower);
			ArrayList<Double> pu=getClassProbs(upper);
			double eL=CalEntropy(pl);
			double eU=CalEntropy(pu);
		
			entropy =(eL*lower.size()+eU*upper.size())/((double)Nsub);
			
			if (entropy < lowestE.d){
				lowestE.d=entropy;
				parent.splitAttributeM=m;
				parent.spiltonCateg=false;
				parent.splitValue = parent.data.get(n).get(m).trim();
				/**
				 * Adding Data to Left/Right Child
				 * 
				 */
				ArrayList<TreeNode> Children2 = new ArrayList<TreeNode>();
				TreeNode Child_left = new TreeNode();
				Child_left.data=lower;
				Child_left.label="Left";
				Children2.add(Child_left);
				TreeNode Child_Right = new TreeNode();
				Child_Right.data=upper;
				Child_Right.label="Right";
				Children2.add(Child_Right);
				parent.ChildNode=Children2;//clone karo....
			}
		}
		return entropy;
	}
	
	/**
	 * Returns lower and upper data for paret.data
	 * 
	 * @param data	parent data
	 * @param n2	data point
	 * @param m		attribute value
	 * @return		map of upper and lower
	 */
	private HashMap<String, ArrayList<ArrayList<String>>> GetUpperLower(ArrayList<ArrayList<String>> data, int n2,int m) {
		
		HashMap<String, ArrayList<ArrayList<String>>> UpperLower = new HashMap<String, ArrayList<ArrayList<String>>>();
		ArrayList<ArrayList<String>> lowerr = new ArrayList<ArrayList<String>>(); 
		ArrayList<ArrayList<String>> upperr = new ArrayList<ArrayList<String>>(); 
		for(int n=0;n<n2;n++)
			lowerr.add(data.get(n));
		for(int n=n2;n<data.size();n++)
			upperr.add(data.get(n));
		UpperLower.put("lower", lowerr);
		UpperLower.put("upper", upperr);
		
		return UpperLower;
	}

	/**
	 * Given a data matrix, return a probabilty mass function representing 
	 * the frequencies of a class in the matrix (the y values)
	 * 
	 * @param records		the data matrix to be examined
	 * @return				the probability mass function
	 */
	private ArrayList<Double> getClassProbs(ArrayList<ArrayList<String>> record){
		double N=record.size();
		HashMap<String, Integer > counts = new HashMap<String, Integer>();
		for(ArrayList<String> s : record){
			String clas = GetClass(s);
			if(counts.containsKey(clas))
				counts.put(clas, counts.get(clas)+1);
			else
				counts.put(clas, 1);
		}
		ArrayList<Double> probs = new ArrayList<Double>();
		for(Entry<String, Integer> entry : counts.entrySet()){
			double prob = entry.getValue()/N;
			probs.add(prob);
		}return probs;
	}
	
	/**
	 *  ln(2)   
	 */
	private static final double logoftwo=Math.log(2);
	
	/**
	 * Given a probability mass function indicating the frequencies of class representation, calculate an "entropy" value using the method
	 * in Tan|Steinbach|Kumar's "Data Mining" textbook
	 * 
	 * @param ps			the probability mass function
	 * @return				the entropy value calculated
	 */
	private double CalEntropy(ArrayList<Double> ps){
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
		return shortRecord;//values from 0-M
	}
	/**
	 * Given a data record, return the Y value - take the last index
	 * 
	 * @param record		the data record
	 * @return				its y value (class)
	 */
	public static String GetClass(ArrayList<String> record){
		return record.get(RandomForest.M).trim();
	}
	/**
	 * Given a data matrix, check if all the y values are the same. If so,
	 * return that y value, null if not
	 * 
	 * @param data		the data matrix
	 * @return			the common class (null if not common)
	 */
	private String CheckIfLeaf(ArrayList<ArrayList<String>> data){
//		System.out.println("checkIfLeaf");
		boolean isCLeaf=true;
		String ClassA=GetClass(data.get(0));
		for(ArrayList<String> record : data){
			if(!ClassA.equalsIgnoreCase(GetClass(record))){
				isCLeaf = false;
				return null;
			}
		}
		if (isCLeaf)
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
	}

}
