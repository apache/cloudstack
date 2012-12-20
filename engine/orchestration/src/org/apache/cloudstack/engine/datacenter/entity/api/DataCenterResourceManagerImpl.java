package org.apache.cloudstack.engine.datacenter.entity.api;

import javax.inject.Inject;

import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State;
import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State.Event;
import org.apache.cloudstack.engine.datacenter.entity.api.db.ClusterVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.DataCenterVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.HostPodVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.ClusterDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.DataCenterDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.HostPodDao;
import org.springframework.stereotype.Component;


import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

@Component
public class DataCenterResourceManagerImpl implements DataCenterResourceManager {

	@Inject
    DataCenterDao _dataCenterDao;

	@Inject
	HostPodDao _podDao;

	@Inject
	ClusterDao _clusterDao;
	
    protected StateMachine2<State, Event, DataCenterResourceEntity> _stateMachine = DataCenterResourceEntity.State.s_fsm;

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
	public boolean changeState(DataCenterResourceEntity entity, Event event) throws NoTransitionException {
		
		if(entity  instanceof ZoneEntity){
				return _stateMachine.transitTo((DataCenterResourceEntity)entity, event, null, _dataCenterDao);
		}else if(entity  instanceof PodEntity){
			return _stateMachine.transitTo((DataCenterResourceEntity)entity, event, null, _podDao);
		}else if(entity  instanceof ClusterEntity){
			return _stateMachine.transitTo((DataCenterResourceEntity)entity, event, null, _clusterDao);
		}

		return false;
	}

	@Override
	public HostPodVO loadPod(String uuid) {
		HostPodVO pod = _podDao.findByUUID(uuid);
    	if(pod == null){
    		throw new InvalidParameterValueException("Pod does not exist");
    	}
		return pod;
	}

	@Override
	public ClusterVO loadCluster(String uuid) {
		ClusterVO cluster = _clusterDao.findByUUID(uuid);
    	if(cluster == null){
    		throw new InvalidParameterValueException("Pod does not exist");
    	}
		return cluster;
	}

	@Override
	public void savePod(HostPodVO pod) {
		_podDao.persist(pod);
	}

	@Override
	public void saveCluster(ClusterVO cluster) {
		_clusterDao.persist(cluster);		
	}

}
