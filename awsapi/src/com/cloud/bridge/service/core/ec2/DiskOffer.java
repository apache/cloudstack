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
package com.cloud.bridge.service.core.ec2;

import java.util.Calendar;

import com.cloud.bridge.util.EC2RestAuth;

public class DiskOffer {

    private String id;
    private String name;
    private int diskSize;       // <- in gigs
    private Calendar created;
    private boolean isCustomized;   // <- true if disk offering uses custom size

    public DiskOffer() {
        id = null;
        name = null;
        diskSize = 0;
        created = null;
        isCustomized = false;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setSize(String diskSize) {
        if (null != diskSize) {
            // -> convert from number of bytes into the number of gigabytes
            long bytes = Long.parseLong(diskSize);
            if (0 != bytes)
                this.diskSize = (int)(bytes / 1073741824);
        } else
            this.diskSize = 0;
    }

    public int getSize() {
        return this.diskSize;
    }

    public void setCreated(String created) {
        this.created = EC2RestAuth.parseDateString(created);
    }

    public Calendar getCreated() {
        return this.created;
    }

    public boolean getIsCustomized() {
        return this.isCustomized;
    }

    public void setIsCustomized(boolean isCustomized) {
        this.isCustomized = isCustomized;
    }
}
