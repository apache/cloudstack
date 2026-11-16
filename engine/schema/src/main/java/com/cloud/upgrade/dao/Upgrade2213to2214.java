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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade2213to2214 extends DbUpgradeAbstractImpl {

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"2.2.13", "2.2.14"};
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.14";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-2213to2214.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        fixIndexes(conn);
    }

    @Override
    public InputStream[] getCleanupScripts() {
        return null;
    }

    private void fixIndexes(Connection conn) {
        //Drop i_usage_event__created key (if exists) and re-add it again
        List<String> keys = new ArrayList<String>();
        keys.add("i_usage_event__created");
        DbUpgradeUtils.dropKeysIfExist(conn, "usage_event", keys, false);
        try {
            PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`usage_event` ADD INDEX `i_usage_event__created`(`created`)");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute usage_event table update", e);
        }

        //In cloud_usage DB, drop i_usage_event__created key (if exists) and re-add it again
        keys = new ArrayList<String>();
        keys.add("i_usage_event__created");
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud_usage.usage_event", keys, false);
        try {
            PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud_usage`.`usage_event` ADD INDEX `i_usage_event__created`(`created`)");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute cloud_usage usage_event table update", e);
        }

        //Drop i_snapshots__removed key (if exists) and re-add it again
        keys = new ArrayList<String>();
        keys.add("i_snapshots__removed");
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.snapshots", keys, false);
        try {
            PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`snapshots` ADD INDEX `i_snapshots__removed`(`removed`)");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to insert index for removed column in snapshots", e);
        }
        //Drop i_op_vm_ruleset_log__instance_id, u_op_vm_ruleset_log__instance_id key (if exists) and re-add u_op_vm_ruleset_log__instance_id again
        keys = new ArrayList<String>();
        keys.add("i_op_vm_ruleset_log__instance_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.op_vm_ruleset_log", keys, false);

        keys = new ArrayList<String>();
        keys.add("u_op_vm_ruleset_log__instance_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.op_vm_ruleset_log", keys, false);
        try {
            PreparedStatement pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`op_vm_ruleset_log` ADD CONSTRAINT `u_op_vm_ruleset_log__instance_id` UNIQUE (`instance_id`)");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute changes for op_vm_ruleset_log", e);
        }

        //Drop i_async__removed, i_async_job__removed  (if exists) and add i_async_job__removed
        keys = new ArrayList<String>();
        keys.add("i_async__removed");
        keys.add("i_async_job__removed");
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.async_job", keys, false);
        try {
            PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`async_job` ADD INDEX `i_async_job__removed`(`removed`)");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to insert index for removed column in async_job", e);
        }

        keys = new ArrayList<String>();
        keys.add("fk_ssh_keypair__account_id");
        keys.add("fk_ssh_keypair__domain_id");
        keys.add("fk_ssh_keypairs__account_id");
        keys.add("fk_ssh_keypairs__domain_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "ssh_keypairs", keys, true);

        keys = new ArrayList<String>();
        keys.add("fk_ssh_keypair__account_id");
        keys.add("fk_ssh_keypair__domain_id");
        keys.add("fk_ssh_keypairs__account_id");
        keys.add("fk_ssh_keypairs__domain_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "ssh_keypairs", keys, false);

        try {
            PreparedStatement pstmt;
            pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`ssh_keypairs` ADD CONSTRAINT `fk_ssh_keypairs__account_id` FOREIGN KEY `fk_ssh_keypairs__account_id` (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute ssh_keypairs table update for adding account_id foreign key", e);
        }

        try {
            PreparedStatement pstmt;
            pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`ssh_keypairs` ADD CONSTRAINT `fk_ssh_keypairs__domain_id` FOREIGN KEY `fk_ssh_keypairs__domain_id` (`domain_id`) REFERENCES `domain` (`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute ssh_keypairs table update for adding domain_id foreign key", e);
        }

        //Drop i_async__removed, i_async_job__removed  (if exists) and add i_async_job__removed
        keys = new ArrayList<String>();
        keys.add("i_async__removed");
        keys.add("i_async_job__removed");
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.async_job", keys, false);
        try {
            PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`async_job` ADD INDEX `i_async_job__removed`(`removed`)");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to insert index for removed column in async_job", e);
        }

        //Drop storage pool details keys (if exists) and insert one with correct name
        keys = new ArrayList<String>();
        keys.add("fk_storage_pool__pool_id");
        keys.add("fk_storage_pool_details__pool_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.storage_pool_details", keys, true);
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.storage_pool_details", keys, false);
        try {
            PreparedStatement pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`storage_pool_details` ADD CONSTRAINT `fk_storage_pool_details__pool_id` FOREIGN KEY `fk_storage_pool_details__pool_id`(`pool_id`) REFERENCES `storage_pool`(`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to insert foreign key in storage_pool_details ", e);
        }

        //Drop securityGroup keys (if exists) and insert one with correct name
        keys = new ArrayList<String>();
        keys.add("fk_security_group___account_id");
        keys.add("fk_security_group__account_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.security_group", keys, true);
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.security_group", keys, false);
        try {
            PreparedStatement pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`security_group` ADD CONSTRAINT `fk_security_group__account_id` FOREIGN KEY `fk_security_group__account_id` (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to insert foreign key in security_group table ", e);
        }

        //Drop vmInstance keys (if exists) and insert one with correct name
        keys = new ArrayList<String>();
        keys.add("i_vm_instance__host_id");
        keys.add("fk_vm_instance__host_id");

        keys.add("fk_vm_instance__last_host_id");
        keys.add("i_vm_instance__last_host_id");

        keys.add("fk_vm_instance__service_offering_id");
        keys.add("i_vm_instance__service_offering_id");

        keys.add("fk_vm_instance__account_id");
        keys.add("i_vm_instance__account_id");

        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.vm_instance", keys, true);
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.vm_instance", keys, false);
        try {
            PreparedStatement pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`vm_instance` ADD CONSTRAINT `fk_vm_instance__host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`)");
            pstmt.executeUpdate();
            pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`vm_instance` ADD CONSTRAINT `fk_vm_instance__last_host_id` FOREIGN KEY (`last_host_id`) REFERENCES `host` (`id`)");
            pstmt.executeUpdate();
            pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`vm_instance` ADD CONSTRAINT `fk_vm_instance__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`)");
            pstmt.executeUpdate();
            pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`vm_instance` ADD CONSTRAINT `fk_vm_instance__service_offering_id` FOREIGN KEY (`service_offering_id`) REFERENCES `service_offering` (`id`)");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to insert foreign key in vm_instance table ", e);
        }

        //Drop user_ip_address keys (if exists) and insert one with correct name
        keys = new ArrayList<String>();
        keys.add("fk_user_ip_address__account_id");
        keys.add("i_user_ip_address__account_id");

        keys.add("fk_user_ip_address__vlan_db_id");
        keys.add("i_user_ip_address__vlan_db_id");

        keys.add("fk_user_ip_address__data_center_id");
        keys.add("i_user_ip_address__data_center_id");

        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.user_ip_address", keys, true);
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.user_ip_address", keys, false);
        try {
            PreparedStatement pstmt = conn.prepareStatement(
                "ALTER TABLE `cloud`.`user_ip_address` ADD CONSTRAINT `fk_user_ip_address__account_id` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`)");
            pstmt.executeUpdate();
            pstmt = conn.prepareStatement(
                "ALTER TABLE `cloud`.`user_ip_address` ADD CONSTRAINT `fk_user_ip_address__vlan_db_id` FOREIGN KEY (`vlan_db_id`) REFERENCES `vlan`(`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt = conn.prepareStatement(
                "ALTER TABLE `cloud`.`user_ip_address` ADD CONSTRAINT `fk_user_ip_address__data_center_id`" +
                    " FOREIGN KEY (`data_center_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to insert foreign key in vm_instance table ", e);
        }

    }
}
