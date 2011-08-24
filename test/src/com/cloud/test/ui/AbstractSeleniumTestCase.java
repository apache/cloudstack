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
