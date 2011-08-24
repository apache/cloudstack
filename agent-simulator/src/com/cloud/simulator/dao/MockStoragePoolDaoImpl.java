package com.cloud.simulator.dao;

import javax.ejb.Local;

import com.cloud.simulator.MockStoragePoolVO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={MockStoragePoolDao.class})
public class MockStoragePoolDaoImpl extends GenericDaoBase<MockStoragePoolVO, Long> implements MockStoragePoolDao {
    protected final SearchBuilder<MockStoragePoolVO> uuidSearch;
    protected final SearchBuilder<MockStoragePoolVO> hostguidSearch;
    @Override
    public MockStoragePoolVO findByUuid(String uuid) {
        SearchCriteria<MockStoragePoolVO> sc = uuidSearch.create();
        sc.setParameters("uuid", uuid);
        return findOneBy(sc);
    }
    
    public MockStoragePoolDaoImpl() {
        uuidSearch = createSearchBuilder();
        uuidSearch.and("uuid", uuidSearch.entity().getUuid(), SearchCriteria.Op.EQ);
        uuidSearch.done();
        
        hostguidSearch = createSearchBuilder();
        hostguidSearch.and("hostguid", hostguidSearch.entity().getHostGuid(), SearchCriteria.Op.EQ);
        hostguidSearch.and("type", hostguidSearch.entity().getPoolType(), SearchCriteria.Op.EQ);
        hostguidSearch.done();
    }

    @Override
    public MockStoragePoolVO findByHost(String hostUuid) {
        SearchCriteria<MockStoragePoolVO> sc = hostguidSearch.create();
        sc.setParameters("hostguid", hostUuid);
        sc.setParameters("type", StoragePoolType.Filesystem.toString());
        return findOneBy(sc);
    }

}
