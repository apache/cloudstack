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
import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class LibvirtCapXMLParser extends LibvirtXMLParser {
    private boolean _host = false;
    private boolean _guest = false;
    private boolean _osType = false;
    private boolean _domainTypeKVM = false;
    private boolean _emulatorFlag = false;
    private boolean _archTypex8664 = false;
    private final StringBuffer _emulator = new StringBuffer();
    private final StringBuffer _capXML = new StringBuffer();
    private final ArrayList<String> guestOsTypes = new ArrayList<String>();

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase("host")) {
            _host = false;
        } else if (qName.equalsIgnoreCase("os_type")) {
            _osType = false;
        } else if (qName.equalsIgnoreCase("guest")) {
            _guest = false;
        } else if (qName.equalsIgnoreCase("domain")) {
            _domainTypeKVM = false;
        } else if (qName.equalsIgnoreCase("emulator")) {
            _emulatorFlag = false;
        } else if (qName.equalsIgnoreCase("arch")) {
            _archTypex8664 = false;
        } else if (_host) {
            _capXML.append("<").append("/").append(qName).append(">");
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (_host) {
            _capXML.append(ch, start, length);
        } else if (_osType) {
            guestOsTypes.add(new String(ch, start, length));
        } else if (_emulatorFlag) {
            logger.debug("Found " + new String(ch, start, length) + " as a suiteable emulator");
            _emulator.append(ch, start, length);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("host")) {
            _host = true;
        } else if (qName.equalsIgnoreCase("guest")) {
            _guest = true;
        } else if (qName.equalsIgnoreCase("os_type")) {
            if (_guest) {
                _osType = true;
            }
        } else if (qName.equalsIgnoreCase("arch")) {
            for (int i = 0; i < attributes.getLength(); i++) {
                if (attributes.getQName(i).equalsIgnoreCase("name") &&
                        (attributes.getValue(i).equalsIgnoreCase("x86_64") || attributes.getValue(i).equalsIgnoreCase("aarch64"))) {
                    _archTypex8664 = true;
                }
            }
        } else if (qName.equalsIgnoreCase("domain")) {
            for (int i = 0; i < attributes.getLength(); i++) {
                if (attributes.getQName(i).equalsIgnoreCase("type") && attributes.getValue(i).equalsIgnoreCase("kvm")) {
                    _domainTypeKVM = true;
                }
            }
        } else if (qName.equalsIgnoreCase("emulator") && _domainTypeKVM && _archTypex8664) {
            _emulatorFlag = true;
            _emulator.delete(0, _emulator.length());
        } else if (_host) {
            _capXML.append("<").append(qName);
            for (int i = 0; i < attributes.getLength(); i++) {
                _capXML.append(" ").append(attributes.getQName(i)).append("=").append(attributes.getValue(i));
            }
            _capXML.append(">");
        }

    }

    public String parseCapabilitiesXML(String capXML) {
        if (!_initialized) {
            return null;
        }
        try {
            _sp.parse(new InputSource(new StringReader(capXML)), this);
            return _capXML.toString();
        } catch (SAXException se) {
            logger.warn(se.getMessage());
        } catch (IOException ie) {
            logger.error(ie.getMessage());
        }
        return null;
    }

    public ArrayList<String> getGuestOsType() {
        return guestOsTypes;
    }

    public String getEmulator() {
        return _emulator.toString();
    }

}
