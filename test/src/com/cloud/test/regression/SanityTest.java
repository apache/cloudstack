/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.test.regression;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.log4j.Logger;

public class SanityTest extends TestCase{

	public static final Logger s_logger = Logger.getLogger(SanityTest.class.getName());
	
	public SanityTest(){
		this.setClient();
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
			
			//verify the response parameters
			if ((api.getResponseCode() !=200 ) && (api.getRequired() == true) )
			{
				s_logger.error("Exiting the test....Command " + api.getName() + " required for the future run, failed with an error code " + api.getResponseCode() + ". Command was sent with the url " + api.getUrl());
				return false;
			}
			else if (api.getResponseCode() != 200) {
				error++;
				s_logger.error("Test " + api.getTestCaseInfo() + " failed with an error code " + api.getResponseCode() + " . Command was sent with url  " + api.getUrl());
			}
			else {
				//set parameters for the future use
				if (api.setParam(this.getParam()) == false) {	
					s_logger.error("Exiting the test...Command " + api.getName() + " didn't return parameters needed for the future use. Command was sent with url " + api.getUrl());
					return false;
				}
				
				//verify parameters
				if (api.verifyParam() == false)
				{
					s_logger.error("Test " + api.getTestCaseInfo() + " failed. Verification for returned parameters failed. The command was sent with url " + api.getUrl());
					error++;
				}
				else if (api.getTestCaseInfo() != null)
				{
					s_logger.info("Test " + api.getTestCaseInfo() + " passed");
				}
			}
		}
		
		//verify event	
		boolean eventResult = ApiCommand.verifyEvents("../metadata/func/regression_events.properties", "INFO", "http://" + this.getParam().get("hostip") + ":8096", this.getParam().get("accountname"));
		s_logger.info("listEvent command verification result is  " + eventResult);
		
		if (error != 0)
			return false;
		else
			return true;
	}
}
