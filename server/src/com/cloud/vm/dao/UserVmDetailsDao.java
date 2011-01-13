package com.cloud.vm.dao;

import java.util.Map;

import com.cloud.utils.db.GenericDao;
import com.cloud.vm.UserVmDetailVO;

public interface UserVmDetailsDao extends GenericDao<UserVmDetailVO, Long> {
    Map<String, String> findDetails(long vmId);
    
    void persist(long vmId, Map<String, String> details);
    
    UserVmDetailVO findDetail(long vmId, String name);
    
	void deleteDetails(long vmId);
}
