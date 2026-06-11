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

import com.cloud.exception.InvalidParameterValueException;
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
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement.PowerState;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementService;
import org.joda.time.DateTime;

import javax.inject.Inject;

public final class KVMHAProvider extends HAAbstractHostProvider implements HAProvider<Host>, Configurable {

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
        logger.debug("Recover the host {}", r);
        try {
            if (outOfBandManagementService.isOutOfBandManagementEnabled(r)) {
                final OutOfBandManagementResponse resp = outOfBandManagementService.executePowerOperation(r, PowerOperation.RESET, null);
                return resp.getSuccess();
            } else {
                logger.warn("OOBM recover operation failed for the host {}", r);
                return false;
            }
        } catch (Exception e) {
            logger.warn("OOBM service is not configured or enabled for this host {} error is {}", r, e.getMessage());
            throw new HARecoveryException(String.format("OOBM service is not configured or enabled for this host %s", r), e);
        }
    }

    @Override
    public boolean fence(Host r) throws HAFenceException {
        if (!outOfBandManagementService.isOutOfBandManagementEnabled(r)) {
            logger.warn("Out-of-band management is not enabled/configured for host {}; cannot fence it.", r);
            return false;
        }

        // Fencing must guarantee the host is powered off, and the only reliable signal for that is
        // the actual chassis power state - not the return code of the power-off command. An already-off
        // (or just-turned-off) host can make the power-off command report an error/conflict even though
        // the host is down; conversely, an unreachable BMC must NOT be treated as a successful fence, to
        // avoid split-brain. Therefore: (1) if already off, consider it fenced; (2) otherwise issue a
        // best-effort power-off; (3) declare the fence successful only if the power state is confirmed Off.
        try {
            if (isHostPoweredOff(r)) {
                logger.info("Host {} is already powered off; considering it fenced.", r);
                return true;
            }

            try {
                outOfBandManagementService.executePowerOperation(r, PowerOperation.OFF, null);
            } catch (Exception e) {
                // The power-off may legitimately fail (e.g. the chassis is already off or the command
                // conflicts with the current power state). Do not fail here - verify the actual power
                // state below instead of trusting the command result.
                logger.warn("Out-of-band power-off command for host {} did not complete successfully ({}); verifying the actual power state.", r, e.getMessage());
            }

            if (isHostPoweredOff(r)) {
                logger.info("Confirmed host {} is powered off; fencing successful.", r);
                return true;
            }

            logger.warn("Could not confirm host {} is powered off after the fence power-off; fencing will be retried.", r);
            return false;
        } catch (Exception e) {
            logger.warn("Out-of-band fence operation failed for host {}: {}", r, e.getMessage());
            throw new HAFenceException(String.format("Out-of-band fence operation failed for host %s", r.getName()), e);
        }
    }

    /**
     * Returns {@code true} only when the host's chassis power is positively confirmed to be Off via
     * out-of-band management. Any failure to determine the power state (e.g. an unreachable BMC) returns
     * {@code false}, so that a host whose state cannot be confirmed is never treated as fenced.
     */
    protected boolean isHostPoweredOff(Host r) {
        try {
            final OutOfBandManagementResponse statusResponse = outOfBandManagementService.executePowerOperation(r, PowerOperation.STATUS, null);
            return statusResponse != null && PowerState.Off.equals(statusResponse.getPowerState());
        } catch (Exception e) {
            logger.warn("Unable to determine the out-of-band power state of host {}: {}", r, e.getMessage());
            return false;
        }
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
                throw new InvalidParameterValueException("Unknown HAProviderConfig " + name.toString());
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
            KVMHAConfig.KvmHARecoverAttemptThreshold,
        };
    }
}
