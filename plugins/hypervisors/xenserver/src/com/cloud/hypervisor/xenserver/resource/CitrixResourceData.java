package com.cloud.hypervisor.xenserver.resource;

import java.util.LinkedList;
import java.util.Queue;

public class CitrixResourceData {
    private boolean bridgeFirewall = false;
    private String cluster;
    private String consolidationFunction = "AVERAGE";
    private long dcId;
    private String guestNetworkName;
    private int heartbeatInterval = 60;
    private int heartbeatTimeout =  120;
    private String instance;
    private boolean ovs = false;
    private String linkLocalPrivateNetworkName;
    private int maxNics = 7;
    private int maxWeight = 256;
    private int migrateWait;
    private String name;
    private Queue<String> passwords = new LinkedList<String>();
    private String pod;
    private int pollingIntervalInSeconds = 60;
    private String privateNetworkName;
    private String publicNetworkName;
    private int retry = 100;
    private boolean securityGroupEnabled;
    private int sleep = 10000;
    private String storageNetworkName1;
    private String storageNetworkName2;
    private String username;
    private String configDriveIsoPath = "/opt/xensource/packages/configdrive_iso/";
    private String configDriveSRName = "ConfigDriveISOs";
    private String attachIsoDeviceNumber = "3";
    private int wait;
    private long xsMemoryUsed = 128 * 1024 * 1024L;
    private double xsVirtualizationFactor = 63.0 / 64.0;

    public CitrixResourceData() {
    }

    public boolean isBridgeFirewall() {
        return bridgeFirewall;
    }

    public void setBridgeFirewall(boolean canBridgeFirewall) {
        this.bridgeFirewall = canBridgeFirewall;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getConsolidationFunction() {
        return consolidationFunction;
    }

    public void setConsolidationFunction(String consolidationFunction) {
        this.consolidationFunction = consolidationFunction;
    }

    public long getDcId() {
        return dcId;
    }

    public void setDcId(long dcId) {
        this.dcId = dcId;
    }

    public String getGuestNetworkName() {
        return guestNetworkName;
    }

    public void setGuestNetworkName(String guestNetworkName) {
        this.guestNetworkName = guestNetworkName;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public int getHeartbeatTimeout() {
        return heartbeatTimeout;
    }

    public void setHeartbeatTimeout(int heartbeatTimeout) {
        this.heartbeatTimeout = heartbeatTimeout;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public boolean isOvs() {
        return ovs;
    }

    public void setOvs(boolean isOvs) {
        this.ovs = isOvs;
    }

    public String getLinkLocalPrivateNetworkName() {
        return linkLocalPrivateNetworkName;
    }

    public void setLinkLocalPrivateNetworkName(String linkLocalPrivateNetworkName) {
        this.linkLocalPrivateNetworkName = linkLocalPrivateNetworkName;
    }

    public int getMaxNics() {
        return maxNics;
    }

    public void setMaxNics(int maxNics) {
        this.maxNics = maxNics;
    }

    public int getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(int maxWeight) {
        this.maxWeight = maxWeight;
    }

    public int getMigrateWait() {
        return migrateWait;
    }

    public void setMigrateWait(int migratewait) {
        this.migrateWait = migratewait;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Queue<String> getPasswords() {
        return passwords;
    }

    public void setPasswords(Queue<String> password) {
        this.passwords = password;
    }

    public String getPod() {
        return pod;
    }

    public void setPod(String pod) {
        this.pod = pod;
    }

    public int getPollingIntervalInSeconds() {
        return pollingIntervalInSeconds;
    }

    public void setPollingIntervalInSeconds(int pollingIntervalInSeconds) {
        this.pollingIntervalInSeconds = pollingIntervalInSeconds;
    }

    public String getPrivateNetworkName() {
        return privateNetworkName;
    }

    public void setPrivateNetworkName(String privateNetworkName) {
        this.privateNetworkName = privateNetworkName;
    }

    public String getPublicNetworkName() {
        return publicNetworkName;
    }

    public void setPublicNetworkName(String publicNetworkName) {
        this.publicNetworkName = publicNetworkName;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public boolean isSecurityGroupEnabled() {
        return securityGroupEnabled;
    }

    public void setSecurityGroupEnabled(boolean securityGroupEnabled) {
        this.securityGroupEnabled = securityGroupEnabled;
    }

    public int getSleep() {
        return sleep;
    }

    public void setSleep(int sleep) {
        this.sleep = sleep;
    }

    public String getStorageNetworkName1() {
        return storageNetworkName1;
    }

    public void setStorageNetworkName1(String storageNetworkName1) {
        this.storageNetworkName1 = storageNetworkName1;
    }

    public String getStorageNetworkName2() {
        return storageNetworkName2;
    }

    public void setStorageNetworkName2(String storageNetworkName2) {
        this.storageNetworkName2 = storageNetworkName2;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getConfigDriveIsoPath() {
        return configDriveIsoPath;
    }

    public void setConfigDriveIsoPath(String configDriveIsopath) {
        this.configDriveIsoPath = configDriveIsopath;
    }

    public String getConfigDriveSRName() {
        return configDriveSRName;
    }

    public void setConfigDriveSRName(String configDriveSRName) {
        this.configDriveSRName = configDriveSRName;
    }

    public String getAttachIsoDeviceNumber() {
        return attachIsoDeviceNumber;
    }

    public void setAttachIsoDeviceNumber(String attachIsoDeviceNum) {
        this.attachIsoDeviceNumber = attachIsoDeviceNum;
    }

    public int getWait() {
        return wait;
    }

    public void setWait(int wait) {
        this.wait = wait;
    }

    public long getXsMemoryUsed() {
        return xsMemoryUsed;
    }

    public void setXsMemoryUsed(long xsMemoryUsed) {
        this.xsMemoryUsed = xsMemoryUsed;
    }

    public double getXsVirtualizationFactor() {
        return xsVirtualizationFactor;
    }

    public void setXsVirtualizationFactor(double xsVirtualizationFactor) {
        this.xsVirtualizationFactor = xsVirtualizationFactor;
    }
}