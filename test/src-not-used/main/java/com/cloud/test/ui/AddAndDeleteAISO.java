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

public class AddAndDeleteAISO extends AbstractSeleniumTestCase {

    @Test
    public void testAddAndDeleteISO() throws Exception {
        try {
            selenium.open("/client/");
            selenium.type("account_username", "admin");
            selenium.type("account_password", "password");
            selenium.click("loginbutton");
            Thread.sleep(3000);
            assertTrue(selenium.isTextPresent("admin"));
            selenium.click("//div[@id='leftmenu_templates']/div");
            selenium.click("//div[@id='leftmenu_submenu_my_iso']/div/div[2]");
            Thread.sleep(3000);
            selenium.click("label");

            selenium.type("add_iso_name", "abc");
            selenium.type("add_iso_display_text", "abc");
            String iso_url = System.getProperty("add_iso_url", "http://10.91.28.6/ISO/Fedora-11-i386-DVD.iso");
            selenium.type("add_iso_url", iso_url);
            String iso_zone = System.getProperty("add_iso_zone", "All Zones");
            selenium.select("add_iso_zone", "label=" + iso_zone);
            String iso_os_type = System.getProperty("add_iso_os_type", "Fedora 11");
            selenium.select("add_iso_os_type", "label=" + iso_os_type);
            selenium.click("//div[28]/div[11]/button[1]");
            Thread.sleep(3000);
            int i = 1;
            try {
                for (;; i++) {
                    System.out.println("i=   " + i);
                    selenium.click("//div[" + i + "]/div/div[2]/span/span");
                }
            } catch (Exception ex) {
                s_logger.info("[ignored]"
                        + "error during clicking test on iso: " + e.getLocalizedMessage());
            }

            for (int second = 0;; second++) {
                if (second >= 60)
                    fail("timeout");
                try {
                    if (selenium.isVisible("//div[@id='after_action_info_container_on_top']"))
                        break;
                } catch (Exception e) {
                    s_logger.info("[ignored]"
                            + "error during visibility test of iso: " + e.getLocalizedMessage());
                }
                Thread.sleep(10000);
            }

            assertTrue(selenium.isTextPresent("Adding succeeded"));
            Thread.sleep(3000);
            int status = 1;
            while (!selenium.isTextPresent("Ready")) {
                for (int j = 1; j <= i; j++)

                {
                    if (selenium.isTextPresent("Ready")) {
                        status = 0;
                        break;
                    }
                    selenium.click("//div[" + j + "]/div/div[2]/span/span");
                }
                if (status == 0) {
                    break;
                } else {
                    selenium.click("//div[@id='leftmenu_submenu_featured_iso']/div/div[2]");
                    Thread.sleep(3000);
                    selenium.click("//div[@id='leftmenu_submenu_my_iso']/div/div[2]");
                    Thread.sleep(3000);
                }

            }
            selenium.click("link=Delete ISO");
            selenium.click("//div[28]/div[11]/button[1]");
            for (int second = 0;; second++) {
                if (second >= 60)
                    fail("timeout");
                try {
                    if (selenium.isVisible("after_action_info_container_on_top"))
                        break;
                } catch (Exception e) {
                    s_logger.info("[ignored]"
                            + "error checking visibility after test completion for iso: " + e.getLocalizedMessage());
                }
                Thread.sleep(1000);
            }

            assertTrue(selenium.isTextPresent("Delete ISO action succeeded"));
            selenium.click("main_logout");
            selenium.waitForPageToLoad("30000");
            assertTrue(selenium.isTextPresent("Welcome to Management Console"));

        } catch (SeleniumException ex) {

            System.err.println(ex.getMessage());
            fail(ex.getMessage());

            throw ex;
        }
    }
}
