package com.cloud.simulator.dao;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.simulator.MockSecurityRulesVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
@Local(value={MockSecurityRulesDao.class})
public class MockSecurityRulesDaoImpl extends GenericDaoBase<MockSecurityRulesVO, Long> implements MockSecurityRulesDao {
    protected  SearchBuilder<MockSecurityRulesVO> vmIdSearch;
    protected  SearchBuilder<MockSecurityRulesVO> hostSearch;  
    @Override
    public MockSecurityRulesVO findByVmId(Long vmId) {
        SearchCriteria<MockSecurityRulesVO> sc = vmIdSearch.create();
        sc.setParameters("vmId", vmId);
        return findOneBy(sc);
    }

    @Override
    public List<MockSecurityRulesVO> findByHost(String hostGuid) {
        SearchCriteria<MockSecurityRulesVO> sc = hostSearch.create();
        sc.setParameters("host", hostGuid);
        return listBy(sc);
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        vmIdSearch = createSearchBuilder();
        vmIdSearch.and("vmId", vmIdSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        vmIdSearch.done();
        
        hostSearch = createSearchBuilder();
        hostSearch.and("host", hostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        hostSearch.done();
        
        return true;
    }
    
}
