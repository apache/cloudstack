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

    public static void addIndexIfNeeded(Connection conn, String tableName, String... columnNames) {
        String indexName = dao.generateIndexName(tableName, columnNames);

        if (!dao.indexExists(conn, tableName, indexName)) {
            dao.createIndex(conn, tableName, indexName, columnNames);
        }
    }

    public static void renameIndexIfNeeded(Connection conn, String tableName, String oldName, String newName) {
        if (!dao.indexExists(conn, tableName, oldName)) {
            dao.renameIndex(conn, tableName, oldName, newName);
        }
    }

    public static void addForeignKey(Connection conn, String tableName, String tableColumn, String foreignTableName, String foreignColumnName) {
        dao.addForeignKey(conn, tableName, tableColumn, foreignTableName, foreignColumnName);
    }
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

    public static String getTableColumnType(Connection conn, String tableName, String columnName) {
        return dao.getColumnType(conn, tableName, columnName);
    }

    public static void addTableColumnIfNotExist(Connection conn, String tableName, String columnName, String columnDefinition) {
        if (!dao.columnExists(conn, tableName, columnName)) {
            dao.addColumn(conn, tableName, columnName, columnDefinition);
        }
    }

    public static void changeTableColumnIfNotExist(Connection conn, String tableName, String oldColumnName, String newColumnName, String columnDefinition) {
        if (dao.columnExists(conn, tableName, oldColumnName)) {
            dao.changeColumn(conn, tableName, oldColumnName, newColumnName, columnDefinition);
        }
    }

}
