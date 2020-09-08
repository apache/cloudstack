package com.cloud.utils;

// TODO : will determine later
public class TungstenUtils {
    private static final String tungstenBridge = "-tungsten";

    public static String getTapName(final String macAddress) {
        return "tap" + macAddress.replace(":", "");
    }

    public static boolean isTungstenBridge(final String bridgeName) {
        return bridgeName.contains(tungstenBridge);
    }

    public static String getBridgeName() {
        return tungstenBridge;
    }

    public static String getVmiName(long id) {
        return "vmi" + id;
    }

    public static String getInstanceIpName(long id) {
        return "instanceIp" + id;
    }
}
