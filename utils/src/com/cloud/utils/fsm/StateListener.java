package com.cloud.utils.fsm;

import com.cloud.utils.component.Adapter;


public interface StateListener <S,E,V> extends Adapter{
	public boolean processStateTransitionEvent(S oldState, E event, S newState, V vo, boolean status, Long id, StateDao<S,E,V> vmDao);
}