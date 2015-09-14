package org.archive.nlp.htmlparser.pk;

import java.io.File;

/**
 * 过滤html网页，对文件名、大小不满足预定条件的网页不予考虑，如过小等；
 * @author T
 *
 */
public class StandarlizeHtml {
	/**统计计数*/
	private static int okNumber = 0;	
	
	/**
	 * 对目录下的中文html的初步过滤
	 * @param file
	 */
	public  static void checkHtmlsCh(File file){
		if(null == file){
			System.out.println("null file");
			return;
		}
		if(file.isDirectory()){
			File [] subFiles = file.listFiles();
			for(int i=0; i<subFiles.length; i++){
				checkHtmlsCh(subFiles[i]);
			}
		}else{
			if(file.isFile()){
				if(checkHtmlCh(file)){
					okNumber ++;
				}
			}			
		}
	}
	/**
	 * 对单个英文html的初步过滤
	 * @param file
	 * @return
	 */
	public static boolean checkHtmlEn(File file){
		if(checkSizeCh(file) && checkFileNameCh(file)){
			return true;
		}else{
			return false;
		}
	}
	/**
	 * 对单个中文html的初步过滤
	 * @param file
	 * @return
	 */
	public static boolean checkHtmlCh(File file){
		if(checkSizeCh(file) && checkFileNameCh(file)){
			return true;
		}else{
			return false;
		}
	}
	/**
	 * 对中文html大小有个初步限制，后面再对提取的文本大小增加限制
	 * @param file
	 * @return
	 */
	public static boolean checkSizeCh(File file){		
		if(((double)file.length()/1000) > 10){
			return true;
		}else{
			return false;
		}
	}
	/**
	 * 根据中文html文件名过滤，需要含有content但不考虑content_4557735_8.htm
	 * @param file
	 * @return
	 */
	public static boolean checkFileNameCh(File file){
		boolean single = false;
		boolean contentStyle = false;
		String name = file.getName();
		if(name.indexOf("content") != -1){
			contentStyle = true;
		}
		if(name.indexOf("_") == name.lastIndexOf("_")){
			single = true;
		}
		if(contentStyle && single){
			return true;
		}else{
			return false;
		}
	}
	
	//test
	public void filterHtml(){
		try{
			File file = new File("D:/content_10656445_1.htm");
			//FileInputStream fis = new FileInputStream(file);
			System.out.println(file.length());
			//System.out.println((double)fis.available()/1000);
			if(checkSizeCh(file)){
				System.out.println("size ok");
			}else{
				System.out.println("size not ok");
			}
			if(checkFileNameCh(file)){
				System.out.println("filename ok");
			}else{
				System.out.println("filename not ok");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	//>>>>>>> 1.2
	
	public static void main(String args[]){
		StandarlizeHtml standard = new StandarlizeHtml();
		//standard.filterHtml();
		String directory = "";
		standard.checkHtmlsCh(new File(directory));
		System.out.println("ok number:		" + okNumber);
	}

}
