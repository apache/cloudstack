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

import org.apache.cloudstack.engine.cloud.entity.api.NetworkEntity;
import org.apache.cloudstack.engine.cloud.entity.api.TemplateEntity;
import org.apache.cloudstack.engine.cloud.entity.api.VirtualMachineEntity;
import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.service.api.OrchestrationService;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.hypervisor.Hypervisor;


public class CloudOrchestrator implements OrchestrationService {



    public VirtualMachineEntity createFromScratch(String uuid, String iso, String os, String hypervisor, String hostName, int cpu, int speed, long memory, List<String> networks, List<String> computeTags,
            Map<String, String> details, String owner) {
        // TODO Auto-generated method stub
        return null;
    }

    public String reserve(String vm, String planner, Long until) throws InsufficientCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    public String deploy(String reservationId) {
        // TODO Auto-generated method stub
        return null;
    }

    public void joinNetwork(String network1, String network2) {
        // TODO Auto-generated method stub

    }

    public void createNetwork() {
        // TODO Auto-generated method stub

    }

    public void destroyNetwork() {
        // TODO Auto-generated method stub

    }

    @Override
    public VolumeEntity createVolume() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TemplateEntity registerTemplate(String name, URL path, String os, Hypervisor hypervisor) {
        return null;
    }

    @Override
    public void destroyNetwork(String networkUuid) {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroyVolume(String volumeEntity) {
        // TODO Auto-generated method stub

    }

    @Override
    public VirtualMachineEntity createVirtualMachine(
            String id, 
            String owner, 
            String templateId, 
            String hostName, 
            String displayName, 
            int cpu, 
            int speed, 
            long memory, 
            List<String> computeTags, 
            List<String> rootDiskTags,
            List<String> networks) {
        return null;
    }

    @Override
    public VirtualMachineEntity createVirtualMachineFromScratch(String id, String owner, String isoId, String hostName, String displayName, String hypervisor, String os, int cpu, int speed, long memory,
            List<String> computeTags, List<String> rootDiskTags, List<String> networks) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NetworkEntity createNetwork(String id, String name, String domainName, String cidr, String gateway) {
        // TODO Auto-generated method stub
        return null;
    }

}
