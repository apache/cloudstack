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


import org.apache.cloudstack.storage.resource.NfsSecondaryStorageResource;
import org.apache.cloudstack.storage.resource.SecondaryStorageResourceHandler;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CancelCommand;
import com.cloud.agent.api.Command;
import com.cloud.hypervisor.Hypervisor;

public class PremiumSecondaryStorageResource extends NfsSecondaryStorageResource {


    private Map<Hypervisor.HypervisorType, SecondaryStorageResourceHandler> _handlers = new HashMap<Hypervisor.HypervisorType, SecondaryStorageResourceHandler>();

    private Map<String, String> _activeOutgoingAddresses = new HashMap<String, String>();

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof CancelCommand) {
            return cancel((CancelCommand) cmd);
        }
        String hypervisor = cmd.getContextParam("hypervisor");
        if (hypervisor != null) {
            Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.getType(hypervisor);
            if (hypervisorType == null) {
                logger.error("Unsupported hypervisor type in command context, hypervisor: " + hypervisor);
                return defaultAction(cmd);
            }

            SecondaryStorageResourceHandler handler = getHandler(hypervisorType);
            if (handler == null) {
                logger.error("No handler can be found for hypervisor type in command context, hypervisor: " + hypervisor);
                return defaultAction(cmd);
            }

            return handler.executeRequest(cmd);
        }

        return defaultAction(cmd);
    }

    public Answer defaultAction(Command cmd) {
        return super.executeRequest(cmd);
    }

    /** Can another request in flight here be stopped (checkOnly), or stop it; only the hypervisor handlers run anything that can be. */
    private Answer cancel(CancelCommand cmd) {
        final long sequence = cmd.getSequence();
        if (cmd.isCheckOnly()) {
            final String reason = checkRequestSequenceIsNotCancellable(sequence);
            return new Answer(cmd, reason == null, reason == null ? "request sequence " + sequence + " can be cancelled" : reason);
        }
        final boolean cancelled = cancelRequestSequence(sequence);
        return new Answer(cmd, cancelled, cancelled
                ? "request sequence " + sequence + " cancelled (" + cmd.getReason() + ")"
                : "request sequence " + sequence + " could not be cancelled on this host");
    }

    @Override
    public String checkRequestSequenceIsNotCancellable(long sequence) {
        String reason = "no handler on this host is running request sequence " + sequence;
        for (SecondaryStorageResourceHandler handler : _handlers.values()) {
            reason = handler.checkRequestSequenceIsNotCancellable(sequence);
            if (reason == null) {
                return null;
            }
        }
        return reason;
    }

    @Override
    public boolean cancelRequestSequence(long sequence) {
        for (SecondaryStorageResourceHandler handler : _handlers.values()) {
            if (handler.cancelRequestSequence(sequence)) {
                return true;
            }
        }
        return false;
    }

    public void ensureOutgoingRuleForAddress(String address) {
        if (address == null || address.isEmpty() || address.startsWith("0.0.0.0")) {
            if (logger.isInfoEnabled())
                logger.info("Drop invalid dynamic route/firewall entry " + address);
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
            if (logger.isInfoEnabled())
                logger.info("Add dynamic route/firewall entry for " + address);
            allowOutgoingOnPrivate(address);
        }
    }

    void registerHandler(Hypervisor.HypervisorType hypervisorType, SecondaryStorageResourceHandler handler) {
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
