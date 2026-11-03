/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.storage.feign.model;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IpInterface {
    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("name")
    private String name;

    @JsonProperty("ip")
    private IpInfo ip;

    @JsonProperty("svm")
    private Svm svm;

    @JsonProperty("services")
    private List<String> services;

    @JsonProperty("state")
    private String state;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("location")
    private Location location;

    // Getters and setters
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IpInfo getIp() {
        return ip;
    }

    public void setIp(IpInfo ip) {
        this.ip = ip;
    }

    public Svm getSvm() {
        return svm;
    }

    public void setSvm(Svm svm) {
        this.svm = svm;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IpInterface that = (IpInterface) o;
        return Objects.equals(uuid, that.uuid) &&
                Objects.equals(name, that.name) &&
                Objects.equals(ip, that.ip) &&
                Objects.equals(svm, that.svm) &&
                Objects.equals(services, that.services) &&
                Objects.equals(state, that.state) &&
                Objects.equals(enabled, that.enabled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, name, ip, svm, services, state, enabled);
    }

    @Override
    public String toString() {
        return "IpInterface{" +
                "uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", ip=" + ip +
                ", svm=" + svm +
                ", services=" + services +
                ", state='" + state + '\'' +
                ", enabled=" + enabled +
                '}';
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Node {
        @JsonProperty("name")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Location {
        @JsonProperty("home_node")
        private Node homeNode;

        @JsonProperty("node")
        private Node node;

        public Node getHomeNode() {
            return homeNode;
        }

        public void setHomeNode(Node homeNode) {
            this.homeNode = homeNode;
        }

        public Node getNode() {
            return node;
        }

        public void setNode(Node node) {
            this.node = node;
        }
    }

    // Nested class for IP information
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IpInfo {
        @JsonProperty("address")
        private String address;

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IpInfo ipInfo = (IpInfo) o;
            return Objects.equals(address, ipInfo.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address);
        }

        @Override
        public String toString() {
            return "IpInfo{" +
                    "address='" + address + '\'' +
                    '}';
        }
    }
}
