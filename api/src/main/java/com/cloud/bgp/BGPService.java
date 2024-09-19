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
package com.cloud.bgp;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.vpc.Vpc;
import com.cloud.utils.Pair;
import org.apache.cloudstack.api.command.user.bgp.ListASNumbersCmd;

import java.util.List;

public interface BGPService {

    ASNumberRange createASNumberRange(long zoneId, long startASNumber, long endASNumber);
    List<ASNumberRange> listASNumberRanges(Long zoneId);
    Pair<List<ASNumber>, Integer> listASNumbers(ListASNumbersCmd cmd);
    boolean allocateASNumber(long zoneId, Long asNumber, Long networkId, Long vpcId);
    Pair<Boolean, String> releaseASNumber(long zoneId, long asNumber, boolean isReleaseNetworkDestroy);
    boolean deleteASRange(long id);

    boolean applyBgpPeers(Network network, boolean continueOnError) throws ResourceUnavailableException;

    boolean applyBgpPeers(Vpc vpc, boolean continueOnError) throws ResourceUnavailableException;
}
