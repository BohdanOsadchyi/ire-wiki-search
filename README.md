WikiSearch - Search engine on top of wikipedia pages

Features included:
1. XML Parsing - Used default SAX parser from Java SE. 
2. Tokenization - Hand-coded tokenizer (without using regular expressions)
3. Case folding - All tokens changed to lower case.
4. Stop words removal - Wordnet (http://www.d.umn.edu/~tpederse/Group01/WordNet/words.txt)
5. Stemming - Porter stemmer (http://tartarus.org/martin/PorterStemmer/java.txt)
6. Posting List / Inverted Index creation 
7. Fetch documents by query (Tfidf rank)

Term Field Abbreviations:
I - Infobox
B - Body
T - Title
L - External Link
R - References
C - Category

Tested environment:
OS - Windows 7 (64bit 4GB RAM I5 Processor)
Language - Java SE 1.8
VM arguments - Default values
Extra Libraries Used - None

Features Failed
1. VB Encoding
2. Okapi bm25