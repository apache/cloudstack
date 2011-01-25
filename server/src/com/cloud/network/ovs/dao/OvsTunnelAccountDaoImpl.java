package com.cloud.network.ovs.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value = { OvsTunnelAccountDao.class })
public class OvsTunnelAccountDaoImpl extends
		GenericDaoBase<OvsTunnelAccountVO, Long> implements OvsTunnelAccountDao {

	protected final SearchBuilder<OvsTunnelAccountVO> fromToAccountSearch;
	protected final SearchBuilder<OvsTunnelAccountVO> fromAccountSearch;
	protected final SearchBuilder<OvsTunnelAccountVO> toAccountSearch;
	
	public OvsTunnelAccountDaoImpl() {
		fromToAccountSearch = createSearchBuilder();
		fromToAccountSearch.and("from", fromToAccountSearch.entity().getFrom(), Op.EQ);
		fromToAccountSearch.and("to", fromToAccountSearch.entity().getTo(), Op.EQ);
		fromToAccountSearch.and("account", fromToAccountSearch.entity().getAccount(), Op.EQ);
		fromToAccountSearch.done();
		
		fromAccountSearch = createSearchBuilder();
		fromAccountSearch.and("from", fromAccountSearch.entity().getFrom(), Op.EQ);
		fromAccountSearch.and("account", fromAccountSearch.entity().getAccount(), Op.EQ);
		fromAccountSearch.done();
		
		toAccountSearch = createSearchBuilder();
		toAccountSearch.and("to", toAccountSearch.entity().getTo(), Op.EQ);
		toAccountSearch.and("account", toAccountSearch.entity().getAccount(), Op.EQ);
		toAccountSearch.done();
	}
	
	@Override
	public OvsTunnelAccountVO getByFromToAccount(long from, long to,
			long account) {
		SearchCriteria<OvsTunnelAccountVO> sc = fromToAccountSearch.create();
        sc.setParameters("from", from);
        sc.setParameters("to", to);
        sc.setParameters("account", account);
		return findOneBy(sc);
	}

    @Override
    public void removeByFromAccount(long from, long account) {
        SearchCriteria<OvsTunnelAccountVO> sc = fromAccountSearch.create();
        sc.setParameters("from", from);
        sc.setParameters("account", account);
        remove(sc);
    }

    @Override
    public List<OvsTunnelAccountVO> listByToAccount(long to, long account) {
        SearchCriteria<OvsTunnelAccountVO> sc = toAccountSearch.create();
        sc.setParameters("to", to);
        sc.setParameters("account", account);
        return listBy(sc);
    }

    @Override
    public void removeByFromToAccount(long from, long to, long account) {
        SearchCriteria<OvsTunnelAccountVO> sc = fromToAccountSearch.create();
        sc.setParameters("from", from);
        sc.setParameters("to", to);
        sc.setParameters("account", account);
        remove(sc);
    }

}
