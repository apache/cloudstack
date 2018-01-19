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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.thoughtworks.selenium.SeleniumException;

public class UIScenarioTest extends AbstractSeleniumTestCase {

    @Test
    public void testLoginStartStopVMScenario() throws Exception {
        try {
            selenium.open("/client/");
            selenium.type("account_username", "admin");
            selenium.type("account_password", "password");
            selenium.click("loginbutton");
            Thread.sleep(3000);
            assertTrue(selenium.isTextPresent("admin"));
            selenium.click("//div[@id='leftmenu_instances']/div");
            selenium.click("//div[@id='leftmenu_instances_stopped_instances']/div/span");

            Thread.sleep(3000);
            selenium.click("//div[@id='midmenu_startvm_link']/div/div[2]");
            selenium.click("//div[39]/div[11]/button[1]");

            for (int second = 0;; second++) {
                if (second >= 60)
                    fail("timeout");
                try {
                    if (selenium.isVisible("//div/p[@id='after_action_info']"))
                        break;
                } catch (Exception e) {
                    s_logger.info("[ignored]"
                            + "error during visibility test after start vm: " + e.getLocalizedMessage());
                }
                Thread.sleep(10000);
            }
            assertTrue(selenium.isTextPresent("Start Instance action succeeded"));
            selenium.click("//div[@id='leftmenu_instances_running_instances']/div/span");

            Thread.sleep(3000);
            selenium.click("//div[@id='midmenu_stopvm_link']/div/div[2]");
            selenium.click("//div[39]/div[11]/button[1]");
            for (int second = 0;; second++) {
                if (second >= 60)
                    fail("timeout");
                try {
                    if (selenium.isVisible("//div/p[@id='after_action_info']"))
                        break;
                } catch (Exception e) {
                    s_logger.info("[ignored]"
                            + "error during visibility test after stop vm: " + e.getLocalizedMessage());
                }
                Thread.sleep(10000);
            }

            assertTrue(selenium.isTextPresent("Stop Instance action succeeded"));
            selenium.click("main_logout");
            selenium.waitForPageToLoad("30000");
            assertTrue(selenium.isTextPresent("Welcome to Management Console"));

        } catch (SeleniumException ex) {
            fail(ex.getMessage());
            System.err.println(ex.getMessage());
            throw ex;
        }
    }
}
