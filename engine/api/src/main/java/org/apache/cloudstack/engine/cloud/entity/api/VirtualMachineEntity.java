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
package org.apache.cloudstack.engine.cloud.entity.api;

import java.util.List;
import java.util.Map;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.cloudstack.engine.entity.api.CloudStackEntity;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.CloudException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.vm.VirtualMachineProfile;

/**
 * VirtualMachineEntity represents a Virtual Machine in Cloud Orchestration
 * Platform.
 *
 */
@Path("vm/{id}")
@Produces({"application/json", "application/xml"})
@XmlRootElement(name = "vm")
public interface VirtualMachineEntity extends CloudStackEntity {

    /**
     * @return List of uuids for volumes attached to this virtual machine.
     */
    @GET
    List<String> listVolumeIds();

    /**
     * @return List of volumes attached to this virtual machine.
     */
    List<VolumeEntity> listVolumes();

    /**
     * @return List of uuids for nics attached to this virtual machine.
     */
    List<String> listNicUuids();

    /**
     * @return List of nics attached to this virtual machine.
     */
    List<NicEntity> listNics();

    /**
     * @return the template this virtual machine is based off.
     */
    TemplateEntity getTemplate();

    /**
     * @return the list of tags associated with the virtual machine
     */
    List<String> listTags();

    void addTag();

    void delTag();

    /**
     * Start the virtual machine with a given deployment plan
     * @param plannerToUse the Deployment Planner that should be used
     * @param plan plan to which to deploy the machine
     * @param exclude list of areas to exclude
     * @return a reservation id
     */
    String reserve(DeploymentPlanner plannerToUse, @BeanParam DeploymentPlan plan, ExcludeList exclude, String caller) throws InsufficientCapacityException,
        ResourceUnavailableException;

    /**
     * Migrate this VM to a certain destination.
     *
     * @param reservationId reservation id from reserve call.
     */
    void migrateTo(String reservationId, String caller);

    /**
     * Deploy this virtual machine according to the reservation from before.
     * @param reservationId reservation id from reserve call.
     *
     */
    void deploy(String reservationId, String caller, Map<VirtualMachineProfile.Param, Object> params, boolean deployOnGivenHost)
            throws InsufficientCapacityException, ResourceUnavailableException;

    /**
     * Stop the virtual machine
     *
     */
    boolean stop(String caller) throws ResourceUnavailableException, CloudException;

    /**
     * Stop the virtual machine, by force if necessary
     *
     */
    boolean stopForced(String caller) throws ResourceUnavailableException, CloudException;

    /**
     * Cleans up after any botched starts.  CloudStack Orchestration Platform
     * will attempt a best effort to actually shutdown any resource but
     * even if it cannot, it releases the resource from its database.
     */
    void cleanup();

    /**
     * Destroys the VM.
     * @param expunge indicates if vm should be expunged
     */
    boolean destroy(String caller, boolean expunge) throws AgentUnavailableException, CloudException, ConcurrentOperationException;

    /**
     * Duplicate this VM in the database so that it will start new
     * @param externalId
     * @return a new VirtualMachineEntity
     */
    VirtualMachineEntity duplicate(String externalId);

    /**
     * Take a VM snapshot
     */
    SnapshotEntity takeSnapshotOf();

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
    void connectTo(NetworkEntity network, short nicId);

    /**
     * Disconnect the VM from this network
     * @param netowrk network to disconnect from
     */
    void disconnectFrom(NetworkEntity netowrk, short nicId);

    /**
     *  passing additional params of deployment associated with the virtual machine
     */
    void setParamsToEntity(Map<VirtualMachineProfile.Param, Object> params);

}
