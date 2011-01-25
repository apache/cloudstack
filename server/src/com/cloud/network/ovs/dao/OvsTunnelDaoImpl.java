package com.cloud.network.ovs.dao;

import javax.ejb.Local;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value = { OvsTunnelDao.class })
public class OvsTunnelDaoImpl extends GenericDaoBase<OvsTunnelVO, Long>
		implements OvsTunnelDao {

	protected final SearchBuilder<OvsTunnelVO> fromToSearch;
	
	public OvsTunnelDaoImpl() {
		fromToSearch = createSearchBuilder();
		fromToSearch.and("from", fromToSearch.entity().getFrom(), Op.EQ);
		fromToSearch.and("to", fromToSearch.entity().getTo(), Op.EQ);
		fromToSearch.done();
	}
	
	@Override
	public OvsTunnelVO lockByFromAndTo(long from, long to) {
		SearchCriteria<OvsTunnelVO> sc = fromToSearch.create();
        sc.setParameters("from", from);
        sc.setParameters("to", to);
        return lockOneRandomRow(sc, true);
	}

	@Override
	@DB
	public int askKey(long from, long to) {
		int key = -1;
		
		final Transaction txn = Transaction.currentTxn();
		txn.start();
		OvsTunnelVO t = lockByFromAndTo(from, to);
		if (t != null) {
			key = t.getKey();
			t.setKey(key+1);
			update(t.getId(), t);
		}

		txn.commit();
		return key;
	}

	@Override
	public OvsTunnelVO getByFromAndTo(long from, long to) {
		SearchCriteria<OvsTunnelVO> sc = fromToSearch.create();
        sc.setParameters("from", from);
        sc.setParameters("to", to);
		return findOneBy(sc);
	}

}
