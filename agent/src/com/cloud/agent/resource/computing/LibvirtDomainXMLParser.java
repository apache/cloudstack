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

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author chiradeep
 *
 */
public class LibvirtDomainXMLParser extends LibvirtXMLParser {

	private final ArrayList<String> interfaces = new ArrayList<String>();
	private final SortedMap<String, String> diskMaps = new TreeMap<String, String>();
	
	private boolean _interface;
	private boolean _disk;
	private boolean _desc;
	private Integer vncPort;
	private String diskDev;
	private String diskFile;
	private String desc;
	
	public Integer getVncPort() {
		return vncPort;
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (_desc) {
			desc = new String(ch, start, length);
		}
	}
	
	@Override
    public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
				if(qName.equalsIgnoreCase("interface")) {
					_interface = true;
				} else if (qName.equalsIgnoreCase("target")){
					if (_interface)
						interfaces.add(attributes.getValue("dev"));
					else if (_disk)
						diskDev = attributes.getValue("dev");
				} else if (qName.equalsIgnoreCase("source")){
					if (_disk)
						diskFile = attributes.getValue("file");
				} else if (qName.equalsIgnoreCase("disk")) {
					_disk = true;
				} else if (qName.equalsIgnoreCase("graphics")) {
					String port = attributes.getValue("port");
					if (port != null) {
						try {
							vncPort = Integer.parseInt(port);
							if (vncPort != -1) {
								vncPort = vncPort - 5900;
							} else {
								vncPort = null;
							}
						}catch (NumberFormatException nfe){
							vncPort = null;
						}
					}
				} else if (qName.equalsIgnoreCase("description")) {
					_desc = true;
				}
			}

	@Override
    public void endElement(String uri, String localName, String qName)
			throws SAXException {
			
				if(qName.equalsIgnoreCase("interface")) {
					_interface = false;
				} else if (qName.equalsIgnoreCase("disk")) {
					diskMaps.put(diskDev, diskFile);
					_disk = false;
				} else if (qName.equalsIgnoreCase("description")) {
					_desc = false;
				}
				
			}

	public ArrayList<String> getInterfaces() {
		return interfaces;
	}
	
	public SortedMap<String, String> getDiskMaps() {
		return diskMaps;
	}
	
	public String getDescription() {
		return desc;
	}

	public static void main(String [] args){
		LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
		parser.parseDomainXML("<domain type='kvm' id='12'>"+
				  "<name>r-6-CV-5002-1</name>"+
				  "<uuid>581b5a4b-b496-8d4d-e44e-a7dcbe9df0b5</uuid>"+
				  "<description>testVM</description>"+
				  "<memory>131072</memory>"+
				  "<currentMemory>131072</currentMemory>"+
				  "<vcpu>1</vcpu>"+
				  "<os>"+
				    "<type arch='i686' machine='pc-0.11'>hvm</type>"+
				    "<kernel>/var/lib/libvirt/qemu/vmlinuz-2.6.31.6-166.fc12.i686</kernel>"+
				    "<cmdline>ro root=/dev/sda1 acpi=force selinux=0 eth0ip=10.1.1.1 eth0mask=255.255.255.0 eth2ip=192.168.10.152 eth2mask=255.255.255.0 gateway=192.168.10.1 dns1=72.52.126.11 dns2=72.52.126.12 domain=v4.myvm.com</cmdline>"+
				    "<boot dev='hd'/>"+
				  "</os>"+
				  "<features>"+
				    "<acpi/>"+
				    "<pae/>"+
				  "</features>"+
				  "<clock offset='utc'/>"+
				  "<on_poweroff>destroy</on_poweroff>"+
				  "<on_reboot>restart</on_reboot>"+
				  "<on_crash>destroy</on_crash>"+
				  "<devices>"+
				    "<emulator>/usr/bin/qemu-kvm</emulator>"+
				    "<disk type='file' device='disk'>"+
				      "<source file='/mnt/tank//vmops/CV/vm/u000004/r000006/rootdisk'/>"+
				      "<target dev='hda' bus='ide'/>"+
				    "</disk>"+
				    "<interface type='bridge'>"+
				      "<mac address='02:00:50:02:00:01'/>"+
				      "<source bridge='vnbr5002'/>"+
				      "<target dev='vtap5002'/>"+
				      "<model type='e1000'/>"+
				    "</interface>"+
				    "<interface type='network'>"+
				      "<mac address='00:16:3e:77:e2:a1'/>"+
				      "<source network='vmops-private'/>"+
				      "<target dev='vnet3'/>"+
				      "<model type='e1000'/>"+
				    "</interface>"+
				    "<interface type='bridge'>"+
				      "<mac address='06:85:00:00:00:04'/>"+
				      "<source bridge='br0'/>"+
				      "<target dev='tap5002'/>"+
				      "<model type='e1000'/>"+
				    "</interface>"+
				    "<input type='mouse' bus='ps2'/>"+
				    "<graphics type='vnc' port='6031' autoport='no' listen=''/>"+
				    "<video>"+
				      "<model type='cirrus' vram='9216' heads='1'/>"+
				    "</video>"+
				  "</devices>"+
				"</domain>"

		);
		for (String intf: parser.getInterfaces()){
			System.out.println(intf);
		}
		System.out.println(parser.getVncPort());
		System.out.println(parser.getDescription());
	}

}
