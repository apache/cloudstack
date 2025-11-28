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

import java.util.Date;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface JobInfo extends Identity, InternalIdentity {
    public enum Status {
        IN_PROGRESS(0, false), SUCCEEDED(1, true), FAILED(2, true), CANCELLED(3, true);

        private final int value;
        private final boolean done;

        private Status(int value, boolean done) {
            this.value = value;
            this.done = done;
        }

        public int value() {
            return value;
        }

        public boolean done() {
            return done;
        }

        public static Status fromValue(int value) {
            for (Status status : Status.values()) {
                if (status.value() == value) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid status value: " + value);
        }
    }

    String getType();

    String getDispatcher();

    int getPendingSignals();

    long getUserId();

    long getAccountId();

    String getCmd();

    int getCmdVersion();

    String getCmdInfo();

    Status getStatus();

    int getProcessStatus();

    int getResultCode();

    String getResult();

    Long getInitMsid();

    Long getExecutingMsid();

    Long getCompleteMsid();

    Date getCreated();

    Date getRemoved();

    Date getLastUpdated();

    Date getLastPolled();

    String getInstanceType();

    Long getInstanceId();
}
