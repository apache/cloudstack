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
package org.apache.cloudstack.entity.cloud;

import java.net.URI;

import org.apache.cloudstack.entity.CloudResource;

import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.Mode;
import com.cloud.vm.Nic.State;

/**
 * Nic resource entity.
 */
public class NicResource extends CloudResource {

    // attributes
    private boolean defaultNic;
    private AddressFormat addressFormat;
    private Mode mode;
    private State state;
    private int deviceId;
    private URI isolationUri;
    private URI broadcastUri;
    private String macAddress; // L2 concept, same for ip4 and ip6
    private String ip4Address;
    private String ip4Cidr;    // use /<prefix length> format
    private String ip4Gateway;
    private String ip6Address;
    private String ip6Cidr;    // use /<prefix length> format
    private String ip6Gateway;

    //relationships
    private VirtualMachineResource vm;
    private NetworkResource network;


    public boolean isDefaultNic() {
        return defaultNic;
    }
    public void setDefaultNic(boolean defaultNic) {
        this.defaultNic = defaultNic;
    }
    public AddressFormat getAddressFormat() {
        return addressFormat;
    }
    public void setAddressFormat(AddressFormat addressFormat) {
        this.addressFormat = addressFormat;
    }
    public Mode getMode() {
        return mode;
    }
    public void setMode(Mode mode) {
        this.mode = mode;
    }
    public State getState() {
        return state;
    }
    public void setState(State state) {
        this.state = state;
    }
    public int getDeviceId() {
        return deviceId;
    }
    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }
    public URI getIsolationUri() {
        return isolationUri;
    }
    public void setIsolationUri(URI isolationUri) {
        this.isolationUri = isolationUri;
    }
    public URI getBroadcastUri() {
        return broadcastUri;
    }
    public void setBroadcastUri(URI broadcastUri) {
        this.broadcastUri = broadcastUri;
    }


    public String getMacAddress() {
        return macAddress;
    }
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
    public String getIp4Address() {
        return ip4Address;
    }
    public void setIp4Address(String ip4Address) {
        this.ip4Address = ip4Address;
    }


    public String getIp4Cidr() {
        return ip4Cidr;
    }
    public void setIp4Cidr(String ip4Cidr) {
        this.ip4Cidr = ip4Cidr;
    }
    public String getIp4Gateway() {
        return ip4Gateway;
    }
    public void setIp4Gateway(String ip4Gateway) {
        this.ip4Gateway = ip4Gateway;
    }
    public String getIp6Address() {
        return ip6Address;
    }
    public void setIp6Address(String ip6Address) {
        this.ip6Address = ip6Address;
    }


    public String getIp6Cidr() {
        return ip6Cidr;
    }
    public void setIp6Cidr(String ip6Cidr) {
        this.ip6Cidr = ip6Cidr;
    }
    public String getIp6Gateway() {
        return ip6Gateway;
    }
    public void setIp6Gateway(String ip6Gateway) {
        this.ip6Gateway = ip6Gateway;
    }
    public VirtualMachineResource getVm() {
        return vm;
    }
    public void setVm(VirtualMachineResource vm) {
        this.vm = vm;
    }
    public NetworkResource getNetwork() {
        return network;
    }
    public void setNetwork(NetworkResource network) {
        this.network = network;
    }



}
