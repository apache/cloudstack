package com.cloud.storage.dao;


import javax.ejb.Local;

import com.cloud.storage.VolumeHostVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
@Local(value={VolumeHostDao.class})
public class VolumeHostDaoImpl extends GenericDaoBase<VolumeHostVO, Long> implements VolumeHostDao {

	protected final SearchBuilder<VolumeHostVO> HostVolumeSearch;
	
	VolumeHostDaoImpl(){
		HostVolumeSearch = createSearchBuilder();
		HostVolumeSearch.and("host_id", HostVolumeSearch.entity().getHostId(), SearchCriteria.Op.EQ);
		HostVolumeSearch.and("volume_id", HostVolumeSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
		HostVolumeSearch.and("destroyed", HostVolumeSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
		HostVolumeSearch.done();
	}
    
    
    
	@Override
	public VolumeHostVO findByHostVolume(long hostId, long volumeId) {
		SearchCriteria<VolumeHostVO> sc = HostVolumeSearch.create();
	    sc.setParameters("host_id", hostId);
	    sc.setParameters("volume_id", volumeId);
        sc.setParameters("destroyed", false);
        return findOneIncludingRemovedBy(sc);
	}	

}
