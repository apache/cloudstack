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

    /**
     * Name of the point-in-time anchor this backup created, used as the parent reference for the
     * next backup in the chain. Its meaning depends on the chain format: for {@link #FORMAT_QCOW2}
     * chains it is the QEMU dirty-bitmap name used as the {@code <incremental>} reference; for
     * {@link #FORMAT_RBD_DIFF} chains it is the RBD snapshot name used as {@code --from-snap}.
     */
    public static final String BITMAP_NAME = "nas.bitmap_name";

    /**
     * On-NAS format of the chain this backup belongs to: {@link #FORMAT_QCOW2} (rebased qcow2
     * files, the file-based storage path) or {@link #FORMAT_RBD_DIFF} ({@code rbd export-diff}
     * streams, the Ceph/RBD path). Absent on legacy-full backups, which are always qcow2.
     */
    public static final String CHAIN_FORMAT = "nas.chain_format";

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
     * Ceph/RBD equivalents of {@link #TYPE_FULL} / {@link #TYPE_INCREMENTAL}. RBD images are raw
     * and cannot carry a QEMU dirty bitmap, so these anchor each cycle on an RBD snapshot and
     * export the delta with {@code rbd export-diff} instead of a libvirt checkpoint + push backup.
     */
    public static final String TYPE_RBD_FULL = "rbd-full";
    public static final String TYPE_RBD_INCREMENTAL = "rbd-incremental";
    // Feature disabled: behave exactly like the pre-incremental full-only backup — no QEMU
    // bitmap/checkpoint is created and no chain metadata is persisted. Matches nasbackup.sh's
    // "legacy-full" mode token (which sets make_checkpoint=0).
    public static final String TYPE_LEGACY_FULL = "legacy-full";

    /**
     * VM-scoped detail (stored in {@code vm_instance_details}) holding the QEMU dirty-bitmap
     * name that currently exists on the running VM and is therefore the only valid parent
     * for the next incremental backup. Written by {@link #BITMAP_NAME} on each successful
     * backup; cleared on restore (the restored disk image has no bitmap, so the next backup
     * must be a fresh full). When the VM has no detail, {@code decideChain} forces full.
     */
    public static final String VM_ACTIVE_CHECKPOINT_ID = "nas.active_checkpoint_id";

    /** Chain format values persisted under {@link #CHAIN_FORMAT}; also the on-NAS file extension. */
    public static final String FORMAT_QCOW2 = "qcow2";
    public static final String FORMAT_RBD_DIFF = "rbddiff";

    private NASBackupChainKeys() {
    }
}
