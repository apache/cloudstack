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
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class KVMHAProvider extends HAAbstractHostProvider implements HAProvider<Host>, Configurable {
    private static final Logger LOG = Logger.getLogger(KVMHAProvider.class);
    private static final Map<HAProviderConfig, ConfigKey<?>> KVM_HA_CONFIG_MAP = new HashMap<>();

    static {
        KVM_HA_CONFIG_MAP.put(HAProviderConfig.HealthCheckTimeout, KVMHAConfig.KvmHAHealthCheckTimeout);
        KVM_HA_CONFIG_MAP.put(HAProviderConfig.ActivityCheckTimeout, KVMHAConfig.KvmHAActivityCheckTimeout);
        KVM_HA_CONFIG_MAP.put(HAProviderConfig.MaxActivityCheckInterval, KVMHAConfig.KvmHAActivityCheckInterval);
        KVM_HA_CONFIG_MAP.put(HAProviderConfig.MaxActivityChecks, KVMHAConfig.KvmHAActivityCheckMaxAttempts);
        KVM_HA_CONFIG_MAP.put(HAProviderConfig.ActivityCheckFailureRatio, KVMHAConfig.KvmHAActivityCheckFailureThreshold);
        KVM_HA_CONFIG_MAP.put(HAProviderConfig.RecoveryWaitTimeout, KVMHAConfig.KvmHARecoverWaitPeriod);
        KVM_HA_CONFIG_MAP.put(HAProviderConfig.RecoveryTimeout, KVMHAConfig.KvmHARecoverTimeout);
        KVM_HA_CONFIG_MAP.put(HAProviderConfig.FenceTimeout, KVMHAConfig.KvmHAFenceTimeout);
        KVM_HA_CONFIG_MAP.put(HAProviderConfig.MaxRecoveryAttempts, KVMHAConfig.KvmHARecoverAttemptThreshold);
        KVM_HA_CONFIG_MAP.put(HAProviderConfig.MaxDegradedWaitTimeout, KVMHAConfig.KvmHADegradedMaxPeriod);
    }

    @Inject
    protected KVMHostActivityChecker hostActivityChecker;
    @Inject
    protected OutOfBandManagementService outOfBandManagementService;

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
                final OutOfBandManagementResponse resp = outOfBandManagementService.executePowerOperation(r, PowerOperation.RESET, null);
                return resp.getSuccess();
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
            if (outOfBandManagementService.isOutOfBandManagementEnabled(r)){
                final OutOfBandManagementResponse resp = outOfBandManagementService.executePowerOperation(r, PowerOperation.OFF, null);
                return resp.getSuccess();
            } else {
                LOG.warn("OOBM fence operation failed for this host " + r.getName());
                return false;
            }
        } catch (Exception e){
            LOG.warn("OOBM service is not configured or enabled for this host " + r.getName() + " error is " + e.getMessage());
            throw new HAFenceException("OOBM service is not configured or enabled for this host " + r.getName() , e);
        }
    }

    @Override
    public HAResource.ResourceSubType resourceSubType() {
        return HAResource.ResourceSubType.KVM;
    }

    @Override
    public Object getConfigValue(final HAProviderConfig name, final Host host) {
        final Long clusterId = host.getClusterId();
        return Optional.ofNullable(KVM_HA_CONFIG_MAP.get(name).valueIn(clusterId))
                .orElseThrow(() -> new InvalidParameterException("Unknown HAProviderConfig " + name.toString()));
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
