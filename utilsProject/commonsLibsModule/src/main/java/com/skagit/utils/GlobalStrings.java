package com.skagit.utils;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

public class GlobalStrings {
    final private Map<String, String> _theMap;

    public GlobalStrings(final String bundleName) {
	final ResourceBundle resourceBundle = ResourceBundle.getBundle(bundleName);
	_theMap = new HashMap<>();
	final Enumeration<String> keys = resourceBundle.getKeys();
	while (keys.hasMoreElements()) {
	    final String key = keys.nextElement();
	    final String value = resourceBundle.getString(key);
	    _theMap.put(key, value);
	}
    }

    public String getString(final String key) {
	try {
	    return _theMap.get(key);
	} catch (final MissingResourceException e) {
	    return null;
	}
    }

    public Set<String> getAllKeys() {
	return _theMap.keySet();
    }

}
