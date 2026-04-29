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
package com.cloud.network.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "op_networks")
public class NetworkOpVO implements InternalIdentity {

    @Id
    @Column(name = "id")
    long id;

    @Column(name = "nics_count")
    int activeNicsCount;

    @Column(name = "gc")
    boolean garbageCollected;

    @Column(name = "check_for_gc")
    boolean checkForGc;

    protected NetworkOpVO() {
    }

    public NetworkOpVO(long id, boolean gc) {
        this.id = id;
        this.garbageCollected = gc;
        this.checkForGc = gc;
        this.activeNicsCount = 0;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getActiveNicsCount() {
        return activeNicsCount;
    }

    public void setActiveNicsCount(int number) {
        activeNicsCount += number;
    }

    public boolean isGarbageCollected() {
        return garbageCollected;
    }

    public boolean isCheckForGc() {
        return checkForGc;
    }

    public void setCheckForGc(boolean check) {
        checkForGc = check;
    }
}
