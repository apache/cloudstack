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

import com.cloud.utils.Pair;
import org.apache.cloudstack.api.command.admin.ipv6.CreateIpv6RangeCmd;
import org.apache.cloudstack.api.command.admin.ipv6.DedicateIpv6RangeCmd;
import org.apache.cloudstack.api.command.admin.ipv6.DeleteIpv6RangeCmd;
import org.apache.cloudstack.api.command.admin.ipv6.ListIpv6RangesCmd;
import org.apache.cloudstack.api.command.admin.ipv6.ReleaseIpv6RangeCmd;
import org.apache.cloudstack.api.command.admin.ipv6.UpdateIpv6RangeCmd;
import org.apache.cloudstack.api.response.Ipv6RangeResponse;

import java.util.List;

public interface Ipv6Service {
    Ipv6Address createIpv6Range(CreateIpv6RangeCmd cmd);

    Ipv6Address updateIpv6Range(UpdateIpv6RangeCmd cmd);

    boolean deleteIpv6Range(DeleteIpv6RangeCmd cmd);

    Ipv6Address dedicateIpv6Range(DedicateIpv6RangeCmd cmd);

    boolean releaseIpv6Range(ReleaseIpv6RangeCmd cmd);

    Pair<List<? extends Ipv6Address>, Integer> searchForIpv6Range(ListIpv6RangesCmd cmd);

    Ipv6RangeResponse createIpv6RangeResponse(Ipv6Address address);

}
