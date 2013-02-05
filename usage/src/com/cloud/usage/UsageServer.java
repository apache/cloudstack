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
package com.cloud.usage;

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentContext;

public class UsageServer {
    private static final Logger s_logger = Logger.getLogger(UsageServer.class.getName());
    public static final String Name = "usage-server";
    
    UsageManager mgr; 
    
    /**
     * @param args
     */
    public static void main(String[] args) {
    	initLog4j();
        UsageServer usage = new UsageServer();
        usage.init(args);
        usage.start();
    }

    public void init(String[] args) {
    }

    public void start() {
    	ApplicationContext appContext = new ClassPathXmlApplicationContext("usageApplicationContext.xml");
	    
    	try {
    		ComponentContext.initComponentsLifeCycle();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	mgr = appContext.getBean(UsageManager.class);
    	
        if (mgr != null) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("UsageServer ready...");
            }
        }
    }

    public void stop() {

    }

    public void destroy() {

    }

    static private void initLog4j() {
    	File file = PropertiesUtil.findConfigFile("log4j-cloud.xml");
    	if (file != null) {
        s_logger.info("log4j configuration found at " + file.getAbsolutePath());
        DOMConfigurator.configureAndWatch(file.getAbsolutePath());
	    } else {
	        file = PropertiesUtil.findConfigFile("log4j-cloud.properties");
	        if (file != null) {
	            s_logger.info("log4j configuration found at " + file.getAbsolutePath());
	            PropertyConfigurator.configureAndWatch(file.getAbsolutePath());
	        }
	    }
   }
}
