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
package com.cloud.agent.api.to;

/**
 * TO class sent to the hypervisor layer with the information needed to handle
 * VM Migrations from Vmware to KVM templates
 */
public class VmwareVmForMigrationTO {

    private String vcenter;
    private String datacenter;
    private String cluster;
    private String username;
    private String password;
    private String url;
    private String host;
    private String vmName;
    private String secondaryStorageUrl;

    public VmwareVmForMigrationTO() {
    }

    public VmwareVmForMigrationTO(String vcenter, String datacenter, String cluster, String username, String password,
                                  String url, String hostname, String vmName, String secondaryStorageUrl) {
        this.vcenter = vcenter;
        this.datacenter = datacenter;
        this.cluster = cluster;
        this.username = username;
        this.password = password;
        this.url = url;
        this.host = hostname;
        this.vmName = vmName;
        this.secondaryStorageUrl = secondaryStorageUrl;
    }

    public String getVcenter() {
        return vcenter;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getUrl() {
        return url;
    }

    public String getHost() {
        return host;
    }

    public String getVmName() {
        return vmName;
    }

    public String getSecondaryStorageUrl() {
        return secondaryStorageUrl;
    }

    public String getCluster() {
        return cluster;
    }
}
