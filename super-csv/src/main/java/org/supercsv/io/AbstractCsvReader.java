/*
 * Copyright 2007 Kasper B. Graversen
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.supercsv.io;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.comment.CommentMatcher;
import org.supercsv.decoder.CsvDecoder;
import org.supercsv.exception.SuperCsvConstraintViolationException;
import org.supercsv.exception.SuperCsvException;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.Util;

/**
 * Defines the standard behaviour of a CSV reader.
 * 
 * @author Kasper B. Graversen
 * @author James Bassett
 */
public abstract class AbstractCsvReader implements ICsvReader {

	private final LineNumberReader lnr;
	
	private final CsvPreference preferences;

	private final CsvDecoder decoder;

	private boolean ignoreEmptyLines;

	private CommentMatcher commentMatcher;

	private final StringBuilder unDecodedRow = new StringBuilder();

	// the current tokenized columns
	private final List<String> columns = new ArrayList<String>();
	
	// the number of CSV records read
	private int rowNumber = 0;
	
	/**
	 * Constructs a new <tt>AbstractCsvReader</tt>, using the default {@link Tokenizer}.
	 * 
	 * @param reader
	 *            the reader
	 * @param preferences
	 *            the CSV preferences
	 * @throws NullPointerException
	 *             if reader or preferences are null
	 */
	public AbstractCsvReader(final Reader reader, final CsvPreference preferences) {
		if( reader == null ) {
			throw new NullPointerException("reader should not be null");
		} else if( preferences == null ) {
			throw new NullPointerException("preferences should not be null");
		}
		
		this.preferences = preferences;
		this.decoder = preferences.getDecoder();
		this.ignoreEmptyLines = preferences.isIgnoreEmptyLines();
		this.commentMatcher = preferences.getCommentMatcher();
		this.lnr = new LineNumberReader(reader);
	}
	
	/**
	 * Closes the Tokenizer and its associated Reader.
	 */
	public void close() throws IOException {
		lnr.close();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String get(final int n) {
		return columns.get(n - 1); // column numbers start at 1
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String[] getHeader(final boolean firstLineCheck) throws IOException {
		
		if( firstLineCheck && lnr.getLineNumber() != 0 ) {
			throw new SuperCsvException(String.format(
				"CSV header must be fetched as the first read operation, but %d lines have already been read",
				lnr.getLineNumber()));
		}
		
		if( readRow() ) {
			return columns.toArray(new String[columns.size()]);
		}
		
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getLineNumber() {
		return lnr.getLineNumber();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getUntokenizedRow() {
		return unDecodedRow.toString();
	}

	public String getUndecodedRow() {
		return unDecodedRow.toString();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getRowNumber() {
		return rowNumber;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int length() {
		return columns.size();
	}
	
	/**
	 * Gets the tokenized columns.
	 * 
	 * @return the tokenized columns
	 */
	protected List<String> getColumns() {
		return columns;
	}
	
	/**
	 * Gets the preferences.
	 * 
	 * @return the preferences
	 */
	protected CsvPreference getPreferences() {
		return preferences;
	}
	
	/**
	 * Calls the tokenizer to read a CSV row. The columns can then be retrieved using {@link #getColumns()}.
	 * 
	 * @return true if something was read, and false if EOF
	 * @throws IOException
	 *             when an IOException occurs
	 * @throws SuperCsvException
	 *             on errors in parsing the input
	 */
	protected boolean readRow() throws IOException {
		columns.clear();
		unDecodedRow.setLength(0);
		String line;
		do {
			if(getLineNumber() == 0){
				line = Util.subtractBom(lnr.readLine());
			}else{
				line = lnr.readLine();
			}
			if(line == null) {
				return false;
			}
		}while( ignoreEmptyLines && line.length() == 0 || (commentMatcher != null && commentMatcher.isComment(line)) );
		unDecodedRow.append(line);
		columns.addAll(decoder.decode(line, preferences));
		rowNumber++;
		return true;
	}
	
	/**
	 * Executes the supplied cell processors on the last row of CSV that was read and populates the supplied List of
	 * processed columns.
	 * 
	 * @param processedColumns
	 *            the List to populate with processed columns
	 * @param processors
	 *            the cell processors
	 * @return the updated List
	 * @throws NullPointerException
	 *             if processedColumns or processors are null
	 * @throws SuperCsvConstraintViolationException
	 *             if a CellProcessor constraint failed
	 * @throws SuperCsvException
	 *             if the wrong number of processors are supplied, or CellProcessor execution failed
	 */
	protected List<Object> executeProcessors(final List<Object> processedColumns, final CellProcessor[] processors) {
		Util.executeCellProcessors(processedColumns, getColumns(), processors, getLineNumber(), getRowNumber());
		return processedColumns;
	}
	
}
