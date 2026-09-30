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
package org.apache.cloudstack.jobs;

/**
 * Lets the job layer ask whoever is running a job's backend work whether that work can be stopped,
 * and then stop it.
 *
 * This is an interface rather than a direct call to the agent manager because the job framework is
 * built before the agent layer and the agent layer already depends on it; calling the other way
 * round would close the cycle.
 */
public interface JobCancellationHandler {

    /**
     * Whether everything this job currently has in flight can be stopped.
     *
     * Asked before the job is marked cancelled. A job whose backend work cannot be stopped is
     * refused outright, rather than recorded as cancelled while the operation runs to completion --
     * that divergence between what CloudStack believes and what the hypervisor did is the whole
     * reason this check exists.
     */
    boolean isJobExecutionCancellable(long jobId);

    /**
     * Stops whatever this job has in flight. Returns true only if all of it was actually stopped.
     */
    boolean cancelJobExecution(long jobId, String reason);
}
