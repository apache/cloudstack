/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.network.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.network.IPAddressVO;
import com.cloud.network.IpAddress.State;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value = { IPAddressDao.class })
@DB
public class IPAddressDaoImpl extends GenericDaoBase<IPAddressVO, String> implements IPAddressDao {
    private static final Logger s_logger = Logger.getLogger(IPAddressDaoImpl.class);

    protected final SearchBuilder<IPAddressVO> AllFieldsSearch;
    protected final SearchBuilder<IPAddressVO> VlanDbIdSearchUnallocated;
    protected final GenericSearchBuilder<IPAddressVO, Integer> AllIpCount;
    protected final GenericSearchBuilder<IPAddressVO, Integer> AllocatedIpCount;

    // make it public for JUnit test
    public IPAddressDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("dataCenterId", AllFieldsSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("ipAddress", AllFieldsSearch.entity().getAddress(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("vlan", AllFieldsSearch.entity().getVlanId(), Op.EQ);
        AllFieldsSearch.and("accountId", AllFieldsSearch.entity().getAllocatedToAccountId(), Op.EQ);
        AllFieldsSearch.and("sourceNat", AllFieldsSearch.entity().isSourceNat(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();

        VlanDbIdSearchUnallocated = createSearchBuilder();
        VlanDbIdSearchUnallocated.and("allocated", VlanDbIdSearchUnallocated.entity().getAllocatedTime(), SearchCriteria.Op.NULL);
        VlanDbIdSearchUnallocated.and("vlanDbId", VlanDbIdSearchUnallocated.entity().getVlanId(), SearchCriteria.Op.EQ);
        VlanDbIdSearchUnallocated.done();

        AllIpCount = createSearchBuilder(Integer.class);
        AllIpCount.select(null, Func.COUNT, AllIpCount.entity().getAddress());
        AllIpCount.and("dc", AllIpCount.entity().getDataCenterId(), Op.EQ);
        AllIpCount.and("vlan", AllIpCount.entity().getVlanId(), Op.EQ);
        AllIpCount.done();

        AllocatedIpCount = createSearchBuilder(Integer.class);
        AllocatedIpCount.select(null, Func.COUNT, AllocatedIpCount.entity().getAddress());
        AllocatedIpCount.and("dc", AllocatedIpCount.entity().getDataCenterId(), Op.EQ);
        AllocatedIpCount.and("vlan", AllocatedIpCount.entity().getVlanId(), Op.EQ);
        AllocatedIpCount.and("allocated", AllocatedIpCount.entity().getAllocatedTime(), Op.NNULL);
        AllocatedIpCount.done();
    }

    @Override
    public boolean mark(long dcId, String ip) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("dataCenterId", dcId);
        sc.setParameters("ipAddress", ip);

        IPAddressVO vo = createForUpdate();
        vo.setAllocatedTime(new Date());
        vo.setState(State.Allocated);

        return update(vo, sc) >= 1;
    }

    @Override
    @DB
    public List<String> assignAcccountSpecificIps(long accountId, long domainId, Long vlanDbId, boolean sourceNat) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("vlan", vlanDbId);
        sc.setParameters("sourceNat", sourceNat);

        List<IPAddressVO> ipList = lockRows(sc, null, true);
        List<String> ipStringList = new ArrayList<String>();

        for (IPAddressVO ip : ipList) {

            ip.setAllocatedToAccountId(accountId);
            ip.setAllocatedTime(new Date());
            ip.setAllocatedInDomainId(domainId);
            ip.setSourceNat(sourceNat);
            ip.setState(State.Allocated);

            if (!update(ip.getAddress(), ip)) {
                throw new CloudRuntimeException("Unable to update a locked ip address " + ip.getAddress());
            }
            ipStringList.add(ip.getAddress());
        }
        txn.commit();
        return ipStringList;
    }

    @Override
    public void setIpAsSourceNat(String ipAddr) {

        IPAddressVO ip = createForUpdate(ipAddr);
        ip.setSourceNat(true);
        s_logger.debug("Setting " + ipAddr + " as source Nat ");
        update(ipAddr, ip);
    }

    @Override
    @DB
    public IPAddressVO assignIpAddress(long accountId, long domainId, long vlanDbId, boolean sourceNat) {
        Transaction txn = Transaction.currentTxn();
        txn.start();

        SearchCriteria<IPAddressVO> sc = VlanDbIdSearchUnallocated.create();
        sc.setParameters("vlanDbId", vlanDbId);
        
        Filter filter = new Filter(IPAddressVO.class, "vlanId", true, 0l, 1l);

        List<IPAddressVO> ips = this.lockRows(sc, filter, true);
        if (ips.size() == 0) {
            s_logger.info("Unable to get an ip address in " + vlanDbId);
            return null;
        }
        
        IPAddressVO ip = ips.get(0);

        ip.setAllocatedToAccountId(accountId);
        ip.setAllocatedTime(new Date());
        ip.setAllocatedInDomainId(domainId);
        ip.setSourceNat(sourceNat);
        ip.setState(State.Allocated);

        if (!update(ip.getAddress(), ip)) {
            throw new CloudRuntimeException("How can I lock the row but can't update it: " + ip.getAddress());
        }

        txn.commit();
        return ip;
    }

    @Override
    public void unassignIpAddress(String ipAddress) {
        IPAddressVO address = createForUpdate();
        address.setAllocatedToAccountId(null);
        address.setAllocatedInDomainId(null);
        address.setAllocatedTime(null);
        address.setSourceNat(false);
        address.setOneToOneNat(false);
        address.setState(State.Free);
        update(ipAddress, address);
    }

    @Override
    public void unassignIpAsSourceNat(String ipAddress) {
        IPAddressVO address = createForUpdate();
        address.setSourceNat(false);
        update(ipAddress, address);
    }

    @Override
    public List<IPAddressVO> listByAccount(long accountId) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<IPAddressVO> listByDcIdIpAddress(long dcId, String ipAddress) {
        SearchCriteria<IPAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("dataCenterId", dcId);
        sc.setParameters("ipAddress", ipAddress);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public int countIPs(long dcId, long vlanId, boolean onlyCountAllocated) {
        SearchCriteria<Integer> sc = onlyCountAllocated ? AllocatedIpCount.create() : AllIpCount.create();
        sc.setParameters("dc", dcId);
        sc.setParameters("vlan", vlanId);

        return customSearch(sc, null).get(0);
    }

    @Override
    @DB
    public int countIPs(long dcId, Long accountId, String vlanId, String vlanGateway, String vlanNetmask) {
        Transaction txn = Transaction.currentTxn();
        int ipCount = 0;
        try {
            String sql = "SELECT count(*) FROM user_ip_address u INNER JOIN vlan v on (u.vlan_db_id = v.id AND v.data_center_id = ? AND v.vlan_id = ? AND v.vlan_gateway = ? AND v.vlan_netmask = ? AND u.account_id = ?)";

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
            s_logger.warn("Exception counting IP addresses", e);
        }

        return ipCount;
    }
}
