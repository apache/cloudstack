//
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
//
package com.cloud.agent.api.to;

import com.cloud.agent.api.LogLevel;
import com.cloud.agent.api.to.deployasis.OVFEulaSectionTO;
import com.cloud.agent.api.to.deployasis.OVFNetworkTO;
import com.cloud.agent.api.to.deployasis.OVFPropertyTO;
import com.cloud.agent.api.to.deployasis.OVFVirtualHardwareSectionTO;
import com.cloud.utils.Pair;

import java.util.List;

/**
 * Placeholder class for all the subclasses obtained from the OVF parsing
 */
public class OVFInformationTO {

    @LogLevel(LogLevel.Log4jLevel.Off)
    private List<OVFPropertyTO> properties;
    @LogLevel(LogLevel.Log4jLevel.Off)
    private List<OVFNetworkTO> networks;
    @LogLevel(LogLevel.Log4jLevel.Off)
    private List<DatadiskTO> disks;
    @LogLevel(LogLevel.Log4jLevel.Off)
    private OVFVirtualHardwareSectionTO hardwareSection;
    @LogLevel(LogLevel.Log4jLevel.Off)
    private List<OVFEulaSectionTO> eulaSections;
    @LogLevel(LogLevel.Log4jLevel.Off)
    private Pair<String, String> guestOsInfo;

    public OVFInformationTO() {
    }

    public List<OVFPropertyTO> getProperties() {
        return properties;
    }

    public void setProperties(List<OVFPropertyTO> properties) {
        this.properties = properties;
    }

    public List<OVFNetworkTO> getNetworks() {
        return networks;
    }

    public void setNetworks(List<OVFNetworkTO> networks) {
        this.networks = networks;
    }

    public List<DatadiskTO> getDisks() {
        return disks;
    }

    public void setDisks(List<DatadiskTO> disks) {
        this.disks = disks;
    }

    public OVFVirtualHardwareSectionTO getHardwareSection() {
        return hardwareSection;
    }

    public void setHardwareSection(OVFVirtualHardwareSectionTO hardwareSection) {
        this.hardwareSection = hardwareSection;
    }

    public List<OVFEulaSectionTO> getEulaSections() {
        return eulaSections;
    }

    public void setEulaSections(List<OVFEulaSectionTO> eulaSections) {
        this.eulaSections = eulaSections;
    }

    public Pair<String, String> getGuestOsInfo() {
        return guestOsInfo;
    }

    public void setGuestOsInfo(Pair<String, String> guestOsInfo) {
        this.guestOsInfo = guestOsInfo;
    }
}
