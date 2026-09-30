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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.api.response.OutOfBandManagementResponse;
import org.apache.cloudstack.ha.provider.HACheckerException;
import org.apache.cloudstack.ha.provider.HAFenceException;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement.PowerOperation;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementService;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;

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
        kvmHAProvider = spy(new KVMHAProvider());
        kvmHAProvider.hostActivityChecker = kvmHostActivityChecker;
        kvmHAProvider.outOfBandManagementService = outOfBandManagementService;
    }

    private OutOfBandManagementResponse oobmResponse(final boolean success) {
        final OutOfBandManagementResponse response = new OutOfBandManagementResponse();
        response.setSuccess(success);
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
    public void testFenceSucceedsViaOobm() throws HAFenceException {
        when(outOfBandManagementService.isOutOfBandManagementEnabled(host)).thenReturn(true);
        when(outOfBandManagementService.executePowerOperation(host, PowerOperation.OFF, null)).thenReturn(oobmResponse(true));

        assertTrue(kvmHAProvider.fence(host));

        verify(kvmHostActivityChecker, never()).getHostAgentStatus(host);
    }

    @Test
    public void testFenceOobmFailedFallbackDisabled() throws HAFenceException {
        when(outOfBandManagementService.isOutOfBandManagementEnabled(host)).thenReturn(true);
        when(outOfBandManagementService.executePowerOperation(host, PowerOperation.OFF, null)).thenReturn(oobmResponse(false));
        doReturn(false).when(kvmHAProvider).isStorageHeartbeatFencingEnabled(host);

        assertFalse(kvmHAProvider.fence(host));

        verify(kvmHostActivityChecker, never()).getHostAgentStatus(host);
    }

    @Test
    public void testFenceOobmFailedStorageHeartbeatReportsHostDown() throws HAFenceException {
        when(outOfBandManagementService.isOutOfBandManagementEnabled(host)).thenReturn(true);
        when(outOfBandManagementService.executePowerOperation(host, PowerOperation.OFF, null)).thenReturn(oobmResponse(false));
        doReturn(true).when(kvmHAProvider).isStorageHeartbeatFencingEnabled(host);
        when(kvmHostActivityChecker.getHostAgentStatus(host)).thenReturn(Status.Down);

        assertTrue(kvmHAProvider.fence(host));
    }

    @Test
    public void testFenceOobmFailedStorageHeartbeatSeesHostAlive() throws HAFenceException {
        when(outOfBandManagementService.isOutOfBandManagementEnabled(host)).thenReturn(true);
        when(outOfBandManagementService.executePowerOperation(host, PowerOperation.OFF, null)).thenReturn(oobmResponse(false));
        doReturn(true).when(kvmHAProvider).isStorageHeartbeatFencingEnabled(host);
        when(kvmHostActivityChecker.getHostAgentStatus(host)).thenReturn(Status.Disconnected);

        assertFalse(kvmHAProvider.fence(host));
    }

    @Test
    public void testFenceOobmExceptionStorageHeartbeatReportsHostDown() throws HAFenceException {
        when(outOfBandManagementService.isOutOfBandManagementEnabled(host)).thenReturn(true);
        when(outOfBandManagementService.executePowerOperation(host, PowerOperation.OFF, null)).thenThrow(new RuntimeException("BMC unreachable"));
        doReturn(true).when(kvmHAProvider).isStorageHeartbeatFencingEnabled(host);
        when(kvmHostActivityChecker.getHostAgentStatus(host)).thenReturn(Status.Down);

        assertTrue(kvmHAProvider.fence(host));
    }

    @Test(expected = HAFenceException.class)
    public void testFenceOobmExceptionFallbackDisabledThrows() throws HAFenceException {
        when(outOfBandManagementService.isOutOfBandManagementEnabled(host)).thenReturn(true);
        when(outOfBandManagementService.executePowerOperation(host, PowerOperation.OFF, null)).thenThrow(new RuntimeException("BMC unreachable"));
        doReturn(false).when(kvmHAProvider).isStorageHeartbeatFencingEnabled(host);

        kvmHAProvider.fence(host);
    }

}
