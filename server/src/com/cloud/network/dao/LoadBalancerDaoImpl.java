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
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.network.LoadBalancerVO;
import com.cloud.network.rules.FirewallRule.State;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;

@Local(value = { LoadBalancerDao.class })
public class LoadBalancerDaoImpl extends GenericDaoBase<LoadBalancerVO, Long> implements LoadBalancerDao {
    private static final Logger s_logger = Logger.getLogger(LoadBalancerDaoImpl.class);
    private static final String LIST_INSTANCES_BY_LOAD_BALANCER = "SELECT vm.id " +
            "    FROM vm_instance vm, load_balancer lb, ip_forwarding fwd, user_ip_address ip " +
            "    WHERE lb.id = ? AND " +
            "          fwd.group_id = lb.id AND " +
            "          fwd.forwarding = 0 AND " +
            "          fwd.private_ip_address = vm.private_ip_address AND " +
            "          lb.ip_address = ip.public_ip_address AND " +
            "          ip.data_center_id = vm.data_center_id ";
    private final SearchBuilder<LoadBalancerVO> ListByIp;
    private final SearchBuilder<LoadBalancerVO> IpAndPublicPortSearch;
    private final SearchBuilder<LoadBalancerVO> AccountAndNameSearch;
    protected final SearchBuilder<LoadBalancerVO> TransitionStateSearch;

    protected final FirewallRulesCidrsDaoImpl _portForwardingRulesCidrsDao = ComponentLocator.inject(FirewallRulesCidrsDaoImpl.class);

    protected LoadBalancerDaoImpl() {
        ListByIp = createSearchBuilder();
        ListByIp.and("ipAddressId", ListByIp.entity().getSourceIpAddressId(), SearchCriteria.Op.EQ);
        ListByIp.and("networkId", ListByIp.entity().getNetworkId(), SearchCriteria.Op.EQ);
        ListByIp.done();

        IpAndPublicPortSearch = createSearchBuilder();
        IpAndPublicPortSearch.and("ipAddressId", IpAndPublicPortSearch.entity().getSourceIpAddressId(), SearchCriteria.Op.EQ);
        IpAndPublicPortSearch.and("publicPort", IpAndPublicPortSearch.entity().getSourcePortStart(), SearchCriteria.Op.EQ);
        IpAndPublicPortSearch.done();

        AccountAndNameSearch = createSearchBuilder();
        AccountAndNameSearch.and("accountId", AccountAndNameSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountAndNameSearch.and("name", AccountAndNameSearch.entity().getName(), SearchCriteria.Op.EQ);
        AccountAndNameSearch.done();

        TransitionStateSearch = createSearchBuilder();
        TransitionStateSearch.and("networkId", TransitionStateSearch.entity().getNetworkId(), Op.EQ);
        TransitionStateSearch.and("state", TransitionStateSearch.entity().getState(), Op.IN);
        TransitionStateSearch.done();
    }

    @Override
    public List<Long> listInstancesByLoadBalancer(long loadBalancerId) {
        Transaction txn = Transaction.currentTxn();
        String sql = LIST_INSTANCES_BY_LOAD_BALANCER;
        PreparedStatement pstmt = null;
        List<Long> instanceList = new ArrayList<Long>();
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, loadBalancerId);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Long vmId = rs.getLong(1);
                instanceList.add(vmId);
            }
        } catch (Exception ex) {
            s_logger.error("error getting recent usage network stats", ex);
        }
        return instanceList;
    }

    @Override
    public List<LoadBalancerVO> listByIpAddress(long ipAddressId) {
        SearchCriteria<LoadBalancerVO> sc = ListByIp.create();
        sc.setParameters("ipAddressId", ipAddressId);
        return listBy(sc);
    }

    @Override
    public List<LoadBalancerVO> listByNetworkId(long networkId) {
        SearchCriteria<LoadBalancerVO> sc = ListByIp.create();
        sc.setParameters("networkId", networkId);
        return listBy(sc);
    }

    @Override
    public LoadBalancerVO findByIpAddressAndPublicPort(long ipAddressId, String publicPort) {
        SearchCriteria<LoadBalancerVO> sc = IpAndPublicPortSearch.create();
        sc.setParameters("ipAddressId", ipAddressId);
        sc.setParameters("publicPort", publicPort);
        return findOneBy(sc);
    }

    @Override
    public LoadBalancerVO findByAccountAndName(Long accountId, String name) {
        SearchCriteria<LoadBalancerVO> sc = AccountAndNameSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("name", name);
        return findOneBy(sc);
    }

    @Override
    public List<LoadBalancerVO> listInTransitionStateByNetworkId(long networkId) {
        SearchCriteria<LoadBalancerVO> sc = TransitionStateSearch.create();
        sc.setParameters("networkId", networkId);
        sc.setParameters("state", State.Add.toString(), State.Revoke.toString());
        return listBy(sc);
    }

}
