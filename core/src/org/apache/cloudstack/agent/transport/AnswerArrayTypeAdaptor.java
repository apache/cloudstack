package org.apache.cloudstack.agent.transport;

import java.lang.reflect.Array;

import com.cloud.agent.api.Answer;
import com.google.gson.stream.JsonWriter;

public class AnswerArrayTypeAdaptor extends GenericArrayTypeAdaptor<Answer> {

    @Override
    protected Answer[] newArray(int size) {
        Answer[] answers = (Answer[])Array.newInstance(Answer.class, size);
        return answers;
    }

    @Override
    protected void writeElement(JsonWriter out, Answer elem) {
        _gson.toJson(elem,elem.getClass(),out);
    }

}
