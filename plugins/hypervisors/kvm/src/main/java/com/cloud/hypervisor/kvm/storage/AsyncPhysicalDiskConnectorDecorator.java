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
package com.cloud.hypervisor.kvm.storage;

import java.util.Map;

/**
 * Decorator for StorageAdapters that implement asynchronous physical disk connections to improve
 * performance on VM starts with large numbers of disks.
 */
public interface AsyncPhysicalDiskConnectorDecorator {
    /**
     * Initiates a connection attempt (may or may not complete it depending on implementation)
     * @param path
     * @param pool
     * @param details
     * @return
     */
    public boolean startConnectPhysicalDisk(String path, KVMStoragePool pool, Map<String,String> details);

    /**
     * Tests if the physical disk is connected
     * @param path
     * @param pool
     * @param details
     * @return
     */
    public boolean isConnected(String path, KVMStoragePool pool, Map<String,String> details);

    /**
     * Completes a connection attempt after isConnected returns true;
     * @param path
     * @param pool
     * @param details
     * @return
     * @throws Exception
     */
    public boolean finishConnectPhysicalDisk(String path, KVMStoragePool pool, Map<String,String> details) throws Exception;
}
