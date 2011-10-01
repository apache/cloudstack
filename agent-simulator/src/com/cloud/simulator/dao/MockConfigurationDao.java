package com.cloud.simulator.dao;

import com.cloud.simulator.MockConfigurationVO;
import com.cloud.utils.db.GenericDao;

public interface MockConfigurationDao extends GenericDao<MockConfigurationVO, Long> {
    MockConfigurationVO findByCommand(Long dcId, Long podId, Long clusterId, Long hostId, String name);

	MockConfigurationVO findByNameBottomUP(Long dcId, Long podId,
			Long clusterId, Long hostId, String name);
}
