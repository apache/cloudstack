package com.cloud.network.guru;

import java.util.Map;

public interface NetworkGuruAdditionalFunctions {

    public static final String NSX_LSWITCH_UUID = "logicalswitch";
    public static final String NSX_LSWITCHPORT_UUID = "logicalswitchport";

    void finalizeNetworkDesign(long networkId, String vlanIdAsUUID);
    Map<String, ? extends Object> listAdditionalNicParams(String nicUuid);
}
