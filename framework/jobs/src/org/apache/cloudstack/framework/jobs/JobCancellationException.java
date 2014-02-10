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

import java.util.concurrent.CancellationException;

import com.cloud.utils.SerialVersionUID;

/**
 * This exception is fired when the job has been cancelled
 *
 */
public class JobCancellationException extends CancellationException {

    private static final long serialVersionUID = SerialVersionUID.AffinityConflictException;

    public enum Reason {
        RequestedByUser, RequestedByCaller, TimedOut;
    }

    Reason reason;

    public JobCancellationException(Reason reason) {
        super("The job was cancelled due to " + reason.toString());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

}
