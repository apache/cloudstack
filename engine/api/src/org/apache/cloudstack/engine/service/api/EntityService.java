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

import javax.ws.rs.Path;

import com.cloud.network.Network;
import com.cloud.storage.Volume;
import com.cloud.vm.VirtualMachine;

/**
 * Service to retrieve CloudStack entities
 * very likely to change
 */
@Path("resources")
public interface EntityService {
    List<String> listVirtualMachines();

    List<String> listVolumes();

    List<String> listNetworks();

    List<String> listNics();

    List<String> listSnapshots();

    List<String> listTemplates();

    List<String> listStoragePools();

    List<String> listHosts();

    VirtualMachine getVirtualMachine(String vm);

    Volume getVolume(String volume);

    Network getNetwork(String network);

}
