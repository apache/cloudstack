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

import com.cloud.upgrade.SystemVmTemplateRegistration;
import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade41520to41600 implements DbUpgrade, DbUpgradeSystemVmTemplate {

    final static Logger LOG = Logger.getLogger(Upgrade41520to41600.class);
    private SystemVmTemplateRegistration systemVmTemplateRegistration;

    public Upgrade41520to41600() {
    }

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.15.2.0", "4.16.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.16.0.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-41520to41600.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        generateUuidForExistingSshKeyPairs(conn);
    }

    private void generateUuidForExistingSshKeyPairs(Connection conn) {
        LOG.debug("Generating uuid for existing ssh key-pairs");
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM `cloud`.`ssh_keypairs` WHERE uuid is null");
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                long sshKeyId = rs.getLong(1);
                pstmt = conn.prepareStatement("UPDATE `cloud`.`ssh_keypairs` SET `uuid` = UUID() WHERE id = ?");
                pstmt.setLong(1, sshKeyId);
                pstmt.executeUpdate();
            }
            if (!rs.isClosed())  {
                rs.close();
            }
            if (!pstmt.isClosed())  {
                pstmt.close();
            }
            LOG.debug("Successfully generated uuid for existing ssh key-pairs");
        } catch (SQLException e) {
            String errMsg = "Exception while generating uuid for existing ssh key-pairs: " + e.getMessage();
            LOG.error(errMsg, e);
            throw new CloudRuntimeException(errMsg, e);
        }
    }

    private void initSystemVmTemplateRegistration() {
        systemVmTemplateRegistration = new SystemVmTemplateRegistration();
    }

    @Override
    @SuppressWarnings("serial")
    public void updateSystemVmTemplates(final Connection conn) {
        LOG.debug("Updating System Vm template IDs");
        initSystemVmTemplateRegistration();
        try {
            systemVmTemplateRegistration.updateSystemVmTemplates(conn);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to find / register SystemVM template(s)");
        }
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-41520to41600-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }
}
