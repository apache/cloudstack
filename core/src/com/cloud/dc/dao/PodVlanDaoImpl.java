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
package com.cloud.dc.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.cloud.dc.PodVlanVO;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * PodVlanDaoImpl maintains the one-to-many relationship between
 * pod and the vlan that appears within its network.
 */
public class PodVlanDaoImpl extends GenericDaoBase<PodVlanVO, Long> implements GenericDao<PodVlanVO, Long> {
    private final SearchBuilder<PodVlanVO> FreeVlanSearch;
    private final SearchBuilder<PodVlanVO> VlanPodSearch;
    private final SearchBuilder<PodVlanVO> PodSearchAllocated;
    
    public List<PodVlanVO> listAllocatedVnets(long podId) {
    	SearchCriteria sc = PodSearchAllocated.create();
    	sc.setParameters("podId", podId);
    	return listActiveBy(sc);
    }
    
    public void add(long podId, int start, int end) {
        String insertVnet = "INSERT INTO `cloud`.`op_pod_vlan_alloc` (vlan, pod_id) VALUES ( ?, ?)";
        
        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertVnet);
            for (int i = start; i < end; i++) {
                stmt.setString(1, String.valueOf(i));
                stmt.setLong(2, podId);
                stmt.addBatch();
            }
            stmt.executeBatch();
            txn.commit();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception caught adding vnet ", e);
        }
    }
    
    public void delete(long podId) {
    	String deleteVnet = "DELETE FROM `cloud`.`op_pod_vlan_alloc` WHERE pod_id = ?";

        Transaction txn = Transaction.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(deleteVnet);
            stmt.setLong(1, podId);
            stmt.executeUpdate();
        } catch (SQLException e) {
        	throw new CloudRuntimeException("Exception caught deleting vnet ", e);
        }
    }

    public PodVlanVO take(long podId, long accountId) {
        SearchCriteria sc = FreeVlanSearch.create();
        sc.setParameters("podId", podId);
        Date now = new Date();
        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            PodVlanVO vo = lock(sc, true);
            if (vo == null) {
                return null;
            }

            vo.setTakenAt(now);
            vo.setAccountId(accountId);
            update(vo.getId(), vo);
            txn.commit();
            return vo;

        } catch (Exception e) {
            throw new CloudRuntimeException("Caught Exception ", e);
        }
    }

    public void release(String vlan, long podId, long accountId) {
        SearchCriteria sc = VlanPodSearch.create();
        sc.setParameters("vlan", vlan);
        sc.setParameters("podId", podId);
        sc.setParameters("account", accountId);

        PodVlanVO vo = findOneBy(sc);
        if (vo == null) {
            return;
        }

        vo.setTakenAt(null);
        vo.setAccountId(null);
        update(vo.getId(), vo);
    }
    
    protected PodVlanDaoImpl() {
    	super();
    	PodSearchAllocated = createSearchBuilder();
    	PodSearchAllocated.and("podId", PodSearchAllocated.entity().getPodId(), SearchCriteria.Op.EQ);
    	PodSearchAllocated.and("allocated", PodSearchAllocated.entity().getTakenAt(), SearchCriteria.Op.NNULL);
    	PodSearchAllocated.done();
        
        FreeVlanSearch = createSearchBuilder();
        FreeVlanSearch.and("podId", FreeVlanSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        FreeVlanSearch.and("taken", FreeVlanSearch.entity().getTakenAt(), SearchCriteria.Op.NULL);
        FreeVlanSearch.done();

        VlanPodSearch = createSearchBuilder();
        VlanPodSearch.and("vlan", VlanPodSearch.entity().getVlan(), SearchCriteria.Op.EQ);
        VlanPodSearch.and("podId", VlanPodSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        VlanPodSearch.and("taken", VlanPodSearch.entity().getTakenAt(), SearchCriteria.Op.NNULL);
        VlanPodSearch.and("account", VlanPodSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        VlanPodSearch.done();
    }
}
