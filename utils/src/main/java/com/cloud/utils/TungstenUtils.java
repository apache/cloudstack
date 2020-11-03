package com.cloud.utils;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

// TODO : will determine later
public class TungstenUtils {
    private static final String tungstenBridge = "-tungsten";
    private static final String proxyVm = "proxyvm";
    private static final String secstoreVm = "secstorevm";
    private static final String userVm = "userVm";
    private static final String guestType = "guest";
    private static final String publicType = "public";
    private static final String managementType = "management";
    private static final String controlType = "control";
    private static final String netnsPrefix = "vrouter-";

    public static String getTapName(final String macAddress) {
        return "tap" + macAddress.replace(":", "");
    }

    public static boolean isTungstenBridge(final String bridgeName) {
        return bridgeName.contains(tungstenBridge);
    }

    public static String getBridgeName() {
        return tungstenBridge;
    }

    public static String getVmiName(String trafficType, String vmType, String vmName, long nicId) {
        if (nicId != 0 && trafficType == getGuestType())
            return "vmi" + trafficType + vmType + nicId;
        else
            return "vmi" + trafficType + vmType + vmName;
    }

    public static String getInstanceIpName(String trafficType, String vmType, String vmName, long nicId) {
        if (nicId != 0 && trafficType == getGuestType())
            return "instanceIp" + trafficType + vmType + nicId;
        else
            return "instanceIp" + trafficType + vmType + vmName;
    }

    public static String getLogicalRouterName(long zoneId) {
        return "logicalRouter" + zoneId;
    }

    public static String getNetworkGatewayVmiName(long vnId) {
        return "internalGatewayVmi" + vnId;
    }

    public static String getNetworkGatewayIiName(long pvnId) {
        return "internalGatewayIi" + pvnId;
    }

    public static String getPublicNetworkName(long pvnId) {
        return "publicNetwork" + pvnId;
    }

    public static String getManagementNetworkName(long mvnId) {
        return "managementNetwork" + mvnId;
    }

    public static String getControlNetworkName(long cvnId) {
        return "controlNetwork" + cvnId;
    }

    public static String getProxyVm() {
        return proxyVm;
    }

    public static String getSecstoreVm() {
        return secstoreVm;
    }

    public static String getUserVm() {
        return userVm;
    }

    public static String getGuestType() {
        return guestType;
    }

    public static String getPublicType() {
        return publicType;
    }

    public static String getManagementType() {
        return managementType;
    }

    public static String getControlType() {
        return controlType;
    }

    public static String getVgwName(long zoneId) {
        return "vgw" + zoneId;
    }

    public static String getDefaultRoute() {
        return "0.0.0.0/0";
    }

    public static String getVrfNetworkName(List<String> networkQualifiedName) {
        List<String> vrfList = new ArrayList<>(networkQualifiedName);
        vrfList.add(networkQualifiedName.get(networkQualifiedName.size() - 1));
        return StringUtils.join(vrfList, ":");
    }

    public static String getFloatingIpPoolName(long zoneId) {
        return "floating-ip-pool" + zoneId;
    }

    public static String getFloatingIpName(long nicId) {
        return "floating-ip" + nicId;
    }

    public static String getNetnsPrefix() {
        return netnsPrefix;
    }
}
