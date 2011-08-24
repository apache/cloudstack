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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Deploy extends TestCase{
	public static final Logger s_logger = Logger.getLogger(Deploy.class.getName());
	
	public Deploy(){
		this.setClient();
		this.setParam(new HashMap<String, String>());
	}

	
	public boolean executeTest() {
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
			
			
			//verify the response of the command
			if (api.getResponseCode() != 200) {
				error++;
				s_logger.error("The command " + api.getUrl() + " failed");
			}
			else {
				s_logger.info("The command " + api.getUrl() + " passsed");
			}
		}
			if (error != 0)
				return false;
			else
				return true;
	}
		
	
	public static void main (String[] args) {
		
		List<String> argsList = Arrays.asList(args);
		Iterator<String> iter = argsList.iterator();
		String host = null;
		String file = null;
		
		while (iter.hasNext()) {
			String arg = iter.next();
			// management server host
			if (arg.equals("-h")) {
				host = iter.next();
			}
			if (arg.equals("-f")) {
				file = iter.next();
			}
		}
		
		Deploy deploy = new Deploy ();
		
		ArrayList<String> inputFile = new ArrayList<String>();
		inputFile.add(file);
		deploy.setInputFile(inputFile);
		deploy.setTestCaseName("Management server deployment");
		deploy.getParam().put("hostip", host);
		deploy.getParam().put("apicommands", "../metadata/func/commands");
		deploy.setCommands();
		
		s_logger.info("Starting deployment against host " + host);
		
		boolean result = deploy.executeTest();
		if (result == false) {
     	   s_logger.error("DEPLOYMENT FAILED");
     	   System.exit(1);
        }
        else {
     	   s_logger.info("DEPLOYMENT IS SUCCESSFUL");
        }
		
	} 
	
}

