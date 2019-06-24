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
package com.cloud.test.ui;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.openqa.selenium.server.RemoteControlConfiguration;
import org.openqa.selenium.server.SeleniumServer;

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

    protected static DefaultSelenium createSeleniumClient(String url) throws Exception {
        return new DefaultSelenium("localhost", 4444, "*firefox", url);
    }
}
