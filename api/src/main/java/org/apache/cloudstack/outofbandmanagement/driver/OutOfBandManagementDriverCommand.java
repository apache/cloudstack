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
package org.apache.cloudstack.outofbandmanagement.driver;

import com.google.common.collect.ImmutableMap;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;
import org.joda.time.Duration;

public abstract class OutOfBandManagementDriverCommand {
    private final ImmutableMap<OutOfBandManagement.Option, String> options;
    private final Duration timeout;

    public OutOfBandManagementDriverCommand(final ImmutableMap<OutOfBandManagement.Option, String> options, final Long timeoutSeconds) {
        this.options = options;
        if (timeoutSeconds != null && timeoutSeconds > 0) {
            this.timeout = new Duration(timeoutSeconds * 1000);
        } else {
            this.timeout = Duration.ZERO;
        }
    }

    public final ImmutableMap<OutOfBandManagement.Option, String> getOptions() {
        return options;
    }

    public final Duration getTimeout() {
        return timeout;
    }
}
