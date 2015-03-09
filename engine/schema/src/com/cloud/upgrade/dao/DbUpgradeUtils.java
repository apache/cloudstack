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
import java.util.List;

public class DbUpgradeUtils {

    private static DatabaseAccessObject dao = new DatabaseAccessObject();

    public static void dropKeysIfExist(Connection conn, String tableName, List<String> keys, boolean isForeignKey) {
        for (String key : keys) {
            dao.dropKey(conn, tableName, key, isForeignKey);
        }
    }

    public static void dropPrimaryKeyIfExists(Connection conn, String tableName) {
        dao.dropPrimaryKey(conn, tableName);
    }

    public static void dropTableColumnsIfExist(Connection conn, String tableName, List<String> columns) {
        for (String columnName : columns) {
            if (dao.columnExists(conn, tableName, columnName)) {
                dao.dropColumn(conn, tableName, columnName);
            }
        }
    }

}
