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
package com.cloud.bridge.service.core.ec2;

import java.util.Calendar;

import com.cloud.bridge.util.EC2RestAuth;

public class ServiceOffer {
	
	private String   id;
	private String   name;
	private String   memory;
	private String   cpuNumber;
	private String   cpuSpeed;
    private Calendar created;
    
	public ServiceOffer() {
		id        = null;
		name      = null;
		memory    = null;
		cpuNumber = null;
		cpuSpeed  = null;
		created   = null;
	}
	
	public void setId( String id ) {
		this.id = id;
	}
	
	public String getId() {
		return this.id;
	}

	public void setName( String name ) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}

	public void setMemory( String memory ) {
		this.memory = memory;
	}
	
	public String getMemory() {
		return this.memory;
	}

	public void setCPUNumber( String param ) {
		this.cpuNumber = param;
	}
	
	public int getCPUNumber() {
		return Integer.parseInt( this.cpuNumber );
	}

	public void setCPUSpeed( String param ) {
		this.cpuSpeed = param;
	}
	
	public String getCPUSpeed() {
		return this.cpuSpeed;
	}

	public void setCreated( String created ) {
		this.created = EC2RestAuth.parseDateString( created );
	}
	
	public Calendar getCreated() {
		return this.created;
	}
}
