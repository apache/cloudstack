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

package com.cloud.utils;

import org.apache.log4j.Logger;
import org.junit.Assert;

import com.cloud.utils.Profiler;
import com.cloud.utils.testcase.Log4jEnabledTestCase;

public class TestProfiler extends Log4jEnabledTestCase {
    protected final static Logger s_logger = Logger.getLogger(TestProfiler.class);
	
	public void testProfiler() {
		s_logger.info("testProfiler() started");
		
		Profiler pf = new Profiler();
		pf.start();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		pf.stop();
		
		s_logger.info("Duration : " + pf.getDuration());
		
		Assert.assertTrue(pf.getDuration() >= 1000);
		
		s_logger.info("testProfiler() stopped");
	}
}
