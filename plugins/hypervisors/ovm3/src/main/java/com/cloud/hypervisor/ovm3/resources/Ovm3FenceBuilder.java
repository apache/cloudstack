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

package com.cloud.hypervisor.ovm3.resources;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;


import com.cloud.agent.AgentManager;
import com.cloud.agent.api.FenceAnswer;
import com.cloud.agent.api.FenceCommand;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.ha.FenceBuilder;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VirtualMachine;

public class Ovm3FenceBuilder extends AdapterBase implements FenceBuilder {
    Map<String, Object> fenceParams;
    @Inject
    AgentManager agentMgr;
    @Inject
    ResourceManager resourceMgr;


    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        fenceParams = params;
        return true;
    }

    @Override
    public boolean start() {
        /* start the agent here ? */
        return true;
    }

    @Override
    public boolean stop() {
        /* stop the agent here ? */
        return true;
    }

    public Ovm3FenceBuilder() {
        super();
    }

    @Override
    public Boolean fenceOff(VirtualMachine vm, Host host) {
        if (host.getHypervisorType() != HypervisorType.Ovm3) {
            logger.debug("Don't know how to fence non Ovm3 hosts "
                    + host.getHypervisorType());
            return null;
        } else {
            logger.debug("Fencing " + vm + " on host " + host
                    + " with params: "+ fenceParams );
        }

        List<HostVO> hosts = resourceMgr.listAllHostsInCluster(host
                .getClusterId());
        FenceCommand fence = new FenceCommand(vm, host);

        for (HostVO h : hosts) {
            if (h.getHypervisorType() == HypervisorType.Ovm3 &&
                    h.getStatus() == Status.Up &&
                    h.getId() != host.getId()) {
                FenceAnswer answer;
                try {
                    answer = (FenceAnswer) agentMgr.send(h.getId(), fence);
                } catch (AgentUnavailableException | OperationTimedoutException e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Moving on to the next host because "
                                + h.toString() + " is unavailable", e);
                    }
                    continue;
                }
                if (answer != null && answer.getResult()) {
                    return true;
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Unable to fence off " + vm.toString() + " on "
                    + host.toString());
        }

        return false;
    }

}
