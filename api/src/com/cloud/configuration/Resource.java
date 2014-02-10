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

    public static final short RESOURCE_UNLIMITED = -1;

    public enum ResourceType { // Primary and Secondary storage are allocated_storage and not the physical storage.
        user_vm("user_vm", 0, ResourceOwnerType.Account, ResourceOwnerType.Domain),
        public_ip("public_ip", 1, ResourceOwnerType.Account, ResourceOwnerType.Domain),
        volume("volume", 2, ResourceOwnerType.Account, ResourceOwnerType.Domain),
        snapshot("snapshot", 3, ResourceOwnerType.Account, ResourceOwnerType.Domain),
        template("template", 4, ResourceOwnerType.Account, ResourceOwnerType.Domain),
        project("project", 5, ResourceOwnerType.Account, ResourceOwnerType.Domain),
        network("network", 6, ResourceOwnerType.Account, ResourceOwnerType.Domain),
        vpc("vpc", 7, ResourceOwnerType.Account, ResourceOwnerType.Domain),
        cpu("cpu", 8, ResourceOwnerType.Account, ResourceOwnerType.Domain),
        memory("memory", 9, ResourceOwnerType.Account, ResourceOwnerType.Domain),
        primary_storage("primary_storage", 10, ResourceOwnerType.Account, ResourceOwnerType.Domain),
        secondary_storage("secondary_storage", 11, ResourceOwnerType.Account, ResourceOwnerType.Domain);

        private String name;
        private ResourceOwnerType[] supportedOwners;
        private int ordinal;
        public static final long bytesToGiB = 1024 * 1024 * 1024;

        ResourceType(String name, int ordinal, ResourceOwnerType... supportedOwners) {
            this.name = name;
            this.supportedOwners = supportedOwners;
            this.ordinal = ordinal;
        }

        public String getName() {
            return name;
        }

        public ResourceOwnerType[] getSupportedOwners() {
            return supportedOwners;
        }

        public boolean supportsOwner(ResourceOwnerType ownerType) {
            boolean success = false;
            if (supportedOwners != null) {
                int length = supportedOwners.length;
                for (int i = 0; i < length; i++) {
                    if (supportedOwners[i].getName().equalsIgnoreCase(ownerType.getName())) {
                        success = true;
                        break;
                    }
                }
            }

            return success;
        }

        public int getOrdinal() {
            return ordinal;
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

}
