package org.archive.nlp.chunk.lpt.addon;

import java.util.HashSet;

public class LTPPosTag {
	//Tag	Description	Example	Tag	Description	Example
	public final static String a  = "a"; //adjective	����
	public final static String nI = "ni";//organization name	���չ�˾
	public final static String b ="b";//other noun-modifier	����, ��ʽ
	public final static String nL ="nl";//location noun	�ǽ�
	public final static String c ="c";//conjunction	��, ��Ȼ	
	public final static String nS ="ns";//geographical name	����
	public final static String d ="d";//adverb	��	
	public final static String nT ="nt";//temporal noun	����, ����
	public final static String e ="e";//exclamation	��	
	public final static String nZ ="nz";//other proper noun	ŵ����
	public final static String g ="g";//morpheme	��, ��	
	public final static String o ="o";//onomatopoeia	����
	public final static String h ="h";//prefix	��, α	
	public final static String p ="p";//preposition	��, ��
	public final static String i ="i";//idiom	�ٻ����	
	public final static String q ="q";//quantity	��
	public final static String j ="j";//abbreviation	���취	
	public final static String r ="r";//pronoun	����
	public final static String k ="k";//suffix	��, ��	
	public final static String u ="u";//auxiliary	��, ��
	public final static String m ="m";//number	һ, ��һ	
	public final static String v ="v";//verb	��, ѧϰ
	public final static String n ="n";//general noun	ƻ��	
	public final static String wP ="wp";//punctuation	������
	public final static String nD ="nd";//direction noun	�Ҳ�	
	public final static String wS ="ws";//foreign words	CPU
	public final static String nH ="nh";//person name	�Ÿ�, ��ķ	
	public final static String x ="x";//non-lexeme	��, ��
	
	private static HashSet<String> FirLevelPosTagSet = new HashSet<String>();
	private static void iniFirLevelPosTagSet(){
		String [] array = {"ni", "nl", "ns", "nt", "nz", "nh", "ws", "n", "i", "j", "v"};
		for(int i=0; i<array.length; i++){
			FirLevelPosTagSet.add(array[i]);
		}
	}
	
	public static int compare(String aPosTag, String bPosTag){
		if(FirLevelPosTagSet.size() == 0){
			iniFirLevelPosTagSet();
		}		
		if(FirLevelPosTagSet.contains(aPosTag)&&FirLevelPosTagSet.contains(bPosTag)){
			//System.out.println("01");
			return 0;
		}else if(!FirLevelPosTagSet.contains(aPosTag) && !FirLevelPosTagSet.contains(bPosTag)){
			//System.out.println("02");
			return 0;
		}else if(FirLevelPosTagSet.contains(aPosTag) && !FirLevelPosTagSet.contains(bPosTag)){
			//System.out.println(-1);
			return -1;
		}else if(!FirLevelPosTagSet.contains(aPosTag) && FirLevelPosTagSet.contains(bPosTag)){}{
			//System.out.println(1);
			return 1;
		}
	}

}
