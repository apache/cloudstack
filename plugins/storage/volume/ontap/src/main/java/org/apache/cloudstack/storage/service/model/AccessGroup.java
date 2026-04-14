/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.storage.service.model;

import com.cloud.host.HostVO;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.storage.feign.model.ExportPolicy;
import org.apache.cloudstack.storage.feign.model.Igroup;

import java.util.List;

public class AccessGroup {

    private Igroup igroup;
    private ExportPolicy exportPolicy;

    private List<HostVO> hostsToConnect;
    private PrimaryDataStoreInfo primaryDataStoreInfo;
    private Scope scope;


    public Igroup getIgroup() {
        return igroup;
    }

    public void setIgroup(Igroup igroup) {
        this.igroup = igroup;
    }

    public ExportPolicy getPolicy() {
        return exportPolicy;
    }

    public void setPolicy(ExportPolicy policy) {
        this.exportPolicy = policy;
    }
    public List<HostVO> getHostsToConnect() {
        return hostsToConnect;
    }
    public void setHostsToConnect(List<HostVO> hostsToConnect) {
        this.hostsToConnect = hostsToConnect;
    }
    public PrimaryDataStoreInfo getPrimaryDataStoreInfo() {
        return primaryDataStoreInfo;
    }
    public void setPrimaryDataStoreInfo(PrimaryDataStoreInfo primaryDataStoreInfo) {
        this.primaryDataStoreInfo = primaryDataStoreInfo;
    }
    public Scope getScope() {
        return scope;
    }
    public void setScope(Scope scope) {
        this.scope = scope;
    }
}
