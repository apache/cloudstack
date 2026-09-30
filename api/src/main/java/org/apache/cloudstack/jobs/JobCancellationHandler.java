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

/** Lets the job layer ask whoever runs a job's backend work whether it can be stopped, and stop it. An interface, not AgentManager: the job framework is built first. */
public interface JobCancellationHandler {

    /** Asked before the job is marked cancelled; a job whose work cannot be stopped is refused, not recorded as cancelled. */
    boolean isJobExecutionCancellable(long jobId);

    /** Returns true only if everything in flight was actually stopped. */
    boolean cancelJobExecution(long jobId, String reason);
}
