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
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade421to430 extends DbUpgradeAbstractImpl {

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.2.1", "4.3.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.3.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-421to430.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        encryptImageStoreDetails(conn);
        upgradeMemoryOfSsvmOffering(conn);
    }

    private void upgradeMemoryOfSsvmOffering(Connection conn) {
        int newRamSize = 512; //512MB
        long serviceOfferingId = 0;

            /**
             * Pick first row in service_offering table which has system vm type as secondary storage vm. User added offerings would start from 2nd row onwards.
             * We should not update/modify any user-defined offering.
             */

        try (
                PreparedStatement selectPstmt = conn.prepareStatement("SELECT id FROM `cloud`.`service_offering` WHERE vm_type='secondarystoragevm'");
                PreparedStatement updatePstmt = conn.prepareStatement("UPDATE `cloud`.`service_offering` SET ram_size=? WHERE id=?");
                ResultSet selectResultSet = selectPstmt.executeQuery();
            ) {
            if(selectResultSet.next()) {
                serviceOfferingId = selectResultSet.getLong("id");
            }

            updatePstmt.setInt(1, newRamSize);
            updatePstmt.setLong(2, serviceOfferingId);
            updatePstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to upgrade ram_size of service offering for secondary storage vm. ", e);
        }
        logger.debug("Done upgrading RAM for service offering of Secondary Storage VM to " + newRamSize);
    }

    private void encryptImageStoreDetails(Connection conn) {
        logger.debug("Encrypting image store details");
        try (
                PreparedStatement selectPstmt = conn.prepareStatement("select id, value from `cloud`.`image_store_details` where name = 'key' or name = 'secretkey'");
                ResultSet rs = selectPstmt.executeQuery();
            ) {
            while (rs.next()) {
                long id = rs.getLong(1);
                String value = rs.getString(2);
                if (value == null) {
                    continue;
                }
                String encryptedValue = DBEncryptionUtil.encrypt(value);
                try (PreparedStatement updatePstmt = conn.prepareStatement("update `cloud`.`image_store_details` set value=? where id=?");) {
                    updatePstmt.setBytes(1, encryptedValue.getBytes("UTF-8"));
                    updatePstmt.setLong(2, id);
                    updatePstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable encrypt image_store_details values ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable encrypt image_store_details values ", e);
        }
        logger.debug("Done encrypting image_store_details");
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-421to430-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

}
