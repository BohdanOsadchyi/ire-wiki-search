package com.ire.rank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import com.google.common.collect.MinMaxPriorityQueue;
import com.ire.commons.AppGlobals;
import com.ire.commons.Utilities;

public class TfidfQueryProcessorImproved {

	public static void search(final String input) throws InterruptedException,
			ExecutionException {
		// Initiate search for the given user input.
		
		
		/*
		 * Step 1 Tokenize, case fold, stop the user query.
		 */
		
		// Parse the input
		final List<String> tokenList=new ArrayList<String>();
		final List<Integer> orList=new ArrayList<Integer>();
		final List<FutureTask<PostingList>> postingTaskList = new ArrayList<FutureTask<PostingList>>();
		boolean isTagLessFieldPresent=false;
		for (String token : input.split("\\s+")) {
			if(tokenList.indexOf(token.toLowerCase())==-1) {
				tokenList.add(token.toLowerCase());
			} else continue;
			
			// Case folding
			char field;
			if (token.indexOf(":") == -1) {
				// if field is not present, assume none.
				field = '*';
				token=token.toLowerCase();
				isTagLessFieldPresent=true;
			} else {
				field = token.charAt(0);
				token=token.substring(2).toLowerCase();
			}
			
			//Check for number
			if(Utilities.isNumber(token)) {
				continue;
				//token="$d";
			} else {	
				// Stop word check
				if (!Utilities.isNonStopWord(token))
					continue;
	
				// Stem the token
				token = Utilities.stemWord(token);
			}

			if(field!='*') {
				if(AppGlobals.IS_DEBUG) System.out.println("Added "+field+":"+token);
				postingTaskList.add(new FutureTask<PostingList>(new PostingFetcher(field
					+ ":" + token)));
				orList.add(-1);
			} else {
				//Generate all possible fields for the string.
				int curPtr=postingTaskList.size();
				for(final Entry<Character,Double> entry:AppGlobals.fields.entrySet()){
					if(AppGlobals.IS_DEBUG) System.out.println("Added "+entry.getKey()+":"+token);
					postingTaskList.add(new FutureTask<PostingList>(new PostingFetcher(entry.getKey()
							+ ":" + token)));
					orList.add(curPtr);
				}
			}
		}
		if(AppGlobals.IS_DEBUG) System.out.println("Added "+postingTaskList.size()+" postings.");		
		if(postingTaskList.size()==0) return;
		
		
		
		/*
		 * Step 2 Retrieve the postings.
		 */
		
		// Start the posting retrieval tasks.
		long curTime=System.currentTimeMillis();
		ExecutorService executor = Executors.newFixedThreadPool(postingTaskList
				.size());
		for (final FutureTask<PostingList> task : postingTaskList)
			executor.execute(task);

		// Collect the postings.
		boolean isDone = false;
		List<PostingList> postingList=new ArrayList<PostingList>();
		while (!isDone) {
			// Check if all postings are fetched.
			if (Utilities.checkAllTasksComplete(postingTaskList)) {
				executor.shutdown();
				for (final FutureTask<PostingList> task : postingTaskList) {
					PostingList posting = task.get();
					postingList.add(posting);
					if(AppGlobals.IS_DEBUG) System.out.println("Fetched "+(posting==null?-1:(posting.term.split(",").length))+" docs. for term "+(posting==null?-1:posting.term));
				}
				isDone = true;
			}
		}		
		if(AppGlobals.IS_DEBUG) System.out.println("Posting retrieved in "+(System.currentTimeMillis()-curTime)+" ms");
		
		
		/*
		 * Step 3 - Find the common doc ids by intersection and union.
		 */
		
		//Modify the postings.		
		curTime=System.currentTimeMillis();
		if(isTagLessFieldPresent) postingList=handleOrQueries(postingList,orList);		
		// Process the postings.
		HashMap<String,ArrayList<Posting>> intersectMap = null;
		Iterator<PostingList> postIterator=postingList.iterator();
		while(postIterator.hasNext()) {
			final PostingList curPost=(PostingList) postIterator.next();
			if(curPost!=null) {
				if (intersectMap == null) {
					intersectMap = convertTextToPostMap(curPost.text.toString());
				} else {
					intersectMap = intersectPostings(intersectMap,curPost.text.toString());
				}
			}
		}
		if(AppGlobals.IS_DEBUG) System.out.println("Intersection completed in "+(System.currentTimeMillis()-curTime)+" ms");
		if(intersectMap==null) {
			System.out.println("Intersection result is empty.");
			return;
		}		
		if(AppGlobals.IS_DEBUG) System.out.println("After intersection, no. of docs "+intersectMap.size());
		
		/*
		 * Step 4 - Compute the rank
		 */
		curTime=System.currentTimeMillis();
		final List<Document> resDocList=new ArrayList<Document>();
		final MinMaxPriorityQueue<Document> docQueue=MinMaxPriorityQueue.maximumSize(AppGlobals.WINDOW_SIZE).create();		
		for(final Entry<String,ArrayList<Posting>> docEntry:intersectMap.entrySet()){
			Double rank=0.0;
			//Compute the rank(doc,term)
			for(final Posting termEntry:docEntry.getValue()){
				final double idf=Math.log(AppGlobals.NO_OF_DOCUMENTS/termEntry.docFreq);
				rank+=(idf*Integer.parseInt(termEntry.termfreq));
			}
			docQueue.add(new Document(docEntry.getKey(),null,rank));
		}
		//Get the top N documents
		final Iterator<Document> iterator=docQueue.iterator();
		while(iterator.hasNext()){
			resDocList.add((Document)iterator.next());
		}
		Collections.sort(resDocList);
		if(AppGlobals.IS_DEBUG) System.out.println("rank computed in "+(System.currentTimeMillis()-curTime)+" ms");
		
		/*
		 * Step 5 - Get the document title.
		 */
		curTime=System.currentTimeMillis();
		final List<FutureTask<DocumentInfo>> docTaskList = new ArrayList<FutureTask<DocumentInfo>>();
		for(final Document doc:resDocList)
			docTaskList.add(new FutureTask<DocumentInfo>(new DocumentInfoFetcher(doc.id)));
		// Start the doc info retrieval tasks.
		executor = Executors.newFixedThreadPool(intersectMap.size());
		for (final FutureTask<DocumentInfo> task : docTaskList)
			executor.execute(task);
		// Collect the doc. info.		
		isDone=false;
		while(!isDone){
			// Check if all document details are fetched.
			if (Utilities.checkAllTasksComplete(docTaskList)) {
				executor.shutdown();
				for (final FutureTask<DocumentInfo> task : docTaskList) {
					final DocumentInfo docInfo=task.get();
					if(docInfo!=null){
						System.out.println(docInfo.title);
					}
				}
				isDone = true;
			}
		}
		if(AppGlobals.IS_DEBUG) System.out.println("Document Info retrieved in "+(System.currentTimeMillis()-curTime)+" ms");
		
	}

	private static HashMap<String,ArrayList<Posting>> convertTextToPostMap(String posting) {
		if(posting==null || posting.trim().length()==0) return null;
		final HashMap<String,ArrayList<Posting>> postMap=new HashMap<String,ArrayList<Posting>>();
		final String[] content=posting.split(",");
		final int docFreq=content.length;
		for(final String post:content){
			final String[] postContent=post.split(":");
			if(postMap.get(postContent[0])==null)
				postMap.put(postContent[0],new ArrayList<Posting>());
			postMap.get(postContent[0]).add(new Posting(docFreq,postContent[1]));
		}
		return postMap;
	}

	private static HashMap<String,ArrayList<Posting>> intersectPostings(final HashMap<String,ArrayList<Posting>> posting1,
			final String posting2) {
		// Intersect two sorted postings list.
		if (posting1 == null || posting2 == null
				|| posting1.size() == 0
				|| posting2.trim().length() == 0)
			return null;
		final String[] pos2 = posting2.split(",");
		int docFreq=pos2.length;
		for(final String post:pos2){
			final String[] postContent=post.split(":");
			if(posting1.get(postContent[0])!=null){
				posting1.get(postContent[0]).add(new Posting(docFreq,postContent[1]));
			}
		}
		return posting1;
	}
	
	private static List<PostingList> handleOrQueries(final List<PostingList> postingList,final List<Integer> orList){
		final List<PostingList> resList=new ArrayList<PostingList>();
		for(int index=0;index<postingList.size();index++){
			if(postingList.get(index)!=null) {
				if(orList.get(index)==-1){
					resList.add(postingList.get(index));
				} else {
					int cur=orList.get(index);
					PostingList curPosting=postingList.get(index);
					final PostingList newPosting=new PostingList("",0,curPosting.term);
					String resStr=curPosting.text.toString();
					int ii;
					for(ii=index+1;ii<postingList.size();ii++) {
						if(postingList.get(ii)!=null) {
							if(orList.get(ii)==cur){
								curPosting=postingList.get(ii);
								resStr=unionTwoPostings(resStr,curPosting.text.toString());
							} else break; 
						}
					}
					newPosting.docFreq+=resStr.split(",").length;
					newPosting.text.append(resStr);
					resList.add(newPosting);
					index=ii-1;
				}
			}
		}
		return resList;
	}
	
	private static String unionTwoPostings(final String pos1,final String pos2){
		final StringBuffer res=new StringBuffer();
		final String[] pos1Str=pos1.split(",");
		final String[] pos2Str=pos2.split(",");
		int p1_left=0,p2_left=0,p1_right=pos1Str.length-1,p2_right=pos2Str.length-1;
		while(p1_left<=p1_right && p2_left<=p2_right){
			final String[] pos1Content=pos1Str[p1_left].split(":");
			final String[] pos2Content=pos2Str[p2_left].split(":");
			int d1=Integer.parseInt(pos1Content[0]);
			int d2=Integer.parseInt(pos2Content[0]);
			if(d1==d2){
				res.append(d1+":"+(Integer.parseInt(pos1Content[1])+Integer.parseInt(pos2Content[1]))+",");
				p1_left++;
				p2_left++;
			} else if(d1<d2){
				res.append(d1+":"+pos1Content[1]+",");
				p1_left++;
			} else {
				res.append(d2+":"+pos2Content[1]+",");
				p2_left++;
			}
		}
		return res.toString();
	}

}