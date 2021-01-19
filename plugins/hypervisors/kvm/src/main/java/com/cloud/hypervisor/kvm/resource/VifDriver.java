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

package com.cloud.hypervisor.kvm.resource;

import java.util.Map;

import javax.naming.ConfigurationException;

import org.libvirt.LibvirtException;

import com.cloud.agent.api.to.NicTO;
import com.cloud.exception.InternalErrorException;

public interface VifDriver {

    public void configure(Map<String, Object> params) throws ConfigurationException;

    public LibvirtVMDef.InterfaceDef plug(NicTO nic, String guestOsType, String nicAdapter, Map<String, String> extraConfig) throws InternalErrorException, LibvirtException;

    public void unplug(LibvirtVMDef.InterfaceDef iface, boolean delete);

    void attach(LibvirtVMDef.InterfaceDef iface);

    void detach(LibvirtVMDef.InterfaceDef iface);

    void createControlNetwork(String privBrName);

    boolean isExistingBridge(String bridgeName);

    void deleteBr(NicTO nic);

}
