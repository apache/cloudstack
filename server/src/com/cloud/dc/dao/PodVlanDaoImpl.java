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

import org.springframework.stereotype.Component;

import com.cloud.dc.PodVlanVO;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * PodVlanDaoImpl maintains the one-to-many relationship between
 */
@Component
public class PodVlanDaoImpl extends GenericDaoBase<PodVlanVO, Long> implements PodVlanDao {
    private final SearchBuilder<PodVlanVO> FreeVlanSearch;
    private final SearchBuilder<PodVlanVO> VlanPodSearch;
    private final SearchBuilder<PodVlanVO> PodSearchAllocated;
    
    public List<PodVlanVO> listAllocatedVnets(long podId) {
    	SearchCriteria<PodVlanVO> sc = PodSearchAllocated.create();
    	sc.setParameters("podId", podId);
    	return listBy(sc);
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
        SearchCriteria<PodVlanVO> sc = FreeVlanSearch.create();
        sc.setParameters("podId", podId);
        Date now = new Date();
        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            PodVlanVO vo = lockOneRandomRow(sc, true);
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
        SearchCriteria<PodVlanVO> sc = VlanPodSearch.create();
        sc.setParameters("vlan", vlan);
        sc.setParameters("podId", podId);
        sc.setParameters("account", accountId);

        PodVlanVO vo = findOneIncludingRemovedBy(sc);
        if (vo == null) {
            return;
        }

        vo.setTakenAt(null);
        vo.setAccountId(null);
        update(vo.getId(), vo);
    }
    
    public PodVlanDaoImpl() {
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
