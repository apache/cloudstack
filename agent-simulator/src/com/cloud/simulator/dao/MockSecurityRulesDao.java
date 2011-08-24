package com.cloud.simulator.dao;

import java.util.List;

import com.cloud.simulator.MockSecurityRulesVO;
import com.cloud.utils.db.GenericDao;

public interface MockSecurityRulesDao extends GenericDao<MockSecurityRulesVO, Long> {
    public MockSecurityRulesVO findByVmId(Long vmId);
    public List<MockSecurityRulesVO> findByHost(String hostGuid);
}
