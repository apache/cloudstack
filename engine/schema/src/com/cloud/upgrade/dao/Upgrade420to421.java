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

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade420to421 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade420to421.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.2.0", "4.2.1"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.2.1";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-420to421.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-420to421.sql");
        }

        return new File[] {new File(script)};
    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }

    @Override
    public void performDataMigration(Connection conn) {
        upgradeResourceCount(conn);
        updateOverprovisioningPerVm(conn);
    }



    private void updateOverprovisioningPerVm(Connection conn) {
        // Get cpu overprovisioning factor from global setting and update user vm details table for all the vms if factor > 1

        try (PreparedStatement selectConfiguration = conn.prepareStatement("select value from `cloud`.`configuration` where name=?");) {
            String cpuoverprov = "1";
            selectConfiguration.setString(1, "cpu.overprovisioning.factor");
            try (ResultSet configData = selectConfiguration.executeQuery()) {
                if (configData.next()) {
                    cpuoverprov = configData.getString(1);
                }
            }
            String memoverprov = "1";
            selectConfiguration.setString(1, "mem.overprovisioning.factor");
            try (ResultSet configData = selectConfiguration.executeQuery()) {
                if (configData.next()) {
                    memoverprov = configData.getString(1);
                }
            }
            // Need to populate only when overprovisioning factor doesn't pre exist.
            s_logger.debug("Starting updating user_vm_details with cpu/memory overprovisioning factors");
            try (
                    PreparedStatement pstmt2 = conn
                            .prepareStatement("select id, hypervisor_type from `cloud`.`vm_instance` where removed is null and id not in (select vm_id from  `cloud`.`user_vm_details` where name='cpuOvercommitRatio')");
                    PreparedStatement pstmt3 = conn.prepareStatement("INSERT IGNORE INTO cloud.user_vm_details (vm_id, name, value) VALUES (?, ?, ?)");
                    ResultSet result2 = pstmt2.executeQuery();) {
                while (result2.next()) {
                    String hypervisor_type = result2.getString(2);
                    if (hypervisor_type.equalsIgnoreCase(Hypervisor.HypervisorType.VMware.name())) {
                        //For cpu
                        pstmt3.setLong(1, result2.getLong(1));
                        pstmt3.setString(2, "cpuOvercommitRatio");
                        pstmt3.setString(3, cpuoverprov);
                        pstmt3.executeUpdate();
                        // For memory
                        pstmt3.setLong(1, result2.getLong(1));
                        pstmt3.setString(2, "memoryOvercommitRatio");
                        pstmt3.setString(3, memoverprov); // memory overprovisioning was used to reserve memory in case of VMware.
                        pstmt3.executeUpdate();
                    } else {
                        //For cpu
                        pstmt3.setLong(1, result2.getLong(1));
                        pstmt3.setString(2, "cpuOvercommitRatio");
                        pstmt3.setString(3, cpuoverprov);
                        pstmt3.executeUpdate();

                        // For memory
                        pstmt3.setLong(1, result2.getLong(1));
                        pstmt3.setString(2, "memoryOvercommitRatio");
                        pstmt3.setString(3, "1"); // memory overprovisioning didn't exist earlier.
                        pstmt3.executeUpdate();
                    }
                }
            }
            s_logger.debug("Done updating user_vm_details with cpu/memory overprovisioning factors");
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update cpu/memory overprovisioning factors", e);
        }
    }

    private void upgradeResourceCount(Connection conn) {
        s_logger.debug("upgradeResourceCount start");
        String sqlSelectAccountIds = "select id, domain_id FROM `cloud`.`account` where removed is NULL ";
        String sqlSelectOfferingTotals = "SELECT SUM(service_offering.cpu), SUM(service_offering.ram_size)"
                + " FROM `cloud`.`vm_instance`, `cloud`.`service_offering`"
                + " WHERE vm_instance.service_offering_id = service_offering.id AND vm_instance.account_id = ?"
                + " AND vm_instance.removed is NULL"
                + " AND vm_instance.vm_type='User' AND state not in ('Destroyed', 'Error', 'Expunging')";
        String sqlSelectTotalVolumeSize =
                "SELECT sum(size) FROM `cloud`.`volumes` WHERE account_id= ?"
                + " AND (path is not NULL OR state in ('Allocated')) AND removed is NULL"
                + " AND instance_id IN (SELECT id FROM `cloud`.`vm_instance` WHERE vm_type='User')";
        String sqlSelectTotalPathlessVolumeSize =
                "SELECT sum(size) FROM `cloud`.`volumes` WHERE account_id= ?"
                + " AND path is NULL AND state not in ('Allocated') AND removed is NULL";
        String sqlSelectTotalSnapshotSize = "SELECT sum(size) FROM `cloud`.`snapshots` WHERE account_id= ? AND removed is NULL";
        String sqlSelectTotalTemplateStoreSize = "SELECT sum(template_store_ref.size) FROM `cloud`.`template_store_ref`,`cloud`.`vm_template` WHERE account_id = ?"
                + " AND template_store_ref.template_id = vm_template.id AND download_state = 'DOWNLOADED' AND destroyed = false AND removed is NULL";
        String sqlSelectDomainIds = "select id FROM `cloud`.`domain`";
        String sqlSelectAccountCount = "select account.domain_id,sum(resource_count.count) from `cloud`.`account` left join `cloud`.`resource_count` on account.id=resource_count.account_id "
                + "where resource_count.type=? group by account.domain_id;";

        try (
                PreparedStatement pstmtSelectAccountIds = conn.prepareStatement(sqlSelectAccountIds);
                PreparedStatement pstmtSelectOfferingTotals = conn.prepareStatement(sqlSelectOfferingTotals);
                PreparedStatement pstmtSelectTotalVolumeSize = conn.prepareStatement(sqlSelectTotalVolumeSize);
                PreparedStatement pstmtSelectTotalPathlessVolumeSize = conn.prepareStatement(sqlSelectTotalPathlessVolumeSize);
                PreparedStatement pstmtSelectTotalSnapshotSize = conn.prepareStatement(sqlSelectTotalSnapshotSize);
                PreparedStatement pstmtSelectTotalTemplateStoreSize = conn.prepareStatement(sqlSelectTotalTemplateStoreSize);
                PreparedStatement pstmtSelectDomainIds = conn.prepareStatement(sqlSelectDomainIds);
                PreparedStatement pstmtSelectAccountCount = conn.prepareStatement(sqlSelectAccountCount);
                ResultSet rsAccount = pstmtSelectAccountIds.executeQuery();
            ) {
            while (rsAccount.next()) {
                long account_id = rsAccount.getLong(1);
                long domain_id = rsAccount.getLong(2);
                // 1. update cpu,memory for all accounts
                pstmtSelectOfferingTotals.setLong(1, account_id);
                try (ResultSet rsOfferingTotals = pstmtSelectOfferingTotals.executeQuery();) {
                    if (rsOfferingTotals.next()) {
                        upgradeResourceCountforAccount(conn, account_id, domain_id, "cpu", rsOfferingTotals.getLong(1));
                        upgradeResourceCountforAccount(conn, account_id, domain_id, "memory", rsOfferingTotals.getLong(2));
                    } else {
                        upgradeResourceCountforAccount(conn, account_id, domain_id, "cpu", 0L);
                        upgradeResourceCountforAccount(conn, account_id, domain_id, "memory", 0L);
                    }
                }

                // 2. update primary_storage for all accounts
                pstmtSelectTotalVolumeSize.setLong(1, account_id);
                try (ResultSet rsTotalVolumeSize = pstmtSelectTotalVolumeSize.executeQuery();) {
                    if (rsTotalVolumeSize.next()) {
                        upgradeResourceCountforAccount(conn, account_id, domain_id, "primary_storage", rsTotalVolumeSize.getLong(1));
                    } else {
                        upgradeResourceCountforAccount(conn, account_id, domain_id, "primary_storage", 0L);
                    }
                }

                // 3. update secondary_storage for all accounts
                long totalVolumesSize = 0;
                long totalSnapshotsSize = 0;
                long totalTemplatesSize = 0;
                pstmtSelectTotalPathlessVolumeSize.setLong(1, account_id);
                try (ResultSet rsTotalPathlessVolumeSize = pstmtSelectTotalPathlessVolumeSize.executeQuery();) {
                    if (rsTotalPathlessVolumeSize.next()) {
                        totalVolumesSize = rsTotalPathlessVolumeSize.getLong(1);
                    }
                }

                pstmtSelectTotalSnapshotSize.setLong(1, account_id);
                try (ResultSet rsTotalSnapshotSize = pstmtSelectTotalSnapshotSize.executeQuery();) {
                    if (rsTotalSnapshotSize.next()) {
                        totalSnapshotsSize = rsTotalSnapshotSize.getLong(1);
                    }
                }
                pstmtSelectTotalTemplateStoreSize.setLong(1, account_id);
                try (ResultSet rsTotalTemplateStoreSize = pstmtSelectTotalTemplateStoreSize.executeQuery();) {
                    if (rsTotalTemplateStoreSize.next()) {
                        totalTemplatesSize = rsTotalTemplateStoreSize.getLong(1);
                    }
                }
                upgradeResourceCountforAccount(conn, account_id, domain_id, "secondary_storage", totalVolumesSize + totalSnapshotsSize + totalTemplatesSize);
            }
            rsAccount.close();

            // 4. upgrade cpu,memory,primary_storage,secondary_storage for domains
            String resource_types[] = {"cpu", "memory", "primary_storage", "secondary_storage"};
            try (ResultSet rsDomainIds = pstmtSelectDomainIds.executeQuery();) {
                while (rsDomainIds.next()) {
                    long domain_id = rsDomainIds.getLong(1);
                    for (int count = 0; count < resource_types.length; count++) {
                        String resource_type = resource_types[count];
                        upgradeResourceCountforDomain(conn, domain_id, resource_type, 0L); // reset value to 0 before statistics
                    }
                }
            }
            for (int count = 0; count < resource_types.length; count++) {
                String resource_type = resource_types[count];
                pstmtSelectAccountCount.setString(1, resource_type);
                try (ResultSet rsAccountCount = pstmtSelectAccountCount.executeQuery();) {
                    while (rsAccountCount.next()) {
                        long domain_id = rsAccountCount.getLong(1);
                        long resource_count = rsAccountCount.getLong(2);
                        upgradeResourceCountforDomain(conn, domain_id, resource_type, resource_count);
                    }
                }
            }
            s_logger.debug("upgradeResourceCount finish");
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to upgrade resource count (cpu,memory,primary_storage,secondary_storage) ", e);
        }
    }

    private static void upgradeResourceCountforAccount(Connection conn, Long accountId, Long domainId, String type, Long resourceCount) throws SQLException {
        //update or insert into resource_count table.
        String sqlInsertResourceCount = "INSERT INTO `cloud`.`resource_count` (account_id, type, count) VALUES (?,?,?) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id), count=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlInsertResourceCount);) {
            pstmt.setLong(1, accountId);
            pstmt.setString(2, type);
            pstmt.setLong(3, resourceCount);
            pstmt.setLong(4, resourceCount);
            pstmt.executeUpdate();
        }
    }

    private static void upgradeResourceCountforDomain(Connection conn, Long domainId, String type, Long resourceCount) throws SQLException {
        //update or insert into resource_count table.
        String sqlInsertResourceCount = "INSERT INTO `cloud`.`resource_count` (domain_id, type, count) VALUES (?,?,?) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id), count=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlInsertResourceCount);) {
            pstmt.setLong(1, domainId);
            pstmt.setString(2, type);
            pstmt.setLong(3, resourceCount);
            pstmt.setLong(4, resourceCount);
            pstmt.executeUpdate();
        }
    }
}
