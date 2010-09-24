package com.cloud.api.response;

import java.lang.reflect.Modifier;
import java.util.List;

import com.cloud.api.ResponseObject;
import com.cloud.serializer.GsonHelper;
import com.google.gson.Gson;

public class ApiResponseSerializer {
    // FIXME:  what about XML response?
    public static String toSerializedString(ResponseObject result) {
        if (result != null) {
            Gson gson = GsonHelper.getBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).create();
            StringBuilder sb = new StringBuilder();

            sb.append("{ \"" + result.getResponseName() + "\" : ");
            if (result instanceof ListResponse) {
                List<? extends ResponseObject> responses = ((ListResponse)result).getResponses();
                if ((responses != null) && !responses.isEmpty()) {
                    int count = responses.size();
                    String jsonStr = gson.toJson(responses.get(0));
                    sb.append("{ \"" + responses.get(0).getResponseName() + "\" : [  " + jsonStr);
                    for (int i = 1; i < count; i++) {
                        jsonStr = gson.toJson(responses.get(i));
                        sb.append(", " + jsonStr);
                    }
                    sb.append(" ] }");
                } else {
                    sb.append("{ }");
                }
            } else {
                // FIXME:  nested objects?
                String jsonStr = gson.toJson(result);
                if ((jsonStr != null) && !"".equals(jsonStr)) {
                    sb.append(jsonStr);
                } else {
                    sb.append("{ }");
                }
            }
            sb.append(" }");
            return sb.toString();
        }
        return null;
    }
}
