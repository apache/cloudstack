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
package com.cloud.hypervisor.xenserver.resource;

import com.cloud.storage.Storage;
import com.xensource.xenapi.Host;

/**
 * Reduce bloat inside CitrixResourceBase
 *
 */
public class CitrixHelper {

    public static String getProductVersion(final Host.Record record) {
        String prodVersion = record.softwareVersion.get("product_version");
        if (prodVersion == null) {
            prodVersion = record.softwareVersion.get("platform_version").trim();
        } else {
            prodVersion = prodVersion.trim();
        }
        return prodVersion;
    }

    public static String getPVbootloaderArgs(String guestOS) {
        if (guestOS.startsWith("SUSE Linux Enterprise Server")) {
            if (guestOS.contains("64-bit")) {
                return "--kernel /boot/vmlinuz-xen --ramdisk /boot/initrd-xen";
            } else if (guestOS.contains("32-bit")) {
                return "--kernel /boot/vmlinuz-xenpae --ramdisk /boot/initrd-xenpae";
            }
        }
        return "";
    }

    public static String getSRNameLabel(final String poolUuid,
                                        final Storage.StoragePoolType poolType,
                                        final String poolPath) {
        if (Storage.StoragePoolType.PreSetup.equals(poolType) &&
                !poolPath.contains(poolUuid)) {
            return  poolPath.replaceFirst("/", "");
        }
        return poolUuid;
    }
}
