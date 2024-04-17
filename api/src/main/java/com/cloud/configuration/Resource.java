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
package com.cloud.configuration;

public interface Resource {

    short RESOURCE_UNLIMITED = -1;
    String UNLIMITED = "Unlimited";

    enum ResourceType { // Primary and Secondary storage are allocated_storage and not the physical storage.
        user_vm("user_vm", 0),
        public_ip("public_ip", 1),
        volume("volume", 2),
        snapshot("snapshot", 3),
        template("template", 4),
        project("project", 5),
        network("network", 6),
        vpc("vpc", 7),
        cpu("cpu", 8),
        memory("memory", 9),
        primary_storage("primary_storage", 10),
        secondary_storage("secondary_storage", 11);

        private String name;
        private int ordinal;
        public static final long bytesToKiB = 1024;
        public static final long bytesToMiB = bytesToKiB * 1024;
        public static final long bytesToGiB = bytesToMiB * 1024;

        ResourceType(String name, int ordinal) {
            this.name = name;
            this.ordinal = ordinal;
        }

        public String getName() {
            return name;
        }

        public int getOrdinal() {
            return ordinal;
        }

        public static ResourceType fromOrdinal(int ordinal) {
            for (ResourceType r : ResourceType.values()) {
                if (r.ordinal == ordinal) {
                    return r;
                }
            }
            return null;
        }
    }

    public static class ResourceOwnerType {

        public static final ResourceOwnerType Account = new ResourceOwnerType("Account");
        public static final ResourceOwnerType Domain = new ResourceOwnerType("Domain");

        private String name;

        public ResourceOwnerType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    ResourceType getType();

    long getOwnerId();

    ResourceOwnerType getResourceOwnerType();
    String getTag();

}
