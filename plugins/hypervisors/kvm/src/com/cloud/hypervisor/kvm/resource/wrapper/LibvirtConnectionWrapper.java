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
package com.cloud.hypervisor.kvm.resource.wrapper;

import org.libvirt.Connect;
import org.libvirt.LibvirtException;

import com.cloud.hypervisor.kvm.resource.LibvirtConnection;

/**
 * This class is used to wrap the calls to LibvirtConnection and ease the burden of the unit tests.
 * Please do not instantiate this class directly, but inject it using the {@code @Inject} annotation.
 */
public class LibvirtConnectionWrapper {

    public Connect getConnectionByName(final String vmName) throws LibvirtException {
        return LibvirtConnection.getConnectionByVmName(vmName);
    }

    public Connect getConnection() throws LibvirtException {
        return LibvirtConnection.getConnection();
    }
}