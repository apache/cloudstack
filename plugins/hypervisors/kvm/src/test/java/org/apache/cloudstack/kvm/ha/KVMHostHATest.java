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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.api.response.OutOfBandManagementResponse;
import org.apache.cloudstack.ha.provider.HACheckerException;
import org.apache.cloudstack.ha.provider.HAFenceException;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement.PowerOperation;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement.PowerState;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementService;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class KVMHostHATest {

    @Mock
    private Host host;
    @Mock
    private KVMHostActivityChecker kvmHostActivityChecker;
    @Mock
    private OutOfBandManagementService outOfBandManagementService;
    private KVMHAProvider kvmHAProvider;

    @Before
    public void setup() {
        kvmHAProvider = new KVMHAProvider();
        kvmHAProvider.hostActivityChecker = kvmHostActivityChecker;
        kvmHAProvider.outOfBandManagementService = outOfBandManagementService;
    }

    private OutOfBandManagementResponse powerStatusResponse(PowerState state) {
        OutOfBandManagementResponse response = mock(OutOfBandManagementResponse.class);
        lenient().when(response.getPowerState()).thenReturn(state);
        return response;
    }

    @Test
    public void testHostActivityForHealthyHost() throws HACheckerException, StorageUnavailableException {
        lenient().when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        when(kvmHostActivityChecker.isHealthy(host)).thenReturn(true);
        assertTrue(kvmHAProvider.isHealthy(host));
    }

    @Test
    public void testHostActivityForUnHealthyHost() throws HACheckerException, StorageUnavailableException {
        lenient().when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        when(kvmHostActivityChecker.isHealthy(host)).thenReturn(false);
        assertFalse(kvmHAProvider.isHealthy(host));
    }

    @Test
    public void testHostActivityForActiveHost() throws HACheckerException, StorageUnavailableException {
        lenient().when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        DateTime dt = new DateTime();
        when(kvmHostActivityChecker.isActive(host, dt)).thenReturn(true);
        assertTrue(kvmHAProvider.hasActivity(host, dt));
    }

    @Test
    public void testHostActivityForDownHost() throws HACheckerException, StorageUnavailableException {
        lenient().when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        DateTime dt = new DateTime();
        when(kvmHostActivityChecker.isActive(host, dt)).thenReturn(false);
        assertFalse(kvmHAProvider.hasActivity(host, dt));
    }

    @Test
    public void testFenceWhenOutOfBandManagementNotEnabled() throws HAFenceException {
        when(outOfBandManagementService.isOutOfBandManagementEnabled(host)).thenReturn(false);
        assertFalse(kvmHAProvider.fence(host));
        verify(outOfBandManagementService, never()).executePowerOperation(eq(host), eq(PowerOperation.OFF), isNull());
    }

    @Test
    public void testFenceWhenHostAlreadyPoweredOff() throws HAFenceException {
        OutOfBandManagementResponse offStatus = powerStatusResponse(PowerState.Off);
        when(outOfBandManagementService.isOutOfBandManagementEnabled(host)).thenReturn(true);
        when(outOfBandManagementService.executePowerOperation(eq(host), eq(PowerOperation.STATUS), isNull())).thenReturn(offStatus);
        assertTrue(kvmHAProvider.fence(host));
        // The host is already off, so no power-off command should be issued.
        verify(outOfBandManagementService, never()).executePowerOperation(eq(host), eq(PowerOperation.OFF), isNull());
    }

    @Test
    public void testFenceConfirmedOffAfterPowerOff() throws HAFenceException {
        OutOfBandManagementResponse onStatus = powerStatusResponse(PowerState.On);
        OutOfBandManagementResponse offStatus = powerStatusResponse(PowerState.Off);
        when(outOfBandManagementService.isOutOfBandManagementEnabled(host)).thenReturn(true);
        // First STATUS (pre-check) returns On, second STATUS (post power-off) returns Off.
        when(outOfBandManagementService.executePowerOperation(eq(host), eq(PowerOperation.STATUS), isNull()))
                .thenReturn(onStatus, offStatus);
        assertTrue(kvmHAProvider.fence(host));
        verify(outOfBandManagementService).executePowerOperation(eq(host), eq(PowerOperation.OFF), isNull());
    }

    @Test
    public void testFencePowerOffCommandFailsButHostConfirmedOff() throws HAFenceException {
        // Regression: the power-off command fails (e.g. the chassis is already off and the BMC returns
        // HTTP 409), but the host is actually off. Fencing must succeed because the confirmed power state -
        // not the power-off command's result - is authoritative.
        OutOfBandManagementResponse onStatus = powerStatusResponse(PowerState.On);
        OutOfBandManagementResponse offStatus = powerStatusResponse(PowerState.Off);
        when(outOfBandManagementService.isOutOfBandManagementEnabled(host)).thenReturn(true);
        when(outOfBandManagementService.executePowerOperation(eq(host), eq(PowerOperation.STATUS), isNull()))
                .thenReturn(onStatus, offStatus);
        when(outOfBandManagementService.executePowerOperation(eq(host), eq(PowerOperation.OFF), isNull()))
                .thenThrow(new CloudRuntimeException("power-off failed: HTTP 409"));
        assertTrue(kvmHAProvider.fence(host));
    }

    @Test
    public void testFenceFailsWhenPowerStateCannotBeConfirmedOff() throws HAFenceException {
        // The host cannot be confirmed off (e.g. an unreachable BMC). Fencing must NOT succeed, to avoid
        // restarting VMs while the host may still be running (split-brain).
        when(outOfBandManagementService.isOutOfBandManagementEnabled(host)).thenReturn(true);
        when(outOfBandManagementService.executePowerOperation(eq(host), eq(PowerOperation.STATUS), isNull()))
                .thenThrow(new CloudRuntimeException("BMC unreachable"));
        when(outOfBandManagementService.executePowerOperation(eq(host), eq(PowerOperation.OFF), isNull()))
                .thenThrow(new CloudRuntimeException("BMC unreachable"));
        assertFalse(kvmHAProvider.fence(host));
    }

}
