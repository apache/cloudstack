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
        updateCpuOverprovisioning(conn);
    }

    private void updateCpuOverprovisioning(Connection conn) {
        PreparedStatement pstmt1 = null;
        PreparedStatement pstmt2 = null;
        PreparedStatement pstmt3 = null;
        ResultSet result1 = null;
        ResultSet result2 = null;

        // Get cpu overprovisioning factor from global setting and update user vm details table for all the vms if factor > 1

        try {
            pstmt1 = conn.prepareStatement("select value from `cloud`.`configuration` where name='cpu.overprovisioning.factor'");
            result1 = pstmt1.executeQuery();
            String overprov = "1";
            if (result1.next()) {
                overprov = result1.getString(1);
            }
            // Need to populate only when overprovisioning factor doesn't pre exist.
            s_logger.debug("Starting updating user_vm_details with cpu/memory overprovisioning factors");
            pstmt2 =
                conn.prepareStatement("select id from `cloud`.`vm_instance` where removed is null and id not in (select vm_id from  `cloud`.`user_vm_details` where name='cpuOvercommitRatio')");
            pstmt3 = conn.prepareStatement("INSERT IGNORE INTO cloud.user_vm_details (vm_id, name, value) VALUES (?, ?, ?)");
            result2 = pstmt2.executeQuery();
            while (result2.next()) {
                //For cpu
                pstmt3.setLong(1, result2.getLong(1));
                pstmt3.setString(2, "cpuOvercommitRatio");
                pstmt3.setString(3, overprov);
                pstmt3.executeUpdate();

                // For memory
                pstmt3.setLong(1, result2.getLong(1));
                pstmt3.setString(2, "memoryOvercommitRatio");
                pstmt3.setString(3, "1"); // memory overprovisioning didn't exist earlier.
                pstmt3.executeUpdate();
            }
            s_logger.debug("Done updating user_vm_details with cpu/memory overprovisioning factors");

        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update cpu/memory overprovisioning factors", e);
        } finally {
            try {
                if (pstmt1 != null)
                    pstmt1.close();
                if (pstmt2 != null)
                    pstmt2.close();
                if (pstmt3 != null)
                    pstmt3.close();
            } catch (SQLException e) {
            }
        }

    }

    private void upgradeResourceCount(Connection conn) {
        s_logger.debug("upgradeResourceCount start");
        PreparedStatement pstmt1 = null;
        PreparedStatement pstmt2 = null;
        PreparedStatement pstmt3 = null;
        PreparedStatement pstmt4 = null;
        PreparedStatement pstmt5 = null;
        ResultSet rs = null;
        ResultSet rsAccount = null;
        ResultSet rsCount = null;
        try {
            pstmt1 = conn.prepareStatement("select id, domain_id FROM `cloud`.`account` where removed is NULL ");
            rsAccount = pstmt1.executeQuery();
            while (rsAccount.next()) {
                long account_id = rsAccount.getLong(1);
                long domain_id = rsAccount.getLong(2);
                // 1. update cpu,memory for all accounts
                pstmt2 =
                    conn.prepareStatement("SELECT SUM(service_offering.cpu), SUM(service_offering.ram_size)" + " FROM `cloud`.`vm_instance`, `cloud`.`service_offering`"
                        + " WHERE vm_instance.service_offering_id = service_offering.id AND vm_instance.account_id = ?" + " AND vm_instance.removed is NULL"
                        + " AND vm_instance.vm_type='User' AND state not in ('Destroyed', 'Error', 'Expunging')");
                pstmt2.setLong(1, account_id);
                rsCount = pstmt2.executeQuery();
                if (rsCount.next()) {
                    upgradeResourceCountforAccount(conn, account_id, domain_id, "cpu", rsCount.getLong(1));
                    upgradeResourceCountforAccount(conn, account_id, domain_id, "memory", rsCount.getLong(2));
                } else {
                    upgradeResourceCountforAccount(conn, account_id, domain_id, "cpu", 0L);
                    upgradeResourceCountforAccount(conn, account_id, domain_id, "memory", 0L);
                }
                // 2. update primary_storage for all accounts
                pstmt3 =
                    conn.prepareStatement("SELECT sum(size) FROM `cloud`.`volumes` WHERE account_id= ?"
                        + " AND (path is not NULL OR state in ('Allocated')) AND removed is NULL"
                        + " AND instance_id IN (SELECT id FROM `cloud`.`vm_instance` WHERE vm_type='User')");
                pstmt3.setLong(1, account_id);
                rsCount = pstmt3.executeQuery();
                if (rsCount.next()) {
                    upgradeResourceCountforAccount(conn, account_id, domain_id, "primary_storage", rsCount.getLong(1));
                } else {
                    upgradeResourceCountforAccount(conn, account_id, domain_id, "primary_storage", 0L);
                }
                // 3. update secondary_storage for all accounts
                long totalVolumesSize = 0;
                long totalSnapshotsSize = 0;
                long totalTemplatesSize = 0;
                pstmt4 =
                    conn.prepareStatement("SELECT sum(size) FROM `cloud`.`volumes` WHERE account_id= ?"
                        + " AND path is NULL AND state not in ('Allocated') AND removed is NULL");
                pstmt4.setLong(1, account_id);
                rsCount = pstmt4.executeQuery();
                if (rsCount.next()) {
                    totalVolumesSize = rsCount.getLong(1);
                }
                pstmt4 = conn.prepareStatement("SELECT sum(size) FROM `cloud`.`snapshots` WHERE account_id= ? AND removed is NULL");
                pstmt4.setLong(1, account_id);
                rsCount = pstmt4.executeQuery();
                if (rsCount.next()) {
                    totalSnapshotsSize = rsCount.getLong(1);
                }
                pstmt4 =
                    conn.prepareStatement("SELECT sum(template_store_ref.size) FROM `cloud`.`template_store_ref`,`cloud`.`vm_template` WHERE account_id = ?"
                        + " AND template_store_ref.template_id = vm_template.id AND download_state = 'DOWNLOADED' AND destroyed = false AND removed is NULL");
                pstmt4.setLong(1, account_id);
                rsCount = pstmt4.executeQuery();
                if (rsCount.next()) {
                    totalTemplatesSize = rsCount.getLong(1);
                }
                upgradeResourceCountforAccount(conn, account_id, domain_id, "secondary_storage", totalVolumesSize + totalSnapshotsSize + totalTemplatesSize);
            }
            // 4. upgrade cpu,memory,primary_storage,secondary_storage for domains
            String resource_types[] = {"cpu", "memory", "primary_storage", "secondary_storage"};
            pstmt5 = conn.prepareStatement("select id FROM `cloud`.`domain`");
            rsAccount = pstmt5.executeQuery();
            while (rsAccount.next()) {
                long domain_id = rsAccount.getLong(1);
                for (int count = 0; count < resource_types.length; count++) {
                    String resource_type = resource_types[count];
                    upgradeResourceCountforDomain(conn, domain_id, resource_type, 0L); // reset value to 0 before statistics
                }
            }
            for (int count = 0; count < resource_types.length; count++) {
                String resource_type = resource_types[count];
                pstmt5 =
                    conn.prepareStatement("select account.domain_id,sum(resource_count.count) from `cloud`.`account` left join `cloud`.`resource_count` on account.id=resource_count.account_id "
                        + "where resource_count.type=? group by account.domain_id;");
                pstmt5.setString(1, resource_type);
                rsCount = pstmt5.executeQuery();
                while (rsCount.next()) {
                    long domain_id = rsCount.getLong(1);
                    long resource_count = rsCount.getLong(2);
                    upgradeResourceCountforDomain(conn, domain_id, resource_type, resource_count);
                }
            }
            s_logger.debug("upgradeResourceCount finish");
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to upgrade resource count (cpu,memory,primary_storage,secondary_storage) ", e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (rsAccount != null) {
                    rsAccount.close();
                }
                if (rsCount != null) {
                    rsCount.close();
                }
                if (pstmt1 != null) {
                    pstmt1.close();
                }
                if (pstmt2 != null) {
                    pstmt2.close();
                }
                if (pstmt3 != null) {
                    pstmt3.close();
                }
                if (pstmt4 != null) {
                    pstmt4.close();
                }
                if (pstmt5 != null) {
                    pstmt5.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    private static void upgradeResourceCountforAccount(Connection conn, Long account_id, Long domain_id, String type, Long resource_count) throws SQLException {
        //update or insert into resource_count table.
        PreparedStatement pstmt = null;
        pstmt =
            conn.prepareStatement("INSERT INTO `cloud`.`resource_count` (account_id, type, count) VALUES (?,?,?) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id), count=?");
        pstmt.setLong(1, account_id);
        pstmt.setString(2, type);
        pstmt.setLong(3, resource_count);
        pstmt.setLong(4, resource_count);
        pstmt.executeUpdate();
        pstmt.close();
    }

    private static void upgradeResourceCountforDomain(Connection conn, Long domain_id, String type, Long resource_count) throws SQLException {
        //update or insert into resource_count table.
        PreparedStatement pstmt = null;
        pstmt =
            conn.prepareStatement("INSERT INTO `cloud`.`resource_count` (domain_id, type, count) VALUES (?,?,?) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id), count=?");
        pstmt.setLong(1, domain_id);
        pstmt.setString(2, type);
        pstmt.setLong(3, resource_count);
        pstmt.setLong(4, resource_count);
        pstmt.executeUpdate();
        pstmt.close();
    }
}
