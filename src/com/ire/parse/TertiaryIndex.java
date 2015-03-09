package com.ire.parse;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeMap;

import com.ire.commons.AppGlobals;

public class TertiaryIndex {
	
	private static TreeMap<Integer,Integer> docInfoTIndex=null;
	private static TreeMap<String,Integer> dictTIndex=null;
	static{
		docInfoTIndex=new TreeMap<Integer,Integer>();
		dictTIndex=new TreeMap<String,Integer>();
	}
	
	public static void load() throws IOException{
		loadDocInfoTertiaryIndex();
		loadDictTertiaryIndex();
	}
	
	private static void loadDocInfoTertiaryIndex() throws IOException{
		//Loads the tertiary index for the document info.
		final long start=System.currentTimeMillis();
		final BufferedReader reader=new BufferedReader(new FileReader(AppGlobals.DOC_INFO_FILE));
		String lineStr=reader.readLine();
		int curLine=0;
		int curDocId=-1;
		while(lineStr!=null){
			if(curLine%AppGlobals.DOC_INFO_TERTIARY_BLOCK_SIZE==0) {
				curDocId=Integer.parseInt(lineStr.substring(0,lineStr.indexOf("$")));
				docInfoTIndex.put(curDocId, curLine);
			}
			++curLine;
			lineStr=reader.readLine();
		}
		reader.close();	
		System.out.println(docInfoTIndex.size());
		System.out.println("Tertiary index for DocInfo loaded in "+((System.currentTimeMillis()-start)/1000)+" s");
	}
	
	public static Integer getDocPtr(final Integer docId){
		return docInfoTIndex.floorEntry(docId).getValue();
	}
	
	private static void loadDictTertiaryIndex() throws IOException{
		//Loads the tertiary index for the document info.
		final long start=System.currentTimeMillis();
		final BufferedReader reader=new BufferedReader(new FileReader(AppGlobals.DICTIONARY_FILE));
		String lineStr=reader.readLine();
		int curLine=0;
		String curTerm=null;
		while(lineStr!=null){
			if(curLine%AppGlobals.DOC_INFO_TERTIARY_BLOCK_SIZE==0) {
				curTerm=lineStr.substring(0,lineStr.indexOf(","));
				dictTIndex.put(curTerm, curLine);
			}
			++curLine;
			lineStr=reader.readLine();
		}
		reader.close();	
		System.out.println(dictTIndex.size());
		System.out.println("Tertiary index for dictionary loaded in "+((System.currentTimeMillis()-start)/1000)+" s");
	}
	
	public static Integer getDictPtr(final String termValue){
		return dictTIndex.floorEntry(termValue).getValue();
	}

}
