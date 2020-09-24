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
package com.cloud.storage.resource;

import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import org.apache.cloudstack.storage.resource.NfsSecondaryStorageResource;
import org.apache.cloudstack.storage.resource.SecondaryStorageResourceHandler;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.hypervisor.Hypervisor;

public class PremiumSecondaryStorageResource extends NfsSecondaryStorageResource {

    private static final Logger s_logger = Logger.getLogger(PremiumSecondaryStorageResource.class);

    private Map<Hypervisor.HypervisorType, SecondaryStorageResourceHandler> _handlers = new HashMap<Hypervisor.HypervisorType, SecondaryStorageResourceHandler>();

    private Map<String, String> _activeOutgoingAddresses = new HashMap<String, String>();

    @Override
    public Answer executeRequest(Command cmd) {
        String hypervisor = cmd.getContextParam("hypervisor");
        if (hypervisor != null) {
            Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.getType(hypervisor);
            if (hypervisorType == null) {
                s_logger.error("Unsupported hypervisor type in command context, hypervisor: " + hypervisor);
                return defaultAction(cmd);
            }

            SecondaryStorageResourceHandler handler = getHandler(hypervisorType);
            if (handler == null) {
                s_logger.error("No handler can be found for hypervisor type in command context, hypervisor: " + hypervisor);
                return defaultAction(cmd);
            }

            return handler.executeRequest(cmd);
        }

        return defaultAction(cmd);
    }

    public Answer defaultAction(Command cmd) {
        return super.executeRequest(cmd);
    }

    public void ensureOutgoingRuleForAddress(String address) {
        if (address == null || address.isEmpty() || address.startsWith("0.0.0.0")) {
            if (s_logger.isInfoEnabled())
                s_logger.info("Drop invalid dynamic route/firewall entry " + address);
            return;
        }

        boolean needToSetRule = false;
        synchronized (_activeOutgoingAddresses) {
            if (!_activeOutgoingAddresses.containsKey(address)) {
                _activeOutgoingAddresses.put(address, address);
                needToSetRule = true;
            }
        }

        if (needToSetRule) {
            if (s_logger.isInfoEnabled())
                s_logger.info("Add dynamic route/firewall entry for " + address);
            allowOutgoingOnPrivate(address);
        }
    }

    private void registerHandler(Hypervisor.HypervisorType hypervisorType, SecondaryStorageResourceHandler handler) {
        _handlers.put(hypervisorType, handler);
    }

    private SecondaryStorageResourceHandler getHandler(Hypervisor.HypervisorType hypervisorType) {
        return _handlers.get(hypervisorType);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        if (_inSystemVM) {
            VmwareSecondaryStorageContextFactory.initFactoryEnvironment();
        }

        String nfsVersion = NfsSecondaryStorageResource.retrieveNfsVersionFromParams(params);
        registerHandler(Hypervisor.HypervisorType.VMware, new VmwareSecondaryStorageResourceHandler(this, nfsVersion));
        return true;
    }
}
