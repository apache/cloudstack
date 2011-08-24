/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.hypervisor.hyperv.resource;

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

}
