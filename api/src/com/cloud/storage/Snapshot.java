/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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

package com.cloud.storage;

import java.util.Date;

import com.cloud.acl.ControlledEntity;
import com.cloud.hypervisor.Hypervisor.HypervisorType;

public interface Snapshot extends ControlledEntity {
    public enum Type {
        MANUAL,
        RECURRING,
        TEMPLATE,
        HOURLY,
        DAILY,
        WEEKLY,
        MONTHLY;
        private int max = 8;

        public void setMax(int max) {
            this.max = max;
        }

        public int getMax() {
            return max;
        }

        public String toString() {
            return this.name();
        }

        public boolean equals(String snapshotType) {
            return this.toString().equalsIgnoreCase(snapshotType);
        }
    }

    public enum Status {
        Creating,
        CreatedOnPrimary,
        BackingUp,
        BackedUp,
        Error;

        public String toString() {
            return this.name();
        }

        public boolean equals(String status) {
            return this.toString().equalsIgnoreCase(status);
        }
    }

    public static final long MANUAL_POLICY_ID = 0L;

    Long getId();

    long getAccountId();

    long getVolumeId();

    String getPath();

    String getName();

    Date getCreated();

    Type getType();

    Status getStatus();

    HypervisorType getHypervisorType();

    boolean isRecursive();

    short getsnapshotType();

}
