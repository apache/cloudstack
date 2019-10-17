/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.globo.globodns.cloudstack.commands;

import com.cloud.agent.api.Command;

public class RemoveRecordCommand extends Command {

    private String recordName;

    private String recordIp;

    private String networkDomain;

    private boolean override;

    public RemoveRecordCommand(String recordName, String recordIp, String networkDomain, boolean override) {
        this.recordName = recordName;
        this.recordIp = recordIp;
        this.networkDomain = networkDomain;
        this.override = override;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getRecordName() {
        return recordName;
    }

    public String getRecordIp() {
        return recordIp;
    }

    public String getNetworkDomain() {
        return networkDomain;
    }

    public boolean isOverride() {
        return override;
    }

}
