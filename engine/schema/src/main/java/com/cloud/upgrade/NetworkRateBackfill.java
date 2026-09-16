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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.dao.ConfigurationDaoImpl;

import com.cloud.dc.DataCenterDetailVO;
import com.cloud.dc.dao.DataCenterDetailsDaoImpl;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDaoImpl;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkDetailsDaoImpl;
import com.cloud.network.dao.NetworkVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.service.dao.ServiceOfferingDaoImpl;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicDaoImpl;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.dao.VMInstanceDaoImpl;

/**
 * Backfills {@code nics.network_rate} and the {@code network_details} "networkrate" entry for
 * pre-existing NICs/networks. Deliberately frozen to the pre-feature precedence of
 * {@link com.cloud.network.NetworkModelImpl#getNetworkRate} - do not redirect this to call the
 * live method, whose precedence will keep evolving.
 */
public class NetworkRateBackfill {
    protected static Logger LOGGER = LogManager.getLogger(NetworkRateBackfill.class);

    private static final String CONFIG_NETWORK_THROTTLING_RATE = "network.throttling.rate";
    private static final String CONFIG_VM_NETWORK_THROTTLING_RATE = "vm.network.throttling.rate";
    private static final String NETWORKRATE_DETAIL_NAME = "networkrate";
    private static final int DEFAULT_THROTTLING_RATE = 200;

    private final NicDao nicDao = new NicDaoImpl();
    private final VMInstanceDao vmInstanceDao = new VMInstanceDaoImpl();
    private final NetworkDao networkDao = new NetworkDaoImpl();
    private final NetworkDetailsDao networkDetailsDao = new NetworkDetailsDaoImpl();
    private final ServiceOfferingDao serviceOfferingDao = new ServiceOfferingDaoImpl();
    private final DataCenterDetailsDaoImpl dataCenterDetailsDao = new DataCenterDetailsDaoImpl();
    private final ConfigurationDao configurationDao = new ConfigurationDaoImpl();

    public void backfillNetworkRates() {
        backfillNicNetworkRates();
        backfillNetworkDetailsRates();
    }

    private void backfillNicNetworkRates() {
        final String sql = "SELECT id, network_id, instance_id, default_nic FROM nics " +
                "WHERE removed IS NULL AND network_rate IS NULL AND instance_id IS NOT NULL";
        try (PreparedStatement pstmt = TransactionLegacy.currentTxn().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                final long nicId = rs.getLong("id");
                final long networkId = rs.getLong("network_id");
                final long instanceId = rs.getLong("instance_id");
                final boolean defaultNic = rs.getBoolean("default_nic");
                try {
                    final Integer rate = computeLegacyNicNetworkRate(networkId, instanceId, defaultNic);
                    if (rate != null && rate > 0) {
                        updateNicNetworkRate(nicId, rate);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to backfill network_rate for nic id=" + nicId + ": " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            LOGGER.warn("Failed to backfill nic network rates: " + e.getMessage());
        }
    }

    private void updateNicNetworkRate(long nicId, int rate) throws SQLException {
        try (PreparedStatement pstmt = TransactionLegacy.currentTxn().prepareStatement(
                "UPDATE nics SET network_rate = ? WHERE id = ?")) {
            pstmt.setInt(1, rate);
            pstmt.setLong(2, nicId);
            pstmt.executeUpdate();
        }
    }

    private Integer computeLegacyNicNetworkRate(long networkId, long instanceId, boolean defaultNic) {
        final NetworkVO network = networkDao.findById(networkId);
        if (network == null) {
            return null;
        }
        final VMInstanceVO vm = vmInstanceDao.findById(instanceId);
        if (vm != null) {
            if (vm.getType() == VirtualMachine.Type.User) {
                if (defaultNic) {
                    return getServiceOfferingNetworkRate(vm.getServiceOfferingId(), network.getDataCenterId());
                }
            } else if (vm.getType() == VirtualMachine.Type.DomainRouter) {
                if (TrafficType.Guest.equals(network.getTrafficType())) {
                    return getNetworkOfferingNetworkRate(network.getNetworkOfferingId(), network.getDataCenterId());
                } else if (TrafficType.Public.equals(network.getTrafficType())) {
                    final Integer rate = findRouterGuestNetworkRate(vm.getId(), network.getDataCenterId());
                    if (rate != null) {
                        return rate;
                    }
                }
            } else if (vm.getType() == VirtualMachine.Type.ConsoleProxy || vm.getType() == VirtualMachine.Type.SecondaryStorageVm) {
                return -1;
            }
        }
        return getNetworkOfferingNetworkRate(network.getNetworkOfferingId(), network.getDataCenterId());
    }

    private Integer findRouterGuestNetworkRate(long routerInstanceId, long dataCenterId) {
        final List<NicVO> routerNics = nicDao.listByVmId(routerInstanceId);
        for (final NicVO routerNic : routerNics) {
            final NetworkVO nw = networkDao.findById(routerNic.getNetworkId());
            if (nw != null && TrafficType.Guest.equals(nw.getTrafficType())) {
                return getNetworkOfferingNetworkRate(nw.getNetworkOfferingId(), dataCenterId);
            }
        }
        return null;
    }

    private void backfillNetworkDetailsRates() {
        final String sql = "SELECT n.id, n.network_offering_id, n.data_center_id FROM networks n " +
                "WHERE n.removed IS NULL AND NOT EXISTS " +
                "(SELECT 1 FROM network_details d WHERE d.network_id = n.id AND d.name = ?)";
        try (PreparedStatement pstmt = TransactionLegacy.currentTxn().prepareStatement(sql)) {
            pstmt.setString(1, NETWORKRATE_DETAIL_NAME);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    final long networkId = rs.getLong("id");
                    final long networkOfferingId = rs.getLong("network_offering_id");
                    final long dataCenterId = rs.getLong("data_center_id");
                    try {
                        final int rate = getNetworkOfferingNetworkRate(networkOfferingId, dataCenterId);
                        networkDetailsDao.addDetail(networkId, NETWORKRATE_DETAIL_NAME, String.valueOf(rate), true);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to backfill network_details rate for network id=" + networkId + ": " + e.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.warn("Failed to backfill network details rates: " + e.getMessage());
        }
    }

    private int getServiceOfferingNetworkRate(long serviceOfferingId, long dataCenterId) {
        final ServiceOfferingVO offering = serviceOfferingDao.findById(serviceOfferingId);
        Integer rate = offering == null ? null : offering.getRateMbps();
        if (rate == null) {
            final String vmType = offering == null ? null : offering.getVmType();
            final String configName = "DomainRouter".equalsIgnoreCase(vmType) ? CONFIG_NETWORK_THROTTLING_RATE : CONFIG_VM_NETWORK_THROTTLING_RATE;
            rate = getZoneScopedConfigValue(configName, dataCenterId);
        }
        return normalizeRate(rate);
    }

    private int getNetworkOfferingNetworkRate(long networkOfferingId, long dataCenterId) {
        Integer rate = getNetworkOfferingRateMbps(networkOfferingId);
        if (rate == null) {
            rate = getZoneScopedConfigValue(CONFIG_NETWORK_THROTTLING_RATE, dataCenterId);
        }
        return normalizeRate(rate);
    }

    // NetworkOfferingDaoImpl's constructor is protected, so it can't be instantiated here directly.
    private Integer getNetworkOfferingRateMbps(long networkOfferingId) {
        try (PreparedStatement pstmt = TransactionLegacy.currentTxn().prepareStatement(
                "SELECT nw_rate FROM network_offerings WHERE id = ?")) {
            pstmt.setLong(1, networkOfferingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    final Object nwRate = rs.getObject(1);
                    return nwRate == null ? null : ((Number) nwRate).intValue();
                }
            }
        } catch (SQLException e) {
            LOGGER.warn("Failed to read nw_rate for network offering id=" + networkOfferingId + ": " + e.getMessage());
        }
        return null;
    }

    private int normalizeRate(int rate) {
        return rate == 0 ? -1 : rate;
    }

    private int getZoneScopedConfigValue(String name, long dataCenterId) {
        final DataCenterDetailVO detail = dataCenterDetailsDao.findDetail(dataCenterId, name);
        final String value = detail != null ? detail.getValue() : configurationDao.getValue(name);
        return value != null ? Integer.parseInt(value) : DEFAULT_THROTTLING_RATE;
    }
}
