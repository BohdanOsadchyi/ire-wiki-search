package com.ire.parse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ire.commons.AppGlobals;
import com.ire.commons.VBEncoding;

public class XMLParser {

	private String file = null;
	private SAXParserFactory factory = null;
	private SAXParser parser = null;

	public XMLParser(final String file) throws ParserConfigurationException,
			SAXException {
		this.file = file;
		this.factory = SAXParserFactory.newInstance();
		this.parser = this.factory.newSAXParser();
	}

	public void parse() throws SAXException, IOException {
		final List<String> tokenStream = new ArrayList<String>();

		// Define the callback function for SAX parser.
		final DefaultHandler handler = new DefaultHandler() {
			private boolean isTitleOn = false, isBodyTextOn = false,
					isRevisionOn = false, isIdOn = false;
			private Tokenizer tokenizer = null;
			
			public void startElement(String uri, String localName,
					String qName, Attributes attributes) throws SAXException {
				final String tagName = qName.toLowerCase();
				if (tagName.equals("title"))
					isTitleOn = true;
				else if (tagName.equals("text"))
					isBodyTextOn = true;
				else if (tagName.equals("revision"))
					isRevisionOn = true;
				else if (tagName.equals("id"))
					isIdOn = true;
			}

			public void endElement(String uri, String localName, String qName)
					throws SAXException {
				final String tagName = qName.toLowerCase();
				if (tagName.equals("revision"))
					isRevisionOn = false;
				else if (tagName.equals("id"))
					isIdOn = false;
				else if (tagName.equals("title"))
					isTitleOn = false;
				else if (tagName.equals("text")) {
					// Create the token stream - input for SPIMI.
					for (Entry<String, Integer> entry : tokenizer.tokenize()
							.entrySet()) {
						tokenStream.add(entry.getKey() + "#" + tokenizer.getId() +":"+ entry.getValue());
						if (tokenStream.size() >= AppGlobals.SPIMI_TOKEN_STREAM_MAX_LIMIT) {
							// Call SPIMI once the block is full.
							try {
								SPIMI.createInvertedIndex(tokenStream);
							} catch (IOException e) {
								e.printStackTrace();
							}
							tokenStream.clear();
						}
					}
					
					//Save the doc details.
					if(tokenizer!=null){
						try {
							DictionaryManager.addDocInfo(tokenizer.getId(), tokenizer.getTitle());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
					System.out.println(count++);
				}
			}
			
			int count=0;
			
			public void characters(char ch[], int start, int length)
					throws SAXException {
				final String text = (new String(ch, start, length));
				if (isTitleOn)
					tokenizer = new Tokenizer(text);
				else if (isIdOn == true && isRevisionOn == false)
					tokenizer.setId(text);
				else if (isBodyTextOn) {
					tokenizer.appendBodyText(text);
					//tokenizer.setBodyLength(tokenizer.getBodyLength()+length);
				}
			}
		};

		this.parser.parse(this.file, handler);
		if (tokenStream.size() > 0) {
			try {
				SPIMI.createInvertedIndex(tokenStream);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Merge all the blocks
		SPIMI.mergeUncompressedBlocks();
	}

}
