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
package com.cloud.bridge.model;

public interface SHost {

    public static final int STORAGE_HOST_TYPE_LOCAL = 0;
    public static final int STORAGE_HOST_TYPE_NFS = 1;
    public static final int STORAGE_HOST_TYPE_CASTOR = 2;

    public static enum StorageHostType {
        STORAGE_HOST_TYPE_LOCAL, //0
        STORAGE_HOST_TYPE_NFS, //1
        STORAGE_HOST_TYPE_CASTOR //2
    }
    /*    private Long id;

        private String host;
        private int hostType;
        private MHostVO mhost;
        private String exportRoot;
        private String userOnHost;
        private String userPassword;

        private Set<SBucket> buckets = new HashSet<SBucket>();
        private Set<MHostMount> mounts = new HashSet<MHostMount>();

        public SHost() {
        }

        public Long getId() {
            return id;
        }

        private void setId(Long id) {
            this.id = id;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getHostType() {
            return hostType;
        }

        public void setHostType(int hostType) {
            this.hostType = hostType;
        }

        public String getExportRoot() {
            return exportRoot;
        }

        public void setExportRoot(String exportRoot) {
            this.exportRoot = exportRoot;
        }

        public String getUserOnHost() {
            return userOnHost;
        }

        public void setUserOnHost(String userOnHost) {
            this.userOnHost = userOnHost;
        }

        public String getUserPassword() {
            return userPassword;
        }

        public void setUserPassword(String userPassword) {
            this.userPassword = userPassword;
        }

        public MHostVO getMhost() {
            return mhost;
        }

        public void setMhost(MHostVO mhost) {
            this.mhost = mhost;
        }

        public Set<SBucket> getBuckets() {
            return buckets;
        }

        public void setBuckets(Set<SBucket> buckets) {
            this.buckets = buckets;
        }

        public Set<MHostMount> getMounts() {
            return mounts;
        }

        public void setMounts(Set<MHostMount> mounts) {
            this.mounts = mounts;
        }*/
}
