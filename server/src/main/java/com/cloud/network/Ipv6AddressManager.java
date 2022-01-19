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

package com.cloud.network;

import com.cloud.dc.DataCenter;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.user.Account;
import com.cloud.utils.component.Manager;
import com.cloud.vm.NicProfile;

public interface Ipv6AddressManager extends Manager {

    public String allocateGuestIpv6(Network network, String requestedIpv6) throws InsufficientAddressCapacityException;

    public String allocatePublicIp6ForGuestNic(Network network, Long podId, Account ipOwner, String requestedIp) throws InsufficientAddressCapacityException;

    public String acquireGuestIpv6Address(Network network, String requestedIpv6) throws InsufficientAddressCapacityException;

    public void setNicIp6Address(final NicProfile nic, final DataCenter dc, final Network network) throws InsufficientAddressCapacityException;

}
