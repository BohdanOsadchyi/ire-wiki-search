package com.ire.rank;

class DocumentInfo {
	String id, title;

	public DocumentInfo() {
	}

	public DocumentInfo(final String id, final String title) {
		this.id = id;
		this.title = title;
	}
	
	public String toString() {
		return id + "-" + title;
	}
}