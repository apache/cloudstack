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
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.GuestOSHypervisorMapping;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.GuestOSDaoImpl;
import com.cloud.storage.dao.GuestOSHypervisorDao;
import com.cloud.storage.dao.GuestOSHypervisorDaoImpl;

public class GuestOsMapper {

    final static Logger LOG = Logger.getLogger(GuestOsMapper.class);

    @Inject
    GuestOSHypervisorDao guestOSHypervisorDao;
    @Inject
    GuestOSDao guestOSDao;

    private static final String updateGuestOsHypervisorSql =
            "UPDATE `cloud`.`guest_os_hypervisor` SET guest_os_id = ? WHERE guest_os_id = ? AND hypervisor_type = ? AND hypervisor_version = ? AND guest_os_name = ? AND is_user_defined = 0 AND removed IS NULL";

    public GuestOsMapper() {
        guestOSHypervisorDao = new GuestOSHypervisorDaoImpl();
        guestOSDao = new GuestOSDaoImpl();
    }

    private long getGuestOsId(long categoryId, String displayName) {
        GuestOSVO guestOS = guestOSDao.findByCategoryIdAndDisplayNameOrderByCreatedDesc(categoryId, displayName);
        long id = 0l;
        if (guestOS != null) {
            id = guestOS.getId();
        } else {
            LOG.warn(String.format("Unable to find the guest OS details with category id: %d and display name: %s",  + categoryId, displayName));
        }
        return id;
    }

    private long getGuestOsIdFromHypervisorMapping(GuestOSHypervisorMapping mapping) {
        GuestOSHypervisorVO guestOSHypervisorVO = guestOSHypervisorDao.findByOsNameAndHypervisorOrderByCreatedDesc(mapping.getGuestOsName(), mapping.getHypervisorType(), mapping.getHypervisorVersion());
        long id = 0;
        if (guestOSHypervisorVO != null) {
            id = guestOSHypervisorVO.getGuestOsId();
        } else {
            LOG.warn(String.format("Unable to find the guest OS hypervisor mapping details for %s", mapping.toString()));
        }
        return id;
    }

    public void addGuestOsAndHypervisorMappings(long categoryId, String displayName, List<GuestOSHypervisorMapping> mappings) {
        if (!addGuestOs(categoryId, displayName)) {
            LOG.warn("Couldn't add the guest OS with category id: " + categoryId + " and display name: " + displayName);
            return;
        }

        if (CollectionUtils.isEmpty(mappings)) {
            return;
        }

        long guestOsId = getGuestOsId(categoryId, displayName);
        if (guestOsId == 0) {
            LOG.debug("No guest OS found with category id: " + categoryId + " and display name: " + displayName);
            return;
        }

        for (final GuestOSHypervisorMapping mapping : mappings) {
            addGuestOsHypervisorMapping(mapping, guestOsId);
        }
    }

    public boolean addGuestOs(long categoryId, String displayName) {
        LOG.debug("Adding guest OS with category id: " + categoryId + " and display name: " + displayName);
        GuestOSVO guestOS = new GuestOSVO();
        guestOS.setCategoryId(categoryId);
        guestOS.setDisplayName(displayName);
        guestOS = guestOSDao.persist(guestOS);
        return (guestOS != null);
    }
    public void addGuestOsHypervisorMapping(GuestOSHypervisorMapping mapping, long category, String displayName) {
        long guestOsId =  getGuestOsId(category, displayName);
        if (guestOsId == 0) {
            LOG.error(String.format("no guest os found for category %d and name %s, skipping mapping it to %s/%s", guestOsId, displayName, mapping.getHypervisorType(), mapping.getHypervisorVersion()));
        } else {
            addGuestOsHypervisorMapping(mapping, guestOsId);
        }
    }

    private void addGuestOsHypervisorMapping(GuestOSHypervisorMapping mapping, long guestOsId) {
        if(!isValidGuestOSHypervisorMapping(mapping)) {
            return;
        }

        LOG.debug("Adding guest OS hypervisor mapping - " + mapping.toString() + ", for guest OS with id - " + guestOsId);
        GuestOSHypervisorVO guestOsMapping = new GuestOSHypervisorVO();
        guestOsMapping.setHypervisorType(mapping.getHypervisorType());
        guestOsMapping.setHypervisorVersion(mapping.getHypervisorVersion());
        guestOsMapping.setGuestOsName(mapping.getGuestOsName());
        guestOsMapping.setGuestOsId(guestOsId);
        guestOSHypervisorDao.persist(guestOsMapping);
    }

    public void updateGuestOsName(long categoryId, String oldDisplayName, String newDisplayName) {
        GuestOSVO guestOS = guestOSDao.findByCategoryIdAndDisplayNameOrderByCreatedDesc(categoryId, oldDisplayName);
        if (guestOS == null) {
            LOG.debug("Unable to update guest OS name, as there is no guest OS with category id: " + categoryId + " and display name: " + oldDisplayName);
            return;
        }

        guestOS.setDisplayName(newDisplayName);
        guestOSDao.update(guestOS.getId(), guestOS);
    }

    public void updateGuestOsNameFromMapping(String newDisplayName, GuestOSHypervisorMapping mapping) {
        if(!isValidGuestOSHypervisorMapping(mapping)) {
            return;
        }

        GuestOSHypervisorVO guestOSHypervisorVO = guestOSHypervisorDao.findByOsNameAndHypervisorOrderByCreatedDesc(mapping.getGuestOsName(), mapping.getHypervisorType(), mapping.getHypervisorVersion());
        if (guestOSHypervisorVO == null) {
            LOG.debug("Unable to update guest OS name, as there is no guest os hypervisor mapping");
            return;
        }

        long guestOsId = guestOSHypervisorVO.getGuestOsId();
        GuestOSVO guestOS = guestOSDao.findById(guestOsId);
        if (guestOS != null) {
            guestOS.setDisplayName(newDisplayName);
            guestOSDao.update(guestOS.getId(), guestOS);
        }
    }

    public void updateGuestOsIdInHypervisorMapping(Connection conn, long categoryId, String displayName, GuestOSHypervisorMapping mapping) {
        if(!isValidGuestOSHypervisorMapping(mapping)) {
            return;
        }

        long oldGuestOsId = getGuestOsIdFromHypervisorMapping(mapping);
        if (oldGuestOsId == 0) {
            LOG.debug("Unable to update guest OS in hypervisor mapping, as there is no guest os hypervisor mapping - " + mapping.toString());
            return;
        }

        long newGuestOsId = getGuestOsId(categoryId, displayName);
        if (newGuestOsId == 0) {
            LOG.debug("Unable to update guest OS id in hypervisor mapping, as there is no guest OS with category id: " + categoryId + " and display name: " + displayName);
            return;
        }

        updateGuestOsIdInMapping(conn, oldGuestOsId, newGuestOsId, mapping);
    }

    private void updateGuestOsIdInMapping(Connection conn, long oldGuestOsId, long newGuestOsId, GuestOSHypervisorMapping mapping) {
        LOG.debug("Updating guest os id: " + oldGuestOsId + " to id: " + newGuestOsId + " in hypervisor mapping - " + mapping.toString());
        try {
            PreparedStatement pstmt = conn.prepareStatement(updateGuestOsHypervisorSql);
            pstmt.setLong(1, newGuestOsId);
            pstmt.setLong(2, oldGuestOsId);
            pstmt.setString(3, mapping.getHypervisorType());
            pstmt.setString(4, mapping.getHypervisorVersion());
            pstmt.setString(5, mapping.getGuestOsName());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Failed to update guest OS id in hypervisor mapping due to: " + e.getMessage(), e);
        }
    }

    private boolean isValidGuestOSHypervisorMapping(GuestOSHypervisorMapping mapping) {
        if (mapping != null && mapping.isValid()) {
            return true;
        }

        LOG.warn("Invalid Guest OS hypervisor mapping");
        return false;
    }

    /**
     * Copies guest OS mappings from src version to dest version for the hypervisor (use this to copy all mappings from older version to newer version during upgrade)
     * @return true if copied successfully, else false.
     */
    public boolean copyGuestOSHypervisorMappings(HypervisorType hypervisorType, String srcVersion, String destVersion) {
        if (hypervisorType == HypervisorType.None || hypervisorType == HypervisorType.Any) {
            LOG.warn("Unable to copy, invalid hypervisor");
            return false;
        }

        if (StringUtils.isBlank(srcVersion) || StringUtils.isBlank(destVersion)) {
            LOG.warn("Unable to copy, invalid hypervisor version details");
            return false;
        }

        List<GuestOSHypervisorVO> guestOSHypervisorMappingsForSrcVersion = guestOSHypervisorDao.listByHypervisorTypeAndVersion(hypervisorType.toString(), srcVersion);
        if (CollectionUtils.isEmpty(guestOSHypervisorMappingsForSrcVersion)) {
            LOG.warn(String.format("Unable to copy, couldn't find guest OS mappings for hypervisor: %s and src version: %s", hypervisorType.toString(), srcVersion));
            return false;
        }

        LOG.debug(String.format("Adding guest OS mappings for hypervisor: %s and version: %s, from version: %s ", hypervisorType.toString(), destVersion, srcVersion));
        for (GuestOSHypervisorVO guestOSHypervisorMapping : guestOSHypervisorMappingsForSrcVersion) {
            GuestOSHypervisorMapping mapping = new GuestOSHypervisorMapping(hypervisorType.toString(), destVersion, guestOSHypervisorMapping.getGuestOsName());
            addGuestOsHypervisorMapping(mapping, guestOSHypervisorMapping.getGuestOsId());
        }
        return true;
    }

    public void updateGuestOsNameInHypervisorMapping(long categoryId, String displayName, GuestOSHypervisorMapping mapping) {
        if (!isValidGuestOSHypervisorMapping(mapping)) {
            return;
        }

        long guestOsId = getGuestOsId(categoryId, displayName);
        if (guestOsId == 0) {
            LOG.error(String.format("no guest os found for category %d and name %s, skipping mapping it to %s/%s", guestOsId, displayName, mapping.getHypervisorType(), mapping.getHypervisorVersion()));
            return;
        }

        GuestOSHypervisorVO guestOsMapping = guestOSHypervisorDao.findByOsIdAndHypervisor(guestOsId, mapping.getHypervisorType(), mapping.getHypervisorVersion());
        guestOsMapping.setGuestOsName(mapping.getGuestOsName());
        guestOSHypervisorDao.update(guestOsMapping.getId(), guestOsMapping);
    }
}
