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
package com.cloud.agent.dhcp;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import com.cloud.utils.Pair;
import com.cloud.utils.component.Adapter;

public interface DhcpSnooper extends Adapter {

    public InetAddress getIPAddr(String macAddr, String vmName);

    public InetAddress getDhcpServerIP();

    public void cleanup(String macAddr, String vmName);

    public Map<String, InetAddress> syncIpAddr();

    @Override
    public boolean stop();

    public void initializeMacTable(List<Pair<String, String>> macVmNameList);

}
