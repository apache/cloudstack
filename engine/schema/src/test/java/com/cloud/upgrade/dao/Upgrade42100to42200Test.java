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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.inOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class Upgrade42100to42200Test {

    @Spy
    Upgrade42100to42200 upgrade;

    @Mock
    private Connection conn;

    @Mock
    private PreparedStatement selectStmt;

    @Mock
    private PreparedStatement updateStmt;

    @Mock
    private ResultSet resultSet;

    @Test
    public void testUpdateSnapshotPolicyOwnership() throws SQLException {
        // Setup mock data for snapshot policies without ownership
        when(conn.prepareStatement("SELECT sp.id, v.account_id, v.domain_id FROM snapshot_policy sp, volumes v WHERE sp.volume_id = v.id AND (sp.account_id IS NULL AND sp.domain_id IS NULL)"))
                .thenReturn(selectStmt);
        when(conn.prepareStatement("UPDATE snapshot_policy SET account_id = ?, domain_id = ? WHERE id = ?"))
                .thenReturn(updateStmt);
        when(selectStmt.executeQuery()).thenReturn(resultSet);

        when(resultSet.next())
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        when(resultSet.getLong(1))
                .thenReturn(1L)
                .thenReturn(2L);
        when(resultSet.getLong(2))
                .thenReturn(100L)
                .thenReturn(200L);
        when(resultSet.getLong(3))
                .thenReturn(1L)
                .thenReturn(2L);

        upgrade.updateSnapshotPolicyOwnership(conn);

        verify(conn).prepareStatement("SELECT sp.id, v.account_id, v.domain_id FROM snapshot_policy sp, volumes v WHERE sp.volume_id = v.id AND (sp.account_id IS NULL AND sp.domain_id IS NULL)");
        verify(conn).prepareStatement("UPDATE snapshot_policy SET account_id = ?, domain_id = ? WHERE id = ?");

        InOrder inOrder = inOrder(updateStmt);

        inOrder.verify(updateStmt).setLong(1, 100L); // account_id
        inOrder.verify(updateStmt).setLong(2, 1L);   // domain_id
        inOrder.verify(updateStmt).setLong(3, 1L);   // policy_id
        inOrder.verify(updateStmt).executeUpdate();

        inOrder.verify(updateStmt).setLong(1, 200L); // account_id
        inOrder.verify(updateStmt).setLong(2, 2L);   // domain_id
        inOrder.verify(updateStmt).setLong(3, 2L);   // policy_id
        inOrder.verify(updateStmt).executeUpdate();

        verify(updateStmt, times(2)).executeUpdate();
    }

    @Test
    public void testUpdateBackupScheduleOwnership() throws SQLException {
        when(conn.prepareStatement("SELECT bs.id, vm.account_id, vm.domain_id FROM backup_schedule bs, vm_instance vm WHERE bs.vm_id = vm.id AND (bs.account_id IS NULL AND bs.domain_id IS NULL)"))
                .thenReturn(selectStmt);
        when(conn.prepareStatement("UPDATE backup_schedule SET account_id = ?, domain_id = ? WHERE id = ?"))
                .thenReturn(updateStmt);
        when(selectStmt.executeQuery()).thenReturn(resultSet);

        when(resultSet.next())
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        when(resultSet.getLong(1))
                .thenReturn(10L)
                .thenReturn(20L)
                .thenReturn(30L);
        when(resultSet.getLong(2))
                .thenReturn(500L)
                .thenReturn(600L)
                .thenReturn(700L);
        when(resultSet.getLong(3))
                .thenReturn(5L)
                .thenReturn(6L)
                .thenReturn(7L);

        upgrade.updateBackupScheduleOwnership(conn);

        verify(conn).prepareStatement("SELECT bs.id, vm.account_id, vm.domain_id FROM backup_schedule bs, vm_instance vm WHERE bs.vm_id = vm.id AND (bs.account_id IS NULL AND bs.domain_id IS NULL)");
        verify(conn).prepareStatement("UPDATE backup_schedule SET account_id = ?, domain_id = ? WHERE id = ?");

        InOrder inOrder = inOrder(updateStmt);

        inOrder.verify(updateStmt).setLong(1, 500L);
        inOrder.verify(updateStmt).setLong(2, 5L);
        inOrder.verify(updateStmt).setLong(3, 10L);
        inOrder.verify(updateStmt).executeUpdate();

        inOrder.verify(updateStmt).setLong(1, 600L);
        inOrder.verify(updateStmt).setLong(2, 6L);
        inOrder.verify(updateStmt).setLong(3, 20L);
        inOrder.verify(updateStmt).executeUpdate();

        inOrder.verify(updateStmt).setLong(1, 700L);
        inOrder.verify(updateStmt).setLong(2, 7L);
        inOrder.verify(updateStmt).setLong(3, 30L);
        inOrder.verify(updateStmt).executeUpdate();

        verify(updateStmt, times(3)).executeUpdate();
    }

    @Test
    public void testUpdateSnapshotPolicyOwnershipNoResults() throws SQLException {
        when(conn.prepareStatement("SELECT sp.id, v.account_id, v.domain_id FROM snapshot_policy sp, volumes v WHERE sp.volume_id = v.id AND (sp.account_id IS NULL AND sp.domain_id IS NULL)"))
                .thenReturn(selectStmt);
        when(conn.prepareStatement("UPDATE snapshot_policy SET account_id = ?, domain_id = ? WHERE id = ?"))
                .thenReturn(updateStmt);
        when(selectStmt.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(false);

        upgrade.updateSnapshotPolicyOwnership(conn);

        verify(selectStmt).executeQuery();
        verify(updateStmt, times(0)).executeUpdate();
    }

    @Test
    public void testUpdateBackupScheduleOwnershipNoResults() throws SQLException {
        when(conn.prepareStatement("SELECT bs.id, vm.account_id, vm.domain_id FROM backup_schedule bs, vm_instance vm WHERE bs.vm_id = vm.id AND (bs.account_id IS NULL AND bs.domain_id IS NULL)"))
                .thenReturn(selectStmt);
        when(conn.prepareStatement("UPDATE backup_schedule SET account_id = ?, domain_id = ? WHERE id = ?"))
                .thenReturn(updateStmt);
        when(selectStmt.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(false);

        upgrade.updateBackupScheduleOwnership(conn);

        verify(selectStmt).executeQuery();
        verify(updateStmt, times(0)).executeUpdate();
    }

    @Test
    public void testPerformDataMigration() throws SQLException {
        when(conn.prepareStatement(anyString())).thenReturn(selectStmt);
        when(selectStmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        upgrade.performDataMigration(conn);

        verify(conn).prepareStatement("SELECT sp.id, v.account_id, v.domain_id FROM snapshot_policy sp, volumes v WHERE sp.volume_id = v.id AND (sp.account_id IS NULL AND sp.domain_id IS NULL)");
        verify(conn).prepareStatement("SELECT bs.id, vm.account_id, vm.domain_id FROM backup_schedule bs, vm_instance vm WHERE bs.vm_id = vm.id AND (bs.account_id IS NULL AND bs.domain_id IS NULL)");
    }

    @Test
    public void testUpdateSnapshotPolicyOwnershipSingleRecord() throws SQLException {
        when(conn.prepareStatement("SELECT sp.id, v.account_id, v.domain_id FROM snapshot_policy sp, volumes v WHERE sp.volume_id = v.id AND (sp.account_id IS NULL AND sp.domain_id IS NULL)"))
                .thenReturn(selectStmt);
        when(conn.prepareStatement("UPDATE snapshot_policy SET account_id = ?, domain_id = ? WHERE id = ?"))
                .thenReturn(updateStmt);
        when(selectStmt.executeQuery()).thenReturn(resultSet);

        when(resultSet.next())
                .thenReturn(true)
                .thenReturn(false);

        when(resultSet.getLong(1)).thenReturn(42L);
        when(resultSet.getLong(2)).thenReturn(999L);
        when(resultSet.getLong(3)).thenReturn(10L);

        upgrade.updateSnapshotPolicyOwnership(conn);

        verify(updateStmt).setLong(1, 999L);
        verify(updateStmt).setLong(2, 10L);
        verify(updateStmt).setLong(3, 42L);
        verify(updateStmt, times(1)).executeUpdate();
    }

    @Test
    public void testUpdateBackupScheduleOwnershipSingleRecord() throws SQLException {
        when(conn.prepareStatement("SELECT bs.id, vm.account_id, vm.domain_id FROM backup_schedule bs, vm_instance vm WHERE bs.vm_id = vm.id AND (bs.account_id IS NULL AND bs.domain_id IS NULL)"))
                .thenReturn(selectStmt);
        when(conn.prepareStatement("UPDATE backup_schedule SET account_id = ?, domain_id = ? WHERE id = ?"))
                .thenReturn(updateStmt);
        when(selectStmt.executeQuery()).thenReturn(resultSet);

        when(resultSet.next())
                .thenReturn(true)
                .thenReturn(false);

        when(resultSet.getLong(1)).thenReturn(55L);
        when(resultSet.getLong(2)).thenReturn(888L);
        when(resultSet.getLong(3)).thenReturn(15L);

        upgrade.updateBackupScheduleOwnership(conn);

        verify(updateStmt).setLong(1, 888L);
        verify(updateStmt).setLong(2, 15L);
        verify(updateStmt).setLong(3, 55L);
        verify(updateStmt, times(1)).executeUpdate();
    }
}
