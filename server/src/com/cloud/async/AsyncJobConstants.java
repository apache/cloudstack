// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.async;

public interface AsyncJobConstants {
	public static final int STATUS_IN_PROGRESS = 0;
	public static final int STATUS_SUCCEEDED = 1;
	public static final int STATUS_FAILED = 2;
	
	public static final String JOB_DISPATCHER_PSEUDO = "pseudoJobDispatcher";
	public static final String PSEUDO_JOB_INSTANCE_TYPE = "Thread";
	
	// Although we may have detailed masks for each individual wakeup event, i.e.
	// periodical timer, matched topic from message bus, it seems that we don't
	// need to distinguish them to such level. Therefore, only one wakeup signal
	// is defined
	public static final int SIGNAL_MASK_WAKEUP = 1;
}
