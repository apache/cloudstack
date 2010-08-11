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
package com.cloud.utils.net;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class NfsUtils {
    
    public static String url2Mount(String urlStr) throws URISyntaxException {
        URI url;
        url = new URI(urlStr);
        int port = url.getPort();
        return url.getHost() + ":" + url.getPath();
    }
    
    public static String uri2Mount(URI uri) {
        return uri.getHost() + ":" + uri.getPath();
    }
    
    public static String url2PathSafeString(String urlStr) {
        String safe = urlStr.replace(File.separatorChar, '-');
        safe = safe.replace("?", "");
        safe = safe.replace("*", "");
        safe = safe.replace("\\", "");
        safe = safe.replace("/", "");
        return safe;
    }
    
    public static String getHostPart(String nfsPath)  {
    	String toks[] = nfsPath.split(":");
    	if (toks != null && toks.length == 2) {
    		return toks[0];
    	}
    	return null;
    }

}
