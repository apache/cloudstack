package com.cloud.network.dao;

import com.cloud.network.element.TungstenProviderVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

@Component
@DB()
public class TungstenProviderDaoImpl extends GenericDaoBase<TungstenProviderVO, Long>
        implements TungstenProviderDao {

    final SearchBuilder<TungstenProviderVO> AllFieldsSearch;

    public TungstenProviderDaoImpl() {
        super();
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(),
                SearchCriteria.Op.EQ);
        AllFieldsSearch.and("uuid", AllFieldsSearch.entity().getUuid(),
                SearchCriteria.Op.EQ);
        AllFieldsSearch.and("hostname", AllFieldsSearch.entity().getHostname(),
                SearchCriteria.Op.EQ);
        AllFieldsSearch.and("provider_name", AllFieldsSearch.entity().getProviderName(),
                SearchCriteria.Op.EQ);
        AllFieldsSearch.and("port", AllFieldsSearch.entity().getPort(),
                SearchCriteria.Op.EQ);
        AllFieldsSearch.and("vrouter", AllFieldsSearch.entity().getVrouter(),
            SearchCriteria.Op.EQ);
        AllFieldsSearch.and("vrouter_port", AllFieldsSearch.entity().getVrouterPort(),
            SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public TungstenProviderVO findByNspId(long nspId) {
        SearchCriteria<TungstenProviderVO> sc = AllFieldsSearch.create();
        sc.setParameters("nsp_id", nspId);
        return findOneBy(sc);
    }

    @Override
    public TungstenProviderVO findByUuid(String uuid) {
        SearchCriteria<TungstenProviderVO> sc = AllFieldsSearch.create();
        sc.setParameters("uuid", uuid);
        return findOneBy(sc);
    }

    @Override
    public void deleteProviderByUuid(String providerUuid) {
        SearchCriteria<TungstenProviderVO> sc = AllFieldsSearch.create();
        sc.setParameters("uuid", providerUuid);
        remove(sc);
    }
}
