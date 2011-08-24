package com.cloud.simulator.dao;

import java.util.List;

import com.cloud.simulator.MockVolumeVO;
import com.cloud.utils.db.GenericDao;

public interface MockVolumeDao extends GenericDao<MockVolumeVO, Long> {
    public List<MockVolumeVO> findByStorageIdAndType(long id, MockVolumeVO.MockVolumeType type);
    public MockVolumeVO findByStoragePathAndType(String path);
    public MockVolumeVO findByNameAndPool(String volumeName, String poolUUID);
    public MockVolumeVO findByName(String volumeName);
    Long findTotalStorageId(long id);
}
