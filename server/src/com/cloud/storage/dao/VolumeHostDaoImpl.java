package com.cloud.storage.dao;


import java.util.List;

import javax.ejb.Local;

import com.cloud.host.HostVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VolumeHostVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
@Local(value={VolumeHostDao.class})
public class VolumeHostDaoImpl extends GenericDaoBase<VolumeHostVO, Long> implements VolumeHostDao {

	protected final SearchBuilder<VolumeHostVO> HostVolumeSearch;
	protected final SearchBuilder<VolumeHostVO> VolumeSearch;
	protected final SearchBuilder<VolumeHostVO> HostSearch;
	protected final SearchBuilder<VolumeHostVO> HostDestroyedSearch;
	
	VolumeHostDaoImpl(){
		HostVolumeSearch = createSearchBuilder();
		HostVolumeSearch.and("host_id", HostVolumeSearch.entity().getHostId(), SearchCriteria.Op.EQ);
		HostVolumeSearch.and("volume_id", HostVolumeSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
		HostVolumeSearch.and("destroyed", HostVolumeSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
		HostVolumeSearch.done();
		
		HostSearch = createSearchBuilder();
		HostSearch.and("host_id", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);		
		HostSearch.and("destroyed", HostSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
		HostSearch.done();
		
		VolumeSearch = createSearchBuilder();
		VolumeSearch.and("volume_id", VolumeSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
		VolumeSearch.and("destroyed", VolumeSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
		VolumeSearch.done();
		
		HostDestroyedSearch = createSearchBuilder();
		HostDestroyedSearch.and("host_id", HostDestroyedSearch.entity().getHostId(), SearchCriteria.Op.EQ);
		HostDestroyedSearch.and("destroyed", HostDestroyedSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
		HostDestroyedSearch.done();	
	}
    
    
    
	@Override
	public VolumeHostVO findByHostVolume(long hostId, long volumeId) {
		SearchCriteria<VolumeHostVO> sc = HostVolumeSearch.create();
	    sc.setParameters("host_id", hostId);
	    sc.setParameters("volume_id", volumeId);
        sc.setParameters("destroyed", false);
        return findOneIncludingRemovedBy(sc);
	}	
	
	@Override
	public VolumeHostVO findByVolumeId(long volumeId) {
	    SearchCriteria<VolumeHostVO> sc = VolumeSearch.create();
	    sc.setParameters("volume_id", volumeId);
	    sc.setParameters("destroyed", false);
	    return findOneBy(sc);
	}



	@Override
	public List<VolumeHostVO> listBySecStorage(long ssHostId) {
	    SearchCriteria<VolumeHostVO> sc = HostSearch.create();
	    sc.setParameters("host_id", ssHostId);
	    sc.setParameters("destroyed", false);
	    return listAll();
	}
	
	@Override
	public List<VolumeHostVO> listDestroyed(long hostId){
		SearchCriteria<VolumeHostVO> sc = HostDestroyedSearch.create();
		sc.setParameters("host_id", hostId);
		sc.setParameters("destroyed", true);
		return listIncludingRemovedBy(sc);
	}

}
