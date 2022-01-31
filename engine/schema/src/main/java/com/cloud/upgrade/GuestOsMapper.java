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
package com.cloud.upgrade;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.cloud.utils.Ternary;

public class GuestOsMapper {

    final static Logger LOG = Logger.getLogger(GuestOsMapper.class);

    private static final String selectGuestOsSql =
            "SELECT id FROM `cloud`.`guest_os` WHERE category_id = ? AND display_name = ? AND is_user_defined = 0 AND removed IS NULL ORDER BY created DESC";
    private static final String insertGuestOsSql =
            "INSERT INTO `cloud`.`guest_os` (uuid, category_id, display_name, created) VALUES (UUID(), ?, ?, now())";
    private static final String selectGuestOsHypervisorSql =
            "SELECT guest_os_id FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type = ? AND hypervisor_version = ? AND guest_os_name = ? AND is_user_defined = 0 AND removed IS NULL ORDER BY created DESC";
    private static final String insertGuestOsHypervisorSql =
            "INSERT INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created) VALUES (UUID(), ?, ?, ?, ?, now())";
    private static final String updateGuestOsSql =
            "UPDATE `cloud`.`guest_os` SET display_name = ? WHERE id = ?";
    private static final String updateGuestOsHypervisorSql =
            "UPDATE `cloud`.`guest_os_hypervisor` SET guest_os_id = ? WHERE guest_os_id = ? AND hypervisor_type = ? AND hypervisor_version = ? AND guest_os_name = ? AND is_user_defined = 0 AND removed IS NULL";

    public GuestOsMapper() {
    }

    private long getGuestOsId(Connection conn, long categoryId, String displayName) {
        try {
            PreparedStatement pstmt = conn.prepareStatement(selectGuestOsSql);
            pstmt.setLong(1, categoryId);
            pstmt.setString(2, displayName);
            ResultSet rs = pstmt.executeQuery();
            if (rs != null && rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            LOG.error("Failed to get the guest OS details with category id: " + categoryId + " and display name: " + displayName + ", due to: " + e.getMessage(), e);
        }
        LOG.warn("Unable to find the guest OS details with category id: " + categoryId + " and display name: " + displayName);
        return 0;
    }

    private long getGuestOsIdFromHypervisorMapping(Connection conn, String hypervisorType, String hypervisorVersion, String guestOsName) {
        try {
            PreparedStatement pstmt = conn.prepareStatement(selectGuestOsHypervisorSql);
            pstmt.setString(1, hypervisorType);
            pstmt.setString(2, hypervisorVersion);
            pstmt.setString(3, guestOsName);
            ResultSet rs = pstmt.executeQuery();
            if (rs != null && rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            LOG.error("Failed to get the guest OS hypervisor mapping details for Hypervisor(Version): " + hypervisorType + "(" + hypervisorVersion + "), Guest OS: " + guestOsName + ", due to: " + e.getMessage(), e);
        }
        LOG.debug("Unable to find the guest OS hypervisor mapping details for Hypervisor(Version): " + hypervisorType + "(" + hypervisorVersion + "), Guest OS: " + guestOsName);
        return 0;
    }

    public void addGuestOsAndHypervisorMappings(Connection conn, long categoryId, String displayName, List<Ternary<String, String, String>> mappings) {
        if (!addGuestOs(conn, categoryId, displayName)) {
            LOG.warn("Couldn't add the guest OS with category id: " + categoryId + " and display name: " + displayName);
            return;
        }

        if (CollectionUtils.isEmpty(mappings)) {
            return;
        }

        long guestOsId = getGuestOsId(conn, categoryId, displayName);
        if (guestOsId == 0) {
            LOG.debug("No guest OS found with category id: " + categoryId + " and display name: " + displayName);
            return;
        }

        for (final Ternary<String, String, String> mapping : mappings) {
            addGuestOsHypervisorMapping(conn, mapping.first(), mapping.second(), mapping.third(), guestOsId);
        }
    }

    private boolean addGuestOs(Connection conn, long categoryId, String displayName) {
        LOG.debug("Adding guest OS with category id: " + categoryId + " and display name: " + displayName);
        try {
            PreparedStatement pstmt = conn.prepareStatement(insertGuestOsSql);
            pstmt.setLong(1, categoryId);
            pstmt.setString(2, displayName);
            return (pstmt.executeUpdate() == 1);
        } catch (SQLException e) {
            LOG.error("Failed to add guest OS due to: " + e.getMessage(), e);
        }
        return false;
    }

    public void addGuestOsHypervisorMapping(Connection conn, String hypervisorType, String hypervisorVersion, String guestOsName, long guestOsId) {
        LOG.debug("Adding guest OS hypervisor mapping - Hypervisor(Version): " + hypervisorType + "(" + hypervisorVersion + "), Guest OS: " + guestOsName);
        try {
            PreparedStatement pstmt = conn.prepareStatement(insertGuestOsHypervisorSql);
            pstmt.setString(1, hypervisorType);
            pstmt.setString(2, hypervisorVersion);
            pstmt.setString(3, guestOsName);
            pstmt.setLong(4, guestOsId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Failed to add guest OS hypervisor mapping due to: " + e.getMessage(), e);
        }
    }

    public void updateGuestOsName(Connection conn, long categoryId, String oldDisplayName, String newDisplayName) {
        long guestOsId = getGuestOsId(conn, categoryId, oldDisplayName);
        if (guestOsId == 0) {
            LOG.debug("Unable to update guest OS name, as there is no guest OS with category id: " + categoryId + " and display name: " + oldDisplayName);
            return;
        }

        updateGuestOs(conn, guestOsId, newDisplayName);
    }

    public void updateGuestOsNameFromMapping(Connection conn, String newDisplayName, Ternary<String, String, String> mapping) {
        long guestOsId = getGuestOsIdFromHypervisorMapping(conn, mapping.first(), mapping.second(), mapping.third());
        if (guestOsId == 0) {
            LOG.debug("Unable to update guest OS name, as there is no guest os hypervisor mapping");
            return;
        }

        updateGuestOs(conn, guestOsId, newDisplayName);
    }

    public void updateGuestOsIdInHypervisorMapping(Connection conn, long categoryId, String displayName, Ternary<String, String, String> mapping) {
        long oldGuestOsId = getGuestOsIdFromHypervisorMapping(conn, mapping.first(), mapping.second(), mapping.third());
        if (oldGuestOsId == 0) {
            LOG.debug("Unable to update guest OS in hypervisor mapping, as there is no guest os hypervisor mapping - Hypervisor(Version): " + mapping.first() + "(" + mapping.second() + "), Guest OS: " + mapping.third());
            return;
        }

        long newGuestOsId = getGuestOsId(conn, categoryId, displayName);
        if (newGuestOsId == 0) {
            LOG.debug("Unable to update guest OS id in hypervisor mapping, as there is no guest OS with category id: " + categoryId + " and display name: " + displayName);
            return;
        }

        updateGuestOsIdInMapping(conn, oldGuestOsId, newGuestOsId, mapping);
    }

    private void updateGuestOs(Connection conn, long guestOsId, String newDisplayName) {
        LOG.debug("Updating display name: " + newDisplayName + " in the guest OS with id: " + guestOsId);
        try {
            PreparedStatement pstmt = conn.prepareStatement(updateGuestOsSql);
            pstmt.setString(1, newDisplayName);
            pstmt.setLong(2, guestOsId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Failed to update guest OS due to: " + e.getMessage(), e);
        }
    }

    private void updateGuestOsIdInMapping(Connection conn, long oldGuestOsId, long newGuestOsId, Ternary<String, String, String> mapping) {
        LOG.debug("Updating guest os id: " + oldGuestOsId + " to id: " + newGuestOsId + " in hypervisor mapping - Hypervisor(Version): " + mapping.first() + "(" + mapping.second() + "), Guest OS: " + mapping.third());
        try {
            PreparedStatement pstmt = conn.prepareStatement(updateGuestOsHypervisorSql);
            pstmt.setLong(1, newGuestOsId);
            pstmt.setLong(2, oldGuestOsId);
            pstmt.setString(3, mapping.first());
            pstmt.setString(4, mapping.second());
            pstmt.setString(5, mapping.third());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Failed to update guest OS id in hypervisor mapping due to: " + e.getMessage(), e);
        }
    }
}
