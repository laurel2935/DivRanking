package org.archive.nlp.chunk.lpt.addon;

public class LTPRelationTag {
	
	//关系类型	Tag	Description	Example
	//主谓关系	
	public final static String SBV ="SBV"; //subject-verb	我送她一束花 (我 <-- 送)
	//动宾关系	
	public final static String VOB ="VOB"; //直接宾语，verb-object	我送她一束花 (送 --> 花)
	//间宾关系	
	public final static String IOB ="IOB"; //间接宾语，indirect-object	我送她一束花 (送 --> 她)
	//前置宾语	
	public final static String FOB ="FOB"; //前置宾语，fronting-object 他什么书都读 (书 <-- 读)
	//兼语	
	public final static String DBL ="DBL"; //double	他请我吃饭 (请 --> 我)
	//定中关系	
	public final static String ATT ="ATT"; //attribute	红苹果 (红 <-- 苹果)
	//状中结构	
	public final static String ADV ="ADV"; //adverbial	非常美丽 (非常 <-- 美丽)
	//动补结构	
	public final static String CMP ="CMP"; //complement	做完了作业 (做 --> 完)
	//并列关系	
	public final static String COO ="COO"; //coordinate	大山和大海 (大山 --> 大海)
	//介宾关系	
	public final static String POB ="POB"; //preposition-object	在贸易区内 (在 --> 内)
	//左附加关系	
	public final static String LAD ="LAD"; //left adjunct	大山和大海 (和 <-- 大海)
	//右附加关系	
	public final static String RAD ="RAD"; //right adjunct	孩子们 (孩子 --> 们)
	//独立结构	
	public final static String IS ="IS"; //independent structure	两个单句在结构上彼此独立
	//核心关系	
	public final static String HED ="HED"; //head	指整个句子的核心

}
