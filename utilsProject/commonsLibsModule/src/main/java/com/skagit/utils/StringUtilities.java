package com.skagit.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.skagit.utils.lsFormatter.LsFormatter;

public class StringUtilities {
	final public static String _WhiteSpace = "\\s+";
	final private static String _SpaceProxy = "%Sp%";

	public static boolean isEmpty(final String s) {
		return s == null || "".equals(s);
	}

	public static String CleanWhiteSpace(final String s) {
		if (s == null) {
			return "";
		}
		final String ss = s.trim().replaceAll(_WhiteSpace, " ");
		return ss.length() == s.length() ? s : ss;
	}

	public static String[][] deepCopy(final String[][] stringArray) {
		final int nRows = stringArray.length, nClmns = stringArray[0].length;
		final String[][] newStringArray = new String[nRows][nClmns];
		for (int row = 0; row < nRows; ++row) {
			System.arraycopy(stringArray[row], 0, newStringArray[row], 0, nClmns);
		}
		return newStringArray;
	}

	public static String getString(final String[][] stringArray, final int fieldLength) {
		if (stringArray == null) {
			return "NULL";
		}
		final int nRows = stringArray.length;
		final StringBuilder s0 = new StringBuilder();
		for (int kRow = 0; kRow < nRows; ++kRow) {
			final String[] row = stringArray[kRow];
			final int nClmns = row == null ? 0 : row.length;
			s0.append(kRow > 0 ? "\n" : "");
			for (int kClmn = 0; kClmn < nClmns; ++kClmn) {
				final String s = row[kClmn];
				final String field = s == null ? "" : s;
				s0.append(kClmn > 0 ? " " : "").append(padRight(field, fieldLength));
			}
		}
		return s0.toString();
	}

	/**
	 * Returns a copy of source, except that any contiguous run from delims is
	 * replaced by a single instance of delim. If delim is (char)0, no delimiter is
	 * inserted.
	 */
	public static String cleanUpString(final String source, final String delims, final char delim) {
		if (source == null) {
			return null;
		}
		final boolean haveFirst, haveLast;
		if (delim == (char) 0) {
			haveFirst = haveLast = false;
		} else {
			haveFirst = delims.indexOf(source.charAt(0)) >= 0;
			haveLast = delims.indexOf(source.charAt(source.length() - 1)) >= 0;
		}
		final StringBuffer b = new StringBuffer();
		if (haveFirst) {
			b.append(delim);
		}
		final java.util.StringTokenizer st = new java.util.StringTokenizer(source, delims);
		while (st.hasMoreElements()) {
			final String nextToken = st.nextToken();
			b.append(nextToken);
			if (st.hasMoreTokens()) {
				b.append(delim);
			}
		}
		if (haveLast) {
			b.append(delim);
		}
		return new String(b);
	}

	public static String cleanUpFilePath(final String filePathIn) {
		if (filePathIn != null) {
			try {
				final File f = new File(filePathIn);
				return f.getCanonicalPath();
			} catch (final IOException | NullPointerException e) {
				/** Not Worried. */
			}
		}
		return null;
	}

	public static String getCanonicalPath(final File f) {
		try {
			return f.getCanonicalPath();
		} catch (final IOException | NullPointerException e) {
			/** Not Worried. */
		}
		return null;
	}

	public static String indent(final String src) {
		/** Kill any double-sharp to the end of its line. */
		String commentFreeString = src;
		commentFreeString = "";
		for (String remainingString = src;;) {
			final int nextDoubleSharp = remainingString.indexOf("##");
			if (nextDoubleSharp < 0) {
				commentFreeString += remainingString;
				break;
			}
			commentFreeString += remainingString.substring(0, nextDoubleSharp);
			/** Skip past the ## and then past the \n. */
			if (nextDoubleSharp + 2 >= remainingString.length()) {
				break;
			}
			remainingString = remainingString.substring(nextDoubleSharp + 2);
			final int newLineIndex = remainingString.indexOf('\n');
			if (newLineIndex < 0) {
				break;
			}
			remainingString = remainingString.substring(newLineIndex);
		}
		/**
		 * Change a contiguous string of tabs and newlines to a single newline.
		 */
		commentFreeString = cleanUpString(commentFreeString, "\n\t", '\n');
		ArrayList<String> lines = new ArrayList<>();
		/** Use newLines only as delimiters. */
		for (String remainingString = commentFreeString;;) {
			final int newLineIndex = remainingString.indexOf('\n');
			if (newLineIndex < 0) {
				lines.add(remainingString);
				break;
			}
			final String line = remainingString.substring(0, newLineIndex).trim();
			if (line.length() > 0) {
				lines.add(line);
			}
			remainingString = remainingString.substring(newLineIndex + 1);
		}
		/** Any open brace or close brace is its own line. */
		for (final char c : new char[] { '{', '}' }) {
			final ArrayList<String> lines2 = new ArrayList<>();
			for (final String line : lines) {
				for (String remainingString = line;;) {
					final int cIndex = remainingString.indexOf(c);
					if (cIndex < 0) {
						final String trimmedLine = remainingString.trim();
						if (trimmedLine.length() > 0) {
							lines2.add(trimmedLine);
						}
						break;
					}
					final String trimmedLine = remainingString.substring(0, cIndex).trim();
					if (trimmedLine.length() > 0) {
						lines2.add(trimmedLine);
					}
					lines2.add("" + c);
					remainingString = remainingString.substring(cIndex + 1);
				}
			}
			lines = lines2;
		}
		int nOpen = 0;
		commentFreeString = "";
		for (final String line : lines) {
			commentFreeString += '\n';
			final char charAtZero = line.charAt(0);
			if (charAtZero == '}') {
				--nOpen;
			}
			for (int k = 0; k < nOpen; ++k) {
				commentFreeString += '\t';
			}
			commentFreeString += line;
			if (line.length() == 1 && charAtZero == '{') {
				++nOpen;
			}
		}
		return commentFreeString;
	}

	public static String toDegreesMinutesSeconds(double angle) {
		if (angle >= 180.0) {
			angle -= 360.0;
		} else if (angle < -180.0) {
			angle += 360.0;
		}
		final boolean negative = angle < 0.0;
		if (negative) {
			angle = -angle;
		}
		final int degrees = (int) Math.round(Math.floor(angle));
		angle -= degrees;
		angle *= 60.0;
		final int minutes = (int) Math.round(Math.floor(angle));
		angle -= minutes;
		angle *= 60.0;
		final int seconds = (int) Math.round(Math.floor(angle));
		return String.format("%1c%03d-%02d.%02d", negative ? '-' : '+', degrees, minutes, seconds);
	}

	public static String toPercent(final double d) {
		return String.format("%.2f%%", d * 100.);
	}

	public static String getStackTraceString(final Throwable e) {
		if (e == null) {
			return "Cannot get Stack Trace for null Throwable.";
		}
		final StackTraceElement[] trace = e.getStackTrace();
		final StringBuilder s = new StringBuilder();
		for (final StackTraceElement element : trace) {
			s.append("\n\t").append(element.getFileName());
			if (element.isNativeMethod()) {
				s.append("\tNATIVE.");
			} else {
				s.append(" ").append(element.toString());
			}
		}
		return s.toString();
	}

	public static String getStackTraceString() {
		final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
		final StringBuilder s = new StringBuilder();
		for (final StackTraceElement element : trace) {
			s.append("\n\t").append(element.getFileName());
			if (element.isNativeMethod()) {
				s.append("\tNATIVE.");
			} else {
				s.append(" ").append(element.toString());
			}
		}
		return s.toString();
	}

	public static void main(final String[] args) {
		final String path = null;
		cleanUpFilePath(path);
		final String test = "#A\t\t\n  ##Junk\nABC\t\nDEF";
		System.out.println(indent(test));
		final String test2 = "{ABC\n\n}{\nA";
		System.out.println(indent(test2));
		final File file = new File(args[0]);
		System.out.println(getContents(file));
	}

	public static String getContents(final File file) {
		final StringBuilder contents = new StringBuilder();
		try (final FileReader fileReader = new FileReader(file);
				final BufferedReader bufferedReader = new BufferedReader(fileReader)) {
			/**
			 * readLine is a bit quirky. It returns the content of a line MINUS the newline.
			 * It returns null only for the END of the stream. It returns an empty String if
			 * two newlines appear in a row.
			 */
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				line = cleanUpString(line, "|", '\n');
				if (line.indexOf("exclu") >= 0) {
					contents.append(line);
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return contents.toString();
	}

	public static String getLegalXmlName(final String key, final String prefix) {
		final String oddBalls = "_.-";
		/**
		 * Strip everything except alpha and numeric. Replace Section Symbol with "._.".
		 */
		final StringBuffer stringBuffer = new StringBuffer(key.length());
		final int sectionSymbolIndex = key.indexOf(Constants0._SectionSymbol);
		if (sectionSymbolIndex < 0) {
			for (int iPass = 0; iPass < 2; ++iPass) {
				for (final char c : key.toCharArray()) {
					if (Character.isAlphabetic(c)) {
						stringBuffer.append(stringBuffer.length() == 0 ? Character.toUpperCase(c) : c);
					} else if ((stringBuffer.length() > 0) && (Character.isDigit(c) || oddBalls.indexOf(c) >= 0)) {
						stringBuffer.append(c);
					}
				}
				if (stringBuffer.length() > 0) {
					return new String(stringBuffer);
				}
				stringBuffer.append(prefix == null ? "XX" : prefix);
			}
			return new String(stringBuffer);
		}
		final String part1String = key.substring(0, sectionSymbolIndex);
		final String part2String = key.substring(sectionSymbolIndex + 1);
		return getLegalXmlName(part1String, "AA_") + "._." + getLegalXmlName(part2String, "_ZZ");
	}

	/**
	 * For commenting, see:
	 *
	 * <pre>
	 *
	 * http://www.exampledepot.com/egs/org.w3c.dom/AddNode.html
	 * http://www.w3.org/TR/2000/REC-xml-20001006#NT-Name
	 */
	public static void fillInAncillaryElement(final LsFormatter formatter, final Element parentElement,
			final Object o) {
		final Document document = parentElement.getOwnerDocument();
		final Class<?> classz = o.getClass();
		final Field[] declaredFields = classz.getDeclaredFields();
		for (final Field field : declaredFields) {
			/** Not interested in statics. */
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			/** Only doubles, floats, and ints. */
			final String typeName = field.getType().getName();
			if (!"double".equals(typeName) && !"int".equals(typeName) && !"float".equals(typeName)) {
				continue;
			}
			/**
			 * Only those starting with an underscore and a lower case letter.
			 */
			final String fieldName = field.getName();
			if ((fieldName.charAt(0) != '_') || !Character.isLowerCase(fieldName.charAt(1))) {
				continue;
			}
			try {
				field.setAccessible(true);
				if ("double".equals(typeName) && Double.isNaN(field.getDouble(o))) {
					continue;
				}
				final String text = "int".equals(typeName) ? ("" + field.getInt(o)) : ("" + field.getDouble(o));
				final String legalTextNodeName = StringUtilities.getLegalXmlName(fieldName, "FLD");
				final Element element1 = formatter.newChild(parentElement, legalTextNodeName);
				element1.appendChild(document.createTextNode(text));
			} catch (final DOMException | IllegalArgumentException | IllegalAccessException e) {
				/** Not Worried. */
			}
		}
	}

	public static Element fillInAncillaryElement(final Object o, final LsFormatter formatter,
			final Element parentElement, final boolean buildElement, final String key, final double[] extraNumbers,
			final String[] extraLabels) {
		final Document document = parentElement.getOwnerDocument();
		final Element entryElement;
		if (buildElement) {
			final String legalTag = StringUtilities.getLegalXmlName(key, null);
			entryElement = formatter.newChild(parentElement, legalTag);
			final String entryCommentBody = "Ancillary Results for Above.";
			final Comment entryComment = document.createComment(entryCommentBody);
			parentElement.insertBefore(entryComment, entryElement);
		} else {
			entryElement = parentElement;
		}
		final Class<?> classz = o.getClass();
		final Field[] declaredFields = classz.getDeclaredFields();
		for (final Field field : declaredFields) {
			final String typeName = field.getType().getName();
			if (!"double".equals(typeName)) {
				continue;
			}
			final String fieldName = field.getName();
			if (fieldName.charAt(0) != '_') {
				continue;
			}
			try {
				field.setAccessible(true);
				final double d = field.getDouble(o);
				if (!Double.isNaN(d)) {
					final String legalTextNodeName = StringUtilities.getLegalXmlName(fieldName, null);
					final Element textNode = formatter.newChild(entryElement, legalTextNodeName);
					textNode.appendChild(document.createTextNode("" + d));
				}
			} catch (final DOMException | IllegalArgumentException | IllegalAccessException e) {
				/** Not Worried. */
			}
		}
		if (extraNumbers != null) {
			final int nExtraNumbers = extraNumbers.length;
			for (int k = 0; k < nExtraNumbers; ++k) {
				final String label = extraLabels[k];
				final double d = extraNumbers[k];
				final String legalTextNodeName = StringUtilities.getLegalXmlName(label, null);
				final Element textNode = formatter.newChild(entryElement, legalTextNodeName);
				if (Math.floor(d) == d) {
					textNode.appendChild(document.createTextNode("" + (int) d));
				} else {
					textNode.appendChild(document.createTextNode("" + d));
				}
			}
		}
		return entryElement;
	}

	public static String padRight(final String s, final int n) {
		return String.format("%1$-" + n + "s", s);
	}

	public static String padLeft(final String s, final int n) {
		return String.format("%1$" + n + "s", s);
	}

	public static String getSystemProperty(final String propertyName, final boolean useSpaceProxy) {
		final String rawProperty = System.getProperty(propertyName);
		if (rawProperty == null || !useSpaceProxy) {
			return rawProperty;
		}
		return rawProperty.replace(_SpaceProxy, " ");
	}

	public static Boolean getBoolean(final String raw) {
		if (raw == null || raw.length() == 0) {
			return null;
		}
		/** True is "On," "Yes," or "True." Case insensitive. */
		final String s = raw.toLowerCase();
		if (s.startsWith("y") || s.startsWith("t")) {
			return Boolean.TRUE;
		}
		if (s.startsWith("n") || s.startsWith("f")) {
			return Boolean.FALSE;
		}
		if (s.length() == 1) {
			return null;
		}
		if ("on".equalsIgnoreCase(s.substring(0, 2))) {
			return Boolean.TRUE;
		}
		if ("of".equalsIgnoreCase(s.substring(0, 2))) {
			return Boolean.FALSE;
		}
		return null;
	}

	public static boolean stringToBoolean(final String raw) {
		return raw == null ? false : getBoolean(raw);
	}
}
