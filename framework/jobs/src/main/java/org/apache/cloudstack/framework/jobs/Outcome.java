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
package org.apache.cloudstack.framework.jobs;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Outcome is returned by clients of jobs framework as a way to wait for the
 * outcome of a job.  It fully complies with how Future interface is designed.
 * In addition, it allows the callee to file a task to be scheduled when the
 * job completes.
 *
 * Note that the callee should schedule a job when using the Task interface.
 * It shouldn't try to complete the job in the schedule code as that will take
 * up threads in the jobs framework.
 *
 * For the client of the jobs framework, you can either use the OutcomeImpl
 * class to implement this interface or you can add to this interface to
 * allow for your specific exceptions to be thrown.
 *
 * @param <T> Object returned to the callee when the job completes
 */
public interface Outcome<T> extends Future<T> {
    AsyncJob getJob();

    /**
     * In addition to the normal Future methods, Outcome allows the ability
     * to register a schedule task to be performed when the job is completed.
     *
     * @param listener
     */
    void execute(Task<T> task);

    void execute(Task<T> task, long wait, TimeUnit unit);

    /**
     * Listener is used by Outcome to schedule a task to run when a job
     * completes.
     *
     * @param <T> T result returned
     */
    public interface Task<T> extends Runnable {
        void schedule(AsyncJobExecutionContext context, T result);

        void scheduleOnError(AsyncJobExecutionContext context, Throwable e);
    }
}
