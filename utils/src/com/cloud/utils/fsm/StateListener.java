/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

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
	 * @param opaque host id
	 * @param vmDao VM dao
	 * @return
	 */
	public boolean preStateTransitionEvent(S oldState, E event, S newState, V vo, boolean status, Object opaque);
	
	/**
	 * Event is triggered after state machine transition finished
	 * @param oldState VM's old state
     * @param event that triggered this VM state change
     * @param newState VM's new state
     * @param vo the VM instance
     * @param status the state transition is allowed or not
	 * @return
	 */
	public boolean postStateTransitionEvent(S oldState, E event, S newState, V vo, boolean status, Object opaque);
}