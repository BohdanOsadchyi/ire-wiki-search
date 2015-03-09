package com.ire.parse;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.ire.commons.AppGlobals;

public class DictionaryManager {	
	public static BufferedWriter postingsWriter=null;
	public static BufferedWriter dictionaryWriter=null; 
	public static BufferedWriter dictionarySecondaryIndexWriter=null;
	private static BufferedWriter docInfoWriter=null;
	private static BufferedWriter docInfoSecondaryIndexWriter=null;
	
	static{
		try{
			postingsWriter=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(AppGlobals.POSTINGS_FILE)));
			dictionaryWriter=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(AppGlobals.DICTIONARY_FILE)));
			dictionarySecondaryIndexWriter=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(AppGlobals.DICTIONARY_SINDEX)));
			docInfoWriter=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(AppGlobals.DOC_INFO_FILE)));
			docInfoSecondaryIndexWriter=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(AppGlobals.DOC_INFO_SINDEX)));
		}catch(IOException e){
			e.printStackTrace();
		}		
	}
	
	public static void closeAll() throws IOException{		
		close(postingsWriter);
		close(dictionaryWriter);
		close(dictionarySecondaryIndexWriter);
		close(docInfoWriter);
		close(docInfoSecondaryIndexWriter);
	}
	
	private static void close(final BufferedWriter bw) throws IOException{
		if(bw!=null) bw.close();
	}

	static long docInfoFP=0;
	public static void addDocInfo(String id, String title) throws IOException{
		if(docInfoWriter==null || docInfoSecondaryIndexWriter==null) return;
		final String content=id+"$"+title+"\n";
		docInfoWriter.append(content);	
		docInfoSecondaryIndexWriter.append(String.format("%010d",docInfoFP)+String.format("%06d",content.length()));
		docInfoFP+=content.length();		
	}
	
	
	
	/*	
	
	
	private static void close(final RandomAccessFile raf) throws IOException{
		if(raf!=null) raf.close();
	}
	
	private static RandomAccessFile termsStream=null; //Writes the term B:$d as 4B:$d
	private static RandomAccessFile postingsStream=null; //Writes the postings 125472:1,125473:3 as 125472:1,1:3
	private static RandomAccessFile dictionaryStream=null; //Writes the dictionary as term pointer, document frequency and dictionary pointer.
	
	public static String createMultiLevelPath(final String term){
		final StringBuffer builder=new StringBuffer("");
		int i=0;
		while(i<term.length() && i<AppGlobals.TERM_MAX_LEVEL){
			if(i!=1) //avoid ':'
			builder.append(term.charAt(i)+"/");
			i++;
		}
		return builder.toString();
	}
	
	public static String remTerm(final String term){
		final StringBuffer builder=new StringBuffer("");
		int i=0;
		while(i<term.length() && i<AppGlobals.TERM_MAX_LEVEL){
			i++;
		}
		while(i<term.length()){
			builder.append(term.charAt(i));
			i++;
		}
		return builder.toString();
	}
	
	public static void appendContent(final String term,final String fileName,final String content) throws IOException{
		//Append the content to the specified file type, with file directory sensed from term prefix.
		final String prefix=createMultiLevelPath(term);
		if(termWriterPool.get(prefix)==null)
			termWriterPool.put(prefix, new HashMap<String,BufferedWriter>());
		
		final String folderPath=AppGlobals.INDEX_ROOT_FOLDER+"term/"+prefix;
		if(termWriterPool.get(prefix).get(fileName)==null) {
			final File file=new File(folderPath);
			if(!file.exists()) file.mkdirs();
			termWriterPool.get(prefix).put(fileName,new BufferedWriter(new OutputStreamWriter(new FileOutputStream(folderPath+fileName))));
			System.out.println("creating "+folderPath+fileName);
		}
		
		termWriterPool.get(prefix).get(fileName).append(content);
		
		//Close used streams
		if(prevChar!='#' && (term.charAt(0)!=prevField || term.charAt(2)!=prevChar)) {
			final Iterator<Map.Entry<String,HashMap<String,BufferedWriter>>> iter = termWriterPool.entrySet().iterator();
			while (iter.hasNext()) {
			    Map.Entry<String,HashMap<String,BufferedWriter>> entry = iter.next();
			    for(final Entry<String,BufferedWriter> ientry:entry.getValue().entrySet())
					close(ientry.getValue());
			    iter.remove();
			}
		}
		prevField=term.charAt(0);
		prevChar=term.charAt(2);
	}
	
	static char prevChar='#',prevField='#';
	public static void addDocInfo(final String id,final String title,final int length) throws IOException{
		//Add to doc info file
		if(docInfoWriter==null) return;
		
		final String prefix=id.substring(0,2);
		final String folderPath=AppGlobals.INDEX_ROOT_FOLDER+"doc/"+prefix.charAt(0)+"/"+prefix.charAt(1)+"/";
		if(docInfoWriterPool.get(prefix)==null) {
			final File file=new File(folderPath);
			if(!file.exists()) file.mkdirs();
			docInfoWriterPool.put(prefix,new BufferedWriter(new OutputStreamWriter(new FileOutputStream(folderPath+"sIndex"))));
		}
		
		final String content=id+"$"+title+"$"+length+"\n";
		docInfoWriter.append(content);	
		//docInfoSIndexWriter.append(String.format("%010d",docInfoFP)+String.format("%06d",content.length()));
		//docInfoWriterPool.get(prefix).append(String.format("%010d",docInfoFP)+String.format("%06d",content.length()));
		//docInfoFP+=content.length();
		
	}
	static long docInfoFP=0;
	
	public static void createSecondaryIndexes(final String src,final String dest) throws IOException{
		//Creates secondary indexes for retrieving files of given type faster.
		final long curTime=System.currentTimeMillis();
		System.out.println("Secondary index creation process started.");		
		
		for(final Entry<String,HashMap<String,BufferedWriter>> entry:termWriterPool.entrySet()){
			final String folderPath=AppGlobals.INDEX_ROOT_FOLDER+entry.getKey().charAt(0)+"/"+entry.getKey().charAt(2)+"/";
			final File readerFile=new File(folderPath+src);
			final long ptrDigitsReq=(""+readerFile.length()).length();
			final BufferedReader reader=new BufferedReader(new FileReader(readerFile));
			final BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(folderPath+dest)));
			String line=reader.readLine();
			long pos=0;
			while(line!=null){
				writer.write(String.format("%0"+ptrDigitsReq+"d",pos)+String.format("%06d",line.length()+1));
				pos+=line.length()+1;
				line=reader.readLine();
			}
			writer.close();
			reader.close();
		}	
		
		System.out.println("Secondary index created in "+((System.currentTimeMillis()-curTime)/1000)+" seconds.");
	}
	
	public static String gapEncodeUncompressedPostingList(final String postingList) throws UnsupportedEncodingException{
		final StringBuilder postings=new StringBuilder();
		final String[] postingContent=postingList.split(",");
		
		//Get the starting doc id.
		int start=Integer.parseInt(postingContent[0].split(":")[0]);
		postings.append(postingContent[0]);
		
		postingLength=1;
		for(int pIndex=1;pIndex<postingContent.length;pIndex++){
			final String[] content=postingContent[pIndex].split(":");
			postings.append(","+(Integer.parseInt(content[0])-start)+":"+content[1]);
			++postingLength;
		}
		postingSize=postings.toString().getBytes("UTF-8").length;
		return postings.toString();
	}	
	 
	private static int postingLength=0;
	private static int postingSize=-1;
	public static String gapEncodeCompressedPostingList(final String postingList) {
		//Encode the posting list with gap from the starting document id.
		if(postingList==null || postingList.length()==0) return null;
		final StringBuffer res=new StringBuffer();
		int cur=0;
		postingLength=1;
		
		//Get the start pair.		
		char ch=postingList.charAt(cur++);
		res.append(ch);
		while((ch>>7 & 1)!=1){
			try{
				ch=postingList.charAt(cur++);
				res.append(ch);
			}catch(StringIndexOutOfBoundsException e){
				System.out.println(postingList);
				e.printStackTrace();
			}			
		}
		int start=VBEncoding.decode(res.toString());
		System.out.println("start-"+start);
		//feed the freq.
		ch=postingList.charAt(cur++);
		res.append(ch);
		while((ch>>7 & 1)!=1){
			ch=postingList.charAt(cur++);
			res.append(ch);
		}		
		
		//Process remaining items if any.
		final StringBuffer buff=new StringBuffer();
		while(cur<postingList.length()){
			//Get the doc id.
			ch=postingList.charAt(cur++);
			buff.append(ch);
			while((ch>>7 & 1)!=1){
				ch=postingList.charAt(cur++);
				buff.append(ch);
			}
			//Save the gap.
			res.append(VBEncoding.encode(VBEncoding.decode(buff.toString())-start));
			System.out.println("Gap-"+(VBEncoding.decode(buff.toString())-start));
			buff.setLength(0);
			
			//Get the doc freq.
			ch=postingList.charAt(cur++);
			res.append(ch);
			while((ch>>7 & 1)!=1){
				ch=postingList.charAt(cur++);
				res.append(ch);
			}
			++postingLength;			
		}
		
		postingSize=res.toString().getBytes().length;
		return res.toString();
	}	
	
	/*
	public static String getPostingList(final String termValue)
			throws IOException {
		//Get the posting list for the specified term value.
		long start=0;
		long end=dictionaryStream.length()-1;
		while(start<end){
			long mid=start+((end-start)/2);
			String midValue=getTermId(mid);
			if(termValue.equals(midValue)){
				//Found the row in the dictionary.
				dictionaryStream.seek(mid+12);
				long postingPtr=dictionaryStream.readLong();				
				return getPostingList(postingPtr);
			} else if(termValue.compareTo(midValue)<0){
				end=mid-20;
			} else {
				start=mid+20;
			}
		}
		
		return null;
	}
	
	public static String getTermId(final long pos) throws IOException{
		//Extracts the term value from the terms.txt
		termsStream.seek(pos); //Sets the pointer to the given position.
		
		//Find the length of the term.
		int len=0;
		byte curByte=termsStream.readByte();
		do{
			len=(len*10)+(curByte-'0');
			curByte=termsStream.readByte();
		}while('0'<=curByte && curByte<='9');
		
		//Retrieve the characters till len.
		final StringBuffer termValue=new StringBuffer();
		termValue.append((char)curByte);
		for(int i=0;i<len-1;i++){
			curByte=termsStream.readByte();
			termValue.append((char)curByte);
		}
		
		return termValue.toString();
	}
	
	public static String getPostingList(final long pos) throws IOException{
		//Extracts the posting list form the postings.txt
		postingsStream.seek(pos);
		
		//Find the starting docId.
		byte curByte=postingsStream.readByte();		
		int startDocId=0;
		while('0'<=curByte && curByte<='9'){
			startDocId=(startDocId*10)+(curByte-'0');
			curByte=postingsStream.readByte();
		}
		
		//Find the corresponding docfreq.
		int startDocFreq=0;
		curByte=postingsStream.readByte();
		while('0'<=curByte && curByte<='9'){
			startDocFreq=(startDocFreq*10)+(curByte-'0');
			curByte=postingsStream.readByte();
		}
		
		//Find the remaining posting list, if any.
		final StringBuffer postingContent=new StringBuffer(startDocId+":"+startDocFreq);
		if(curByte!='\n'){
			curByte=postingsStream.readByte();
			boolean isDocIdOn=true;
			int gap=0,freq=0;
			while(curByte!='\n'){
				if('0'<=curByte && curByte<='9'){
					if(isDocIdOn) gap=gap*10+(curByte-'0');
					else freq=freq*10+(curByte-'0');
				} else if(curByte==':'){
					isDocIdOn=false;
				} else if(curByte==','){
					postingContent.append(","+(startDocId+gap)+":"+freq);
					isDocIdOn=true;
					freq=0;
					gap=0;
				}	
				curByte=postingsStream.readByte();
			}
			if(freq!=0){
				postingContent.append(","+(startDocId+gap)+":"+freq);
			}			
		}
				
		return postingContent.toString();
	}
	*/

}
