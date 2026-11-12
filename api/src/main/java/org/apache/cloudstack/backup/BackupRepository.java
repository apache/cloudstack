//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.backup;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

public interface BackupRepository extends InternalIdentity, Identity {
    String getProvider();
    long getZoneId();
    String getName();
    String getType();
    String getAddress();
    String getMountOptions();
    Long getCapacityBytes();
    Long getUsedBytes();
    Date getCreated();

    /**
     * Trims every entry of a comma separated mount option list and drops the empty ones.
     * Older releases accepted and stored values such as "vers=4.1 ", whose stray whitespace the
     * shell used to split away. Now that the options reach mount as a single argument, mount
     * rejects that whitespace, so the stored value is cleaned before it is used.
     */
    static String normalizeMountOptions(String mountOptions) {
        if (mountOptions == null) {
            return null;
        }
        return Arrays.stream(mountOptions.split(","))
                .map(String::trim)
                .filter(option -> !option.isEmpty())
                .collect(Collectors.joining(","));
    }
}
