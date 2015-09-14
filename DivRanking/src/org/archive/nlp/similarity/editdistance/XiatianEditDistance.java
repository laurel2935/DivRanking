package org.archive.nlp.similarity.editdistance;


import org.archive.nlp.similarity.editdistance.definition.EditUnit;
import org.archive.nlp.similarity.editdistance.definition.SuperString;
import org.archive.util.Language.Lang;



/**
 * 夏天提出的新的支持非相邻块交互的编辑距离算法
 * 
 * @author <a href="mailto:iamxiatian@gmail.com">夏天</a>
 * @organization 中国人民大学信息资源管理学院 知识工程实验室
 */
public class XiatianEditDistance extends EditDistance {
	
	public double getSimilarity(String s1, String s2){
		return -1;
		/*
    	
    	SuperString<SemanticTermEditUnit> S = SuperString.createSemanticTermSuperString(s1);
    	SuperString<SemanticTermEditUnit> T = SuperString.createSemanticTermSuperString(s2);
    	
    	return 1-(getEditDistance(S, T))/(Math.max(S.length(), T.length()));
    	*/
    	
    }
	
    /** 块交换代价 */
	public static double swapCost = 0.5;
    
    private SuperString<? extends EditUnit> S,T;
    private double[][][][] QArray;
    
    public double getEditDistance(SuperString<? extends EditUnit> S, SuperString<? extends EditUnit> T, Lang lang){
    	this.S = S;
    	this.T = T;
    	QArray = new double[S.length()+1][S.length()+1][T.length()+1][T.length()+1];
        for(int i=0;i<=S.length();i++){
            for(int i2=0;i2<=S.length();i2++)
                for(int j=0;j<=T.length();j++)
                    for(int j2=0;j2<=T.length();j2++){
                        QArray[i][i2][j][j2]=Double.MAX_VALUE;
                    }
        }        
        return Q(0,S.length()-1,0,T.length()-1, lang);
    }
    
    private double Q(int i1,int im,int j1,int jn, Lang lang){
    	if(QArray[i1][im][j1][jn]<Double.MAX_VALUE){
    		return QArray[i1][im][j1][jn];
    	}
        double cost = 0;                
        if(im<i1){
        	for(int j = j1; j<=jn; j++){
        		cost += T.elementAt(j).getInsertionCost();
        	}        	
        }else if(jn<j1){
        	for(int i=i1; i<=im; i++){
        		cost += S.elementAt(i).getDeletionCost();
        	}
        }else if(im==i1 && jn==j1){
        	cost = S.elementAt(i1).getSubstitutionCost(T.elementAt(j1), lang);        	
        } else if(i1==im){            
            double minSubValue = S.elementAt(i1).getSubstitutionCost(T.elementAt(j1), lang);
            int minPosJ = j1;
            for(int j=j1+1;j<=jn;j++){
            	double subValue = S.elementAt(i1).getSubstitutionCost(T.elementAt(j), lang);
            	if(minSubValue > subValue){
            		minSubValue = subValue;
            		minPosJ = j;
            	}                	
            }
            for(int j=j1;j<=jn;j++){
            	if(j == minPosJ){
            		cost += minSubValue;             	
            	}else{
            		cost += T.elementAt(j).getInsertionCost();
            	}
            }              
        }else if(j1==jn){    
        	int minPosI = i1;
        	double minSubValue = S.elementAt(i1).getSubstitutionCost(T.elementAt(j1), lang);            
            for(int i=i1+1;i<=im;i++){
            	double subValue = S.elementAt(i).getSubstitutionCost(T.elementAt(j1), lang);
            	if(minSubValue > subValue){
            		minSubValue = subValue;
            		minPosI = i;
            	}                	
            }
            for(int i=i1;i<=im;i++){
            	if(i == minPosI){
            		cost += minSubValue;             	
            	}else{
            		cost += S.elementAt(i).getDeletionCost();
            	}
            }            
        }else{        	
        	cost = QArray[i1][im][j1][jn];
            loop:for(int i=i1;i<im;i++){
                //block X divide to 3 parts.
                for(int LX=0;LX<=im-i;LX++){                    
                    //process Y sentence
                    for(int j=j1;j<jn;j++){
                    	//if(cost<=swapCost)break;
                        for(int LY=0;LY<=jn-j;LY++){                                 	
                        	//不交换的代价
                            double cost1 = Q(i1,i,j1,j, lang)+Q(i+1,i+LX,j+1,j+LY, lang)+Q(i+LX+1,im,j+LY+1,jn, lang);
                            //交互代价
                            double cost2 = Q(i1,i,j+LY+1,jn, lang)+Q(i+1,i+LX,j+1,j+LY, lang)+Q(i+LX+1,im,j1,j, lang)+swapCost;
                            cost = Math.min(Math.min(cost1, cost2),cost);
                            if(cost == 0) break loop;
                        }
                    }                    
                }             
            }            
        }        
        
        QArray[i1][im][j1][jn] = cost;
        return cost;
    }
    
    public static void main(String[] argv) {    	
        EditDistance ed = new XiatianEditDistance();
        String s1 = "abcxdef";
        String s2 = "def";
        //String s2 = "我的密码我忘记了,我该怎样做呢?";
       // System.out.println(ed.getEditDistance(SuperString.createCharSuperString(s1), SuperString.createCharSuperString(s2)));

    }
}

