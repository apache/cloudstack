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

package com.cloud.network.security.dao;

import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import com.cloud.ha.WorkVO;
import com.cloud.network.security.NetworkGroupWorkVO;
import com.cloud.network.security.NetworkGroupWorkVO.Step;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value={NetworkGroupWorkDao.class})
public class NetworkGroupWorkDaoImpl extends GenericDaoBase<NetworkGroupWorkVO, Long> implements NetworkGroupWorkDao {
    private SearchBuilder<NetworkGroupWorkVO> VmIdTakenSearch;
    private SearchBuilder<NetworkGroupWorkVO> VmIdSeqNumSearch;
    private SearchBuilder<NetworkGroupWorkVO> VmIdUnTakenSearch;
    private SearchBuilder<NetworkGroupWorkVO> UntakenWorkSearch;
    private SearchBuilder<NetworkGroupWorkVO> VmIdStepSearch;
    private SearchBuilder<NetworkGroupWorkVO> CleanupSearch;


    protected NetworkGroupWorkDaoImpl() {
        VmIdTakenSearch = createSearchBuilder();
        VmIdTakenSearch.and("vmId", VmIdTakenSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        VmIdTakenSearch.and("taken", VmIdTakenSearch.entity().getDateTaken(), SearchCriteria.Op.NNULL);

        VmIdTakenSearch.done();
        
        VmIdUnTakenSearch = createSearchBuilder();
        VmIdUnTakenSearch.and("vmId", VmIdUnTakenSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        VmIdUnTakenSearch.and("taken", VmIdUnTakenSearch.entity().getDateTaken(), SearchCriteria.Op.NULL);

        VmIdUnTakenSearch.done();
        
        UntakenWorkSearch = createSearchBuilder();
        UntakenWorkSearch.and("server", UntakenWorkSearch.entity().getServerId(), SearchCriteria.Op.NULL);
        UntakenWorkSearch.and("taken", UntakenWorkSearch.entity().getDateTaken(), SearchCriteria.Op.NULL);
        UntakenWorkSearch.and("step", UntakenWorkSearch.entity().getStep(), SearchCriteria.Op.EQ);

        UntakenWorkSearch.done();
        
        VmIdSeqNumSearch = createSearchBuilder();
        VmIdSeqNumSearch.and("vmId", VmIdSeqNumSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        VmIdSeqNumSearch.and("seqno", VmIdSeqNumSearch.entity().getLogsequenceNumber(), SearchCriteria.Op.EQ);

        VmIdSeqNumSearch.done();
        
        VmIdStepSearch = createSearchBuilder();
        VmIdStepSearch.and("vmId", VmIdStepSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        VmIdStepSearch.and("step", VmIdStepSearch.entity().getStep(), SearchCriteria.Op.EQ);

        VmIdStepSearch.done();
        
        CleanupSearch = createSearchBuilder();
        CleanupSearch.and("taken", CleanupSearch.entity().getDateTaken(), Op.LTEQ);
        CleanupSearch.and("step", CleanupSearch.entity().getStep(), SearchCriteria.Op.IN);

        CleanupSearch.done();
        

    }

    @Override
    public NetworkGroupWorkVO findByVmId(long vmId, boolean taken) {
        SearchCriteria sc = taken?VmIdTakenSearch.create():VmIdUnTakenSearch.create();
        sc.setParameters("vmId", vmId);
        return findOneBy(sc);
    }

	@Override
	public NetworkGroupWorkVO take(long serverId) {
		final Transaction txn = Transaction.currentTxn();
        try {
            final SearchCriteria sc = UntakenWorkSearch.create();
            sc.setParameters("step", Step.Scheduled);

            final Filter filter = new Filter(NetworkGroupWorkVO.class, null, true, 0l, 1l);//FIXME: order desc by update time?

            txn.start();
            final List<NetworkGroupWorkVO> vos = lock(sc, filter, true);
            if (vos.size() == 0) {
                txn.commit();
                return null;
            }
            NetworkGroupWorkVO work = null;
            for (NetworkGroupWorkVO w: vos) {       
            	//ensure that there is no job in Processing state for the same VM
            	if ( findByVmIdStep(w.getInstanceId(), Step.Processing) == null) {
            		work = w;
            		break;
            	}
            }
            if (work == null) {
            	txn.commit();
            	return null;
            }
            work.setServerId(serverId);
            work.setDateTaken(new Date());
            work.setStep(NetworkGroupWorkVO.Step.Processing);

            update(work.getId(), work);

            txn.commit();

            return work;

        } catch (final Throwable e) {
            throw new CloudRuntimeException("Unable to execute take", e);
        }
	}

	@Override
	public void updateStep(Long vmId, Long logSequenceNumber, Step step) {
		final Transaction txn = Transaction.currentTxn();
		txn.start();
        SearchCriteria sc = VmIdSeqNumSearch.create();
        sc.setParameters("vmId", vmId);
        sc.setParameters("seqno", logSequenceNumber);
        
        final Filter filter = new Filter(WorkVO.class, null, true, 0l, 1l);

        final List<NetworkGroupWorkVO> vos = lock(sc, filter, true);
        if (vos.size() == 0) {
            txn.commit();
            return;
        }
        NetworkGroupWorkVO work = vos.get(0);
        work.setStep(step);
        update(work.getId(), work);

        txn.commit();
	}

	@Override
	public NetworkGroupWorkVO findByVmIdStep(long vmId, Step step) {
        SearchCriteria sc = VmIdStepSearch.create();
        sc.setParameters("vmId", vmId);
        sc.setParameters("step", step);
        return findOneBy(sc);
	}

	@Override
	public void updateStep(Long workId, Step step) {
		final Transaction txn = Transaction.currentTxn();
		txn.start();
        
        NetworkGroupWorkVO work = lock(workId, true);
        if (work == null) {
        	txn.commit();
        	return;
        }
        work.setStep(step);
        update(work.getId(), work);

        txn.commit();
		
	}

	@Override
	public int deleteFinishedWork(Date timeBefore) {
		final SearchCriteria sc = CleanupSearch.create();
		sc.setParameters("taken", timeBefore);
		sc.setParameters("step", Step.Done);

		return delete(sc);
	}

	@Override
	public List<NetworkGroupWorkVO> findUnfinishedWork(Date timeBefore) {
		final SearchCriteria sc = CleanupSearch.create();
		sc.setParameters("taken", timeBefore);
		sc.setParameters("step", Step.Processing);

		List<NetworkGroupWorkVO> result = listBy(sc);
		
		NetworkGroupWorkVO work = createForUpdate();
		work.setStep(Step.Error);
		update(work, sc);
		
		return result;
	}

    
}
