package org.apache.cloudstack.storage.datastore.response;

import org.apache.cloudstack.storage.datastore.model.Tsm;
import org.apache.cloudstack.storage.datastore.model.TsmWrapper;

import com.google.gson.annotations.SerializedName;

public class CreateTsmCmdResponse {

    @SerializedName("createTsmResponse")
    private TsmWrapper tsmWrapper;

    public Tsm getTsm() {
        return tsmWrapper.getTsm();
    }

}
