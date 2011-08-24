package com.cloud.simulator.dao;

import com.cloud.simulator.MockHost;
import com.cloud.simulator.MockHostVO;
import com.cloud.utils.db.GenericDao;

public interface MockHostDao extends GenericDao<MockHostVO, Long> {
    public MockHost findByGuid(String guid);
    public MockHost findByVmId(long vmId);
    public boolean removeByGuid(String guid);
}
