/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;

public class HypervisorHostEndPoint implements EndPoint {
	private static final Logger s_logger = Logger.getLogger(HypervisorHostEndPoint.class);
	private long hostId;
	@Inject
	AgentManager agentMgr;
	public HypervisorHostEndPoint(long hostId) {
		this.hostId = hostId;
	}
	
	@Override
	public Answer sendMessage(Command cmd) {
		Answer answer = null;
		try {
			answer = agentMgr.send(hostId, cmd);
		} catch (AgentUnavailableException e) {
			s_logger.debug("Unable to send command:" + cmd + ", due to: " + e.toString());
		} catch (OperationTimedoutException e) {
			s_logger.debug("Unable to send command:" + cmd + ", due to: " + e.toString());
		} catch (Exception e) {
			s_logger.debug("Unable to send command:" + cmd + ", due to: " + e.toString());
		}
		return answer;
	}

}
