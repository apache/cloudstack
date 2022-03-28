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

import com.cloud.upgrade.SystemVmTemplateRegistration;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import java.util.List;

public class Upgrade41610to41700 implements DbUpgrade, DbUpgradeSystemVmTemplate {

    final static Logger LOG = Logger.getLogger(Upgrade41700to41710.class);
    private SystemVmTemplateRegistration systemVmTemplateRegistration;

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.16.1.0", "4.17.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.17.0.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-41610to41700.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        fixWrongDatastoreClusterPoolUuid(conn);
        updateConfigurationGroups(conn);
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-41610to41700-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    private void initSystemVmTemplateRegistration() {
        systemVmTemplateRegistration = new SystemVmTemplateRegistration("");
    }

    @Override
    public void updateSystemVmTemplates(Connection conn) {
        LOG.debug("Updating System Vm template IDs");
        initSystemVmTemplateRegistration();
        try {
            systemVmTemplateRegistration.updateSystemVmTemplates(conn);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to find / register SystemVM template(s)");
        }
    }

    public void fixWrongDatastoreClusterPoolUuid(Connection conn) {
        LOG.debug("Replacement of faulty pool uuids on datastorecluster");
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT id,uuid FROM storage_pool "
                + "WHERE uuid NOT LIKE \"%-%-%-%\" AND removed IS NULL "
                + "AND pool_type = 'DatastoreCluster';"); ResultSet rs = pstmt.executeQuery()) {
            PreparedStatement updateStmt = conn.prepareStatement("update storage_pool set uuid = ? where id = ?");
            while (rs.next()) {
                    UUID poolUuid = new UUID(
                            new BigInteger(rs.getString(2).substring(0, 16), 16).longValue(),
                            new BigInteger(rs.getString(2).substring(16), 16).longValue()
                    );
                    updateStmt.setLong(2, rs.getLong(1));
                    updateStmt.setString(1, poolUuid.toString());
                    updateStmt.addBatch();
            }
            updateStmt.executeBatch();
        } catch (SQLException ex) {
            String errorMsg = "fixWrongPoolUuid:Exception while updating faulty pool uuids";
            LOG.error(errorMsg,ex);
            throw new CloudRuntimeException(errorMsg, ex);
        }
    }

    private void updateConfigurationGroups(Connection conn) {
        LOG.debug("Updating configuration groups");
        try {
            String stmt = "SELECT name FROM `cloud`.`configuration`";
            PreparedStatement pstmt = conn.prepareStatement(stmt);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String configName = rs.getString(1);
                if (StringUtils.isBlank(configName)) {
                    continue;
                }

                // Get words from the dot notation in the configuration
                String[] nameWords = configName.split("\\.");
                if (nameWords.length <= 0) {
                    continue;
                }

                for (int index = 0; index < nameWords.length; index++) {
                    Pair<Long, Long> configGroupAndSubGroup = getConfigurationGroupAndSubGroup(conn, nameWords[index]);
                    if (configGroupAndSubGroup.first() != 1 && configGroupAndSubGroup.second() != 1) {
                        stmt = "UPDATE `cloud`.`configuration` SET group_id = ?, subgroup_id = ? WHERE name = ?";
                        pstmt = conn.prepareStatement(stmt);
                        pstmt.setLong(1, configGroupAndSubGroup.first());
                        pstmt.setLong(2, configGroupAndSubGroup.second());
                        pstmt.setString(3, configName);
                        pstmt.executeUpdate();
                        break;
                    }
                }
            }

            rs.close();
            pstmt.close();
            LOG.debug("Successfully updated configuration groups.");
        } catch (SQLException e) {
            String errorMsg = "Failed to update configuration groups due to " + e.getMessage();
            LOG.error(errorMsg, e);
            throw new CloudRuntimeException(errorMsg, e);
        }
    }

    private Pair<Long, Long> getConfigurationGroupAndSubGroup(Connection conn, String name) {
        Long subGroupId = 1L;
        Long groupId = 1L;
        try {
            String stmt = "SELECT id, group_id FROM `cloud`.`configuration_subgroup` WHERE name = ?";
            PreparedStatement pstmt = conn.prepareStatement(stmt);
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                subGroupId = rs.getLong(1);
                groupId = rs.getLong(2);
            } else {
                // Try with keywords in the configuration subgroup
                stmt = "SELECT id, group_id, keywords FROM `cloud`.`configuration_subgroup` WHERE keywords IS NOT NULL";
                pstmt = conn.prepareStatement(stmt);
                ResultSet rsConfigurationSubGroups = pstmt.executeQuery();
                while (rsConfigurationSubGroups.next()) {
                    Long keywordsSubGroupId = rsConfigurationSubGroups.getLong(1);
                    Long keywordsGroupId = rsConfigurationSubGroups.getLong(2);
                    String keywords = rsConfigurationSubGroups.getString(3);
                    if(StringUtils.isBlank(keywords)) {
                        continue;
                    }

                    String[] configKeywords = keywords.split(",");
                    if (configKeywords.length <= 0) {
                        continue;
                    }

                    List<String> keywordsList = Arrays.asList(configKeywords);
                    for (String configKeyword : keywordsList) {
                        if (StringUtils.isNotBlank(configKeyword)) {
                            configKeyword = configKeyword.strip();
                            if (configKeyword.equalsIgnoreCase(name) || configKeyword.toLowerCase().startsWith(name.toLowerCase())) {
                                subGroupId = keywordsSubGroupId;
                                groupId = keywordsGroupId;
                                return new Pair<Long, Long>(groupId, subGroupId);
                            }
                        }
                    }
                }
                rsConfigurationSubGroups.close();
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            LOG.error("Failed to get configuration subgroup due to " + e.getMessage(), e);
        }

        return new Pair<Long, Long>(groupId, subGroupId);
    }
}
