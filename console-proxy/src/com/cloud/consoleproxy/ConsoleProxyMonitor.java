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
package com.cloud.consoleproxy;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.xml.DOMConfigurator;

import com.cloud.console.Logger;

public class ConsoleProxyMonitor {
	private static final Logger s_logger = Logger.getLogger(ConsoleProxyMonitor.class);
	
	private String[] _argv;
	private Map<String, String> _argMap = new HashMap<String, String>();
	
	private volatile Process _process;
	private boolean _quit = false;
	
	public ConsoleProxyMonitor(String[] argv) {
		_argv = argv;
		
		for(String arg : _argv) {
			String[] tokens = arg.split("=");
			if(tokens.length == 2) {
				s_logger.info("Add argument " + tokens[0] + "=" + tokens[1] + " to the argument map");

				_argMap.put(tokens[0].trim(), tokens[1].trim());
			} else {
				s_logger.warn("unrecognized argument, skip adding it to argument map");
			}
		}
	}
	
	private void run() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				onShutdown();
				_quit = true;
			}
		});
		
		while(!_quit) {
			String cmdLine = getLaunchCommandLine();
			
			s_logger.info("Launch console proxy process with command line: " + cmdLine);
			
			try {
				_process = Runtime.getRuntime().exec(cmdLine);
			} catch (IOException e) {
				s_logger.error("Unexpected exception ", e);
				System.exit(1);
			}
			
			try {
				_process.waitFor();
			} catch (InterruptedException e) {
				// TODO
			}
		}
	}
	
	private String getLaunchCommandLine() {
		StringBuffer sb = new StringBuffer("java ");
		String jvmOptions = _argMap.get("jvmoptions");
		
		if(jvmOptions != null)
			sb.append(jvmOptions);
		
		for(Map.Entry<String, String> entry : _argMap.entrySet()) {
			if(!"jvmoptions".equalsIgnoreCase(entry.getKey()))
				sb.append(" ").append(entry.getKey()).append("=").append(entry.getValue());
		}
		
		return sb.toString();
	}
	
	private void onShutdown() {
	}

	private static void configLog4j() {
		URL configUrl = System.class.getResource("/conf/log4j-cloud.xml");
		if(configUrl == null)
			configUrl = System.class.getClassLoader().getSystemResource("log4j-cloud.xml");
		
		if(configUrl == null)
			configUrl = System.class.getClassLoader().getSystemResource("conf/log4j-cloud.xml");
			
		if(configUrl != null) {
			try {
				System.out.println("Configure log4j using " + configUrl.toURI().toString());
			} catch (URISyntaxException e1) {
				e1.printStackTrace();
			}

			try {
				File file = new File(configUrl.toURI());
				
				System.out.println("Log4j configuration from : " + file.getAbsolutePath());
				DOMConfigurator.configureAndWatch(file.getAbsolutePath(), 10000);
			} catch (URISyntaxException e) {
				System.out.println("Unable to convert log4j configuration Url to URI");
			}
		} else {
			System.out.println("Configure log4j with default properties");
		}
	}
	
	public static void main(String[] argv) {
		configLog4j();
		
		(new ConsoleProxyMonitor(argv)).run();
	}
}
