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
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;

import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade450to451 implements DbUpgrade {
        final static Logger s_logger = Logger.getLogger(Upgrade450to451.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.5.0", "4.5.1"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.5.1";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-450to451.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-450to451.sql");
        }
        return new File[] {new File(script)};
    }

    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-450to451-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-450to451-cleanup.sql");
        }

        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
        encryptKeyInKeyStore(conn);
        encryptIpSecPresharedKeysOfRemoteAccessVpn(conn);
        encryptStoragePoolUserInfo(conn);
        updateUserVmDetailsWithNicAdapterType(conn);
        upgradeVMWareLocalStorage(conn);
    }

    private void encryptKeyInKeyStore(Connection conn) {
        try (
                PreparedStatement selectStatement = conn.prepareStatement("SELECT ks.id, ks.key FROM cloud.keystore ks WHERE ks.key IS NOT null");
                ResultSet selectResultSet = selectStatement.executeQuery();
            ) {
            while (selectResultSet.next()) {
                Long keyId = selectResultSet.getLong(1);
                String preSharedKey = selectResultSet.getString(2);
                try (PreparedStatement updateStatement = conn.prepareStatement("UPDATE cloud.keystore ks SET ks.key = ? WHERE ks.id = ?");) {
                    updateStatement.setString(1, DBEncryptionUtil.encrypt(preSharedKey));
                    updateStatement.setLong(2, keyId);
                    updateStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while encrypting key column in keystore table", e);
        }
        s_logger.debug("Done encrypting keystore's key column");
    }

    private void encryptIpSecPresharedKeysOfRemoteAccessVpn(Connection conn) {
        try (
                PreparedStatement selectStatement = conn.prepareStatement("SELECT id, ipsec_psk FROM `cloud`.`remote_access_vpn`");
                ResultSet resultSet = selectStatement.executeQuery();
            ) {
            while (resultSet.next()) {
                Long rowId = resultSet.getLong(1);
                String preSharedKey = resultSet.getString(2);
                try {
                    preSharedKey = DBEncryptionUtil.decrypt(preSharedKey);
                } catch (EncryptionOperationNotPossibleException ignored) {
                    s_logger.debug("The ipsec_psk preshared key id=" + rowId + "in remote_access_vpn is not encrypted, encrypting it.");
                }
                try (PreparedStatement updateStatement = conn.prepareStatement("UPDATE `cloud`.`remote_access_vpn` SET ipsec_psk=? WHERE id=?");) {
                    updateStatement.setString(1, DBEncryptionUtil.encrypt(preSharedKey));
                    updateStatement.setLong(2, rowId);
                    updateStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update the remote_access_vpn's preshared key ipsec_psk column", e);
        }
        s_logger.debug("Done encrypting remote_access_vpn's ipsec_psk column");
    }

    private void encryptStoragePoolUserInfo(Connection conn) {
        List<PreparedStatement> listOfStatements = new ArrayList<PreparedStatement>();
        try (
                PreparedStatement selectStatement = conn.prepareStatement("SELECT id, user_info FROM `cloud`.`storage_pool` WHERE user_info IS NOT NULL");
                ResultSet resultSet = selectStatement.executeQuery();
            ) {
            while (resultSet.next()) {
                long id = resultSet.getLong(1);
                String userInfo = resultSet.getString(2);
                String encryptedUserInfo = DBEncryptionUtil.encrypt(userInfo);
                try (PreparedStatement preparedStatement = conn.prepareStatement("UPDATE `cloud`.`storage_pool` SET user_info=? WHERE id=?");) {
                    listOfStatements.add(preparedStatement);
                    if (encryptedUserInfo == null)
                        preparedStatement.setNull(1, 12);
                    else {
                        preparedStatement.setBytes(1, encryptedUserInfo.getBytes("UTF-8"));
                    }
                    preparedStatement.setLong(2, id);
                    preparedStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable encrypt storage pool user info ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable encrypt storage pool user info ", e);
        }
        s_logger.debug("Done encrypting storage_pool's user_info column");
    }

    private void updateUserVmDetailsWithNicAdapterType(Connection conn) {
        try (PreparedStatement insertPstmt = conn.prepareStatement("INSERT INTO `cloud`.`user_vm_details`(vm_id,name,value,display) select v.id as vm_id, details.name, details.value, details.display from `cloud`.`vm_instance` as v, `cloud`.`vm_template_details` as details  where v.removed is null and v.vm_template_id=details.template_id and details.name='nicAdapter' and details.template_id in (select id from `cloud`.`vm_template` where hypervisor_type = 'vmware') and v.id not in (select vm_id from `cloud`.`user_vm_details` where name='nicAdapter');");) {
            insertPstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to update user_vm_details table with nicAdapter entries by copying from vm_template_detail table", e);
        }
        s_logger.debug("Done. Updated user_vm_details table with nicAdapter entries by copying from vm_template_detail table. This affects only VM/templates with hypervisor_type as VMware.");
    }

    private void upgradeVMWareLocalStorage(Connection conn) {
        try (PreparedStatement updatePstmt = conn.prepareStatement("UPDATE storage_pool SET pool_type='VMFS',host_address=@newaddress WHERE (@newaddress:=concat('VMFS datastore: ', path)) IS NOT NULL AND scope = 'HOST' AND pool_type = 'LVM' AND id IN (SELECT * FROM (SELECT storage_pool.id FROM storage_pool,cluster WHERE storage_pool.cluster_id = cluster.id AND cluster.hypervisor_type='VMware') AS t);");) {
            updatePstmt.executeUpdate();
            s_logger.debug("Done, upgraded VMWare local storage pool type to VMFS and host_address to the VMFS format");
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to upgrade VMWare local storage pool type", e);
        }
    }
}
