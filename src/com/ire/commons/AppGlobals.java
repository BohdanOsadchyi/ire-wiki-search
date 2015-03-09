package com.ire.commons;

import java.util.HashMap;

public class AppGlobals {
	public final static boolean IS_DEBUG=false;
	public final static boolean DELETE_TEMP_FILE=false;
	public final static int SPIMI_TOKEN_STREAM_MAX_LIMIT = 3000000;
	public final static int SPIMI_READER_POOL_MAX_LINES = 1;
	public final static int WINDOW_SIZE=10;
	public final static int NO_OF_DOCUMENTS=14040877;
	public final static int DOC_INFO_TERTIARY_BLOCK_SIZE=5;
	public final static int DICT_TERTIARY_BLOCK_SIZE=5;
	public final static HashMap<Character,Double> fields=new HashMap<Character,Double>();
	static {
		fields.put('T',8.0);
		//fields.put('B',4.0);
		//fields.put('C',25.0);
		//fields.put('I',25.0);
		//fields.put('L',20.0);
		//fields.put('R',20.0);
	}
	
	//Set of files used by the search engine
	public final static String STOP_WORDS_LIST="stopWords.txt";
	public final static String OUTPUT_SUB_FILE_PREFIX = "temp/block-";
	public final static String POSTINGS_FILE="index/postings";
	public final static String DICTIONARY_FILE="index/dictionary";
	public final static String DICTIONARY_SINDEX="index/dictionary_sindex";
	public final static String DOC_INFO_FILE="index/doc_info";
	public final static String DOC_INFO_SINDEX="index/doc_info_sindex";
}
