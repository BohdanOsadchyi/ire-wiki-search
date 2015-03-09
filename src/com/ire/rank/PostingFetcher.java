package com.ire.rank;

import java.io.File;
import java.util.concurrent.Callable;

import com.ire.commons.AppGlobals;
import com.ire.commons.Utilities;
import com.ire.parse.TertiaryIndex;

class PostingFetcher implements Callable<PostingList> {

	private String term;

	public PostingFetcher() {
	}

	public PostingFetcher(final String term) {
		this.term = term;
	}

	@Override
	public PostingList call() throws Exception {
		// Retrieve the posting list for the term.
		final File sIndexFile = new File(AppGlobals.DICTIONARY_SINDEX);
		final File dictFile = new File(AppGlobals.DICTIONARY_FILE);
		final File postFile = new File(AppGlobals.POSTINGS_FILE);
		if (!sIndexFile.exists())
			return null; // secondary index missing.

		// Binary search.
		long totalLines=(sIndexFile.length()/16)-1;
		long left = TertiaryIndex.getDictPtr(term);
		long right = ((left+AppGlobals.DICT_TERTIARY_BLOCK_SIZE)-1)>totalLines?totalLines:((left+AppGlobals.DICT_TERTIARY_BLOCK_SIZE)-1);
		while (left <= right) {
			// Calculate the middle of the file (order of 16 bytes)
			long mid = left + ((right - left) / 2);
			// Get the dictionary ptrs.
			final String sIndexEntry = Utilities.readFromFile(sIndexFile,
					mid * 16, 16);
			final String[] dictEntry = Utilities.readFromFile(dictFile,
					Long.parseLong(sIndexEntry.substring(0, 10)),
					Integer.parseInt(sIndexEntry.substring(10)) - 1).split(",",
					-1);
			if (dictEntry[0].equals(term)) {
				// Correct dictionary entry found.
				final String[] postPtr = dictEntry[1].split(":");
				return new PostingList(Utilities.readFromFile(postFile,
						Long.parseLong(postPtr[0]),
						Integer.parseInt(postPtr[1]) - 1),
						Integer.parseInt(dictEntry[2]), term);
			} else if (dictEntry[0].compareTo(term) < 0) {
				// Search in the right half, as the mid string precedes search
				// string
				left = mid + 1;
			} else {
				// Search in the left half, as the mid string succeeds search
				// string
				right = mid - 1;
			}
		}
		System.out.println("Not found - posting - "+term);
		return null;
	}

}