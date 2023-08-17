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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.ha.provider.HACheckerException;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;

@RunWith(MockitoJUnitRunner.class)
public class KVMHostHATest {

    @Mock
    private Host host;
    @Mock
    private KVMHostActivityChecker kvmHostActivityChecker;
    private KVMHAProvider kvmHAProvider;

    @Before
    public void setup() {
        kvmHAProvider = new KVMHAProvider();
        kvmHAProvider.hostActivityChecker = kvmHostActivityChecker;
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

}
