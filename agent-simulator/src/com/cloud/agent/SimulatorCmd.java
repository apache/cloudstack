/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.agent;

import java.io.Serializable;

public class SimulatorCmd implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String testCase = "DEFAULT";
	
	public SimulatorCmd(String testCase) {
		this.testCase = testCase; 
	}
	
	public String getTestCase() {
		return testCase;
	}
	
	public void setTestCase(String testCase) {
		this.testCase = testCase;
	}
}
