package com.cloud.simulator.dao;

import javax.ejb.Local;

import com.cloud.simulator.MockSecStorageVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={MockSecStorageDao.class})
public class MockSecStorageDaoImpl extends GenericDaoBase<MockSecStorageVO, Long> implements MockSecStorageDao {
    protected final SearchBuilder<MockSecStorageVO> urlSearch;  
    @Override
    public MockSecStorageVO findByUrl(String url) {
        SearchCriteria<MockSecStorageVO> sc = urlSearch.create();
        sc.setParameters("url", url);
        return findOneBy(sc);
    }
    
    public MockSecStorageDaoImpl() {
        urlSearch = createSearchBuilder();
        urlSearch.and("url", urlSearch.entity().getUrl(), SearchCriteria.Op.EQ);
        urlSearch.done();
    }

}
