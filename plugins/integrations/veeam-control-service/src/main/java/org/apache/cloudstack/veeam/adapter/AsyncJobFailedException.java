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

package org.apache.cloudstack.veeam.adapter;

import com.cloud.utils.exception.CloudRuntimeException;

/**
 * Thrown when a synchronous request did not get a successful result from its async job, either because
 * the job failed or was cancelled, or because waiting for it timed out or was interrupted. In the latter
 * case the job may still be running in the background.
 */
public class AsyncJobFailedException extends CloudRuntimeException {
    private final String jobUuid;

    public AsyncJobFailedException(String jobUuid, String message) {
        super(message);
        this.jobUuid = jobUuid;
    }

    public String getJobUuid() {
        return jobUuid;
    }
}
