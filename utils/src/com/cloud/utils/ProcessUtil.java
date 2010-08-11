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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

import java.util.Properties;

public class ProcessUtil {
    private static final Logger s_logger = Logger.getLogger(ProcessUtil.class.getName());

	// paths cannot be hardcoded
	public static void pidCheck(String run) throws ConfigurationException {
		
		String dir = "/var/run";
		
		try {
			final File propsFile = PropertiesUtil.findConfigFile("environment.properties");
			if (propsFile == null) {
				s_logger.debug("environment.properties could not be opened");
			}
			else {
				final FileInputStream finputstream = new FileInputStream(propsFile);
				final Properties props = new Properties();
				props.load(finputstream);
				finputstream.close();
				dir = props.getProperty("paths.pid");
				if (dir == null) {
					dir = "/var/run";
				}
			}
		} catch (IOException e) {
			s_logger.debug("environment.properties could not be opened");
		}
		
	    final File pidFile = new File(dir + File.separator + run);
	    try {
	        if (!pidFile.createNewFile()) {
	            if (!pidFile.exists()) {
	                throw new ConfigurationException("Unable to write to " + pidFile.getAbsolutePath() + ".  Are you sure you're running as root?");
	            }
	
	            final FileInputStream is = new FileInputStream(pidFile);
	            final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	            final String pidLine = reader.readLine();
	            if (pidLine == null) {
	                throw new ConfigurationException("Java process is being started twice.  If this is not true, remove " + pidFile.getAbsolutePath());
	            }
	            try {
	                final long pid = Long.parseLong(pidLine);
	                final Script script = new Script("bash", 120000, s_logger);
	                script.add("-c", "ps -p " + pid);
	                final String result = script.execute();
	                if (result == null) {
	                    throw new ConfigurationException("Java process is being started twice.  If this is not true, remove " + pidFile.getAbsolutePath());
	                }
	                if (!pidFile.delete()) {
	                    throw new ConfigurationException("Java process is being started twice.  If this is not true, remove " + pidFile.getAbsolutePath());
	                }
	                if (!pidFile.createNewFile()) {
	                    throw new ConfigurationException("Java process is being started twice.  If this is not true, remove " + pidFile.getAbsolutePath());
	                }
	            } catch (final NumberFormatException e) {
	                throw new ConfigurationException("Java process is being started twice.  If this is not true, remove " + pidFile.getAbsolutePath());
	            }
	        }
	        pidFile.deleteOnExit();
	
	        final Script script = new Script("bash", 120000, s_logger);
	        script.add("-c", "echo $PPID");
	        final OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
	        script.execute(parser);
	
	        final String pid = parser.getLine();
	
	        final FileOutputStream strm = new FileOutputStream(pidFile);
	        strm.write((pid + "\n").getBytes());
	        strm.close();
	    } catch (final IOException e) {
	        throw new CloudRuntimeException("Unable to create the " + pidFile.getAbsolutePath() + ".  Are you running as root?", e);
	    }
	}
	
	public static String dumpStack() {
		StringBuilder sb = new StringBuilder();
		StackTraceElement[] elems = Thread.currentThread().getStackTrace();
		if(elems != null && elems.length > 0) {
			for(StackTraceElement elem : elems) {
				sb.append("\tat ").append(elem.getMethodName()).append("(").append(elem.getFileName()).append(":").append(elem.getLineNumber()).append(")\n");
			}
		}
		return sb.toString();
	}
}
