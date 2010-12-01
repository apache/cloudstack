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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import com.cloud.utils.exception.CloudRuntimeException;

public class UriUtils {
    public static String formNfsUri(String host, String path) {
        try {
            URI uri = new URI("nfs", host, path, null);
            return uri.toString();
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("Unable to form nfs URI: " + host + " - " + path);
        }
    }
    
    public static String formIscsiUri(String host, String iqn, Integer lun) {
        try {
            String path = iqn;
            if (lun != null) {
                path += "/" + lun.toString();
            }
            URI uri = new URI("iscsi", host, path, null);
            return uri.toString();
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("Unable to form iscsi URI: " + host + " - " + iqn + " - " + lun);
        }
    }

    public static String formFileUri(String path) {
        File file = new File(path);
        
        return file.toURI().toString();
    }
    
    // a simple URI component helper (Note: it does not deal with URI paramemeter area)
    public static String encodeURIComponent(String url) {
    	int schemeTail = url.indexOf("://");
   	
    	int pathStart = 0;
    	if(schemeTail > 0)
    		pathStart = url.indexOf('/', schemeTail + 3);
    	else
    		pathStart = url.indexOf('/');
    	
    	if(pathStart > 0) {
    		String[] tokens = url.substring(pathStart + 1).split("/");
    		if(tokens != null) {
    			StringBuffer sb = new StringBuffer();
    			sb.append(url.substring(0, pathStart));
    			for(String token : tokens) {
    				sb.append("/").append(URLEncoder.encode(token));
    			}
    			
    			return sb.toString();
    		}
    	}
    	
		// no need to do URL component encoding
		return url;
    }
}
