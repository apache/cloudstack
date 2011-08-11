/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.network.rules;

public class StaticNatImpl implements StaticNat{
    long accountId;
    long domainId;
    long networkId;
    long sourceIpAddressId;
    String destIpAddress;
    boolean forRevoke;

    

    public StaticNatImpl(long accountId, long domainId, long networkId, long sourceIpAddressId, String destIpAddress, boolean forRevoke) {
        super();
        this.accountId = accountId;
        this.domainId = domainId;
        this.networkId = networkId;
        this.sourceIpAddressId = sourceIpAddressId;
        this.destIpAddress = destIpAddress;
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
    public boolean isForRevoke() {
        return forRevoke;
    }
    
}
