package com.skagit.utils;

import java.util.Arrays;

public class NamedEntity implements Comparable<NamedEntity> {

	public final String _name;

	public NamedEntity(final String name) {
		_name = CleanWhiteSpace(name);
	}

	public String getString() {
		return "";
	}

	@Override
	final public String toString() {
		return getString();
	}

	@Override
	public int compareTo(final NamedEntity namedEntity) {
		if (namedEntity == null) {
			return -1;
		}
		final String hisName = namedEntity._name;
		if ((_name == null) != (hisName == null)) {
			return _name == null ? -1 : 1;
		}
		if (_name == null) {
			return 0;
		}
		return _name.compareTo(hisName);
	}

	public static void mainx(final String[] args) {
		final NamedEntity1[] namedEntity1s = { //
				new NamedEntity1("b", 2), //
				new NamedEntity1("a", 1), //
				new NamedEntity1("c", 3), //
		};
		Arrays.sort(namedEntity1s);
		for (int k = 0; k < namedEntity1s.length; ++k) {
			System.out.println(namedEntity1s[k].getString());
		}
		System.out.println(Arrays.binarySearch(namedEntity1s, new NamedEntity("b")));
	}

	public static String CleanWhiteSpace(final String s) {
		if (s == null) {
			return null;
		}
		String ss = StringUtilities.CleanWhiteSpace(s);
		if (ss.startsWith("\'")) {
			ss = ss.length() == 1 ? "" : ss.substring(1);
		}
		return ss.length() == s.length() ? s : ss;
	}
}

class NamedEntity1 extends NamedEntity {
	final int _abc;

	public NamedEntity1(final String name, final int abc) {
		super(name);
		_abc = abc;
	}

	@Override
	public String getString() {
		return String.format("%s %d", _name, _abc);
	}

}
