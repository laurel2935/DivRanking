package org.archive.nlp.similarity.editdistance;


import org.archive.nlp.similarity.editdistance.definition.EditUnit;
import org.archive.nlp.similarity.editdistance.definition.SuperString;
import org.archive.util.Language.Lang;


public class GregorEditDistance extends EditDistance {        
    //
    public static double swapCost = 0.5;
    
    private SuperString<? extends EditUnit> S,T;
    //
    private double[][][][] QArray;
    
    public double getEditDistance(SuperString<? extends EditUnit> S,SuperString<? extends EditUnit> T, Lang lang){    
    	this.S = S;
    	this.T = T;
        QArray = new double[S.length()][S.length()][T.length()][T.length()];
        for(int i=0;i<S.length();i++){
            for(int i2=0;i2<S.length();i2++)
                for(int j=0;j<T.length();j++)
                    for(int j2=0;j2<T.length();j2++){
                        QArray[i][i2][j][j2] = Double.MAX_VALUE;
                    }
        }
        
        return Q(0,S.length()-1,0,T.length()-1, lang);
    }  
    
    private double Q(int i0,int i1,int j0,int j1, Lang lang){
        double cost = 0;
        
        if(i1<i0){
        	for(int j = j0; j<=j1; j++){
        		cost += T.elementAt(j).getInsertionCost();
        	}
        	return cost;
        }else if(j1<j0){
        	for(int i=i0; i<=i1; i++){
        		cost += S.elementAt(i).getDeletionCost();
        	}
        	return cost;
        }else if(i1==i0 && j1==j0){
        	cost = S.elementAt(i0).getSubstitutionCost(T.elementAt(j0), lang);        	
        	QArray[i0][i1][j0][j1] = cost;
        	return cost;
        } else if(i1==i0){            
            double minSubstituteValue = 1.0;
            int minPosJ = j0;
            for(int j=j0;j<=j1;j++){
            	double subsitituteValue = S.elementAt(i0).getSubstitutionCost(T.elementAt(j), lang);
            	if(minSubstituteValue > subsitituteValue){
            		minSubstituteValue = subsitituteValue;
            		minPosJ = j;
            	}                	
            }
            for(int j=j0;j<=j1;j++){
            	if(j == minPosJ){
            		cost += minSubstituteValue;             	
            	}else{
            		cost += T.elementAt(j).getInsertionCost();
            	}
            }                 
        }else if(j1==j0){            
        	double minSubstituteValue = 1.0;
            int minPosI = i0;
            for(int i=i0;i<=i1;i++){
            	double subsitituteValue = S.elementAt(i).getSubstitutionCost(T.elementAt(j0), lang);
            	if(minSubstituteValue > subsitituteValue){
            		minSubstituteValue = subsitituteValue;
            		minPosI = i;
            	}                	
            }
            for(int i=i0;i<=i1;i++){
            	if(i == minPosI){
            		cost += minSubstituteValue;             	
            	}else{
            		cost += S.elementAt(i).getDeletionCost();
            	}
            }            	             
        }else{
        	if(QArray[i0][i1][j0][j1]<Double.MAX_VALUE){
        		return QArray[i0][i1][j0][j1];
        	}
            for(int i=i0;i<i1;i++){
                for(int j=j0;j<j1;j++){
                    double c = Math.min(Q(i0,i,j0,j, lang)+Q(i+1,i1,j+1,j1, lang),
                            Q(i0,i,j+1,j1, lang)+Q(i+1,i1,j0,j, lang)+swapCost);
                    if(c<QArray[i0][i1][j0][j1]){
                    	QArray[i0][i1][j0][j1] = c;
                    }
                }
            }
            return QArray[i0][i1][j0][j1];
        }
        QArray[i0][i1][j0][j1] = cost;        
        return cost;
    }
    
    public double getSimilarity(String item1, String item2){
    	return -1;
    }
    
    public static void main(String[] argv) {
        String s1 = "abcxdef";
        String s2 = "defxabc";
        //String s2 = "我的密码我忘记了,我该怎样做呢?";
        GregorEditDistance ed = new GregorEditDistance();
        System.out.println(ed.getEditDistance(SuperString.createCharSuperString(s1), SuperString.createCharSuperString(s2), Lang.Chinese));
    }

	
}

