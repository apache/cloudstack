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
package com.cloud.network.rules;

public class StaticNatImpl implements StaticNat {
    long accountId;
    long domainId;
    long networkId;
    long sourceIpAddressId;
    String destIpAddress;
    String sourceMacAddress;
    boolean forRevoke;

    public StaticNatImpl(long accountId, long domainId, long networkId, long sourceIpAddressId, String destIpAddress, boolean forRevoke) {
        super();
        this.accountId = accountId;
        this.domainId = domainId;
        this.networkId = networkId;
        this.sourceIpAddressId = sourceIpAddressId;
        this.destIpAddress = destIpAddress;
        this.sourceMacAddress = null;
        this.forRevoke = forRevoke;
    }

    public StaticNatImpl(long accountId, long domainId, long networkId, long sourceIpAddressId, String destIpAddress, String sourceMacAddress, boolean forRevoke) {
        super();
        this.accountId = accountId;
        this.domainId = domainId;
        this.networkId = networkId;
        this.sourceIpAddressId = sourceIpAddressId;
        this.destIpAddress = destIpAddress;
        this.sourceMacAddress = sourceMacAddress;
        this.forRevoke = forRevoke;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public long getNetworkId() {
        return networkId;
    }

    @Override
    public long getSourceIpAddressId() {
        return sourceIpAddressId;
    }

    @Override
    public String getDestIpAddress() {
        return destIpAddress;
    }

    @Override
    public String getSourceMacAddress() {
        return sourceMacAddress;
    }

    @Override
    public boolean isForRevoke() {
        return forRevoke;
    }

}
