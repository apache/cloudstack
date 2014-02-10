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
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;

public class DbUpgradeUtils {
    final static Logger s_logger = Logger.getLogger(DbUpgradeUtils.class);

    public static void dropKeysIfExist(Connection conn, String tableName, List<String> keys, boolean isForeignKey) {
        for (String key : keys) {
            PreparedStatement pstmt = null;
            try {
                if (isForeignKey) {
                    pstmt = conn.prepareStatement("ALTER TABLE " + tableName + " DROP FOREIGN KEY " + key);
                } else {
                    pstmt = conn.prepareStatement("ALTER TABLE " + tableName + " DROP KEY " + key);
                }
                pstmt.executeUpdate();
                s_logger.debug("Key " + key + " is dropped successfully from the table " + tableName);
            } catch (SQLException e) {
                // do nothing here

                continue;
            } finally {
                try {
                    if (pstmt != null) {
                        pstmt.close();
                    }
                } catch (SQLException e) {
                }
            }
        }
    }

    public static void dropPrimaryKeyIfExists(Connection conn, String tableName) {
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement("ALTER TABLE " + tableName + " DROP PRIMARY KEY ");
            pstmt.executeUpdate();
            s_logger.debug("Primary key is dropped successfully from the table " + tableName);
        } catch (SQLException e) {
            // do nothing here
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    public static void dropTableColumnsIfExist(Connection conn, String tableName, List<String> columns) {
        PreparedStatement pstmt = null;
        try {
            for (String column : columns) {
                try {
                    pstmt = conn.prepareStatement("SELECT " + column + " FROM " + tableName);
                    pstmt.executeQuery();
                } catch (SQLException e) {
                    // if there is an exception, it means that field doesn't exist, so do nothing here
                    s_logger.trace("Field " + column + " doesn't exist in " + tableName);
                    continue;
                }

                pstmt = conn.prepareStatement("ALTER TABLE " + tableName + " DROP COLUMN " + column);
                pstmt.executeUpdate();
                s_logger.debug("Column " + column + " is dropped successfully from the table " + tableName);
            }
        } catch (SQLException e) {
            s_logger.warn("Unable to drop columns using query " + pstmt + " due to exception", e);
            throw new CloudRuntimeException("Unable to drop columns due to ", e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
    }
}
