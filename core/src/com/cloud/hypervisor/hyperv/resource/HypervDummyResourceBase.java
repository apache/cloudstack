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
package com.cloud.hypervisor.hyperv.resource;

import java.util.Map;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResource;
import com.cloud.resource.ServerResourceBase;

/**
 * Implementation of dummy resource to be returned from discoverer
 **/

public class HypervDummyResourceBase extends ServerResourceBase implements 
		ServerResource {

	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StartupCommand[] initialize() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PingCommand getCurrentStatus(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Answer executeRequest(Command cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getDefaultScriptsDir() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setConfigParams(Map<String, Object> params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Object> getConfigParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getRunLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setRunLevel(int level) {
		// TODO Auto-generated method stub
		
	}

}
