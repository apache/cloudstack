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
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * @author chiradeep
 *
 */
public class LibvirtCapXMLParser extends LibvirtXMLParser {
	private boolean _host = false;
	private boolean _guest = false;
	private boolean _osType = false;
	private boolean _domainTypeKVM = false;
	private boolean _emulatorFlag = false;
	private final StringBuffer _emulator = new StringBuffer() ;
	private final StringBuffer _capXML = new StringBuffer();
    private static final Logger s_logger = Logger.getLogger(LibvirtCapXMLParser.class);
    private final ArrayList<String> guestOsTypes = new ArrayList<String>();
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if(qName.equalsIgnoreCase("host")) {
			_host = false;
		} else if (qName.equalsIgnoreCase("os_type")) {
			_osType = false;
		} else if (qName.equalsIgnoreCase("guest")) {
			_guest = false;
		} else if (qName.equalsIgnoreCase("domain")) {
			_domainTypeKVM = false;
		} else if (qName.equalsIgnoreCase("emulator")) {
			_emulatorFlag = false;

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
			_emulator.append(ch, start, length);
		}
	}


	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if(qName.equalsIgnoreCase("host")) {
			_host = true;
		} else if (qName.equalsIgnoreCase("guest")) {
			_guest = true;
		} else if (qName.equalsIgnoreCase("os_type")) {
			if (_guest) {
				_osType = true;
			}
		} else if (qName.equalsIgnoreCase("domain")) {
			for (int i = 0; i < attributes.getLength(); i++) {
				if (attributes.getQName(i).equalsIgnoreCase("type")
					&&	attributes.getValue(i).equalsIgnoreCase("kvm")) {
					_domainTypeKVM = true;
				}
			}
		} else if (qName.equalsIgnoreCase("emulator") && _domainTypeKVM) {
			_emulatorFlag = true;
			_emulator.delete(0, _emulator.length());
		} else if (_host) {
			_capXML.append("<").append(qName);
			for (int i=0; i < attributes.getLength(); i++) {
				_capXML.append(" ").append(attributes.getQName(i)).append("=").append(attributes.getValue(i));
			}
			_capXML.append(">");
		}

	}

	public String parseCapabilitiesXML(String capXML) {
		if (!_initialized){
			return null;
		}
		try {
			_sp.parse(new InputSource(new StringReader(capXML)), this);
			return _capXML.toString();
		} catch(SAXException se) {
			s_logger.warn(se.getMessage());
		} catch (IOException ie) {
			s_logger.error(ie.getMessage());
		}
		return null;
	}

	public ArrayList<String> getGuestOsType() {
		return guestOsTypes;
	}

	public String getEmulator() {
		return _emulator.toString();
	}

	public static void main(String [] args) {
		String capXML = "<capabilities>"+
		"  <host>"+
		"    <cpu>"+
		"      <arch>x86_64</arch>"+
		"      <model>core2duo</model>"+
		"      <topology sockets='1' cores='2' threads='1'/>"+
		"      <feature name='lahf_lm'/>"+
		"      <feature name='xtpr'/>"+
		"      <feature name='cx16'/>"+
		"      <feature name='tm2'/>"+
		"      <feature name='est'/>"+
		"      <feature name='vmx'/>"+
		"      <feature name='ds_cpl'/>"+
		"      <feature name='pbe'/>"+
		"      <feature name='tm'/>"+
		"      <feature name='ht'/>"+
		"      <feature name='ss'/>"+
		"      <feature name='acpi'/>"+
		"      <feature name='ds'/>"+
		"    </cpu>"+
		"    <migration_features>"+
		"      <live/>"+
		"      <uri_transports>"+
		"        <uri_transport>tcp</uri_transport>"+
		"      </uri_transports>"+
		"    </migration_features>"+
		"    <topology>"+
		"      <cells num='1'>"+
		"        <cell id='0'>"+
		"          <cpus num='2'>"+
		"            <cpu id='0'/>"+
		"            <cpu id='1'/>"+
		"          </cpus>"+
		"        </cell>"+
		"      </cells>"+
		"    </topology>"+
		"  </host>"+
		""+
		"  <guest>"+
		"    <os_type>hvm</os_type>"+
		"    <arch name='i686'>"+
		"      <wordsize>32</wordsize>"+
		"      <emulator>/usr/bin/qemu</emulator>"+
		"      <machine>pc-0.11</machine>"+
		"      <machine canonical='pc-0.11'>pc</machine>"+
		"      <machine>pc-0.10</machine>"+
		"      <machine>isapc</machine>"+
		"      <domain type='qemu'>"+
		"      </domain>"+
		"      <domain type='kvm'>"+
		"        <emulator>/usr/bin/qemu-kvm</emulator>"+
		"        <machine>pc-0.11</machine>"+
		"        <machine canonical='pc-0.11'>pc</machine>"+
		"        <machine>pc-0.10</machine>"+
		"        <machine>isapc</machine>"+
		"      </domain>"+
		"    </arch>"+
		"    <features>"+
		"      <cpuselection/>"+
		"      <pae/>"+
		"      <nonpae/>"+
		"      <acpi default='on' toggle='yes'/>"+
		"      <apic default='on' toggle='no'/>"+
		"    </features>"+
		"  </guest>"+
		"  <guest>"+
		"    <os_type>hvm</os_type>"+
		"    <arch name='x86_64'>"+
		"      <wordsize>64</wordsize>"+
		"      <emulator>/usr/bin/qemu-system-x86_64</emulator>"+
		"      <machine>pc-0.11</machine>"+
		"      <machine canonical='pc-0.11'>pc</machine>"+
		"      <machine>pc-0.10</machine>"+
		"      <machine>isapc</machine>"+
		"      <domain type='qemu'>"+
		"      </domain>"+
		"      <domain type='kvm'>"+
		"        <emulator>/usr/bin/qemu-kvm</emulator>"+
		"        <machine>pc-0.11</machine>"+
		"        <machine canonical='pc-0.11'>pc</machine>"+
		"        <machine>pc-0.10</machine>"+
		"        <machine>isapc</machine>"+
		"      </domain>"+
		"    </arch>"+
		"    <features>"+
		"      <cpuselection/>"+
		"      <acpi default='on' toggle='yes'/>"+
		"      <apic default='on' toggle='no'/>"+
		"    </features>"+
		"  </guest>"+
		"</capabilities>";

		LibvirtCapXMLParser parser = new LibvirtCapXMLParser();
		String cap = parser.parseCapabilitiesXML(capXML);
		System.out.println(parser.getGuestOsType());
		System.out.println(parser.getEmulator());
	}
}
