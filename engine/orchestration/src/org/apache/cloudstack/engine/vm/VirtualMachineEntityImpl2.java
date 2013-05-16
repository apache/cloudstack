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
package org.apache.cloudstack.engine.vm;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.cloud.entity.VMEntityVO;
import org.apache.cloudstack.engine.cloud.entity.api.NetworkEntity;
import org.apache.cloudstack.engine.cloud.entity.api.NicEntity;
import org.apache.cloudstack.engine.cloud.entity.api.SnapshotEntity;
import org.apache.cloudstack.engine.cloud.entity.api.TemplateEntity;
import org.apache.cloudstack.engine.cloud.entity.api.VirtualMachineEntity;
import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageOrchestrator;
import org.apache.cloudstack.network.NetworkOrchestrator;

import com.cloud.dao.EntityManager;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.CloudException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.storage.VolumeVO;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachineProfile.Param;

public class VirtualMachineEntityImpl2 implements VirtualMachineEntity {

    private static EntityManager s_entityMgr;
    private static VirtualMachineOrchestrator s_vmOrchestrator;
    private static NetworkOrchestrator s_networkOrchestrator;
    private static StorageOrchestrator s_storageOrchestrator;

    private static GenericSearchBuilder<VolumeVO, String> VolumeUuidSB;
    private static SearchBuilder<VolumeVO> VolumeSB;
    private static GenericSearchBuilder<NicVO, String> NicUuidSB;
    private static SearchBuilder<NicVO> NicSB;

    public static void init(
            EntityManager entityMgr,
            VirtualMachineOrchestrator vmOrchestrator,
            NetworkOrchestrator networkOrchestrator,
            StorageOrchestrator storageOrchestrator) {
        s_entityMgr = entityMgr;
        s_vmOrchestrator = vmOrchestrator;
        s_networkOrchestrator = networkOrchestrator;
        s_storageOrchestrator = storageOrchestrator;

        VolumeUuidSB = s_entityMgr.createGenericSearchBuilder(VolumeVO.class, String.class);
        VolumeVO vol = VolumeUuidSB.entity();
        VolumeUuidSB.selectField(vol.getUuid()).and("vm", vol.getInstanceId(), Op.EQ).done();

        VolumeSB = s_entityMgr.createSearchBuilder(VolumeVO.class);
        vol = VolumeSB.entity();
        VolumeSB.and("vm", vol.getInstanceId(), Op.EQ).done();

        NicUuidSB = s_entityMgr.createGenericSearchBuilder(NicVO.class, String.class);
        NicVO nic = NicUuidSB.entity();
        NicUuidSB.selectField(nic.getUuid()).and("vm", nic.getInstanceId(), Op.EQ).done();

        NicSB = s_entityMgr.createSearchBuilder(NicVO.class);
        nic = NicSB.entity();
        NicSB.and("vm", nic.getInstanceId(), Op.EQ).done();
    }

    VMEntityVO _vm;
    DeployDestination _dest;

    public VirtualMachineEntityImpl2(VMEntityVO vm) {
        _vm = vm;
    }

    @Override
    public String getUuid() {
        return _vm.getUuid();
    }

    @Override
    public long getId() {
        return _vm.getId();
    }

    @Override
    public String getCurrentState() {
        return _vm.getState().toString();
    }

    @Override
    public String getDesiredState() {
        return null;
    }

    @Override
    public Date getCreatedTime() {
        return _vm.getCreated();
    }

    @Override
    public Date getLastUpdatedTime() {
        return _vm.getUpdateTime();
    }

    @Override
    public String getOwner() {
        return _vm.getOwner();
    }

    @Override
    public Map<String, String> getDetails() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addDetail(String name, String value) {
    }

    @Override
    public void delDetail(String name, String value) {
    }

    @Override
    public void updateDetail(String name, String value) {
    }

    @Override
    public List<Method> getApplicableActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> listVolumeIds() {
        SearchCriteria<String> sc = VolumeUuidSB.create();
        sc.setParameters("vm", _vm.getId());

        return s_entityMgr.search(VolumeVO.class, sc);
    }

    @Override
    public List<VolumeEntity> listVolumes() {
        SearchCriteria<VolumeVO> sc = VolumeSB.create();
        sc.setParameters("vm", _vm.getId());

        List<VolumeVO> vols = s_entityMgr.search(VolumeVO.class, sc);
        List<VolumeEntity> entities = new ArrayList<VolumeEntity>(vols.size());
        for (VolumeVO vol : vols) {
            entities.add(null);
        }

        return entities;
    }

    @Override
    public List<String> listNicUuids() {
        SearchCriteria<String> sc = NicUuidSB.create();
        sc.setParameters("vm", _vm.getId());

        return s_entityMgr.search(NicVO.class, sc);
    }

    @Override
    public List<NicEntity> listNics() {
        return null;
    }

    @Override
    public TemplateEntity getTemplate() {
        return null;
    }

    @Override
    public List<String> listTags() {
        return null;
    }

    @Override
    public void addTag() {
    }

    @Override
    public void delTag() {
        // TODO Auto-generated method stub

    }

    @Override
    public String reserve(String plannerToUse, DeploymentPlan plan, ExcludeList exclude, String caller)
            throws InsufficientCapacityException, ResourceUnavailableException {
        return null;
    }

    @Override
    public void migrateTo(String reservationId, String caller) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deploy(String reservationId, String caller, Map<Param, Object> params) throws InsufficientCapacityException, ResourceUnavailableException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean stop(String caller) throws ResourceUnavailableException, CloudException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void cleanup() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean destroy(String caller) throws AgentUnavailableException, CloudException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return false;
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
}
