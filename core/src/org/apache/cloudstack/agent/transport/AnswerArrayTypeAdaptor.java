package org.apache.cloudstack.agent.transport;

import java.io.IOException;
import java.lang.reflect.Array;

import com.cloud.agent.api.Answer;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.stream.JsonWriter;

public class AnswerArrayTypeAdaptor extends GenericArrayTypeAdaptor<Answer> {

    @Override
    protected Answer[] newArray(int size) {
        Answer[] answers = (Answer[])Array.newInstance(Answer.class, size);
        return answers;
    }

    @Override
    protected void writeElement(JsonWriter out, Answer elem) {
        try {
            String data = _gson.toJson(elem);
            if(data != null && !data.equals("null")) {
                out.beginObject();
                out.name(elem.getClass().getCanonicalName());
                out.jsonValue(data);
                out.endObject();
            }
        } catch (IOException e) {
            throw new CloudRuntimeException("serializing json failed for " + elem.getClass(), e);
        }
    }

}
