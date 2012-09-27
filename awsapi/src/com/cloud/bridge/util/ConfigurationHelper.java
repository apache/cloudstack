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
package com.cloud.bridge.util;

import java.io.File;
import java.net.URL;

import javax.servlet.ServletContext;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;


public class ConfigurationHelper {
	
	private static String configPath;
	
	public static void preConfigureConfigPathFromServletContext(ServletContext context){
    	String servletConficPath = context.getRealPath("/");
    	preSetConfigPath(servletConficPath + File.separator + "WEB-INF" + File.separator + "classes");
	}
	
	public static void preSetConfigPath(String path){
		configPath=path;
	}
	
	public static File findConfigurationFile(String name) {

	if(configPath!=null){
    	File file = new File(configPath + File.separator + name);
        if (file.exists()) {
        	return file;
        }
	}
	ServletContext context = getServletContext();
	if(context!=null){
		String newPath = context.getRealPath("/");
        	File file = new File(newPath + File.separator + "WEB-INF" + File.separator + "classes" + File.separator + name);
	        if (file.exists()) {
			return file;
		}

	}
	String newPath = "conf" + (name.startsWith(File.separator) ? "" : "/") + name;
        URL url = ClassLoader.getSystemResource(newPath);
        if (url != null) {
            return new File(url.getFile());
        }
		
        // if running under Tomcat
        newPath = System.getenv("CATALINA_BASE");
        
        if (newPath == null) {
        	newPath = System.getProperty("catalina.base");
        }
        
        if (newPath == null) {
        	newPath = System.getenv("CATALINA_HOME");
        }
        
        if (newPath == null) {
            return null;
        }
        
        File file = new File(newPath + File.separator + "conf" + File.separator + name);
        if (file.exists()) {
            return file;
        }
        return file;
	}

	public static ServletContext getServletContext()
	{
		try{
			MessageContext mc = MessageContext.getCurrentMessageContext();
			if(mc!=null){
				return (ServletContext) mc.getProperty(HTTPConstants.MC_HTTP_SERVLETCONTEXT);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}



}
