package com.ire.rank;

public class Posting {
	int docFreq;
	String termfreq;
	String term;
	
	public Posting(){}
	public Posting(final int docFreq,final String termFreq){this.docFreq=docFreq; this.termfreq=termFreq;}
}
