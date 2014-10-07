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

import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class Upgrade430to431 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade430to431.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.3.0", "4.3.1"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.3.1";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public File[] getPrepareScripts() {
        return null;
    }

    @Override
    public void performDataMigration(Connection conn) {
        updateVlanUris(conn);
    }

    private void updateVlanUris(Connection conn) {
        s_logger.debug("updating vlan URIs");
        CloudRuntimeException thrown = null;
    	PreparedStatement selectstatement = null;
        ResultSet results = null;
        try{
            selectstatement = conn.prepareStatement("SELECT id, vlan_id FROM `cloud`.`vlan` where vlan_id not like '%:%'");
            results = selectstatement.executeQuery();

            while (results.next()) {
                long id = results.getLong(1);
                String vlan = results.getString(2);
                if (vlan == null || "".equals(vlan)) {
                    continue;
                }
                String vlanUri = BroadcastDomainType.Vlan.toUri(vlan).toString();
                PreparedStatement updatestatement = conn.prepareStatement("update `cloud`.`vlan` set vlan_id=? where id=?");
                try {
                    updatestatement.setString(1, vlanUri);
                    updatestatement.setLong(2, id);
                    updatestatement.executeUpdate();
                } catch (SQLException e) {
                    thrown = new CloudRuntimeException("Unable to update vlan URI " + vlanUri + " for vlan record " + id, e);
                } finally {
                    try {
                        updatestatement.close();
                    } catch (Exception e) {
                        if(thrown == null) {
                            thrown =  new CloudRuntimeException("Unable to close update statement vlan URI " + vlanUri + " for vlan record " + id, e);
                        } //else don't obfuscate the original exception
                    }
                }
            }
        } catch (SQLException e) {
            if(thrown == null) {
                thrown = new CloudRuntimeException("Unable to update vlan URIs ", e);
            } //else don't obfuscate the original exception
        }
        finally
        {
            try {
                if(results != null)
                    results.close();
            } catch (SQLException e) {
                if(thrown == null) {
                    thrown = new CloudRuntimeException("Unable to update vlan URIs ", e);
                } //else don't obfuscate the original exception
            }
            try {
                if (selectstatement != null)
                    selectstatement.close();
            } catch (SQLException e) {
                if(thrown == null) {
                    thrown = new CloudRuntimeException("Unable to update vlan URIs ", e);
                } //else don't obfuscate the original exception
            }
        }
        if (thrown != null) {
            throw thrown;
        }
        s_logger.debug("Done updateing vlan URIs");
    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }

}
