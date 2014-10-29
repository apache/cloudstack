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
package org.apache.cloudstack.engine.cloud.entity.api;

import org.apache.cloudstack.engine.datacenter.entity.api.StorageEntity;
import org.apache.cloudstack.engine.entity.api.CloudStackEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.DiskFormat;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;

public interface VolumeEntity extends CloudStackEntity {

    /**
     * Take a snapshot of the volume
     */
    SnapshotEntity takeSnapshotOf(boolean full);

    /**
     * Make a reservation to do storage migration
     *
     * @param expirationTime time in seconds the reservation is cancelled
     * @return reservation token
     */
    String reserveForMigration(long expirationTime);

    /**
     * Migrate using a reservation.
     * @param reservationToken reservation token
     */
    void migrate(String reservationToken);

    /**
     * Setup for a copy of this volume.
     * @return destination to copy to
     */
    VolumeEntity setupForCopy();

    /**
     * Perform the copy
     * @param dest copy to this volume
     */
    void copy(VolumeEntity dest);

    /**
     * Attach to the vm
     * @param vm vm to attach to
     * @param deviceId device id to use
     */
    void attachTo(String vm, long deviceId);

    /**
     * Detach from the vm
     */
    void detachFrom();

    /**
     * Destroy the volume
     */
    void destroy();

    long getSize();

    DiskFormat getDiskType();

    VolumeType getType();

    StorageEntity getDataStore();
}
