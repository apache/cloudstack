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
package org.apache.cloudstack.platform.orchestration;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.framework.ipc.Publisher;
import org.apache.cloudstack.platform.planning.Concierge;
import org.apache.cloudstack.platform.service.api.OrchestrationService;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.vm.VirtualMachine;

public class CloudOrchestrator implements OrchestrationService {
    int _retryCount = 5;
    Concierge _concierge = null;    // These are injected
    Publisher _publisher = null;

    @Override
    public VirtualMachine create(String uuid, String template, String hostName, int cpu, int speed, long memory, List<String> networks, List<String> rootDiskTags, List<String> computeTags, Map<String, String> details,
            String owner) {
        // creates a virtual machine and relevant work in database
        return null;
    }

    @Override
    public VirtualMachine createFromScratch(String uuid, String iso, String os, String hypervisor, String hostName, int cpu, int speed, long memory, List<String> networks, List<String> computeTags,
            Map<String, String> details, String owner) {
        // creates a virtual machine and relevant work in database
        return null;
    }

    @Override
    public String reserve(String vm, String planner, Long until) throws InsufficientCapacityException {
        return _concierge.reserve(vm, planner);
    }

    @Override
    public String cancel(String reservationId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String deploy(String reservationId, String callback) {
        for (int i = 0; i < _retryCount; i++) {
            try {
                // Retrieves the reservation
                // Signals Network and Storage to prepare
                // Signals Compute
                _concierge.claim(reservationId);
            } catch (Exception e) {
                // Cancel reservations.
            }
            _concierge.reserveAnother(reservationId);
        }
        return null;
    }

    @Override
    public String stop(String vm) {

        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void destroy(String vm) {
        // TODO Auto-generated method stub

    }

    @Override
    public void joinNetwork(String network1, String network2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void attachNetwork(String network, String vm) {
        // TODO Auto-generated method stub

    }

    @Override
    public void detachNetwork(String network, String vm) {
        // TODO Auto-generated method stub

    }

    @Override
    public void attachVolume(String vm, String vol) {
        // TODO Auto-generated method stub

    }

    @Override
    public void createNetwork() {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroyNetwork() {
        // TODO Auto-generated method stub

    }

    @Override
    public void createVolume() {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroyVolume() {
        // TODO Auto-generated method stub

    }

    @Override
    public void snapshotVirtualMachine(String vm) {
        // TODO Auto-generated method stub

    }

    @Override
    public void snapshotVolume(String volume) {
        // TODO Auto-generated method stub

    }

    @Override
    public void backup(String snapshot) {
        // TODO Auto-generated method stub

    }

    @Override
    public void registerTemplate(String name, URL path, String os, Hypervisor hypervisor) {
        // TODO Auto-generated method stub

    }

}
