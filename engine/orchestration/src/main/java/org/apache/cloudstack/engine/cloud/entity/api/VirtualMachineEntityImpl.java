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
package org.apache.cloudstack.engine.cloud.entity.api;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import javax.inject.Inject;

import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Component;
import org.apache.cloudstack.engine.cloud.entity.api.db.VMEntityVO;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.vm.VirtualMachineProfile;

@Component
public class VirtualMachineEntityImpl implements VirtualMachineEntity {

    @Inject
    private VMEntityManager manager;

    private VMEntityVO vmEntityVO;

    public VirtualMachineEntityImpl() {
    }

    public void init(String vmId) {
        this.vmEntityVO = this.manager.loadVirtualMachine(vmId);
    }

    public void init(String vmId, String owner, String hostName, String displayName, int cpu, int speed, long memory, List<String> computeTags,
        List<String> rootDiskTags, List<String> networks) {
        init(vmId);
        this.vmEntityVO.setOwner(owner);
        this.vmEntityVO.setHostname(hostName);
        this.vmEntityVO.setDisplayname(displayName);
        this.vmEntityVO.setComputeTags(computeTags);
        this.vmEntityVO.setRootDiskTags(rootDiskTags);
        this.vmEntityVO.setNetworkIds(networks);

        manager.saveVirtualMachine(vmEntityVO);
    }

    public VirtualMachineEntityImpl(String vmId, VMEntityManager manager) {
        this.manager = manager;
        this.vmEntityVO = this.manager.loadVirtualMachine(vmId);
    }

    public VirtualMachineEntityImpl(String vmId, String owner, String hostName, String displayName, int cpu, int speed, long memory, List<String> computeTags,
            List<String> rootDiskTags, List<String> networks, VMEntityManager manager) {
        this(vmId, manager);
        this.vmEntityVO.setOwner(owner);
        this.vmEntityVO.setHostname(hostName);
        this.vmEntityVO.setDisplayname(displayName);
        this.vmEntityVO.setComputeTags(computeTags);
        this.vmEntityVO.setRootDiskTags(rootDiskTags);
        this.vmEntityVO.setNetworkIds(networks);

        manager.saveVirtualMachine(vmEntityVO);
    }

    @Override
    public String getUuid() {
        return vmEntityVO.getUuid();
    }

    @Override
    public long getId() {
        return vmEntityVO.getId();
    }

    @Override
    public String getCurrentState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDesiredState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getCreatedTime() {
        return vmEntityVO.getCreated();
    }

    @Override
    public Date getLastUpdatedTime() {
        return vmEntityVO.getUpdateTime();
    }

    @Override
    public String getOwner() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getDetails() {
        return vmEntityVO.getDetails();
    }

    @Override
    public void addDetail(String name, String value) {
        vmEntityVO.setDetail(name, value);
    }

    @Override
    public void delDetail(String name, String value) {
        // TODO Auto-generated method stub
    }

    @Override
    public void updateDetail(String name, String value) {
        // TODO Auto-generated method stub
    }

    @Override
    public List<Method> getApplicableActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> listVolumeIds() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<VolumeEntity> listVolumes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> listNicUuids() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NicEntity> listNics() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TemplateEntity getTemplate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> listTags() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addTag() {
        // TODO Auto-generated method stub

    }

    @Override
    public void delTag() {
        // TODO Auto-generated method stub

    }

    @Override
    public String reserve(DeploymentPlanner plannerToUse, DeploymentPlan plan, ExcludeList exclude, String caller) throws InsufficientCapacityException,
        ResourceUnavailableException {
        return manager.reserveVirtualMachine(this.vmEntityVO, plannerToUse, plan, exclude);
    }

    @Override
    public void migrateTo(String reservationId, String caller) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deploy(String reservationId, String caller, Map<VirtualMachineProfile.Param, Object> params, boolean deployOnGivenHost) throws InsufficientCapacityException,
        ResourceUnavailableException {
        manager.deployVirtualMachine(reservationId, this.vmEntityVO, caller, params, deployOnGivenHost);
    }

    @Override
    public boolean stop(String caller) throws ResourceUnavailableException {
        return manager.stopvirtualmachine(this.vmEntityVO, caller);
    }

    @Override
    public boolean stopForced(String caller) throws ResourceUnavailableException {
        return manager.stopvirtualmachineforced(this.vmEntityVO, caller);
    }

    @Override
    public void cleanup() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean destroy(String caller, boolean expunge) throws AgentUnavailableException, OperationTimedoutException, ConcurrentOperationException {
        return manager.destroyVirtualMachine(this.vmEntityVO, caller, expunge);
    }

    @Override
    public VirtualMachineEntity duplicate(String externalId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SnapshotEntity takeSnapshotOf() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void attach(VolumeEntity volume, short deviceId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void detach(VolumeEntity volume) {
        // TODO Auto-generated method stub

    }

    @Override
    public void connectTo(NetworkEntity network, short nicId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void disconnectFrom(NetworkEntity netowrk, short nicId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setParamsToEntity(Map<VirtualMachineProfile.Param, Object> map) {
        if (MapUtils.isNotEmpty(map)) {
            if (this.vmEntityVO != null) {
                Map<String, String> details = this.vmEntityVO.getDetails();
                if (details == null) {
                    details = new HashMap<String, String>();
                }
                for (Map.Entry<VirtualMachineProfile.Param, Object> entry : map.entrySet()) {
                    if (null != entry && null != entry.getValue() && null != entry.getKey()) {
                        details.put(entry.getKey().getName(), entry.getValue().toString());
                    }
                }
                this.vmEntityVO.setDetails(details);
            }
        }

    }
}
