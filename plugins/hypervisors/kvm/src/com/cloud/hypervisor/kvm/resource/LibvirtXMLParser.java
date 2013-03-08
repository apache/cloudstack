// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.hypervisor.kvm.resource;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
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
        } catch (ParserConfigurationException e) {
            s_logger.trace("Ignoring xml parser error.", e);
        } catch (SAXException e) {
            s_logger.trace("Ignoring xml parser error.", e);
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
