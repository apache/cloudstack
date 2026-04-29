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

package org.apache.cloudstack.storage.formatinspector;

public enum Qcow2HeaderField {
    MAGIC(0, 4),
    VERSION(4, 4),
    BACKING_FILE_OFFSET(8, 8),
    BACKING_FILE_NAME_LENGTH(16, 4),
    CLUSTER_BITS(20, 4),
    SIZE(24, 8),
    CRYPT_METHOD(32, 4),
    L1_SIZE(36, 4),
    LI_TABLE_OFFSET(40, 8),
    REFCOUNT_TABLE_OFFSET(48, 8),
    REFCOUNT_TABLE_CLUSTERS(56, 4),
    NB_SNAPSHOTS(60, 4),
    SNAPSHOTS_OFFSET(64, 8),
    INCOMPATIBLE_FEATURES(72, 8);

    private final int offset;
    private final int length;

    Qcow2HeaderField(int offset, int length) {
        this.offset = offset;
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    public int getOffset() {
        return offset;
    }
}
