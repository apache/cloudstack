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
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade224to225 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade224to225.class);

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-224to225.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        // create security groups for existing accounts if not present
        createSecurityGroups(conn);
        dropKeysIfExist(conn);
        dropTableColumnsIfExist(conn);
        addMissingKeys(conn);
        addMissingOvsAccount(conn);
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-224to225-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"2.2.4", "2.2.4"};
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.5";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    private void createSecurityGroups(Connection conn) {
        s_logger.debug("Creating missing default security group as a part of 224-225 upgrade");
        try {
            List<Long> accounts = new ArrayList<Long>();
            PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM account WHERE removed IS NULL and id != 1");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                accounts.add(rs.getLong(1));
            }

            for (Long accountId : accounts) {
                // get default security group
                pstmt = conn.prepareStatement("SELECT * FROM security_group WHERE name='default' and account_id=?");
                pstmt.setLong(1, accountId);
                rs = pstmt.executeQuery();
                if (!rs.next()) {
                    s_logger.debug("Default security group is missing for account id=" + accountId + " so adding it");

                    // get accountName/domainId information

                    pstmt = conn.prepareStatement("SELECT account_name, domain_id FROM account WHERE id=?");
                    pstmt.setLong(1, accountId);
                    ResultSet rs1 = pstmt.executeQuery();
                    if (!rs1.next()) {
                        throw new CloudRuntimeException("Unable to create default security group for account id=" + accountId +
                            ": unable to get accountName/domainId info");
                    }
                    String accountName = rs1.getString(1);
                    Long domainId = rs1.getLong(2);

                    pstmt =
                        conn.prepareStatement("INSERT INTO `cloud`.`security_group` (name, description, account_name, account_id, domain_id) VALUES ('default', 'Default Security Group', ?, ?, ?)");
                    pstmt.setString(1, accountName);
                    pstmt.setLong(2, accountId);
                    pstmt.setLong(3, domainId);
                    pstmt.executeUpdate();
                }
                rs.close();
                pstmt.close();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to create default security groups for existing accounts due to", e);
        }
    }

    private void dropTableColumnsIfExist(Connection conn) {
        HashMap<String, List<String>> tablesToModify = new HashMap<String, List<String>>();

        // account table
        List<String> columns = new ArrayList<String>();
        columns.add("network_domain");
        tablesToModify.put("account", columns);

        // console proxy table
        columns = new ArrayList<String>();
        columns.add("gateway");
        columns.add("dns1");
        columns.add("dns2");
        columns.add("domain");
        columns.add("guest_mac_address");
        columns.add("guest_ip_address");
        columns.add("guest_netmask");
        columns.add("vlan_db_id");
        columns.add("vlan_id");
        columns.add("ram_size");
        tablesToModify.put("console_proxy", columns);

        // secondary storage table
        columns = new ArrayList<String>();
        columns.add("gateway");
        columns.add("dns1");
        columns.add("dns2");
        columns.add("domain");
        columns.add("guest_mac_address");
        columns.add("guest_ip_address");
        columns.add("guest_netmask");
        columns.add("vlan_db_id");
        columns.add("vlan_id");
        columns.add("ram_size");
        tablesToModify.put("secondary_storage_vm", columns);

        // disk offering table
        columns = new ArrayList<String>();
        columns.add("mirrored");
        tablesToModify.put("disk_offering", columns);

        // domain router table
        columns = new ArrayList<String>();
        columns.add("gateway");
        columns.add("ram_size");
        columns.add("dns1");
        columns.add("dns2");
        columns.add("domain");
        columns.add("guest_mac_address");
        columns.add("guest_dc_mac_address");
        columns.add("vnet");
        columns.add("dc_vlan");
        columns.add("vlan_db_id");
        columns.add("vlan_id");
        columns.add("dhcp_ip_address");
        tablesToModify.put("domain_router", columns);

        // volumes table
        columns = new ArrayList<String>();
        columns.add("mirror_state");
        columns.add("mirror_vol");
        columns.add("destroyed");
        tablesToModify.put("volumes", columns);

        // vm_instance table
        columns = new ArrayList<String>();
        columns.add("mirrored_vols");
        tablesToModify.put("vm_instance", columns);

        // user_vm table
        columns = new ArrayList<String>();
        columns.add("domain_router_id");
        columns.add("vnet");
        columns.add("dc_vlan");
        columns.add("external_ip_address");
        columns.add("external_mac_address");
        columns.add("external_vlan_db_id");
        tablesToModify.put("user_vm", columns);

        // service_offerings table
        columns = new ArrayList<String>();
        columns.add("guest_ip_type");
        tablesToModify.put("service_offering", columns);

        s_logger.debug("Dropping columns that don't exist in 2.2.5 version of the DB...");
        for (String tableName : tablesToModify.keySet()) {
            DbUpgradeUtils.dropTableColumnsIfExist(conn, tableName, tablesToModify.get(tableName));
        }
    }

    private void dropKeysIfExist(Connection conn) {
        HashMap<String, List<String>> foreignKeys = new HashMap<String, List<String>>();
        HashMap<String, List<String>> indexes = new HashMap<String, List<String>>();

        // console proxy table
        List<String> keys = new ArrayList<String>();
        keys.add("fk_console_proxy__vlan_id");
        foreignKeys.put("console_proxy", keys);

        keys = new ArrayList<String>();
        keys.add("i_console_proxy__vlan_id");
        indexes.put("console_proxy", keys);

        // mshost table
        keys = new ArrayList<String>();
        keys.add("msid_2");
        indexes.put("mshost", keys);

        // domain router table
        keys = new ArrayList<String>();
        keys.add("fk_domain_router__vlan_id");
        keys.add("fk_domain_route__id");
        foreignKeys.put("domain_router", keys);

        keys = new ArrayList<String>();
        keys.add("i_domain_router__public_ip_address");
        keys.add("i_domain_router__vlan_id");
        indexes.put("domain_router", keys);

        // user_vm table
        keys = new ArrayList<String>();
        keys.add("i_user_vm__domain_router_id");
        keys.add("i_user_vm__external_ip_address");
        keys.add("i_user_vm__external_vlan_db_id");
        indexes.put("user_vm", keys);

        keys = new ArrayList<String>();
        keys.add("fk_user_vm__domain_router_id");
        keys.add("fk_user_vm__external_vlan_db_id");
        keys.add("fk_user_vm__external_ip_address");
        foreignKeys.put("user_vm", keys);

        // user_vm_details table
        keys = new ArrayList<String>();
        keys.add("fk_user_vm_details__vm_id");
        foreignKeys.put("user_vm_details", keys);
        indexes.put("user_vm_details", keys);

        // snapshots table
        keys = new ArrayList<String>();
        keys.add("id_2");
        indexes.put("snapshots", keys);

        // remote_access_vpn
        keys = new ArrayList<String>();
        keys.add("fk_remote_access_vpn__server_addr");
        foreignKeys.put("remote_access_vpn", keys);

        keys = new ArrayList<String>();
        keys.add("fk_remote_access_vpn__server_addr_id");
        indexes.put("remote_access_vpn", keys);

        // drop all foreign keys first
        s_logger.debug("Dropping keys that don't exist in 2.2.5 version of the DB...");
        for (String tableName : foreignKeys.keySet()) {
            DbUpgradeUtils.dropKeysIfExist(conn, tableName, foreignKeys.get(tableName), true);
        }

        // drop indexes now
        for (String tableName : indexes.keySet()) {
            DbUpgradeUtils.dropKeysIfExist(conn, tableName, indexes.get(tableName), false);
        }
    }

    private void addMissingKeys(Connection conn) {
        PreparedStatement pstmt = null;
        try {
            s_logger.debug("Adding missing foreign keys");

            HashMap<String, String> keyToTableMap = new HashMap<String, String>();
            keyToTableMap.put("fk_console_proxy__id", "console_proxy");
            keyToTableMap.put("fk_secondary_storage_vm__id", "secondary_storage_vm");
            keyToTableMap.put("fk_template_spool_ref__template_id", "template_spool_ref");
            keyToTableMap.put("fk_template_spool_ref__pool_id", "template_spool_ref");
            keyToTableMap.put("fk_user_vm_details__vm_id", "user_vm_details");
            keyToTableMap.put("fk_op_ha_work__instance_id", "op_ha_work");
            keyToTableMap.put("fk_op_ha_work__mgmt_server_id", "op_ha_work");
            keyToTableMap.put("fk_op_ha_work__host_id", "op_ha_work");

            HashMap<String, String> keyToStatementMap = new HashMap<String, String>();
            keyToStatementMap.put("fk_console_proxy__id", "(`id`) REFERENCES `vm_instance` (`id`) ON DELETE CASCADE");
            keyToStatementMap.put("fk_secondary_storage_vm__id", "(`id`) REFERENCES `vm_instance` (`id`) ON DELETE CASCADE");
            keyToStatementMap.put("fk_template_spool_ref__template_id", "(`template_id`) REFERENCES `vm_template` (`id`)");
            keyToStatementMap.put("fk_template_spool_ref__pool_id", "(`pool_id`) REFERENCES `storage_pool` (`id`) ON DELETE CASCADE");
            keyToStatementMap.put("fk_user_vm_details__vm_id", "(`vm_id`) REFERENCES `user_vm` (`id`) ON DELETE CASCADE");
            keyToStatementMap.put("fk_op_ha_work__instance_id", "(`instance_id`) REFERENCES `vm_instance` (`id`) ON DELETE CASCADE");
            keyToStatementMap.put("fk_op_ha_work__mgmt_server_id", "(`mgmt_server_id`) REFERENCES `mshost`(`msid`)");
            keyToStatementMap.put("fk_op_ha_work__host_id", "(`host_id`) REFERENCES `host` (`id`)");

            for (String key : keyToTableMap.keySet()) {
                String tableName = keyToTableMap.get(key);
                pstmt =
                    conn.prepareStatement("SELECT * FROM information_schema.table_constraints a JOIN information_schema.key_column_usage b ON a.table_schema = b.table_schema AND a.constraint_name = b.constraint_name WHERE a.table_schema=database() AND a.constraint_type='FOREIGN KEY' and a.constraint_name=?");
                pstmt.setString(1, key);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    continue;
                }

                pstmt = conn.prepareStatement("ALTER TABLE " + tableName + " ADD CONSTRAINT " + key + " FOREIGN KEY " + keyToStatementMap.get(key));
                pstmt.executeUpdate();
                s_logger.debug("Added missing key " + key + " to table " + tableName);
                rs.close();
            }
            s_logger.debug("Missing keys were added successfully as a part of 224 to 225 upgrade");
            pstmt.close();
        } catch (SQLException e) {
            s_logger.error("Unable to add missing foreign key; following statement was executed:" + pstmt);
            throw new CloudRuntimeException("Unable to add missign keys due to exception", e);
        }
    }

    private void addMissingOvsAccount(Connection conn) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT * from ovs_tunnel_account");
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                s_logger.debug("Adding missing ovs tunnel account");
                pstmt =
                    conn.prepareStatement("INSERT INTO `cloud`.`ovs_tunnel_account` (`from`, `to`, `account`, `key`, `port_name`, `state`) VALUES (0, 0, 0, 0, 'lock', 'SUCCESS')");
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            s_logger.error("Unable to add missing ovs tunnel account due to ", e);
            throw new CloudRuntimeException("Unable to add missign ovs tunnel account due to ", e);
        }
    }
}
