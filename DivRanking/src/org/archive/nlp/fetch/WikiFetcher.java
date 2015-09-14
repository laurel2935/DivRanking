package org.archive.nlp.fetch;

import java.util.ArrayList;
import java.util.List;

import org.archive.util.Language.Lang;

import info.bliki.api.Page;
import info.bliki.api.User;

public class WikiFetcher {
	
	private MyWikiModel _wikiModel;
	
	private MyHtmlConverter _converter;
	
	private User _user;
	
	private Lang _lang;
	
	private static final String enWiki = "http://en.wikipedia.org/w/api.php";
	private static final String chWiki = "http://zh.wikipedia.org/w/api.php";
	
	public WikiFetcher(Lang lang){
		_wikiModel = new MyWikiModel("${image}", "${title}");
		_converter = new MyHtmlConverter(true, true);
		_lang = lang;
		if(Lang.Chinese == lang){
			_user = new User("", "", chWiki);
		}else if(Lang.English == lang){
			_user = new User("", "", enWiki);
		}else{
			System.err.println("Unaccepted Lang Error!");
		}		
	}
	
	public String fetchWikiPage(String title){		
		_user.login();
		ArrayList<String> listOfTitleStrings = new ArrayList<String>();
		listOfTitleStrings.add(title);
		List<Page> listOfPages = _user.queryContent(listOfTitleStrings);
		System.out.println(listOfPages.size());
		if(listOfPages.size() > 0){
			Page page = listOfPages.get(0);
			String currentContent = page.getCurrentContent();
			String html = _wikiModel.render(_converter, currentContent);
		    //System.out.println(html);
			return html;
		}else{
			return null;
		}
	}
	
	public String fetchWikiDisambiguationPage(String title){		
		_user.login();
		ArrayList<String> listOfTitleStrings = new ArrayList<String>();
		
		if(Lang.Chinese == _lang){
			listOfTitleStrings.add(title+" (消歧義)");
		}else if(Lang.English == _lang){
			listOfTitleStrings.add(title+" (disambiguation)");
		}else{
			System.err.println("Unaccepted Lang Error!");
			return null;
		}
				
		List<Page> listOfPages = _user.queryContent(listOfTitleStrings);
		System.out.println(listOfPages.size());
		if(listOfPages.size() > 0){
			Page page = listOfPages.get(0);
			String currentContent = page.getCurrentContent();
			String html = _wikiModel.render(_converter, currentContent);
		    //System.out.println(html);
			return html;
		}else{
			return null;
		}
	}
	
	
	public static void test(){
		//1
		/*
		String[] listOfTitleStrings = { "apple" };
		User user = new User("", "", "http://en.wikipedia.org/w/api.php");
		//http://en.wikipedia.org/w/api.php
		user.login();
		List<Page> listOfPages = user.queryContent(listOfTitleStrings);
		System.out.println(listOfPages.size());
		for (Page page : listOfPages) {
		  WikiModel wikiModel = new WikiModel("${image}", "${title}");
		  String html = wikiModel.render(page.toString());
		  System.out.println(html);
		}
		*/
		//2
		/*
		String[] listOfTitleStrings = { "eclipse (disambiguation)" };
		User user = new User("", "", "http://en.wikipedia.org/w/api.php");
		//http://en.wikipedia.org/w/api.php
		user.login();
		List<Page> listOfPages = user.queryContent(listOfTitleStrings);
		System.out.println(listOfPages.size());
		for (Page page : listOfPages) {
			MyWikiModel wikiModel = new MyWikiModel("${image}", "${title}");
			String currentContent = page.getCurrentContent();
			String html = wikiModel.render(new MyHtmlConverter(true, true), currentContent);
		    System.out.println(html);
		}
		*/
		WikiFetcher wikiFetcher = new WikiFetcher(Lang.Chinese);
		String htmlString = wikiFetcher.fetchWikiPage("波斯貓");
		System.out.println(htmlString);
		
	}
	
	public static void main(String []args){
		//1
		WikiFetcher.test();
	}

}
