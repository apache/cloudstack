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
import com.cloud.agent.api.CheckOnHostAnswer;
import com.cloud.agent.api.CheckOnHostCommand;
import com.cloud.agent.api.CheckVMActivityOnStoragePoolCommand;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.ha.HighAvailabilityManager;
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

import javax.inject.Inject;
import java.util.ArrayList;
import org.joda.time.DateTime;
import java.util.HashMap;
import java.util.List;

public class KVMHostActivityChecker extends AdapterBase implements ActivityCheckerInterface<Host>, HealthCheckerInterface<Host> {

    @Inject
    private ClusterDao clusterDao;
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
            return hasVMActivityOnHost(r, suspectTime);
        } catch (HACheckerException e) {
            //Re-throwing the exception to avoid poluting the 'HACheckerException' already thrown
            throw e;
        } catch (Exception e){
            String message = String.format("Operation timed out, probably the %s is not reachable.", r.toString());
            logger.warn(message, e);
            throw new HACheckerException(message, e);
        }
    }

    @Override
    public boolean isHealthy(Host r) {
        return isHostAgentUp(r);
    }

    private boolean isHostAgentUp(Host host) {
        if (host.getHypervisorType() != Hypervisor.HypervisorType.KVM && host.getHypervisorType() != Hypervisor.HypervisorType.LXC) {
            throw new IllegalStateException(String.format("Calling KVM investigator for non KVM Host of type [%s].", host.getHypervisorType()));
        }

        Status hostStatus = getHostAgentStatus(host);

        logger.debug("{} has the status [{}].", host.toString(), hostStatus);
        return hostStatus == Status.Up;
    }

    public Status getHostAgentStatus(Host host) {
        if (host.getHypervisorType() != Hypervisor.HypervisorType.KVM && host.getHypervisorType() != Hypervisor.HypervisorType.LXC) {
            return null;
        }

        Status hostStatusFromItself = checkHostStatusWithSameHost(host);
        if (hostStatusFromItself == Status.Up) {
            return Status.Up;
        }

        Status hostStatusFromNeighbour = checkHostStatusWithNeighbourHosts(host);
        Status hostStatus = hostStatusFromItself;
        if (hostStatusFromNeighbour == Status.Up && (hostStatusFromItself == Status.Disconnected || hostStatusFromItself == Status.Down)) {
            hostStatus = Status.Disconnected;
        }
        if (hostStatusFromNeighbour == Status.Down && (hostStatusFromItself == Status.Disconnected || hostStatusFromItself == Status.Down)) {
            hostStatus = Status.Down;
        }

        logger.debug("HA: HOST is ineligible legacy state {} for host {}", hostStatus, host);
        return hostStatus;
    }

    private Status checkHostStatusWithSameHost(Host host) {
        Status hostStatus;
        boolean reportFailureIfOneStorageIsDown = HighAvailabilityManager.KvmHAFenceHostIfHeartbeatFailsOnStorage.value();
        final CheckOnHostCommand cmd = new CheckOnHostCommand(host, reportFailureIfOneStorageIsDown);
        try {
            logger.debug("Checking {} status...", host.toString());
            Answer answer = agentMgr.easySend(host.getId(), cmd);
            if (answer != null) {
                if (answer.getResult()) {
                    hostStatus = ((CheckOnHostAnswer)answer).isAlive() ? Status.Up : Status.Down;
                } else {
                    logger.debug("{} is not active according to itself, details: {}.", host.toString(), answer.getDetails());
                    hostStatus = Status.Down;
                }
                logger.debug("{} has the status [{}].", host.toString(), hostStatus);
            } else {
                logger.debug("Setting {} to \"Disconnected\" status.", host.toString());
                hostStatus = Status.Disconnected;
            }
        } catch (Exception e) {
            logger.warn("Failed to send command CheckOnHostCommand to {}.", host.toString(), e);
            hostStatus = Status.Disconnected;
        }

        return hostStatus;
    }

    private Status checkHostStatusWithNeighbourHosts(Host host) {
        Status hostStatusFromNeighbour = Status.Unknown;
        boolean reportFailureIfOneStorageIsDown = HighAvailabilityManager.KvmHAFenceHostIfHeartbeatFailsOnStorage.value();
        final CheckOnHostCommand cmd = new CheckOnHostCommand(host, reportFailureIfOneStorageIsDown);
        List<HostVO> neighbors = resourceManager.listHostsInClusterByStatus(host.getClusterId(), Status.Up);
        for (HostVO neighbor : neighbors) {
            if (neighbor.getId() == host.getId()
                    || (neighbor.getHypervisorType() != Hypervisor.HypervisorType.KVM && neighbor.getHypervisorType() != Hypervisor.HypervisorType.LXC)) {
                continue;
            }

            try {
                logger.debug("Investigating {} via neighboring {}.", host.toString(), neighbor.toString());
                Answer answer = agentMgr.easySend(neighbor.getId(), cmd);
                if (answer != null) {
                    if (answer.getResult()) {
                        hostStatusFromNeighbour = ((CheckOnHostAnswer)answer).isAlive() ? Status.Up : Status.Down;
                        logger.debug("Neighboring {} returned status [{}] for the investigated {}.", neighbor.toString(), hostStatusFromNeighbour, host.toString());
                        if (hostStatusFromNeighbour == Status.Up) {
                            return hostStatusFromNeighbour;
                        }
                    } else {
                        logger.debug("{} is not active according to neighbor {}, details: {}.", host.toString(), neighbor.toString(), answer.getDetails());
                    }
                } else {
                    logger.debug("Neighboring {} is Disconnected.", neighbor.toString());
                }
            } catch (Exception e) {
                logger.warn("Failed to send command CheckOnHostCommand to neighbor {}.", neighbor.toString(), e);
            }
        }

        return hostStatusFromNeighbour;
    }

    private boolean hasVMActivityOnHost(Host host, DateTime suspectTime) throws HACheckerException {
        if (host.getHypervisorType() != Hypervisor.HypervisorType.KVM && host.getHypervisorType() != Hypervisor.HypervisorType.LXC) {
            throw new IllegalStateException(String.format("Calling KVM investigator for non KVM Host of type [%s].", host.getHypervisorType()));
        }
        boolean activityStatus = true;
        HashMap<StoragePool, List<Volume>> poolVolMap = getVolumeUuidOnHost(host);
        for (StoragePool pool : poolVolMap.keySet()) {
            activityStatus = verifyActivityOfStorageOnHost(poolVolMap, pool, host, suspectTime, activityStatus);
            if (!activityStatus) {
                logger.warn("It seems that the storage pool [{}] does not have activity on {}.", pool, host);
                break;
            }
        }

        return activityStatus;
    }

    protected boolean verifyActivityOfStorageOnHost(HashMap<StoragePool, List<Volume>> poolVolMap, StoragePool pool, Host host, DateTime suspectTime, boolean activityStatus) throws HACheckerException, IllegalStateException {
        List<Volume> volume_list = poolVolMap.get(pool);
        final CheckVMActivityOnStoragePoolCommand cmd = new CheckVMActivityOnStoragePoolCommand(host, pool, volume_list, suspectTime);

        logger.debug("Checking VM activity for {} on storage pool [{}].", host.toString(), pool);
        try {
            Answer answer = storageManager.sendToPool(pool, getNeighbors(host), cmd);

            if (answer != null) {
                activityStatus = !answer.getResult();
                logger.debug("{} {} activity on storage pool [{}]", host.toString(), activityStatus ? "has" : "does not have", pool);
            } else {
                String message = String.format("Did not get a valid response for VM activity check for %s on storage pool [%s].", host.toString(), pool);
                logger.debug(message);
                throw new IllegalStateException(message);
            }
        } catch (StorageUnavailableException e){
            String message = String.format("Storage [%s] is unavailable to do the check, probably the %s is not reachable.", pool, host);
            logger.warn(message, e);
            throw new HACheckerException(message, e);
        }
        return activityStatus;
    }

    private HashMap<StoragePool, List<Volume>> getVolumeUuidOnHost(Host host) {
        List<VMInstanceVO> vm_list = vmInstanceDao.listByHostId(host.getId());
        List<VolumeVO> volume_list = new ArrayList<VolumeVO>();
        for (VirtualMachine vm : vm_list) {
            logger.debug("Retrieving volumes of VM [{}]...", vm);
            List<VolumeVO> vm_volume_list = volumeDao.findByInstance(vm.getId());
            volume_list.addAll(vm_volume_list);
        }

        HashMap<StoragePool, List<Volume>> poolVolMap = new HashMap<StoragePool, List<Volume>>();
        for (Volume vol : volume_list) {
            StoragePool sp = storagePool.findById(vol.getPoolId());
            logger.debug("Retrieving storage pool [{}] of volume [{}]...", sp, vol);
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

    public long[] getNeighbors(Host host) {
        List<Long> neighbors = new ArrayList<Long>();
        List<HostVO> clusterHosts = resourceManager.listHostsInClusterByStatus(host.getClusterId(), Status.Up);
        logger.debug("Retrieving all \"Up\" hosts from cluster [{}]...", clusterDao.findById(host.getClusterId()));
        for (HostVO clusterHost : clusterHosts) {
            if (clusterHost.getId() == host.getId() || (clusterHost.getHypervisorType() != Hypervisor.HypervisorType.KVM && clusterHost.getHypervisorType() != Hypervisor.HypervisorType.LXC)) {
                continue;
            }
            neighbors.add(clusterHost.getId());
        }
        return ArrayUtils.toPrimitive(neighbors.toArray(new Long[neighbors.size()]));
    }

}
