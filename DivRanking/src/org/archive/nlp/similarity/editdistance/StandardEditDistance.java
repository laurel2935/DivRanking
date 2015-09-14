package org.archive.nlp.similarity.editdistance;

import org.archive.nlp.similarity.editdistance.definition.EditUnit;
import org.archive.nlp.similarity.editdistance.definition.SuperString;
import org.archive.util.Language.Lang;



public class StandardEditDistance extends EditDistance {       
    //
    public double getEditDistance(SuperString<? extends EditUnit> X, SuperString<? extends EditUnit> Y, Lang lang){
    	double[][] D; //
        
        int m = X.length(); 
        int n = Y.length(); 
        //char ch_x_i;       
        //char ch_y_j;       
        
        if(m == 0){
        	double distance = 0.0;
        	for(int j=0; j<n; j++){
        		distance += Y.elementAt(j).getInsertionCost();
        	}
            return distance;
        }else if(n == 0){
        	double distance = 0.0;
        	for(int i=0; i<m; i++){
        		distance += X.elementAt(i).getDeletionCost();
        	}
            return distance;
        }
                      
        D = new double[n+1][m+1];
        D[0][0] = 0.0; //
        
        /** 初始化D[0][j] */
        for(int j = 1; j<=m; j++){
            D[0][j] = D[0][j-1]+X.elementAt(j-1).getDeletionCost();
        }
        
        /** 初始化D[i][0] */
        for(int i = 1;i<=n; i++){
            D[i][0] = D[i-1][0]+ Y.elementAt(i-1).getInsertionCost();
        }        
        
        for(int i=1; i<=m; i++){
        	EditUnit unit_x_i = X.elementAt(i-1);
            for(int j=1; j<=n; j++){
            	EditUnit unit_y_j = Y.elementAt(j-1);
                double cost = unit_x_i.getSubstitutionCost(unit_y_j, lang);
                D[j][i] = Math.min(D[j-1][i]+Y.elementAt(j-1).getInsertionCost(),D[j][i-1]+X.elementAt(i-1).getDeletionCost());
                D[j][i] = Math.min(D[j][i], D[j-1][i-1]+cost);
            }
        }
        
        return D[n][m];
    }
	//
    public double getSimilarity(String item1, String item2){
    	return -1;
    }
    
    
    public static void main(String[] args) {
        String s1 = "abcdefg";
        String s2 = "gcdefab";
        
        StandardEditDistance ed = new StandardEditDistance();        
        s1 = "";
        s2 = "";
        //System.out.println(ed.getEditDistance(SuperString.createCharSuperString(s1), SuperString.createCharSuperString(s2)));        
        //System.out.println(ed.getEditDistance(SuperString.createWordSuperString(s1), SuperString.createWordSuperString(s2)));
     }

	

}
