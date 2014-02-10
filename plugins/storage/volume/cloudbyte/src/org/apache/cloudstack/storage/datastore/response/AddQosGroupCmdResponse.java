package org.apache.cloudstack.storage.datastore.response;

import org.apache.cloudstack.storage.datastore.model.QoSGroup;
import org.apache.cloudstack.storage.datastore.model.QoSGroupWrapper;

import com.google.gson.annotations.SerializedName;

public class AddQosGroupCmdResponse {

    @SerializedName("addqosgroupresponse")
    private QoSGroupWrapper qosGroupWrapper;

    public QoSGroup getQoSGroup() {
        return qosGroupWrapper.getQosGroup();
    }
}
