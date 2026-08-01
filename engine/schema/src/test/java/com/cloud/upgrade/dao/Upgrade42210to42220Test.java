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
package com.cloud.upgrade.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.cloud.network.Network;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.utils.exception.CloudRuntimeException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Upgrade42210to42220Test {

    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement deleteVpcMapping;
    @Mock
    private PreparedStatement deleteOfferingMapping;

    private Upgrade42210to42220 upgrade;

    @Before
    public void setUp() throws SQLException {
        upgrade = new Upgrade42210to42220();
        when(connection.prepareStatement(Upgrade42210to42220.DELETE_VPC_SERVICE_MAPPING)).thenReturn(deleteVpcMapping);
        when(connection.prepareStatement(Upgrade42210to42220.DELETE_VPC_OFFERING_SERVICE_MAPPING)).thenReturn(deleteOfferingMapping);
    }

    @Test
    public void testVersionRange() {
        assertArrayEquals(new String[] {"4.22.1.0", "4.22.2.0"}, upgrade.getUpgradableVersionRange());
        assertEquals("4.22.2.0", upgrade.getUpgradedVersion());
        assertEquals(0, upgrade.getPrepareScripts().length);
        assertEquals(0, upgrade.getCleanupScripts().length);
    }

    @Test
    public void testPerformDataMigrationRemovesOnlySeededNsxVpnMappings() throws SQLException {
        when(deleteVpcMapping.executeUpdate()).thenReturn(2);
        when(deleteOfferingMapping.executeUpdate()).thenReturn(1);

        upgrade.performDataMigration(connection);

        verifyParameters(deleteVpcMapping);
        verifyParameters(deleteOfferingMapping);
        InOrder executionOrder = inOrder(deleteVpcMapping, deleteOfferingMapping);
        executionOrder.verify(deleteVpcMapping).executeUpdate();
        executionOrder.verify(deleteOfferingMapping).executeUpdate();
    }

    @Test
    public void testPerformDataMigrationIsSafeWhenMappingsAreAlreadyAbsent() throws SQLException {
        when(deleteVpcMapping.executeUpdate()).thenReturn(0);
        when(deleteOfferingMapping.executeUpdate()).thenReturn(0);

        upgrade.performDataMigration(connection);

        verify(deleteVpcMapping).executeUpdate();
        verify(deleteOfferingMapping).executeUpdate();
    }

    @Test
    public void testPerformDataMigrationFailsUpgradeOnDatabaseError() throws SQLException {
        SQLException cause = new SQLException("database failure");
        when(deleteVpcMapping.executeUpdate()).thenThrow(cause);

        CloudRuntimeException exception = assertThrows(CloudRuntimeException.class,
                () -> upgrade.performDataMigration(connection));

        assertEquals(cause, exception.getCause());
    }

    private void verifyParameters(PreparedStatement statement) throws SQLException {
        verify(statement).setString(1, VpcOffering.DEFAULT_VPC_NAT_NSX_OFFERING_NAME);
        verify(statement).setString(2, Network.Service.Vpn.getName());
        verify(statement).setString(3, Network.Provider.Nsx.getName());
    }
}
