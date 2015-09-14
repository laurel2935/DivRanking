package org.archive.nlp.tokenizer.ictclas;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class ICTCLAS2014 {
	//
	private static final boolean DEBUG = false;
	
	public static boolean CONFIGED = false;

	//
	public interface CLibrary extends Library {
		//
		CLibrary Instance = (CLibrary) Native.loadLibrary(
				"./ICTCLAS/NLPIR", CLibrary.class);
		
		public int NLPIR_Init(String sDataPath, int encoding,
				String sLicenceCode);
				
		public String NLPIR_ParagraphProcess(String sSrc, int bPOSTagged);

		public String NLPIR_GetKeyWords(String sLine, int nMaxKeyLimit,
				boolean bWeightOut);
		public int NLPIR_AddUserWord(String sWord);//add by qp 2008.11.10
		public int NLPIR_DelUsrWord(String sWord);//add by qp 2008.11.10
		
		public void NLPIR_Exit();
	}

	public static String transString(String aidString, String ori_encoding, String new_encoding) {
		try {
			return new String(aidString.getBytes(ori_encoding), new_encoding);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void iniConfig(){
		String _DataDir = "ICTCLAS/";
		// String system_charset = "GBK";//GBK----0
		//String system_charset = "UTF-8";
		int charset_type = 1;
		
		int init_flag = CLibrary.Instance.NLPIR_Init(_DataDir, charset_type, "0");

		if (0 == init_flag) {
			System.err.println("error!");
			return;
		}else{
			System.out.println("ok！");
		}
		//
		CONFIGED = true;
	}
	
	public static void exitConfig(){
		try {
			CLibrary.Instance.NLPIR_Exit();
			CONFIGED = false;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	//interface
	public static ArrayList<String> segment(String text){		
		if(!CONFIGED){
			iniConfig();
			//CONFIGED = true;
		}
		//
		ArrayList<String> wordList = new ArrayList<String>();
		String sentence = ICTCLAS2014.CLibrary.Instance.NLPIR_ParagraphProcess(text, 0);
		String [] array = sentence.split(" ");
		for(int i=0; i<array.length; i++){
			wordList.add(array[i]);
		}
		if(DEBUG){
			System.out.println(wordList);
		}
		return wordList.size()>0? wordList:null;		
	}
	
	public static void check(){
		String _DataDir = "ICTCLAS/";
		// String system_charset = "GBK";//GBK----0
		String system_charset = "UTF-8";
		int charset_type = 1;
		
		int init_flag = CLibrary.Instance.NLPIR_Init(_DataDir, charset_type, "0");

		if (0 == init_flag) {
			System.err.println("初始化失败！");
			return;
		}

		String sInput = "据悉，质检总局已将最新有关情况再次通报美方，要求美方加强对输华玉米的产地来源、运输及仓储等环节的管控措施，有效避免输华玉米被未经我国农业部安全评估并批准的转基因品系污染。";

		String nativeBytes = null;
		try {
			int bPOSTagged = 0;
			
			nativeBytes = CLibrary.Instance.NLPIR_ParagraphProcess(sInput, bPOSTagged);			

			System.out.println("分词结果为： " + nativeBytes);
			
			CLibrary.Instance.NLPIR_AddUserWord("要求美方加强对输 n");
			CLibrary.Instance.NLPIR_AddUserWord("华玉米的产地来源 n");
			nativeBytes = CLibrary.Instance.NLPIR_ParagraphProcess(sInput, 1);
			System.out.println("增加用户词典后分词结果为： " + nativeBytes);
			
			CLibrary.Instance.NLPIR_DelUsrWord("要求美方加强对输");
			nativeBytes = CLibrary.Instance.NLPIR_ParagraphProcess(sInput, 1);
			System.out.println("删除用户词典后分词结果为： " + nativeBytes);
			
			
			int nCountKey = 0;
			String nativeByte = CLibrary.Instance.NLPIR_GetKeyWords(sInput, 10,false);

			System.out.print("关键词提取结果是：" + nativeByte);


			CLibrary.Instance.NLPIR_Exit();

		} catch (Exception ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		
		//1
		ICTCLAS2014.check();
		
		//2 adobe部分源代码被盗
		//System.out.println(ICTCLAS2014.segment("adobe部分源代码被盗"));

	}
}

