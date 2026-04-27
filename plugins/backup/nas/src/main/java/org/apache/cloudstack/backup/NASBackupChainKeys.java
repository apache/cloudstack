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

    /** Backup type marker: {@value #TYPE_FULL} or {@value #TYPE_INCREMENTAL}. Mirrors {@code backups.type} for fast lookup without a join. */
    public static final String TYPE = "nas.type";

    public static final String TYPE_FULL = "full";
    public static final String TYPE_INCREMENTAL = "incremental";

    private NASBackupChainKeys() {
    }
}
