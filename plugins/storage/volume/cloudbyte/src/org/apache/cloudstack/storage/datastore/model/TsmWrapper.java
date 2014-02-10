package org.apache.cloudstack.storage.datastore.model;

import com.google.gson.annotations.SerializedName;

public class TsmWrapper {

    @SerializedName("tsm")
    private Tsm tsm;

    public Tsm getTsm() {
        return tsm;
    }

}
