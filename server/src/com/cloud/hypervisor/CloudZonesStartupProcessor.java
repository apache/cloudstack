// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.hypervisor;

import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.net.MacAddress;

/**
 * Creates a host record and supporting records such as pod and ip address
 *
 */
@Component
public class CloudZonesStartupProcessor extends AdapterBase {

    @Inject
    private AgentManager _agentManager;

    private long _nodeId = -1;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _agentManager.registerForInitialConnects(this, false);
        if (_nodeId == -1) {
            // FIXME: We really should not do this like this. It should be done
            // at config time and is stored as a config variable.
            _nodeId = MacAddress.getMacAddress().toLong();
        }
        return true;
    }
}
