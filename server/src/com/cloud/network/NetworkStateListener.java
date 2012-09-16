package com.cloud.network;

import com.cloud.event.dao.UsageEventDao;
import com.cloud.network.Network.Event;
import com.cloud.network.Network.State;
import com.cloud.network.dao.NetworkDao;
import com.cloud.utils.fsm.StateListener;

public class NetworkStateListener implements StateListener<State, Event, Network> {

    protected UsageEventDao _usageEventDao;
    protected NetworkDao _networkDao;

    public NetworkStateListener(UsageEventDao usageEventDao, NetworkDao networkDao) {
        this._usageEventDao = usageEventDao;
        this._networkDao = networkDao;
    }

    @Override
    public boolean preStateTransitionEvent(State oldState, Event event, State newState, Network vo, boolean status, Object opaque) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean postStateTransitionEvent(State oldState, Event event, State newState, Network vo, boolean status, Object opaque) {
        // TODO Auto-generated method stub
        return false;
    }

}
