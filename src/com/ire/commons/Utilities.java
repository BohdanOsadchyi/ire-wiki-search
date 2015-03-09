package com.ire.commons;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.FutureTask;

public class Utilities {

	public static List<String> stopWordList = new ArrayList<String>();

	public static void initializeStopWords() throws IOException {
		// Preprocess, stem and save the stop words in map.
		final BufferedReader reader = new BufferedReader(new FileReader(
				AppGlobals.STOP_WORDS_LIST));
		String word = reader.readLine();
		while (word != null) {
			word = word.toLowerCase().trim();
			stopWordList.add(word);
			word = reader.readLine();
		}
		reader.close();
	}

	public static boolean isNonStopWord(final String word) {
		// Utility to check if a word is non stop word or not.
		return stopWordList.indexOf(word) == -1;
	}

	public static String stemWord(final String word) {
		// Stem the word and return it.
		final Stemmer stemmer = new Stemmer();
		stemmer.add(word.toCharArray(), word.length());
		stemmer.stem();
		return stemmer.toString();
	}

	public static String readFromFile(final String file) throws IOException {
		// Utility to read contents from a file.
		final FileReader reader = new FileReader(file);
		final StringBuffer buffer = new StringBuffer();
		int c;
		while ((c = reader.read()) != -1)
			buffer.append((char) c);
		reader.close();
		return buffer.toString();
	}

	public static List<String> removeSpecialChars(final String word) {
		// Utility to generate only alpha-numeric tokens from the word.
		if (word.length() == 0)
			return null;
		final List<String> tokenList = new ArrayList<String>();
		String token = "";
		for (int index = 0; index < word.length(); index++) {
			char ch = word.charAt(index);
			if (('a' <= ch && ch <= 'z') || ('0' <= ch && ch <= '9'))
				token += ch;
			else if (token.trim().length() != 0) {
				tokenList.add(token);
				token = "";
			}
		}
		if (token.length() != 0)
			tokenList.add(token);
		return tokenList;
	}

	public static List<String> markNumbers(final List<String> tokenList,
			final String replacement) {
		// Utility to mark the numbers in the given list with special
		// replacement.
		if (tokenList == null)
			return null;
		final List<String> resultList = new ArrayList<String>();
		for (final String token : tokenList)
			if (isNumber(token))
				resultList.add(replacement);
			else
				resultList.add(token);
		return resultList;
	}

	public static boolean isNumber(final String text) {
		return text.matches("[0-9]+") || text.matches("[0-9]+.[0-9]+");
	}
	
	public static int countNumbers(final String text){
		int count=0;
		for(int i=0;i<text.length();i++)
			if('0'<=text.charAt(i) && text.charAt(i)<='9')
				++count;
		return count;
				
	}

	public static HashMap<String, Integer> genTokenMap(
			final List<String> tokenList) {
		// Utility to convert list to frequency map.
		final HashMap<String, Integer> tokenMap = new HashMap<String, Integer>();
		for (final String token : tokenList)
			if (tokenMap.get(token) != null)
				tokenMap.put(token, tokenMap.get(token) + 1);
			else
				tokenMap.put(token, 1);
		return tokenMap;
	}

	public static boolean isFullAlphabetic(final String token) {
		// Utility to check if the given token contains only alphabets
		boolean isAlphaOn = false, isNumOn = false;
		for (int i = 0; i < token.length(); i++)
			if ('a' <= token.charAt(i) && token.charAt(i) <= 'z')
				isAlphaOn = true;
			else if ('0' <= token.charAt(i) && token.charAt(i) <= '9')
				isNumOn = true;
		return (isAlphaOn == true && isNumOn == false);
	}
	
	public static boolean isValidToken(final String token){
		if(token==null || token.trim().length()==0 || token.trim().length()>12 || !isFullAlphabetic(token)) return false;
		//Check if the same char is occurring more than 3 times
		char prevChar=token.charAt(0);
		int count=1;
		for(int index=1;index<token.length();index++){
			if(token.charAt(index)==prevChar) ++count;
			else {prevChar=token.charAt(index); count=1;}
			if(count==4) return false;
		}		
		return true;
	}

	public static boolean isSpecialChar(final String token) {
		// Utility to check if special char is present.
		for (int i = 0; i < token.length(); i++)
			if (!(('a' <= token.charAt(i) && token.charAt(i) <= 'z') || ('0' <= token
					.charAt(i) && token.charAt(i) <= '9')))
				return true;
		return false;
	}

	public static String readFromFile(final String FILE, long byteStart,
			int length) throws IOException {
		return readFromFile(new File(FILE), byteStart, length);
	}

	public static String readFromFile(final File file, long byteStart,
			int length) throws IOException {
		final FileInputStream fis = new FileInputStream(file);
		fis.skip(byteStart);
		final BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		final char[] content = new char[length];
		br.read(content, 0, length);
		br.close();
		fis.close();
		return new String(content);
	}

	public static <T> boolean checkAllTasksComplete(
			final List<FutureTask<T>> taskList) {
		for (final FutureTask<T> task : taskList)
			if (!task.isDone())
				return false;
		return true;
	}

	public static <T> TreeMap<T, Integer> sortByValue(
			TreeMap<T, Integer> unsortedMap) {
		final ValueComparator vc = new ValueComparator(unsortedMap);
		final TreeMap<T, Integer> sortedMap = new TreeMap<T, Integer>(
				vc);
		sortedMap.putAll(unsortedMap);
		return sortedMap;
	}

}

class ValueComparator<T> implements Comparator<T> {
	Map<String, Integer> map;

	public ValueComparator(Map<String, Integer> base) {
		this.map = base;
	}

	public int compare(T a, T b) {
		if (map.get(a) <= map.get(b)) {
			return -1;
		} else {
			return 1;
		} // returning 0 would merge keys
	}
}
