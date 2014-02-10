package org.apache.cloudstack.storage.datastore.response;

import org.apache.cloudstack.storage.datastore.model.Tsms;
import com.google.gson.annotations.SerializedName;

public class ListTsmsResponse {

    @SerializedName("listTsmResponse")
    private Tsms tsms;

    public int getTsmsCount() {
        return tsms.getCount();
    }

    public Tsms getTsms() {
        return tsms;
    }
}
