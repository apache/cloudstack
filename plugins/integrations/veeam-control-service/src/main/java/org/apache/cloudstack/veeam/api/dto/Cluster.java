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

package org.apache.cloudstack.veeam.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "cluster")
public final class Cluster extends BaseDto {

    private String name;
    private String description;
    private String comment;
    private String ballooningEnabled;
    private String biosType;
    private Cpu cpu;
    private CustomSchedulingPolicyProperties customSchedulingPolicyProperties;
    private ErrorHandling errorHandling;
    private FencingPolicy fencingPolicy;
    private String fipsMode; // "disabled"
    private String firewallType; // "firewalld"
    private String glusterService;
    private String haReservation;
    private Ksm ksm;
    private String logMaxMemoryUsedThreshold;
    private String logMaxMemoryUsedThresholdType;
    private MemoryPolicy memoryPolicy;
    private Migration migration;
    private RequiredRngSources requiredRngSources;
    private String switchType;
    private String threadsAsCores;
    private String trustedService;
    private String tunnelMigration;
    private String upgradeInProgress;
    private String upgradePercentComplete;
    private Version version;
    private String virtService;
    private String vncEncryption;
    private Ref dataCenter;
    private Ref macPool;
    private Ref schedulingPolicy;
    private Actions actions;
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Link> link;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getBallooningEnabled() {
        return ballooningEnabled;
    }

    public void setBallooningEnabled(String ballooningEnabled) {
        this.ballooningEnabled = ballooningEnabled;
    }

    public String getBiosType() {
        return biosType;
    }

    public void setBiosType(String biosType) {
        this.biosType = biosType;
    }

    public Cpu getCpu() {
        return cpu;
    }

    public void setCpu(Cpu cpu) {
        this.cpu = cpu;
    }

    public CustomSchedulingPolicyProperties getCustomSchedulingPolicyProperties() {
        return customSchedulingPolicyProperties;
    }

    public void setCustomSchedulingPolicyProperties(CustomSchedulingPolicyProperties customSchedulingPolicyProperties) {
        this.customSchedulingPolicyProperties = customSchedulingPolicyProperties;
    }

    public ErrorHandling getErrorHandling() {
        return errorHandling;
    }

    public void setErrorHandling(ErrorHandling errorHandling) {
        this.errorHandling = errorHandling;
    }

    public FencingPolicy getFencingPolicy() {
        return fencingPolicy;
    }

    public void setFencingPolicy(FencingPolicy fencingPolicy) {
        this.fencingPolicy = fencingPolicy;
    }

    public String getFipsMode() {
        return fipsMode;
    }

    public void setFipsMode(String fipsMode) {
        this.fipsMode = fipsMode;
    }

    public String getFirewallType() {
        return firewallType;
    }

    public void setFirewallType(String firewallType) {
        this.firewallType = firewallType;
    }

    public String getGlusterService() {
        return glusterService;
    }

    public void setGlusterService(String glusterService) {
        this.glusterService = glusterService;
    }

    public String getHaReservation() {
        return haReservation;
    }

    public void setHaReservation(String haReservation) {
        this.haReservation = haReservation;
    }

    public Ksm getKsm() {
        return ksm;
    }

    public void setKsm(Ksm ksm) {
        this.ksm = ksm;
    }

    public String getLogMaxMemoryUsedThreshold() {
        return logMaxMemoryUsedThreshold;
    }

    public void setLogMaxMemoryUsedThreshold(String logMaxMemoryUsedThreshold) {
        this.logMaxMemoryUsedThreshold = logMaxMemoryUsedThreshold;
    }

    public String getLogMaxMemoryUsedThresholdType() {
        return logMaxMemoryUsedThresholdType;
    }

    public void setLogMaxMemoryUsedThresholdType(String logMaxMemoryUsedThresholdType) {
        this.logMaxMemoryUsedThresholdType = logMaxMemoryUsedThresholdType;
    }

    public MemoryPolicy getMemoryPolicy() {
        return memoryPolicy;
    }

    public void setMemoryPolicy(MemoryPolicy memoryPolicy) {
        this.memoryPolicy = memoryPolicy;
    }

    public Migration getMigration() {
        return migration;
    }

    public void setMigration(Migration migration) {
        this.migration = migration;
    }

    public RequiredRngSources getRequiredRngSources() {
        return requiredRngSources;
    }

    public void setRequiredRngSources(RequiredRngSources requiredRngSources) {
        this.requiredRngSources = requiredRngSources;
    }

    public String getSwitchType() {
        return switchType;
    }

    public void setSwitchType(String switchType) {
        this.switchType = switchType;
    }

    public String getThreadsAsCores() {
        return threadsAsCores;
    }

    public void setThreadsAsCores(String threadsAsCores) {
        this.threadsAsCores = threadsAsCores;
    }

    public String getTrustedService() {
        return trustedService;
    }

    public void setTrustedService(String trustedService) {
        this.trustedService = trustedService;
    }

    public String getTunnelMigration() {
        return tunnelMigration;
    }

    public void setTunnelMigration(String tunnelMigration) {
        this.tunnelMigration = tunnelMigration;
    }

    public String getUpgradeInProgress() {
        return upgradeInProgress;
    }

    public void setUpgradeInProgress(String upgradeInProgress) {
        this.upgradeInProgress = upgradeInProgress;
    }

    public String getUpgradePercentComplete() {
        return upgradePercentComplete;
    }

    public void setUpgradePercentComplete(String upgradePercentComplete) {
        this.upgradePercentComplete = upgradePercentComplete;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public String getVirtService() {
        return virtService;
    }

    public void setVirtService(String virtService) {
        this.virtService = virtService;
    }

    public String getVncEncryption() {
        return vncEncryption;
    }

    public void setVncEncryption(String vncEncryption) {
        this.vncEncryption = vncEncryption;
    }

    public Ref getDataCenter() {
        return dataCenter;
    }

    public void setDataCenter(Ref dataCenter) {
        this.dataCenter = dataCenter;
    }

    public Ref getMacPool() {
        return macPool;
    }

    public void setMacPool(Ref macPool) {
        this.macPool = macPool;
    }

    public Ref getSchedulingPolicy() {
        return schedulingPolicy;
    }

    public void setSchedulingPolicy(Ref schedulingPolicy) {
        this.schedulingPolicy = schedulingPolicy;
    }

    public Actions getActions() {
        return actions;
    }

    public void setActions(Actions actions) {
        this.actions = actions;
    }

    public List<Link> getLink() {
        return link;
    }

    public void setLink(List<Link> link) {
        this.link = link;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class CustomSchedulingPolicyProperties {
        @JacksonXmlElementWrapper(useWrapping = false)
        public List<Property> property;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Property {
        public String name;
        public String value;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class ErrorHandling {
        public String onError; // "migrate"
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class FencingPolicy {
        public String enabled;
        public SkipIfConnectivityBroken skipIfConnectivityBroken;
        public String skipIfGlusterBricksUp;
        public String skipIfGlusterQuorumNotMet;
        public SkipIfSdActive skipIfSdActive;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class SkipIfConnectivityBroken {
        public String enabled;
        public String threshold;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class SkipIfSdActive {
        public String enabled;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Ksm {
        public String enabled;
        public String mergeAcrossNodes;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class MemoryPolicy {
        public OverCommit overCommit;
        public TransparentHugepages transparentHugepages;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class OverCommit {
        public String percent;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class TransparentHugepages {
        public String enabled;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Migration {
        public String autoConverge;
        public Bandwidth bandwidth;
        public String compressed;
        public String encrypted;
        public String parallelMigrationsPolicy;
        public Ref policy;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Bandwidth {
        public String assignmentMethod;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class RequiredRngSources {
        @JacksonXmlElementWrapper(useWrapping = false)
        public List<String> requiredRngSource;
    }
}
