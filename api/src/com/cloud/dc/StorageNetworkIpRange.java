package com.cloud.dc;

public interface StorageNetworkIpRange {
    String getUuid();

    Integer getVlan();

    String getPodUuid();

    String getStartIp();

    String getEndIp();

    String getNetworkUuid();

    String getZoneUuid();
}
