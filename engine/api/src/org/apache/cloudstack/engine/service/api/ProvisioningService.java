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
package org.apache.cloudstack.engine.service.api;

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.datacenter.entity.api.ClusterEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.HostEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.PodEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.StorageEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.ZoneEntity;

import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.storage.StoragePool;


/**
 * ProvisioningService registers and deregisters physical and virtual 
 * resources that the management server can use.  
 */
public interface ProvisioningService {

    StorageEntity registerStorage(String name, List<String> tags, Map<String, String> details);

    ZoneEntity registerZone(String zoneUuid, String name, String owner, List<String> tags, Map<String, String> details);

    PodEntity registerPod(String podUuid, String name, String owner, String zoneUuid, List<String> tags, Map<String, String> details);

    ClusterEntity registerCluster(String clusterUuid, String name, String owner, List<String> tags, Map<String, String> details);

    HostEntity registerHost(String uuid, String name, String owner, List<String> tags, Map<String, String> details);

    void deregisterStorage(String uuid);

    void deregisterZone(String uuid);

    void deregisterPod(String uuid);

    void deregisterCluster(String uuid);

    void deregisterHost(String uuid);

    void changeState(String type, String entity, Status state);

    List<Host> listHosts();

    List<PodEntity> listPods();

    List<ZoneEntity> listZones();

    List<StoragePool> listStorage();

    ZoneEntity getZone(String id);
}
