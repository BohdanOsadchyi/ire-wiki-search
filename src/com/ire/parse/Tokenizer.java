package com.ire.parse;

import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ire.commons.Utilities;

public class Tokenizer {
	private String title = null, id = null;
	final String PATTERN_TOKEN = "\\$%{}[]()`<>='&:,;/.~ ;*\n|\"^_-+!?#\t@";
	final HashMap<String, Integer> masterTokenList = new HashMap<String, Integer>();
	private StringBuffer bodyText = new StringBuffer("");
	private int bodyLength=0;
	
	public Tokenizer() {
	}

	public Tokenizer(final String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void appendBodyText(final String text) {
		bodyText.append(text);
	}

	public String getBodyText() {
		return bodyText.toString();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public int getBodyLength() {
		return bodyLength;
	}

	public void setBodyLength(int bodyLength) {
		this.bodyLength = bodyLength;
	}

	public HashMap<String, Integer> tokenize() {
		// Initator of parsing.
		final String content = bodyText.toString().toLowerCase();
		extractToken(title.toLowerCase(), 'T');
		extractBody(content);
		extractLinks(content);
		extractCategories(content);
		extractReferences(content);
		extractInfoBox(content);
		return masterTokenList;
	}
	
	public void extractToken(final String content,final char field){
		//Use PATTERN_TOKEN to tokenize the text.
		final StringTokenizer normalTokenizer = new StringTokenizer(content,PATTERN_TOKEN);
		while(normalTokenizer.hasMoreTokens()){
			String word = normalTokenizer.nextToken().trim();
			if(word.length()>0 && Utilities.isNonStopWord(word) && !Utilities.isSpecialChar(word)) {
				word=Utilities.stemWord(word);
				if(Utilities.isNumber(word)) 
					addToMasterMap(field+":$d");
				else
					addToMasterMap(field+":"+word);
			}
		}
	}
	
	public void extractBody(final String content){
		//Tokenize the page body text.
		
		//Remove the useless tokens.
		String newContent = content.toString();
		newContent = newContent.replaceAll("<ref>.*?</ref>", "");
		newContent = newContent.replaceAll("</?.*?>", "");
		newContent = newContent.replaceAll("\\{\\{.*?\\}\\}", "");
		newContent = newContent.replaceAll("\\[\\[.*?:.*?\\]\\]", "");
		newContent = newContent.replaceAll("\\[\\[(.*?)\\]\\]", "");
		newContent = newContent.replaceAll("\\s(.*?)\\|(\\w+\\s)", " $2");
		newContent = newContent.replaceAll("\\[.*?\\]", " ");
		newContent = newContent.replaceAll("(?s)<!--.*?-->", "");
		newContent = newContent.replaceAll("\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", "");
		newContent=deleteCitation(newContent);

		extractToken(newContent,'B');
	}
	
	public void extractLinks(final String content){
		//Tokenize the link. [[user innovation]]
		final Pattern linkPattern = Pattern.compile("\\[\\[(.*?)\\]\\]", Pattern.MULTILINE);
		final Matcher matcher = linkPattern.matcher(content);
		while(matcher.find()) {
			final String [] match = matcher.group(1).split("\\|");
			if(match == null || match.length == 0) continue;
			final String link = match[0];
			if(link.contains(":") == false) {
				extractToken(link,'L');
			}
		}
	}
	
	public void extractCategories(final String content){
		//Tokenize the categories
		final Pattern categoryPattern = Pattern.compile("\\[\\[category:(.*?)\\]\\]", Pattern.MULTILINE);
		final Matcher matcher = categoryPattern.matcher(content);
		while(matcher.find()) {
			final String [] match = matcher.group(1).split("\\|");
			extractToken(match[0],'C');
		}
	}
	
	public void extractInfoBox(final String content){
		//Tokenize the infobox.
		final String infoBoxPatterm = "{{infobox";
		
		//Find the start pos and end pos of info box.
	    int startPos = content.indexOf(infoBoxPatterm);
	    if(startPos < 0) return ;
	    int bracketCount = 2;
	    int endPos = startPos + infoBoxPatterm.length();
	    for(; endPos < content.length(); endPos++) {
	      switch(content.charAt(endPos)) {
	        case '}':
	          bracketCount--;
	          break;
	        case '{':
	          bracketCount++;
	          break;
	        default:
	      }
	      if(bracketCount == 0) break;
	    }
	    if(endPos+1 >= content.length()) return;

	    //Filter the infobox
	    String infoBoxText = content.substring(startPos, endPos+1);
	    infoBoxText = deleteCitation(infoBoxText);	    
	    infoBoxText = infoBoxText.replaceAll("&gt;", ">");
	    infoBoxText = infoBoxText.replaceAll("&lt;", "<");
	    infoBoxText = infoBoxText.replaceAll("<ref.*?>.*?</ref>", " ");
		infoBoxText = infoBoxText.replaceAll("</?.*?>", " ");
		
		extractToken(content.substring(startPos, endPos+1),'I');
	}
	
	public String deleteCitation(final String content) {
		//Deletes the citation from the content.
		final String CITE_PATTERN = "{{cite";
		
		//Find the start pos and end pos of citation.
	    int startPos = content.indexOf(CITE_PATTERN);
	    if(startPos < 0) return content;
	    int bracketCount = 2;
	    int endPos = startPos + CITE_PATTERN.length();
	    for(; endPos < content.length(); endPos++) {
	      switch(content.charAt(endPos)) {
	        case '}':
	          bracketCount--;
	          break;
	        case '{':
	          bracketCount++;
	          break;
	        default:
	      }
	      if(bracketCount == 0) break;
	    }
	    
	    //Discard the citation and search for remaining citations.
	    final String text = content.substring(0, startPos-1) + content.substring(endPos);
	    return deleteCitation(text); 
	}
	
	public String extractReferences(final String content) {
		//Extracts the citation from the content.
		final String CITE_PATTERN = "{{cite";
		
		//Find the start pos and end pos of citation.
	    int startPos = content.indexOf(CITE_PATTERN);
	    if(startPos < 0) return content;
	    int bracketCount = 2;
	    int endPos = startPos + CITE_PATTERN.length();
	    for(; endPos < content.length(); endPos++) {
	      switch(content.charAt(endPos)) {
	        case '}':
	          bracketCount--;
	          break;
	        case '{':
	          bracketCount++;
	          break;
	        default:
	      }
	      if(bracketCount == 0) break;
	    }
	    
	    //Extract the citation and search for remaining citations.
	    extractToken(content.substring(startPos, endPos),'R');
		
	    final String text = content.substring(0, startPos-1) + content.substring(endPos);
	    return extractReferences(text); 
	}
	
	public void addToMasterMap(final String token) {
		if (masterTokenList.get(token) != null)
			masterTokenList.put(token, masterTokenList.get(token) + 1);
		else
			masterTokenList.put(token, 1);
	}

}
