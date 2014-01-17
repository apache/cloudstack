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
package com.cloud.storage;

import com.cloud.utils.exception.CloudRuntimeException;

public enum DataStoreRole {
    Primary("primary"), Image("image"), ImageCache("imagecache"), Backup("backup");

    public boolean isImageStore() {
        return (role.equalsIgnoreCase("image") || role.equalsIgnoreCase("imagecache")) ? true : false;
    }

    private final String role;

    DataStoreRole(String type) {
        role = type;
    }

    public static DataStoreRole getRole(String role) {
        if (role == null) {
            throw new CloudRuntimeException("role can't be empty");
        }
        if (role.equalsIgnoreCase("primary")) {
            return Primary;
        } else if (role.equalsIgnoreCase("image")) {
            return Image;
        } else if (role.equalsIgnoreCase("imagecache")) {
            return ImageCache;
        } else if (role.equalsIgnoreCase("backup")) {
            return Backup;
        } else {
            throw new CloudRuntimeException("can't identify the role");
        }
    }
}
