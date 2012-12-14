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
package org.apache.cloudstack.framework.codestyle;

import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCallbackDriver;
import org.apache.cloudstack.framework.async.AsyncCallbackHandler;

public class AsyncSampleEventDrivenStyleCaller {
	AsyncSampleCallee _ds = new AsyncSampleCallee();
	AsyncCallbackDriver _callbackDriver;
	
	public void MethodThatWillCallAsyncMethod() {
		Object vol = new Object();
		_ds.createVolume(vol,
			new AsyncCallbackDispatcher(this)
				.setOperationName("volume.create")
				.setContextParam("origVolume", vol)
				);
	}

	@AsyncCallbackHandler(operationName="volume.create")
	public void HandleVolumeCreateAsyncCallback(AsyncCallbackDispatcher callback) {
		Object origVol = callback.getContextParam("origVolume");
		
		Object resultVol = callback.getResult();
	}
	
	public static void main(String[] args) {
		AsyncSampleEventDrivenStyleCaller caller = new AsyncSampleEventDrivenStyleCaller();
		caller.MethodThatWillCallAsyncMethod();
	}
}
