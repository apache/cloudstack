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
package com.cloud.network.ovs;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.log4j.Logger;

public class OvsApi {
	private static final Logger s_logger = Logger.getLogger(OvsApi.class);
	private final static String _protocol = "http";
	private final static MultiThreadedHttpConnectionManager s_httpClientManager = new MultiThreadedHttpConnectionManager();

	private String _host;

	private final HttpClient _client;

	protected HttpClient createHttpClient() {
		return new HttpClient(s_httpClientManager);
	}

	protected HttpMethod createMethod(String type, String uri, int port)
			throws OvsApiException {
		String url;
		try {
			url = new URL(_protocol, _host, port, uri).toString();
		} catch (MalformedURLException e) {
			s_logger.error("Unable to build Ovs API URL", e);
			throw new OvsApiException("Unable to Ovs API URL", e);
		}

		if ("post".equalsIgnoreCase(type)) {
			return new PostMethod(url);
		} else if ("get".equalsIgnoreCase(type)) {
			return new GetMethod(url);
		} else if ("delete".equalsIgnoreCase(type)) {
			return new DeleteMethod(url);
		} else if ("put".equalsIgnoreCase(type)) {
			return new PutMethod(url);
		} else {
			throw new OvsApiException("Requesting unknown method type");
		}
	}

	public OvsApi() {
		_client = createHttpClient();
		_client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
	}

	public void setControllerAddress(String address) {
		this._host = address;
	}

	// TODO: implement requests
}
