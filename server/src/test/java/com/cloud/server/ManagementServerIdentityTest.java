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

package com.cloud.server;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.net.MacAddress;

// Check that the server and its stats jobs use the saved ID
public class ManagementServerIdentityTest {
    // Use different values for the saved ID and the ID from the network card
    private static final long SAVED_ID = 123L;
    private static final long CHANGED_MAC_ID = 456L;

    // Return the saved ID even when the network card gives a different ID
    @Test
    public void managementServiceReportsSavedIdAfterMacChanges() {
        try (MockedStatic<ManagementServerNode> node = mockStatic(ManagementServerNode.class);
                MockedStatic<MacAddress> mac = mockStatic(MacAddress.class)) {
            node.when(ManagementServerNode::getManagementServerId).thenReturn(SAVED_ID);
            mac.when(MacAddress::getMacAddress).thenReturn(new MacAddress(CHANGED_MAC_ID));

            ManagementServerImpl server = mock(ManagementServerImpl.class, Mockito.CALLS_REAL_METHODS);
            assertEquals(SAVED_ID, server.getId());
        }
    }

    // Still start the disk stats job when the saved ID says this server is in charge
    @Test
    public void owningServerStillUpdatesDiskStatsAfterMacChanges() {
        StatsCollector previous = StatsCollector.getInstance();
        try (MockedStatic<ManagementServerNode> node = mockStatic(ManagementServerNode.class);
                MockedStatic<MacAddress> mac = mockStatic(MacAddress.class);
                MockedStatic<GlobalLock> locks = mockStatic(GlobalLock.class);
                MockedStatic<Transaction> transactions = mockStatic(Transaction.class)) {
            node.when(ManagementServerNode::getManagementServerId).thenReturn(SAVED_ID);
            mac.when(MacAddress::getMacAddress).thenReturn(new MacAddress(CHANGED_MAC_ID));
            GlobalLock lock = mock(GlobalLock.class);
            locks.when(() -> GlobalLock.getInternLock("vm.disk.stats")).thenReturn(lock);
            when(lock.lock(5)).thenReturn(true);

            // Pretend the database says this server should do the job
            ManagementServerHostVO host = new ManagementServerHostVO();
            host.setMsid(SAVED_ID);
            ManagementServerHostDao dao = mock(ManagementServerHostDao.class);
            when(dao.findOneInUpState(any(Filter.class))).thenReturn(host);
            StatsCollector collector = new StatsCollector();
            ReflectionTestUtils.setField(collector, "managementServerHostDao", dao);

            collector.new VmDiskStatsUpdaterTask().runInContext();

            transactions.verify(() -> Transaction.execute(any(TransactionCallbackNoReturn.class)));
        } finally {
            // Put the old stats collector back so other tests are not affected
            ReflectionTestUtils.setField(StatsCollector.class, "s_instance", previous);
        }
    }
}
