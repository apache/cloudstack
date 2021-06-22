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

package com.cloud.hypervisor.ovm3.resources.helpers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.commons.lang.BooleanUtils;
import org.apache.log4j.Logger;

import com.cloud.hypervisor.ovm3.objects.Network;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.net.NetUtils;

/* holds config data for the Ovm3 Hypervisor */
public class Ovm3Configuration {
    private static final Logger LOGGER = Logger
            .getLogger(Ovm3Configuration.class);
    private String agentIp;
    private Long agentZoneId;
    private Long agentPodId;
    private String agentPoolId;
    private Long agentClusterId;
    private String agentHostname;
    private String csHostGuid;
    private String agentSshUserName = "root";
    private String agentSshPassword;
    private String agentOvsAgentUser = "oracle";
    private String agentOvsAgentPassword;
    private Integer agentOvsAgentPort = 8899;
    private Boolean agentOvsAgentSsl = false;
    private String agentSshKeyFile = "id_rsa.cloud";
    private String agentOwnedByUuid = "d1a749d4295041fb99854f52ea4dea97";
    private Boolean agentIsPrimary = false;
    private Boolean agentHasPrimary = false;
    private Boolean agentInOvm3Pool = false;
    private Boolean agentInOvm3Cluster = false;
    private String ovm3PoolVip = "";
    private String agentPrivateNetworkName;
    private String agentPublicNetworkName;
    private String agentGuestNetworkName;
    private String agentStorageNetworkName;
    private String agentControlNetworkName = "control0";
    private String agentOvmRepoPath = "/OVS/Repositories";
    private String agentSecStoragePath = "/nfsmnt";
    private String agentScript = "cloudstack.py";
    private String agentCheckStorageScript = "storagehealth.py";
    private Integer agentStorageCheckTimeout = 120;
    private Integer agentStorageCheckInterval = 1;
    private List<String> agentScripts = Arrays.asList(agentCheckStorageScript, agentScript);
    private String agentScriptsDir = "/opt/cloudstack/bin";
    private int domRSshPort = 3922;
    private String domRCloudPath = "/opt/cloud/bin/";
    private String virtualdiskdir = "VirtualDisks";
    private String templatedir = "Templates";
    private Map<String, Network.Interface> agentInterfaces = null;
    private Boolean istest = false;
    private Map<String, Object> rawParams = new HashMap<String, Object>();

    public Ovm3Configuration(Map<String, Object> params)
            throws ConfigurationException {
        setAgentZoneId(Long.parseLong((String) params.get("zone")));
        setAgentPodId(Long.parseLong(validateParam("PodId",
                (String) params.get("pod"))));
        setAgentClusterId(Long.parseLong((String) params.get("cluster")));
        setOvm3PoolVip(String.valueOf(params.get("ovm3vip")));
        setAgentInOvm3Pool(BooleanUtils.toBoolean((String) params
                .get("ovm3pool")));
        setAgentInOvm3Cluster(BooleanUtils.toBoolean((String) params
                .get("ovm3cluster")));
        setAgentHostname(validateParam("Hostname", (String) params.get("host")));
        setAgentIp((String) params.get("ip"));
        if (params.get("agentport") != null) {
            setAgentOvsAgentPort(Integer.parseInt((String) params
                    .get("agentport")));
        }
        setAgentSshUserName(validateParam("Username",
                (String) params.get("username")));
        setAgentSshPassword(validateParam("Password",
                (String) params.get("password")));
        setCsHostGuid(validateParam("Cloudstack Host GUID", (String) params.get("guid")));
        setAgentOvsAgentUser(validateParam("OVS Username",
                (String) params.get("agentusername")));
        setAgentOvsAgentPassword(validateParam("OVS Password",
                (String) params.get("agentpassword")));
        setAgentPrivateNetworkName((String) params
                .get("private.network.device"));
        setAgentPublicNetworkName((String) params.get("public.network.device"));
        setAgentGuestNetworkName((String) params.get("guest.network.device"));
        setAgentStorageNetworkName((String) params
                .get("storage.network.device1"));
        this.setAgentStorageCheckTimeout(NumbersUtil.parseInt(
                (String) params.get("ovm3.heartbeat.timeout"),
                agentStorageCheckTimeout));
        this.setAgentStorageCheckInterval(NumbersUtil.parseInt(
                (String) params.get("ovm3.heartbeat.interval"),
                agentStorageCheckInterval));
        validatePoolAndCluster();
        if (params.containsKey("istest")) {
            setIsTest((Boolean) params.get("istest"));
        }
    }

    /**
     * validatePoolAndCluster:
     * A cluster is impossible with a  pool.
     * A pool is impossible without a vip.
     */
    private void validatePoolAndCluster() {
        if (agentInOvm3Cluster) {
            LOGGER.debug("Clustering requires a pool, setting pool to true");
            agentInOvm3Pool = true;
        }
        if (!NetUtils.isValidIp4(ovm3PoolVip)) {
            LOGGER.debug("No VIP, Setting ovm3pool and ovm3cluster to false");
            agentInOvm3Pool = false;
            agentInOvm3Cluster = false;
            ovm3PoolVip = "";
        }
    }

    public String getAgentName() {
        return agentHostname;
    }

    public void setAgentName(String agentName) {
        this.agentHostname = agentName;
    }

    public String getAgentIp() {
        return agentIp;
    }

    public void setAgentIp(String agentIp) {
        this.agentIp = agentIp;
    }

    public Long getAgentZoneId() {
        return agentZoneId;
    }

    public void setAgentZoneId(Long agentZoneId) {
        this.agentZoneId = agentZoneId;
    }

    public Long getAgentPodId() {
        return agentPodId;
    }

    public void setAgentPodId(Long agentPodId) {
        this.agentPodId = agentPodId;
    }

    public String getAgentPoolId() {
        return agentPoolId;
    }

    public void setAgentPoolId(String agentPoolId) {
        this.agentPoolId = agentPoolId;
    }

    public Long getAgentClusterId() {
        return agentClusterId;
    }

    public void setAgentClusterId(Long agentClusterId) {
        this.agentClusterId = agentClusterId;
    }

    public String getAgentHostname() {
        return agentHostname;
    }

    public void setAgentHostname(String agentHostname) {
        this.agentHostname = agentHostname;
    }

    public String getCsHostGuid() {
        return csHostGuid;
    }

    public void setCsHostGuid(String csHostGuid) {
        this.csHostGuid = csHostGuid;
    }

    public String getAgentSshUserName() {
        return agentSshUserName;
    }

    public void setAgentSshUserName(String agentSshUserName) {
        this.agentSshUserName = agentSshUserName;
    }

    public String getAgentSshPassword() {
        return agentSshPassword;
    }

    public void setAgentSshPassword(String agentSshPassword) {
        this.agentSshPassword = agentSshPassword;
    }

    public String getAgentOvsAgentUser() {
        return agentOvsAgentUser;
    }

    public void setAgentOvsAgentUser(String agentOvsAgentUser) {
        this.agentOvsAgentUser = agentOvsAgentUser;
    }

    public String getAgentOvsAgentPassword() {
        return agentOvsAgentPassword;
    }

    public void setAgentOvsAgentPassword(String agentOvsAgentPassword) {
        this.agentOvsAgentPassword = agentOvsAgentPassword;
    }

    public Integer getAgentOvsAgentPort() {
        return agentOvsAgentPort;
    }

    public void setAgentOvsAgentPort(Integer agentOvsAgentPort) {
        this.agentOvsAgentPort = agentOvsAgentPort;
    }

    public Boolean getAgentOvsAgentSsl() {
        return agentOvsAgentSsl;
    }

    public void setAgentOvsAgentSsl(Boolean agentOvsAgentSsl) {
        this.agentOvsAgentSsl = agentOvsAgentSsl;
    }

    public String getAgentSshKeyFileName() {
        return agentSshKeyFile;
    }

    public void setAgentSshKeyFileName(String agentSshFile) {
        this.agentSshKeyFile = agentSshFile;
    }

    public String getAgentOwnedByUuid() {
        return agentOwnedByUuid;
    }

    public void setAgentOwnedByUuid(String agentOwnedByUuid) {
        this.agentOwnedByUuid = agentOwnedByUuid;
    }

    public Boolean getAgentIsPrimary() {
        return agentIsPrimary;
    }

    public void setAgentIsPrimary(Boolean agentIsPrimary) {
        this.agentIsPrimary = agentIsPrimary;
    }

    public Boolean getAgentHasPrimary() {
        return agentHasPrimary;
    }

    public void setAgentHasPrimary(Boolean agentHasPrimary) {
        this.agentHasPrimary = agentHasPrimary;
    }

    public Boolean getAgentInOvm3Pool() {
        return agentInOvm3Pool;
    }

    public void setAgentInOvm3Pool(Boolean agentInOvm3Pool) {
        this.agentInOvm3Pool = agentInOvm3Pool;
    }

    public Boolean getAgentInOvm3Cluster() {
        return agentInOvm3Cluster;
    }

    public void setAgentInOvm3Cluster(Boolean agentInOvm3Cluster) {
        this.agentInOvm3Cluster = agentInOvm3Cluster;
    }

    public String getOvm3PoolVip() {
        return ovm3PoolVip;
    }

    public void setOvm3PoolVip(String ovm3PoolVip) {
        this.ovm3PoolVip = ovm3PoolVip;
    }

    public String getAgentPrivateNetworkName() {
        return agentPrivateNetworkName;
    }

    public void setAgentPrivateNetworkName(String agentPrivateNetworkName) {
        this.agentPrivateNetworkName = agentPrivateNetworkName;
    }

    public String getAgentPublicNetworkName() {
        return agentPublicNetworkName;
    }

    public void setAgentPublicNetworkName(String agentPublicNetworkName) {
        this.agentPublicNetworkName = agentPublicNetworkName;
    }

    public String getAgentGuestNetworkName() {
        return agentGuestNetworkName;
    }

    public void setAgentGuestNetworkName(String agentGuestNetworkName) {
        this.agentGuestNetworkName = agentGuestNetworkName;
    }

    public String getAgentStorageNetworkName() {
        return agentStorageNetworkName;
    }

    public void setAgentStorageNetworkName(String agentStorageNetworkName) {
        this.agentStorageNetworkName = agentStorageNetworkName;
    }

    public String getAgentControlNetworkName() {
        return agentControlNetworkName;
    }

    public void setAgentControlNetworkName(String agentControlNetworkName) {
        this.agentControlNetworkName = agentControlNetworkName;
    }

    public String getAgentOvmRepoPath() {
        return agentOvmRepoPath;
    }

    public void setAgentOvmRepoPath(String agentOvmRepoPath) {
        this.agentOvmRepoPath = agentOvmRepoPath;
    }

    public String getAgentSecStoragePath() {
        return agentSecStoragePath;
    }

    public void setAgentSecStoragePath(String agentSecStoragePath) {
        this.agentSecStoragePath = agentSecStoragePath;
    }

    public int getDomRSshPort() {
        return domRSshPort;
    }

    public void setDomRSshPort(int domRSshPort) {
        this.domRSshPort = domRSshPort;
    }

    public String getDomRCloudPath() {
        return domRCloudPath;
    }

    public void setDomRCloudPath(String domRCloudPath) {
        this.domRCloudPath = domRCloudPath;
    }
    public Boolean getIsTest() {
        return istest;
    }
    public void setIsTest(Boolean t) {
        istest = t;
    }
    public String getAgentScript() {
        return agentScript;
    }

    public void setAgentScript(String agentScript) {
        this.agentScript = agentScript;
    }

    public String getAgentScriptsDir() {
        return agentScriptsDir;
    }

    public void setAgentScriptsDir(String agentScriptsDir) {
        this.agentScriptsDir = agentScriptsDir;
    }

    public Map<String, Network.Interface> getAgentInterfaces() {
        return agentInterfaces;
    }

    public void setAgentInterfaces(Map<String, Network.Interface> agentInterfaces) {
        this.agentInterfaces = agentInterfaces;
    }
    public List<String> getAgentScripts() {
        return agentScripts;
    }

    public void setAgentScripts(List<String> agentScripts) {
        this.agentScripts = agentScripts;
    }
    public String getAgentCheckStorageScript() {
        return agentCheckStorageScript;
    }

    public void setAgentCheckStorageScript(String agentCheckStorageScript) {
        this.agentCheckStorageScript = agentCheckStorageScript;
    }
    public Integer getAgentStorageCheckTimeout() {
        return agentStorageCheckTimeout;
    }
    public void setAgentStorageCheckTimeout(Integer agentStorageCheckTimeout) {
        this.agentStorageCheckTimeout = agentStorageCheckTimeout;
    }

    public Integer getAgentStorageCheckInterval() {
        return agentStorageCheckInterval;
    }

    public void setAgentStorageCheckInterval(Integer agentStorageCheckInterval) {
        this.agentStorageCheckInterval = agentStorageCheckInterval;
    }
    public String getVirtualDiskDir() {
        return this.virtualdiskdir;
    }
    public String getTemplateDir() {
        return this.templatedir;
    }
    /**
     * ValidateParam: Validate the input for configure
     * @param name
     * @param param
     * @return param
     * @throws ConfigurationException
     */
    private String validateParam(String name, String param) throws ConfigurationException {
        if (param == null) {
            String msg = "Unable to get " + name + " params are null";
            LOGGER.debug(msg);
            throw new ConfigurationException(msg);
        }
        return param;
    }

    public Map<String, Object> getRawParams() {
        return rawParams;
    }
    public void setRawParams(Map<String, Object> params) {
        rawParams.putAll(params);
    }
}
