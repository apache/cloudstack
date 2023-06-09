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
package com.cloud.network.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.resourcedetail.dao.UserIpAddressDetailsDao;
import org.springframework.stereotype.Component;

import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.network.IpAddress.State;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.net.Ip;

@Component
@DB
public class IPAddressDaoImpl extends GenericDaoBase<IPAddressVO, Long> implements IPAddressDao {

    protected SearchBuilder<IPAddressVO> AllFieldsSearch;
    protected SearchBuilder<IPAddressVO> VlanDbIdSearchUnallocated;
    protected GenericSearchBuilder<IPAddressVO, Integer> AllIpCount;
    protected GenericSearchBuilder<IPAddressVO, Integer> AllIpCountForDc;
    protected GenericSearchBuilder<IPAddressVO, Integer> AllocatedIpCount;
    protected GenericSearchBuilder<IPAddressVO, Integer> AllocatedIpCountForDc;
    protected GenericSearchBuilder<IPAddressVO, Integer> AllIpCountForDashboard;
    protected SearchBuilder<IPAddressVO> DeleteAllExceptGivenIp;
    protected GenericSearchBuilder<IPAddressVO, Long> AllocatedIpCountForAccount;
    protected SearchBuilder<IPAddressVO> tungstenFloatingIpSearch;
    @Inject
    protected VlanDao _vlanDao;
    protected GenericSearchBuilder<IPAddressVO, Long> CountFreePublicIps;
    protected SearchBuilder<IPAddressVO> PublicIpSearchByAccountAndState;
    @Inject
    ResourceTagDao _tagsDao;
    @Inject
    UserIpAddressDetailsDao _detailsDao;

    // make it public for JUnit test
    public IPAddressDaoImpl() {
    }

    @PostConstruct
    public void init() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), Op.EQ);
        AllFieldsSearch.and("dataCenterId", AllFieldsSearch.entity().getDataCenterId(), Op.EQ);
        AllFieldsSearch.and("ipAddress", AllFieldsSearch.entity().getAddress(), Op.EQ);
        AllFieldsSearch.and("vlan", AllFieldsSearch.entity().getVlanId(), Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), Op.EQ);
        AllFieldsSearch.and("accountId", AllFieldsSearch.entity().getAllocatedToAccountId(), Op.EQ);
        AllFieldsSearch.and("sourceNat", AllFieldsSearch.entity().isSourceNat(), Op.EQ);
        AllFieldsSearch.and("network", AllFieldsSearch.entity().getAssociatedWithNetworkId(), Op.EQ);
        AllFieldsSearch.and("associatedWithVmId", AllFieldsSearch.entity().getAssociatedWithVmId(), Op.EQ);
        AllFieldsSearch.and("oneToOneNat", AllFieldsSearch.entity().isOneToOneNat(), Op.EQ);
        AllFieldsSearch.and("sourcenetwork", AllFieldsSearch.entity().getSourceNetworkId(), Op.EQ);
        AllFieldsSearch.and("physicalNetworkId", AllFieldsSearch.entity().getPhysicalNetworkId(), Op.EQ);
        AllFieldsSearch.and("vpcId", AllFieldsSearch.entity().getVpcId(), Op.EQ);
        AllFieldsSearch.and("associatedVmIp", AllFieldsSearch.entity().getVmIp(), Op.EQ);
        AllFieldsSearch.done();

        VlanDbIdSearchUnallocated = createSearchBuilder();
        VlanDbIdSearchUnallocated.and("allocated", VlanDbIdSearchUnallocated.entity().getAllocatedTime(), Op.NULL);
        VlanDbIdSearchUnallocated.and("vlanDbId", VlanDbIdSearchUnallocated.entity().getVlanId(), Op.EQ);
        VlanDbIdSearchUnallocated.done();

        AllIpCount = createSearchBuilder(Integer.class);
        AllIpCount.select(null, Func.COUNT, AllIpCount.entity().getAddress());
        AllIpCount.and("dc", AllIpCount.entity().getDataCenterId(), Op.EQ);
        AllIpCount.and("vlan", AllIpCount.entity().getVlanId(), Op.EQ);
        AllIpCount.done();

        AllIpCountForDc = createSearchBuilder(Integer.class);
        AllIpCountForDc.select(null, Func.COUNT, AllIpCountForDc.entity().getAddress());
        AllIpCountForDc.and("dc", AllIpCountForDc.entity().getDataCenterId(), Op.EQ);
        AllIpCountForDc.done();

        AllocatedIpCount = createSearchBuilder(Integer.class);
        AllocatedIpCount.select(null, Func.COUNT, AllocatedIpCount.entity().getAddress());
        AllocatedIpCount.and("dc", AllocatedIpCount.entity().getDataCenterId(), Op.EQ);
        AllocatedIpCount.and("vlan", AllocatedIpCount.entity().getVlanId(), Op.EQ);
        AllocatedIpCount.and("allocated", AllocatedIpCount.entity().getAllocatedTime(), Op.NNULL);
        AllocatedIpCount.done();

        AllocatedIpCountForDc = createSearchBuilder(Integer.class);
        AllocatedIpCountForDc.select(null, Func.COUNT, AllocatedIpCountForDc.entity().getAddress());
        AllocatedIpCountForDc.and("dc", AllocatedIpCountForDc.entity().getDataCenterId(), Op.EQ);
        AllocatedIpCountForDc.and("allocated", AllocatedIpCountForDc.entity().getAllocatedTime(), Op.NNULL);
        AllocatedIpCountForDc.done();

        AllIpCountForDashboard = createSearchBuilder(Integer.class);
        AllIpCountForDashboard.select(null, Func.COUNT, AllIpCountForDashboard.entity().getAddress());
        AllIpCountForDashboard.and("dc", AllIpCountForDashboard.entity().getDataCenterId(), Op.EQ);
        AllIpCountForDashboard.and("state", AllIpCountForDashboard.entity().getState(), SearchCriteria.Op.NEQ);

        SearchBuilder<VlanVO> virtaulNetworkVlan = _vlanDao.createSearchBuilder();
        virtaulNetworkVlan.and("vlanType", virtaulNetworkVlan.entity().getVlanType(), SearchCriteria.Op.EQ);

        AllIpCountForDashboard.join("vlan", virtaulNetworkVlan, virtaulNetworkVlan.entity().getId(), AllIpCountForDashboard.entity().getVlanId(),
            JoinBuilder.JoinType.INNER);
        virtaulNetworkVlan.done();
        AllIpCountForDashboard.done();

        AllocatedIpCountForAccount = createSearchBuilder(Long.class);
        AllocatedIpCountForAccount.select(null, Func.COUNT, AllocatedIpCountForAccount.entity().getAddress());
        AllocatedIpCountForAccount.and("account", AllocatedIpCountForAccount.entity().getAllocatedToAccountId(), Op.EQ);
        AllocatedIpCountForAccount.and("allocated", AllocatedIpCountForAccount.entity().getAllocatedTime(), Op.NNULL);
        AllocatedIpCountForAccount.and().op("network", AllocatedIpCountForAccount.entity().getAssociatedWithNetworkId(), Op.NNULL);
        AllocatedIpCountForAccount.or("vpc", AllocatedIpCountForAccount.entity().getVpcId(), Op.NNULL);
        AllocatedIpCountForAccount.or("state", AllocatedIpCountForAccount.entity().getState(), Op.EQ);
        AllocatedIpCountForAccount.cp();AllocatedIpCountForAccount.done();

        CountFreePublicIps = createSearchBuilder(Long.class);
        CountFreePublicIps.select(null, Func.COUNT, null);
        CountFreePublicIps.and("state", CountFreePublicIps.entity().getState(), SearchCriteria.Op.EQ);
        CountFreePublicIps.and("networkId", CountFreePublicIps.entity().getSourceNetworkId(), SearchCriteria.Op.EQ);
        SearchBuilder<VlanVO> join = _vlanDao.createSearchBuilder();
        join.and("vlanType", join.entity().getVlanType(), Op.EQ);
        CountFreePublicIps.join("vlans", join, CountFreePublicIps.entity().getVlanId(), join.entity().getId(), JoinBuilder.JoinType.INNER);
        CountFreePublicIps.done();

        tungstenFloatingIpSearch = createSearchBuilder();
        tungstenFloatingIpSearch.and("dc", tungstenFloatingIpSearch.entity().getDataCenterId(), Op.EQ);
        tungstenFloatingIpSearch.and("state", tungstenFloatingIpSearch.entity().getState(), Op.EQ);
        tungstenFloatingIpSearch.and("network", tungstenFloatingIpSearch.entity().getAssociatedWithNetworkId(), Op.NNULL);
        tungstenFloatingIpSearch.and("sourceNat", tungstenFloatingIpSearch.entity().isSourceNat(), Op.EQ);
        tungstenFloatingIpSearch.done();

        DeleteAllExceptGivenIp = createSearchBuilder();
        DeleteAllExceptGivenIp.and("vlanDbId", DeleteAllExceptGivenIp.entity().getVlanId(), Op.EQ);
        DeleteAllExceptGivenIp.and("ip", DeleteAllExceptGivenIp.entity().getAddress(), Op.NEQ);

        PublicIpSearchByAccountAndState = createSearchBuilder();
        PublicIpSearchByAccountAndState.and("accountId", PublicIpSearchByAccountAndState.entity().getAllocatedToAccountId(), Op.EQ);
        PublicIpSearchByAccountAndState.and("dcId", PublicIpSearchByAccountAndState.entity().getDataCenterId(), Op.EQ);
        PublicIpSearchByAccountAndState.and("state", PublicIpSearchByAccountAndState.entity().getState(), Op.EQ);
        PublicIpSearchByAccountAndState.and("allocated", PublicIpSearchByAccountAndState.entity().getAllocatedTime(), Op.NNULL);
        PublicIpSearchByAccountAndState.and("ipAddress", PublicIpSearchByAccountAndState.entity().getAddress(), Op.EQ);
    }

    @Override
    public boolean mark(long dcId, Ip ip) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("dataCenterId", dcId);
        sc.setParameters("ipAddress", ip);

        IPAddressVO vo = createForUpdate();
        vo.setAllocatedTime(new Date());
        vo.setState(State.Allocated);

        return update(vo, sc) >= 1;
    }

    @Override
    public void unassignIpAddress(long ipAddressId) {
        IPAddressVO address = createForUpdate();
        address.setAllocatedToAccountId(null);
        address.setAllocatedInDomainId(null);
        address.setAllocatedTime(null);
        address.setSourceNat(false);
        address.setOneToOneNat(false);
        address.setAssociatedWithVmId(null);
        address.setState(State.Free);
        address.setAssociatedWithNetworkId(null);
        address.setVpcId(null);
        address.setSystem(false);
        address.setVmIp(null);
        address.setDisplay(true);
        //remove resource details for the ip
        _detailsDao.removeDetails(ipAddressId);
        update(ipAddressId, address);
    }

    @Override
    public List<IPAddressVO> listByAccount(long accountId) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }

    @Override
    public List<IPAddressVO> listByVlanId(long vlanId) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("vlan", vlanId);
        return listBy(sc);
    }

    @Override
    public IPAddressVO findByIpAndSourceNetworkId(long networkId, String ipAddress) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("sourcenetwork", networkId);
        sc.setParameters("ipAddress", ipAddress);
        return findOneBy(sc);
    }

    @Override
    public IPAddressVO findByIpAndNetworkIdAndDcId(long networkId, long dcId, String ipAddress) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("ipAddress", ipAddress);
        sc.setParameters("dataCenterId", dcId);
        return findOneBy(sc);
    }

    @Override
    public IPAddressVO findByIpAndNetworkId(long networkId, String ipAddress) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("ipAddress", ipAddress);
        return findOneBy(sc);
    }

    @Override
    public IPAddressVO findByIpAndVlanId(String ipAddress, long vlanid) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("ipAddress", ipAddress);
        sc.setParameters("vlan", vlanid);
        return findOneBy(sc);
    }

    @Override
    public IPAddressVO findByIpAndDcId(long dcId, String ipAddress) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("dataCenterId", dcId);
        sc.setParameters("ipAddress", ipAddress);
        return findOneBy(sc);
    }

    @Override
    public List<IPAddressVO> listByDcId(long dcId) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("dataCenterId", dcId);
        return listBy(sc);
    }

    @Override
    public List<IPAddressVO> listByDcIdIpAddress(long dcId, String ipAddress) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("dataCenterId", dcId);
        sc.setParameters("ipAddress", ipAddress);
        return listBy(sc);
    }

    @Override
    public List<IPAddressVO> listByAssociatedNetwork(long networkId, Boolean isSourceNat) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);

        if (isSourceNat != null) {
            sc.setParameters("sourceNat", isSourceNat);
        }

        return listBy(sc);
    }

    @Override
    public List<IPAddressVO> listStaticNatPublicIps(long networkId) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("oneToOneNat", true);
        return listBy(sc);
    }

    @Override
    public IPAddressVO findByAssociatedVmId(long vmId) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("associatedWithVmId", vmId);

        return findOneBy(sc);
    }


    // for vm secondary ips case mapping is  IP1--> vmIp1, IP2-->vmIp2, etc
    // Used when vm is mapped to multiple to public ips
    @Override
    public List<IPAddressVO> findAllByAssociatedVmId(long vmId) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("associatedWithVmId", vmId);

        return listBy(sc);
    }

    @Override
    public IPAddressVO findByIp(String ipAddress) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("ipAddress", ipAddress);
        return findOneBy(sc);
    }

    @Override
    public int countIPs(long dcId, boolean onlyCountAllocated) {
        SearchCriteria<Integer> sc = onlyCountAllocated ? AllocatedIpCountForDc.create() : AllIpCountForDc.create();
        sc.setParameters("dc", dcId);

        return customSearch(sc, null).get(0);
    }

    @Override
    public int countIPs(long dcId, long vlanId, boolean onlyCountAllocated) {
        SearchCriteria<Integer> sc = onlyCountAllocated ? AllocatedIpCount.create() : AllIpCount.create();
        sc.setParameters("dc", dcId);
        sc.setParameters("vlan", vlanId);

        return customSearch(sc, null).get(0);
    }

    @Override
    public int countIPsForNetwork(long dcId, boolean onlyCountAllocated, VlanType vlanType) {
        SearchCriteria<Integer> sc = AllIpCountForDashboard.create();
        sc.setParameters("dc", dcId);
        if (onlyCountAllocated) {
            sc.setParameters("state", State.Free);
        }
        sc.setJoinParameters("vlan", "vlanType", vlanType.toString());
        return customSearch(sc, null).get(0);
    }

    @Override
    @DB
    public int countIPs(long dcId, Long accountId, String vlanId, String vlanGateway, String vlanNetmask) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        int ipCount = 0;
        try {
            String sql =
                "SELECT count(*) FROM user_ip_address u INNER JOIN vlan v on (u.vlan_db_id = v.id AND v.data_center_id = ? AND v.vlan_id = ? AND v.vlan_gateway = ? AND v.vlan_netmask = ? AND u.account_id = ?)";

            PreparedStatement pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, dcId);
            pstmt.setString(2, vlanId);
            pstmt.setString(3, vlanGateway);
            pstmt.setString(4, vlanNetmask);
            pstmt.setLong(5, accountId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                ipCount = rs.getInt(1);
            }
        } catch (Exception e) {
            logger.warn("Exception counting IP addresses", e);
        }

        return ipCount;
    }

    @Override
    @DB
    public IPAddressVO markAsUnavailable(long ipAddressId) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("id", ipAddressId);

        IPAddressVO ip = createForUpdate();
        ip.setState(State.Releasing);
        if (update(ip, sc) != 1) {
            return null;
        }

        return findOneBy(sc);
    }

    @Override
    public long countAllocatedIPsForAccount(long accountId) {
        SearchCriteria<Long> sc = AllocatedIpCountForAccount.create();
        sc.setParameters("account", accountId);
        sc.setParameters("state", State.Reserved);
        return customSearch(sc, null).get(0);
    }

    @Override
    public List<IPAddressVO> listByPhysicalNetworkId(long physicalNetworkId) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return listBy(sc);
    }

    @Override
    public long countFreePublicIPs() {
        SearchCriteria<Long> sc = CountFreePublicIps.create();
        sc.setParameters("state", State.Free);
        sc.setJoinParameters("vlans", "vlanType", VlanType.VirtualNetwork);
        return customSearch(sc, null).get(0);
    }

    @Override
    public long countFreeIpsInVlan(long vlanDbId) {
        SearchCriteria<IPAddressVO> sc = VlanDbIdSearchUnallocated.create();
        sc.setParameters("vlanDbId", vlanDbId);
        return listBy(sc).size();
    }

    @Override
    public List<IPAddressVO> listByAssociatedVpc(long vpcId, Boolean isSourceNat) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("vpcId", vpcId);

        if (isSourceNat != null) {
            sc.setParameters("sourceNat", isSourceNat);
        }

        return listBy(sc);
    }

    @Override
    public long countFreeIPsInNetwork(long networkId) {
        SearchCriteria<Long> sc = CountFreePublicIps.create();
        sc.setParameters("state", State.Free);
        sc.setParameters("networkId", networkId);
        return customSearch(sc, null).get(0);
    }

    @Override
    public void deletePublicIPRangeExceptAliasIP(long vlanDbId, String aliasIp) {
        SearchCriteria<IPAddressVO> sc = DeleteAllExceptGivenIp.create();
        sc.setParameters("vlan", vlanDbId);
        sc.setParameters("ip", aliasIp);
        remove(sc);
    }

    @Override
    public boolean deletePublicIPRange(long vlanDbId) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("vlan", vlanDbId);
        remove(sc);
        return true;
    }

    @Override
    @DB
    public boolean remove(Long id) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        IPAddressVO entry = findById(id);
        if (entry != null) {
            _tagsDao.removeByIdAndType(id, ResourceObjectType.SecurityGroup);
        }
        boolean result = super.remove(id);
        txn.commit();
        return result;
    }

    @Override
    public IPAddressVO findByAssociatedVmIdAndVmIp(long vmId, String vmIp) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("associatedWithVmId", vmId);
        sc.setParameters("associatedVmIp", vmIp);
        return findOneBy(sc);
    }

    @Override
    public List<IPAddressVO> listByAssociatedVmId(long vmId) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("associatedWithVmId", vmId);
        return listBy(sc);
    }

    @Override
    public IPAddressVO findByVmIdAndNetworkId(long networkId, long vmId) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("associatedWithVmId", vmId);
        return findOneBy(sc);
    }

    @Override
    public void lockRange(long vlandbId) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("vlan", vlandbId);
        lockRows(sc, null, true);
    }

    @Override
    public List<IPAddressVO> listByVlanIdAndState(long vlanId, State state) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("vlan", vlanId);
        sc.setParameters("state", state);
        return listBy(sc);
    }

    @Override
    public IPAddressVO findByAccountIdAndZoneIdAndStateAndIpAddress(long accountId, long dcId, State state, String ipAddress) {
        SearchCriteria<IPAddressVO> sc = PublicIpSearchByAccountAndState.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("dcId", dcId);
        sc.setParameters("state", state);
        sc.setParameters("ipAddress", ipAddress);
        return findOneBy(sc);
    }

    @Override
    public List<IPAddressVO> listByDcIdAndAssociatedNetwork(final long dcId) {
        SearchCriteria<IPAddressVO> sc = tungstenFloatingIpSearch.create();
        sc.setParameters("dataCenterId", dcId);
        sc.setParameters("sourceNat", false);
        sc.setParameters("state", State.Allocated);
        return listBy(sc);
    }

    @Override
    public List<IPAddressVO> listByNetworkId(long networkId) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("state", State.Allocated);
        return listBy(sc);
    }
}
