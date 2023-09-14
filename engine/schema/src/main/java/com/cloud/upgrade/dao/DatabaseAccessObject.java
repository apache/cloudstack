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
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class DatabaseAccessObject {

    private static Logger s_logger = Logger.getLogger(DatabaseAccessObject.class);

    public void addForeignKey(Connection conn, String tableName, String tableColumn, String foreignTableName, String foreignColumnName) {
        String addForeignKeyStmt = String.format("ALTER TABLE `cloud`.`%s` ADD CONSTRAINT `fk_%s__%s` FOREIGN KEY `fk_%s__%s`(`%s`) REFERENCES `%s`(`%s`)", tableName, tableName, tableColumn, tableName, tableColumn, tableColumn, foreignTableName, foreignColumnName);
        try(PreparedStatement pstmt = conn.prepareStatement(addForeignKeyStmt);)
        {
            pstmt.executeUpdate();
            s_logger.debug(String.format("Foreign key is added successfully from the table %s", tableName));
        } catch (SQLException e) {
            s_logger.error("Ignored SQL Exception when trying to add foreign key on table "  + tableName + " exception: " + e.getMessage());
        }
    }

    public void dropKey(Connection conn, String tableName, String key, boolean isForeignKey)
    {
        String alter_sql_str;
        if (isForeignKey) {
            alter_sql_str = "ALTER TABLE " + tableName + " DROP FOREIGN KEY " + key;
        } else {
            alter_sql_str = "ALTER TABLE " + tableName + " DROP KEY " + key;
        }
        try(PreparedStatement pstmt = conn.prepareStatement(alter_sql_str);)
        {
            pstmt.executeUpdate();
            s_logger.debug("Key " + key + " is dropped successfully from the table " + tableName);
        } catch (SQLException e) {
            s_logger.debug("Ignored SQL Exception when trying to drop " + (isForeignKey ? "foreign " : "") + "key " + key + " on table "  + tableName + " exception: " + e.getMessage());

        }
    }

    public void dropPrimaryKey(Connection conn, String tableName) {
        try(PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE " + tableName + " DROP PRIMARY KEY ");) {
            pstmt.executeUpdate();
            s_logger.debug("Primary key is dropped successfully from the table " + tableName);
        } catch (SQLException e) {
            s_logger.debug("Ignored SQL Exception when trying to drop primary key on table " + tableName + " exception: " + e.getMessage());
        }
    }

    public void dropColumn(Connection conn, String tableName, String columnName) {
        try (PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE " + tableName + " DROP COLUMN " + columnName);){
            pstmt.executeUpdate();
            s_logger.debug("Column " + columnName + " is dropped successfully from the table " + tableName);
        } catch (SQLException e) {
            s_logger.warn("Unable to drop column " + columnName + " due to exception", e);
        }
    }

    public boolean columnExists(Connection conn, String tableName, String columnName) {
        boolean columnExists = false;
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT " + columnName + " FROM " + tableName);){
            pstmt.executeQuery();
            columnExists = true;
        } catch (SQLException e) {
            s_logger.debug("Field " + columnName + " doesn't exist in " + tableName + " ignoring exception: " + e.getMessage());
        }
        return columnExists;
    }

    public String generateIndexName(String tableName, String columnName) {
        return String.format("i_%s__%s", tableName, columnName);
    }

    public boolean indexExists(Connection conn, String tableName, String indexName) {
        try (PreparedStatement pstmt = conn.prepareStatement(String.format("SHOW INDEXES FROM %s where Key_name = \"%s\"", tableName, indexName))) {
            ResultSet result = pstmt.executeQuery();
            if (result.next()) {
                return true;
            }
        } catch (SQLException e) {
            s_logger.debug(String.format("Index %s doesn't exist, ignoring exception:", indexName, e.getMessage()));
        }
        return false;
    }

    public void createIndex(Connection conn, String tableName, String columnName, String indexName) {
        String stmt = String.format("CREATE INDEX %s on %s (%s)", indexName, tableName, columnName);
        s_logger.debug("Statement: " + stmt);
        try (PreparedStatement pstmt = conn.prepareStatement(stmt)) {
            pstmt.execute();
            s_logger.debug(String.format("Created index %s", indexName));
        } catch (SQLException e) {
            s_logger.warn(String.format("Unable to create index %s", indexName), e);
        }
    }

    protected static void closePreparedStatement(PreparedStatement pstmt, String errorMessage) {
        try {
            if (pstmt != null) {
                pstmt.close();
            }
        } catch (SQLException e) {
            s_logger.warn(errorMessage, e);
        }
    }
}
