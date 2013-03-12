// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils;

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class LogUtils {
	public static final Logger s_logger = Logger.getLogger(LogUtils.class);
	
    public static void initLog4j(String log4jConfigFileName) {
    	assert(log4jConfigFileName != null);
    	File file = PropertiesUtil.findConfigFile(log4jConfigFileName);
    	if (file != null) {
	        s_logger.info("log4j configuration found at " + file.getAbsolutePath());
	        DOMConfigurator.configureAndWatch(file.getAbsolutePath());
	    } else {
	    	String nameWithoutExtension = log4jConfigFileName.substring(0, log4jConfigFileName.lastIndexOf('.'));
	        file = PropertiesUtil.findConfigFile(nameWithoutExtension + ".properties");
	        if (file != null) {
	            s_logger.info("log4j configuration found at " + file.getAbsolutePath());
	            DOMConfigurator.configureAndWatch(file.getAbsolutePath());
	        }
	    }
   }
}
