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

package org.apache.cloudstack.storage.datastore.util;

import com.cloud.utils.StringUtils;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DateraObject {

    public static final String DEFAULT_CREATE_MODE = "cloudstack";
    public static final String DEFAULT_STORAGE_NAME = "storage-1";
    public static final String DEFAULT_VOLUME_NAME = "volume-1";
    public static final String DEFAULT_ACL = "deny_all";

    public enum AppState {
        ONLINE, OFFLINE;

        @Override
        public String toString(){
            return this.name().toLowerCase();
        }
    }


    public enum DateraOperation {
        ADD, REMOVE;

        @Override
        public String toString(){
            return this.name().toLowerCase();
        }
    }

    public enum DateraErrorTypes {
        PermissionDeniedError, InvalidRouteError, AuthFailedError,
        ValidationFailedError, InvalidRequestError, NotFoundError,
        NotConnectedError, InvalidSessionKeyError, DatabaseError,
        InternalError;

        public boolean equals(DateraError err){
            return this.name().equals(err.getName());
        }
    }

    public static class DateraConnection {

        private int managementPort;
        private String managementIp;
        private String username;
        private String password;

        public DateraConnection(String managementIp, int managementPort, String username, String password) {
            this.managementPort = managementPort;
            this.managementIp = managementIp;
            this.username = username;
            this.password = password;
        }

        public int getManagementPort() {
            return managementPort;
        }

        public String getManagementIp() {
            return managementIp;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }

    public static class DateraLogin {

        private final String name;
        private final String password;

        public DateraLogin(String username, String password) {
            this.name = username;
            this.password = password;
        }
    }

    public static class DateraLoginResponse {

        private String key;

        public String getKey() {
            return key;
        }
    }

    public class Access {
        private String iqn;
        private List<String> ips;


        public Access(String iqn, List<String> ips) {
            this.iqn = iqn;
            this.ips = ips;
        }

        public String getIqn() {
            return iqn;
        }
    }

    public static class PerformancePolicy {

        @SerializedName("total_iops_max")
        private Integer totalIops;


        public PerformancePolicy(int totalIops) {
            this.totalIops = totalIops;
        }

        public Integer getTotalIops() {
            return totalIops;
        }
    }

    public static class Volume {

        private String name;
        private String path;
        private Integer size;

        @SerializedName("replica_count")
        private Integer replicaCount;

        @SerializedName("performance_policy")
        private PerformancePolicy performancePolicy;

        @SerializedName("placement_mode")
        private String placementMode;

        @SerializedName("op_state")
        private String opState;

        public Volume(int size, int totalIops, int replicaCount) {
            this.name = DEFAULT_VOLUME_NAME;
            this.size = size;
            this.replicaCount = replicaCount;
            this.performancePolicy = new PerformancePolicy(totalIops);
        }

        public Volume(int size, int totalIops, int replicaCount, String placementMode) {
            this.name = DEFAULT_VOLUME_NAME;
            this.size = size;
            this.replicaCount = replicaCount;
            this.performancePolicy = new PerformancePolicy(totalIops);
            this.placementMode = placementMode;
        }

        public Volume(Integer newSize) {
            this.size = newSize;
        }

        public Volume(String newPlacementMode) {
            this.placementMode = newPlacementMode;
        }

        public PerformancePolicy getPerformancePolicy() {
            return performancePolicy;
        }

        public int getSize() {
            return size;
        }

        public String getPlacementMode(){
            return placementMode;
        }

        public String getPath(){
            return path;
        }

        public String getOpState() {
            return opState;
        }
    }

    public static class StorageInstance {

        private final String name = DEFAULT_STORAGE_NAME;
        private Map<String, Volume> volumes;
        private Access access;

        public StorageInstance(int size, int totalIops, int replicaCount) {
            Volume volume = new Volume(size, totalIops, replicaCount);
            volumes = new HashMap<String, Volume>();
            volumes.put(DEFAULT_VOLUME_NAME, volume);
        }

        public StorageInstance(int size, int totalIops, int replicaCount, String placementMode) {
            Volume volume = new Volume(size, totalIops, replicaCount, placementMode);
            volumes = new HashMap<String, Volume>();
            volumes.put(DEFAULT_VOLUME_NAME, volume);
        }

        public Access getAccess(){
            return access;
        }

        public Volume getVolume() {
            return volumes.get(DEFAULT_VOLUME_NAME);
        }

        public int getSize() {
            return getVolume().getSize();
        }

    }

    public static class AppInstance {

        private String name;

        @SerializedName("access_control_mode")
        private String accessControlMode;

        @SerializedName("create_mode")
        private String createMode;

        @SerializedName("storage_instances")
        private Map<String, StorageInstance> storageInstances;

        @SerializedName("clone_src")
        private String cloneSrc;

        @SerializedName("admin_state")
        private String adminState;
        private Boolean force;

        public AppInstance(String name, int size, int totalIops, int replicaCount) {
            this.name = name;
            StorageInstance storageInstance = new StorageInstance(size, totalIops, replicaCount);
            this.storageInstances = new HashMap<String, StorageInstance>();
            this.storageInstances.put(DEFAULT_STORAGE_NAME, storageInstance);
            this.accessControlMode = DEFAULT_ACL;
            this.createMode = DEFAULT_CREATE_MODE;
        }

        public AppInstance(String name, int size, int totalIops, int replicaCount, String placementMode) {
            this.name = name;
            StorageInstance storageInstance = new StorageInstance(size, totalIops, replicaCount, placementMode);
            this.storageInstances = new HashMap<String, StorageInstance>();
            this.storageInstances.put(DEFAULT_STORAGE_NAME, storageInstance);
            this.accessControlMode = DEFAULT_ACL;
            this.createMode = DEFAULT_CREATE_MODE;
        }

        public AppInstance(AppState state) {
            this.adminState = state.toString();
            this.force = true;
        }

        public AppInstance(String name, String cloneSrc) {
            this.name = name;
            this.cloneSrc = cloneSrc;
        }

        public String getIqn() {
            StorageInstance storageInstance = storageInstances.get(DEFAULT_STORAGE_NAME);
            return storageInstance.getAccess().getIqn();
        }

        public int getTotalIops() {
            StorageInstance storageInstance = storageInstances.get(DEFAULT_STORAGE_NAME) ;
            return storageInstance.getVolume().getPerformancePolicy().getTotalIops();
        }

        public String getName() {
            return name;
        }

        public int getSize() {
            StorageInstance storageInstance = storageInstances.get(DEFAULT_STORAGE_NAME);
            return storageInstance.getSize();
        }

        public String getVolumePath(){
            StorageInstance storageInstance = storageInstances.get(DEFAULT_STORAGE_NAME);
            return storageInstance.getVolume().getPath();
        }

        public String getVolumeOpState(){
            StorageInstance storageInstance = storageInstances.get(DEFAULT_STORAGE_NAME);
            return storageInstance.getVolume().getOpState();
        }
    }

    public static class Initiator {

        private String id; // IQN
        private String name;
        private String path;
        private String op;

        public Initiator(String name, String id) {
            this.id = id;
            this.name = name;
        }

        public Initiator(String path, DateraOperation op){
            this.path = path;
            this.op = op.toString();
        }

        public String getPath() {
            return path;
        }
    }

    public static class InitiatorGroup {

        private String name;
        private List<String> members;
        private String path;
        private String op;

        public InitiatorGroup(String name, List<String> members) {
            this.name = name;
            this.members = members;
        }

        public InitiatorGroup(String path, DateraOperation op) {
            this.path = path;
            this.op = op.toString();
        }

        public String getPath() {
            return path;
        }

        public String getName() {
            return name;
        }

        public List<String> getMembers() {
            return members;
        }
    }


    public static class VolumeSnapshot {

        private String uuid;
        private String timestamp;
        private String path;

        @SerializedName("op_state")
        private String opState;


        VolumeSnapshot(String uuid) {
            this.uuid = uuid;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getOpState() {
            return opState;
        }

        public String getPath(){
            return path;
        }
    }

    public static class DateraError extends Exception {

        private String name;
        private int code;
        private List<String> errors;
        private String message;

        public DateraError(String name, int code, List<String> errors, String message) {
            this.name = name;
            this.code = code;
            this.errors = errors;
            this.message = message;
        }

        public List<String> getErrors() {
            return errors;
        }

        public boolean isError() {
            return message != null && name.endsWith("Error");
        }

        public String getMessage() {

            String errMesg = name  + "\n";
            if (message != null) {
                errMesg += message + "\n";
            }

            if (errors != null) {
                errMesg += StringUtils.join(errors, "\n");

            }

            return errMesg;
        }

        public String getName(){
            return name;
        }
    }
}
