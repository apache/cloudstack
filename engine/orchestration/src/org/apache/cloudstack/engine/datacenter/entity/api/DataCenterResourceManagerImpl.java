package org.apache.cloudstack.engine.datacenter.entity.api;

import javax.inject.Inject;

import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State;
import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State.Event;
import org.apache.cloudstack.engine.datacenter.entity.api.db.DataCenterVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.DataCenterDao;
import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenter;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.Pair;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.VirtualMachine;

@Component
public class DataCenterResourceManagerImpl implements DataCenterResourceManager {

   // @Inject
    DataCenterDao _dataCenterDao;
    
    protected StateMachine2<State, Event, DataCenterResourceEntity> _stateMachine;

	@Override
	public DataCenterVO loadDataCenter(String dataCenterId) {
    	DataCenterVO dataCenterVO = _dataCenterDao.findByUUID(dataCenterId);
    	if(dataCenterVO == null){
    		throw new InvalidParameterValueException("Zone does not exist");
    	}
    	return dataCenterVO;
	}

	@Override
	public void saveDataCenter(DataCenterVO dc) {
		_dataCenterDao.persist(dc);

	}

	@Override
	public boolean changeState(ZoneEntity entity, Event event) throws NoTransitionException {
		return _stateMachine.transitTo((DataCenterResourceEntity)entity, event, null, _dataCenterDao);
	}

}
