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

package org.apache.cloudstack.kvm.ha;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckOnHostCommand;
import com.cloud.agent.api.CheckVMActivityOnStoragePoolCommand;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.ha.provider.ActivityCheckerInterface;
import org.apache.cloudstack.ha.provider.HACheckerException;
import org.apache.cloudstack.ha.provider.HealthCheckerInterface;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import org.joda.time.DateTime;
import java.util.HashMap;
import java.util.List;

public class KVMHostActivityChecker extends AdapterBase implements ActivityCheckerInterface<Host>, HealthCheckerInterface<Host> {
    private final static Logger LOG = Logger.getLogger(KVMHostActivityChecker.class);

    @Inject
    private VolumeDao volumeDao;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private AgentManager agentMgr;
    @Inject
    private PrimaryDataStoreDao storagePool;
    @Inject
    private StorageManager storageManager;
    @Inject
    private ResourceManager resourceManager;

    @Override
    public boolean isActive(Host r, DateTime suspectTime) throws HACheckerException {
        try {
            return isVMActivtyOnHost(r, suspectTime);
        }
        catch (StorageUnavailableException e){
            throw new HACheckerException("Storage is unavailable to do the check, mostly host is not reachable ", e);
        }
        catch (Exception e){
            throw new HACheckerException("Operation timed out, mostly host is not reachable ", e);
        }
    }

    @Override
    public boolean isHealthy(Host r) {
        return isAgentActive(r);
    }

    private boolean isAgentActive(Host agent) {
        if (agent.getHypervisorType() != Hypervisor.HypervisorType.KVM && agent.getHypervisorType() != Hypervisor.HypervisorType.LXC) {
            throw new IllegalStateException("Calling KVM investigator for non KVM Host of type " + agent.getHypervisorType());
        }
        Status hostStatus = Status.Unknown;
        Status neighbourStatus = Status.Unknown;
        final CheckOnHostCommand cmd = new CheckOnHostCommand(agent);
        try {
            Answer answer = agentMgr.easySend(agent.getId(), cmd);
            if (answer != null) {
                hostStatus = answer.getResult() ? Status.Down : Status.Up;
                if ( hostStatus == Status.Up ){
                    return true;
                }
            }
            else {
                hostStatus = Status.Disconnected;
            }
        } catch (Exception e) {
            LOG.warn("Failed to send command to host: " + agent.getId());
        }

        List<HostVO> neighbors = resourceManager.listHostsInClusterByStatus(agent.getClusterId(), Status.Up);
        for (HostVO neighbor : neighbors) {
            if (neighbor.getId() == agent.getId() || (neighbor.getHypervisorType() != Hypervisor.HypervisorType.KVM && neighbor.getHypervisorType() != Hypervisor.HypervisorType.LXC)) {
                continue;
            }
            if (LOG.isTraceEnabled()){
                LOG.trace("Investigating host:" + agent.getId() + " via neighbouring host:" + neighbor.getId());
            }
            try {
                Answer answer = agentMgr.easySend(neighbor.getId(), cmd);
                if (answer != null) {
                    neighbourStatus = answer.getResult() ? Status.Down : Status.Up;
                    if (LOG.isTraceEnabled()){
                        LOG.trace("Neighbouring host:" + neighbor.getId() + " returned status:" + neighbourStatus + " for the investigated host:" + agent.getId());
                    }
                    if (neighbourStatus == Status.Up) {
                        break;
                    }
                }
            } catch (Exception e) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Failed to send command to host: " + neighbor.getId());
                }
            }
        }
        if (neighbourStatus == Status.Up && (hostStatus == Status.Disconnected || hostStatus == Status.Down)) {
            hostStatus = Status.Disconnected;
        }
        if (neighbourStatus == Status.Down && (hostStatus == Status.Disconnected || hostStatus == Status.Down)) {
            hostStatus = Status.Down;
        }

        if (LOG.isTraceEnabled()){
            LOG.trace("Resource state = " + hostStatus.name());
        }
        return hostStatus == Status.Up;
    }

    private boolean isVMActivtyOnHost(Host agent, DateTime suspectTime) throws StorageUnavailableException {
        if (agent.getHypervisorType() != Hypervisor.HypervisorType.KVM && agent.getHypervisorType() != Hypervisor.HypervisorType.LXC) {
            throw new IllegalStateException("Calling KVM investigator for non KVM Host of type " + agent.getHypervisorType());
        }
        boolean activityStatus = true;
        HashMap<StoragePool, List<Volume>> poolVolMap = getVolumeUuidOnHost(agent);
        for (StoragePool pool : poolVolMap.keySet()) {
            //for each storage pool find activity
            List<Volume> volume_list = poolVolMap.get(pool);
            final CheckVMActivityOnStoragePoolCommand cmd = new CheckVMActivityOnStoragePoolCommand(agent, pool, volume_list, suspectTime);
            //send the command to appropriate storage pool
            Answer answer = storageManager.sendToPool(pool, getNeighbors(agent), cmd);
            if (answer != null) {
                activityStatus = ! answer.getResult();
            } else {
                throw new IllegalStateException("Did not get a valid response for VM activity check for host " + agent.getId());
            }
        }
        if (LOG.isDebugEnabled()){
            LOG.debug("Resource active = " + activityStatus);
        }
        return activityStatus;
    }

    private HashMap<StoragePool, List<Volume>> getVolumeUuidOnHost(Host agent) {
        List<VMInstanceVO> vm_list = vmInstanceDao.listByHostId(agent.getId());
        List<VolumeVO> volume_list = new ArrayList<VolumeVO>();
        for (VirtualMachine vm : vm_list) {
            List<VolumeVO> vm_volume_list = volumeDao.findByInstance(vm.getId());
            volume_list.addAll(vm_volume_list);
        }

        HashMap<StoragePool, List<Volume>> poolVolMap = new HashMap<StoragePool, List<Volume>>();
        for (Volume vol : volume_list) {
            StoragePool sp = storagePool.findById(vol.getPoolId());
            if (!poolVolMap.containsKey(sp)) {
                List<Volume> list = new ArrayList<Volume>();
                list.add(vol);

                poolVolMap.put(sp, list);
            } else {
                poolVolMap.get(sp).add(vol);
            }
        }
        return poolVolMap;
    }

    public long[] getNeighbors(Host agent) {
        List<Long> neighbors = new ArrayList<Long>();
        List<HostVO> cluster_hosts = resourceManager.listHostsInClusterByStatus(agent.getClusterId(), Status.Up);
        for (HostVO host : cluster_hosts) {
            if (host.getId() == agent.getId() || (host.getHypervisorType() != Hypervisor.HypervisorType.KVM && host.getHypervisorType() != Hypervisor.HypervisorType.LXC)) {
                continue;
            }
            neighbors.add(host.getId());
        }
        return ArrayUtils.toPrimitive(neighbors.toArray(new Long[neighbors.size()]));
    }

}
