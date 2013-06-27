Random Forests
===============

How to grow a Decision Tree
----------------------------

LearnUnprunedTree(X,Y)

Input: X a matrix of R rows and M columns where Xij = the value of the j'th attribute in the i'th input datapoint. Each column consists of either all real values or all categorical values.
Input: Y a vector of R elements, where Yi = the output class of the i'th datapoint. The Yi values are categorical.
Output: An Unpruned decision tree

If all records in X have identical values in all their attributes (this includes the case where R<2), return a Leaf Node predicting the majority output, breaking ties randomly. This case also includes
If all values in Y are the same, return a Leaf Node predicting this value as the output
Else
    select m variables at random out of the M variables
    For j = 1:m
        If j'th attribute is categorical
            IGj = IG(Y|Xj) (see Information Gain)            
        Else (j'th attribute is real-valued)
            IGj = IG*(Y|Xj) (see Information Gain)
    Let j* = argmaxj IGj (this is the splitting attribute we'll use)
    If j* is categorical then
        For each value v of the j'th attribute
            Let Xv = subset of rows of X in which Xij = v. Let Yv = corresponding subset of Y
            Let Childv = LearnUnprunedTree(Xv,Yv)
        Return a decision tree node, splitting on j'th attribute. The number of children equals the number of values of the j'th attribute, and the v'th child is Childv
    Else j* is real-valued and let t be the best split threshold
        Let XLO = subset of rows of X in which Xij <= t. Let YLO = corresponding subset of Y
        Let ChildLO = LearnUnprunedTree(XLO,YLO)
        Let XHI = subset of rows of X in which Xij > t. Let YHI = corresponding subset of Y
        Let ChildHI = LearnUnprunedTree(XHI,YHI)
        Return a decision tree node, splitting on j'th attribute. It has two children corresponding to whether the j'th attribute is above or below the given threshold.

Note: There are alternatives to Information Gain for splitting nodes
 

Information gain
-----------------

nominal attributes
suppose X can have one of m values V1,V2,...,Vm
P(X=V1)=p1, P(X=V2)=p2,...,P(X=Vm)=pm
 
H(X)= -sumj=1m pj log2 pj (The entropy of X)
H(Y|X=v) = the entropy of Y among only those records in which X has value v
H(Y|X) = sumj pj H(Y|X=vj)
IG(Y|X) = H(Y) - H(Y|X)

real-valued attributes
suppose X is real valued
define IG(Y|X:t) as H(Y) - H(Y|X:t)
define H(Y|X:t) = H(Y|X<t) P(X<t) + H(Y|X>=t) P(X>=t)
define IG*(Y|X) = maxt IG(Y|X:t)

How to grow a Random Forest
----------------------------

Each tree is grown as follows:

if the number of cases in the training set is N, sample N cases at random -but with replacement, from the original data. This sample will be the training set for the growing tree.
if there are M input variables, a number m << M is specified such that at each node, m variables are selected at random out of the M and the best split on these m is used to split the node. The value of m is held constant during the forest growing.
each tree is grown to its large extent possible. There is no pruning.
Random Forest parameters
source : [2]
Random Forests are easy to use, the only 2 parameters a user of the technique has to determine are the number of trees to be used and the number of variables (m) to be randomly selected from the available set of variables.
Breinman's recommendations are to pick a large number of trees, as well as the square root of the number of variables for m.
 

How to predict the label of a case 
------------------------------------
Classify(node,V)
    Input: node from the decision tree, if node.attribute = j then the split is done on the j'th attribute

    Input: V a vector of M columns where Vj = the value of the j'th attribute.
    Output: label of V

    If node is a Leaf then
            Return the value predicted by node

    Else
            Let j = node.attribute
            If j is categorical then
                    Let v = Vj
                    Let childv = child node corresponding to the attribute's value v
                    Return Classify(childv,V)

            Else j is real-valued
                    Let t = node.threshold (split threshold)
                    If Vj < t then
                            Let childLO = child node corresponding to (<t)
                            Return Classify(childLO,V)
                    Else
                            Let childHI = child node corresponding to (>=t)
                            Return Classify(childHI,V)
 

The out of bag (oob) error estimation
--------------------------------------

in random forests, there is no need for cross-validation or a separate test set to get an unbiased estimate of the test set error. It is estimated internally, during the run, as follows:

each tree is constructed using a different bootstrap sample from the original data. About one-third of the cases left of the bootstrap sample and not used in the construction of the kth tree.
put each case left out in the construction of the kth tree down the kthtree to get a classification. In this way, a test set classification is obtained for each case in about one-thrid of the trees. At the end of the run, take j to be the class that got most of the the votes every time case n was oob. The proportion of times that j is not equal to the true class of n averaged over all cases is the oob error estimate. This has proven to be unbiased in many tests.
