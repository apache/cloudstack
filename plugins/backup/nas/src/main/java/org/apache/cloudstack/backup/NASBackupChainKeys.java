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
package org.apache.cloudstack.backup;

/**
 * Keys used by the NAS backup provider when storing incremental-chain metadata
 * in the existing {@code backup_details} key/value table. Stored here (not on
 * the {@code backups} table) so other providers do not need a schema change to
 * support their own incremental implementations.
 */
public final class NASBackupChainKeys {

    /** UUID of the parent backup (full or previous incremental). Empty for full backups. */
    public static final String PARENT_BACKUP_ID = "nas.parent_backup_id";

    /** QEMU dirty-bitmap name created by this backup, used as the {@code <incremental>} reference for the next one. */
    public static final String BITMAP_NAME = "nas.bitmap_name";

    /** Identifier shared by every backup in the same chain (the full anchors a chain; its incrementals inherit the id). */
    public static final String CHAIN_ID = "nas.chain_id";

    /** Position within the chain: 0 for the full, 1 for the first incremental, and so on. */
    public static final String CHAIN_POSITION = "nas.chain_position";

    /**
     * In-memory chain-mode sentinels used by {@code ChainDecision.mode}. The persisted
     * full-vs-incremental backup type lives on the {@code backup.type} column (set in
     * {@code takeBackup}) — single source of truth. Not duplicated into backup_details.
     */
    public static final String TYPE_FULL = "full";
    public static final String TYPE_INCREMENTAL = "incremental";

    /**
     * Tombstone key stored in {@code backup_details} when a backup is requested for deletion
     * but still has live children. The on-NAS file is preserved until the last child is gone,
     * at which point cascade deletion collects every tombstoned ancestor. Mirrors the snapshot
     * subsystem's {@code Hidden} state semantics (see {@code DefaultSnapshotStrategy}).
     */
    public static final String DELETE_PENDING = "nas.delete_pending";

    /**
     * VM-scoped detail (stored in {@code vm_instance_details}) holding the QEMU dirty-bitmap
     * name that currently exists on the running VM and is therefore the only valid parent
     * for the next incremental backup. Written by {@link #BITMAP_NAME} on each successful
     * backup; cleared on restore (the restored disk image has no bitmap, so the next backup
     * must be a fresh full). When the VM has no detail, {@code decideChain} forces full.
     */
    public static final String VM_ACTIVE_CHECKPOINT_ID = "nas.active_checkpoint_id";

    private NASBackupChainKeys() {
    }
}
