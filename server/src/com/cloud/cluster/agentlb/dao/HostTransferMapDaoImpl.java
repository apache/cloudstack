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

package com.cloud.cluster.agentlb.dao;

import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.cluster.agentlb.HostTransferMapVO;
import com.cloud.cluster.agentlb.HostTransferMapVO.HostTransferState;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value = { HostTransferMapDao.class })
@DB(txn = false)
public class HostTransferMapDaoImpl extends GenericDaoBase<HostTransferMapVO, Long> implements HostTransferMapDao {
    private static final Logger s_logger = Logger.getLogger(HostTransferMapDaoImpl.class);

    protected final SearchBuilder<HostTransferMapVO> AllFieldsSearch;
    protected final SearchBuilder<HostTransferMapVO> IntermediateStateSearch;
    protected final SearchBuilder<HostTransferMapVO> InactiveSearch;

    public HostTransferMapDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("initialOwner", AllFieldsSearch.entity().getInitialOwner(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("futureOwner", AllFieldsSearch.entity().getFutureOwner(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
        
        IntermediateStateSearch = createSearchBuilder();
        IntermediateStateSearch.and("futureOwner", IntermediateStateSearch.entity().getFutureOwner(), SearchCriteria.Op.EQ);
        IntermediateStateSearch.and("state", IntermediateStateSearch.entity().getState(), SearchCriteria.Op.IN);
        IntermediateStateSearch.done();
        
        InactiveSearch = createSearchBuilder();
        InactiveSearch.and("created", InactiveSearch.entity().getCreated(),  SearchCriteria.Op.LTEQ);
        InactiveSearch.and("id", InactiveSearch.entity().getId(), SearchCriteria.Op.EQ);
        InactiveSearch.and("state", InactiveSearch.entity().getState(), SearchCriteria.Op.EQ);
        InactiveSearch.done();
        
    }

    @Override
    public List<HostTransferMapVO> listHostsLeavingCluster(long currentOwnerId) {
        SearchCriteria<HostTransferMapVO> sc = IntermediateStateSearch.create();
        sc.setParameters("initialOwner", currentOwnerId);

        return listBy(sc);
    }

    @Override
    public List<HostTransferMapVO> listHostsJoiningCluster(long futureOwnerId) {
        SearchCriteria<HostTransferMapVO> sc = IntermediateStateSearch.create();
        sc.setParameters("futureOwner", futureOwnerId);

        return listBy(sc);
    }
    
    @Override
    public HostTransferMapVO startAgentTransfering(long hostId, long initialOwner, long futureOwner) {
        HostTransferMapVO transfer = new HostTransferMapVO(hostId, initialOwner, futureOwner);
        return persist(transfer); 
    }

    @Override
    public boolean completeAgentTransfer(long hostId) {
        return remove(hostId);
    }
    
    @Override
    public List<HostTransferMapVO> listBy(long futureOwnerId, HostTransferState state) {
        SearchCriteria<HostTransferMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("futureOwner", futureOwnerId);
        sc.setParameters("state", state);

        return listBy(sc);
    }
    
    @Override
    public boolean isNotActive(long hostId, Date cutTime) {
        SearchCriteria<HostTransferMapVO> sc = InactiveSearch.create();
        sc.setParameters("id", hostId);
        sc.setParameters("state", HostTransferState.TransferRequested);
        sc.setParameters("created", cutTime);
        
        if (listBy(sc).isEmpty()) {
            return false;
        } else {
            return true;
        }
    }
    
    @Override
    public boolean startAgentTransfer(long hostId) {
        HostTransferMapVO transfer = findById(hostId);
        transfer.setState(HostTransferState.TransferStarted);
        return update(hostId, transfer);
    }
    
    
    @Override
    public HostTransferMapVO findByIdAndFutureOwnerId(long id, long futureOwnerId) {
        SearchCriteria<HostTransferMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("futureOwner", futureOwnerId);
        sc.setParameters("id", id);

        return findOneBy(sc);
    }
    
    
    @Override
    public HostTransferMapVO findByIdAndCurrentOwnerId(long id, long currentOwnerId) {
        SearchCriteria<HostTransferMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("initialOwner", currentOwnerId);
        sc.setParameters("id", id);

        return findOneBy(sc);
    }
    
}
