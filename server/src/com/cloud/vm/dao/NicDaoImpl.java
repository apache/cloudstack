/**
 * 
 */
package com.cloud.vm.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.NicVO;

@Local(value=NicDao.class)
public class NicDaoImpl extends GenericDaoBase<NicVO, Long> implements NicDao {
    private final SearchBuilder<NicVO> InstanceSearch;
    
    protected NicDaoImpl() {
        super();
        
        InstanceSearch = createSearchBuilder();
        InstanceSearch.and("instance", InstanceSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        InstanceSearch.done();
    }
    
    @Override
    public List<NicVO> listBy(long instanceId) {
        SearchCriteria<NicVO> sc = InstanceSearch.create();
        sc.setParameters("instance", instanceId);
        return listBy(sc);
    }
    
}
