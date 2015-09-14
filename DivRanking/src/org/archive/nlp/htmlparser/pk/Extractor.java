package org.archive.nlp.htmlparser.pk;

/**
 * 将web extracting做成jar包时的接口文件，外部程序通过调用该接口类来获得主内容
 * */
public class Extractor {
	//public double tt, yy;
	public StringBuffer htmlToText(StringBuffer htmlFile){
		StringBuffer res=new StringBuffer();
		HTML2Tree h2t;
		HTree tree;
		double th,alp;
		String str;
		
		h2t=new HTML2Tree();
		h2t.main(htmlFile);
		tree=h2t.getTree();
		//tree.print();
		th=0.79;alp=15;
		ChooseBlock cb=new ChooseBlock();
		cb.initiate(tree,th);		
		
		str=cb.getContent();
		//tt = cb.err_val;
		/*
		int block[];
		block=cb.sizeBlock;
		System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<");
		for(int i=0; i<block.length; i++){
			System.out.println(block[i]);
		}
		System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<");
		*/
		//yy = tree.contentRatio();
		///locate(str,htmlFile);
		//if(cb.getWebAlpha()<=15)
			//str=null;
		//System.out.printf("alpha:%.2f err:%.2f\n",cb.getWebAlpha(),cb.getErr());
		if (str==null){
			return null;
		}
		return res.append(str);
	}
	private void locate(String str,StringBuffer file){
		String[] c_blocks=str.split("<cwirf/2008/qjt>");
        int[] res;
		for(int i=0,len=c_blocks.length ;i<len;i++){
        	if(c_blocks[i].equals("")) continue;
			res=position(c_blocks[i],file);
        	System.out.println(res[0]+","+res[1]);
        }
	}
	private int[] position(String str,StringBuffer file){
		String headstr,tailstr;
		int start,end,len;
		int[] ret=new int[2];
		if (str.length()>10){
			headstr=str.substring(0,10);
			tailstr=str.substring(str.length()-10);
			start=file.indexOf(headstr);
			end=file.indexOf(tailstr);
			len=end-start;
		}else{ 
			start=file.indexOf(str);
			len=str.length();
		}
		ret[0]=start;
		ret[1]=len+10;
		return ret;
	}
}
