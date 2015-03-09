package com.ire.rank;

public class PostingList implements Comparable<PostingList>{
	final StringBuffer text = new StringBuffer();
	String term;
	int docFreq = 0;

	public PostingList() {
	}

	public PostingList(final String text, final int docFreq, final String term) {
		this.text.append(text);
		this.docFreq = docFreq;
		this.term = term;
	}

	public String toString() {
		return term + "(" + docFreq + ") = " + text.toString();
	}
	
	@Override
	public int compareTo(PostingList post) {
		//Define the posting comparator. (increasing order by doc freq.)
		if(this.docFreq<post.docFreq) return -1;
		if(this.docFreq>post.docFreq) return 1;
		return 0;
	}

}
