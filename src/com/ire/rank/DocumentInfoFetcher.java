package com.ire.rank;

import java.io.File;
import java.util.concurrent.Callable;

import com.ire.commons.AppGlobals;
import com.ire.commons.Utilities;
import com.ire.parse.TertiaryIndex;

class DocumentInfoFetcher implements Callable<DocumentInfo> {

	private int id;

	public DocumentInfoFetcher() {
	}

	public DocumentInfoFetcher(final String docId) {
		this.id = Integer.parseInt(docId.split(":")[0]);
	}

	@Override
	public DocumentInfo call() throws Exception {
		// Retrieve the posting list for the term.
		final File sIndexFile = new File(AppGlobals.DOC_INFO_SINDEX);
		final File docInfoFile = new File(AppGlobals.DOC_INFO_FILE);
		if (!sIndexFile.exists())
			return null; // secondary index missing.

		// Binary search.
		long totalLines=(sIndexFile.length()/16)-1;
		long left = TertiaryIndex.getDocPtr(id);
		long right = ((left+AppGlobals.DOC_INFO_TERTIARY_BLOCK_SIZE)-1)>totalLines?totalLines:((left+AppGlobals.DOC_INFO_TERTIARY_BLOCK_SIZE)-1);
		while (left <= right) {
			// Calculate the middle of the file (order of 16 bytes)
			long mid = left + ((right - left) / 2);

			// Get the docInfo ptrs.
			final String sIndexEntry = Utilities.readFromFile(sIndexFile,
					mid * 16, 16);
			final String[] docInfoEntry = Utilities.readFromFile(docInfoFile,
					Long.parseLong(sIndexEntry.substring(0, 10)),
					Integer.parseInt(sIndexEntry.substring(10)) - 1).split("\\$",
					-1);
			final int midDocId=Integer.parseInt(docInfoEntry[0]);
			if (midDocId==id) {
				// Correct docInfo entry found.
				return new DocumentInfo(docInfoEntry[0], docInfoEntry[1]);
			} else if (id>midDocId) {
				// Search in the right half, as the mid string precedes search
				// string
				left = mid + 1;
			} else {
				// Search in the left half, as the mid string succeeds search
				// string
				right = mid - 1;
			}
		}
		System.out.println("Not found - document - "+id);
		return null;
	}

}