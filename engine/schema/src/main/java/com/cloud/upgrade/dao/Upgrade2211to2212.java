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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade2211to2212 extends DbUpgradeAbstractImpl {

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"2.2.11", "2.2.11"};
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.12";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-2211to2212.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        createResourceCount(conn);
    }

    @Override
    public InputStream[] getCleanupScripts() {
        return null;
    }

    private void createResourceCount(Connection conn) {
        logger.debug("Creating missing resource_count records as a part of 2.2.11-2.2.12 upgrade");
        try {

            //Get all non removed accounts
            List<Long> accounts = new ArrayList<Long>();
            PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM account");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                accounts.add(rs.getLong(1));
            }
            rs.close();

            //get all non removed domains
            List<Long> domains = new ArrayList<Long>();
            pstmt = conn.prepareStatement("SELECT id FROM domain");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                domains.add(rs.getLong(1));
            }
            rs.close();

            //2.2.12 resource types
            String[] resourceTypes = {"user_vm", "public_ip", "volume", "snapshot", "template"};

            for (Long accountId : accounts) {
                for (String resourceType : resourceTypes) {
                    pstmt = conn.prepareStatement("SELECT * FROM resource_count WHERE type=? and account_id=?");
                    pstmt.setString(1, resourceType);
                    pstmt.setLong(2, accountId);
                    rs = pstmt.executeQuery();
                    if (!rs.next()) {
                        logger.debug("Inserting resource_count record of type " + resourceType + " for account id=" + accountId);
                        pstmt = conn.prepareStatement("INSERT INTO resource_count (account_id, domain_id, type, count) VALUES (?, null, ?, 0)");
                        pstmt.setLong(1, accountId);
                        pstmt.setString(2, resourceType);
                        pstmt.executeUpdate();
                    }
                    rs.close();
                }
                pstmt.close();
            }

            for (Long domainId : domains) {
                for (String resourceType : resourceTypes) {
                    pstmt = conn.prepareStatement("SELECT * FROM resource_count WHERE type=? and domain_id=?");
                    pstmt.setString(1, resourceType);
                    pstmt.setLong(2, domainId);
                    rs = pstmt.executeQuery();
                    if (!rs.next()) {
                        logger.debug("Inserting resource_count record of type " + resourceType + " for domain id=" + domainId);
                        pstmt = conn.prepareStatement("INSERT INTO resource_count (account_id, domain_id, type, count) VALUES (null, ?, ?, 0)");
                        pstmt.setLong(1, domainId);
                        pstmt.setString(2, resourceType);
                        pstmt.executeUpdate();
                    }
                    rs.close();
                }
                pstmt.close();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to create default security groups for existing accounts due to", e);
        }
    }

}
