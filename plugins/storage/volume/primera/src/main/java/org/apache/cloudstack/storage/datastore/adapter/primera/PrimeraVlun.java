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
package org.apache.cloudstack.storage.datastore.adapter.primera;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrimeraVlun {
    private int lun;
    private String volumeName;
    private String hostname;
    private String remoteName;
    private int type;
    private String serial;
    private PrimeraPortPosition portPos;
    private String volumeWWN;
    private int multipathing;
    private int failedPathPol;
    private int failedPathInterval;
    private String hostDeviceName;
    @JsonProperty("Subsystem_NQN")
    private String subsystemNQN;
    private boolean active;

    public static class PrimeraPortPosition {
        private int node;
        private int slot;
        private int cardPort;
        public int getNode() {
            return node;
        }
        public void setNode(int node) {
            this.node = node;
        }
        public int getSlot() {
            return slot;
        }
        public void setSlot(int slot) {
            this.slot = slot;
        }
        public int getCardPort() {
            return cardPort;
        }
        public void setCardPort(int cardPort) {
            this.cardPort = cardPort;
        }

    }

    public int getLun() {
        return lun;
    }

    public void setLun(int lun) {
        this.lun = lun;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public void setRemoteName(String remoteName) {
        this.remoteName = remoteName;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public PrimeraPortPosition getPortPos() {
        return portPos;
    }

    public void setPortPos(PrimeraPortPosition portPos) {
        this.portPos = portPos;
    }

    public String getVolumeWWN() {
        return volumeWWN;
    }

    public void setVolumeWWN(String volumeWWN) {
        this.volumeWWN = volumeWWN;
    }

    public int getMultipathing() {
        return multipathing;
    }

    public void setMultipathing(int multipathing) {
        this.multipathing = multipathing;
    }

    public int getFailedPathPol() {
        return failedPathPol;
    }

    public void setFailedPathPol(int failedPathPol) {
        this.failedPathPol = failedPathPol;
    }

    public int getFailedPathInterval() {
        return failedPathInterval;
    }

    public void setFailedPathInterval(int failedPathInterval) {
        this.failedPathInterval = failedPathInterval;
    }

    public String getHostDeviceName() {
        return hostDeviceName;
    }

    public void setHostDeviceName(String hostDeviceName) {
        this.hostDeviceName = hostDeviceName;
    }

    public String getSubsystemNQN() {
        return subsystemNQN;
    }

    public void setSubsystemNQN(String subsystemNQN) {
        this.subsystemNQN = subsystemNQN;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }


}
