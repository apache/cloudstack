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

import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
    }

    private void encryptKeyInKeyStore(Connection conn) {
        PreparedStatement selectStatement = null;
        ResultSet selectResultSet = null;
        PreparedStatement updateStatement = null;
        try {
            selectStatement = conn.prepareStatement("SELECT ks.id, ks.key FROM cloud.keystore ks WHERE ks.key IS NOT null");
            selectResultSet = selectStatement.executeQuery();
            while (selectResultSet.next()) {
                updateStatement = conn.prepareStatement("UPDATE cloud.keystore ks SET ks.key = ? WHERE ks.id = ?");
                updateStatement.setString(1, DBEncryptionUtil.encrypt(selectResultSet.getString(2)));
                updateStatement.setLong(2, selectResultSet.getLong(1));
                updateStatement.executeUpdate();
                updateStatement.close();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while encrypting key column in keystore table", e);
        } finally {
            if (selectResultSet != null)
                try {
                    selectResultSet.close();
                } catch (SQLException e) {
                }
            if (selectStatement != null)
                try {
                    selectStatement.close();
                } catch (SQLException e) {
                }
            if (updateStatement != null)
                try {
                    updateStatement.close();
                } catch (SQLException e) {
                }
        }
        s_logger.debug("Done encrypting keystore's key column");
    }

    private void encryptIpSecPresharedKeysOfRemoteAccessVpn(Connection conn) {
        PreparedStatement selectStatement = null;
        PreparedStatement updateStatement = null;
        ResultSet resultSet = null;
        try {
            selectStatement = conn.prepareStatement("SELECT id, ipsec_psk FROM `cloud`.`remote_access_vpn`");
            resultSet = selectStatement.executeQuery();
            while (resultSet.next()) {
                String preSharedKey = resultSet.getString(2);
                updateStatement = conn.prepareStatement("UPDATE `cloud`.`remote_access_vpn` SET ipsec_psk=? WHERE id=?");
                updateStatement.setString(1, DBEncryptionUtil.encrypt(preSharedKey));
                updateStatement.setLong(2, resultSet.getLong(1));
                updateStatement.executeUpdate();
                updateStatement.close();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update the remote_access_vpn's preshared key ipsec_psk column", e);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if ((selectStatement != null) && (!selectStatement.isClosed())) {
                    selectStatement.close();
                }
                if ((updateStatement != null) && (!updateStatement.isClosed()))
                    updateStatement.close();
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Done encrypting remote_access_vpn's ipsec_psk column");
    }

    private void encryptStoragePoolUserInfo(Connection conn) {
        List<PreparedStatement> listOfStatements = new ArrayList<PreparedStatement>();
        try {
            PreparedStatement preparedStatement = conn.prepareStatement("SELECT id, user_info FROM `cloud`.`storage_pool` WHERE user_info IS NOT NULL");
            listOfStatements.add(preparedStatement);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                long id = resultSet.getLong(1);
                String userInfo = resultSet.getString(2);
                String encryptedUserInfo = DBEncryptionUtil.encrypt(userInfo);
                preparedStatement = conn.prepareStatement("UPDATE `cloud`.`storage_pool` SET user_info=? WHERE id=?");
                listOfStatements.add(preparedStatement);
                if (encryptedUserInfo == null)
                    preparedStatement.setNull(1, 12);
                else {
                    preparedStatement.setBytes(1, encryptedUserInfo.getBytes("UTF-8"));
                }
                preparedStatement.setLong(2, id);
                preparedStatement.executeUpdate();
                preparedStatement.close();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable encrypt storage pool user info ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable encrypt storage pool user info ", e);
        } finally {
            TransactionLegacy.closePstmts(listOfStatements);
        }
        s_logger.debug("Done encrypting storage_pool's user_info column");
    }
}
