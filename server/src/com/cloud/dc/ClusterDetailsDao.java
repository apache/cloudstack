package com.cloud.dc;

import java.util.Map;

import com.cloud.utils.db.GenericDao;

public interface ClusterDetailsDao extends GenericDao<ClusterDetailsVO, Long> {
    Map<String, String> findDetails(long clusterId);
    
    void persist(long clusterId, Map<String, String> details);
    
    ClusterDetailsVO findDetail(long clusterId, String name);

	void deleteDetails(long clusterId);
}
