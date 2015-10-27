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
package com.cloud.vm;

/**
 * VM Name Service generates and checks VM names for use on hypervisors (separate
 * from display name and hostname of the VM).
 */
public interface VirtualMachineNameService {

    public boolean isValidCloudStackVmName(String name, String instance);
    public String getVnetName(long vnetId);

    public boolean isValidVmName(String vmName);

    public boolean isValidVmName(String vmName, String instance);

    public String getVmName(long vmId, long userId, String instance);

    public long getVmId(String vmName);

    public long getRouterId(String routerName);

    public long getConsoleProxyId(String vmName);

    public long getSystemVmId(String vmName);

    public String getRouterName(long routerId, String instance);

    public String getConsoleProxyName(long vmId, String instance);

    public String getSystemVmName(long vmId, String instance, String prefix);

    public String attachVnet(String name, String vnet);

    public boolean isValidRouterName(String name);

    public boolean isValidRouterName(String name, String instance);

    public boolean isValidConsoleProxyName(String name);

    public boolean isValidConsoleProxyName(String name, String instance);

    public boolean isValidSecStorageVmName(String name, String instance);

    public boolean isValidSystemVmName(String name, String instance, String prefix);
}
