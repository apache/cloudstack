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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.springframework.stereotype.Component;

import com.cloud.dc.AccountVlanMapVO;
import com.cloud.dc.DomainVlanMapVO;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;

@Component
public class VlanDaoImpl extends GenericDaoBase<VlanVO, Long> implements VlanDao {

    private final String FindZoneWideVlans =
        "SELECT * FROM vlan WHERE data_center_id=? and vlan_type=? and vlan_id!=? and id not in (select vlan_db_id from account_vlan_map)";

    protected SearchBuilder<VlanVO> ZoneVlanIdSearch;
    protected SearchBuilder<VlanVO> ZoneSearch;
    protected SearchBuilder<VlanVO> ZoneTypeSearch;
    protected SearchBuilder<VlanVO> ZoneTypeAllPodsSearch;
    protected SearchBuilder<VlanVO> ZoneTypePodSearch;
    protected SearchBuilder<VlanVO> ZoneVlanSearch;
    protected SearchBuilder<VlanVO> NetworkVlanSearch;
    protected SearchBuilder<VlanVO> PhysicalNetworkVlanSearch;
    protected SearchBuilder<VlanVO> ZoneWideNonDedicatedVlanSearch;
    protected SearchBuilder<VlanVO> VlanGatewaysearch;
    protected SearchBuilder<VlanVO> DedicatedVlanSearch;

    protected SearchBuilder<AccountVlanMapVO> AccountVlanMapSearch;
    protected SearchBuilder<DomainVlanMapVO> DomainVlanMapSearch;

    @Inject
    protected PodVlanMapDao _podVlanMapDao;
    @Inject
    protected AccountVlanMapDao _accountVlanMapDao;
    @Inject
    protected DomainVlanMapDao _domainVlanMapDao;
    @Inject
    protected IPAddressDao _ipAddressDao;

    @Override
    public VlanVO findByZoneAndVlanId(long zoneId, String vlanId) {
        SearchCriteria<VlanVO> sc = ZoneVlanIdSearch.create();
        sc.setParameters("zoneId", zoneId);
        sc.setParameters("vlanId", vlanId);
        return findOneBy(sc);
    }

    /**
     * Returns a vlan by the network id and if the given IPv4 is in the network IP range.
     */
    @Override
    public VlanVO findByNetworkIdAndIpv4(long networkId, String ipv4Address) {
        List<VlanVO> vlanVoList = listVlansByNetworkId(networkId);
        for (VlanVO vlan : vlanVoList) {
            String ipRange = vlan.getIpRange();
            String[] ipRangeParts = ipRange.split("-");
            String startIP = ipRangeParts[0];
            String endIP = ipRangeParts[1];
            if (NetUtils.isIpInRange(ipv4Address, startIP, endIP)) {
                return vlan;
            }
        }
        return null;
    }

    @Override
    public List<VlanVO> listByZone(long zoneId) {
        SearchCriteria<VlanVO> sc = ZoneSearch.create();
        sc.setParameters("zoneId", zoneId);
        return listBy(sc);
    }

    public VlanDaoImpl() {
        ZoneVlanIdSearch = createSearchBuilder();
        ZoneVlanIdSearch.and("zoneId", ZoneVlanIdSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        ZoneVlanIdSearch.and("vlanId", ZoneVlanIdSearch.entity().getVlanTag(), SearchCriteria.Op.EQ);
        ZoneVlanIdSearch.done();

        ZoneSearch = createSearchBuilder();
        ZoneSearch.and("zoneId", ZoneSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        ZoneSearch.done();

        ZoneTypeSearch = createSearchBuilder();
        ZoneTypeSearch.and("zoneId", ZoneTypeSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        ZoneTypeSearch.and("vlanType", ZoneTypeSearch.entity().getVlanType(), SearchCriteria.Op.EQ);
        ZoneTypeSearch.done();

        NetworkVlanSearch = createSearchBuilder();
        NetworkVlanSearch.and("networkId", NetworkVlanSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        NetworkVlanSearch.done();

        PhysicalNetworkVlanSearch = createSearchBuilder();
        PhysicalNetworkVlanSearch.and("physicalNetworkId", PhysicalNetworkVlanSearch.entity().getPhysicalNetworkId(), SearchCriteria.Op.EQ);
        PhysicalNetworkVlanSearch.done();

        VlanGatewaysearch = createSearchBuilder();
        VlanGatewaysearch.and("gateway", VlanGatewaysearch.entity().getVlanGateway(), SearchCriteria.Op.EQ);
        VlanGatewaysearch.and("networkid", VlanGatewaysearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        VlanGatewaysearch.done();
    }

    @Override
    public List<VlanVO> listZoneWideVlans(long zoneId, VlanType vlanType, String vlanId) {
        SearchCriteria<VlanVO> sc = ZoneVlanSearch.create();
        sc.setParameters("zoneId", zoneId);
        sc.setParameters("vlanId", vlanId);
        sc.setParameters("vlanType", vlanType);
        return listBy(sc);
    }

    @Override
    public List<VlanVO> listByZoneAndType(long zoneId, VlanType vlanType) {
        SearchCriteria<VlanVO> sc = ZoneTypeSearch.create();
        sc.setParameters("zoneId", zoneId);
        sc.setParameters("vlanType", vlanType);
        return listBy(sc);
    }

    @Override
    public List<VlanVO> listByType(VlanType vlanType) {
        SearchCriteria<VlanVO> sc = ZoneTypeSearch.create();
        sc.setParameters("vlanType", vlanType);
        return listBy(sc);
    }

    @Override
    public List<VlanVO> listVlansForPod(long podId) {
        //FIXME: use a join statement to improve the performance (should be minor since we expect only one or two
        List<PodVlanMapVO> vlanMaps = _podVlanMapDao.listPodVlanMapsByPod(podId);
        List<VlanVO> result = new ArrayList<VlanVO>();
        for (PodVlanMapVO pvmvo : vlanMaps) {
            VlanVO vlanByPodId = findById(pvmvo.getVlanDbId());
            if (vlanByPodId != null) {
                result.add(vlanByPodId);
            }
        }
        return result;
    }

    @Override
    public List<VlanVO> listVlansForPodByType(long podId, VlanType vlanType) {
        //FIXME: use a join statement to improve the performance (should be minor since we expect only one or two)
        List<PodVlanMapVO> vlanMaps = _podVlanMapDao.listPodVlanMapsByPod(podId);
        List<VlanVO> result = new ArrayList<VlanVO>();
        for (PodVlanMapVO pvmvo : vlanMaps) {
            VlanVO vlan = findById(pvmvo.getVlanDbId());
            if (vlan.getVlanType() == vlanType) {
                result.add(vlan);
            }
        }
        return result;
    }

    @Override
    public List<VlanVO> listVlansForAccountByType(Long zoneId, long accountId, VlanType vlanType) {
        //FIXME: use a join statement to improve the performance (should be minor since we expect only one or two)
        List<AccountVlanMapVO> vlanMaps = _accountVlanMapDao.listAccountVlanMapsByAccount(accountId);
        List<VlanVO> result = new ArrayList<VlanVO>();
        for (AccountVlanMapVO acvmvo : vlanMaps) {
            VlanVO vlan = findById(acvmvo.getVlanDbId());
            if (vlan.getVlanType() == vlanType && (zoneId == null || vlan.getDataCenterId() == zoneId)) {
                result.add(vlan);
            }
        }
        return result;
    }

    @Override
    public void addToPod(long podId, long vlanDbId) {
        PodVlanMapVO pvmvo = new PodVlanMapVO(podId, vlanDbId);
        _podVlanMapDao.persist(pvmvo);

    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        boolean result = super.configure(name, params);
        ZoneTypeAllPodsSearch = createSearchBuilder();
        ZoneTypeAllPodsSearch.and("zoneId", ZoneTypeAllPodsSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        ZoneTypeAllPodsSearch.and("vlanType", ZoneTypeAllPodsSearch.entity().getVlanType(), SearchCriteria.Op.EQ);

        SearchBuilder<PodVlanMapVO> PodVlanSearch = _podVlanMapDao.createSearchBuilder();
        PodVlanSearch.and("podId", PodVlanSearch.entity().getPodId(), SearchCriteria.Op.NNULL);
        ZoneTypeAllPodsSearch.join("vlan", PodVlanSearch, PodVlanSearch.entity().getVlanDbId(), ZoneTypeAllPodsSearch.entity().getId(), JoinBuilder.JoinType.INNER);

        ZoneTypeAllPodsSearch.done();
        PodVlanSearch.done();

        ZoneTypePodSearch = createSearchBuilder();
        ZoneTypePodSearch.and("zoneId", ZoneTypePodSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        ZoneTypePodSearch.and("vlanType", ZoneTypePodSearch.entity().getVlanType(), SearchCriteria.Op.EQ);

        SearchBuilder<PodVlanMapVO> PodVlanSearch2 = _podVlanMapDao.createSearchBuilder();
        PodVlanSearch2.and("podId", PodVlanSearch2.entity().getPodId(), SearchCriteria.Op.EQ);
        ZoneTypePodSearch.join("vlan", PodVlanSearch2, PodVlanSearch2.entity().getVlanDbId(), ZoneTypePodSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        PodVlanSearch2.done();
        ZoneTypePodSearch.done();

        ZoneWideNonDedicatedVlanSearch = createSearchBuilder();
        ZoneWideNonDedicatedVlanSearch.and("zoneId", ZoneWideNonDedicatedVlanSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AccountVlanMapSearch = _accountVlanMapDao.createSearchBuilder();
        AccountVlanMapSearch.and("accountId", AccountVlanMapSearch.entity().getAccountId(), SearchCriteria.Op.NULL);
        ZoneWideNonDedicatedVlanSearch.join("AccountVlanMapSearch", AccountVlanMapSearch, ZoneWideNonDedicatedVlanSearch.entity().getId(), AccountVlanMapSearch.entity()
            .getVlanDbId(), JoinBuilder.JoinType.LEFTOUTER);
        DomainVlanMapSearch = _domainVlanMapDao.createSearchBuilder();
        DomainVlanMapSearch.and("domainId", DomainVlanMapSearch.entity().getDomainId(), SearchCriteria.Op.NULL);
        ZoneWideNonDedicatedVlanSearch.join("DomainVlanMapSearch", DomainVlanMapSearch, ZoneWideNonDedicatedVlanSearch.entity().getId(), DomainVlanMapSearch.entity().getVlanDbId(), JoinBuilder.JoinType.LEFTOUTER);
        ZoneWideNonDedicatedVlanSearch.done();
        AccountVlanMapSearch.done();
        DomainVlanMapSearch.done();

        DedicatedVlanSearch = createSearchBuilder();
        AccountVlanMapSearch = _accountVlanMapDao.createSearchBuilder();
        AccountVlanMapSearch.and("accountId", AccountVlanMapSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        DedicatedVlanSearch.join("AccountVlanMapSearch", AccountVlanMapSearch, DedicatedVlanSearch.entity().getId(), AccountVlanMapSearch.entity().getVlanDbId(),
            JoinBuilder.JoinType.LEFTOUTER);
        DedicatedVlanSearch.done();
        AccountVlanMapSearch.done();

        return result;
    }

    private VlanVO findNextVlan(long zoneId, Vlan.VlanType vlanType) {
        List<VlanVO> allVlans = listByZoneAndType(zoneId, vlanType);
        List<VlanVO> emptyVlans = new ArrayList<VlanVO>();
        List<VlanVO> fullVlans = new ArrayList<VlanVO>();

        // Try to find a VLAN that is partially allocated
        for (VlanVO vlan : allVlans) {
            long vlanDbId = vlan.getId();

            int countOfAllocatedIps = _ipAddressDao.countIPs(zoneId, vlanDbId, true);
            int countOfAllIps = _ipAddressDao.countIPs(zoneId, vlanDbId, false);

            if ((countOfAllocatedIps > 0) && (countOfAllocatedIps < countOfAllIps)) {
                return vlan;
            } else if (countOfAllocatedIps == 0) {
                emptyVlans.add(vlan);
            } else if (countOfAllocatedIps == countOfAllIps) {
                fullVlans.add(vlan);
            }
        }

        if (emptyVlans.isEmpty()) {
            return null;
        }

        // Try to find an empty VLAN with the same tag/subnet as a VLAN that is full
        for (VlanVO fullVlan : fullVlans) {
            for (VlanVO emptyVlan : emptyVlans) {
                if (fullVlan.getVlanTag().equals(emptyVlan.getVlanTag()) && fullVlan.getVlanGateway().equals(emptyVlan.getVlanGateway()) &&
                    fullVlan.getVlanNetmask().equals(emptyVlan.getVlanNetmask())) {
                    return emptyVlan;
                }
            }
        }

        // Return a random empty VLAN
        return emptyVlans.get(0);
    }

    @Override
    public boolean zoneHasDirectAttachUntaggedVlans(long zoneId) {
        SearchCriteria<VlanVO> sc = ZoneTypeAllPodsSearch.create();
        sc.setParameters("zoneId", zoneId);
        sc.setParameters("vlanType", VlanType.DirectAttached);

        return listIncludingRemovedBy(sc).size() > 0;
    }

    public Pair<String, VlanVO> assignPodDirectAttachIpAddress(long zoneId, long podId, long accountId, long domainId) {
        SearchCriteria<VlanVO> sc = ZoneTypePodSearch.create();
        sc.setParameters("zoneId", zoneId);
        sc.setParameters("vlanType", VlanType.DirectAttached);
        sc.setJoinParameters("vlan", "podId", podId);

        VlanVO vlan = findOneIncludingRemovedBy(sc);
        if (vlan == null) {
            return null;
        }

        return null;
    }

    @Override
    @DB
    public List<VlanVO> searchForZoneWideVlans(long dcId, String vlanType, String vlanId) {
        StringBuilder sql = new StringBuilder(FindZoneWideVlans);
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        List<VlanVO> zoneWideVlans = new ArrayList<VlanVO>();
        try(PreparedStatement pstmt = txn.prepareStatement(sql.toString());){
            if(pstmt != null) {
                pstmt.setLong(1, dcId);
                pstmt.setString(2, vlanType);
                pstmt.setString(3, vlanId);
                try(ResultSet rs = pstmt.executeQuery();) {
                    while (rs.next()) {
                        zoneWideVlans.add(toEntityBean(rs, false));
                    }
                }catch (SQLException e) {
                    throw new CloudRuntimeException("searchForZoneWideVlans:Exception:" + e.getMessage(), e);
                }
            }
            return zoneWideVlans;
        } catch (SQLException e) {
            throw new CloudRuntimeException("searchForZoneWideVlans:Exception:" + e.getMessage(), e);
        }
    }

    @Override
    public List<VlanVO> listVlansByNetworkId(long networkId) {
        SearchCriteria<VlanVO> sc = NetworkVlanSearch.create();
        sc.setParameters("networkId", networkId);
        return listBy(sc);
    }

    @Override public List<VlanVO> listVlansByNetworkIdIncludingRemoved(long networkId) {
        SearchCriteria<VlanVO> sc = NetworkVlanSearch.create();
        sc.setParameters("networkId", networkId);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<VlanVO> listVlansByNetworkIdAndGateway(long networkid, String gateway) {
        SearchCriteria<VlanVO> sc = VlanGatewaysearch.create();
        sc.setParameters("networkid", networkid);
        sc.setParameters("gateway", gateway);
        return listBy(sc);
    }

    @Override
    public List<VlanVO> listVlansByPhysicalNetworkId(long physicalNetworkId) {
        SearchCriteria<VlanVO> sc = PhysicalNetworkVlanSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return listBy(sc);
    }

    @Override
    public List<VlanVO> listZoneWideNonDedicatedVlans(long zoneId) {
        SearchCriteria<VlanVO> sc = ZoneWideNonDedicatedVlanSearch.create();
        sc.setParameters("zoneId", zoneId);
        return listBy(sc);
    }

    @Override
    public List<VlanVO> listDedicatedVlans(long accountId) {
        SearchCriteria<VlanVO> sc = DedicatedVlanSearch.create();
        sc.setJoinParameters("AccountVlanMapSearch", "accountId", accountId);
        return listBy(sc);
    }

}
