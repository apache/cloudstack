package com.cloud.network.security.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.network.security.NetworkGroupRulesVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={NetworkGroupRulesDao.class})
public class NetworkGroupRulesDaoImpl extends GenericDaoBase<NetworkGroupRulesVO, Long> implements NetworkGroupRulesDao {
    private SearchBuilder<NetworkGroupRulesVO> AccountGroupNameSearch;
    private SearchBuilder<NetworkGroupRulesVO> AccountSearch;
    private SearchBuilder<NetworkGroupRulesVO> GroupSearch;


    protected NetworkGroupRulesDaoImpl() {
        AccountGroupNameSearch = createSearchBuilder();
        AccountGroupNameSearch.and("accountId", AccountGroupNameSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountGroupNameSearch.and("name", AccountGroupNameSearch.entity().getName(), SearchCriteria.Op.EQ);
        AccountGroupNameSearch.done();

        AccountSearch = createSearchBuilder();
        AccountSearch.and("accountId", AccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.done();
        
        GroupSearch = createSearchBuilder();
        GroupSearch.and("groupId", GroupSearch.entity().getId(), SearchCriteria.Op.EQ);
        GroupSearch.done();
        
    }

    @Override
    public List<NetworkGroupRulesVO> listNetworkGroupRules() {
        Filter searchFilter = new Filter(NetworkGroupRulesVO.class, "id", true, null, null);
        return listAll(searchFilter);
    }

    @Override
    public List<NetworkGroupRulesVO> listNetworkGroupRules(long accountId, String groupName) {
        Filter searchFilter = new Filter(NetworkGroupRulesVO.class, "id", true, null, null);

        SearchCriteria<NetworkGroupRulesVO> sc = AccountGroupNameSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("name", groupName);

        return listBy(sc, searchFilter);
    }

    @Override
    public List<NetworkGroupRulesVO> listNetworkGroupRules(long accountId) {
        Filter searchFilter = new Filter(NetworkGroupRulesVO.class, "id", true, null, null);
        SearchCriteria<NetworkGroupRulesVO> sc = AccountSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc, searchFilter);
    }
    
    @Override
    public List<NetworkGroupRulesVO> listNetworkRulesByGroupId(long groupId) {
        Filter searchFilter = new Filter(NetworkGroupRulesVO.class, "id", true, null, null);
        SearchCriteria<NetworkGroupRulesVO> sc = GroupSearch.create();
        sc.setParameters("groupId", groupId);
        return listBy(sc, searchFilter);
    }
}
