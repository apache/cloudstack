/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.platform.subsystem.api.storage;

import com.cloud.utils.fsm.StateMachine;

public enum DataObjectBackupStorageOperationState {
	Copying,
	Deleting,
	Ready,
	NonOperational;
	
	public enum Event {
		Initial("Init state machine"),
		CopyingRequested("Copy operation is requested"),
		DeleteRequested("Delete operation is requested"),
		OperationSuccess("Operation successed"),
		OperationFailed("Operation failed");

		private final String _msg;
		
		private Event(String msg) {
			_msg = msg;
		}
	}
	
	   public DataObjectBackupStorageOperationState getNextState(Event a) {
	        return s_fsm.getNextState(this, a);
	    }
	
	protected static final StateMachine<DataObjectBackupStorageOperationState, Event> s_fsm = new StateMachine<DataObjectBackupStorageOperationState, Event>();
	static {
        s_fsm.addTransition(null, Event.Initial, DataObjectBackupStorageOperationState.Ready);
        s_fsm.addTransition(DataObjectBackupStorageOperationState.Ready, Event.CopyingRequested, DataObjectBackupStorageOperationState.Copying);
        s_fsm.addTransition(DataObjectBackupStorageOperationState.Copying, Event.CopyingRequested, DataObjectBackupStorageOperationState.Copying);
        s_fsm.addTransition(DataObjectBackupStorageOperationState.Copying, Event.OperationFailed, DataObjectBackupStorageOperationState.Ready);
        s_fsm.addTransition(DataObjectBackupStorageOperationState.Copying, Event.OperationSuccess, DataObjectBackupStorageOperationState.Ready);
        s_fsm.addTransition(DataObjectBackupStorageOperationState.Ready, Event.DeleteRequested, DataObjectBackupStorageOperationState.Deleting);
        s_fsm.addTransition(DataObjectBackupStorageOperationState.Deleting, Event.OperationFailed, DataObjectBackupStorageOperationState.Ready);
        s_fsm.addTransition(DataObjectBackupStorageOperationState.Deleting, Event.OperationSuccess, DataObjectBackupStorageOperationState.NonOperational);
	}
}
