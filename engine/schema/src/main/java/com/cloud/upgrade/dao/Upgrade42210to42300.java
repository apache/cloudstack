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
import java.util.Arrays;
import java.util.List;

import com.cloud.network.vpc.VpcOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade42210to42300 extends DbUpgradeAbstractImpl implements DbUpgrade, DbUpgradeSystemVmTemplate {

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[]{"4.22.1.0", "4.23.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.23.0.0";
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-42210to42300.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        updateNetworkDefaultOfferingsForVPCWithFirewallService(conn);
        updateVpcOfferingsWithFirewallService(conn);
    }

    private void updateNetworkDefaultOfferingsForVPCWithFirewallService(Connection conn) {
        logger.debug("Updating default Network offerings for VPC to add Firewall service with VpcVirtualRouter provider");

        final List<String> defaultVpcOfferingUniqueNames = Arrays.asList(
                NetworkOffering.DefaultIsolatedNetworkOfferingForVpcNetworks,
                NetworkOffering.DefaultIsolatedNetworkOfferingForVpcNetworksNoLB,
                NetworkOffering.DefaultIsolatedNetworkOfferingForVpcNetworksWithInternalLB,
                NetworkOffering.DEFAULT_NAT_NSX_OFFERING_FOR_VPC,
                NetworkOffering.DEFAULT_ROUTED_NSX_OFFERING_FOR_VPC,
                NetworkOffering.DEFAULT_NAT_NSX_OFFERING_FOR_VPC_WITH_ILB,
                NetworkOffering.DEFAULT_ROUTED_NETRIS_OFFERING_FOR_VPC,
                NetworkOffering.DEFAULT_NAT_NETRIS_OFFERING_FOR_VPC
        );

        try {
            for (String uniqueName : defaultVpcOfferingUniqueNames) {
                PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM `cloud`.`network_offerings` WHERE unique_name = ?");
                pstmt.setString(1, uniqueName);

                ResultSet rs = pstmt.executeQuery();
                if (!rs.next()) {
                    continue;
                }

                long offeringId = rs.getLong(1);
                rs.close();
                pstmt.close();

                // Insert into ntwk_offering_service_map
                pstmt = conn.prepareStatement("INSERT IGNORE INTO `cloud`.`ntwk_offering_service_map` " +
                        "(network_offering_id, service, provider, created) " +
                        "VALUES (?, 'Firewall', 'VpcVirtualRouter', now())");
                pstmt.setLong(1, offeringId);
                pstmt.executeUpdate();
                pstmt.close();

                // Update existing networks (ntwk_service_map)
                pstmt = conn.prepareStatement("SELECT id FROM `cloud`.`networks` WHERE network_offering_id = ?");
                pstmt.setLong(1, offeringId);

                rs = pstmt.executeQuery();
                while (rs.next()) {
                    long networkId = rs.getLong(1);
                    PreparedStatement insertService = conn.prepareStatement("INSERT INGORE INTO `cloud`.`ntwk_service_map` " +
                            "(network_id, service, provider, created) " +
                            "VALUES (?, 'Firewall', 'VpcVirtualRouter', now())");
                    insertService.setLong(1, networkId);
                    insertService.executeUpdate();
                    insertService.close();
                }

                rs.close();
                pstmt.close();
            }

        } catch (SQLException e) {
            logger.warn("Exception while updating VPC default offerings with Firewall service: " + e.getMessage(), e);
        }
    }

    private void updateVpcOfferingsWithFirewallService(Connection conn) {
        logger.debug("Updating default VPC offerings to add Firewall service with VpcVirtualRouter provider");

        final List<String> vpcOfferingUniqueNames = Arrays.asList(
                VpcOffering.defaultVPCOfferingName,
                VpcOffering.defaultVPCNSOfferingName,
                VpcOffering.redundantVPCOfferingName,
                VpcOffering.DEFAULT_VPC_NAT_NSX_OFFERING_NAME,
                VpcOffering.DEFAULT_VPC_ROUTE_NSX_OFFERING_NAME,
                VpcOffering.DEFAULT_VPC_ROUTE_NETRIS_OFFERING_NAME,
                VpcOffering.DEFAULT_VPC_NAT_NETRIS_OFFERING_NAME
        );

        try {
            for (String uniqueName : vpcOfferingUniqueNames) {
                PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM `cloud`.`vpc_offerings` WHERE unique_name = ?");
                pstmt.setString(1, uniqueName);

                ResultSet rs = pstmt.executeQuery();
                if (!rs.next()) {
                    continue;
                }

                long vpcOfferingId = rs.getLong(1);
                rs.close();
                pstmt.close();

                // Insert into vpc_offering_service_map
                pstmt = conn.prepareStatement("INSERT IGNORE INTO `cloud`.`vpc_offering_service_map` " +
                        "(vpc_offering_id, service, provider, created) " +
                        "VALUES (?, 'Firewall', 'VpcVirtualRouter', now())");
                pstmt.setLong(1, vpcOfferingId);
                pstmt.executeUpdate();
                pstmt.close();

                // Update existing VPCs
                pstmt = conn.prepareStatement("SELECT id FROM `cloud`.`vpcs` WHERE vpc_offering_id = ?");
                pstmt.setLong(1, vpcOfferingId);

                rs = pstmt.executeQuery();
                while (rs.next()) {
                    long vpcId = rs.getLong(1);
                    PreparedStatement insertService = conn.prepareStatement("INSERT IGNORE INTO `cloud`.`vpc_service_map` " +
                            "(vpc_id, service, provider, created) " +
                            "VALUES (?, 'Firewall', 'VpcVirtualRouter', now())");
                    insertService.setLong(1, vpcId);
                    insertService.executeUpdate();
                    insertService.close();
                }

                rs.close();
                pstmt.close();
            }

        } catch (SQLException e) {
            logger.warn("Exception while updating VPC offerings with Firewall service: " + e.getMessage(), e);
        }
    }
}
