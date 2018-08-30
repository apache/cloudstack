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
package com.cloud.dc.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;


import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenterIpAddressVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;

@Component
@DB
public class DataCenterIpAddressDaoImpl extends GenericDaoBase<DataCenterIpAddressVO, Long> implements DataCenterIpAddressDao, Configurable {
    private static final Logger s_logger = Logger.getLogger(DataCenterIpAddressDaoImpl.class);

    private final SearchBuilder<DataCenterIpAddressVO> AllFieldsSearch;
    private final GenericSearchBuilder<DataCenterIpAddressVO, Integer> AllIpCount;
    private final GenericSearchBuilder<DataCenterIpAddressVO, Integer> AllIpCountForDc;
    private final GenericSearchBuilder<DataCenterIpAddressVO, Integer> AllAllocatedIpCount;
    private final GenericSearchBuilder<DataCenterIpAddressVO, Integer> AllAllocatedIpCountForDc;

    private static final ConfigKey<Boolean> SystemVmManagementIpReservationModeStrictness = new ConfigKey<Boolean>("Advanced",
            Boolean.class, "system.vm.management.ip.reservation.mode.strictness", "false","If enabled, the use of System VMs management IP reservation is strict, preferred if not.", false, ConfigKey.Scope.Global);

    @Override
    @DB
    public DataCenterIpAddressVO takeIpAddress(long dcId, long podId, long instanceId, String reservationId, boolean forSystemVms) {
        SearchCriteria<DataCenterIpAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("pod", podId);
        sc.setParameters("taken", (Date)null);
        sc.setParameters("forSystemVms", forSystemVms);

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        DataCenterIpAddressVO vo = lockOneRandomRow(sc, true);

        // If there is no explicitly created range for system vms and reservation mode is preferred (strictness = false)
        if (forSystemVms && vo == null && !SystemVmManagementIpReservationModeStrictness.value()) {
            sc.setParameters("forSystemVms", false);
            vo = lockOneRandomRow(sc, true);
        }
        if (vo == null) {
            txn.rollback();
            return null;
        }
        vo.setTakenAt(new Date());
        vo.setInstanceId(instanceId);
        vo.setReservationId(reservationId);
        update(vo.getId(), vo);
        txn.commit();
        return vo;
    }

    @Override
    @DB
    public DataCenterIpAddressVO takeDataCenterIpAddress(long dcId, String reservationId) {
        SearchCriteria<DataCenterIpAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("dc", dcId);
        sc.setParameters("taken", (Date)null);

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        DataCenterIpAddressVO vo = lockOneRandomRow(sc, true);
        if (vo == null) {
            txn.rollback();
            return null;
        }
        vo.setTakenAt(new Date());
        vo.setReservationId(reservationId);
        update(vo.getId(), vo);
        txn.commit();
        return vo;
    }

    @Override
    public boolean deleteIpAddressByPod(long podId) {
        SearchCriteria<DataCenterIpAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("pod", podId);
        return remove(sc) > 0;
    }

    @Override
    public boolean deleteIpAddressByPodDc(String ipAddress, long podId, long dcId) {
        SearchCriteria<DataCenterIpAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("ipAddress", ipAddress);
        sc.setParameters("pod", podId);
        sc.setParameters("dc", dcId);

        return remove(sc) > 0;
    }

    @Override
    public boolean mark(long dcId, long podId, String ip) {
        SearchCriteria<DataCenterIpAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("pod", podId);
        sc.setParameters("ipAddress", ip);

        DataCenterIpAddressVO vo = createForUpdate();
        vo.setTakenAt(new Date());

        return update(vo, sc) >= 1;
    }

    @Override
    @DB
    public void addIpRange(long dcId, long podId, String start, String end, boolean forSystemVms, Integer vlan) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        String insertSql = "INSERT INTO `cloud`.`op_dc_ip_address_alloc` (ip_address, data_center_id, pod_id, mac_address, forsystemvms" + (vlan == null ? ") " : ", vlan) ") +
                "VALUES (?, ?, ?, (select mac_address from `cloud`.`data_center` where id=?), ?" + (vlan == null ? ")" : ", ?)");
        String updateSql = "UPDATE `cloud`.`data_center` set mac_address = mac_address+1 where id=?";

        long startIP = NetUtils.ip2Long(start);
        long endIP = NetUtils.ip2Long(end);

        try {
            txn.start();

            while (startIP <= endIP) {
                try(PreparedStatement insertPstmt = txn.prepareStatement(insertSql);) {
                    insertPstmt.setString(1, NetUtils.long2Ip(startIP++));
                    insertPstmt.setLong(2, dcId);
                    insertPstmt.setLong(3, podId);
                    insertPstmt.setLong(4, dcId);
                    insertPstmt.setBoolean(5, forSystemVms);
                    if (vlan != null) {
                        insertPstmt.setInt(6, vlan);
                    }
                    insertPstmt.executeUpdate();
                }
                try(PreparedStatement updatePstmt = txn.prepareStatement(updateSql);) {
                    updatePstmt.setLong(1, dcId);
                    updatePstmt.executeUpdate();
                }
            }
            txn.commit();
        } catch (SQLException ex) {
            throw new CloudRuntimeException("Unable to persist ip address range ", ex);
        }
    }

    @Override
    public void releaseIpAddress(String ipAddress, long dcId, Long instanceId) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing ip address: " + ipAddress + " data center " + dcId);
        }
        SearchCriteria<DataCenterIpAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("ip", ipAddress);
        sc.setParameters("dc", dcId);
        sc.setParameters("instance", instanceId);

        DataCenterIpAddressVO vo = createForUpdate();

        vo.setTakenAt(null);
        vo.setInstanceId(null);
        vo.setReservationId(null);
        update(vo, sc);
    }

    @Override
    public void releaseIpAddress(long nicId, String reservationId) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing ip address for reservationId=" + reservationId + ", instance=" + nicId);
        }
        SearchCriteria<DataCenterIpAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", nicId);
        sc.setParameters("reservation", reservationId);

        DataCenterIpAddressVO vo = createForUpdate();
        vo.setTakenAt(null);
        vo.setInstanceId(null);
        vo.setReservationId(null);
        update(vo, sc);
    }

    @Override
    public void releasePodIpAddress(long id) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing ip address for ID=" + id);
        }

        DataCenterIpAddressVO vo = this.findById(id);
        vo.setTakenAt(null);
        vo.setInstanceId(null);
        vo.setReservationId(null);
        persist(vo);
    }

    @Override
    public void releaseIpAddress(long nicId) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing ip address for instance=" + nicId);
        }
        SearchCriteria<DataCenterIpAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", nicId);

        DataCenterIpAddressVO vo = createForUpdate();
        vo.setTakenAt(null);
        vo.setInstanceId(null);
        vo.setReservationId(null);
        update(vo, sc);
    }


    @Override
    public List<DataCenterIpAddressVO> listByPodIdDcId(long podId, long dcId) {
        SearchCriteria<DataCenterIpAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("pod", podId);
        return listBy(sc);
    }

    @Override
    public List<DataCenterIpAddressVO> listByPodIdDcIdIpAddress(long podId, long dcId, String ipAddress) {
        SearchCriteria<DataCenterIpAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("pod", podId);
        sc.setParameters("ipAddress", ipAddress);
        return listBy(sc);
    }

    @Override
    public int countIPs(long podId, long dcId, boolean onlyCountAllocated) {
        SearchCriteria<Integer> sc;
        if (onlyCountAllocated) {
            sc = AllAllocatedIpCount.create();
        } else {
            sc = AllIpCount.create();
        }

        sc.setParameters("pod", podId);
        List<Integer> count = customSearch(sc, null);
        return count.get(0);
    }

    @Override
    public int countIPs(long dcId, boolean onlyCountAllocated) {
        SearchCriteria<Integer> sc;
        if (onlyCountAllocated) {
            sc = AllAllocatedIpCountForDc.create();
        } else {
            sc = AllIpCountForDc.create();
        }

        sc.setParameters("data_center_id", dcId);
        List<Integer> count = customSearch(sc, null);
        return count.get(0);
    }

    @Override
    public int countIpAddressUsage(final String ipAddress, final long podId, final long dcId, final boolean onlyCountAllocated) {
        SearchCriteria<DataCenterIpAddressVO> sc = createSearchCriteria();

        if(onlyCountAllocated) {
            sc.addAnd("takenAt", SearchCriteria.Op.NNULL);
        }

        sc.addAnd("ipAddress", SearchCriteria.Op.EQ, ipAddress);
        sc.addAnd("podId", SearchCriteria.Op.EQ, podId);
        sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, dcId);

        List<DataCenterIpAddressVO> result = listBy(sc);
        return result.size();
    }

    public DataCenterIpAddressDaoImpl() {
        super();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("ip", AllFieldsSearch.entity().getIpAddress(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("dc", AllFieldsSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("pod", AllFieldsSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("instance", AllFieldsSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("ipAddress", AllFieldsSearch.entity().getIpAddress(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("reservation", AllFieldsSearch.entity().getReservationId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("taken", AllFieldsSearch.entity().getTakenAt(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("forSystemVms", AllFieldsSearch.entity().isForSystemVms(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();

        AllIpCount = createSearchBuilder(Integer.class);
        AllIpCount.select(null, Func.COUNT, AllIpCount.entity().getId());
        AllIpCount.and("pod", AllIpCount.entity().getPodId(), SearchCriteria.Op.EQ);
        AllIpCount.done();

        AllIpCountForDc = createSearchBuilder(Integer.class);
        AllIpCountForDc.select(null, Func.COUNT, AllIpCountForDc.entity().getId());
        AllIpCountForDc.and("data_center_id", AllIpCountForDc.entity().getPodId(), SearchCriteria.Op.EQ);
        AllIpCountForDc.done();

        AllAllocatedIpCount = createSearchBuilder(Integer.class);
        AllAllocatedIpCount.select(null, Func.COUNT, AllAllocatedIpCount.entity().getId());
        AllAllocatedIpCount.and("pod", AllAllocatedIpCount.entity().getPodId(), SearchCriteria.Op.EQ);
        AllAllocatedIpCount.and("removed", AllAllocatedIpCount.entity().getTakenAt(), SearchCriteria.Op.NNULL);
        AllAllocatedIpCount.done();

        AllAllocatedIpCountForDc = createSearchBuilder(Integer.class);
        AllAllocatedIpCountForDc.select(null, Func.COUNT, AllAllocatedIpCountForDc.entity().getId());
        AllAllocatedIpCountForDc.and("data_center_id", AllAllocatedIpCountForDc.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AllAllocatedIpCountForDc.and("removed", AllAllocatedIpCountForDc.entity().getTakenAt(), SearchCriteria.Op.NNULL);
        AllAllocatedIpCountForDc.done();
    }

    @Override
    public String getConfigComponentName() {
        return DataCenterIpAddressDao.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {SystemVmManagementIpReservationModeStrictness};
    }
}
