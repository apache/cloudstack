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
package org.apache.cloudstack.platform.service.api;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.platform.cloud.entity.api.VirtualMachineEntity;
import org.apache.cloudstack.platform.cloud.entity.api.VolumeEntity;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.hypervisor.Hypervisor;

public interface OrchestrationService {
    /**
     * creates a new virtual machine
     * 
     * @param uuid externally unique name to reference the virtual machine
     * @param template reference to the template
     * @param hostName name of the host
     * @param cpu # of cpu cores
     * @param speed speed of the cpu core
     * @param memory memory to allocate in bytes
     * @param networks networks that this VM belongs in
     * @param rootDiskTags tags for the root disk
     * @param computeTags tags for the compute
     * @param details extra details to store for the VM
     * @return VirtualMachine
     */
    VirtualMachineEntity create(String name, 
            String template,
            String hostName,
            int cpu, 
            int speed, 
            long memory, 
            List<String> networks, 
            List<String> rootDiskTags,
            List<String> computeTags, 
            Map<String, String> details,
            String owner);

    VirtualMachineEntity createFromScratch(String uuid,
            String iso,
            String os,
            String hypervisor,
            String hostName,
            int cpu,
            int speed,
            long memory,
            List<String> networks,
            List<String> computeTags,
            Map<String, String> details,
            String owner);

    /**
     * Make reservations for a VM
     * @param vm uuid of the VM
     * @param planner DeploymentPlanner to use
     * @param until time specified in seconds before reservation expires.  null means to reserve forever.
     * @return reservation id
     */
    String reserve(String vm, String planner, Long until) throws InsufficientCapacityException;

    /**
     * Deploy the reservation
     * @param reservationId  reservation id during the deployment
     * @return job Id
     * @throws CloudRuntimeException if error 
     */
    String deploy(String reservationId);

    void joinNetwork(String network1, String network2);

    void createNetwork();

    void destroyNetwork();

    VolumeEntity createVolume();

    void registerTemplate(String name, URL path, String os, Hypervisor hypervisor);
}
