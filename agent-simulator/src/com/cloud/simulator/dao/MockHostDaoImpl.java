package com.cloud.simulator.dao;

import javax.ejb.Local;

import com.cloud.simulator.MockHost;
import com.cloud.simulator.MockHostVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={MockHostDao.class})
public class MockHostDaoImpl extends GenericDaoBase<MockHostVO, Long> implements MockHostDao {
    protected final SearchBuilder<MockHostVO> GuidSearch; 
    public MockHostDaoImpl() {
        GuidSearch = createSearchBuilder();
        GuidSearch.and("guid", GuidSearch.entity().getGuid(), SearchCriteria.Op.EQ);
        GuidSearch.done();
    }
    @Override
    public MockHost findByGuid(String guid) {
        SearchCriteria<MockHostVO> sc = GuidSearch.create();
        sc.setParameters("guid", guid);
        return findOneBy(sc);
    }
    @Override
    public MockHost findByVmId(long vmId) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public boolean removeByGuid(String guid) {
       MockHost host = this.findByGuid(guid);
       if (host == null) {
           return false;
       }
       return this.remove(host.getId());
    }

}
