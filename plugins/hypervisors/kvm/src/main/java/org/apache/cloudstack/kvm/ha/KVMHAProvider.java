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

package org.apache.cloudstack.kvm.ha;

import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;

import org.apache.cloudstack.api.response.OutOfBandManagementResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.ha.HAResource;
import org.apache.cloudstack.ha.provider.HACheckerException;
import org.apache.cloudstack.ha.provider.HAFenceException;
import org.apache.cloudstack.ha.provider.HAProvider;
import org.apache.cloudstack.ha.provider.HARecoveryException;
import org.apache.cloudstack.ha.provider.host.HAAbstractHostProvider;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement.PowerOperation;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementService;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement.PowerState;
import org.apache.cloudstack.outofbandmanagement.dao.OutOfBandManagementDao;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import java.security.InvalidParameterException;

public final class KVMHAProvider extends HAAbstractHostProvider implements HAProvider<Host>, Configurable {
    private final static Logger LOG = Logger.getLogger(KVMHAProvider.class);

    @Inject
    protected KVMHostActivityChecker hostActivityChecker;
    @Inject
    protected OutOfBandManagementService outOfBandManagementService;
    @Inject
    private OutOfBandManagementDao outOfBandManagementDao;
    @Inject
    private VolumeDao volumeDao;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private PrimaryDataStoreDao storagePool;

    @Override
    public boolean isEligible(final Host host) {
       if (outOfBandManagementService.isOutOfBandManagementEnabled(host)){
            return !isInMaintenanceMode(host) && !isDisabled(host) &&
                    hostActivityChecker.getNeighbors(host).length > 0 &&
                    (Hypervisor.HypervisorType.KVM.equals(host.getHypervisorType()) ||
                            Hypervisor.HypervisorType.LXC.equals(host.getHypervisorType()));
        }
        return false;
    }

    @Override
    public boolean isHealthy(final Host r) throws HACheckerException {
        return hostActivityChecker.isHealthy(r);
    }

    @Override
    public boolean hasActivity(final Host r, final DateTime suspectTime) throws HACheckerException {
        return hostActivityChecker.isActive(r, suspectTime);
    }

    @Override
    public boolean recover(Host r) throws HARecoveryException {
        try {
            if (outOfBandManagementService.isOutOfBandManagementEnabled(r)){
                final OutOfBandManagement oobm = outOfBandManagementDao.findByHost(r.getId());
                if(oobm.getPowerState() == PowerState.Off){
                    LOG.warn("OOBM recover operation failed for the host " + r.getName() + " already OFF");
                    return false;
                }else{
                    final OutOfBandManagementResponse resp = outOfBandManagementService.executePowerOperation(r, PowerOperation.RESET, null);
                    return resp.getSuccess();
                }
            } else {
                LOG.warn("OOBM recover operation failed for the host " + r.getName());
                return false;
            }
        } catch (Exception e){
            LOG.warn("OOBM service is not configured or enabled for this host " + r.getName() + " error is " + e.getMessage());
            throw new HARecoveryException(" OOBM service is not configured or enabled for this host " + r.getName(), e);
        }
    }

    @Override
    public boolean fence(Host r) throws HAFenceException {
        try {
            //INVALIDATE CACHE Test
            HashMap<StoragePool, List<Volume>> poolVolMap = getVolumeUuidOnHost(r);
            for (StoragePool pool : poolVolMap.keySet()) {
                List<Volume> volume_list = poolVolMap.get(pool);
                LOG.warn("=====================KVMHAProvider.java====");
                LOG.warn("pool = "+pool);
                LOG.warn("volume_list = "+volume_list);
                LOG.warn("=========================");
            }    

            if (outOfBandManagementService.isOutOfBandManagementEnabled(r)){
                final OutOfBandManagement oobm = outOfBandManagementDao.findByHost(r.getId());
                if (oobm.getPowerState() == PowerState.Unknown){
                    return true;
                } else {
                    final OutOfBandManagementResponse resp = outOfBandManagementService.executePowerOperation(r, PowerOperation.OFF, null);
                    return resp.getSuccess();
                }
            } else {
                LOG.warn("OOBM fence operation failed for this host " + r.getName());
                return false;
            }
        } catch (Exception e){
            LOG.warn("OOBM service is not configured or enabled for this host " + r.getName() + " error is " + e.getMessage());
            throw new HAFenceException("OOBM service is not configured or enabled for this host " + r.getName() , e);
        }
    }

    private HashMap<StoragePool, List<Volume>> getVolumeUuidOnHost(Host r) {
        List<VMInstanceVO> vm_list = vmInstanceDao.listByHostId(r.getId());
        List<VolumeVO> volume_list = new ArrayList<VolumeVO>();
        for (VirtualMachine vm : vm_list) {
            LOG.debug(String.format("Retrieving volumes of VM [%s]...", vm.getId()));
            List<VolumeVO> vm_volume_list = volumeDao.findByInstance(vm.getId());
            volume_list.addAll(vm_volume_list);
        }

        HashMap<StoragePool, List<Volume>> poolVolMap = new HashMap<StoragePool, List<Volume>>();
        for (Volume vol : volume_list) {
            LOG.debug(String.format("Retrieving storage pool [%s] of volume [%s]...", vol.getPoolId(), vol.getId()));
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
    
    @Override
    public HAResource.ResourceSubType resourceSubType() {
        return HAResource.ResourceSubType.KVM;
    }

    @Override
    public Object getConfigValue(final HAProviderConfig name, final Host host) {
        final Long clusterId = host.getClusterId();
        switch (name) {
            case HealthCheckTimeout:
                return KVMHAConfig.KvmHAHealthCheckTimeout.valueIn(clusterId);
            case ActivityCheckTimeout:
                return KVMHAConfig.KvmHAActivityCheckTimeout.valueIn(clusterId);
            case MaxActivityCheckInterval:
                return KVMHAConfig.KvmHAActivityCheckInterval.valueIn(clusterId);
            case MaxActivityChecks:
                return KVMHAConfig.KvmHAActivityCheckMaxAttempts.valueIn(clusterId);
            case ActivityCheckFailureRatio:
                return KVMHAConfig.KvmHAActivityCheckFailureThreshold.valueIn(clusterId);
            case RecoveryWaitTimeout:
                return KVMHAConfig.KvmHARecoverWaitPeriod.valueIn(clusterId);
            case RecoveryTimeout:
                return KVMHAConfig.KvmHARecoverTimeout.valueIn(clusterId);
            case FenceTimeout:
                return KVMHAConfig.KvmHAFenceTimeout.valueIn(clusterId);
            case MaxRecoveryAttempts:
                return KVMHAConfig.KvmHARecoverAttemptThreshold.valueIn(clusterId);
            case MaxDegradedWaitTimeout:
                return KVMHAConfig.KvmHADegradedMaxPeriod.valueIn(clusterId);
            default:
                throw new InvalidParameterException("Unknown HAProviderConfig " + name.toString());
        }
    }

    @Override
    public String getConfigComponentName() {
        return KVMHAConfig.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
            KVMHAConfig.KvmHAHealthCheckTimeout,
            KVMHAConfig.KvmHAActivityCheckTimeout,
            KVMHAConfig.KvmHARecoverTimeout,
            KVMHAConfig.KvmHAFenceTimeout,
            KVMHAConfig.KvmHAActivityCheckInterval,
            KVMHAConfig.KvmHAActivityCheckMaxAttempts,
            KVMHAConfig.KvmHAActivityCheckFailureThreshold,
            KVMHAConfig.KvmHADegradedMaxPeriod,
            KVMHAConfig.KvmHARecoverWaitPeriod,
            KVMHAConfig.KvmHARecoverAttemptThreshold
        };
    }
}
