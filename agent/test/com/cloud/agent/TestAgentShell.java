/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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

package com.cloud.agent;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentShell;
import com.cloud.utils.testcase.Log4jEnabledTestCase;

public class TestAgentShell extends Log4jEnabledTestCase {
    protected final static Logger s_logger = Logger.getLogger(TestAgentShell.class);
    
    public void testWget() {
        File file = null;
        try {
            file = File.createTempFile("wget", ".html");
        	AgentShell.wget("http://www.google.com/", file);
        	
	        if (s_logger.isDebugEnabled()) {
	            s_logger.debug("file saved to " + file.getAbsolutePath());
	        }
        	
        } catch (final IOException e) {
            s_logger.warn("Exception while downloading agent update package, ", e);
        }
    }
}
