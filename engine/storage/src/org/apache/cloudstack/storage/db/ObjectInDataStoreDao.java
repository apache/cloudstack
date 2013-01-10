package org.apache.cloudstack.storage.db;

import org.apache.cloudstack.storage.volume.ObjectInDataStoreStateMachine;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;

public interface ObjectInDataStoreDao extends GenericDao<ObjectInDataStoreVO, Long>, StateDao<ObjectInDataStoreStateMachine.State, ObjectInDataStoreStateMachine.Event, ObjectInDataStoreVO>  {

}
