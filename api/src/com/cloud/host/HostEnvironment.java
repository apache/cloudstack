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
package com.cloud.host;

/**
 *  the networking environment of the host.  The
 *  the environment.
 */
public class HostEnvironment {

    public String managementIpAddress;
    public String managementNetmask;
    public String managementGateway;
    public String managementVlan;

    public String[] neighborHosts;

    public String storageIpAddress;
    public String storageNetwork;
    public String storageGateway;
    public String storageVlan;
    public String secondaryStroageIpAddress;

    public String storage2IpAddress;
    public String storage2Network;
    public String storage2Gateway;
    public String storage2Vlan;
    public String secondaryStorageIpAddress2;

    public String[] neighborStorages;
    public String[] neighborStorages2;

    public String publicIpAddress;
    public String publicNetmask;
    public String publicGateway;
    public String publicVlan;
}
