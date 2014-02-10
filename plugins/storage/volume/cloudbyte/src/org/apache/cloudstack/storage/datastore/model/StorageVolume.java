package org.apache.cloudstack.storage.datastore.model;

import java.util.HashMap;

import com.google.gson.annotations.SerializedName;

public class StorageVolume {

    @SerializedName("volumes")
    private HashMap<String, String> volumeProperties;

    @SerializedName("id")
    private String uuid;

    @SerializedName("name")
    private String name;

    @SerializedName("ipaddress")
    private String ipAddress;

    @SerializedName("iqnname")
    private String iqnName;

    public String getUuid() {
        return null == volumeProperties ? uuid : volumeProperties.get("id");
    }

    public String getName() {
        return null == volumeProperties ? name : volumeProperties.get("name");
    }

    public String getIPAddress() {
        return ipAddress;
    }

    public String getIQNName() {
        return iqnName;
    }
}
