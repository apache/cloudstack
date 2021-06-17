//
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
//

package com.cloud.agent.api;

import com.cloud.network.element.NetworkElement;
import com.cloud.utils.exception.ExceptionUtil;

public class HandleConfigDriveIsoAnswer extends Answer {

    @LogLevel(LogLevel.Log4jLevel.Off)
    private NetworkElement.Location location = NetworkElement.Location.SECONDARY;

    public HandleConfigDriveIsoAnswer(final HandleConfigDriveIsoCommand cmd) {
        super(cmd);
    }

    public HandleConfigDriveIsoAnswer(final HandleConfigDriveIsoCommand cmd, final NetworkElement.Location location) {
        super(cmd);
        this.location = location;
    }

    public HandleConfigDriveIsoAnswer(final HandleConfigDriveIsoCommand cmd, final NetworkElement.Location location, final String details) {
        super(cmd, true, details);
        this.location = location;
    }

    public HandleConfigDriveIsoAnswer(final HandleConfigDriveIsoCommand cmd, final String details) {
        super(cmd, false, details);
    }

    public HandleConfigDriveIsoAnswer(final HandleConfigDriveIsoCommand cmd, final Exception e) {
        this(cmd, ExceptionUtil.toString(e));
    }

    public NetworkElement.Location getConfigDriveLocation() {
        return location;
    }
}
