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
package org.apache.cloudstack.agent.api;

import com.cloud.agent.api.Command;

import java.util.Objects;

public class NsxCommand extends Command {
    private String zoneName;
    private String accountName;
    private String domainName;

    public NsxCommand(String domainName, String accountName, String zoneName) {
        this.zoneName = zoneName;
        this.accountName = accountName;
        this.domainName = domainName;
    }

    public String getZoneName() {
        return zoneName;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getDomainName() {
        return domainName;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        NsxCommand that = (NsxCommand) o;
        return Objects.equals(zoneName, that.zoneName) && Objects.equals(accountName, that.accountName) && Objects.equals(domainName, that.domainName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), zoneName, accountName, domainName);
    }
}
