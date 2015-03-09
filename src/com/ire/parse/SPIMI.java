package com.ire.parse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.ire.commons.AppGlobals;
import com.ire.commons.Utilities;

public class SPIMI {
	/*
	 * Single pass in-memory indexing algorithm.
	 */
	static int fileIndex = 0;

	public static void createInvertedIndex(final List<String> tokenStream)
			throws IOException {
		// Creates one block, stores it into the disk.
		long curTime=System.currentTimeMillis();
		final TreeMap<String, ArrayList<String>> dictionary = new TreeMap<String, ArrayList<String>>();
		// Fill the dictionary.
		for (final String tokenPair : tokenStream) {
			final String[] content = tokenPair.split("#");
			if (dictionary.get(content[0]) == null) {
				// New term.
				dictionary.put(content[0], new ArrayList<String>());
			}
			dictionary.get(content[0]).add(content[1]);
		}
		System.out.println("Sorted in "+((System.currentTimeMillis()-curTime)/1000)+" seconds.");

		// Write the block to disk.		
		curTime=System.currentTimeMillis();
		final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
				AppGlobals.OUTPUT_SUB_FILE_PREFIX + (fileIndex++) + ".txt")));
		for (final Entry<String, ArrayList<String>> entry : dictionary
				.entrySet()) {
			writer.append(entry.getKey() + "=");
			for (int i = 0; i < entry.getValue().size()-1; i++)
				writer.append(entry.getValue().get(i)+",");
			writer.append(entry.getValue().get(entry.getValue().size()-1));
			writer.append("\n");
		}
		writer.close();		
		System.out.println("Block created in "+((System.currentTimeMillis()-curTime)/1000)+" seconds.");
	}
	

	final static List<RandomAccessFile> readerList = new ArrayList<RandomAccessFile>();
	final static List<List<String>> readerWordList = new ArrayList<List<String>>();
	final static List<Integer> curPointer = new ArrayList<Integer>();

	public static void mergeCompressedBlocks() throws IOException { 
		// Merge the previously created inverted index blocks.
		if (fileIndex == 0) // No blocks are previously written.
			return;
		final long curTime=System.currentTimeMillis();
		System.out.println("Merge started.");
		
		// Fill the readers
		for (int i = 0; i < fileIndex; i++) {
			readerList.add(new RandomAccessFile(
					AppGlobals.OUTPUT_SUB_FILE_PREFIX + i + ".txt","rw"));
			// Fill the initial set of words.
			readerWordList.add(new ArrayList<String>());
			if (readerList.get(i).length()==0 || (readerList.get(i).getFilePointer()==readerList.get(i).length()-1))
				curPointer.add(-1);
			else
				curPointer.add(0);
			int count = AppGlobals.SPIMI_READER_POOL_MAX_LINES;
			while (count > 0) {
				final String line = readBlock(readerList.get(i));
				if (line == null) {
					readerList.get(i).close();
					readerList.set(i, null);
					if (AppGlobals.DELETE_TEMP_FILE)
						(new File(AppGlobals.OUTPUT_SUB_FILE_PREFIX + i
								+ ".txt")).delete();
					break;
				}
				readerWordList.get(i).add(line);
				count--;
			}
		}

		for (;;) {
			// Get the least precedent string.
			String leastPrecStr = null;
			for (int i = 0; i < fileIndex; i++) {
				if (curPointer.get(i) != -1) {
					final String curWord = readerWordList.get(i)
							.get(curPointer.get(i)).split("=")[0];
					if (leastPrecStr == null
							|| curWord.compareTo(leastPrecStr) < 0) {
						leastPrecStr = curWord;
					}
				}
			}
			if (leastPrecStr == null)
				break;

			// Initialize the dictionary parameters.
			final String termValue = leastPrecStr;
			final StringBuffer postingList = new StringBuffer();

			for (int i = 0; i < fileIndex; i++) {
				if (curPointer.get(i) != -1) {
					final String[] curWordContent = readerWordList.get(i)
							.get(curPointer.get(i)).split("=");
					if (curWordContent[0].equals(leastPrecStr)) {
						postingList.append(curWordContent[1]);

						int curLoc = curPointer.get(i);
						int curSize = readerWordList.get(i).size();
						if (curLoc + 1 < curSize) {
							curPointer.set(i, curLoc + 1);
						} else {
							// Buffer empty, fetch few more lines.
							if (readerList.get(i) != null) {
								String line = readBlock(readerList.get(i));
								if (line == null) {
									curPointer.set(i, -1);
								} else {
									curPointer.set(i, 0);
									int count = AppGlobals.SPIMI_READER_POOL_MAX_LINES;
									readerWordList.set(i,
											new ArrayList<String>());
									readerWordList.get(i).add(line);
									while (count > 0) {
										line =  readBlock(readerList.get(i));
										if (line == null) {
											readerList.get(i).close();
											readerList.set(i, null);
											if (AppGlobals.DELETE_TEMP_FILE)
												(new File(
														AppGlobals.OUTPUT_SUB_FILE_PREFIX
																+ i + ".txt"))
														.delete();
											break;
										}
										readerWordList.get(i).add(line);
										count--;
									}
								}
							} else {
								curPointer.set(i, -1);
							}
						}
					}
				}
			}

			// Persist in the dictionary.
			//DictionaryManager.addToDictionary(termValue, postingList.toString());
		}

		// Close the readers
		for (int i = 0; i < fileIndex; i++)
			if (readerList.get(i) != null) {
				readerList.get(i).close();
				if (AppGlobals.DELETE_TEMP_FILE)
					(new File(AppGlobals.OUTPUT_SUB_FILE_PREFIX + i + ".txt"))
							.delete();
			}
		
		System.out.println("Merging done in "+((System.currentTimeMillis()-curTime)/1000)+" seconds.");
	}
	
	public static String readBlock(final RandomAccessFile file) throws IOException{
		//Returns the current line
		if (file.length()==0 || (file.getFilePointer()==file.length())) return null;
		
		//Read the term
		final StringBuffer buff=new StringBuffer();
		byte curChar=file.readByte();
		do{
			buff.append((char)curChar);
			curChar=file.readByte();			
		}while(curChar!='=');
		buff.append("=");
		
		//Read the posting
		curChar=file.readByte();
		do{
			//read the docId.
			buff.append((char)curChar);
			while((curChar>>7 & 1)!=1){
				curChar=file.readByte();
				buff.append((char)curChar);
			}
			
			//read the frequency.
			curChar=file.readByte();
			buff.append((char)curChar);
			while((curChar>>7 & 1)!=1){
				curChar=file.readByte();
				buff.append((char)curChar);
			}
			
			curChar=file.readByte();
		}while(curChar!='#');		
		
		return buff.toString();
	}

 	public static void mergeUncompressedBlocks() throws IOException {
		// Merge the previously created inverted index blocks.
		if (fileIndex == 0) // No blocks are previously written.
			return;
		
		final long curTime=System.currentTimeMillis();
		System.out.println("Merge started.");
		
		final List<BufferedReader> readerList = new ArrayList<BufferedReader>();
		final List<List<String>> readerWordList = new ArrayList<List<String>>();
		final List<Integer> curPointer = new ArrayList<Integer>();

		// Fill the readers
		for (int i = 0; i < fileIndex; i++) {
			readerList.add(new BufferedReader(new FileReader(
					AppGlobals.OUTPUT_SUB_FILE_PREFIX + i + ".txt")));
			// Fill the initial set of words.
			readerWordList.add(new ArrayList<String>());
			String line = readerList.get(i).readLine();
			if (line == null)
				curPointer.add(-1);
			else
				curPointer.add(0);
			int count = AppGlobals.SPIMI_READER_POOL_MAX_LINES - 1;
			readerWordList.get(i).add(line);
			while (count > 0) {
				line = readerList.get(i).readLine();
				if (line == null) {
					readerList.get(i).close();
					readerList.set(i, null);
					if (AppGlobals.DELETE_TEMP_FILE)
						(new File(AppGlobals.OUTPUT_SUB_FILE_PREFIX + i
								+ ".txt")).delete();
					break;
				}
				readerWordList.get(i).add(line);
				count--;
			}
		}

		long docFreq=-1,postingSize=-1;
		for (;;) {
			// Get the least precedent string.
			String leastPrecStr = null;
			for (int i = 0; i < fileIndex; i++) {
				if (curPointer.get(i) != -1) {
					final String curWord = readerWordList.get(i)
							.get(curPointer.get(i)).split("=")[0];
					if (leastPrecStr == null
							|| curWord.compareTo(leastPrecStr) < 0) {
						leastPrecStr = curWord;
					}
				}
			}
			if (leastPrecStr == null)
				break;
			
			// Initialize the dictionary parameters.
			final String termValue = leastPrecStr;
			docFreq=0;
			postingSize=0;
			final boolean isValidToken=Utilities.isValidToken(leastPrecStr);
			if(isValidToken) System.out.println(leastPrecStr);
			
			for (int i = 0; i < fileIndex; i++) {
				if (curPointer.get(i) != -1) {
					final String[] curWordContent = readerWordList.get(i)
							.get(curPointer.get(i)).split("=");
					if (curWordContent[0].equals(leastPrecStr)) {
						if(isValidToken) {
							final String content=curWordContent[1]+",";
							DictionaryManager.postingsWriter.append(content);
							postingSize+=content.length();
							docFreq+=curWordContent[1].split(",").length;
						}
						/*
						final StringBuilder gapEncodedPostings=new StringBuilder();
						for(final String docPair:curWordContent[1].split(",")) {
							final String[] content=docPair.split(":");
							int curId=Integer.parseInt(content[0]);
							if(startDoc==-1) {
								startDoc=curId;
								gapEncodedPostings.append(curId+":"+content[1]+",");
							} else {
								//Save the gap only instead of full docid
								gapEncodedPostings.append((curId-startDoc)+":"+content[1]+",");
							}
							docFreq++;
						}					
						DictionaryManager.postingsWriter.append(gapEncodedPostings);
						postingSize+=gapEncodedPostings.length();
						*/
						
						//docFreq+=curWordContent[1].split(",").length;
						//postingSize+=curWordContent[1].length()+1;
						//DictionaryManager.appendContent(termValue,"postings",curWordContent[1] + ",");

						int curLoc = curPointer.get(i);
						int curSize = readerWordList.get(i).size();
						if (curLoc + 1 < curSize) {
							curPointer.set(i, curLoc + 1);
						} else {
							// Buffer empty, fetch few more lines.
							if (readerList.get(i) != null) {
								String line = readerList.get(i).readLine();
								if (line == null) {
									curPointer.set(i, -1);
								} else {
									curPointer.set(i, 0);
									int count = AppGlobals.SPIMI_READER_POOL_MAX_LINES;
									readerWordList.set(i,
											new ArrayList<String>());
									readerWordList.get(i).add(line);
									while (count > 0) {
										line = readerList.get(i).readLine();
										if (line == null) {
											readerList.get(i).close();
											readerList.set(i, null);
											if (AppGlobals.DELETE_TEMP_FILE)
												(new File(
														AppGlobals.OUTPUT_SUB_FILE_PREFIX
																+ i + ".txt"))
														.delete();
											break;
										}
										readerWordList.get(i).add(line);
										count--;
									}
								}
							} else {
								curPointer.set(i, -1);
							}
						}
					}
				}
			}
			
			if(isValidToken) {
				DictionaryManager.postingsWriter.append("\n");
				postingSize++;
				
				//Add to dictionary
				final String content=termValue+","+postingFP+":"+postingSize+","+docFreq+"\n";
				DictionaryManager.dictionaryWriter.append(content);
				
				//Update the posting fp.
				postingFP+=postingSize;
				
				//Add to secondary index
				final String sindex=String.format("%010d",dictionaryFP)+String.format("%06d",content.length());
				DictionaryManager.dictionarySecondaryIndexWriter.append(sindex);
				
				//Update the dictionary fp.
				dictionaryFP+=content.length();
			}
			
			/*
			DictionaryManager.appendContent(termValue, "postings","\n");
			postingSize++;
					
			//Add to dictionary
			//Get the posting byte index.
			final String key=DictionaryManager.createMultiLevelPath(termValue);
			if(postingFPMap.get(key)==null) postingFPMap.put(key,(long)0);
			final String content=DictionaryManager.remTerm(termValue)+","+postingFPMap.get(key)+":"+postingSize+","+docFreq+"\n";
			DictionaryManager.appendContent(termValue, "dictionary",content);			
			postingFPMap.put(key,postingFPMap.get(key)+postingSize);
			
			//Add to secondary index
			if(dictFPMap.get(key)==null) dictFPMap.put(key,(long)0);
			DictionaryManager.appendContent(termValue,"sIndex",String.format("%010d",dictFPMap.get(key))+String.format("%06d",content.length()));
			dictFPMap.put(key,dictFPMap.get(key)+content.length());
			
			if(prevChar!='#' && (termValue.charAt(0)!=prevField || termValue.charAt(2)!=prevChar)){
				postingFPMap.clear();
				dictFPMap.clear();
			}
			prevField=termValue.charAt(0);
			prevChar=termValue.charAt(2);
			*/
		}

		// Close the readers
		for (int i = 0; i < fileIndex; i++)
			if (readerList.get(i) != null) {
				readerList.get(i).close();
				if (AppGlobals.DELETE_TEMP_FILE)
					(new File(AppGlobals.OUTPUT_SUB_FILE_PREFIX + i + ".txt"))
							.delete();
			}
		
		System.out.println("Merging done in "+((System.currentTimeMillis()-curTime)/1000)+" seconds.");
	}

	static long postingFP=0,dictionaryFP=0;
 	
 	/*
 	static char prevChar='#',prevField='#'; 	
 	final static HashMap<String,Long> postingFPMap=new HashMap<String,Long>();
 	final static HashMap<String,Long> dictFPMap=new HashMap<String,Long>();
 	*/
}