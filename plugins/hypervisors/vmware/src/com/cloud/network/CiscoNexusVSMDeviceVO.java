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

package com.cloud.network;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.Identity;
import com.cloud.utils.db.Encrypt;
import org.apache.cloudstack.api.InternalIdentity;

/**
 * CiscoNexusVSMDeviceVO contains information on external Cisco Nexus 1000v VSM devices added into a deployment.
 * This should be probably made as a more generic class so that we can handle multiple versions of Nexus VSMs
 * in future.
 */

@Entity
@Table(name="virtual_supervisor_module")
public class CiscoNexusVSMDeviceVO implements CiscoNexusVSMDevice {

    // We need to know what properties a VSM has. Put them here.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name="uuid")
    private final String uuid;

    @Column(name = "host_id")
    private long hostId;

    @Column(name = "vsm_name")
    private String vsmName;

    @Column(name = "username")
    private String vsmUserName;

    @Encrypt
    @Column(name = "password")
    private String vsmPassword;

    @Column(name = "ipaddr")
    private String ipaddr;

    @Column(name = "management_vlan")
    private int managementVlan;

    @Column(name = "control_vlan")
    private int controlVlan;

    @Column(name = "packet_vlan")
    private int packetVlan;

    @Column(name = "storage_vlan")
    private int storageVlan;

    @Column(name = "vsm_domain_id")
    private long vsmDomainId;

    @Column(name = "config_mode")
    private VSMConfigMode vsmConfigMode;

    @Column(name = "config_state")
    private VSMConfigState vsmConfigState;

    @Column(name = "vsm_device_state")
    private VSMDeviceState vsmDeviceState;

    // Accessor methods
    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getvsmName() {
        return vsmName;
    }

    @Override
    public long getHostId() {
        return hostId;
    }

    @Override
    public String getUserName() {
        return vsmUserName;
    }

    @Override
    public String getPassword() {
        return vsmPassword;
    }

    @Override
    public String getipaddr() {
        return ipaddr;
    }

    @Override
    public int getManagementVlan() {
        return managementVlan;
    }

    @Override
    public int getControlVlan() {
        return controlVlan;
    }

    @Override
    public int getPacketVlan() {
        return packetVlan;
    }

    @Override
    public int getStorageVlan() {
        return storageVlan;
    }

    @Override
    public long getvsmDomainId() {
        return vsmDomainId;
    }

    @Override
    public VSMConfigMode getvsmConfigMode() {
        return vsmConfigMode;
    }

    @Override
    public VSMConfigState getvsmConfigState() {
        return vsmConfigState;
    }

    @Override
    public VSMDeviceState getvsmDeviceState() {
        return vsmDeviceState;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    // Setter methods

    public void setHostId(long hostid) {
        this.hostId = hostid;
    }

    public void setVsmUserName(String username) {
        this.vsmUserName = username;
    }

    public void setVsmName(String vsmName) {
        this.vsmName = vsmName;
    }

    public void setVsmPassword(String password) {
        this.vsmPassword = password;
    }

    public void setMgmtIpAddr(String ipaddr) {
        this.ipaddr = ipaddr;
    }

    public void setManagementVlan(int vlan) {
        this.managementVlan = vlan;
    }

    public void setControlVlan(int vlan) {
        this.controlVlan = vlan;
    }

    public void setPacketVlan(int vlan) {
        this.packetVlan = vlan;
    }

    public void setStorageVlan(int vlan) {
        this.storageVlan = vlan;
    }

    public void setVsmDomainId(long id) {
        this.vsmDomainId = id;
    }

    public void setVsmConfigMode(VSMConfigMode mode) {
        this.vsmConfigMode = mode;
    }

    public void setVsmConfigState(VSMConfigState state) {
        this.vsmConfigState = state;
    }

    public void setVsmDeviceState(VSMDeviceState devState) {
        this.vsmDeviceState = devState;
    }


    // Constructors.

    public CiscoNexusVSMDeviceVO(String vsmIpAddr, String username, String password) {
        // Set all the VSM's properties here.
        this.uuid = UUID.randomUUID().toString();
        this.setMgmtIpAddr(vsmIpAddr);
        this.setVsmUserName(username);
        this.setVsmPassword(password);
        this.setVsmName(vsmName);
        this.setVsmDeviceState(VSMDeviceState.Enabled);
    }

    public CiscoNexusVSMDeviceVO() {
        this.uuid = UUID.randomUUID().toString();
    }
}
