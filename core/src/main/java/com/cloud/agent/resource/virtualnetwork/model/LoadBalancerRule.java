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

package com.cloud.agent.resource.virtualnetwork.model;


public class LoadBalancerRule {

    private String[] configuration;
    private String tmpCfgFilePath;
    private String tmpCfgFileName;
    private Boolean isTransparent;
    private String networkCidr;
    private SslCertEntry[] sslCerts;

    private String[] addRules;
    private String[] removeRules;
    private String[] statRules;

    private String routerIp;

    public static class SslCertEntry {
        private String name;
        private String cert;
        private String key;
        private String chain;
        private String password;

        public SslCertEntry(String name, String cert, String key, String chain, String password) {
            this.name = name;
            this.cert = cert;
            this.key = key;
            this.chain = chain;
            this.password = password;
        }

        public void setName(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
        public void setCert(String cert) {
            this.cert = cert;
        }
        public String getCert() {
            return cert;
        }
        public void setKey(String key) {
            this.key = key;
        }
        public String getKey() {
            return key;
        }
        public void setChain(String chain) {
            this.chain = chain;
        }
        public String getChain() {
            return chain;
        }
        public void setPassword(String password) {
            this.password = password;
        }
        public String getPassword() {
            return password;
        }
    }

    public LoadBalancerRule() {
        // Empty constructor for (de)serialization
    }

    public LoadBalancerRule(final String[] configuration, final String tmpCfgFilePath, final String tmpCfgFileName, final String[] addRules, final String[] removeRules, final String[] statRules, final String routerIp) {
        this.configuration = configuration;
        this.tmpCfgFilePath = tmpCfgFilePath;
        this.tmpCfgFileName = tmpCfgFileName;
        this.addRules = addRules;
        this.removeRules = removeRules;
        this.statRules = statRules;
        this.routerIp = routerIp;
    }

    public String[] getConfiguration() {
        return configuration;
    }

    public void setConfiguration(final String[] configuration) {
        this.configuration = configuration;
    }

    public String getTmpCfgFilePath() {
        return tmpCfgFilePath;
    }

    public void setTmpCfgFilePath(final String tmpCfgFilePath) {
        this.tmpCfgFilePath = tmpCfgFilePath;
    }

    public String getTmpCfgFileName() {
        return tmpCfgFileName;
    }

    public void setTmpCfgFileName(final String tmpCfgFileName) {
        this.tmpCfgFileName = tmpCfgFileName;
    }

    public void setIsTransparent(final Boolean isTransparent) {
        this.isTransparent = isTransparent;
    }

    public Boolean isTransparent() {
        return isTransparent;
    }

    public String[] getAddRules() {
        return addRules;
    }

    public void setAddRules(final String[] addRules) {
        this.addRules = addRules;
    }

    public String[] getRemoveRules() {
        return removeRules;
    }

    public void setRemoveRules(final String[] removeRules) {
        this.removeRules = removeRules;
    }

    public String[] getStatRules() {
        return statRules;
    }

    public void setStatRules(final String[] statRules) {
        this.statRules = statRules;
    }

    public String getRouterIp() {
        return routerIp;
    }

    public void setRouterIp(final String routerIp) {
        this.routerIp = routerIp;
    }

    public void setNetworkCidr(String networkCidr) {
        this.networkCidr = networkCidr;
    }

    public String getNetworkCidr() {
        return networkCidr;
    }

    public SslCertEntry[] getSslCerts() {
        return sslCerts;
    }

    public void setSslCerts(final SslCertEntry[] sslCerts) {
        this.sslCerts = sslCerts;
    }
}
