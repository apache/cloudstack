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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class PropertiesUtil {
    /**
     * Searches the class path and local paths to find the config file.
     * @param path path to find.  if it starts with / then it's absolute path.
     * @return File or null if not found at all.
     */
    public static File findConfigFile(String path) {
        ClassLoader cl = PropertiesUtil.class.getClassLoader();
        URL url = cl.getResource(path);
        if (url != null) {
            return new File(url.getFile());
        }
        
        url =  ClassLoader.getSystemResource(path);
        if (url != null) {
            return new File(url.getFile());
        }
        
        File file = new File(path);
        if (file.exists()) {
            return file;
        }
        
        String newPath = "conf" + (path.startsWith(File.separator) ? "" : "/") + path;
        url = ClassLoader.getSystemResource(newPath);
        if (url != null) {
            return new File(url.getFile());
        }
        
        url = cl.getResource(newPath);
        if (url != null) {
            return new File(url.getFile());
        }
        
        newPath = "conf" + (path.startsWith(File.separator) ? "" : File.separator) + path;
        file = new File(newPath);
        if (file.exists()) {
            return file;
        }
        
        newPath = System.getenv("CATALINA_HOME");
        if (newPath == null) {
        	newPath = System.getenv("CATALINA_BASE");
        }
        
        if (newPath == null) {
        	newPath = System.getProperty("catalina.home");
        }
        
        if (newPath == null) {
            return null;
        }
        
        file = new File(newPath + File.separator + "conf" + File.separator + path);
        if (file.exists()) {
            return file;
        }
        
        return null;
    }
    
    public static Map<String, Object> toMap(Properties props) {
        Set<String> names = props.stringPropertyNames();
        HashMap<String, Object> map = new HashMap<String, Object>(names.size());
        for (String name : names) {
            map.put(name, props.getProperty(name));
        }
        
        return map;
    }
    
    /*
     * Returns an InputStream for the given resource 
     * This is needed to read the files within a jar in classpath.
     */
    public static InputStream openStreamFromURL(String path){
        ClassLoader cl = PropertiesUtil.class.getClassLoader();
        URL url = cl.getResource(path);
        if (url != null) {
             try{
                 InputStream stream = url.openStream();
                 return stream;
            } catch (IOException ioex) {
                return null;
            }
        }
        return null;
    }
}
