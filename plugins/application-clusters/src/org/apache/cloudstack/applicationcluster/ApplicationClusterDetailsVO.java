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
package org.apache.cloudstack.applicationcluster;


import javax.persistence.Column;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

@Entity
@Table(name = "sb_ccs_container_cluster_details")
public class ApplicationClusterDetailsVO implements ApplicationClusterDetails {

    public long getId() {
        return id;
    }

    public long getClusterId() {
        return clusterId;
    }

    public String getUserName() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setClusterId(long clusterId) {
        this.clusterId = clusterId;
    }

    public String getRegistryUsername() {
        return registryUsername;
    }

    public void setRegistryUsername(String registryUsername) {
        this.registryUsername = registryUsername;
    }

    public String getRegistryPassword() {
        return registryPassword;
    }

    public void setRegistryPassword(String registryPassword) {
        this.registryPassword = registryPassword;
    }

    public String getRegistryUrl() {
        return registryUrl;
    }

    public void setRegistryUrl(String registryUrl) {
        this.registryUrl = registryUrl;
    }

    public String getRegistryEmail() {
        return registryEmail;
    }

    public void setRegistryEmail(String registryEmail) {
        this.registryEmail = registryEmail;
    }

    public boolean getNetworkCleanup() {
        return networkCleanup;
    }

    public void setNetworkCleanup(boolean networkCleanup) {
        this.networkCleanup = networkCleanup;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "cluster_id")
    long clusterId;

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Column(name = "username")
    String username;

    @Column(name = "password")
    String password;

    @Column(name = "registry_username")
    String registryUsername;

    @Column(name = "registry_password")
    String registryPassword;

    @Column(name = "registry_url")
    String registryUrl;

    @Column(name = "registry_email")
    String registryEmail;

    @Column(name = "network_cleanup")
    boolean networkCleanup;

    public ApplicationClusterDetailsVO() {

    }

    public ApplicationClusterDetailsVO(long clusterId, String userName, String password) {
        this.clusterId = clusterId;
        this.username = userName;
        this.password = password;
    }
}
