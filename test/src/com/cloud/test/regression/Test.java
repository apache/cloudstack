/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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

package com.cloud.test.regression;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Test extends TestCase{
	public static final Logger s_logger = Logger.getLogger(Test.class.getName());
	
	public Test(){
		this.setClient();
		this.setParam(new HashMap<String, String>());
	}
	
	public boolean executeTest(){
		
		int error=0;	
		Element rootElement = this.getInputFile().get(0).getDocumentElement();
		NodeList commandLst = rootElement.getElementsByTagName("command");
		
		//Analyze each command, send request and build the array list of api commands
		for (int i=0; i<commandLst.getLength(); i++) {
			Node fstNode = commandLst.item(i);
		    Element fstElmnt = (Element) fstNode;
		    
		    //new command
			ApiCommand api = new ApiCommand(fstElmnt, this.getParam(), this.getCommands());
			
			//send a command
			api.sendCommand(this.getClient(), null);
			
		}
		
		//Try to create portForwarding rule for all available private/public ports
		ArrayList<String> port = new ArrayList<String>();
		for (int j=1; j<1000; j++){
			port.add(Integer.toString(j));
		}
		
		//try all public ports	
		for (String portValue : port) {
	        try {
	        	s_logger.info("public port is " + portValue);
	        	String url = "http://" + this.getParam().get("hostip") + ":8096/?command=createNetworkRule&publicPort=" + portValue + "&privatePort=22&protocol=tcp&isForward=true&securityGroupId=1&account=admin";
	        	HttpClient client = new HttpClient();
				HttpMethod method = new GetMethod(url);
				int responseCode = client.executeMethod(method);
				if (responseCode != 200 ) {
					error++;
					s_logger.error("Can't create portForwarding network rule for the public port " + portValue + ". Request was sent with url " + url);
				}	
	        }catch (Exception ex) {
	        	s_logger.error(ex);
	        }
		}
		
		
			if (error != 0)
				return false;
			else
				return true;
	}
}
