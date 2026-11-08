package com.skagit.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class LineGetter {
    final public static String _EndIndicator = "END!!";
    private String _nextLine;
    final private InputStream _inputStream;
    final private boolean _ignoreEmptyLines;
    private Scanner _scanner;
    private int _nLinesRead;
    private int _nLinesReturned;

    public LineGetter(final InputStream inputStream, final boolean ignoreEmptyLines) {
	_inputStream = inputStream;
	_ignoreEmptyLines = ignoreEmptyLines;
	_scanner = new Scanner(inputStream);
	_nLinesRead = 0;
	_nLinesReturned = 0;
	setNextLine();
    }

    public boolean hasNext() {
	return _nextLine != null;
    }

    public String nextLine() {
	final String line = _nextLine;
	setNextLine();
	return line;
    }

    public void close() {
	if (_scanner != null) {
	    _scanner.close();
	    try {
		_inputStream.close();
	    } catch (final IOException e) {
	    }
	    _scanner = null;
	}
    }

    private void setNextLine() {
	_nextLine = null;
	boolean readNextLine = true;
	while (_scanner.hasNext() && readNextLine) {
	    readNextLine = false;
	    String line = _scanner.nextLine();
	    ++_nLinesRead;
	    /** Completely blank lines might be ignored. */
	    if (line.isEmpty()) {
		if (_ignoreEmptyLines) {
		    readNextLine = true;
		    continue;
		}
		/**
		 * This is a blank line. If we're not ignoring it, we're done. There's nothing
		 * to add, but we might want to set _nextLine to the blank.
		 */
		if (_nextLine == null) {
		    _nextLine = line;
		}
		break;
	    }
	    /** Ignore empty lines and those that start with a '#'. */
	    if (line.charAt(0) == '#') {
		readNextLine = true;
		continue;
	    }
	    /** We're done if it starts with _EndIndicator. */
	    if (line.startsWith(_EndIndicator)) {
		_nextLine = null;
		break;
	    }
	    /** Ignore everything after "//". */
	    int index = line.indexOf("//");
	    if (index >= 0) {
		line = line.substring(0, index);
	    }
	    /** Ignore all that's after '^', but record that we need the next line. */
	    index = line.indexOf('^');
	    if (index >= 0) {
		readNextLine = true;
		line = line.substring(0, index);
	    }
	    if (_nextLine == null) {
		_nextLine = line;
	    } else {
		_nextLine += line;
	    }
	}
	if (_nextLine == null) {
	    close();
	}
	if (_nextLine != null) {
	    ++_nLinesReturned;
	}
    }

    public int getNLinesRead() {
	return _nLinesRead;
    }

    public int getNLinesReturned() {
	return _nLinesReturned;
    }

    public static void main(final String[] args) {
	try {
	    final char a = (char) System.in.read();
	    System.out.printf("a |%c|", a);
	} catch (final IOException e) {
	}
	System.exit(33);
	try (Scanner scanner = new Scanner(System.in)) {
	    System.out.printf("\nHit enter to commence Resolution[%d]. ", 5);
	    final int b = scanner.nextInt();
	    System.out.printf("b |%d|", b);
	}
    }
}
