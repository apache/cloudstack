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
package com.cloud.upgrade;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RolePermissionChecker {

    protected Logger logger = LogManager.getLogger(getClass());

    private static final String checkAnnotationRulesPermissionPreparedStatement =
            "SELECT permission FROM `cloud`.`role_permissions` WHERE role_id = ? AND rule = ?";
    private static final String insertAnnotationRulePermissionPreparedStatement =
            "INSERT IGNORE INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission) VALUES (UUID(), ?, ?, 'ALLOW')";

    public RolePermissionChecker() {
    }

    public boolean existsRolePermissionByRoleIdAndRule(Connection conn, long roleId, String rule) {
        try {
            PreparedStatement pstmt = conn.prepareStatement(checkAnnotationRulesPermissionPreparedStatement);
            pstmt.setLong(1, roleId);
            pstmt.setString(2, rule);
            ResultSet rs = pstmt.executeQuery();
            return rs != null && rs.next();
        } catch (SQLException e) {
            logger.error("Error on existsRolePermissionByRoleIdAndRule: " + e.getMessage(), e);
            return false;
        }
    }

    public void insertAnnotationRulePermission(Connection conn, long roleId, String rule) {
        try {
            PreparedStatement pstmt = conn.prepareStatement(insertAnnotationRulePermissionPreparedStatement);
            pstmt.setLong(1, roleId);
            pstmt.setString(2, rule);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error on insertAnnotationRulePermission: " + e.getMessage(), e);
        }
    }
}
