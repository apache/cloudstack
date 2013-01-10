package org.apache.cloudstack.storage.db;



import org.apache.cloudstack.storage.volume.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.storage.volume.ObjectInDataStoreStateMachine.State;

import com.cloud.utils.db.GenericDaoBase;


public class ObjectInDataStoreDaoImpl extends GenericDaoBase<ObjectInDataStoreVO, Long> implements ObjectInDataStoreDao {

    @Override
    public boolean updateState(State currentState, Event event,
            State nextState, ObjectInDataStoreVO vo, Object data) {
        // TODO Auto-generated method stub
        return false;
    }

}
