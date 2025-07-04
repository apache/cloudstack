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
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.db.TransactionLegacy;

@RunWith(MockitoJUnitRunner.class)
public class Upgrade42010to42100Test {
    @Spy
    Upgrade42010to42100 upgrade;

    @Mock
    private Connection conn;

    @Test
    public void testPerformDataMigration() throws SQLException {
        try (MockedStatic<DbUpgradeUtils> ignored = Mockito.mockStatic(DbUpgradeUtils.class)) {
            DbUpgradeUtils dbUpgradeUtils = Mockito.mock(DbUpgradeUtils.class);
            when(dbUpgradeUtils.getTableColumnType(conn, "configuration", "scope")).thenReturn("varchar(255)");

            try (MockedStatic<TransactionLegacy> ignored2 = Mockito.mockStatic(TransactionLegacy.class)) {
                TransactionLegacy txn = Mockito.mock(TransactionLegacy.class);
                when(TransactionLegacy.currentTxn()).thenReturn(txn);
                PreparedStatement pstmt = Mockito.mock(PreparedStatement.class);
                String sql = "UPDATE configuration\n" +
                        "SET new_scope =" +
                        "     CASE" +
                        "         WHEN scope = 'Global' THEN 1" +
                        "         WHEN scope = 'Zone' THEN 2" +
                        "         WHEN scope = 'Cluster' THEN 4" +
                        "         WHEN scope = 'StoragePool' THEN 8" +
                        "         WHEN scope = 'ManagementServer' THEN 16" +
                        "         WHEN scope = 'ImageStore' THEN 32" +
                        "         WHEN scope = 'Domain' THEN 64" +
                        "         WHEN scope = 'Account' THEN 128" +
                        "         ELSE 0" +
                        "     END WHERE scope IS NOT NULL;";
                when(txn.prepareAutoCloseStatement(sql)).thenReturn(pstmt);

                PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
                ResultSet resultSet = Mockito.mock(ResultSet.class);
                Mockito.when(resultSet.next()).thenReturn(false);
                Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
                Mockito.when(conn.prepareStatement(anyString())).thenReturn(preparedStatement);
                upgrade.performDataMigration(conn);

                Mockito.verify(pstmt, Mockito.times(1)).executeUpdate();
            }
        }
    }
}
