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
package org.apache.cloudstack.storage;

import java.util.List;

public interface StorageOrchestrator {

    /**
     * Prepares all storage ready for a VM to start
     * @param vm
     * @param reservationId
     */
    void prepare(String vm, String reservationId);

    /**
     * Releases all storage that were used for a VM shutdown
     * @param vm
     * @param disks
     * @param reservationId
     */
    void release(String vm, String reservationId);

    /**
     * Destroy all disks
     * @param disks
     * @param reservationId
     */
    void destroy(List<String> disks, String reservationId);

    /**
     * Cancel a reservation
     * @param reservationId reservation to 
     */
    void cancel(String reservationId);
}
