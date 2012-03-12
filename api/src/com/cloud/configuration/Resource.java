/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.configuration;

public interface Resource {

    public static final short RESOURCE_UNLIMITED = -1;

    public enum ResourceType {
        user_vm("user_vm", 0, ResourceOwnerType.Account, ResourceOwnerType.Domain),
        public_ip("public_ip", 1, ResourceOwnerType.Account, ResourceOwnerType.Domain),
        volume("volume", 2, ResourceOwnerType.Account, ResourceOwnerType.Domain),
        snapshot("snapshot", 3, ResourceOwnerType.Account, ResourceOwnerType.Domain),
        template("template", 4, ResourceOwnerType.Account, ResourceOwnerType.Domain),
        project("project", 5, ResourceOwnerType.Account, ResourceOwnerType.Domain),
        network("network", 6, ResourceOwnerType.Account, ResourceOwnerType.Domain);

        private String name;
        private ResourceOwnerType[] supportedOwners;
        private int ordinal;

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
