package org.apache.cloudstack.ontapsvm.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchCriteria.Func;
import org.apache.cloudstack.ontapsvm.OntapSvmVO;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class OntapSvmDaoImpl extends GenericDaoBase<OntapSvmVO, Long> implements OntapSvmDao {

    private SearchBuilder<OntapSvmVO> NetworkSearch;
    private GenericSearchBuilder<OntapSvmVO, String> NetworkIpSearch;

    public OntapSvmDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        NetworkSearch = createSearchBuilder();
        NetworkSearch.and("network", NetworkSearch.entity().getNetworkId(), Op.EQ);
        NetworkSearch.done();

        NetworkIpSearch = createSearchBuilder(String.class);
        NetworkIpSearch.select(null, Func.DISTINCT, NetworkIpSearch.entity().getiPv4Address());
        NetworkIpSearch.and("network", NetworkIpSearch.entity().getNetworkId(), Op.EQ);
        NetworkIpSearch.done();
    }

    @Override
    public List<OntapSvmVO> getSvmsForNetwork(long networkId) {
        SearchCriteria<OntapSvmVO> sc = NetworkSearch.create();
        sc.setParameters("network", networkId);
        return listBy(sc);
    }

    @Override
    public List<String> getIpsForNetwork(long networkId) {
        SearchCriteria<String> sc = NetworkIpSearch.create();
        sc.setParameters("network", networkId);
        return customSearch(sc, null);
    }
}
