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
package org.apache.cloudstack.engine.subsystem.api.storage;

/**
 * enumerates different capabilities storage drivers may have
 */
public enum DataStoreCapabilities {
    VOLUME_SNAPSHOT_QUIESCEVM,
    /**
     * indicates that this driver takes CloudStack volume snapshots on its own system (as either back-end snapshots or back-end clones)
     */
    STORAGE_SYSTEM_SNAPSHOT,
    /**
     * indicates that this driver supports the "cloneOfSnapshot" property of cloud.snapshot_details (for creating a back-end volume
     *     from a back-end snapshot or a back-end clone) and that it supports the invocation of the createAsync method where a SnapshotInfo is passed in while using
     *     the "tempVolume" property of snapshot_details
     */
    CAN_CREATE_VOLUME_FROM_SNAPSHOT,
    /**
     * indicates that this driver supports the "cloneOfSnapshot" property of cloud.snapshot_details (for creating a volume from a volume)
     */
    CAN_CREATE_VOLUME_FROM_VOLUME,
    /**
     * indicates that this driver supports reverting a volume to a snapshot state
     */
    CAN_REVERT_VOLUME_TO_SNAPSHOT,
    /**
    * indicates that the driver supports copying snapshot between zones on pools of the same type
    */
    CAN_COPY_SNAPSHOT_BETWEEN_ZONES_AND_SAME_POOL_TYPE,
    /**
     * indicates that this driver supports the option to create a template from the back-end snapshot
     */
    CAN_CREATE_TEMPLATE_FROM_SNAPSHOT
}
