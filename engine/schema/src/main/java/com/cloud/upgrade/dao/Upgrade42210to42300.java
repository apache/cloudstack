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
import java.util.Arrays;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.cloud.utils.crypt.DBEncryptionUtil;
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
        unhideJsInterpretationEnabled(conn);
        updateVpcDefaultOfferingsWithFirewallService(conn);
    }

    protected void unhideJsInterpretationEnabled(Connection conn) {
        String value = getJsInterpretationEnabled(conn);
        if (value != null) {
            updateJsInterpretationEnabledFields(conn, value);
        }
    }

    protected String getJsInterpretationEnabled(Connection conn) {
        String query = "SELECT value FROM cloud.configuration WHERE name = 'js.interpretation.enabled' AND category = 'Hidden';";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("value");
            }
            logger.debug("Unable to retrieve value of hidden configuration 'js.interpretation.enabled'. The configuration may already be unhidden.");
            return null;
        } catch (SQLException e) {
            throw new CloudRuntimeException("Error while retrieving value of hidden configuration 'js.interpretation.enabled'.", e);
        }
    }

    protected void updateJsInterpretationEnabledFields(Connection conn, String encryptedValue) {
        String query = "UPDATE cloud.configuration SET value = ?, category = 'System', component = 'JsInterpreter', is_dynamic = 1 WHERE name = 'js.interpretation.enabled';";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            String decryptedValue = DBEncryptionUtil.decrypt(encryptedValue);
            logger.info("Updating setting 'js.interpretation.enabled' to decrypted value [{}], category 'System', component 'JsInterpreter', and is_dynamic '1'.", decryptedValue);
            pstmt.setString(1, decryptedValue);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Error while unhiding configuration 'js.interpretation.enabled'.", e);
        } catch (CloudRuntimeException e) {
            logger.warn("Error while decrypting configuration 'js.interpretation.enabled'. The configuration may already be decrypted.");
        }
    }

    private void updateVpcDefaultOfferingsWithFirewallService(Connection conn) {
        logger.debug("Updating default VPC offerings to add Firewall service with VpcVirtualRouter provider");

        final List<String> defaultVpcOfferingUniqueNames = Arrays.asList(
                "DefaultIsolatedNetworkOfferingForVpcNetworks",
                "DefaultIsolatedNetworkOfferingForVpcNetworksNoLB",
                "DefaultIsolatedNetworkOfferingForVpcNetworksWithInternalLB",
                "DefaultNATNSXNetworkOfferingForVpc",
                "DefaultRoutedNSXNetworkOfferingForVpc",
                "DefaultNATNSXNetworkOfferingForVpcWithInternalLB",
                "DefaultRoutedNetrisNetworkOfferingForVpc",
                "DefaultNATNetrisNetworkOfferingForVpc",
                "DefaultNSXVPCNetworkOfferingforKubernetesService"
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

                // Insert into ntwk_offering_service_map (if not exists)
                pstmt = conn.prepareStatement("INSERT INTO `cloud`.`ntwk_offering_service_map` " +
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
                    PreparedStatement insertService = conn.prepareStatement("INSERT INTO `cloud`.`ntwk_service_map` " +
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
}
