/*
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.bridge.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

/**
 * @author Kelven Yang
 */
public class ServletRequestDataSource implements DataSource {
    protected final static Logger logger = Logger.getLogger(ServletRequestDataSource.class);

    private HttpServletRequest request;
    
    public ServletRequestDataSource(HttpServletRequest request) {
    	this.request = request;
    }
    
	@Override
	public String getContentType() {
		assert(false);
		return null;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return request.getInputStream();
	}

	@Override
	public String getName() {
		assert(false);
		return null;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		assert(false);
		return null;
	}
}
