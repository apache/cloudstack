package com.cloud.utils.fsm;

public interface StateListener <S,E,V> {
	public boolean processStateTransitionEvent(S oldState, E event, S newState, V vo, boolean status, Long id);
}