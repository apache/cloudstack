/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.test.ui;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import junit.framework.TestSuite;
import junit.framework.Test;
import junit.framework.JUnit4TestAdapter;
import org.openqa.selenium.server.SeleniumServer;
import org.openqa.selenium.server.RemoteControlConfiguration;
import org.junit.runners.Suite;

import com.thoughtworks.selenium.DefaultSelenium;

@RunWith(JUnit4.class)
public abstract class AbstractSeleniumTestCase {
	protected static DefaultSelenium selenium;
	private static SeleniumServer seleniumServer;

	@BeforeClass
	public static void setUp() throws Exception {
		System.out.println("*** Starting selenium ... ***");
		RemoteControlConfiguration seleniumConfig = new RemoteControlConfiguration();
		seleniumConfig.setPort(4444);
		seleniumServer = new SeleniumServer(seleniumConfig);
		seleniumServer.start();

		String host = System.getProperty("myParam", "localhost");
		selenium = createSeleniumClient("http://" + host + ":" + "8080/client/");
		selenium.start();
		System.out.println("*** Started selenium ***");
	}

	@AfterClass
	public static void tearDown() throws Exception {
		selenium.stop();
	}

	protected static DefaultSelenium createSeleniumClient(String url)
			throws Exception {
		return new DefaultSelenium("localhost", 4444, "*firefox", url);
	}
}
