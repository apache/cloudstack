package com.cloud.vm.dao;

import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.cloud.vm.InstanceGroupVMMapVO;

public interface InstanceGroupVMMapDao extends GenericDao<InstanceGroupVMMapVO, Long>{
	List<InstanceGroupVMMapVO> listByInstanceId(long instanceId);
	List<InstanceGroupVMMapVO> listByGroupId(long groupId);
	InstanceGroupVMMapVO findByVmIdGroupId(long instanceId, long groupId);
}
