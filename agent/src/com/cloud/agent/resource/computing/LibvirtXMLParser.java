/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.cloud.agent.resource.computing;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class LibvirtXMLParser extends DefaultHandler {
	private static final Logger s_logger = Logger
			.getLogger(LibvirtXMLParser.class);
	protected static SAXParserFactory s_spf;

	static {
		s_spf = SAXParserFactory.newInstance();

	}
	protected SAXParser _sp;
	protected boolean _initialized = false;

	public LibvirtXMLParser() {

		try {
			_sp = s_spf.newSAXParser();
			_initialized = true;
		} catch (Exception ex) {
		}

	}

	public boolean parseDomainXML(String domXML) {
		if (!_initialized) {
			return false;
		}
		try {
			_sp.parse(new InputSource(new StringReader(domXML)), this);
			return true;
		} catch (SAXException se) {
			s_logger.warn(se.getMessage());
		} catch (IOException ie) {
			s_logger.error(ie.getMessage());
		}
		return false;
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
	}

}
