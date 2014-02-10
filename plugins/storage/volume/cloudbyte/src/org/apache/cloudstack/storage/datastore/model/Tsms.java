package org.apache.cloudstack.storage.datastore.model;

import com.google.gson.annotations.SerializedName;

public class Tsms {

    @SerializedName("count")
    private int count;

    @SerializedName("listTsm")
    private Tsm[] tsms;

    public int getCount() {
        return count;
    }

    public Tsm getTsm(int i) {
        return tsms[i];
    }
}
