package com.cloud.utils.fsm;


public interface StateDao <S,E,V> {
	boolean updateState(S currentState, E event, S nextState, V vo, Long id);

}
