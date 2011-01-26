package com.cloud.utils.fsm;

public interface StateListener <S,E,V> {
	/**
	 * Event is triggered before state machine transition finished. 
     * If you want to get the state of vm before state machine changed, you need to listen on this event
	 * @param oldState VM's old state
	 * @param event that triggered this VM state change
	 * @param newState VM's new state
	 * @param vo the VM instance
	 * @param status the state transition is allowed or not
	 * @param id host id
	 * @param vmDao VM dao
	 * @return
	 */
	public boolean preStateTransitionEvent(S oldState, E event, S newState, V vo, boolean status, Long id);
	
	/**
	 * Event is triggered after state machine transition finished
	 * @param oldState VM's old state
     * @param event that triggered this VM state change
     * @param newState VM's new state
     * @param vo the VM instance
     * @param status the state transition is allowed or not
	 * @return
	 */
	public boolean postStateTransitionEvent(S oldState, E event, S newState, V vo, boolean status);
}