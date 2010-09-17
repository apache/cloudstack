package com.cloud.api.response;

import com.cloud.api.ResponseObject;
import com.cloud.serializer.GsonHelper;
import com.google.gson.Gson;

public class ApiResponseSerializer {
    // FIXME:  what about XML response?
    public static String toSerializedString(ResponseObject result) {
        if (result != null) {
            Gson gson = GsonHelper.getBuilder().create();
            
            return gson.toJson(result); 
        } 
        return null;
    }
}
