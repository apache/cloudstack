package com.cloud.vm.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.InstanceGroupVO;

@Local (value={InstanceGroupDao.class})
public class InstanceGroupDaoImpl extends GenericDaoBase<InstanceGroupVO, Long> implements InstanceGroupDao{
	private SearchBuilder<InstanceGroupVO> AccountIdNameSearch;
	protected final SearchBuilder<InstanceGroupVO> AccountSearch;
	
	protected InstanceGroupDaoImpl() {
        AccountSearch = createSearchBuilder();
        AccountSearch.and("account", AccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.done();
        
        AccountIdNameSearch = createSearchBuilder();
        AccountIdNameSearch.and("accountId", AccountIdNameSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountIdNameSearch.and("groupName", AccountIdNameSearch.entity().getName(), SearchCriteria.Op.EQ);
        AccountIdNameSearch.done();
        
	}
	
    @Override
    public boolean isNameInUse(Long accountId, String name) {
        SearchCriteria<InstanceGroupVO> sc = createSearchCriteria();
        sc.addAnd("name", SearchCriteria.Op.EQ, name);
        if (accountId != null) {
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        }
        List<InstanceGroupVO> vmGroups = listBy(sc);
        return ((vmGroups != null) && !vmGroups.isEmpty());
    }
    
	@Override
	public InstanceGroupVO findByAccountAndName(Long accountId, String name) {
		SearchCriteria<InstanceGroupVO> sc = AccountIdNameSearch.create();
		sc.setParameters("accountId", accountId);
		sc.setParameters("groupName", name);
		return findOneBy(sc);
	}
	
    @Override
    public void updateVmGroup(long id, String name) {
        InstanceGroupVO vo = createForUpdate();
        vo.setName(name);
        update(id, vo);
    }
    
    @Override
    public List<InstanceGroupVO> listByAccountId(long id) {
        SearchCriteria<InstanceGroupVO> sc = AccountSearch.create();
        sc.setParameters("account", id);
        return listBy(sc);
    }
}
