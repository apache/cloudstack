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
package org.apache.cloudstack.engine.subsystem.api.storage;

import java.util.Map;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage.StoragePoolType;

public class PrimaryDataStoreParameters {
    private Long zoneId;
    private Long podId;
    private Long clusterId;
    private String providerName;
    private Map<String, String> details;
    private String tags;
    private StoragePoolType type;
    private HypervisorType hypervisorType;
    private String host;
    private String path;
    private int port;
    private String uuid;
    private String name;
    private String userInfo;
    private long capacityBytes;
    private long usedBytes;
    private boolean managed;
    private Long capacityIops;

    /**
     * @return the userInfo
     */
    public String getUserInfo() {
        return userInfo;
    }

    /**
     * @param userInfo
     *            the userInfo to set
     */
    public void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the uuid
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * @param uuid
     *            the uuid to set
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port
     *            the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path
     *            the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host
     *            the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the type
     */
    public StoragePoolType getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(StoragePoolType type) {
        this.type = type;
    }

    /**
     * @return the tags
     */
    public String getTags() {
        return tags;
    }

    /**
     * @param tags
     *            the tags to set
     */
    public void setTags(String tags) {
        this.tags = tags;
    }

    /**
     * @return the details
     */
    public Map<String, String> getDetails() {
        return details;
    }

    /**
     * @param details
     *            the details to set
     */
    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    /**
     * @return the providerName
     */
    public String getProviderName() {
        return providerName;
    }

    /**
     * @param providerName
     *            the providerName to set
     */
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public void setManaged(boolean managed) {
        this.managed = managed;
    }

    public boolean isManaged() {
        return managed;
    }

    public void setCapacityIops(Long capacityIops) {
        this.capacityIops = capacityIops;
    }

    public Long getCapacityIops() {
        return capacityIops;
    }

    public void setHypervisorType(HypervisorType hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    /**
     * @return the clusterId
     */
    public Long getClusterId() {
        return clusterId;
    }

    /**
     * @param clusterId
     *            the clusterId to set
     */
    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    /**
     * @return the podId
     */
    public Long getPodId() {
        return podId;
    }

    /**
     * @param podId
     *            the podId to set
     */
    public void setPodId(Long podId) {
        this.podId = podId;
    }

    /**
     * @return the zoneId
     */
    public Long getZoneId() {
        return zoneId;
    }

    /**
     * @param zoneId
     *            the zoneId to set
     */
    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public long getCapacityBytes() {
        return capacityBytes;
    }

    public void setCapacityBytes(long capacityBytes) {
        this.capacityBytes = capacityBytes;
    }

    public long getUsedBytes() {
        return usedBytes;
    }

    public void setUsedBytes(long usedBytes) {
        this.usedBytes = usedBytes;
    }
}
