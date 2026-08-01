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

import com.cloud.network.Network;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade42210to42220 extends DbUpgradeAbstractImpl implements DbUpgrade, DbUpgradeSystemVmTemplate {

    static final String DELETE_VPC_SERVICE_MAPPING = "DELETE FROM cloud.vpc_service_map "
            + "WHERE vpc_id IN (SELECT v.id FROM cloud.vpc v "
            + "INNER JOIN cloud.vpc_offerings vo ON vo.id = v.vpc_offering_id WHERE vo.unique_name = ?) "
            + "AND service = ? AND provider = ?";

    static final String DELETE_VPC_OFFERING_SERVICE_MAPPING = "DELETE FROM cloud.vpc_offering_service_map "
            + "WHERE vpc_offering_id IN (SELECT vo.id FROM cloud.vpc_offerings vo WHERE vo.unique_name = ?) "
            + "AND service = ? AND provider = ?";

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.22.1.0", "4.22.2.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.22.2.0";
    }

    @Override
    public InputStream[] getPrepareScripts() {
        return new InputStream[0];
    }

    @Override
    public void performDataMigration(Connection conn) {
        removeUnsupportedNsxVpnServiceMappings(conn);
    }

    @Override
    public InputStream[] getCleanupScripts() {
        return new InputStream[0];
    }

    protected void removeUnsupportedNsxVpnServiceMappings(Connection conn) {
        try (PreparedStatement deleteVpcMapping = conn.prepareStatement(DELETE_VPC_SERVICE_MAPPING);
             PreparedStatement deleteOfferingMapping = conn.prepareStatement(DELETE_VPC_OFFERING_SERVICE_MAPPING)) {
            setNsxVpnMappingParameters(deleteVpcMapping);
            int vpcMappingsRemoved = deleteVpcMapping.executeUpdate();

            setNsxVpnMappingParameters(deleteOfferingMapping);
            int offeringMappingsRemoved = deleteOfferingMapping.executeUpdate();

            logger.info("Removed {} VPC and {} VPC offering unsupported Vpn/Nsx service mappings",
                    vpcMappingsRemoved, offeringMappingsRemoved);
        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to remove unsupported Vpn/Nsx service mappings from the default NSX NAT VPC offering", e);
        }
    }

    private void setNsxVpnMappingParameters(PreparedStatement statement) throws SQLException {
        statement.setString(1, VpcOffering.DEFAULT_VPC_NAT_NSX_OFFERING_NAME);
        statement.setString(2, Network.Service.Vpn.getName());
        statement.setString(3, Network.Provider.Nsx.getName());
    }
}
