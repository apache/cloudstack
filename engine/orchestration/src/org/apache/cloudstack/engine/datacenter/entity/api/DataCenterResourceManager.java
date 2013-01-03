package org.apache.cloudstack.engine.datacenter.entity.api;


import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State.Event;
import org.apache.cloudstack.engine.datacenter.entity.api.db.ClusterVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.DataCenterVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.HostPodVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.HostVO;

import com.cloud.utils.fsm.NoTransitionException;



public interface DataCenterResourceManager {
	
	DataCenterVO loadDataCenter(String dataCenterId);
	
	void saveDataCenter(DataCenterVO dc);
	
	void savePod(HostPodVO dc);
	
	void saveCluster(ClusterVO cluster);
	
	boolean changeState(DataCenterResourceEntity entity, Event event) throws NoTransitionException;
	
	HostPodVO loadPod(String uuid);
	
	ClusterVO loadCluster(String uuid);

	HostVO loadHost(String uuid);

	void saveHost(HostVO hostVO);

}
