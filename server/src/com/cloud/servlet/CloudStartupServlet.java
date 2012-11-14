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

import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

import com.cloud.api.ApiServer;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.server.ConfigurationServer;
import com.cloud.server.ManagementServer;
import com.cloud.utils.SerialVersionUID;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.SystemIntegrityChecker;
import com.cloud.utils.component.LegacyComponentLocator.ComponentInfo;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericDaoBase;

public class CloudStartupServlet extends HttpServlet implements ServletContextListener {
	public static final Logger s_logger = Logger.getLogger(CloudStartupServlet.class.getName());
	
    static final long serialVersionUID = SerialVersionUID.CloudStartupServlet;
   
    protected static ComponentLocator s_locator;
    
	@Override
    public void init() throws ServletException {
		
	    // Save Configuration Values
        //ComponentLocator loc = ComponentLocator.getLocator(ConfigurationServer.Name);
	    ConfigurationServer c = (ConfigurationServer)ComponentLocator.getComponent(ConfigurationServer.Name);
	    //ConfigurationServer c = new ConfigurationServerImpl();
	    try {
	    	c.persistDefaultValues();
	    	s_locator = ComponentLocator.getLocator(ManagementServer.Name);
		    ManagementServer ms = (ManagementServer)ComponentLocator.getComponent(ManagementServer.Name);
		    ApiServer.initApiServer(ms.getApiConfig());
	    } catch (InvalidParameterValueException ipve) {
	    	s_logger.error("Exception starting management server ", ipve);
	    	throw new ServletException (ipve.getMessage());
	    } catch (Exception e) {
	    	s_logger.error("Exception starting management server ", e);
	    	throw new ServletException (e.getMessage());
	    }
	}
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
	    try {
	        init();
	    } catch (ServletException e) {
	        s_logger.error("Exception starting management server ", e);
	        throw new RuntimeException(e);
	    }
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}

	//
	// following should be moved to CloudStackServer component later to encapsulate business logic in one place
	//
	private void initCloudStackComponents() {
        runCheckers();
        startDaos();    // daos should not be using managers and adapters.
     
/*        
        configureManagers();
        configureAdapters();
        startManagers();
        startAdapters();
*/	
	}
	
    private void runCheckers() {
		Map<String, SystemIntegrityChecker> checkers = ComponentContext.getApplicationContext().getBeansOfType(
			SystemIntegrityChecker.class);
		
		for(SystemIntegrityChecker checker : checkers.values()) {
			try {
				checker.check();
			} catch (Exception e) {
                s_logger.error("Problems with running checker:" + checker.getClass().getName(), e);
                System.exit(1);
			}
		}
    }
	
    private void startDaos() {
		@SuppressWarnings("rawtypes")
		Map<String, GenericDaoBase> daos = ComponentContext.getApplicationContext().getBeansOfType(
				GenericDaoBase.class);
			
		for(GenericDaoBase dao : daos.values()) {
			try {
				
				// dao.configure(dao.getClass().getSimpleName(), params);
			} catch (Exception e) {
                s_logger.error("Problems with running checker:" + dao.getClass().getName(), e);
                System.exit(1);
			}
		}
    }
	
}
