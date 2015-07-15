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
import java.sql.Types;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade421to430 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade421to430.class);

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
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-421to430.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-421to430.sql");
        }

        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
        encryptLdapConfigParams(conn);
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
        s_logger.debug("Done upgrading RAM for service offering of Secondary Storage VM to " + newRamSize);
    }

    private void encryptLdapConfigParams(Connection conn) {
        String[][] ldapParams = { {"ldap.user.object", "inetOrgPerson", "Sets the object type of users within LDAP"},
                {"ldap.username.attribute", "uid", "Sets the username attribute used within LDAP"}, {"ldap.email.attribute", "mail", "Sets the email attribute used within LDAP"},
                {"ldap.firstname.attribute", "givenname", "Sets the firstname attribute used within LDAP"},
                {"ldap.lastname.attribute", "sn", "Sets the lastname attribute used within LDAP"},
                {"ldap.group.object", "groupOfUniqueNames", "Sets the object type of groups within LDAP"},
                {"ldap.group.user.uniquemember", "uniquemember", "Sets the attribute for uniquemembers within a group"}};

        String insertSql = "INSERT INTO `cloud`.`configuration`(category, instance, component, name, value, description) VALUES ('Secure', 'DEFAULT', 'management-server', ?, ?, "
                + "?) ON DUPLICATE KEY UPDATE category='Secure';";

        try (PreparedStatement pstmt_insert_ldap_parameters = conn.prepareStatement(insertSql);){
            for (String[] ldapParam : ldapParams) {
                String name = ldapParam[0];
                String value = ldapParam[1];
                String desc = ldapParam[2];
                String encryptedValue = DBEncryptionUtil.encrypt(value);
                pstmt_insert_ldap_parameters.setString(1, name);
                pstmt_insert_ldap_parameters.setBytes(2, encryptedValue.getBytes("UTF-8"));
                pstmt_insert_ldap_parameters.setString(3, desc);
                pstmt_insert_ldap_parameters.executeUpdate();
            }

            /**
             * if encrypted, decrypt the ldap hostname and port and then update as they are not encrypted now.
             */
            try (
                    PreparedStatement pstmt_ldap_hostname = conn.prepareStatement("SELECT conf.value FROM `cloud`.`configuration` conf WHERE conf.name='ldap.hostname'");
                    ResultSet resultSet_ldap_hostname = pstmt_ldap_hostname.executeQuery();
                ) {
                String hostname = null;
                String port;
                int portNumber = 0;
                if (resultSet_ldap_hostname.next()) {
                    hostname = DBEncryptionUtil.decrypt(resultSet_ldap_hostname.getString(1));
                }

                try (
                        PreparedStatement pstmt_ldap_port = conn.prepareStatement("SELECT conf.value FROM `cloud`.`configuration` conf WHERE conf.name='ldap.port'");
                        ResultSet resultSet_ldap_port = pstmt_ldap_port.executeQuery();
                    ) {
                    if (resultSet_ldap_port.next()) {
                        port = DBEncryptionUtil.decrypt(resultSet_ldap_port.getString(1));
                        if (StringUtils.isNotBlank(port)) {
                            portNumber = Integer.parseInt(port);
                        }
                    }

                    if (StringUtils.isNotBlank(hostname)) {
                        try (PreparedStatement pstmt_insert_ldap_hostname_port = conn.prepareStatement("INSERT INTO `cloud`.`ldap_configuration`(hostname, port) VALUES(?,?)");) {
                            pstmt_insert_ldap_hostname_port.setString(1, hostname);
                            if (portNumber != 0) {
                                pstmt_insert_ldap_hostname_port.setInt(2, portNumber);
                            } else {
                                pstmt_insert_ldap_hostname_port.setNull(2, Types.INTEGER);
                            }
                            pstmt_insert_ldap_hostname_port.executeUpdate();
                        }
                    }
                }
            }

        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to insert ldap configuration values ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable to insert ldap configuration values ", e);
        }
        s_logger.debug("Done encrypting ldap Config values");

    }

    private void encryptImageStoreDetails(Connection conn) {
        s_logger.debug("Encrypting image store details");
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
        s_logger.debug("Done encrypting image_store_details");
    }

    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-421to430-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-421to430-cleanup.sql");
        }

        return new File[] {new File(script)};
    }

}
