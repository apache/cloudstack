package org.apache.cloudstack.network.tungsten.vrouter;

import com.google.gson.annotations.SerializedName;

public class Subnet {
    @SerializedName("ip-address")
    private String prefix;
    @SerializedName("prefix-len")
    private int length;

    public Subnet(final String prefix, final int length) {
        this.prefix = prefix;
        this.length = length;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    public int getLength() {
        return length;
    }

    public void setLength(final int length) {
        this.length = length;
    }
}

