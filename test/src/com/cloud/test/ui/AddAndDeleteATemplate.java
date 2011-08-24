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

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import com.cloud.test.ui.AbstractSeleniumTestCase;
import com.thoughtworks.selenium.SeleniumException;

public class AddAndDeleteATemplate extends AbstractSeleniumTestCase {

	@Test
	public void testAddAndDeleteTemplate() throws Exception {
		try {
			selenium.open("/client/");
			selenium.type("account_username", "admin");
			selenium.type("account_password", "password");
			selenium.click("loginbutton");
			Thread.sleep(3000);
			assertTrue(selenium.isTextPresent("admin"));
			selenium.click("//div[@id='leftmenu_templates']/div");
			selenium.click("//div[@id='leftmenu_submenu_my_template']/div/div[2]");
			Thread.sleep(3000);
			selenium.click("label");
			selenium.type("add_template_name", "abc");
			selenium.type("add_template_display_text", "abc");
			String template_url = System.getProperty("add_template_url", "http://10.91.28.6/templates/centos53-x86_64/latest/f59f18fb-ae94-4f97-afd2-f84755767aca.vhd.bz2");
			selenium.type("add_template_url", template_url);
			String template_zone = System.getProperty("add_template_zone", "All Zones");	
			selenium.select("add_template_zone", "label="+template_zone);
			String template_os_type = System.getProperty("add_template_os_type", "CentOS 5.3 (32-bit)");			
			selenium.select("add_template_os_type", "label="+template_os_type);
			selenium.click("//div[28]/div[11]/button[1]");
			Thread.sleep(3000);
			int  i=1;
			try
			{
				for(;;i++)
				{
					System.out.println("i=   "+i);
					selenium.click("//div[" +i+ "]/div/div[2]/span/span");
				}
			}
			catch(Exception ex) {				
			}

			for (int second = 0;; second++) {
				if (second >= 60) fail("timeout");
				try { if (selenium.isVisible("//div[@id='after_action_info_container_on_top']")) break; } catch (Exception e) {}
				Thread.sleep(10000);
			}

			assertTrue(selenium.isTextPresent("Adding succeeded"));
			Thread.sleep(3000);
			int status=1;
			while(!selenium.isTextPresent("Ready"))
			{
				for(int j =1;j<=i;j++)

				{
					if (selenium.isTextPresent("Ready"))	
					{
						status=0;
						break;
					}
					selenium.click("//div["+j+"]/div/div[2]/span/span");
				}
				if(status==0){
					break;
				}
				else
				{
					selenium.click("//div[@id='leftmenu_submenu_featured_template']/div/div[2]");
					Thread.sleep(3000);
					selenium.click("//div[@id='leftmenu_submenu_my_template']/div/div[2]");
					Thread.sleep(3000);
				}

			}
			selenium.click("link=Delete Template");
			selenium.click("//div[28]/div[11]/button[1]");
			for (int second = 0;; second++) {
				if (second >= 60) fail("timeout");
				try { if (selenium.isVisible("after_action_info_container_on_top")) break; } catch (Exception e) {}
				Thread.sleep(1000);
			}

			assertTrue(selenium.isTextPresent("Delete Template action succeeded"));
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