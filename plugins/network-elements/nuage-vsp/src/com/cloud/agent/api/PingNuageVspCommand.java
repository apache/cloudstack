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

import com.cloud.host.Host;

public class PingNuageVspCommand extends PingCommand {

    private final boolean shouldAudit;

    public PingNuageVspCommand(Host.Type type, long id, boolean shouldAudit) {
        super(type, id);
        this.shouldAudit = shouldAudit;
    }

    public boolean shouldAudit() {
        return shouldAudit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PingNuageVspCommand)) return false;
        if (!super.equals(o)) return false;

        PingNuageVspCommand that = (PingNuageVspCommand) o;

        if (shouldAudit != that.shouldAudit) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (shouldAudit ? 1 : 0);
        return result;
    }
}
