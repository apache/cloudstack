package com.cloud.api.commands;


import org.apache.cloudstack.network.ExternalNetworkDeviceManager.NetworkDevice;

import com.cloud.network.Network.Provider;

public class VnsConstants {
    public static final String BIGSWITCH_VNS_DEVICE_ID = "vnsdeviceid";
    public static final String BIGSWITCH_VNS_DEVICE_NAME = "bigswitchdevicename";

    public static final String EVENT_EXTERNAL_VNS_CONTROLLER_ADD = "PHYSICAL.VNSCONTROLLER.ADD";
    public static final String EVENT_EXTERNAL_VNS_CONTROLLER_DELETE = "PHYSICAL.VNSCONTROLLER.DELETE";

    public static final Provider BigSwitchVns = new Provider("BigSwitchVns", true);

    public static final NetworkDevice BigSwitchVnsDevice = new NetworkDevice("BigSwitchVns", BigSwitchVns.getName());

}
