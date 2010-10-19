package com.cloud.network.security.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.network.security.SecurityGroupRulesVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={NetworkGroupRulesDao.class})
public class NetworkGroupRulesDaoImpl extends GenericDaoBase<SecurityGroupRulesVO, Long> implements NetworkGroupRulesDao {
    private SearchBuilder<SecurityGroupRulesVO> AccountGroupNameSearch;
    private SearchBuilder<SecurityGroupRulesVO> AccountSearch;

    protected NetworkGroupRulesDaoImpl() {
        AccountGroupNameSearch = createSearchBuilder();
        AccountGroupNameSearch.and("accountId", AccountGroupNameSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountGroupNameSearch.and("name", AccountGroupNameSearch.entity().getName(), SearchCriteria.Op.EQ);
        AccountGroupNameSearch.done();

        AccountSearch = createSearchBuilder();
        AccountSearch.and("accountId", AccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.done();
    }

    @Override
    public List<SecurityGroupRulesVO> listNetworkGroupRules() {
        Filter searchFilter = new Filter(SecurityGroupRulesVO.class, "id", true, null, null);
        return listAll(searchFilter);
    }

    @Override
    public List<SecurityGroupRulesVO> listNetworkGroupRules(long accountId, String groupName) {
        Filter searchFilter = new Filter(SecurityGroupRulesVO.class, "id", true, null, null);

        SearchCriteria<SecurityGroupRulesVO> sc = AccountGroupNameSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("name", groupName);

        return listBy(sc, searchFilter);
    }

    @Override
    public List<SecurityGroupRulesVO> listNetworkGroupRules(long accountId) {
        Filter searchFilter = new Filter(SecurityGroupRulesVO.class, "id", true, null, null);
        SearchCriteria<SecurityGroupRulesVO> sc = AccountSearch.create();
        sc.setParameters("accountId", accountId);

        return listBy(sc, searchFilter);
    }
}
