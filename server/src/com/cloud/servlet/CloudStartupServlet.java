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
package com.cloud.servlet;

import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.cloud.utils.LogUtils;
import com.cloud.utils.SerialVersionUID;
import com.cloud.utils.component.ComponentContext;

public class CloudStartupServlet extends HttpServlet {
    public static final Logger s_logger = Logger.getLogger(CloudStartupServlet.class.getName());
    static final long serialVersionUID = SerialVersionUID.CloudStartupServlet;
    
    Timer _timer = new Timer();
    
    @Override
    public void init(ServletConfig config) throws ServletException {
    	LogUtils.initLog4j("log4j-cloud.xml");
    	SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());       	
    	
    	// wait when condition is ready for initialization
    	_timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if(ComponentContext.getApplicationContext() != null) {
					_timer.cancel();
					ComponentContext.initComponentsLifeCycle();
				}
			}
    	}, 0, 1000);
    }
}
