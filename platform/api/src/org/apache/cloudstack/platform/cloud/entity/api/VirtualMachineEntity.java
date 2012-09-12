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
package org.apache.cloudstack.platform.cloud.entity.api;

import java.util.List;

import org.apache.cloudstack.platform.entity.api.CloudEntity;

import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.vm.VirtualMachine;

/**
 * VirtualMachineEntity represents a Virtual Machine in Cloud Orchestration 
 * Platform.  
 *
 */
public interface VirtualMachineEntity extends VirtualMachine, CloudEntity {


    List<VolumeEntity> getVolumes();

    List<NicEntity> getNics();

    TemplateEntity getTemplate();

    /**
     * @return the list of tags associated with the virtual machine
     */
    List<String> getTags();

    /**
     * Start the virtual machine with a given deploy destination
     * @param dest destination to which to deploy the machine
     * @param exclude list of areas to exclude
     * @param plannerToUse the Deployment Planner that should be used 
     */
    void startIn(DeployDestination dest, ExcludeList exclude, String plannerToUse);

    /**
     * Migrate this VM to a certain destination.
     * 
     * @param dest
     * @param exclude
     * @param plannerToUse
     */
    void migrateTo(DeployDestination dest, ExcludeList exclude);

    /**
     * Stop the virtual machine
     * 
     */
    void stop();

    /**
     * Cleans up after any botched starts.  CloudStack Orchestration Platform
     * will attempt a best effort to actually shutdown any resource but
     * even if it cannot, it releases the resource from its database.
     */
    void cleanup();

    /**
     * Destroys the VM.
     */
    void destroy();

    /**
     * Duplicate this VM in the database so that it will start new
     * @param externalId
     * @return a new VirtualMachineEntity
     */
    VirtualMachineEntity duplicate(String externalId);

    /**
     * Take a VM snapshot
     */
    void takeSnapshotOf();

    /**
     * Attach volume to this VM
     * @param volume volume to attach
     * @param deviceId deviceId to use
     */
    void attach(VolumeEntity volume, short deviceId);

    /**
     * Detach the volume from this VM
     * @param volume volume to detach
     */
    void detach(VolumeEntity volume);

    /**
     * Connect the VM to a network
     * @param network network to attach
     * @param deviceId device id to use when a nic is created
     */
    void connectTo(NetworkEntity network, short deviceId);

    /**
     * Disconnect the VM from this network
     * @param netowrk network to disconnect from
     */
    void disconnectFrom(NetworkEntity netowrk);
}
