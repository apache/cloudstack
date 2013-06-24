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
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Upgrade410to411 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade410to411.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "4.1.0", "4.1.1" };
    }

    @Override
    public String getUpgradedVersion() {
        return "4.1.1";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-410to411.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-410to411.sql");
        }

        return new File[] { new File(script) };
    }

    @Override
    public void performDataMigration(Connection conn) {
        upgradeSystemVmPassword(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }

    private void upgradeSystemVmPassword(Connection conn) {
        PreparedStatement pstmt = null;
        PreparedStatement pstmtSecond = null;
        PreparedStatement stmt = null;
        PreparedStatement psCategory = null;
        ResultSet rsOne = null;
        ResultSet rsSecond = null;
        ResultSet rsCategory = null;
        String rpassword = null;
        String randomPassword = null;
        String configCategory = null;
        try {
            //Check if system.vm.random.password is set to true
            pstmt = conn.prepareStatement("select value from cloud.configuration where name='system.vm.random.password'");
            rsOne = pstmt.executeQuery();
            if(rsOne.first()) {
                randomPassword = (String) rsOne.getString(1);
                if (randomPassword.equalsIgnoreCase("true")) {
                    // Check if the category of the password is not Secure
                    // If category is Secure, then it was already encrypted in 4.1, no need to further encrypt
                    psCategory = conn.prepareStatement("select category from cloud.configuration where name='system.vm.password'");
                    rsCategory = psCategory.executeQuery();
                    if(rsCategory.first()) {
                        configCategory = (String) rsCategory.getString(1);
                        if(!configCategory.equalsIgnoreCase("Secure")) {
                            //Encrypt the password now
                            pstmtSecond = conn.prepareStatement("select value from cloud.configuration where name='system.vm.password'");
                            rsSecond = pstmtSecond.executeQuery();

                            if(rsSecond.first()) {
                                s_logger.info("Encrypting systemvmpassword as it is not encrypted");
                                rpassword = (String) rsSecond.getString(1);
                                stmt = conn.prepareStatement("UPDATE `cloud`.`configuration` SET value = ?, category = 'Secure' WHERE name = 'system.vm.password'");
                                stmt.setString(1, DBEncryptionUtil.encrypt(rpassword));
                                stmt.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Error while updating system vm password", e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (psCategory !=null) {
                    psCategory.close();
                }
                if (pstmtSecond !=null) {
                    pstmtSecond.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
            }
        }
    }
}
