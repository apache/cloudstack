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
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.framework.config.ConfigKey;

public class Upgrade42010to42100 extends DbUpgradeAbstractImpl implements DbUpgrade, DbUpgradeSystemVmTemplate {
    private SystemVmTemplateRegistration systemVmTemplateRegistration;

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.20.1.0", "4.21.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.21.0.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-42010to42100.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        updateKubernetesClusterNodeVersions(conn);
        migrateConfigurationScopeToBitmask(conn);
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-42010to42100-cleanup.sql";
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
        logger.debug("Updating System Vm template IDs");
        initSystemVmTemplateRegistration();
        try {
            systemVmTemplateRegistration.updateSystemVmTemplates(conn);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to find / register SystemVM template(s)");
        }
    }

    private void updateKubernetesClusterNodeVersions(Connection conn) {
        //get list of all non removed kubernetes clusters
        try {
            Map<Long, String> clusterAndVersion = getKubernetesClusterIdsAndVersion(conn);
            updateKubernetesNodeVersions(conn, clusterAndVersion);
        } catch (Exception e) {
            String errMsg = "Failed to update kubernetes cluster nodes version";
            logger.error(errMsg);
            throw new CloudRuntimeException(errMsg, e);
        }
    }

    private Map<Long, String> getKubernetesClusterIdsAndVersion(Connection conn) {
        String listKubernetesClusters = "SELECT c.id, v.semantic_version FROM `cloud`.`kubernetes_cluster` c JOIN `cloud`.`kubernetes_supported_version` v ON (c.kubernetes_version_id = v.id) WHERE c.removed is NULL;";
        Map<Long, String> clusterAndVersion = new HashMap<>();
        try {
            PreparedStatement pstmt = conn.prepareStatement(listKubernetesClusters);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                clusterAndVersion.put(rs.getLong(1), rs.getString(2));
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            String errMsg = String.format("Failed to get all the kubernetes cluster ids due to: %s", e.getMessage());
            logger.error(errMsg);
            throw new CloudRuntimeException(errMsg, e);
        }
        return clusterAndVersion;
    }

    private void updateKubernetesNodeVersions(Connection conn, Map<Long, String> clusterAndVersion) {
        List<Long> kubernetesClusterVmIds;
        for (Map.Entry<Long, String> clusterVersionEntry : clusterAndVersion.entrySet()) {
            try {
                Long cksClusterId = clusterVersionEntry.getKey();
                String cksVersion = clusterVersionEntry.getValue();
                logger.debug(String.format("Adding CKS version %s to existing CKS cluster %s nodes", cksVersion, cksClusterId));
                kubernetesClusterVmIds = getKubernetesClusterVmMapIds(conn, cksClusterId);
                updateKubernetesNodeVersion(conn, kubernetesClusterVmIds, cksClusterId, cksVersion);
            } catch (Exception e) {
                String errMsg = String.format("Failed to update the node version for kubernetes cluster nodes for the" +
                        " kubernetes cluster with id: %s," +
                        " due to: %s", clusterVersionEntry.getKey(), e.getMessage());
                logger.error(errMsg, e);
                throw new CloudRuntimeException(errMsg, e);
            }
        }
    }

    private List<Long> getKubernetesClusterVmMapIds(Connection conn, Long cksClusterId) {
        List<Long> kubernetesClusterVmIds = new ArrayList<>();
        String getKubernetesClustersVmMap = "SELECT id FROM `cloud`.`kubernetes_cluster_vm_map` WHERE cluster_id = %s;";
        try {
            PreparedStatement pstmt = conn.prepareStatement(String.format(getKubernetesClustersVmMap, cksClusterId));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                kubernetesClusterVmIds.add(rs.getLong(1));
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            String errMsg = String.format("Failed to get the kubernetes cluster vm map IDs for kubernetes cluster with id: %s," +
                    " due to: %s", cksClusterId, e.getMessage());
            logger.error(errMsg, e);
            throw new CloudRuntimeException(errMsg, e);
        }
        return kubernetesClusterVmIds;
    }

    private void updateKubernetesNodeVersion(Connection conn, List<Long> kubernetesClusterVmIds, Long cksClusterId, String cksVersion) {
        String updateKubernetesNodeVersion = "UPDATE `cloud`.`kubernetes_cluster_vm_map` set kubernetes_node_version = ? WHERE id = ?;";
        for (Long nodeVmId : kubernetesClusterVmIds) {
            try {
                PreparedStatement pstmt = conn.prepareStatement(updateKubernetesNodeVersion);
                pstmt.setString(1, cksVersion);
                pstmt.setLong(2, nodeVmId);
                pstmt.executeUpdate();
                pstmt.close();
            } catch (Exception e) {
                String errMsg = String.format("Failed to update the node version for kubernetes cluster nodes for the" +
                        " kubernetes cluster with id: %s," +
                        " due to: %s", cksClusterId, e.getMessage());
                logger.error(errMsg, e);
                throw new CloudRuntimeException(errMsg, e);
            }
        }
    }

    protected void migrateConfigurationScopeToBitmask(Connection conn) {
        String scopeDataType = DbUpgradeUtils.getTableColumnType(conn, "configuration", "scope");
        logger.info("Data type of the column scope of table configuration is {}", scopeDataType);
        if (!"varchar(255)".equals(scopeDataType)) {
            return;
        }
        DbUpgradeUtils.addTableColumnIfNotExist(conn, "configuration", "new_scope", "BIGINT DEFAULT 0");
        migrateExistingConfigurationScopeValues(conn);
        DbUpgradeUtils.dropTableColumnsIfExist(conn, "configuration", List.of("scope"));
        DbUpgradeUtils.changeTableColumnIfNotExist(conn, "configuration", "new_scope", "scope", "BIGINT NOT NULL DEFAULT 0 COMMENT 'Bitmask for scope(s) of this parameter'");
    }

    protected void migrateExistingConfigurationScopeValues(Connection conn) {
        StringBuilder sql = new StringBuilder("UPDATE configuration\n" +
                "SET new_scope = " +
                "    CASE ");
        for (ConfigKey.Scope scope : ConfigKey.Scope.values()) {
            sql.append("        WHEN scope = '").append(scope.name()).append("' THEN ").append(scope.getBitValue()).append(" ");
        }
        sql.append("        ELSE 0 " +
                "    END " +
                "WHERE scope IS NOT NULL;");
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try (PreparedStatement pstmt = txn.prepareAutoCloseStatement(sql.toString())) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to migrate existing configuration scope values to bitmask", e);
            throw new CloudRuntimeException(String.format("Failed to migrate existing configuration scope values to bitmask due to: %s", e.getMessage()));
        }
    }
}
