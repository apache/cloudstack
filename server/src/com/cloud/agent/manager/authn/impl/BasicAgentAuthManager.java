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
package com.cloud.agent.manager.authn.impl;

import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.manager.authn.AgentAuthnException;
import com.cloud.agent.manager.authn.AgentAuthorizer;
import com.cloud.utils.component.AdapterBase;

@Component
public class BasicAgentAuthManager extends AdapterBase implements AgentAuthorizer {

    @Inject
    private AgentManager _agentManager;

    @Override
    public boolean authorizeAgent(StartupCommand[] cmd) throws AgentAuthnException {
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _agentManager.registerForInitialConnects(this, true);
        return true;
    }
}
