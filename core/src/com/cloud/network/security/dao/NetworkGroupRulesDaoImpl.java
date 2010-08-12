package com.cloud.network.security.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.network.security.NetworkGroupRulesVO;
import com.cloud.server.ManagementServer;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={NetworkGroupRulesDao.class})
public class NetworkGroupRulesDaoImpl extends GenericDaoBase<NetworkGroupRulesVO, Long> implements NetworkGroupRulesDao {
    private SearchBuilder<NetworkGroupRulesVO> AccountGroupNameSearch;
    private SearchBuilder<NetworkGroupRulesVO> AccountSearch;
    private SearchBuilder<NetworkGroupRulesVO> DomainSearch;

    private DomainDao _domainDao = null;

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
    public List<NetworkGroupRulesVO> listNetworkGroupRules() {
        Filter searchFilter = new Filter(NetworkGroupRulesVO.class, "id", true, null, null);
        return listAllActive(searchFilter);
    }

    @Override
    public List<NetworkGroupRulesVO> listNetworkGroupRules(long accountId, String groupName) {
        Filter searchFilter = new Filter(NetworkGroupRulesVO.class, "id", true, null, null);

        SearchCriteria sc = AccountGroupNameSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("name", groupName);

        return listActiveBy(sc, searchFilter);
    }

    @Override
    public List<NetworkGroupRulesVO> listNetworkGroupRules(long accountId) {
        Filter searchFilter = new Filter(NetworkGroupRulesVO.class, "id", true, null, null);
        SearchCriteria sc = AccountSearch.create();
        sc.setParameters("accountId", accountId);

        return listActiveBy(sc, searchFilter);
    }

    @Override
    public List<NetworkGroupRulesVO> listNetworkGroupRulesByDomain(long domainId, boolean recursive) {

        if (_domainDao == null) {
            ComponentLocator locator = ComponentLocator.getLocator(ManagementServer.Name);
            _domainDao = locator.getDao(DomainDao.class);

            DomainSearch = createSearchBuilder();
            DomainSearch.and("domainId", DomainSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            DomainSearch.join("domainSearch", domainSearch, DomainSearch.entity().getDomainId(), domainSearch.entity().getId());
            DomainSearch.done();
        }

        Filter searchFilter = new Filter(NetworkGroupRulesVO.class, "id", true, null, null);
        SearchCriteria sc = DomainSearch.create();

        if (!recursive) {
            sc.setParameters("domainId", domainId);
        }

        DomainVO domain = _domainDao.findById(domainId);
        if (domain != null) {
            sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
        }

        return listActiveBy(sc, searchFilter);
    }
}
