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

package com.cloud.cluster.dao;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.management.ManagementServerHost.State;
import org.junit.Test;

import com.cloud.cluster.ManagementServerHostVO;

// Checks that old server records do not make agents skip an available address
public class ManagementServerHostDaoImplTest {
    private final ManagementServerHostDaoImpl dao = spy(new ManagementServerHostDaoImpl());

    // Do not avoid an address if one record says Up and an old record says Down
    @Test
    public void staleDownIdentityDoesNotBlockAnUpServerAtTheSameIp() {
        ManagementServerHostVO down = host("10.0.0.1", State.Down);
        ManagementServerHostVO up = host("10.0.0.1", State.Up);
        assertAvoided(Arrays.asList(down, up));
        assertAvoided(Arrays.asList(up, down));
    }

    // Keep unavailable addresses in the avoid list and include each address only once
    @Test
    public void unavailableAddressesRemainAvoidedWithoutDuplicates() {
        assertAvoided(Arrays.asList(host("10.0.0.1", State.Down), host("10.0.0.1", State.Down),
                host("10.0.0.2", State.Maintenance), host("10.0.0.3", State.Up)), "10.0.0.1", "10.0.0.2");
    }

    // Allow an address marked Up even if an old record says it is in maintenance
    @Test
    public void upServerTakesPrecedenceOverAnOldMaintenanceRecord() {
        assertAvoided(Arrays.asList(host("10.0.0.1", State.Maintenance), host("10.0.0.1", State.Up)));
    }

    // No server records means there are no addresses to avoid
    @Test
    public void emptyServerListHasNoAvoidedAddresses() {
        assertAvoided(Collections.emptyList());
    }

    // Run the avoid-list code with pretend records and check the returned addresses
    private void assertAvoided(List<ManagementServerHostVO> hosts, String... expected) {
        doReturn(hosts).when(dao).listAll();
        assertEquals(Arrays.asList(expected), dao.listNonUpStateMsIPs());
    }

    // Make a pretend server record with an address and a state
    private ManagementServerHostVO host(String ip, State state) {
        ManagementServerHostVO host = new ManagementServerHostVO();
        host.setServiceIP(ip);
        host.setState(state);
        return host;
    }
}
