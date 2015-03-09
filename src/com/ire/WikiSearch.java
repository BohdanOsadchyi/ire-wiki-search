package com.ire;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;

import com.ire.commons.AppGlobals;
import com.ire.commons.Utilities;
import com.ire.parse.TertiaryIndex;
import com.ire.rank.PostingList;

public class WikiSearch {

	public static void main(final String[] argv) throws Exception {
		//Main class that takes the file name as a command line argument.
		Class.forName("com.ire.commons.AppGlobals");
		Utilities.initializeStopWords();
		TertiaryIndex.load();
		BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
		while(true){
			String str=reader.readLine();
			if(str.equals("0")) break;
			long start=System.currentTimeMillis();
			com.ire.rank.TfidfQueryProcessorImproved.search(str.trim());
			System.out.println("total "+((System.currentTimeMillis()-start))+" ms");
		}
		reader.close();
		

		/*Class.forName("com.ire.DictionaryManager");
		Class.forName("com.ire.AppGlobals");
		final long start=System.currentTimeMillis();
		Utilities.initializeStopWords();
		final XMLParser parser = new XMLParser(argv[0]);
		parser.parse();
		DictionaryManager.closeAll();
		System.out.println("Index building done in "+((System.currentTimeMillis()-start)/1000)+" seconds.");*/
		/*
		Class.forName("com.ire.DictionaryManager");
		Class.forName("com.ire.AppGlobals");
		final long start=System.currentTimeMillis();
		SPIMI.fileIndex=481;
		SPIMI.mergeUncompressedBlocks();		
		System.out.println("merged in "+((System.currentTimeMillis()-start)/1000)+" seconds.");
		*/
		
		/*
		BufferedReader reader=new BufferedReader(new FileReader("copy/doc_info"));
		PrintWriter writerP=new PrintWriter(AppGlobals.DOC_INFO_FILE);
		PrintWriter writerS=new PrintWriter(AppGlobals.DOC_INFO_SINDEX);
		String str=reader.readLine();
		long fp=0;
		while(str!=null){
			String content=str.substring(0,str.lastIndexOf("$"))+"\n";
			System.out.println(content);
			writerP.print(content);
			
			int len=content.length();
			writerS.print(String.format("%010d",fp)+String.format("%06d",len));
			fp+=len;
			str=reader.readLine();
		}		
		writerP.close();
		writerS.close();
		reader.close();
		*/
		
		/*
		Class.forName("com.ire.AppGlobals");
		Utilities.initializeStopWords();
		TertiaryIndex.load();
		BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
		while(true){
			String str=reader.readLine();
			if(str.equals("0")) break;
			long start=System.currentTimeMillis();
			System.out.println(call(Integer.parseInt(str.trim())));
			System.out.println("total "+((System.currentTimeMillis()-start))+" ms");
		}
		reader.close();		
		*/
		
		/*
		//Validate dict list.		
		Class.forName("com.ire.commons.AppGlobals");
		Utilities.initializeStopWords();
		TertiaryIndex.load();
		BufferedReader reader=new BufferedReader(new FileReader(AppGlobals.DICTIONARY_FILE));
		String str=reader.readLine();
		while(str!=null){
			String term=str.substring(0,str.indexOf(','));
			System.out.println(term);
			if(call(term)==null){
				break;
			}
			str=reader.readLine();
		}
		reader.close();
		*/
		
		/*
		Class.forName("com.ire.commons.AppGlobals");
		Utilities.initializeStopWords();
		TertiaryIndex.load();
		System.out.println(call("T:sachin"));
		*/
	}
	
}