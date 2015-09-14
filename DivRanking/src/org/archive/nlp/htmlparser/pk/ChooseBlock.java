package org.archive.nlp.htmlparser.pk;


import java.util.Arrays;

/**
 * 挑选出主内容所在的块
 * */
public class ChooseBlock {
	private double threshold;            //确定web是否含有主内容的均方差阈值
    int[]   sizeBlock;                   //存储每个块的大小
    boolean[]  staBlock;                 //true表示该对应的block不用于方差计算
	String[]   contBlock;                //内容块
    double err_val;
    
	public ChooseBlock(){
    	sizeBlock =null;
    	staBlock  =null;
    	contBlock =null;
    }
	/**
	 * 初始化设置
	 * */
	public boolean initiate(HTree tree,double th){
		int len;
		this.threshold=th;
		contBlock=tree.getBlock();
		if (contBlock==null)
			return false;
		sizeBlock=getSizeBlock(contBlock);
		len=sizeBlock.length;
		if (len==0) return false;
		getMaxNoiseBlock(contBlock);           //把该噪声块削平
		//for(int ii=0;ii<len;ii++){System.out.println(sizeBlock[ii]);}
		staBlock=new boolean[len];             //辅助数组，在计算方差时，排除在计算外的块 在staBlock中设为true
		iniStaBlock();
		return true;
	}
	/**
	 * 返回web page的主内容<p>
	 * 如果返回null 表示不含主内容,调用此函数前必须先调用initiate函数
	 * */
	public String getContent(){
		int i,len,index,aidIndex,maxIndex=0;
		double val,tmp,max;
		int[] aid;
		double err;
		String str="";
		
		len=sizeBlock.length;
		aid=new int[len];
		val=calError();
		err_val=val;
		//System.out.println(val+"******************");
		
		//System.out.println("val:"+val);
		if (val<threshold) return null;
		i=0;
		max=0;
		while(true){
			index=getIndex2();
			aid[i]=index;
			setBlock(index);
			tmp=val;
			val=calError();
			err=tmp-val;
			if (err>max){
				max=err;
				maxIndex=i;
			}
			if(err>val) break;
			i++;
		}
		if (maxIndex>5) return null;   //我们认为 主内容块应该少于5个是合理的
		for(i=0;i<=maxIndex;i++){
			index=aid[i];
			str+=contBlock[index];
		}
		return str;
	}
	/**
	 * 返回块的大小的数组
	 * */
	public int[] getSizeBlock(String[] contBlock){
		int len;
		int[] sizeBlock;
		len=contBlock.length;
		sizeBlock=new int[len];
		for(int i=0;i<len;i++){
			sizeBlock[i]=contBlock[i].length();
		}
		//Arrays.sort(sizeBlock);   此处不能排序，因为内容块和size块是对应的，排序size块将是两者不能对应
		return sizeBlock;
	}
	/**
	 * 设置某个块的状态为真
	 * */
	private void setBlock(int index){
		staBlock[index]=true;
	}
	private void iniStaBlock(){
		int len;
		len=staBlock.length;
		for(int i=0;i<len;i++){
			staBlock[i]=false;
		}
	}
	/**
	 * 计算总块的方差
	 * */
	private double calError(){
		double avg,val,err;
		int sum=0,num=0,len;
		len=sizeBlock.length;
		if(len<=0) return 0;
		for(int i=0;i<len;i++){
			if (staBlock[i])
				continue;
			num++;
			sum+=sizeBlock[i];
		}
		avg=sum/(1.0*num);
		err=0;
		for(int i=0;i<len;i++){
			if (staBlock[i])
				continue;
			val=sizeBlock[i]-avg;
			val*=val;
			err+=val;
		}
		return Math.sqrt(err)/(1.0*num);
	}
	/**
	 * 发现目前最大块编号
	 * <p>sizeBlock数组已经由小到大排序
	 * */
	private int getIndex(){
		int index=0;
		int len;
		len=sizeBlock.length;
		for(int i=len-1;i>=0;i--){
			if (staBlock[i]) continue;
				return i;
		}
		return index;
	}
	/**
	 * 发现目前最大块编号
	 * */
	private int getIndex2(){
		int index=0,max;
		int len;
		max=0;
		len=sizeBlock.length;
		for(int i=0;i<len;i++){
			if (staBlock[i]) continue;
			if (sizeBlock[i]>max){
				max=sizeBlock[i];
				index=i;
			}
		}
		return index;
	}
	/**
	 * 获得网页的方差值,调用之前应该先调用了intiate函数
	 * */
	public double getWebError(){
		int len;
		if (contBlock==null) return 0;
		len=sizeBlock.length;
		return calError();
	}
	public double getErr(){
		return err_val;
	}
	/**
	 * 获得一个网页幂律分布的α值，调用之前应该先调用了initiate函数
	 * */
	public double getWebAlpha(){
		if (contBlock==null) return 0;
		return getAlpha2();
	}
	/**
	 * 找出块链表中，第个一块到其他块的斜率，将最大斜率作为幂律分布的alpha值返回
	 * */	
	private double getAlpha(){
		int size=sizeBlock.length;
		int[] sizeBlock2=new int[size];
		double val,max=0;
		double fir;
		System.arraycopy(sizeBlock,0,sizeBlock2,0,size);
		Arrays.sort(sizeBlock2);
		//for(int ii=0;ii<size;ii++){System.out.println(sizeBlock2[ii]);}
		fir=sizeBlock2[size-1];
		for(int i=size-2;i>=0;i--){
			if((fir)<max){          //循环终止条件,当最大块值
				break;
			}
			val=calAlpha(fir,sizeBlock2[i],size-i-1);
			if (val>max) max=val;
		}
		return max;
	}

	/**
	 * 找出块链表中，最大斜率
	 * */	
	private double getAlpha2(){
		double max=0,val;
		int size=sizeBlock.length;
		int[] sizeBlock2=new int[size];
		System.arraycopy(sizeBlock,0,sizeBlock2,0,size);
		Arrays.sort(sizeBlock2);
		//for(int ii=0;ii<size;ii++){System.out.println(sizeBlock2[ii]);}
		for(int i=size-1;i>=0;i--){
			if(sizeBlock2[i]<max)break;
			val=getOneAlpha(i,sizeBlock2);
			if (val>max) max=val;
		}
		return max;
	}
	/**
	 * 在排序后的块上，从第index个块开始找该块的最大斜率
	 * */
	private double getOneAlpha(int index,int[] sizeBlock2){
		double val,max=0;
		double fir;
		fir=sizeBlock2[index];
		for(int i=index-1;i>=0;i--){
			if(fir<max)break;          //循环终止条件,当最大块值
			val=calAlpha(fir,sizeBlock2[i],index-i);
			if (val>max) max=val;
		}
		return max;
	}
	private double calAlpha(double fir,double ith,int index){
		return (fir-ith)/index;
	}
	/**
	 * 将块分布两端（20%）中，最大和次大的两个块削平<p>
	 * 此时的内容块中，最初的块是网页中最后的块，削平最大和次大的块意在将每个网页中copyright
	 * 那一部分和其他较大的干扰部分，减少它们对主内容块提取干扰很大
	 * @return 返回下标
	 * */
	private void getMaxNoiseBlock(String[] content){
		int index=0,len,max=0,nextIndex=0;
		int size;
		size=content.length;
		for(int i=0;i<size;i++){
			len=content[i].length();
			if (len>max){
				nextIndex=index;index=i;max=len;
			}
		}
		if(judge(size,index))
			sizeBlock[index]    =10;
		if(judge(size,nextIndex))
			sizeBlock[nextIndex]=10;
	}
	/**
	 * 根据最大快所处在的整个文档块中的位置来判断是否删除最大块
	 * 即若不处在文档块分布的两端（20%），同时在距离两端6个块之内。则不是噪声块
	 * */
	private boolean judge(int size,int pos){
		if (pos<=(size*0.2)||pos>=(size*0.8))
			if(pos<2||pos>size-2)
				return true;
			else
				return false;
		else 
			return false;
	}
}




