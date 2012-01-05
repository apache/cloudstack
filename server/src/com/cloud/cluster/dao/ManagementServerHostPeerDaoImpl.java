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

package com.cloud.cluster.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.cluster.ManagementServerHost;
import com.cloud.cluster.ManagementServerHostPeerVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value={ManagementServerHostPeerDao.class})
public class ManagementServerHostPeerDaoImpl extends GenericDaoBase<ManagementServerHostPeerVO, Long> implements ManagementServerHostPeerDao {
    private static final Logger s_logger = Logger.getLogger(ManagementServerHostPeerDaoImpl.class);
    
    private final SearchBuilder<ManagementServerHostPeerVO> ClearPeerSearch;
    private final SearchBuilder<ManagementServerHostPeerVO> FindForUpdateSearch;
    private final SearchBuilder<ManagementServerHostPeerVO> CountSearch;

    public ManagementServerHostPeerDaoImpl() {
        ClearPeerSearch = createSearchBuilder();
        ClearPeerSearch.and("ownerMshost", ClearPeerSearch.entity().getOwnerMshost(), SearchCriteria.Op.EQ);
        ClearPeerSearch.done();
        
        FindForUpdateSearch = createSearchBuilder();
        FindForUpdateSearch.and("ownerMshost", FindForUpdateSearch.entity().getOwnerMshost(), SearchCriteria.Op.EQ);
        FindForUpdateSearch.and("peerMshost", FindForUpdateSearch.entity().getPeerMshost(), SearchCriteria.Op.EQ);
        FindForUpdateSearch.and("peerRunid", FindForUpdateSearch.entity().getPeerRunid(), SearchCriteria.Op.EQ);
        FindForUpdateSearch.done();
        
        CountSearch = createSearchBuilder();
        CountSearch.and("peerMshost", CountSearch.entity().getPeerMshost(), SearchCriteria.Op.EQ);
        CountSearch.and("peerRunid", CountSearch.entity().getPeerRunid(), SearchCriteria.Op.EQ);
        CountSearch.and("peerState", CountSearch.entity().getPeerState(), SearchCriteria.Op.EQ);
        CountSearch.done();
    }
    
    @Override
    @DB
    public void clearPeerInfo(long ownerMshost) {
        SearchCriteria<ManagementServerHostPeerVO>  sc = ClearPeerSearch.create();
        sc.setParameters("ownerMshost", ownerMshost);
        
        expunge(sc);
    }
    
    @Override
    @DB
    public void updatePeerInfo(long ownerMshost, long peerMshost, long peerRunid, ManagementServerHost.State peerState) {
        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
        
            SearchCriteria<ManagementServerHostPeerVO> sc = FindForUpdateSearch.create();
            sc.setParameters("ownerMshost", ownerMshost);
            sc.setParameters("peerMshost", peerMshost);
            sc.setParameters("peerRunid", peerRunid);
            List<ManagementServerHostPeerVO> l = listBy(sc);
            if(l.size() == 1) {
                ManagementServerHostPeerVO peer = l.get(0);
                peer.setPeerState(peerState);
                update(peer.getId(), peer);
            } else {
                ManagementServerHostPeerVO peer = new ManagementServerHostPeerVO(ownerMshost, peerMshost, peerRunid, peerState);
                persist(peer);
            }
            txn.commit();
        } catch(Exception e) {
            s_logger.warn("Unexpected exception, ", e);
            txn.rollback();
        }
    }
    
    @Override
    @DB
    public int countStateSeenInPeers(long mshost, long runid, ManagementServerHost.State state) {
        SearchCriteria<ManagementServerHostPeerVO> sc = CountSearch.create();
        sc.setParameters("peerMshost", mshost);
        sc.setParameters("peerRunid", runid);
        sc.setParameters("peerState", state);
        
        List<ManagementServerHostPeerVO> l = listBy(sc);
        return l.size();
    }
}
