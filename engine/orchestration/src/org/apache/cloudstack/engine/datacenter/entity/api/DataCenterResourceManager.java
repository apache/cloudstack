package org.apache.cloudstack.engine.datacenter.entity.api;


import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State.Event;
import org.apache.cloudstack.engine.datacenter.entity.api.db.DataCenterVO;

import com.cloud.utils.fsm.NoTransitionException;



public interface DataCenterResourceManager {
	
	DataCenterVO loadDataCenter(String dataCenterId);
	
	void saveDataCenter(DataCenterVO dc);
	
	boolean changeState(ZoneEntity dc, Event event) throws NoTransitionException;

}
