package com.ire.rank;

class Document implements Comparable<Document>{
	String id;
	String title;
	Double rank;
	
	public Document(){}
	public Document(String id,String title,double rank){this.id=id;this.title=title;this.rank=rank;}
	public String toString() {
		return id + "-" + title + "-(" + rank+")";
	}
	
	@Override
	public int compareTo(Document doc) {
		int val=this.rank.compareTo(doc.rank);
		if(val>0) return -1;
		if(val<0) return 1;
		return 0;
	}
	
}