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
package com.cloud.vm.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.State;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.DomainRouter.Role;

@Local(value = { DomainRouterDao.class })
public class DomainRouterDaoImpl extends GenericDaoBase<DomainRouterVO, Long> implements DomainRouterDao {
    private static final Logger s_logger = Logger.getLogger(DomainRouterDaoImpl.class);

    private static final String FindLonelyRoutersSql = "SELECT dr.id FROM domain_router dr, vm_instance vm WHERE dr.id=vm.id AND vm.state = 'Running' AND dr.id NOT IN (SELECT DISTINCT domain_router_id FROM user_vm uvm, vm_instance vmi WHERE (vmi.state = 'Running' OR vmi.state = 'Starting' OR vmi.state='Stopping' OR vmi.state = 'Migrating') AND vmi.id = uvm.id)";
    private static final String GetNextDhcpAddressSql = "UPDATE domain_router set dhcp_ip_address = (@LAST_DHCP:=dhcp_ip_address) + 1 WHERE id = ?";
    private static final String GetLastDhcpSql = "SELECT @LAST_DHCP";

    protected final SearchBuilder<DomainRouterVO> IdStatesSearch;
    protected final SearchBuilder<DomainRouterVO> AccountDcSearch;
    protected final SearchBuilder<DomainRouterVO> AccountDcRoleSearch;

    protected final SearchBuilder<DomainRouterVO> AccountSearch;
    protected final SearchBuilder<DomainRouterVO> DcSearch;
    protected final SearchBuilder<DomainRouterVO> IpSearch;
    protected final SearchBuilder<DomainRouterVO> HostSearch;
    protected final SearchBuilder<DomainRouterVO> HostUpSearch;
    protected final SearchBuilder<DomainRouterVO> DomainIdSearch;
    protected final SearchBuilder<DomainRouterVO> VlanDbIdSearch;
    protected final SearchBuilder<DomainRouterVO> StateChangeSearch;
    protected final Attribute _updateTimeAttr;

    protected DomainRouterDaoImpl() {
        DcSearch = createSearchBuilder();
        DcSearch.and("dc", DcSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DcSearch.done();

        IdStatesSearch = createSearchBuilder();
        IdStatesSearch.and("id", IdStatesSearch.entity().getId(), SearchCriteria.Op.EQ);
        IdStatesSearch.and("states", IdStatesSearch.entity().getState(), SearchCriteria.Op.IN);
        IdStatesSearch.done();

        AccountDcSearch = createSearchBuilder();
        AccountDcSearch.and("account", AccountDcSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountDcSearch.and("dc", AccountDcSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AccountDcSearch.done();
        
        AccountDcRoleSearch = createSearchBuilder();
        AccountDcRoleSearch.and("account", AccountDcRoleSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountDcRoleSearch.and("dc", AccountDcRoleSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AccountDcRoleSearch.and("role", AccountDcRoleSearch.entity().getRole(), SearchCriteria.Op.EQ);
        AccountDcRoleSearch.done();

        AccountSearch = createSearchBuilder();
        AccountSearch.and("account", AccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.done();

        IpSearch = createSearchBuilder();
        IpSearch.and("ip", IpSearch.entity().getPublicIpAddress(), SearchCriteria.Op.EQ);
        IpSearch.done();

        HostSearch = createSearchBuilder();
        HostSearch.and("host", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostSearch.done();

        HostUpSearch = createSearchBuilder();
        HostUpSearch.and("host", HostUpSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostUpSearch.and("states", HostUpSearch.entity().getState(), SearchCriteria.Op.NIN);
        HostUpSearch.done();

        DomainIdSearch = createSearchBuilder();
        DomainIdSearch.and("domainId", DomainIdSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        DomainIdSearch.done();

        VlanDbIdSearch = createSearchBuilder();
        VlanDbIdSearch.and("vlanDbId", VlanDbIdSearch.entity().getVlanDbId(), SearchCriteria.Op.EQ);
        VlanDbIdSearch.done();

        StateChangeSearch = createSearchBuilder();
        StateChangeSearch.and("id", StateChangeSearch.entity().getId(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("states", StateChangeSearch.entity().getState(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("host", StateChangeSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("update", StateChangeSearch.entity().getUpdated(), SearchCriteria.Op.EQ);
        StateChangeSearch.done();

        _updateTimeAttr = _allAttributes.get("updateTime");
        assert _updateTimeAttr != null : "Couldn't get this updateTime attribute";
    }

    @Override
    public DomainRouterVO findByPublicIpAddress(String ipAddress) {
        SearchCriteria sc = IpSearch.create();
        sc.setParameters("ip", ipAddress);
        return findOneActiveBy(sc);
    }

    @Override
    public boolean remove(Long id) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        DomainRouterVO router = createForUpdate();
        router.setPublicIpAddress(null);
        UpdateBuilder ub = getUpdateBuilder(router);
        ub.set(router, "state", State.Destroyed);
        update(id, ub);

        boolean result = super.remove(id);
        txn.commit();
        return result;
    }

    @Override
    public boolean updateIf(DomainRouterVO router, VirtualMachine.Event event, Long hostId) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("updateIf called on " + router.toString() + " event " + event.toString() + " host " + hostId);
        }
        State oldState = router.getState();
        State newState = oldState.getNextState(event);
        long oldDate = router.getUpdated();

        Long oldHostId = router.getHostId();

        if (newState == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("There's no way to transition from old state: " + oldState.toString() + " event: " + event.toString());
            }
            return false;
        }

        SearchCriteria sc = StateChangeSearch.create();
        sc.setParameters("id", router.getId());
        sc.setParameters("states", oldState);
        sc.setParameters("host", router.getHostId());
        sc.setParameters("update", router.getUpdated());

        router.incrUpdated();
        UpdateBuilder ub = getUpdateBuilder(router);
        if(newState == State.Running) {
        	// save current running host id
        	ub.set(router, "lastHostId", router.getHostId());
        }
        
        ub.set(router, "state", newState);
        ub.set(router, "hostId", hostId);
        ub.set(router, _updateTimeAttr, new Date());

        int result = update(router, sc);
        if (result == 0 && s_logger.isDebugEnabled()) {
            DomainRouterVO vo = findById(router.getId());
            StringBuilder str = new StringBuilder("Unable to update ").append(vo.toString());
            str.append(": DB Data={Host=").append(vo.getHostId()).append("; State=").append(vo.getState().toString()).append("; updated=").append(
                    vo.getUpdated());
            str.append("} New Data: {Host=").append(router.getHostId()).append("; State=").append(router.getState().toString()).append("; updated=").append(
                    router.getUpdated());
            str.append("} Stale Data: {Host=").append(oldHostId).append("; State=").append(oldState.toString()).append("; updated=").append(oldDate)
                    .append("}");
            s_logger.debug(str.toString());
        }

        return result > 0;
    }

    @Override
    public List<DomainRouterVO> listByDataCenter(long dcId) {
        SearchCriteria sc = DcSearch.create();
        sc.setParameters("dc", dcId);
        return listActiveBy(sc);
    }

    @Override
    public DomainRouterVO findBy(long accountId, long dcId) {
        SearchCriteria sc = AccountDcRoleSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("dc", dcId);
        sc.setParameters("role", Role.DHCP_FIREWALL_LB_PASSWD_USERDATA);
        return findOneActiveBy(sc);
    }
    
    @Override
    public DomainRouterVO findBy(long accountId, long dcId, Role role) {
        SearchCriteria sc = AccountDcRoleSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("dc", dcId);
        sc.setParameters("role", role);
        return findOneActiveBy(sc);
    }

    @Override
    public List<DomainRouterVO> listBy(long accountId) {
        SearchCriteria sc = AccountSearch.create();
        sc.setParameters("account", accountId);
        return listActiveBy(sc);
    }

    @Override
    public List<DomainRouterVO> listByHostId(Long hostId) {
        SearchCriteria sc = HostSearch.create();
        sc.setParameters("host", hostId);
        return listActiveBy(sc);
    }
    
    @Override
    public List<DomainRouterVO> listUpByHostId(Long hostId) {
        SearchCriteria sc = HostUpSearch.create();
        sc.setParameters("host", hostId);
        sc.setParameters("states", new Object[] {State.Destroyed, State.Stopped, State.Expunging});
        return listActiveBy(sc);
    }

    @Override
    public List<Long> findLonelyRouters() {
        ArrayList<Long> ids = new ArrayList<Long>();
        PreparedStatement pstmt = null;
        Transaction txn = Transaction.currentTxn();
        try {
            pstmt = txn.prepareAutoCloseStatement(FindLonelyRoutersSql);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ids.add(rs.getLong(1));
            }

        } catch (SQLException e) {
            throw new CloudRuntimeException("Problem finding routers: " + pstmt.toString(), e);
        }
        return ids;
    }

    @Override
    public long getNextDhcpIpAddress(long id) {
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(GetNextDhcpAddressSql);
            pstmt.setLong(1, id);
            pstmt.executeUpdate();

            pstmt = txn.prepareAutoCloseStatement(GetLastDhcpSql);
            ResultSet rs = pstmt.executeQuery();
            if (rs == null || !rs.next()) {
                throw new CloudRuntimeException("Unable to fetch a sequence with " + pstmt.toString());
            }

            long result = rs.getLong(1);
            return result;
        } catch (SQLException e) {
            txn.rollback();
            s_logger.warn("DB Exception", e);
            throw new CloudRuntimeException("DB Exception on " + pstmt.toString(), e);
        }
    }

    @Override
    public List<DomainRouterVO> listByDomain(Long domainId) {
        SearchCriteria sc = DomainIdSearch.create();
        sc.setParameters("domainId", domainId);
        return listBy(sc);
    }

    @Override
    public List<DomainRouterVO> listByVlanDbId(Long vlanDbId) {
        SearchCriteria sc = VlanDbIdSearch.create();
        sc.setParameters("vlanDbId", vlanDbId);
        return listBy(sc);
    }
}
