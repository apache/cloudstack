package com.cloud.simulator.dao;

import java.util.List;

import com.cloud.simulator.MockVMVO;
import com.cloud.utils.db.GenericDao;

public interface MockVMDao extends GenericDao<MockVMVO, Long> {
    public List<MockVMVO> findByHostId(long hostId);
    public List<MockVMVO> findByHostGuid(String guid);
    public MockVMVO findByVmName(String vmName);
    public MockVMVO findByVmNameAndHost(String vmName, String hostGuid);
}
