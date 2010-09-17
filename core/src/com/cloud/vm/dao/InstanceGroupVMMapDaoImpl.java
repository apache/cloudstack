package com.cloud.vm.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.InstanceGroupVMMapVO;

@Local(value={InstanceGroupVMMapDao.class})
public class InstanceGroupVMMapDaoImpl extends GenericDaoBase<InstanceGroupVMMapVO, Long> implements InstanceGroupVMMapDao{
	
	private SearchBuilder<InstanceGroupVMMapVO> ListByVmId;
	private SearchBuilder<InstanceGroupVMMapVO> ListByGroupId;
    private SearchBuilder<InstanceGroupVMMapVO> ListByVmIdGroupId;
	
	protected InstanceGroupVMMapDaoImpl() {
		ListByVmId  = createSearchBuilder();
		ListByVmId.and("instanceId", ListByVmId.entity().getInstanceId(), SearchCriteria.Op.EQ);
		ListByVmId.done();
		
		ListByGroupId  = createSearchBuilder();
		ListByGroupId.and("groupId", ListByGroupId.entity().getGroupId(), SearchCriteria.Op.EQ);
		ListByGroupId.done();
		
        ListByVmIdGroupId  = createSearchBuilder();
        ListByVmIdGroupId.and("instanceId", ListByVmIdGroupId.entity().getInstanceId(), SearchCriteria.Op.EQ);
        ListByVmIdGroupId.and("groupId", ListByVmIdGroupId.entity().getGroupId(), SearchCriteria.Op.EQ);
        ListByVmIdGroupId.done();
	}
	
    @Override
    public List<InstanceGroupVMMapVO> listByInstanceId(long vmId) {
        SearchCriteria<InstanceGroupVMMapVO> sc = ListByVmId.create();
        sc.setParameters("instanceId", vmId);
        return listBy(sc);
    }
    
    @Override
    public List<InstanceGroupVMMapVO> listByGroupId(long groupId) {
        SearchCriteria<InstanceGroupVMMapVO> sc = ListByGroupId.create();
        sc.setParameters("groupId", groupId);
        return listBy(sc);
    }
    
	@Override
	public InstanceGroupVMMapVO findByVmIdGroupId(long instanceId, long groupId) {
        SearchCriteria<InstanceGroupVMMapVO> sc = ListByVmIdGroupId.create();
        sc.setParameters("groupId", groupId);
        sc.setParameters("instanceId", instanceId);
		return findOneBy(sc);
	}

}
