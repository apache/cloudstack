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
package org.apache.cloudstack.api.response;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class LdapConfigurationResponse extends BaseResponse {
	@SerializedName("hostname")
	@Param(description = "hostname")
	private String hostname;

	@SerializedName("port")
	@Param(description = "port")
	private int port;

	public LdapConfigurationResponse() {
		super();
	}

	public LdapConfigurationResponse(final String hostname) {
		super();
		this.hostname = hostname;
	}

	public LdapConfigurationResponse(final String hostname, final int port) {
		this.hostname = hostname;
		this.port = port;
	}

	public String getHostname() {
		return hostname;
	}

	public int getPort() {
		return port;
	}

	public void setHostname(final String hostname) {
		this.hostname = hostname;
	}

	public void setPort(final int port) {
		this.port = port;
	}
}