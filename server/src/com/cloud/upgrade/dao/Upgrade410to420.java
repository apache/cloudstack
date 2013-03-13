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

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.log4j.Logger;

public class Upgrade410to420 implements DbUpgrade {
	final static Logger s_logger = Logger.getLogger(Upgrade410to420.class);

	@Override
	public String[] getUpgradableVersionRange() {
		return new String[] { "4.1.0", "4.2.0" };
	}

	@Override
	public String getUpgradedVersion() {
		return "4.2.0";
	}

	@Override
	public boolean supportsRollingUpgrade() {
		return false;
	}

	@Override
	public File[] getPrepareScripts() {
		String script = Script.findScript("", "db/schema-410to420.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-410to420.sql");
        }

        return new File[] { new File(script) };
	}

	@Override
	public void performDataMigration(Connection conn) {
        upgradeVmwareLabels(conn);
        PreparedStatement sql = null;
        try {
            sql = conn.prepareStatement("update vm_template set image_data_store_id = 1 where type = 'SYSTEM' or type = 'BUILTIN'");
            sql.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to upgrade vm template data store uuid: " + e.toString());
        } finally {
            if (sql != null) {
                try {
                    sql.close();
                } catch (SQLException e) {
                }
            }
        }
    }

	@Override
	public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-410to420-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-410to420-cleanup.sql");
        }

        return new File[] { new File(script) };
    }

    private String getNewLabel(ResultSet rs, String oldParamValue) {
        int separatorIndex;
        String oldGuestLabel;
        String newGuestLabel = oldParamValue;
        try {
            // No need to iterate because the global param setting applies to all physical networks irrespective of traffic type
            if (rs.next()) {
                oldGuestLabel = rs.getString("vmware_network_label");
                // guestLabel is in format [[<VSWITCHNAME>],VLANID]
                separatorIndex = oldGuestLabel.indexOf(",");
                if(separatorIndex > -1) {
                    newGuestLabel += oldGuestLabel.substring(separatorIndex);
                }
            }
        } catch (SQLException e) {
            s_logger.error(new CloudRuntimeException("Failed to read vmware_network_label : " + e));
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
            }
        }
        return newGuestLabel;
    }

    private void upgradeVmwareLabels(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rsParams = null;
        ResultSet rsLabel = null;
        String newLabel;
        String trafficType = null;
        String trafficTypeVswitchParam;
        String trafficTypeVswitchParamValue;

        try {
            // update the existing vmware traffic labels
            pstmt = conn.prepareStatement("select name,value from `cloud`.`configuration` where category='Hidden' and value is not NULL and name REGEXP 'vmware\\.*\\.vswitch';");
            rsParams = pstmt.executeQuery();
            while (rsParams.next()) {
                trafficTypeVswitchParam = rsParams.getString("name");
                trafficTypeVswitchParamValue = rsParams.getString("value");
                // When upgraded from 4.0 to 4.1 update physical network traffic label with trafficTypeVswitchParam
                if (trafficTypeVswitchParam.equals("vmware.private.vswitch")) {
                    trafficType = "Management"; //TODO(sateesh): Ignore storage traffic, as required physical network already implemented, anything else tobe done?
                } else if (trafficTypeVswitchParam.equals("vmware.public.vswitch")) {
                    trafficType = "Public";
                } else if (trafficTypeVswitchParam.equals("vmware.guest.vswitch")) {
                    trafficType = "Guest";
                }
                s_logger.debug("Updating vmware label for " + trafficType + " traffic. Update SQL statement is " + pstmt);
                pstmt = conn.prepareStatement("select physical_network_id, traffic_type, vmware_network_label from physical_network_traffic_types where vmware_network_label is not NULL and traffic_type='" + trafficType + "';");
                rsLabel = pstmt.executeQuery();
                newLabel = getNewLabel(rsLabel, trafficTypeVswitchParamValue);
                pstmt = conn.prepareStatement("update physical_network_traffic_types set vmware_network_label = " + newLabel + " where traffic_type = '" + trafficType + "' and vmware_network_label is not NULL;");
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to set vmware traffic labels ", e);
        } finally {
            try {
                if (rsParams != null) {
                    rsParams.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
    }
}
