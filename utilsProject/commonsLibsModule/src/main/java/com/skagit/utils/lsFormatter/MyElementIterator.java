package com.skagit.utils.lsFormatter;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MyElementIterator {
	final private int _numberOfChildren;
	private int _childIdx;
	final private NodeList _childrenList;
	private Node _currentNode;

	public MyElementIterator(final Node node) {
		_childrenList = node.getChildNodes();
		_numberOfChildren = _childrenList.getLength();
		_childIdx = 0;
		_currentNode = null;
		hasNextElement();
	}

	public boolean hasNextElement() {
		while (_childIdx < _numberOfChildren) {
			final Node childNode = _childrenList.item(_childIdx);
			final short nodeType = childNode.getNodeType();
			if (nodeType != Node.ELEMENT_NODE) {
				++_childIdx;
			} else {
				_currentNode = childNode;
				return true;
			}
		}
		return (_childIdx < _numberOfChildren);
	}

	public Element nextElement() {
		if (_currentNode == null) {
			hasNextElement();
		}
		final Element element = (Element) _currentNode;
		_currentNode = null;
		_childIdx++;
		return element;
	}

	public static Element getChildIgnoreCase(final Element element, final String childTagName) {
		final String childTagNameLc = childTagName.toLowerCase();
		final MyElementIterator iterator = new MyElementIterator(element);
		while (iterator.hasNextElement()) {
			final Element child = iterator.nextElement();
			if (child.getTagName().toLowerCase().equals(childTagNameLc))
				return child;
		}
		return null;
	}

	public static List<Element> getDescendantChildrenElementsWithTag(final Element element, final String tagName,
			final boolean includeSelf) {
		final ArrayList<Element> matchingElements = new ArrayList<>();
		if (includeSelf && element.getTagName().equals(tagName)) {
			matchingElements.add(element);
		}
		final NodeList relevantNodes = element.getElementsByTagName(tagName);
		final int nNodes = relevantNodes.getLength();
		for (int k = 0; k < nNodes; k++) {
			final Node node = relevantNodes.item(k);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				matchingElements.add((Element) node);
			}
		}
		return matchingElements;
	}
}
