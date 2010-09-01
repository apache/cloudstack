package com.cloud.utils;

import java.util.Locale;
import java.util.ResourceBundle;

public class CloudResourceBundle {
	
	private ResourceBundle _bundle;
	
	public CloudResourceBundle(ResourceBundle bundle) {
		_bundle = bundle;
	}
	
	public static CloudResourceBundle getBundle(String baseName, Locale locale) {
	   	return new CloudResourceBundle(ResourceBundle.getBundle(baseName, locale));
	}

	private String getString(String key) {
		try {
			return _bundle.getString(key);
		} catch(Exception e) {
			return key; //if translation is not found, just return original word (i.e. English).
		}
	}
	
	public String t(String key) {
	    return getString(key);	
	}
}
