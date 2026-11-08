package com.skagit.utils.lsFormatter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import com.skagit.utils.Constants0;
import com.skagit.utils.DirsTracker;
import com.skagit.utils.StringUtilities;
import com.skagit.utils.myLogger.MyLogger;

/**
 * Class responsible for producing formatted XML. It has utility functions
 * allowing to generate an XML using the DOM API; it also provides for a simple
 * way of writing the formatted document into a file or a String.
 */
public class LsFormatter {

	final public static DocumentBuilder _DocumentBuilder;
	static {
		DocumentBuilder documentBuilder = null;
		try {
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			documentBuilder = factory.newDocumentBuilder();
		} catch (final Exception e) {
		}
		_DocumentBuilder = documentBuilder;
	}

	/** UTF-8 seems to work well within SAROPS. */
	final public static String _CharsetName = "UTF-8";
	final private static int _NDigitsForStandardDouble = 17;
	final private static int _NDigitsForLatorLng = 7;
	/** Object responsible for writing the formatted document. */
	final DOMImplementationLS _domImplementationLs;
	final LSSerializer _lsSerializer;
	/**
	 * The document managing all XML structures that will eventually be formatted.
	 */
	final private Document _document;
	/**
	 * When not null, the document type that needs to be specified in the formatted
	 * output.
	 */
	final private String _docType;
	/**
	 * When not null, the root tag that needs to be specified in the formatted
	 * output.
	 */
	final private String _rootTag;

	/** Generates a new document and a new serializer. */
	public LsFormatter() {
		/** Both of these are always null for our applications. */
		_docType = null;
		_rootTag = null;
		DOMImplementationRegistry domImplementationRegistry = null;
		try {
			domImplementationRegistry = DOMImplementationRegistry.newInstance();
		} catch (final ClassNotFoundException | InstantiationException | IllegalAccessException
				| ClassCastException e) {
		}
		if (domImplementationRegistry == null) {
			_document = null;
			_domImplementationLs = null;
			_lsSerializer = null;
			return;
		}
		_domImplementationLs = (DOMImplementationLS) domImplementationRegistry.getDOMImplementation("LS");
		_lsSerializer = _domImplementationLs.createLSSerializer();
		final DOMConfiguration domConfiguration = _lsSerializer.getDomConfig();
		domConfiguration.setParameter("format-pretty-print", true);
		DocumentBuilder documentBuilder = null;
		try {
			documentBuilder = getBuilder();
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(343);
		}
		_document = documentBuilder.newDocument();
		if (Constants0.Functions.returnTrue()) {
		} else {
			_document.setXmlStandalone(true);
		}
	}

	/** Shortcut function to create a new element with the prescribed tag. */
	public Element newElement(final String tag) {
		return _document.createElement(tag);
	}

	/**
	 * Adds a newly created child with the indicated tag to the element passed as
	 * first argument.
	 */
	public Element newChild(final Element element, final String tag) {
		final Element child = newElement(tag);
		element.appendChild(child);
		return child;
	}

	/** Adds text as a child node of the element passed as argument. */
	public Node newText(final Element element, final String text) {
		final Text textNode = _document.createTextNode(text);
		element.appendChild(textNode);
		return textNode;
	}

	/** Adds a comment as a child node of the element passed as argument. */
	public void newComment(final Element element, final String comment) {
		element.appendChild(_document.createComment(comment));
	}

	/** Generates a String containing the formatted version of the element. */
	public String dump(final Element element) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			dumpWithTimeComment(element, baos);
			final byte[] byteArray = baos.toByteArray();
			final String s = new String(byteArray, LsFormatter._CharsetName);
			return s;
		} catch (final Exception e) {
			System.exit(187);
		}
		return null;
	}

	/** Writes a formatted version of the element into an output stream. */
	private void dumpWithTimeCommentChoice(final Element element, final boolean addTimeComment, final OutputStream os)
			throws Exception {
		synchronized (getClass()) {
			try (final PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, _CharsetName))) {
				if (_rootTag != null && _docType != null) {
					pw.print("<!DOCTYPE ");
					pw.print(_rootTag);
					pw.print(" SYSTEM \"");
					pw.print(_docType);
					pw.println("\">");
				}
				Comment comment = null;
				if (addTimeComment) {
					final Calendar calendar = Calendar.getInstance();
					final SimpleDateFormat format = new SimpleDateFormat();
					final String commentString = "Generation time: " + format.format(calendar.getTime());
					comment = element.getOwnerDocument().createComment(commentString);
					element.insertBefore(comment, element.getFirstChild());
				}
				final LSOutput lsOutput = _domImplementationLs.createLSOutput();
				lsOutput.setEncoding(_CharsetName);
				lsOutput.setByteStream(os);
				_lsSerializer.write(element, lsOutput);
				if (comment != null) {
					element.removeChild(comment);
				}
			} catch (final Exception e) {
				throw e;
			}
		}
	}

	public void dumpWithTimeComment(final Element element, final OutputStream os) throws Exception {
		dumpWithTimeCommentChoice(element, /* addTimeComment= */true, os);
	}

	public void dumpWithNoTimeComment(final Element element, final OutputStream os) throws Exception {
		dumpWithTimeCommentChoice(element, /* addTimeComment= */false, os);
	}

	/**
	 * Creates a copy of a DOM element, stripped of its comment and text nodes.
	 */
	private Element copyTagsAndAttributes(final Element source) {
		final Element copy = _document.createElement(source.getTagName());
		final MyElementIterator childIterator = new MyElementIterator(source);
		while (childIterator.hasNextElement()) {
			final Element child = childIterator.nextElement();
			copy.appendChild(copyTagsAndAttributes(child));
		}
		final int numberOfAttributes = source.getAttributes().getLength();
		for (int index = 0; index < numberOfAttributes; ++index) {
			final Node node = source.getAttributes().item(index);
			final String name = node.getNodeName();
			final String value = node.getNodeValue();
			copy.setAttribute(name, value);
		}
		return copy;
	}

	public Element writeAsPoint(final Element parent, final double[] latLngArray, final String dmString) {
		final Element element = newChild(parent, "POINT");
		final double lat = latLngArray[0];
		final double lng = latLngArray[1];
		element.setAttribute("lat", StandardFormatForLatOrLng(lat));
		element.setAttribute("lng", StandardFormatForLatOrLng(lng));
		if (dmString != null) {
			element.setAttribute("latLng", dmString);
		}
		return element;
	}

	public static void appendTextNode(final Element element, final String nodeName, final String text) {
		final Document document = element.getOwnerDocument();
		final Element nodeElement = document.createElement(nodeName);
		nodeElement.appendChild(document.createTextNode(text));
		element.appendChild(nodeElement);
	}

	public static String StandardFormat(final boolean b) {
		final String s = String.valueOf(b);
		return s;
	}

	public static String StandardFormat(final double d) {
		return StandardFormat(d, _NDigitsForStandardDouble);
	}

	public static String StandardFormatForLatOrLng(final double latOrLng) {
		if (!(-180d <= latOrLng && latOrLng < 180d))
			return String.format("%.2g", latOrLng >= 0d ? 1.0e20 : -1.0e20);
		return StandardFormat(latOrLng, _NDigitsForLatorLng);
	}

	private static String StandardFormat(final double d, final int nDigitsInitial) {
		final double absD = Math.abs(d);
		if (!(absD < Long.MAX_VALUE))
			return String.format("%.2g", d >= 0d ? 1.0e20 : -1.0e20);
		if (absD >= 1d) {
			final long intPart = Math.round(Math.floor(absD));
			final String intPartString = String.format("%s%d", d < 0 ? "-" : "", intPart);
			final double fracPart = absD - intPart;
			if (!(0d <= fracPart && fracPart < 1d))
				/** Something is gummed up. Must be a huge number. */
				return String.format("%.2g", d >= 0d ? 1.0e20 : -1.0e20);
			if (fracPart <= 0d)
				return intPartString;
			final String fracPartStringX = StandardFormat(fracPart);
			final int dot = fracPartStringX.indexOf('.');
			if (dot >= fracPartStringX.length() - 1)
				return intPartString;
			final String fracPartString = fracPartStringX.substring(dot + 1);
			final String s = String.format("%s.%s", intPartString, fracPartString);
			return s;
		}
		for (int nDigits = nDigitsInitial;;) {
			final BigDecimal bigDecimal1 = new BigDecimal(d, new MathContext(nDigits, RoundingMode.HALF_UP));
			final BigDecimal bigDecimal2 = bigDecimal1.stripTrailingZeros();
			final char[] patternChars = new char[2 + nDigits];
			patternChars[0] = '0';
			patternChars[1] = '.';
			for (int k = 0; k < nDigits; ++k) {
				patternChars[2 + k] = '#';
			}
			final String pattern = new String(patternChars);
			final DecimalFormat decimalFormat = new DecimalFormat(pattern);
			final String s1 = decimalFormat.format(bigDecimal2);
			final int dot = s1.indexOf('.');
			if (dot < 0)
				return s1;
			final int k0 = s1.indexOf("0000000");
			if (k0 > dot) {
				nDigits = k0 - dot;
				continue;
			}
			final int k9 = s1.indexOf("9999999");
			if (k9 > dot) {
				nDigits = k9 - dot + 1;
				continue;
			}
			return s1;
		}
	}

	final private static double _Eps = 1.0e-7;
	final public static String _BadNumberString = "--";
	final private static String _BadPercentString = _BadNumberString + '%';
	final private static String _MagicFormat = "%.0f%%";
	final private static String _ZeroString = String.format(_MagicFormat, 0d);
	final private static String _SmallString = "<0.5%";
	final private static String _BigString = ">99.5%";
	final private static String _OneHunderedString = String.format(_MagicFormat, 100d);

	public static String StandardPercentFormat(final double[] numDen) {
		final double num = numDen[0], den = numDen[1];
		/** Check for bad. */
		if (!(0d <= num && num < Double.MAX_VALUE) || !(0d < den && den < Double.MAX_VALUE))
			return _BadPercentString;
		/**
		 * num is at least 0, den is bigger than 0, and both are of reasonable size.
		 */
		final double q = (100d * num) / den;
		if (!(0d <= q && q <= 100d + _Eps))
			/** It's bad. */
			return _BadPercentString;
		else if (num == 0d)
			/** Exactly 0. */
			return _ZeroString;
		else if (q >= 100d - _Eps)
			/** "Exactly 100." */
			return _OneHunderedString;
		else {
			final String s = String.format(_MagicFormat, q);
			if (s.equals(_ZeroString))
				/** Not 0, but looks like 0. */
				return _SmallString;
			else if (s.equals(_OneHunderedString))
				/** Not 100, but looks like 100. */
				return _BigString;
			else
				return s;
		}
	}

	public static DocumentBuilder getBuilder() {
		final javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		try {
			return factory.newDocumentBuilder();
		} catch (final javax.xml.parsers.ParserConfigurationException ignored) {
			return null;
		}
	}

	public static double zeroOutBad(final double d) {
		return Double.isFinite(d) ? d : 0d;
	}

	/** The parser factory. */
	final private static File _UserDir = DirsTracker.getUserDir();

	public static Document buildDocumentFromString(final MyLogger logger, final String elementString) {
		try {
			final byte[] textBytes = elementString.getBytes(_CharsetName);
			final Document document = _DocumentBuilder.parse(new ByteArrayInputStream(textBytes));
			return document;
		} catch (final SAXException | IOException e) {
			logger.err(StringUtilities.getStackTraceString(e));
		}
		return null;
	}

	/** Main programs and their utility routines. */
	private static Document buildSampleDom() throws ParserConfigurationException {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder builder = factory.newDocumentBuilder();
		final Document document = builder.newDocument();
		document.setXmlStandalone(true);
		final Element rootElement = document.createElement("root");
		final Element element1 = document.createElement("exampleElement");
		element1.setAttribute("exampleAttribute", "attrValue");
		final Node node1 = document.createTextNode("ExampleTextNode");
		element1.appendChild(node1);
		rootElement.appendChild(element1);
		final Element element2 = document.createElement("exampleElement");
		element2.setAttribute("exampleAttribute", "attrValue");
		final Node node2 = document.createTextNode("ExampleTextNode2");
		element2.appendChild(node2);
		rootElement.appendChild(element2);
		document.appendChild(rootElement);
		return document;
	}

	public static void main(final String[] args) {
		final String elementString = """
				    <COMP_OBJECT_TYPE id="536434" isActive="yes">\r
				        <SENSOR Type="Lateral" SubType="InverseCube"\r
				        sw="1.2435 NM"\r
				        ApplyToCrossLegs="true"\r
				        UpCreepMinRange="0.0 NM"\r
				        UpCreepMaxRange="100.0 NM"\r
				        UpCreepMinLookAngle="0.0 degs"\r
				        UpCreepMaxLookAngle="180.0 degs"\r
				        DownCreepMinRange="0.0 NM"\r
				        DownCreepMaxRange="100.0 NM"\r
				        DownCreepMinLookAngle="0.0 degs"\r
				        DownCreepMaxLookAngle="180.0 degs" />\r
				    </COMP_OBJECT_TYPE> \r
				""";
		final MyLogger _Logger = MyLogger.CreateMyLogger("Lsf", _UserDir, "LsLogDgb", "LsLogOut", "LsLogWrn",
				"LsLogErr", "txt", /* append= */false);
		final Document document = buildDocumentFromString(_Logger, elementString);
		final LsFormatter formatter = new LsFormatter();

		final File xmlFile = new File(".", "foo.xml");
		try (final FileOutputStream fos = new FileOutputStream(xmlFile)) {
			formatter.dumpWithTimeComment(document.getDocumentElement(), fos);
		} catch (final Exception e) {
		}
	}

	/**
	 * Main program that reads in an XML file and prints it out formatted and
	 * stripped of all texts and comments.
	 *
	 * @param arguments the names of the files to be processed.
	 */
	public static void main1(final String[] arguments) {
		for (final String fileName : arguments) {
			Document document = null;
			try (final FileInputStream fileInputStream = new FileInputStream(fileName)) {
				document = getBuilder().parse(fileInputStream);
			} catch (final SAXException | IOException e) {
				e.printStackTrace();
			}
			final File f = new File(fileName);
			final String fullPathName = fileName.substring(0, fileName.length() - 4) + "Orig.xml";
			f.renameTo(new File(fullPathName));
			final Element element = document.getDocumentElement();
			final LsFormatter lsFormatter = new LsFormatter();
			final Element cloneElement = lsFormatter.copyTagsAndAttributes(element);
			try (final FileOutputStream fos = new FileOutputStream(fileName)) {
				lsFormatter.dumpWithTimeComment(cloneElement, fos);
			} catch (final Exception e) {
			}
		}
	}

	public static void main2(final String[] args) {
		final FilenameFilter fileNameFilter = (dummy, name) -> {
			final int nameLength = name == null ? 0 : name.length();
			if ((nameLength < 4) || (name.lastIndexOf('.') != nameLength - 4))
				return false;
			String myEnding = name.substring(nameLength - 4);
			if (myEnding.compareToIgnoreCase(".xml") != 0)
				return false;
			/** Skip the ones that are already "pretty." */
			final String superSuffix = "_pretty.xml";
			final int superSuffixLength = superSuffix.length();
			if (nameLength >= superSuffixLength) {
				myEnding = name.substring(nameLength - superSuffixLength);
				if (myEnding.compareToIgnoreCase(superSuffix) == 0)
					return false;
			}
			return true;
		};
		final ArrayList<String> allNames = new ArrayList<>();
		for (final String fileName : args) {
			final File file = new File(fileName);
			if (file.isDirectory()) {
				final String[] names = file.list(fileNameFilter);
				for (final String name : names) {
					final File file2 = new File(file, name);
					allNames.add(StringUtilities.getCanonicalPath(file2));
				}
			} else {
				if (fileNameFilter.accept(null, fileName)) {
					allNames.add(fileName);
				}
			}
		}
		final String[] allFileNames = allNames.toArray(new String[allNames.size()]);
		main1(allFileNames);
	}

	public static void main3(final String[] arguments) {
		try {
			final Document document = buildSampleDom();
			final LsFormatter lsFormatter = new LsFormatter();
			final Element documentElement = document.getDocumentElement();
			try (final FileOutputStream fos = new FileOutputStream("foo.xml")) {
				lsFormatter.dumpWithTimeComment(documentElement, fos);
			} catch (final Exception e) {
			}
		} catch (final ParserConfigurationException e) {
		}
	}

	public static void main4(final String[] arguments) {
		final double[][] numDens = { new double[] { 0.00000005 - Double.MIN_VALUE, 0.001 }, //
				new double[] { 5d, 5d + 0.000001 }, //
				new double[] { 5d, 5d + 0.0000000001 }, //
				new double[] { 0d, Double.MIN_VALUE }, //
				new double[] { Double.MIN_VALUE, Double.MAX_VALUE }, //
				new double[] { 5d, 5d }, //
				new double[] { 0d, 5d }, //
		};
		final int n = numDens.length;
		for (int k = 0; k < n; ++k) {
			final double num = numDens[k][0];
			final double den = numDens[k][1];
			System.out.printf("\n(num,den-num)=(%.5g,%.5g): %s", num, den - num, StandardPercentFormat(numDens[k]));
		}
	}
}
