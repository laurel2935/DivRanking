package org.archive.dataset;

public class DataSetDiretory {
	private static final String _sysTag = "MAC";
	
	//
	public static String ROOT;	
	
	//ntcir-11
	public static String NTCIR11_SM;
	
	//
	public static String STOPWORGS_LDA;
	
	static{
		if(_sysTag.equals("WIN")){
			ROOT = "C:/T/WorkBench/Bench_Dataset/DataSet_DiversifiedRanking/";
			NTCIR11_SM = "ntcir/ntcir-11/SM/";
			
			
		}else{
			STOPWORGS_LDA = "/Users/dryuhaitao/git/DivRanking/DivRanking/dic/stopwords_en.txt";
			
		}
	}

}
