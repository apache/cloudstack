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
package org.apache.cloudstack.engine.subsystem.api.storage.disktype;

import com.cloud.utils.exception.CloudRuntimeException;

public enum DiskFormat {
    VMDK, VHD, VHDX, ISO, QCOW2;
    public static DiskFormat getFormat(String format) {
        if (VMDK.toString().equalsIgnoreCase(format)) {
            return VMDK;
        } else if (VHD.toString().equalsIgnoreCase(format)) {
            return VHD;
        } else if (VHDX.toString().equalsIgnoreCase(format)) {
            return VHDX;
        } else if (QCOW2.toString().equalsIgnoreCase(format)) {
            return QCOW2;
        } else if (ISO.toString().equalsIgnoreCase(format)) {
            return ISO;
        }
        throw new CloudRuntimeException("can't find format match: " + format);
    }
}
